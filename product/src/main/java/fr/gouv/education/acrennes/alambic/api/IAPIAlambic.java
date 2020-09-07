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
package fr.gouv.education.acrennes.alambic.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;

public interface IAPIAlambic {

	List<Future<ActivityMBean>> run(
			String addonName,
			String jobFileName,
			boolean isReadOnlyJob,
			List<String> tasksList,
			Map<String, String> parameters
	);

	void closeInstance();

}