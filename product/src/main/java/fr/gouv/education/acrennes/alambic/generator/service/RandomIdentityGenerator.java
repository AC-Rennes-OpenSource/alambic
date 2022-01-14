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
import javax.persistence.Query;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntityPK;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class RandomIdentityGenerator extends AbstractRandomGenerator {

//	private static final Log log = LogFactory.getLog(RandomIdentityGenerator.class);
	private static String QUERY_TOTAL_COUNT_OF_ITEMS = "SELECT count(rie.primaryKey.id) FROM RandomDictionaryEntity rie WHERE rie.primaryKey.element = :element";

	public enum IDENTITY_GENDER {
		FEMALE,
		MALE
	}

	private long maleFirstNameCount;
	private long femaleFirstNameCount;
	private long lastNameCount;

	public RandomIdentityGenerator(final EntityManager em) throws AlambicException{
		super(em);
		initialize();
	}

	@Override
	public RandomEntity getEntity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		RandomEntity entity;
		
		String randomFirstName;
		String randomLastName;
		long randomDictionaryIndex;
		RandomDictionaryEntity rde;
		RandomDictionaryEntityPK rdepk;

		// Define the queried gender
		IDENTITY_GENDER queriedGender = getQueriedGender(query);

		// Get a random first name
		if (IDENTITY_GENDER.FEMALE.equals(queriedGender)) {
			randomDictionaryIndex = getRandomNumber(1, femaleFirstNameCount);
			rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_FEMALE, randomDictionaryIndex);
		} else {
			randomDictionaryIndex = getRandomNumber(1, maleFirstNameCount);
			rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_MALE, randomDictionaryIndex);
		}
		rde = em.find(RandomDictionaryEntity.class, rdepk);
		randomFirstName = rde.getValue();
		
		// Get a random last name
		randomDictionaryIndex = getRandomNumber(1, lastNameCount);
		rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.LASTNAME, randomDictionaryIndex);
		rde = em.find(RandomDictionaryEntity.class, rdepk);
		randomLastName = rde.getValue();

		entity = new RandomLambdaEntity(String.format("{\"gender\":\"%s\", \"name\":{\"first\":\"%s\", \"last\":\"%s\"}}", queriedGender.toString().toLowerCase(), randomFirstName, randomLastName));
		return entity;
	}

	private void initialize() {
		Query emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_ITEMS);
		emQuery.setParameter("element", RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_FEMALE);		
		this.femaleFirstNameCount = (long) emQuery.getSingleResult();
		
		emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_ITEMS);
		emQuery.setParameter("element", RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_MALE);		
		this.maleFirstNameCount = (long) emQuery.getSingleResult();

		emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_ITEMS);
		emQuery.setParameter("element", RandomDictionaryEntityPK.IDENTITY_ELEMENT.LASTNAME);		
		this.lastNameCount = (long) emQuery.getSingleResult();
	}

	@Override
	public GENERATOR_TYPE getType(final Map<String, Object> query) throws AlambicException {
		return RandomGeneratorService.GENERATOR_TYPE.IDENTITY;
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		long capacity = 0;

		if (getQueriedGender(query).equals(IDENTITY_GENDER.FEMALE)) {
			capacity = femaleFirstNameCount * lastNameCount;
		} else {
			capacity = maleFirstNameCount * lastNameCount;
		}

		return capacity;
	}

	@Override
	public String getCapacityFilter(final Map<String, Object> query) {
		return getQueriedGender(query).toString();
	}
	
	// Define the queried gender (or define randomly one if free)
	protected IDENTITY_GENDER getQueriedGender(final Map<String, Object> query) {
		String gender = (String) query.get("gender");
		return IDENTITY_GENDER.valueOf(gender.toUpperCase());
	}

}