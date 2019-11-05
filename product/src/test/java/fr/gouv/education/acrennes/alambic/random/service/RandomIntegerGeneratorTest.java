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
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.AbstractRandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomIntegerGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;
import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RandomGeneratorService.class })
@PowerMockIgnore("javax.management.*")
public class RandomIntegerGeneratorTest {

	private static final String UNIT_TEST_PERSISTENCE_UNIT = "DERBY_PERSISTENCE_UNIT";

	private RandomGeneratorService rgs;
	private RandomGenerator rg;

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embbeded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
	}

	/**
	 * Request one random integer with range [not defined - 1000]
	 */
	@Test
	public void test1() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"maxValue\":1000}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities.size());
			Map<String, List<String>> sb = rgs.toStateBaseEntry(entities.get(0));
			int value = Integer.parseInt(((String) sb.get("value").get(0)));
			Assert.assertTrue(0 <= value && value <= 1000);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request one random integer with range [500 - 1000]
	 */
	@Test
	public void test2() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"minValue\":500,\"maxValue\":1000}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities.size());
			Map<String, List<String>> sb = rgs.toStateBaseEntry(entities.get(0));
			int value = Integer.parseInt(((String) sb.get("value").get(0)));
			Assert.assertTrue(500 <= value && value <= 1000);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request one random integer with range [2 - 2]
	 */
	@Test
	public void test3() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"minValue\":2,\"maxValue\":2}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities.size());
			Map<String, List<String>> sb = rgs.toStateBaseEntry(entities.get(0));
			int value = Integer.parseInt(((String) sb.get("value").get(0)));
			Assert.assertTrue(value == 2);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request 4 random integer with range [2 - 5]
	 * Check the generator capacity is NOT overlaid.
	 */
	@Test
	public void test4() {
		try {
			String resultStg = "";
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entities = rg.getEntities("{\"count\":4,\"minValue\":2,\"maxValue\":5}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entities.size());
			for (RandomEntity entity : entities) {
				Map<String, List<String>> sb = rgs.toStateBaseEntry(entity);
				resultStg = resultStg.concat((String) sb.get("value").get(0));
			}
			Assert.assertTrue(resultStg.matches("[2345]{4}"));				
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request 5 random integer with range [2 - 5]
	 * Check the generator capacity IS overlaid.
	 */
	@Test
	public void test5() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			rg.getEntities("{\"count\":5,\"minValue\":2,\"maxValue\":5}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.fail();
		} catch (AlambicException e) {
			Assert.assertTrue(Boolean.TRUE);				
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request 4 random integer with range [2 - 5] with one process
	 * Request 4 random integer with range [2 - 5] with a different process
	 * Check the generator capacity is NOT overlaid.
	 */
	@Test
	public void test6() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entities = rg.getEntities("{\"count\":4,\"minValue\":2,\"maxValue\":5}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entities.size());

			entities = rg.getEntities("{\"count\":4,\"minValue\":2,\"maxValue\":5}", "PROCESS_TESTU_2", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entities.size());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request 4 random integer with range [0 - 10 000] with one process
	 * Request 4 random integer with range [0 - 10 000] with the same process and reuse enabled and same blur identifier
	 * Check the same values are returned
	 */
	@Test
	public void test7() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entitiesA = rg.getEntities("{\"count\":4,\"minValue\":0,\"maxValue\":10000,\"reuse\":\"true\",\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entitiesA.size());

			List<RandomEntity> entitiesB = rg.getEntities("{\"count\":4,\"minValue\":0,\"maxValue\":10000,\"reuse\":\"true\",\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entitiesB.size());
			Assert.assertTrue(entitiesA.equals(entitiesB));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request 4 random integer with range [0 - 10 000] with one process
	 * Request 4 random integer with range [0 - 10 000] with the same process and reuse enabled and different blur identifier
	 * Check different values are returned
	 */
	@Test
	public void test8() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entitiesA = rg.getEntities("{\"count\":4,\"minValue\":0,\"maxValue\":10000,\"reuse\":\"true\",\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entitiesA.size());

			List<RandomEntity> entitiesB = rg.getEntities("{\"count\":4,\"minValue\":0,\"maxValue\":10000,\"reuse\":\"true\",\"blurid\":\"29\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(4 == entitiesB.size());
			Assert.assertTrue(!entitiesA.equals(entitiesB));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request one random integer with range [5 - 2]
	 * Check an exception is raised.
	 */
	@Test
	public void test9() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			rg.getEntities("{\"count\":1,\"minValue\":5,\"maxValue\":2}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.fail();
		} catch (AlambicException e) {
			Assert.assertTrue(Boolean.TRUE);
		} finally {
			rg.close();
			rgs.close();
		}
	}

	/**
	 * Request 1 random integer with range [100 - 900] with one process
	 * Request 1 random integer with range [500 - 800] with the same process and reuse enabled and same blur identifier
	 * Check different values are returned
	 */
	@Test
	public void test10() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);
			List<RandomEntity> entitiesA = rg.getEntities("{\"count\":1,\"minValue\":100,\"maxValue\":900,\"reuse\":\"true\",\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entitiesA.size());

			List<RandomEntity> entitiesB = rg.getEntities("{\"count\":1,\"minValue\":500,\"maxValue\":800,\"reuse\":\"true\",\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entitiesB.size());
			Assert.assertTrue(!entitiesA.equals(entitiesB));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
		}
	}

	@Test
	public void test11() {
		try {
			rgs = RandomGeneratorService.getInstance();

			PowerMockito.whenNew(RandomIntegerGenerator.class).withAnyArguments().thenReturn(new RandomTestGenerator(EntityManagerHelper.getEntityManager()));
			RandomGenerator rgA = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);

			PowerMockito.whenNew(RandomIntegerGenerator.class).withAnyArguments().thenReturn(new RandomTestGenerator(EntityManagerHelper.getEntityManager()));
			RandomGenerator rgB = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);

			List<RandomEntity> entitiesA = rgA.getEntities("{\"count\":10,\"minValue\":1,\"maxValue\":10}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(10 == entitiesA.size());

			List<RandomEntity> entitiesB = rgB.getEntities("{\"count\":10,\"minValue\":1,\"maxValue\":10}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(10 == entitiesB.size());
			Assert.assertTrue(!entitiesA.equals(entitiesB));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			rgs.close();
		}
	}

	/**
	 * Request 1 random integer with range [1 - 100 000] with no unicity control with blurId
	 * Request 1 random integer with range [1 - 100 000] with no unicity control with blurid and reuse enabled
	 * Check the same values are returned
	 */
	@Test
	public void test12() {
		try {
			rgs = RandomGeneratorService.getInstance();
			rg = rgs.getRandomGenerator(GENERATOR_TYPE.INTEGER);

			List<RandomEntity> entitiesA = rg.getEntities("{\"count\":1,\"minValue\":1,\"maxValue\":100000,\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(1 == entitiesA.size());

			List<RandomEntity> entitiesB = rg.getEntities("{\"count\":1,\"minValue\":1,\"maxValue\":100000,\"reuse\":\"true\",\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(1 == entitiesB.size());
			Assert.assertTrue(entitiesA.equals(entitiesB));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		} finally {
			rg.close();
			rgs.close();
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
			EntityManagerHelper.close();
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException e) {
			System.out.println("Shutdown derby, message: " + e.getMessage());
		}
	}

	private class RandomTestGenerator extends RandomIntegerGenerator {

		public RandomTestGenerator(EntityManager em) throws AlambicException {
			super(em);
			em.setFlushMode(FlushModeType.AUTO);
		}

		@Override
		public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope)	throws AlambicException {
			return new RandomLambdaEntity("{\"value\":\"9\"}");
		}

	}

}
