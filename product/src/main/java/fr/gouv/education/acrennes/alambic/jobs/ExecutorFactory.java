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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutorFactory {

    public static final String THREAD_POOL_SIZE = "multithreading.pool.size";
    private static final Log log = LogFactory.getLog(ExecutorFactory.class);
    private static final String DEFAULT_THREAD_POOL_SIZE = "20";
    private static ExecutorFactory instance = null;
    private final ExecutorService executor;

    private ExecutorFactory(int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
    }

    private static ExecutorFactory getInstance() throws AlambicException {
        if (null == instance) {
            throw new AlambicException("ExecutorFactory is not already instanciated!");
        }

        return instance;
    }

    public static void initialize(Properties properties) throws AlambicException {
        String size = properties.getProperty(THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
        if (null == instance) {
            instance = new ExecutorFactory(Integer.parseInt(size));
        } else {
            throw new AlambicException("ExecutorFactory is already instanciated!");
        }
    }

    public static Future<ActivityMBean> submitJob(final CallableJob job) throws Exception {
        log.info("Submit the job '" + job.getName() + "' for execution");

        Future<ActivityMBean> future = null;
        if (job.isAsynchronous()) {
            future = getInstance().submit(job);
        } else {
            ActivityMBean activity = job.call();
            future = new CompletedJobFuture(activity);
        }

        return future;
    }

    public static void close() throws AlambicException {
        getInstance().shutdown();
    }

    private Future<ActivityMBean> submit(final CallableJob job) {
        return executor.submit(job);
    }

    private void shutdown() {
        executor.shutdown();
    }

}
