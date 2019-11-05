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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;

public class CompletedJobFuture implements Future<ActivityMBean> {

	private final ActivityMBean bean;

	public CompletedJobFuture(final ActivityMBean bean) {
		this.bean = bean;
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return true; // already completed - nothing to cancel
	}

	@Override
	public boolean isCancelled() {
		return false; // no since completed
	}

	@Override
	public boolean isDone() {
		return true; // yes since completed
	}

	@Override
	public ActivityMBean get() throws InterruptedException, ExecutionException {
		return bean;
	}

	@Override
	public ActivityMBean get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return bean; // ignore timeout parameters since completed
	}

}
