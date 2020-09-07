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
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class RandomUAIGenerator extends AbstractRandomGenerator {

//	private static final Log log = LogFactory.getLog(RandomUAIGenerator.class);
	private static final String DEFAULT_UAI_ROOT = "035";
	private static final int UAI_DIGIT_LENGTH = 7;

	public RandomUAIGenerator(final EntityManager em) throws AlambicException {
		super(em);
	}

	@Override
	public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope) throws AlambicException {
		RandomEntity entity;
		
		String root = (String) query.getOrDefault("root", DEFAULT_UAI_ROOT);
		int randomRange = UAI_DIGIT_LENGTH - root.length();
		int maxValue = Integer.parseUnsignedInt(StringUtils.rightPad("9", randomRange, "9"));
		int randomNumeric = (int) (Math.round(1 + (Math.random() * (maxValue - 1))));
		String randomEndingCharacter =  RandomStringUtils.randomAlphabetic(1);
		String uai = String.format("%s%0" + randomRange + "d%s", root, randomNumeric, randomEndingCharacter).toUpperCase();
		entity = new RandomLambdaEntity("{\"uai\":\"" + uai + "\"}");
		return entity;
	}

	@Override
	public RandomGeneratorService.GENERATOR_TYPE getType(final Map<String, Object> query) {
		return RandomGeneratorService.GENERATOR_TYPE.UAI;
	}

	@Override
	public String getCapacityFilter(Map<String, Object> query) throws AlambicException {
		return String.format("[%s]", (String) query.getOrDefault("root", DEFAULT_UAI_ROOT));
	}
	
	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		String root = (String) query.getOrDefault("root", DEFAULT_UAI_ROOT);
		return (long) (Math.pow(10, root.trim().length()) * 26);
	}

}
