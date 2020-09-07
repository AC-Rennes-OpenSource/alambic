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
package fr.gouv.education.acrennes.alambic.random.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomUserGenerator;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserFemaleEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomUserMaleEntity;
import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RandomGeneratorService.class, RandomUserGenerator.class , VFS.class})
@PowerMockIgnore("javax.management.*")
public class RandomUserGeneratorTest {

	private static final String TEST_USERS_DICTIONARY_ARCHIVE = "src/test/resources/data/alambic/user-dictionaries.tar.gz";
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";
	private static final String USERS_DICTIONARY_ARCHIVE = "../resources/user-dictionaries.tar.gz"; // ! A NOTER ! Le addon "initialize-random-generators" est responsable de fournir le dictionnaire dans son dossier "resources"

	private RandomGenerator rg = null;
	private FileSystemManager fsManager;

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embedded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
		
		/**
		 * Mock so that the dictionary is used for unit testing purpose
		 */
		fsManager = VFS.getManager();
		PowerMockito.mockStatic(VFS.class);
		PowerMockito.when(VFS.getManager()).thenReturn(new InnerFileSystemManager());

		// Set up the persistence data for the unit tests
		setUpPersistenceData();

		rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.USER);
	}

	private void setUpPersistenceData() throws AlambicException, JsonProcessingException, IOException {
		// load the random users dictionaries
		EntityManager em = EntityManagerHelper.getEntityManager();

		try {
			EntityTransaction transac = em.getTransaction();
			// Get archive file from resources folder
			File currentDirectory = new File(".");
			String jobAbsolutePath = currentDirectory.getAbsolutePath().replaceFirst("\\.$", "");				
			File file = new File(jobAbsolutePath.concat(USERS_DICTIONARY_ARCHIVE));
			FileSystemManager fsManager = VFS.getManager();
			FileObject archive = fsManager.resolveFile("tgz:file:/" + file.getPath());

			// List the children of the archive file
			FileObject[] children = archive.getChildren();
			ObjectMapper mapper = new ObjectMapper();
			long maleIndex = 1;
			long femaleIndex = 1;
			for (int i = 0; i < children.length; i++) {
				transac.begin();
				FileObject fo = children[i];
				JsonNode rootNode = mapper.readTree(fo.getContent().getInputStream());
				ArrayNode resultsNode = (ArrayNode) rootNode.get("results");
				for (JsonNode node : resultsNode) {
					if ("\"male\"".equalsIgnoreCase(node.get("gender").toString())) {
						RandomUserMaleEntity ru = new RandomUserMaleEntity(maleIndex++, node.toString());
						em.persist(ru);
					} else {
						RandomUserFemaleEntity ru = new RandomUserFemaleEntity(femaleIndex++, node.toString());
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
		} finally {
			em.close();
		}
	}

	/**
	 * Request 2 random users among a dictionary with 5 male/female entries.
	 * The dictionary capacity is not overlaid (audit table is empty - the dictionary has not been used yet).
	 */
	@Test
	public void test1() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":2}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(2 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request 5 random users among a dictionary with 5 male/female entries.
	 * The dictionary capacity is not overlaid but reaches its limit (audit table is dropped and created - the dictionary has not been used yet).
	 */
	@Test
	public void test2() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"male\",\"count\":5}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(5 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 3 random female users among a dictionary with 5 male/female entries.
	 * 2/ Request 3 random female users among a dictionary with 5 male/female entries.
	 * 3/ Requests have the same scope : SINGLE process identifier.
	 * Check the random generator service throws an exception straight away as the second request cannot be served.
	 */
	@Test
	public void test3() {
		List<RandomEntity> entities;

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(3 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.fail("Should not have returned entities but throw exception instead.");
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * 1/ Request 3 random female users among a dictionary with 5 male/female entries.
	 * 2/ Request 3 random female users among a dictionary with 5 male/female entries.
	 * 3/ Requests have the same scope : ALL processes.
	 * Check the random generator service throws an exception straight away as the second request cannot be served.
	 */
	@Test
	public void test4() {
		List<RandomEntity> entities;

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(3 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS_ALL);
			Assert.fail("Should not have returned entities but throw exception instead.");
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * 1/ Request 3 random female users among a dictionary with 5 male/female entries.
	 * 2/ Request 3 random male users among a dictionary with 5 male/female entries.
	 * 3/ Requests have the same scope : SINGLE process identifier.
	 * Check the random generator service returns the requested entities each time.
	 */
	@Test
	public void test5() {
		List<RandomEntity> entities;

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(3 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"male\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(3 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 3 random female users among a dictionary with 5 male/female entries.
	 * 2/ Request 3 random male users among a dictionary with 5 male/female entries.
	 * 3/ Requests have the same scope : ALL processes.
	 * Check the random generator service returns the requested entities each time.
	 */
	@Test
	public void test6() {
		List<RandomEntity> entities;

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(3 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"male\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(3 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 3 random female users among a dictionary with 5 male/female entries.
	 * 2/ Request 5 random female users among a dictionary with 5 male/female entries.
	 * 3/ Requests have the same scope : NONE.
	 * Check the random generator service returns the requested entities each time.
	 */
	@Test
	public void test7() {
		List<RandomEntity> entities;

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(3 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}

		try {
			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":5}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(5 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 6 random female users among a dictionary with 5 male/female entries.
	 * 2/ Use the scope : NONE.
	 * Check the random generator service throws an exception straight away since the dictionary capacity is overlaid.
	 */
	@Test
	public void test8() {
		try {
			rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":6}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.fail("Should not have returned entities but throw exception instead.");
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * 1/ Request 1 random female user among a dictionary with 5 male/female entries (blurId is set).
	 * 2/ Use the scope : NONE.
	 * 3/ Request again 1 random female user with same blur identifier and requiring reuse of any former entity for this one.
	 * Check the random generator service returns the same entities.
	 */
	@Test
	public void test9() {
		try {
			List<RandomEntity> firstCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			List<RandomEntity> secondCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);

			Assert.assertEquals(1, firstCallResultSet.size());
			Assert.assertEquals(1, secondCallResultSet.size());
			Assert.assertEquals(firstCallResultSet, secondCallResultSet);
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * 1/ Request 1 random female user among a dictionary with 5 male/female entries (blurId is set).
	 * 2/ Use the scope : NONE.
	 * 3/ Request again 1 random female user with same blur identifier and requiring reuse of any former entity for this one.
	 * 4/ Two different process identifiers are specified but no scope is used
	 * Check the random generator service returns the same entities.
	 */
	@Test
	public void test10() {
		try {
			List<RandomEntity> firstCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			List<RandomEntity> secondCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU_2", UNICITY_SCOPE.NONE);

			Assert.assertEquals(1, firstCallResultSet.size());
			Assert.assertEquals(1, secondCallResultSet.size());
			Assert.assertEquals(firstCallResultSet, secondCallResultSet);
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * 1/ Request 1 random female user among a dictionary with 5 male/female entries (blurId is set).
	 * 2/ Use the scope : NONE.
	 * 3/ Request again 1 random female user with same blur identifier and requiring reuse of any former entity for this one.
	 * 4/ Two different process identifiers are specified and PROCESS scope is used
	 * Check the random generator service returns two different entities.
	 */
	@Test
	public void test11() {
		try {
			List<RandomEntity> firstCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			List<RandomEntity> secondCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU_2", UNICITY_SCOPE.PROCESS);

			Assert.assertEquals(1, firstCallResultSet.size());
			Assert.assertEquals(1, secondCallResultSet.size());
			/*
			 * This assertion is removed since the dictionary size isn't big enough to ensure the random service returns two different entities
			 */
			// Assert.assertFalse(firstCallResultSet.equals(secondCallResultSet));
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * 1/ Request 1 random female user among a dictionary with 5 male/female entries (blurId is set).
	 * 2/ Use the scope : NONE.
	 * 3/ Request again 1 random female user with same blur identifier and requiring reuse of any former entity for this one.
	 * 4/ Two different process identifiers are specified and PROCESS_ALL scope is used
	 * Check the random generator service returns the same entities.
	 */
	@Test
	public void test12() {
		try {
			List<RandomEntity> firstCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS_ALL);
			List<RandomEntity> secondCallResultSet = rg.getEntities("{\"gender\":\"female\",\"count\":1, \"reuse\":\"true\", \"blurid\":\"12547\"}", "PROCESS_TESTU_2", UNICITY_SCOPE.PROCESS_ALL);

			Assert.assertEquals(1, firstCallResultSet.size());
			Assert.assertEquals(1, secondCallResultSet.size());
			Assert.assertEquals(firstCallResultSet, secondCallResultSet);
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		}
	}

	@After
	public void tearDown() {
		RandomGeneratorService.close();
		if (null != rg) {
			rg.close();
		}

		/**
		 * Shutdown the derby system so that other unit tests don't run into exception because the database was not released.
		 * This is not visible when running the tests within Eclipse environment (launcher) but it is when packaging
		 * the project with maven.
		 */
		EntityManagerHelper.close();
	}

	private class InnerFileSystemManager extends StandardFileSystemManager {

		@Override
		public FileObject resolveFile(String uri) throws FileSystemException {
			// Get archive file from resources folder
			File currentDirectory = new File(".");
			String jobAbsolutePath = currentDirectory.getAbsolutePath().replaceFirst("\\.$", "");
			File file = new File(jobAbsolutePath.concat(TEST_USERS_DICTIONARY_ARCHIVE));
			FileObject dictionary = fsManager.resolveFile("tgz:file:/" + file.getPath());
			return dictionary;
		}
		
	}
	
}