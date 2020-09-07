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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomAuditEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

public abstract class AbstractRandomGenerator implements RandomGenerator {

	private static final Log log = LogFactory.getLog(AbstractRandomGenerator.class);

	private static final long DEFAULT_RANDOM_GENERATOR_CAPACITY = 1000;
	
	/**
	 * A cache is implemented to prevent observed 'LightWeightLocks' (LWLock of type 'buffer_mapping') on SELECT commands
	 * when getting the actual generator capacity (method call 'getActualCapacity()'). The SELECT commands were blocking 
	 * each other then leading to poor performances when multi-threading was used by jobs.
	 * The cache aims to limit the database accesses to minimum required.
	 */
	private final ObjectMapper mapper;
	protected final EntityManager em;

	public AbstractRandomGenerator(final EntityManager em) throws AlambicException {
		this.mapper = new ObjectMapper();
		this.em = em;
	}
	
	@Override
	public List<RandomEntity> getEntities(final String query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		List<RandomEntity> entitiesList = new ArrayList<>();
		Map<String, Object> queryMap = null;
		String innerProcessId;
		String capacityCacheKey;
		String contentionToken;

		try {
			queryMap = mapper.readValue(query, new TypeReference<Map<String, Object>>() {});
			String blurId = (String) queryMap.get("blurid");
			if (StringUtils.isBlank(blurId)) {
				throw new AlambicException("Mising mandatory parameter 'blur identifier'");
			}
			innerProcessId = (StringUtils.isNotBlank((String) queryMap.get("processId")) ? (String) queryMap.get("processId") : processId);
			capacityCacheKey = getCapacityCacheKey(queryMap, innerProcessId, scope);			
			contentionToken = blurId.concat(getType(queryMap).toString());
			queryMap.put(Constants.RANDOM_GENERATOR_INNER_ITERATION, 1); // allow requested generator to handle increment when needed
		} catch (IOException e) {
			throw new AlambicException("Failed to get random entries (type : " + getType(queryMap) + "), error: " + e.getMessage());
		}

		/**
		 * Synchronize the code block to avoid race condition ONLY when multiple threads request the same generator type and
		 * pass-in parameter the same blur identifier. Otherwise, it is not worth locking.
		 * The couple {generator type, blur identifier value} is the contention key.
		 * This ensures that good performance (the synchronization mechanism pet peeve) are obtained even when a large number of thread are used.
		 */
		WriteLock lock = RandomGeneratorService.getLock(contentionToken).writeLock();
		lock.lock();

		try {
			boolean doReuse = Boolean.parseBoolean((String) queryMap.get("reuse"));
			if (doReuse) {
				String blurId = (String) queryMap.get("blurid");
				if (StringUtils.isNotBlank(blurId)) {
					entitiesList = getFormerEntities(queryMap, innerProcessId, scope);
				} else {
					log.error("Requested to get the same former random user entity but no blurId was passed as parameter");
				}
			}

			// get a random entity whenever either no former entity was requested nor found
			while (entitiesList.size() < (int) queryMap.get("count")) {
				/**
				 * Check the random generator capacity to serve the requested entities count (object: preserve from non finishing loops).
				 */
				if (!Constants.UNLIMITED_GENERATOR_FILTER.equals(getCapacityFilter(queryMap))) {
					long remainingGeneratorCapacity = getActualCapacity(queryMap, innerProcessId, scope);
					long remainingRequestedCount = (int) queryMap.get("count") - entitiesList.size();
					if (remainingGeneratorCapacity < remainingRequestedCount) {
						throw new AlambicException("Failed to get '" + (int) queryMap.get("count") + "' random entry from generator type '" + getType(queryMap) + "'. The generator capacity is exceeded.");
					}
				}

				// Get a random entity
				RandomEntity entity = getEntity(queryMap, innerProcessId, scope);
				
				if (!this.em.getTransaction().isActive()) {
					this.em.getTransaction().begin();
				}
				
				// persist the entity built (within the persistence context) and keep audit record so that the entity can be found in "reuse" context
				if (!isAlreadyUsed(queryMap, entity, innerProcessId, scope)) {
					entitiesList.add(entity);
					persist(entity);
					auditEntity(queryMap, entity, innerProcessId);
					
					if (StringUtils.isNotBlank(capacityCacheKey) && RandomGeneratorService.getCapacityCache().containsKey(capacityCacheKey)) {
						long actual_count = RandomGeneratorService.getCapacityCache().get(capacityCacheKey);
						RandomGeneratorService.getCapacityCache().put(capacityCacheKey, (actual_count + 1));
					} else {
						RandomGeneratorService.getCapacityCache().put(capacityCacheKey, (long) 1);
					}
				}
				
				this.em.getTransaction().commit();

				// increment iteration index
				queryMap.put(Constants.RANDOM_GENERATOR_INNER_ITERATION, (1 + ((int) queryMap.get(Constants.RANDOM_GENERATOR_INNER_ITERATION))));
			}
		} finally {
			lock.unlock();
			RandomGeneratorService.releaseLock(contentionToken);
		}

		return entitiesList;
	}

