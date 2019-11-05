/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.utils.Variables;

public class Jobs {

	private static final Log log = LogFactory.getLog(Jobs.class);

	private CallableContext ctxt;
	private List<Element> listeJobs;
	private List<Element> listeTemplateJobs = null;

	public Jobs(final String executionPath, final String fichierXml, final Variables variables, final Properties configuration) throws IOException, AlambicException, JDOMException {
		final InputSource fXml = new InputSource(fichierXml);
		
		final SAXBuilder saxBuilder = new SAXBuilder();
		final Element racineJobs = saxBuilder.build(fXml).getRootElement();
		
		if (racineJobs.getChild("jobs") == null) {
			throw new AlambicException("L'élément 'jobs' n'existe pas dans le fichier de taches");
		}

		Element varEntries = racineJobs.getChild("variables");
		if (varEntries != null) {
			variables.loadFromXmlNode(varEntries.getChildren());
		}
		
		// Création d'une liste contenant tous les jobs
		listeJobs = racineJobs.getChild("jobs").getChildren();
		if (racineJobs.getChild("templates") != null) {
			listeTemplateJobs = racineJobs.getChild("templates").getChildren();
		}

		// initialize the execution context of the jobs
		ctxt = new JobContext(executionPath, fichierXml, variables, configuration);
	}

	public List<Future<ActivityMBean>> executeAllJobs(final String runId) throws AlambicException {
		final List<Future<ActivityMBean>> list = new ArrayList<Future<ActivityMBean>>();
		for (final Element job : listeJobs) {
			list.add(executeJob(ctxt, job, runId));
		}
		return list;
	}

	public List<Future<ActivityMBean>> executeJobList(final List<String> tasksList, final String runId) {
		final List<Future<ActivityMBean>> list = new ArrayList<>();
		for (String task : tasksList) {
			try {
				// Localisation du job
				boolean found = false;
				for (final Element job : listeJobs) {
					if (task.equals(job.getAttributeValue("name"))) {
						found = true;
						list.add(executeJob(ctxt, job, runId));
					}
				}
				if (!found) {
					log.error("Tache [" + task + "] inconnue");
				}
			} catch (final Exception e) {
				log.error("Erreur à l'execution de la tache [" + task + "] ", e);
			}
		}

		return list;
	}

	public Map<String, String> getJobs() {
		final Map<String, String> jobs = new HashMap<>();
		final Iterator<Element> i = listeJobs.iterator();
		// Récupération du job
		while (i.hasNext()) {
			final Element job = i.next();
			jobs.put(job.getAttributeValue("name"), job.getTextNormalize());
		}
		return jobs;
	}

	public Variables getVariables() {
		return ctxt.getVariables();
	}

	private Future<ActivityMBean> executeJob(final CallableContext context, final Element job, final String runId) throws AlambicException {
		Future<ActivityMBean> jobFuture = null;

		try {
			final JobRunner runnableJob = new JobRunner(context, job, listeTemplateJobs, listeJobs, null, null, runId);
			jobFuture = ExecutorFactory.submitJob(runnableJob);
		} catch (final Exception e) {
			throw new AlambicException(e);
		}

		return jobFuture;
	}

}
