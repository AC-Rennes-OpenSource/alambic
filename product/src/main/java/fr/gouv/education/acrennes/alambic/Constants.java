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
package fr.gouv.education.acrennes.alambic;

import freemarker.template.Configuration;
import freemarker.template.Version;

public interface Constants {

	public static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_28;
	public static final String JOB_ASYNCH_ATTRIBUTE_NAME = "asynch";
	public static final String CHILD_JOBS_ASYNCH_ATTRIBUTE_NAME = "asynchChildJobs";
	public static final String RANDOM_GENERATOR_INNER_ITERATION = "random_generator_inner_iteration";
	public static final String UNLIMITED_GENERATOR_FILTER = "UNLIMITED_GENERATOR_FILTER";
	public static final int MAX_LDAP_RELATION_SIZE = 10000;
	public static final int DEFAULT_SALT_LENGTH = 16;
	
}
