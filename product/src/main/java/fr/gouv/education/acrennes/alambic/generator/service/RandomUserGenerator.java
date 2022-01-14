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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class RandomUserGenerator extends AbstractRandomGenerator {

//	private static final Log log = LogFactory.getLog(RandomUserGenerator.class);
	
	private RandomIdentityGenerator rig;
	private RandomAddressGenerator rag;

	public RandomUserGenerator(final EntityManager em) throws AlambicException{
		super(em);
		initialize(em);
	}

	@Override
	public RandomEntity getEntity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		RandomEntity rie = rig.getEntity(query, processId, scope);
		RandomEntity rae = rag.getEntity(query, processId, scope);
		RandomEntity re = join(rie, rae);
		re.setHash(rie.getHash()); // force the resulting random entity hash with the identity one so that pattern to detect already used entity (audit table) is based on firstname & name only (not address).
		return re;
	}

	private void initialize(final EntityManager em) throws AlambicException {
		this.rig = new RandomIdentityGenerator(em);
		this.rag = new RandomAddressGenerator(em);
	}

	@Override
	public GENERATOR_TYPE getType(final Map<String, Object> query) throws AlambicException {
		return RandomGeneratorService.GENERATOR_TYPE.USER;
	}
	
	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		return Math.min(rig.getCapacity(query), rag.getCapacity(query));
	}

	@Override
	public String getCapacityFilter(final Map<String, Object> query) {
		return rig.getQueriedGender(query).toString().toUpperCase();
	}
	
	private RandomEntity join(final RandomEntity ... res) {
		String joinJsonDefinition = "{%s}";
		List<String> jsonDefinitionsList = new ArrayList<>();

		for (RandomEntity re : res) {
			jsonDefinitionsList.add(re.getJson().replaceAll("^\\{|\\}$", ""));
		}

		return new RandomLambdaEntity(String.format(joinJsonDefinition, StringUtils.join(jsonDefinitionsList, ", ")));
	}

}