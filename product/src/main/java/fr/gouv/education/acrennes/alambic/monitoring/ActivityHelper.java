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
package fr.gouv.education.acrennes.alambic.monitoring;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean.ACTIVITY_TYPE;

public class ActivityHelper {

	private static final Log log = LogFactory.getLog(ActivityHelper.class);
	private static final String MBEAN_ID_TEMPLATE = "fr.toutatice.alambic.jmx.%s.%s.%s:type=Activity";

	private static ActivityHelper instance = null;
	private final MBeanServer server;

	private ActivityHelper() {
		server = ManagementFactory.getPlatformMBeanServer();
	}

	private static <T> ActivityHelper getInstance() {
		if (null == instance) {
			instance = new ActivityHelper();
		}

		return instance;
	}

	private ActivityMBean registerJob(final String jobName, final ACTIVITY_TYPE type, final String runId) {
		ActivityMBean amb = null;

		try {
			amb = new Activity(jobName, new ObjectName(getMBeanID(jobName, type, runId)), Thread.currentThread().getName());
			server.registerMBean(amb, amb.getObjectName());
		} catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException | MalformedObjectNameException e1) {
			log.error("Failed to register a JMX MBean for the job '" + jobName + "'", e1);
		}

		return amb;
	}
	
	private <T> void unregisterJob(ActivityMBean amb) {
		try {
			server.unregisterMBean(amb.getObjectName());
		} catch (InstanceNotFoundException | MBeanRegistrationException e1) {
			log.error("Failed to unregister a JMX MBean for the job '" + amb.getJobName() + "'", e1);
		}
	}

	/* Spaces are significant everywhere in an Object Name (source : https://www.oracle.com/technetwork/java/javase/tech/best-practices-jsp-136021.html)
	 * Hence, they must be discarded from the job's name via normalization
	 */
	private String getMBeanID(final String jobName, final ACTIVITY_TYPE type, final String runId) {
		return String.format(MBEAN_ID_TEMPLATE,
				(ACTIVITY_TYPE.META.equals(type)) ? "meta" : "inner",
				normalizeJobName(jobName),
				runId);
	}
	
	private String normalizeJobName(final String jobName) {
		return jobName.replaceAll("\\W", "-").toLowerCase();
	}
	
	public static ActivityMBean getMBean(final String jobName, final ACTIVITY_TYPE type, final String runId) {
		return getInstance().registerJob(jobName, type, runId);
	}
	
	public static void releaseMBean(ActivityMBean amb) {
		getInstance().unregisterJob(amb);
	}

}
