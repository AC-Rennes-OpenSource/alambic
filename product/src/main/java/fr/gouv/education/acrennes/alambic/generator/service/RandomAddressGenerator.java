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

public class RandomAddressGenerator extends AbstractRandomGenerator {

//	private static final Log log = LogFactory.getLog(RandomAddressGenerator.class);
	private static String QUERY_TOTAL_COUNT_OF_ITEMS = "SELECT count(rie.primaryKey.id) FROM RandomDictionaryEntity rie WHERE rie.primaryKey.elementname = :elementname";

	private long addressTypeCount;
	private long addressLabelCount;
	private long addressCityCount;

	public RandomAddressGenerator(final EntityManager em) throws AlambicException{
		super(em);
		initialize();
	}

	@Override
	public RandomEntity getEntity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		RandomEntity entity;
		
		String randomAddressType;
		String randomAddressLabel;
		String randomAddressCity;
		long randomAddressNumber;
		long randomAddressPostCode;
		long randomDictionaryIndex;
		RandomDictionaryEntity rde;
		RandomDictionaryEntityPK rdepk;

		// Get a random address number
		randomAddressNumber = getRandomNumber(1, 1000);

		// Get a random address type
		randomDictionaryIndex = getRandomNumber(1, addressTypeCount);
		rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_TYPE, randomDictionaryIndex);
		rde = em.find(RandomDictionaryEntity.class, rdepk);
		randomAddressType = rde.getElementvalue();
		
		// Get a random address label
		randomDictionaryIndex = getRandomNumber(1, addressLabelCount);
		rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_LABEL, randomDictionaryIndex);
		rde = em.find(RandomDictionaryEntity.class, rdepk);
		randomAddressLabel = rde.getElementvalue();
		
		// Get a random address city
		randomDictionaryIndex = getRandomNumber(1, addressCityCount);
		rdepk = new RandomDictionaryEntityPK(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_CITY, randomDictionaryIndex);
		rde = em.find(RandomDictionaryEntity.class, rdepk);
		randomAddressCity = rde.getElementvalue();

		// Get a random postcode
		randomAddressPostCode = getRandomNumber(1, 99999);
		
		entity = new RandomLambdaEntity(String.format("{\"location\":{\"street\":\"%s %s %s\", \"city\":\"%s\", \"postcode\":%d}}", randomAddressNumber, randomAddressType, randomAddressLabel, randomAddressCity, randomAddressPostCode));
		return entity;
	}

	private void initialize() {
		Query emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_ITEMS);
		emQuery.setParameter("elementname", RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_TYPE);		
		this.addressTypeCount = (long) emQuery.getSingleResult();
		
		emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_ITEMS);
		emQuery.setParameter("elementname", RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_LABEL);		
		this.addressLabelCount = (long) emQuery.getSingleResult();

		emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_ITEMS);
		emQuery.setParameter("elementname", RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_CITY);		
		this.addressCityCount = (long) emQuery.getSingleResult();
	}

	@Override
	public GENERATOR_TYPE getType(final Map<String, Object> query) throws AlambicException {
		return RandomGeneratorService.GENERATOR_TYPE.ADDRESS;
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		return (this.addressCityCount * this.addressLabelCount * this.addressTypeCount * 1000 * 99999);
	}

	@Override
	public String getCapacityFilter(final Map<String, Object> query) {
		return "NONE";
	}
	
}
