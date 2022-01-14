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
package fr.gouv.education.acrennes.alambic.jobs.load;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

public class StateBaseToFileDelete extends AbstractDestination {

	private static final Log log = LogFactory.getLog(StateBaseToFileDelete.class);

	private int count;

	public StateBaseToFileDelete(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
		super(context, job, jobActivity);
		this.count = 0;
	}

	@Override
	public void execute() throws AlambicException {
		List<Map<String, List<String>>> stateBase = (source != null) ? source.getEntries() : Collections.emptyList();

		for (final Map<String, List<String>> item : stateBase) {
			// activity monitoring
			jobActivity.setProgress(((this.count + 1) * 100) / stateBase.size());
			jobActivity.setProcessing("processing entry " + (this.count + 1) + "/" + stateBase.size());

			// delete the file (ignore directories)
	        try {
	        	if (Boolean.parseBoolean(item.get("isFile").get(0))) {
	        		Path path = Paths.get(item.get("path").get(0));
	        		if (!isDryMode) {
	        			Files.delete(path);
	        			log.info("Deleted the file '" + path + "'");
	        		} else {
	        			log.info("[DRY MODE] Deleted the file '" + path + "'");
	        		}
	        	} else {
	        		log.info("Ignore '" + item.get("name").get(0) + "' since it is a directory");
	        	}
			} catch (IOException e) {
				jobActivity.setTrafficLight(ActivityTrafficLight.RED);
				throw new AlambicException(e);
			}
		}

		count++;
	}

	@Override
	public boolean isDryModeSupported() {
		return true; // This files won't be deleted from the file system
	}

}