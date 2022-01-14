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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.junit.After;
import org.junit.Assert;
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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomUserGenerator;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomDictionaryEntityPK;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RandomGeneratorService.class, RandomUserGenerator.class , VFS.class})
@PowerMockIgnore("javax.management.*")
public class RandomUserGeneratorTest {

	private static final String TEST_USERS_DICTIONARY_ARCHIVE = "src/test/resources/data/alambic/user-dictionaries-testu.tar.gz";
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";
	private static final String USERS_DICTIONARY_ARCHIVE = "../resources/user-dictionaries.tar.gz";

	private RandomGenerator rg = null;
	private FileSystemManager fsManager;
	
	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embedded persistence unit (h2) is used
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
			
			Map<String, Long> idmap = new HashMap<>();
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_MALE.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.FIRSTNAME_FEMALE.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.LASTNAME.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_TYPE.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_LABEL.toString(), (long) 1);
			idmap.put(RandomDictionaryEntityPK.IDENTITY_ELEMENT.ADDRESS_CITY.toString(), (long) 1);
			for (int i = 0; i < children.length; i++) {
				transac.begin();
				FileObject fo = children[i];
				JsonNode rootNode = mapper.readTree(fo.getContent().getInputStream());
				ArrayNode dictionaryNode = (ArrayNode) rootNode.get("dictionary");
				for (JsonNode node : dictionaryNode) {
					RandomDictionaryEntityPK.IDENTITY_ELEMENT element = RandomDictionaryEntityPK.IDENTITY_ELEMENT.valueOf(node.get("element").textValue().toUpperCase());
					Long index = idmap.get(element.toString());
					RandomDictionaryEntityPK pk = new RandomDictionaryEntityPK(element, index);
					RandomDictionaryEntity rie = new RandomDictionaryEntity(pk, node.get("value").textValue());
					em.persist(rie);
					idmap.put(element.toString(), ++index);
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
	 * Request 2 random female identities among a dictionary with 9 female entries.
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
	 * Request 9 random female identities among a dictionary with 9 female entries.
	 * The dictionary capacity is not overlaid but reaches its limit (audit table is dropped and created - the dictionary has not been used yet).
	 */
	@Test
	public void test2() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":9}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(9 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request 10 random female identities among a dictionary with 9 female entries.
	 * The dictionary capacity is overlaid => un exception should be raised.
	 */
	@Test
	public void test3() {
		try {
			rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":10}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.fail("Should not have returned entities but throw exception instead.");
		} catch (AlambicException e) {
			Assert.assertTrue(e instanceof AlambicException);
		}
	}

	/**
	 * 1/ Request 7 random female identities among a dictionary with 9 female entries.
	 * 2/ Request 4 random female identities among a dictionary with 9 female entries.
	 * 3/ Requests have the same scope : SINGLE process identifier.
	 * Check the random generator service throws an exception straight away as the second request cannot be served.
	 */
	@Test
	public void test4() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":7}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(7 == entities.size());

			rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":4}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.fail("un exception should be raised for overlaid capacity reason.");
		} catch (AlambicException e) {
			Assert.assertTrue(e instanceof AlambicException);
		}
	}

	/**
	 * 1/ Request 7 random female identities among a dictionary with 9 female entries.
	 * 2/ Request 4 random female identities among a dictionary with 9 female entries.
	 * 3/ Requests have two different scopes : SINGLE process identifier.
	 * Check the random generator services the two requests.
	 */
	@Test
	public void test5() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":7}", "PROCESS_TESTU_A", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(7 == entities.size());

			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":4}", "PROCESS_TESTU_B", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 7 random female identities among a dictionary with 9 female entries.
	 * 2/ Request 4 random female identities among a dictionary with 9 female entries.
	 * 3/ Requests have the same scope : ALL (unicity is required over all processes).
	 * Check the random generator service throws an exception straight away as the second request cannot be served.
	 */
	@Test
	public void test6() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":7}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(7 == entities.size());

			rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":4}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS_ALL);
			Assert.fail("un exception should be raised for overlaid capacity reason.");
		} catch (AlambicException e) {
			Assert.assertTrue(e instanceof AlambicException);
		}
	}

	/**
	 * 1/ Request 7 random female identities among a dictionary with 9 female entries.
	 * 2/ Request 4 random female identities among a dictionary with 9 female entries.
	 * 3/ Requests have two different scopes : NONE (no unicity control est required).
	 * Check the random generator services the two requests.
	 */
	@Test
	public void test7() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":7}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(7 == entities.size());

			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":4}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(4 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 7 random female identities among a dictionary with 9 female entries.
	 * 2/ Request 5 random male identities among a dictionary with 9 male entries.
	 * 3/ Requests have two different scopes : NONE (no unicity control est required).
	 * Check the random generator services the two requests.
	 */
	@Test
	public void test8() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":7}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(7 == entities.size());

			entities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"male\",\"count\":5}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(5 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 2 random female identity among a dictionary with 9 female entries.
	 * 2/ Request again 2 random female identity and requiring reuse of any former entity for this one.
	 * 3/ Requests have the same scope and blur identifier : SINGLE process identifier.
	 * Check the random generator services the two requests with the SAME results.
	 */
	@Test
	public void test9() {
		try {
			List<RandomEntity> firstCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":2, \"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(2 == firstCallEntities.size());

			List<RandomEntity> secondCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":2, \"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(2 == secondCallEntities.size());
			
			Assert.assertTrue(firstCallEntities.toString().equals(secondCallEntities.toString()));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 1 random female identity among a dictionary with 9 female entries.
	 * 2/ Request again 1 random female identity and requiring reuse of any former entity for this one.
	 * 3/ Requests have the same scope and two different blur identifiers : SINGLE process identifier.
	 * Check the random generator services the two requests with DIFFERENT results.
	 */
	@Test
	public void test10() {
		try {
			List<RandomEntity> firstCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == firstCallEntities.size());

			List<RandomEntity> secondCallEntities = rg.getEntities("{\"blurid\":\"2\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == secondCallEntities.size());
			
			Assert.assertFalse(firstCallEntities.toString().equals(secondCallEntities.toString()));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 1 random female identity among a dictionary with 9 female entries.
	 * 2/ Request again 1 random female identity and requiring reuse of any former entity for this one.
	 * 3/ Requests have the same scope and same blur identifiers BUT TWO DIFFERENT PROCESS NAME
	 * Check the random generator services the two requests with DIFFERENT results.
	 */
	@Test
	public void test11() {
		try {
			List<RandomEntity> firstCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_A", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == firstCallEntities.size());

			List<RandomEntity> secondCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_B", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == secondCallEntities.size());
			
			Assert.assertFalse(firstCallEntities.toString().equals(secondCallEntities.toString()));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 1 random female identity among a dictionary with 9 female entries.
	 * 2/ Request again 1 random female identity and requiring reuse of any former entity for this one.
	 * 3/ Requests have the same scope NONE and same blur identifiers
	 * Check the random generator services the two requests with same results.
	 */
	@Test
	public void test12() {
		try {
			List<RandomEntity> firstCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_A", UNICITY_SCOPE.NONE);
			Assert.assertTrue(1 == firstCallEntities.size());

			List<RandomEntity> secondCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_B", UNICITY_SCOPE.NONE);
			Assert.assertTrue(1 == secondCallEntities.size());
			
			Assert.assertTrue(firstCallEntities.toString().equals(secondCallEntities.toString()));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 1 random female identity among a dictionary with 9 female entries.
	 * 2/ Request again 1 random female identity and requiring reuse of any former entity for this one.
	 * 3/ Requests have the same scope NONE and DIFFERENT blur identifiers
	 * Check the random generator services the two requests with different results.
	 */
	@Test
	public void test13() {
		try {
			List<RandomEntity> firstCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_A", UNICITY_SCOPE.NONE);
			Assert.assertTrue(1 == firstCallEntities.size());

			List<RandomEntity> secondCallEntities = rg.getEntities("{\"blurid\":\"2\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_B", UNICITY_SCOPE.NONE);
			Assert.assertTrue(1 == secondCallEntities.size());
			
			Assert.assertFalse(firstCallEntities.toString().equals(secondCallEntities.toString()));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 1 random female identity among a dictionary with 9 female entries.
	 * 2/ Request again 1 random female identity and requiring reuse of any former entity for this one.
	 * 3/ Requests have the same scope ALL and same blur identifiers
	 * Check the random generator services the two requests with same results.
	 */
	@Test
	public void test14() {
		try {
			List<RandomEntity> firstCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_A", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(1 == firstCallEntities.size());

			List<RandomEntity> secondCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_B", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(1 == secondCallEntities.size());
			
			Assert.assertTrue(firstCallEntities.toString().equals(secondCallEntities.toString()));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * 1/ Request 1 random female identity among a dictionary with 9 female entries.
	 * 2/ Request again 1 random female identity and requiring reuse of any former entity for this one.
	 * 3/ Requests have the same scope ALL and DIFFERENT blur identifiers
	 * Check the random generator services the two requests with different results.
	 */
	@Test
	public void test15() {
		try {
			List<RandomEntity> firstCallEntities = rg.getEntities("{\"blurid\":\"1\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_A", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(1 == firstCallEntities.size());

			List<RandomEntity> secondCallEntities = rg.getEntities("{\"blurid\":\"2\",\"gender\":\"female\",\"count\":1, \"reuse\":\"true\"}", "PROCESS_TESTU_B", UNICITY_SCOPE.PROCESS_ALL);
			Assert.assertTrue(1 == secondCallEntities.size());
			
			Assert.assertFalse(firstCallEntities.toString().equals(secondCallEntities.toString()));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
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