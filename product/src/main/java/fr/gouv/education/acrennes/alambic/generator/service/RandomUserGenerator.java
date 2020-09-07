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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserFemaleEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserMaleEntity;

public class RandomUserGenerator extends AbstractRandomGenerator {

	private static final Log log = LogFactory.getLog(RandomUserGenerator.class);
	private static String QUERY_TOTAL_COUNT_OF_MALE_GENDER = "SELECT count(rue.id) FROM RandomUserMaleEntity rue";
	private static String QUERY_TOTAL_COUNT_OF_FEMALE_GENDER = "SELECT count(rue.id) FROM RandomUserFemaleEntity rue";

	public enum USER_GENDER {
		FEMALE,
		MALE
	}

	private long maleCount;
	private long femaleCount;

	public RandomUserGenerator(final EntityManager em) throws AlambicException {
		super(em);
		initialize();
	}

	/**
	 * In order to enhance the script performance, get not already used list of entities.
	 * This is preferred to getting a random and then detect it is already used (and look for another one and so forth...)
	 * This control is possible since each random user entity (the dictionary) is persisted in database.
	 * Obviously, this is relevant only if the unicity scope is not none.
	 * For performance constraint also, the number of results returned to build the entity list is limited.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public RandomEntity getEntity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		RandomEntity resultSet = null;
		long randomDictionaryIndex;

		// Define the queried gender (or define randomly one if free)
		USER_GENDER queriedGender = getQueriedGender(query);

		if (RandomGenerator.UNICITY_SCOPE.NONE.equals(scope)) {
			// no matter if already used: get a random entity among the whole dictionary
			randomDictionaryIndex = (USER_GENDER.MALE.equals(queriedGender)) ? getRandomNumber(1, maleCount) : getRandomNumber(1, femaleCount);
		} else {
			String BASE_COLLECTION_QUERY = "SELECT rue.id FROM " + getPersistanceEntityType(query) + " rue WHERE rue.is_available = true";
			List<Long> results = Collections.emptyList();

			/*
			 * Get a limited list of unused entities restricted to the process.
			 */
			Query emQuery = em.createQuery(BASE_COLLECTION_QUERY);
			emQuery.setMaxResults(100); // For performance constraint
			results = emQuery.getResultList();

			// Get a random entity among the list
			randomDictionaryIndex = getRandomNumber(0, results.size() - 1);
			randomDictionaryIndex = results.get((int) randomDictionaryIndex);
		}

		if (USER_GENDER.FEMALE.equals(queriedGender)) {
			resultSet = (RandomEntity) em.find(RandomUserFemaleEntity.class, randomDictionaryIndex);
		} else {
			resultSet = (RandomEntity) em.find(RandomUserMaleEntity.class, randomDictionaryIndex);
		}

		return resultSet;
	}

	private void initialize() {
		Query emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_FEMALE_GENDER);
		femaleCount = (long) emQuery.getSingleResult();
		
		emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_MALE_GENDER);
		maleCount = (long) emQuery.getSingleResult();
	}

	@Override
	public RandomGeneratorService.GENERATOR_TYPE getType(final Map<String, Object> query) throws AlambicException {
		String gender = getQueriedGender(query).toString().toUpperCase();
		return RandomGeneratorService.GENERATOR_TYPE.valueOf("USER_".concat(gender));
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		long capacity = maleCount;

		if (getQueriedGender(query).equals(USER_GENDER.FEMALE)) {
			capacity = femaleCount;
		}

		return capacity;
	}

	@Override
	public String getCapacityFilter(final Map<String, Object> query) {
		String filter = "NO_GENDER";

		String gender = (String) query.get("gender");
		if (StringUtils.isNotBlank(gender)) {
			filter = USER_GENDER.valueOf(gender.toUpperCase()).toString();
		}

		return filter;
	}
	
	@Override
	public String getPersistanceEntityType(final Map<String, Object> query) throws AlambicException {
		String gender = getQueriedGender(query).toString().toLowerCase();
		return String.format("RandomUser%sEntity", gender.substring(0,  1).toUpperCase().concat(gender.substring(1)));
	}
	
	@Override
	public void persist(final Serializable entity) {
		((RandomUserEntity) entity).setIs_available(false);
		this.em.persist(entity);
	}

	@Override
	public void revoke(final RandomEntity entity) throws AlambicException {
		log.warn("Revoked the user entity with hash '" + entity.getHash() + "'");
		((RandomUserEntity) entity).setIs_available(false);
		this.em.persist(entity);
	}

	// Define the queried gender (or define randomly one if free)
	private USER_GENDER getQueriedGender(final Map<String, Object> query) throws AlambicException {
		USER_GENDER queriedGender = USER_GENDER.FEMALE;
		String gender = (String) query.get("gender");
		if (StringUtils.isNotBlank(gender)) {
			if ("male".equals(gender)) {
				queriedGender = USER_GENDER.MALE;
			}
		} else {
			throw new AlambicException("Missing gender parameter within the request : '" + query.entrySet().stream()
					.map(entry -> entry.getKey() + ":" + (String) entry.getValue())
					.collect(Collectors.joining(", ")) + "'");
		}
		return queriedGender;
	}

}