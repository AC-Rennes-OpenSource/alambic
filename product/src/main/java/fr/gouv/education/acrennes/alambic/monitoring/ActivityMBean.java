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
package fr.gouv.education.acrennes.alambic.monitoring;

import java.util.List;

import javax.management.ObjectName;

import fr.gouv.education.acrennes.alambic.nuxeo.marshaller.Report;

public interface ActivityMBean {

	public enum ACTIVITY_STATUS {
		WAITING,
		RUNNING,
		COMPLETED
	}

	public enum ACTIVITY_TYPE {
		META,
		INNER
	}

	public String getJobName();

	public int getProgress();

	public void setProgress(int value);
	
	public long getDuration();

	public String getProcessing();

	public void setProcessing(String processing);

	public String getStatus();

	public void setStatus(String status);

	public long getStartTime();

	public void setStartTime(long time);

	public long getEndTime();

	public void setEndTime(long time);

	public String getThread();

	public void setThread(String name);

	public ActivityTrafficLight getTrafficLight();

	public void setTrafficLight(ActivityTrafficLight light);

	public void registerInnerActivity(final ActivityMBean innerActivity);
	
	public void setInnerJobsCount(int count);

	public int getInnerJobsCount();
	
	public Object getResult();

	public void setResult(Object val);
	
	public ObjectName getObjectName();
	
	public void addError(String e);

	public void addError(Exception e);
	
	public List<Exception> getErrors();
	
	public String toString();
	
	public Report getReport();	
}