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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserEntityPK;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserEntityPK.USER_GENDER;

public class RandomUserGenerator extends AbstractRandomGenerator {

	private static final Log log = LogFactory.getLog(RandomUserGenerator.class);
	private static String QUERY_TOTAL_COUNT = "SELECT count(rue.primaryKey.id) FROM RandomUserEntity rue";
	private static String QUERY_TOTAL_COUNT_OF_GENDER = "SELECT count(rue.primaryKey.id) FROM RandomUserEntity rue WHERE rue.primaryKey.gender = :gender";
	private static String USERS_DICTIONARY_ARCHIVE = "../resources/user-dictionaries.tar.gz"; // ! A NOTER ! Le addon "initialize-random-generators" est responsable de fournir le dictionnaire dans son dossier "resources"

	private long maleCount;
	private long femaleCount;

	public RandomUserGenerator(final EntityManager em) throws AlambicException{
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
		RandomUserEntity resultSet = null;

		// Define the queried gender (or define randomly one if free)
		String queriedGender = (String) query.get("gender");
		USER_GENDER electedGender = USER_GENDER.FEMALE;
		long modulus = getRandomNumber(0, 1) % 2;
		if ((StringUtils.isBlank(queriedGender) && 0 != modulus) || (StringUtils.isNotBlank(queriedGender) && "male".equals(queriedGender))) {
			electedGender = USER_GENDER.MALE;
		}

		if (RandomGenerator.UNICITY_SCOPE.NONE.equals(scope)) {
			// no matter if already used: get a random entity among the whole dictionary
			long randomDictionaryIndex = (USER_GENDER.MALE.equals(electedGender)) ? getRandomNumber(0, maleCount - 1) : getRandomNumber(0, femaleCount - 1);
			RandomUserEntityPK rupk = new RandomUserEntityPK(electedGender, randomDictionaryIndex);
			resultSet = em.find(RandomUserEntity.class, rupk);
		} else {
			String BASE_COLLECTION_QUERY = "SELECT rue.primaryKey.id FROM RandomUserEntity rue WHERE %s AND NOT EXISTS (SELECT rae.hash FROM RandomAuditEntity rae WHERE rae.type = 'USER' AND rae.hash = rue.hash %s)";
			List<Long> results = Collections.emptyList();
			List<String> predicatlist = new ArrayList<>();

			if (RandomGenerator.UNICITY_SCOPE.PROCESS.equals(scope)) {
				predicatlist.add(String.format("AND rae.processId = '%s'", processId));
			} /* else scope is RandomGenerator.UNICITY_SCOPE.PROCESS_ALL : no predicate since no process restriction is suitable */

			/*
			 * Get a limited list of unused entities restricted to the process.
			 * ORDER BY clause is required so that index scan is preferred within SQL execution plan instead of sequential scan.
			 */
			Query emQuery = em.createQuery(String.format(BASE_COLLECTION_QUERY, "rue.primaryKey.gender = :gender", StringUtils.join(predicatlist, " AND ")));
			emQuery.setParameter("gender", electedGender);
			emQuery.setMaxResults((int) DEFAULT_RANDOM_GENERATOR_CAPACITY); // For performance constraint
			results = emQuery.getResultList();

			// Get a random entity among the list
			long randomDictionaryIndex = getRandomNumber(0, results.size() - 1);
			RandomUserEntityPK rupk = new RandomUserEntityPK(electedGender, results.get((int) randomDictionaryIndex));
			resultSet = em.find(RandomUserEntity.class, rupk);
		}

		return resultSet;
	}

	private void initialize() {
		/**
		 * Initialize the database with dictionaries if not already done
		 */
		Query emQuery = em.createQuery(QUERY_TOTAL_COUNT);
		long count = (Long) emQuery.getSingleResult();
		if (0 == count) {
			EntityTransaction transac = em.getTransaction();

			// load the random users dictionaries
			try {
				// Get archive file from resources folder
				File currentDirectory = new File(".");
				String jobAbsolutePath = currentDirectory.getAbsolutePath().replaceFirst("\\.$", "");				
				File file = new File(jobAbsolutePath.concat(USERS_DICTIONARY_ARCHIVE));
				FileSystemManager fsManager = VFS.getManager();
				FileObject archive = fsManager.resolveFile("tgz:file:/" + file.getPath());
				
				// List the children of the archive file
				FileObject[] children = archive.getChildren();
				ObjectMapper mapper = new ObjectMapper();
				int maleIndex = 0;
				int femaleIndex = 0;
				for (int i = 0; i < children.length; i++) {
					transac.begin();
					FileObject fo = children[i];
					log.info("Load the dictionary file: " + fo.getName().getBaseName());
					JsonNode rootNode = mapper.readTree(fo.getContent().getInputStream());
					ArrayNode resultsNode = (ArrayNode) rootNode.get("results");
					for (JsonNode node : resultsNode) {
						if ("\"male\"".equalsIgnoreCase(node.get("gender").toString())) {
							RandomUserEntityPK rupk = new RandomUserEntityPK(USER_GENDER.MALE, maleIndex++);
							RandomUserEntity ru = new RandomUserEntity(rupk, node.toString());
							em.persist(ru);
						} else {
							RandomUserEntityPK rupk = new RandomUserEntityPK(USER_GENDER.FEMALE, femaleIndex++);
							RandomUserEntity ru = new RandomUserEntity(rupk, node.toString());
							em.persist(ru);
						}
					}
					/**
					 * Add flush and clear methods so that persistence entities objects are made available
					 * for garbage collection.
					 * (see : http://www.eclipse.org/eclipselink/documentation/2.4/jpa/extensions/p_persistence_context_referencemode.htm) 
					 */
					em.flush();
					em.clear();
					transac.commit();
				}
			} catch (IOException e) {
				log.error("Failed to initialize the random service, error:" + e.getMessage());
			}
		}

		emQuery = em.createQuery(QUERY_TOTAL_COUNT_OF_GENDER);
		emQuery.setParameter("gender", USER_GENDER.FEMALE);
		femaleCount = (long) emQuery.getSingleResult();

		emQuery.setParameter("gender", USER_GENDER.MALE);
		maleCount = (long) emQuery.getSingleResult();
	}

	@Override
	public String getType() {
		return RandomGeneratorService.GENERATOR_TYPE.USER.toString();
	}

	@Override
	public long getCapacity(final Map<String, Object> query) throws AlambicException {
		long capacity = maleCount;

		String gender = (String) query.get("gender");
		if ("female".equalsIgnoreCase(gender)) {
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
	public String getPersistanceEntityType() {
		return "RandomUserEntity";
	}

}
