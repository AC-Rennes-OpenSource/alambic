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
package fr.gouv.education.acrennes.alambic.generator.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

public interface RandomGenerator {

	public enum UNICITY_SCOPE {
		NONE,
		PROCESS,
		PROCESS_ALL
	}

	public List<RandomEntity> getEntities(final String query, final String processId, final UNICITY_SCOPE scope) throws AlambicException;

	public RandomEntity getEntity(final Map<String, Object> query, String processId, final UNICITY_SCOPE scope) throws AlambicException;
	
	public boolean isAlreadyUsed(final Map<String, Object> query, final RandomEntity entity, final String processId, final UNICITY_SCOPE scope) throws AlambicException;

	public long getCapacity(final Map<String, Object> query) throws AlambicException;

	public String getCapacityFilter(final Map<String, Object> query) throws AlambicException;

	public RandomGeneratorService.GENERATOR_TYPE getType(final Map<String, Object> query) throws AlambicException;

	public String getPersistanceEntityType(final Map<String, Object> query) throws AlambicException;
	
	public void persist(final Serializable entity);

	public void auditEntity(final Map<String, Object> query, final RandomEntity entity, final String processId) throws AlambicException;
	
	public void close();

}