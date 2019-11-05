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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomAuditEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

public abstract class AbstractRandomGenerator implements RandomGenerator {

	private static final Log log = LogFactory.getLog(AbstractRandomGenerator.class);

	protected static final long DEFAULT_RANDOM_GENERATOR_CAPACITY = 1000;
	/**
	 * A cache is implemented to prevent observed 'LightWeightLocks' (LWLock of type 'buffer_mapping') on SELECT commands
	 * when getting the actual generator capacity (method call 'getActualCapacity()'). The SELECT commands were blocking 
	 * each other then leading to poor performances when multi-threading was used by jobs.
	 * The cache aims to limit the database accesses to minimum required.
	 */
	protected static ConcurrentHashMap<String, Long> capacity_cache = new ConcurrentHashMap<>(20);
	protected ObjectMapper mapper;
	protected EntityManager em;

	public AbstractRandomGenerator(final EntityManager em) throws AlambicException {
		mapper = new ObjectMapper();
		this.em = em;
	}

	@Override
	public List<RandomEntity> getEntities(final String query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		List<RandomEntity> list = new ArrayList<>();

		try {
			EntityTransaction transac = em.getTransaction();

			Map<String, Object> queryMap = mapper.readValue(query, new TypeReference<Map<String, Object>>() {});
			queryMap.put(Constants.RANDOM_GENERATOR_INNER_ITERATION, 1); // allow requested generator to handle increment when needed
			String innerProcessId = (StringUtils.isNotBlank((String) queryMap.get("processId")) ? (String) queryMap.get("processId") : processId);
			String capacityCacheKey = getCapacityCacheKey(queryMap, processId, scope);

			boolean doReuse = Boolean.parseBoolean((String) queryMap.get("reuse"));
			if (doReuse) {
				String blurId = (String) queryMap.get("blurid");
				if (StringUtils.isNotBlank(blurId)) {
					list = getFormerEntities(queryMap, innerProcessId, scope);
				} else {
					log.error("Requested to get the same former random user entity but no blurId was passed as parameter");
				}
			}

			// get a random entity whenever either no former entity was requested nor found
			while (list.size() < (int) queryMap.get("count")) {
				/**
				 * Check the random generator capacity to serve the requested entities count.
				 * Object: preserve from non finishing loops.
				 */
				if (!Constants.UNLIMITED_GENERATOR_FILTER.equals(getCapacityFilter(queryMap))) {
					long remainingGeneratorCapacity = getActualCapacity(queryMap, innerProcessId, scope);
					long remainingRequestedCount = (int) queryMap.get("count") - list.size();
					if (remainingGeneratorCapacity < remainingRequestedCount) {
						throw new AlambicException("Failed to get '" + (int) queryMap.get("count") + "' random entry from generator type '" + getType() + "'. The generator capacity is exceeded.");
					}
				}

				RandomEntity entity = getEntity(queryMap, processId, scope);
				if (!isAlreadyUSed(entity, innerProcessId, scope)) {
					list.add(entity);
					transac.begin();
					persist(entity);
					auditEntity(queryMap, entity, innerProcessId);
					transac.commit();
					if (StringUtils.isNotBlank(capacityCacheKey) && capacity_cache.containsKey(capacityCacheKey)) {
						long actual_count = capacity_cache.get(capacityCacheKey);
						capacity_cache.put(capacityCacheKey, (actual_count + 1));
					} else {
						capacity_cache.put(capacityCacheKey, (long) 1);
					}
				}
				
				// increment iteration index
				queryMap.put(Constants.RANDOM_GENERATOR_INNER_ITERATION, (1 + ((int) queryMap.get(Constants.RANDOM_GENERATOR_INNER_ITERATION))));
			}
		} catch (IOException e) {
			log.error("Failed to get random entries, error: " + e.getMessage());
		}

		return list;
	}

	public String getCapacityCacheKey(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) {
		String key = null;
		try {
			String capacityFilter = getCapacityFilter(query);
			key = String.format("%s-%s-%s", getType(), processId, (StringUtils.isNotBlank(capacityFilter)) ? capacityFilter : "NONE");
		} catch (AlambicException e) {
			log.error("Failed to get the generator actual capacity cache key, error : " + e.getMessage());
		}
		return key;
	}

	protected void auditEntity(final Map<String, Object> query, final RandomEntity entity, final String processId) throws AlambicException {
		String blurId = (String) query.get("blurid");
		RandomAuditEntity rae = new RandomAuditEntity(processId, getType(), entity.getHash(), getCapacityFilter(query), blurId);
		em.persist(rae);
	}

	@Override
	public void close() {
		if (null != em) {
			em.close();
			em = null;
		}
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		return DEFAULT_RANDOM_GENERATOR_CAPACITY;
	}

