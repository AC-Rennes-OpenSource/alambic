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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Activity implements ActivityMBean {

	private static final Log log = LogFactory.getLog(Activity.class);

	private final String jobName;
	private String threadName;
	private int progress;
	private int innerJobsCount;
	private String processing;
	private ACTIVITY_STATUS status;
	private ActivityTrafficLight trafficLight;
	private List<ActivityMBean> innerActivitiesList;
	private long startTime;
	private long endTime;
	private Object result;
	private ObjectName objectName;
	
	public Activity(final String name, final ObjectName objectName, final String threadName) {
		jobName = name;
		this.threadName = threadName;
		progress = 0;
		processing = "";
		innerActivitiesList = Collections.emptyList();
		startTime = 0;
		endTime = 0;
		innerJobsCount = 0;
		result = null;
		status = ACTIVITY_STATUS.WAITING;
		trafficLight = ActivityTrafficLight.GREEN;
		this.objectName = objectName;
	}

	@Override
	public String getJobName() {
		return jobName;
	}

	@Override
	public int getProgress() {
		int p = progress;
		if (!this.innerActivitiesList.isEmpty()) {
			p = 0;
			for (ActivityMBean innerActivity : this.innerActivitiesList) {
				p += innerActivity.getProgress();
			}
			p = p / ((this.innerJobsCount > this.innerActivitiesList.size()) ? this.innerJobsCount : this.innerActivitiesList.size() );
		}
		return p;
	}

	@Override
	public void setProgress(final int value) {
		progress = value;
	}

	@Override
	public String getProcessing() {
		return processing;
	}

	@Override
	public void setProcessing(final String processing) {
		this.processing = processing;
	}

	@Override
	public String getStatus() {
		return status.toString();
	}

	@Override
	public void setStatus(final String statusStg) {
		ACTIVITY_STATUS previousStatus = status;
		status = ACTIVITY_STATUS.valueOf(statusStg);

		// update start/end activity time
		if (!ACTIVITY_STATUS.RUNNING.equals(previousStatus) && ACTIVITY_STATUS.RUNNING.equals(status)) {
			startTime = System.currentTimeMillis();
		} else if (ACTIVITY_STATUS.RUNNING.equals(previousStatus) && !ACTIVITY_STATUS.RUNNING.equals(status)) {
			endTime = System.currentTimeMillis();
		}

		if (ACTIVITY_STATUS.COMPLETED.equals(status)) {
			processing = "";
			progress = 100;
		}
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public void setStartTime(final long time) {
		startTime = time;
	}

	@Override
	public long getEndTime() {
		return endTime;
	}

	@Override
	public void setEndTime(final long time) {
		endTime = time;
	}

	@Override
	public void registerInnerActivity(final ActivityMBean innerActivity) {
		if (Collections.EMPTY_LIST.equals(innerActivitiesList)) {
			innerActivitiesList = new ArrayList<>();
		}
		innerActivitiesList.add(innerActivity);
	}

	@Override
	public String getThread() {
		return threadName;
	}

	@Override
	public void setThread(final String name) {
		threadName = name;
	}

	@Override
	public ActivityTrafficLight getTrafficLight() {
		return trafficLight;
	}

	@Override
	public void setTrafficLight(final ActivityTrafficLight light) {
		if (0 > trafficLight.compareTo(light)) {
			trafficLight = light;
		}
	}

	@Override
	public void setInnerJobsCount(int count) {
		this.innerJobsCount = count;
	}

	@Override
	public int getInnerJobsCount() {
		return this.innerJobsCount;
	}
	
	@Override
	public void setResult(Object val) {
		this.result = val;
	}
	
	@Override
	public Object getResult() {
		ActivityMBean ambr = null; // the activity bean whose result will be returned
		if (!this.innerActivitiesList.isEmpty()) {
			for (ActivityMBean innerActivity : this.innerActivitiesList) {
				if (null != innerActivity.getResult()) {
					if (null == ambr) {
						ambr = innerActivity;
					} else {
						log.warn("The job '" + this.getJobName() + "' has already registered a result from the inner job '" + ambr.getJobName() + "'. The conflicting inner job '" + innerActivity.getJobName() + "' result is ignored");
					}
				}
			}
		}
		return (null == ambr) ? this.result : ambr.getResult();
	}

	@Override
	public ObjectName getObjectName() {
		return objectName;
	}
	
}
