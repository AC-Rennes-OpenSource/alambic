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

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.freemarker.FMFunctions;
import fr.gouv.education.acrennes.alambic.freemarker.NormalizationPolicy;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class RandomUidGenerator extends AbstractRandomGenerator {

//	private static final Log log = LogFactory.getLog(RandomUidGenerator.class);
	private static enum RANDOM_UID_FORMAT {
		SHORT,
		LONG
	}
	
	private final FMFunctions fcts;

	public RandomUidGenerator(final EntityManager em) throws AlambicException{
		super(em);
		this.fcts = new FMFunctions();
	}

	@Override
	public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope) throws AlambicException {
		RandomEntity entity;
		RANDOM_UID_FORMAT format = RANDOM_UID_FORMAT.LONG;
		
		String firstName = (String) query.get("firstName");
		String lastName = (String) query.get("lastName");
		if (StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName)) {
			throw new AlambicException("Both parameters 'firstName' and 'lastName' must be set");
		}

		if (StringUtils.isNotBlank((String) query.get("format"))) {
			if ( ((String) query.get("format")).equals(RANDOM_UID_FORMAT.SHORT.toString()) ||
				((String) query.get("format")).equals(RANDOM_UID_FORMAT.LONG.toString()) ) {
				format = RANDOM_UID_FORMAT.valueOf((String) query.get("format"));
			} else {
				throw new AlambicException("Not relevant format value '" + (String) query.get("format") + "'. Supported values are 'SHORT' and 'LONG'");
			}
		}

		String uid;
		if (RANDOM_UID_FORMAT.LONG.equals(format) ) {
			uid = this.fcts.normalize(String.format("%s.%s", firstName, lastName), NormalizationPolicy.UID, true).toLowerCase();
		} else {
			uid = this.fcts.normalize(String.format("%s%s", firstName.substring(0, 1), lastName), NormalizationPolicy.UID, true).toLowerCase();
		}
		
		// handle random generation iteration
		int iteration = (int) query.get(Constants.RANDOM_GENERATOR_INNER_ITERATION);
		if (1 < iteration) {
			uid = uid.concat(String.valueOf(iteration));
		}

		// persist the entity built so that it can be found in "reuse" context
		entity = new RandomLambdaEntity("{\"uid\":\"" + uid + "\"}");
		em.persist(entity);

		return entity;
	}

	@Override
	public String getType() {
		return RandomGeneratorService.GENERATOR_TYPE.UID.toString();
	}

	@Override
	public String getCapacityFilter(Map<String, Object> query) throws AlambicException {
		RANDOM_UID_FORMAT format = RANDOM_UID_FORMAT.LONG;

		String firstName = (String) query.get("firstName");
		String lastName = (String) query.get("lastName");
		if (StringUtils.isNotBlank(firstName) && StringUtils.isNotBlank(lastName)) {
			firstName = fcts.normalize(firstName, NormalizationPolicy.UID);
			lastName = fcts.normalize(lastName, NormalizationPolicy.UID);			
		} else {
			throw new AlambicException("Both parameters 'firstName' and 'lastName' must be set");
		}

		if (StringUtils.isNotBlank((String) query.get("format"))) {
			if ( ((String) query.get("format")).equals(RANDOM_UID_FORMAT.SHORT.toString()) ||
				((String) query.get("format")).equals(RANDOM_UID_FORMAT.LONG.toString()) ) {
				format = RANDOM_UID_FORMAT.valueOf((String) query.get("format"));
			} else {
				throw new AlambicException("Not relevant format value '" + format + "'. Supported values are 'SHORT' and 'LONG'");
			}
		}

		return String.format("[%s-%s-%s]", format, firstName, lastName);
	}

}