	private long getActualCapacity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) {
		long count = 0;
		long capacity = 0;

		try {			
			if (!RandomGenerator.UNICITY_SCOPE.NONE.equals(scope)) {
				String cacheKey = getCapacityCacheKey(query, processId, scope);
				if (StringUtils.isNotBlank(cacheKey) && capacity_cache.containsKey(cacheKey)) {
					count = capacity_cache.get(cacheKey);
				} else {
					List<String> predicatlist = new ArrayList<>();
					String BASE_QUERY = "SELECT count(rae.id) FROM RandomAuditEntity rae WHERE %s";
					
					predicatlist.add(String.format("rae.type = '%s'", getType()));
					
					if (RandomGenerator.UNICITY_SCOPE.PROCESS.equals(scope)) {
						// Count the already used entities by this single process id
						predicatlist.add(String.format("rae.processId = '%s'", processId));
					} // else RandomGenerator.UNICITY_SCOPE.PROCESS_ALL : no process predicate as the process id doesn't matter.
					
					String capacityFilter = getCapacityFilter(query);
					if (StringUtils.isNotBlank(capacityFilter)) {
						predicatlist.add(String.format("rae.capacityFilter = '%s'", capacityFilter));
					}
					
					Query emQuery = em.createQuery(String.format(BASE_QUERY, StringUtils.join(predicatlist, " AND ")));					
					count = (Long) emQuery.getSingleResult();
					capacity_cache.put(cacheKey, count);
				}
			}
			capacity = getCapacity(query) - count;
		} catch (AlambicException e) {
			log.error("Failed to get the actual generator capacity, error : " + e.getMessage());
		}

		return capacity;
	}

	@SuppressWarnings("unchecked")
	private boolean isAlreadyUSed(final RandomEntity entity, final String processId, final UNICITY_SCOPE scope) {
		boolean isUsed = true;
		List<RandomEntity> results = Collections.emptyList();

		if (RandomGenerator.UNICITY_SCOPE.PROCESS.equals(scope)) {
			// Check the current entity was already used by this single process id
			Query emQuery = em.createQuery("SELECT rae FROM RandomAuditEntity rae WHERE rae.processId = '" + processId + "' AND rae.type = '" + getType() + "' AND rae.hash = '" + entity.getHash() + "'");
			results = emQuery.getResultList();
		} else if (RandomGenerator.UNICITY_SCOPE.PROCESS_ALL.equals(scope)) {
			// Check the current entity was already used by any process id
			Query emQuery = em.createQuery("SELECT rae FROM RandomAuditEntity rae WHERE rae.type = '" + getType() + "' AND rae.hash = '" + entity.getHash() + "'");
			results = emQuery.getResultList();
		} /*
		 * else {
		 * RandomGenerator.UNICITY_SCOPE.NONE : no matter if already used
		 * }
		 */

		if (0 == results.size()) {
			isUsed = false;
		}

		return isUsed;
	}

	@SuppressWarnings("unchecked")
	private List<RandomEntity> getFormerEntities(final Map<String, Object> queryMap, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		List<RandomEntity> results = new ArrayList<>();
		List<RandomAuditEntity> auditResultSet;

		String blurId = (String) queryMap.get("blurid");
		String query = "SELECT rae FROM RandomAuditEntity rae WHERE rae.type = '" + getType() + "' AND rae.blurId = '" + blurId + "'";
		if (RandomGenerator.UNICITY_SCOPE.PROCESS.equals(scope)) {
			// Check the current entity was already used by this single process id
			query = query.concat(" AND rae.processId = '" + processId + "'");
		}

		String capacityFilter = getCapacityFilter(queryMap);
		if (StringUtils.isNotBlank(capacityFilter)) {
			// Focus on same query parameters (defined via capacity filter)
			query = query.concat(" AND rae.capacityFilter = '" + capacityFilter + "'");
		}

		Query emQuery = em.createQuery(query);
		auditResultSet = emQuery.getResultList();
		if (!auditResultSet.isEmpty()) {
			for (RandomAuditEntity auditEntity : auditResultSet) {
				emQuery = em.createQuery("SELECT entities FROM " + getPersistanceEntityType() + " entities WHERE entities.hash = '" + auditEntity.getHash() + "'");
				results.addAll(emQuery.getResultList());
			}
		}

		return results;
	}

	protected long getRandomNumber(final long min, final long max) {
		return (long) (Math.random() * ((max - min) + 1) + min);
	}

	@Override
	public String getPersistanceEntityType() {
		return "RandomLambdaEntity";
	}

	@Override
	public void persist(RandomEntity entity) {
		em.persist(entity);
	}
	
	/* For unit tests purpose only as test are based on principle of clean context each time a test is run */
	public static void cleanCapacityCache() {
		capacity_cache.clear();
	}
	
	@Override
	abstract public RandomEntity getEntity(Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException;

	@Override
	abstract public String getType();

	@Override
	abstract public String getCapacityFilter(final Map<String, Object> query) throws AlambicException;

}
