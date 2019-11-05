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
package fr.gouv.education.acrennes.alambic.jobs.extract.clients;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

public class RandomClientToStateBase implements IToStateBase {

	private static final Log log = LogFactory.getLog(RandomClientToStateBase.class);

	private List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private RandomGenerator randomGenerator;
	private final String processId;

	public RandomClientToStateBase(final String defaultProcessId, final GENERATOR_TYPE type) throws AlambicException {
		this.randomGenerator = RandomGeneratorService.getInstance().getRandomGenerator(type);
		this.processId = defaultProcessId;
	}

	@Override
	public void executeQuery(final String query) {
		executeQuery(query, null);
	}

	@Override
	public void executeQuery(final String query, final String scope) {
		stateBase = new ArrayList<>();
		if (StringUtils.isNotBlank(query)) {
			try {
				List<RandomEntity> entities = this.randomGenerator.getEntities(query, this.processId, UNICITY_SCOPE.valueOf(scope));
				for (RandomEntity entity : entities) {
					this.stateBase.add(RandomGeneratorService.getInstance().toStateBaseEntry(entity));
				}
			} catch (AlambicException e) {
				log.error("Failed to execute the query '" + query + "', error: " + e.getMessage());
			}
		} else {
			log.error("Empty query is not allowed");
			// ignore
		}
	}

	@Override
	public List<Map<String, List<String>>> getStateBase() {
		return this.stateBase;
	}

	@Override
	public int getCountResults() {
		return this.stateBase.size();
	}

	@Override
	public void close() {
		if (null != this.randomGenerator) {
			this.randomGenerator.close();
			this.randomGenerator = null;
		}
	}

	@Override
	public void clear() {
		this.stateBase.clear();
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy)
			throws AlambicException {
		throw new AlambicException("Not supported operation");
	}

}
