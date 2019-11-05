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
package fr.gouv.education.acrennes.alambic.generator.service;

import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class RandomUUidGenerator extends AbstractRandomGenerator {

//	private static final Log log = LogFactory.getLog(RandomUUidGenerator.class);

	public RandomUUidGenerator(final EntityManager em) throws AlambicException {
		super(em);
	}

	@Override
	public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope) throws AlambicException {
		return new RandomLambdaEntity("{\"uuid\":\"" + UUID.randomUUID() + "\"}");
	}

	@Override
	public String getType() {
		return RandomGeneratorService.GENERATOR_TYPE.UUID.toString();
	}

	@Override
	public String getCapacityFilter(Map<String, Object> query) {
		return Constants.UNLIMITED_GENERATOR_FILTER;
	}

}
