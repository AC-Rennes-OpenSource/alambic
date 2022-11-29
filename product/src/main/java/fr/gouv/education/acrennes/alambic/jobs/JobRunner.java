/*******************************************************************************
 * Copyright (C) 2019-2020 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.gouv.education.acrennes.alambic.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Attribute;
import org.jdom2.Element;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.FakeSource;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.SourceFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination.IsAnythingToDoStatus;
import fr.gouv.education.acrennes.alambic.jobs.load.Destination;
import fr.gouv.education.acrennes.alambic.jobs.load.DestinationFactory;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityHelper;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean.ACTIVITY_STATUS;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean.ACTIVITY_TYPE;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

public class JobRunner implements CallableJob {

	private static final Log log = LogFactory.getLog(JobRunner.class);

	private final Element job;
	private Source source;
	private Source pagedSource;
	private Map<String, Source> resources;
	private final boolean isAsynchronous;
	private final CallableContext context;
	private final ActivityMBean parentActivityBean;
	private final String runId;

	public JobRunner(final CallableContext context, final Element job, final String runId) {
		this(context, job, null, null, runId);
	}

	public JobRunner(final CallableContext context, final Element job, final Source pagedSource, ActivityMBean parentActivityBean, final String runId) {
		this.context = context;
		this.job = job;
		this.pagedSource = pagedSource;
		this.isAsynchronous = Boolean.parseBoolean(job.getAttributeValue(Constants.JOB_ASYNCH_ATTRIBUTE_NAME));
		this.parentActivityBean = parentActivityBean;
		this.runId = runId;
	}

	@Override
	public ActivityMBean call() {
		ActivityMBean bean = null;
		String jobName = getName();
		
		log.info("Start processing job '" + jobName + "'");

		bean = execute(this.job, this.parentActivityBean, runId);

		log.info("Finished processing job '" + jobName + "'");

		return bean;
	}

	public ActivityMBean execute(final Element job, final ActivityMBean parentActivityBean, final String runId) {
		Destination destination = null;
		
		final ActivityMBean jobActivity = ActivityHelper.getMBean(getName(job), (null == parentActivityBean) ? ACTIVITY_TYPE.META : ACTIVITY_TYPE.INNER, runId);
		jobActivity.setStatus(ACTIVITY_STATUS.RUNNING.toString());
		jobActivity.setProcessing("Analyze job definition...");
		if (null != parentActivityBean) {
			parentActivityBean.registerInnerActivity(jobActivity);
		}

		try {
			final List<Element> jobList = job.getChildren("execute-job");
			final List<Element> templateList = job.getChildren("execute-template");

			if (!templateList.isEmpty()) {
				executeSubJobTemplateList(templateList);
			} else if (!jobList.isEmpty()) {
				final List<Future<ActivityMBean>> futuresList = new ArrayList<>();
				jobActivity.setInnerJobsCount(jobList.size());
				final Iterator<Element> itr = jobList.iterator();
				while (itr.hasNext() && doRunJob(job, jobActivity)) {
					final Element node = itr.next();
					JobDefinition jobDefinition = JobHelper.getJobDefinition(this.context, node);
					if (null != jobDefinition) {
						if (Boolean.parseBoolean(job.getAttributeValue(Constants.CHILD_JOBS_ASYNCH_ATTRIBUTE_NAME))) {
							jobDefinition.getDefinition().setAttribute(Constants.JOB_ASYNCH_ATTRIBUTE_NAME, "true");
						}
						jobActivity.setProcessing("Submit job '" + jobDefinition.getDefinition().getAttributeValue("name") + "' for execution");
						final Future<ActivityMBean> future = ExecutorFactory.submitJob(new JobRunner(jobDefinition.getContext(), jobDefinition.getDefinition(), null, jobActivity, runId));
						futuresList.add(future);
					} else {
						throw new AlambicException("Failed to find the XML definition of the job '" + node.getAttributeValue("name") + "'");
					}
				}

				// Wait for all inner jobs to complete
				for (final Future<ActivityMBean> future : futuresList) {
					future.get(); // waiting for the job completion
				}
			} else {
				final Source pagedSrc = getPagedSource(job);
				if ((null == pagedSource) && (null != pagedSrc)) {
					pagedSource = pagedSrc;
					jobActivity.setProcessing("Handle paged source '" + pagedSrc.getName() + "'...");
					final Iterator<List<Map<String, List<String>>>> pageItr = pagedSource.getPageIterator();
					int page = 1;
					final List<Future<ActivityMBean>> futuresList = new ArrayList<>();
					while (pageItr.hasNext() && doRunJob(job, jobActivity)) {
						// Run a new job dealing with this page of results straight away (since multi-threaded) and go back to search for a new page of results
						job.setAttribute(Constants.JOB_ASYNCH_ATTRIBUTE_NAME, "true");
						final Future<ActivityMBean> future = ExecutorFactory.submitJob(new JobRunner(context, job, new FakeSource(pagedSource.getName(), page++, pageItr.next()), jobActivity, runId));
						futuresList.add(future);
					}

					// Wait for all paged jobs to complete
					for (final Future<ActivityMBean> future : futuresList) {
						future.get(); // waiting for the job completion
					}
				} else {
					jobActivity.setProcessing("Initialize sources...");
					// possible multiple resources
					if (job.getChild("resources") != null) {
						resources = new HashMap<>();
						final Element resourcesElt = job.getChild("resources");
						final List<Element> resourcesList = resourcesElt.getChildren("resource");
						for (final Element resource : resourcesList) {
							final String resourceName = resource.getAttributeValue("name");
							if (StringUtils.isNotBlank(resourceName)) {
								if ((null != pagedSource) && (pagedSource.getName().equals(resourceName))) {
									resources.put(pagedSource.getName(), pagedSource);
								} else {
									final Source source = SourceFactory.getSource(context, resource);
									if (null != source) {
										resources.put(resourceName, source);
									}
								}
							} else {
								throw new AlambicException("L'attribut 'name' n'est pas renseigné sur une ressource.");
							}
						}
					}

					// single source
					source = SourceFactory.getSource(context, job.getChild("source"));
					if ((null != pagedSource) && (null != source) && (pagedSource.getName().equals(source.getName()))) {
						source = pagedSource;
					}
					
					// destination
					destination = DestinationFactory.getDestination(context, job.getChild("destination"), jobActivity);
					jobActivity.setProcessing("Start loading in destination...");
					if (null != destination) {
						destination.setResources(resources);
						destination.setSource(source);
						destination.setPage((null != pagedSource) ? pagedSource.getPage() : AbstractDestination.NOT_PAGED);
						if (destination.isAnythingToDo().equals(IsAnythingToDoStatus.YES)) {
							destination.execute();
						} else {
							log.info("Job '" + getName() + "' : no operations defined by the job input file");
						}
					}
				}
			}
		} catch (final Exception e) {
			log.error("Failed to run the job '" + getName() + "', error : " + e.getMessage());
			if (null == e.getMessage()) {
				e.printStackTrace();
			}
			
			if (null != jobActivity) {
				jobActivity.setTrafficLight(ActivityTrafficLight.RED);
				jobActivity.addError(e);
			}
		} finally {
			if (null != pagedSource) {
				pagedSource.close();
			}

			if ((null != source) && (source != pagedSource)) {
				source.close();
			}

			if (null != resources) {
				for (final String key : resources.keySet()) {
					if (resources.get(key) != pagedSource) {
						resources.get(key).close();
					}
				}
			}

			pagedSource = null;
			source = null;
			resources = null;

			if (null != destination) {
				try {
					destination.close();
					destination = null;
				} catch (final AlambicException e) {
					log.error("Failed to close the destination '" + destination.getType(), e);
				}
			}

			if (null != jobActivity) {
				jobActivity.setStatus(ACTIVITY_STATUS.COMPLETED.toString());
			}
		}
		
		ActivityHelper.releaseMBean(jobActivity);
		return jobActivity;
	}

	@Override
	public String getName() {
		return getName(this.job);
	}

	private String getName(Element job) {
		return job.getAttributeValue("name") + ((null != pagedSource) ? "-page-" + pagedSource.getPage() : "");
	}

	@Override
	public boolean isAsynchronous() {
		return isAsynchronous;
	}

	private Source getPagedSource(final Element job) throws AlambicException {
		Source pagedSource = null;

		// possible multiple resources
		if (job.getChild("resources") != null) {
			resources = new HashMap<>();
			final Element resourcesElt = job.getChild("resources");
			final List<Element> resourcesList = resourcesElt.getChildren("resource");
			for (final Element resource : resourcesList) {
				final String page = resource.getAttributeValue("page");
				if (StringUtils.isNotBlank(page)) {
					pagedSource = SourceFactory.getSource(context, resource);
					break; // a single paged source is possible
				}
			}
		}

		// single source
		if (null == pagedSource) {
			final Element sourceElt = job.getChild("source");
			final String page = (null != sourceElt) ? sourceElt.getAttributeValue("page") : "";
			if (StringUtils.isNotBlank(page)) {
				pagedSource = SourceFactory.getSource(context, sourceElt);
			}
		}

		return pagedSource;
	}
	
	private boolean doRunJob(final Element job, final ActivityMBean activityBean) {
		boolean doRun = false;
		
		String jobFailureThreshold = job.getAttributeValue("failure-threshold");
		if ( StringUtils.isBlank(jobFailureThreshold) || ActivityTrafficLight.valueOf(jobFailureThreshold).isGreaterThan(activityBean.getTrafficLight()) ) {
			doRun = true;
		} else {
			activityBean.setStatus("COMPLETED");
			log.warn("The failure threshold '" + jobFailureThreshold + "' is reached. Job '" + job.getAttributeValue("name") + "' is interrupted (report : " + activityBean + ").");
		}
		
		return doRun;
	}

	private void executeSubJobTemplateList(final List<Element> executeList) throws AlambicException {
		int index = 0;
		final Iterator<Element> i = executeList.iterator();
		while (i.hasNext()) {
			index++;
			final Element node = i.next();

			// Récupération de l'attribut parameters
			final Attribute attrParameters = node.getAttribute("parameters");
			if (attrParameters != null) {
				log.debug("Chargement des parametres");
				final String parameters = attrParameters.getValue();

				if (parameters.matches("(.+ *)+")) {
					log.debug("Liste des paramètres ");
					final String[] params = parameters.split(" ");
					for (int i1 = 0; i1 < params.length; i1++) {
						context.getVariables().put("p" + (i1 + 1), params[i1]);
					}
				}
			}

			// Récupération de l'attribut template
			final Attribute attrTemplate = node.getAttribute("name");
			Element jobTemplate = null;
			if (attrTemplate != null) {
				final String template = attrTemplate.getValue();
				jobTemplate = JobHelper.getTemplateDefinition(this.context.getJobDocument(), template);
				if (jobTemplate == null) {
					throw new IllegalArgumentException("Il n'y a pas de template correspondant au nom["	+ template + "]");
				}
			} else {
				log.debug("Chargement du template de job");
				throw new IllegalArgumentException("Veuillez preciser un template de Job.'");
			}

			// Mode Iteratif
			final Attribute attrIterator = node.getAttribute("iterator");
			if (attrIterator != null) {
				// get iterator
				final String iter = attrIterator.getValue();

				if (iter.matches("[a-z]+\\.\\.[a-z]+")) {
					log.debug("Incrementation alphabetique");
					final String start = iter.substring(0, iter.indexOf(".."));
					final String end = iter.substring(iter.indexOf("..") + 2);
					if (start.length() != end.length()) {
						throw new IllegalArgumentException(
								"Incrementation alphabetique: le nombre de carateres de debut et fin doivent être identiques !");
					}
					final int nStart = fromBase26(start);
					final int nEnd = fromBase26(end);
					if (nStart >= nEnd) {
						throw new IllegalArgumentException(
								"Incrementation alphabetique: les carateres de debut doivent etre superieur à ceux de fin !");
					}
					for (int pos = nStart; pos <= nEnd; pos++) {
						final String iterator = toBase26(pos);
						context.getVariables().put("i", iterator);
						final Element job = jobTemplate.clone();
						job.setAttribute("name", String.format("%s-%d-%d", job.getAttributeValue("name"), index, pos));
						execute(job, null, runId);
					}
				} else if (iter.matches("[0-9]+\\.\\.[0-9]+")) {
					log.debug("Incrementation numérique");
					throw new IllegalArgumentException("Incrementation numérique n'est pas implémentée dans cette version.");
				} else if (iter.matches("([A-Za-z\\-\\*\\.]+ *)+")) {
					log.debug("Incrementation par liste ");
					final String[] params = iter.split(" ");
					for (int i1 = 0; i1 < params.length; i1++) {
						context.getVariables().put("i", params[i1]);
						final Element job = jobTemplate.clone();
						job.setAttribute("name", String.format("%s-%d-%d", job.getAttributeValue("name"), index, i1));
						execute(job, null, runId);
					}
				} else {
					throw new IllegalArgumentException("La logique d'iteration exprimee n'est pas prise en charge. elle doit etre de type 'aa..zz' ou '045/AZT/adam.'");
				}
			} else {
				final Element job = jobTemplate.clone();
				job.setAttribute("name", String.format("%s-%d", job.getAttributeValue("name"), index));
				execute(job, null, runId);
			}
		}
	}

	public static int fromBase26(final String number) {
		int s = 0;
		if ((number != null) && (number.length() > 0)) {
			s = (number.charAt(0) - 'a');
			for (int i = 1; i < number.length(); i++) {
				s *= 26;
				s += (number.charAt(i) - 'a');
			}
		}
		return s;
	}

	public static String toBase26(int number) {
		number = Math.abs(number);
		String converted = "";
		do {
			final int remainder = number % 26;
			converted = (char) (remainder + 'a') + converted;
			number = (number - remainder) / 26;
		} while (number > 0);

		return converted;
	}
	
}
