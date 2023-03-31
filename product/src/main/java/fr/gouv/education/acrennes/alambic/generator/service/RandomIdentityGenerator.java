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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntityPK;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Map;
import java.util.Random;

public class RandomIdentityGenerator extends AbstractRandomGenerator {
	private static final String QUERY_TOTAL_COUNT_OF_ITEMS = "SELECT count(rie.primaryKey.id) FROM RandomDictionaryEntity rie WHERE rie.primaryKey.elementname = :elementname";

	public enum IDENTITY_GENDER {
		FEMALE("FEMALE"),
		MALE("MALE"),
		RANDOM("");

		private final String capacityFilter;

		IDENTITY_GENDER(String capacityFilter) {
			this.capacityFilter = capacityFilter;
		}

		public static IDENTITY_GENDER getRandomGender() {
			if (rand.nextInt(2) == 0) {
				return MALE;
			} else {
				return FEMALE;
			}
		}

		public String getCapacityFilter() {
			return capacityFilter;
		}
	}

	private long maleFirstNameCount;
	private long femaleFirstNameCount;
	private long lastNameCount;
	private static final Random rand = new Random();

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

		// Define the queried gender and handle the random case
		IDENTITY_GENDER chosenGender = getQueriedGender(query);
		if (IDENTITY_GENDER.RANDOM.equals(chosenGender)) {
			chosenGender = IDENTITY_GENDER.getRandomGender();
		}

		// Get a random first name
		if (IDENTITY_GENDER.FEMALE.equals(chosenGender)) {
			randomDictionaryIndex = getRandomNumber(1, femaleFirstNameCount);
			rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_FEMALE, randomDictionaryIndex);
		} else {
			randomDictionaryIndex = getRandomNumber(1, maleFirstNameCount);
			rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_MALE, randomDictionaryIndex);
		}
		rde = em.find(RandomDictionaryEntity.class, rdepk);
		randomFirstName = rde.getElementvalue();
		
		// Get a random last name
		randomDictionaryIndex = getRandomNumber(1, lastNameCount);
		rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.LASTNAME, randomDictionaryIndex);
		rde = em.find(RandomDictionaryEntity.class, rdepk);
		randomLastName = rde.getElementvalue();

		entity = new RandomLambdaEntity(String.format("{\"gender\":\"%s\", \"name\":{\"first\":\"%s\", \"last\":\"%s\"}}", chosenGender.toString().toLowerCase(), randomFirstName, randomLastName));
		return entity;
	}

	private void initialize() {
		this.femaleFirstNameCount = countElement(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_FEMALE);
		this.maleFirstNameCount = countElement(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_MALE);
		this.lastNameCount = countElement(RandomDictionaryEntityPK.IDENTITY_ELEMENT.LASTNAME);

	}

	private long countElement(RandomDictionaryEntityPK.IDENTITY_ELEMENT element) {
		Query emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_ITEMS);
		emQuery.setParameter("elementname", element);
		return (long) emQuery.getSingleResult();
	}

	@Override
	public GENERATOR_TYPE getType(final Map<String, Object> query) throws AlambicException {
		return RandomGeneratorService.GENERATOR_TYPE.IDENTITY;
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		long capacity = 0;

		switch (getQueriedGender(query)) {
			case MALE:
				capacity = maleFirstNameCount * lastNameCount;
				break;
			case FEMALE:
				capacity = femaleFirstNameCount * lastNameCount;
				break;
			case RANDOM:
				capacity = Math.min(maleFirstNameCount, femaleFirstNameCount) * lastNameCount;
				break;
		}

		return capacity;
	}

	@Override
	public String getCapacityFilter(final Map<String, Object> query) {
		return getQueriedGender(query).getCapacityFilter();
	}
	
	// Define the queried gender (or define randomly one if free)
	protected IDENTITY_GENDER getQueriedGender(final Map<String, Object> query) {
		String gender = (String) query.get("gender");
		return IDENTITY_GENDER.valueOf(gender.toUpperCase());
	}

}