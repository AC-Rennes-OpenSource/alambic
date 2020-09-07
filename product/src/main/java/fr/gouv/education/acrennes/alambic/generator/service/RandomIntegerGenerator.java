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

import java.util.Map;

import javax.persistence.EntityManager;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class RandomIntegerGenerator extends AbstractRandomGenerator {

//	private static final Log log = LogFactory.getLog(RandomIntegerGenerator.class);

	public RandomIntegerGenerator(final EntityManager em) throws AlambicException {
		super(em);
	}

	@Override
	public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope) throws AlambicException {
		RandomEntity entity = null;

		int minValue = (null != query.get("minValue")) ? (int) query.get("minValue") : 0;
		int maxValue = (null != query.get("maxValue")) ? (int) query.get("maxValue") : 0;
		if (maxValue != 0 && (maxValue >= minValue)) {
			int randomValue = (int) (Math.round(minValue + (Math.random() * (maxValue - minValue))));
			entity = new RandomLambdaEntity("{\"value\":\"" + randomValue + "\"}");
		} else {
			throw new AlambicException("Not consistent value of parameters 'minValue' and/or 'maxValue'");
		}
		
		return entity;
	}

	@Override
	public RandomGeneratorService.GENERATOR_TYPE getType(final Map<String, Object> query) {
		return RandomGeneratorService.GENERATOR_TYPE.INTEGER;
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		long capacity = 0;

		int minValue = (null != query.get("minValue")) ? (int) query.get("minValue") : 0;
		int maxValue = (null != query.get("maxValue")) ? (int) query.get("maxValue") : 0;
		if (maxValue != 0 && (maxValue >= minValue)) {
			capacity = maxValue - minValue + 1;
		} else {
			throw new AlambicException("Not consistent value of parameters 'minValue' and/or 'maxValue'");
		}

		return capacity;
	}

	@Override
	public String getCapacityFilter(Map<String, Object> query) {
		int minValue = (null != query.get("minValue")) ? (int) query.get("minValue") : 0;
		int maxValue = (null != query.get("maxValue")) ? (int) query.get("maxValue") : 0;
		return String.format("[%d-%d]", minValue, maxValue);
	}

}
