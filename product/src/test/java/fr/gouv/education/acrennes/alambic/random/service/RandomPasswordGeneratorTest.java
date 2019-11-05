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
package fr.gouv.education.acrennes.alambic.random.service;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.AbstractRandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

public class RandomPasswordGeneratorTest {

	// private EntityManagerFactory emf;
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "DERBY_PERSISTENCE_UNIT";

	// private EntityManagerFactory emf;
	private RandomGeneratorService rgs;
	private RandomGenerator rg;

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embbeded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
	}

	/**
	 * Request 1 random password of length 8 characters.
	 * All dictionaries type are used: letters (upper/lower case), digits and special characters.
	 */
	@Test
	public void test1() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.PASSWORD);

			List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"symbols\":\"letter_maj,letter_min,special,digit\",\"length\":8}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (null != rg) {
				rg.close();
			}
			if (null != rgs) {
				rgs.close();
			}
		}
	}

	/**
	 * Request 27 random password of length 1 characters.
	 * Only the dictionary of type upper letters is used.
	 * Check an exception is raised since the capacity is exceeded (26)
	 */
	@Test
	public void test2() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.PASSWORD);

			rg.getEntities("{\"count\":27,\"symbols\":\"letter_maj\",\"length\":1}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.fail("An exception should have been raised: capacity is exceeded.");
		} catch (AlambicException e) {
			Assert.assertTrue(true);
		} finally {
			if (null != rg) {
				rg.close();
			}
			if (null != rgs) {
				rgs.close();
			}
		}
	}

	/**
	 * Request 27 random password of length 2 characters.
	 * Only the dictionary of type upper letters is used.
	 * Check NO exception is raised since the capacity is NOT exceeded (27 < 26*26)
	 */
	@Test
	public void test3() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.PASSWORD);

			List<RandomEntity> entities = rg.getEntities("{\"count\":27,\"symbols\":\"letter_maj\",\"length\":2}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(27 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (null != rg) {
				rg.close();
			}
			if (null != rgs) {
				rgs.close();
			}
		}
	}

	/**
	 * Request 2 random passwords of length 8 characters with same blur id (seed) + 1 password for a second blur id (seed).
	 * Check NO exception is raised and the password of same blur id are same.
	 */
	@Test
	public void test4() {
		try {
			RandomGeneratorService rgs = RandomGeneratorService.getInstance();
			RandomGenerator rug = rgs.getRandomGenerator(GENERATOR_TYPE.PASSWORD);

			// get password with blur id A
			List<RandomEntity> entities = rug.getEntities("{\"count\":1,\"length\":8,\"symbols\":\"LETTER_MAJ,LETTER_MIN,DIGIT,SPECIAL\",\"reuse\":\"true\",\"blurid\":\"John.Doe@noo.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(entities.size() == 1);
			String valueA = entities.get(0).getJson();

			// get password with blur id B
			entities = rug.getEntities("{\"count\":1,\"length\":8,\"symbols\":\"LETTER_MAJ,LETTER_MIN,DIGIT,SPECIAL\",\"reuse\":\"true\",\"blurid\":\"Lucie.fer@hell.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(entities.size() == 1);
			String valueB = entities.get(0).getJson();

			// get again password with blur id A
			entities = rug.getEntities("{\"count\":1,\"length\":8,\"symbols\":\"LETTER_MAJ,LETTER_MIN,DIGIT,SPECIAL\",\"reuse\":\"true\",\"blurid\":\"John.Doe@noo.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(entities.size() == 1);
			String valueC = entities.get(0).getJson();

			// check the initial password is given again (A == C != B)
			Assert.assertEquals(valueA, valueC);
			Assert.assertNotSame(valueA, valueB);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (null != rg) {
				rg.close();
			}
			if (null != rgs) {
				rgs.close();
			}
		}
	}

	@After
	public void tearDown() {
		try {
			AbstractRandomGenerator.cleanCapacityCache();
			
			/**
			 * Shutdown the derby system so that other unit tests don't run into exception because the database was not
			 * released.
			 * This is not visible when running the tests within Eclipse environment (launcher) but it is when packaging
			 * the project with maven.
			 */
			// emf.close();
			EntityManagerHelper.close();
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException e) {
			System.out.println("Shutdown derby, message: " + e.getMessage());
		}
	}

}
