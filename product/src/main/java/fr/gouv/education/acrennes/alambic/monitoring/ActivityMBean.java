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

import fr.gouv.education.acrennes.alambic.nuxeo.marshaller.Report;

import javax.management.ObjectName;
import java.util.List;

public interface ActivityMBean {

    enum ACTIVITY_STATUS {
        WAITING,
        RUNNING,
        COMPLETED
    }

    enum ACTIVITY_TYPE {
        META,
        INNER
    }

    String getJobName();

    int getProgress();

    void setProgress(int value);

    long getDuration();

    String getProcessing();

    void setProcessing(String processing);

    String getStatus();

    void setStatus(String status);

    long getStartTime();

    void setStartTime(long time);

    long getEndTime();

    void setEndTime(long time);

    String getThread();

    void setThread(String name);

    ActivityTrafficLight getTrafficLight();

    void setTrafficLight(ActivityTrafficLight light);

    void registerInnerActivity(final ActivityMBean innerActivity);

    void setInnerJobsCount(int count);

    int getInnerJobsCount();

    Object getResult();

    void setResult(Object val);

    ObjectName getObjectName();

    void addError(String e);

    void addError(Exception e);

    List<Exception> getErrors();

    String toString();

    Report getReport();
}