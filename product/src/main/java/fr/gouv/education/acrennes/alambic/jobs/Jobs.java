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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

public class Jobs {

    private static final Log log = LogFactory.getLog(Jobs.class);
    private final CallableContext ctxt;

    public Jobs(final String executionPath, final String fichierXml, final Variables variables, final Properties configuration)
            throws IOException, AlambicException, JDOMException {
        Document jobsDocument = JobHelper.parse(fichierXml);

        // load the file local variables (added to the variables previously loaded from the file variables.xml)
        List<Element> varEntries = JobHelper.getVariables(jobsDocument);
        if (varEntries != null && !varEntries.isEmpty()) {
            variables.loadFromXmlNode(varEntries);
        }

        // initialize the execution context of the jobs
        ctxt = new JobContext(executionPath, jobsDocument, variables, configuration);
    }

    public List<Future<ActivityMBean>> executeAllJobs(final String runId) throws AlambicException {
        final List<Future<ActivityMBean>> list = new ArrayList<Future<ActivityMBean>>();
        List<Element> jobsList = JobHelper.getJobs(this.ctxt.getJobDocument());
        for (final Element job : jobsList) {
            list.add(executeJob(ctxt, job, runId));
        }
        return list;
    }

    public List<Future<ActivityMBean>> executeJobList(final List<String> tasksList, final String runId) {
        final List<Future<ActivityMBean>> list = new ArrayList<>();
        for (String task : tasksList) {
            try {
                Element jobDefinition = JobHelper.getJobDefinition(this.ctxt.getJobDocument(), task);
                if (null != jobDefinition) {
                    list.add(executeJob(ctxt, jobDefinition, runId));
                } else {
                    log.error("Tache [" + task + "] inconnue");
                }
            } catch (final Exception e) {
                log.error("Erreur à l'execution de la tache [" + task + "] ", e);
            }
        }

        return list;
    }

    private Future<ActivityMBean> executeJob(final CallableContext context, final Element job, final String runId) throws AlambicException {
        Future<ActivityMBean> jobFuture = null;

        try {
            final JobRunner runnableJob = new JobRunner(context, job, runId);
            jobFuture = ExecutorFactory.submitJob(runnableJob);
        } catch (final Exception e) {
            throw new AlambicException(e);
        }

        return jobFuture;
    }

}