	public String getCapacityCacheKey(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) {
		String key = null;
		try {
			String capacityFilter = getCapacityFilter(query);
			key = String.format("%s-%s-%s", getType(query), processId, (StringUtils.isNotBlank(capacityFilter)) ? capacityFilter : "NONE");
		} catch (AlambicException e) {
			log.error("Failed to get the generator actual capacity cache key, error : " + e.getMessage());
		}
		return key;
	}

	@Override
	public void persist(final Serializable entity) {
		this.em.persist(entity);
	}
	
	@Override
	public void auditEntity(final Map<String, Object> query, final RandomEntity entity, final String processId) throws AlambicException {
		String blurId = (String) query.get("blurid");
		RandomAuditEntity rae = new RandomAuditEntity(processId, getType(query), entity.getHash(), getCapacityFilter(query), blurId);
		this.em.persist(rae);
	}

	@Override
	public void close() {
		if (null != em) {
			if (this.em.getTransaction().isActive()) {
				this.em.getTransaction().commit();
			}
			this.em.close();
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
				if (StringUtils.isNotBlank(cacheKey) && RandomGeneratorService.getCapacityCache().containsKey(cacheKey)) {
					count = RandomGeneratorService.getCapacityCache().get(cacheKey);
				} else {
					List<String> predicatlist = new ArrayList<>();
					String BASE_QUERY = "SELECT count(rae.id) FROM RandomAuditEntity rae WHERE %s";

					predicatlist.add(String.format("rae.type = '%s'", getType(query)));

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
					RandomGeneratorService.getCapacityCache().put(cacheKey, count);
				}
			}
			capacity = getCapacity(query) - count;
		} catch (AlambicException e) {
			log.error("Failed to get the actual generator capacity, error : " + e.getMessage());
		}

		return capacity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isAlreadyUsed(final Map<String, Object> query, final RandomEntity entity, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		boolean isUsed = true;
		List<RandomEntity> results = Collections.emptyList();

		if (RandomGenerator.UNICITY_SCOPE.PROCESS.equals(scope)) {
			// Check the current entity was already used by this single process id
			Query emQuery = em.createQuery("SELECT rae.id FROM RandomAuditEntity rae WHERE rae.processId = '" + processId + "' AND rae.type = '" + getType(query) + "' AND rae.hash = '" + entity.getHash() + "'");
			emQuery.setMaxResults(1); // For performance constraint. Only interested in the boolean result : exists or not
			results = emQuery.getResultList();
		} else if (RandomGenerator.UNICITY_SCOPE.PROCESS_ALL.equals(scope)) {
			// Check the current entity was already used by any process id
			Query emQuery = em.createQuery("SELECT rae.id FROM RandomAuditEntity rae WHERE rae.type = '" + getType(query) + "' AND rae.hash = '" + entity.getHash() + "'");
			emQuery.setMaxResults(1); // For performance constraint. Only interested in the boolean result : exists or not
			results = emQuery.getResultList();
		} /*
		 * else {
		 * RandomGenerator.UNICITY_SCOPE.NONE : no matter if already used
		 * }
		 */

		if (0 == results.size()) {
			isUsed = false;
		}

		/*
		 * Additional security to resolve possible inconsistencies between table RandomAuditEntity and RandomUserMale/FemaleEntity (due to human operation)
		 */
		if (isUsed) {
			revoke(entity);
		}
		
		return isUsed;
	}

	@Override
	public void revoke(final RandomEntity entity) throws AlambicException {
		// no-op
	}

	@SuppressWarnings("unchecked")
	private List<RandomEntity> getFormerEntities(final Map<String, Object> queryMap, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
		List<RandomEntity> formerEntities = new ArrayList<>();

		String blurId = (String) queryMap.get("blurid");
		String query = "SELECT rae FROM RandomAuditEntity rae WHERE rae.type = '" + getType(queryMap) + "' AND rae.blurId = '" + blurId + "'";
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
		List<RandomAuditEntity>  auditResultSet = emQuery.getResultList();
		if (!auditResultSet.isEmpty()) {
			for (RandomAuditEntity auditEntity : auditResultSet) {
				emQuery = em.createQuery("SELECT entities FROM " + getPersistanceEntityType(queryMap) + " entities WHERE entities.hash = '" + auditEntity.getHash() + "'");
				formerEntities.addAll(emQuery.getResultList());
			}
		}

		return formerEntities;
	}

	protected long getRandomNumber(final long min, final long max) {
		return (long) (Math.random() * ((max - min) + 1) + min);
	}

	@Override
	public String getPersistanceEntityType(final Map<String, Object> query) throws AlambicException {
		return "RandomLambdaEntity";
	}

	@Override
	abstract public RandomEntity getEntity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException;

	@Override
	abstract public GENERATOR_TYPE getType(final Map<String, Object> query) throws AlambicException;

	@Override
	abstract public String getCapacityFilter(final Map<String, Object> query) throws AlambicException;

}