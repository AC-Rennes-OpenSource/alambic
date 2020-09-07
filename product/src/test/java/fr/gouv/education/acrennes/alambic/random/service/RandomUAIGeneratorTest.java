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

import java.util.List;
import java.util.function.Predicate;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import junit.framework.Assert;

public class RandomUAIGeneratorTest {

	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

	private RandomGenerator rg;

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embedded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
		rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.UAI);
	}

	/**
	 * Request three UAI
	 * A root UAI is set
	 * Control three distinct UAI are provided
	 * Control UAI format is respected
	 */
	@Test
	public void test1() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":3,\"root\":\"029\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			long distinctCount = entities.stream()
					.map(e -> e.getJson())
					.distinct()
					.count();
			Assert.assertTrue(3 == distinctCount);
			Assert.assertTrue(entities.stream().allMatch(new Predicate<RandomEntity>() {

				@Override
				public boolean test(RandomEntity re) {
					return re.getJson().matches("\\{\"uai\":\"029[0-9]{4}[A-Z]\"\\}");
				}
			}));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request three UAI
	 * A less restrictive root UAI is set
	 * Control three distinct UAI are provided
	 * Control UAI format is respected
	 */
	@Test
	public void test2() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":3,\"root\":\"02\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			long distinctCount = entities.stream()
					.map(e -> e.getJson())
					.distinct()
					.count();
			Assert.assertTrue(3 == distinctCount);
			Assert.assertTrue(entities.stream().allMatch(new Predicate<RandomEntity>() {

				@Override
				public boolean test(RandomEntity re) {
					return re.getJson().matches("\\{\"uai\":\"02[0-9]{5}[A-Z]\"\\}");
				}
			}));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request three UAI
	 * A more restrictive root UAI is set
	 * Control three distinct UAI are provided
	 * Control UAI format is respected
	 */
	@Test
	public void test3() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":3,\"root\":\"03541\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			long distinctCount = entities.stream()
					.map(e -> e.getJson())
					.distinct()
					.count();
			Assert.assertTrue(3 == distinctCount);
			Assert.assertTrue(entities.stream().allMatch(new Predicate<RandomEntity>() {

				@Override
				public boolean test(RandomEntity re) {
					return re.getJson().matches("\\{\"uai\":\"03541[0-9]{2}[A-Z]\"\\}");
				}
			}));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request three UAI
	 * No root UAI is set
	 * Control three distinct UAI are provided
	 * Control UAI format is respected
	 */
	@Test
	public void test4() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":3}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			long distinctCount = entities.stream()
					.map(e -> e.getJson())
					.distinct()
					.count();
			Assert.assertTrue(3 == distinctCount);
			Assert.assertTrue(entities.stream().allMatch(new Predicate<RandomEntity>() {

				@Override
				public boolean test(RandomEntity re) {
					return re.getJson().matches("\\{\"uai\":\"035[0-9]{4}[A-Z]\"\\}");
				}
			}));
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request three UAI
	 * A root UAI is set
	 * Reuse mode is enabled
	 * Control three distinct UAI are provided
	 * Control UAI format is respected
	 * Control reuse is done
	 */
	@Test
	public void test5() {
		try {
			List<RandomEntity> entities1 = rg.getEntities("{\"blurid\":\"1\",\"count\":3,\"reuse\":\"true\",\"root\":\"029\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			long distinctCount = entities1.stream()
					.map(e -> e.getJson())
					.distinct()
					.count();
			Assert.assertTrue(3 == distinctCount);
			Assert.assertTrue(entities1.stream().allMatch(new Predicate<RandomEntity>() {

				@Override
				public boolean test(RandomEntity re) {
					return re.getJson().matches("\\{\"uai\":\"029[0-9]{4}[A-Z]\"\\}");
				}
			}));
			
			List<RandomEntity> entities2 = rg.getEntities("{\"blurid\":\"1\",\"count\":3,\"reuse\":\"true\",\"root\":\"029\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			distinctCount = entities1.stream()
					.map(e -> e.getJson())
					.distinct()
					.count();
			Assert.assertTrue(3 == distinctCount);
			entities2.forEach(e -> Assert.assertTrue(entities1.stream().anyMatch(new Predicate<RandomEntity>() {

				@Override
				public boolean test(RandomEntity re) {
					return re.getHash().equals(e.getHash());
				}
			})));

		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request one UAI
	 * A root UAI is set
	 * A single process is specified with scope "process"
	 * Reuse is enabled but two different blur identifier at set
	 * Control UAI format is respected
	 * Control the two result are different
	 */
	@Test
	public void test6() {
		try {
			List<RandomEntity> entities1 = rg.getEntities("{\"blurid\":\"1\",\"count\":1,\"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities1.size());
			
			List<RandomEntity> entities2 = rg.getEntities("{\"blurid\":\"2\",\"count\":1,\"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities2.size());
			
			Assert.assertFalse(entities1.get(0).getHash().equals(entities2.get(0).getHash()));
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
		 * Shutdown the derby system so that other unit tests don't run into exception because the database was not
		 * released.
		 * This is not visible when running the tests within Eclipse environment (launcher) but it is when packaging
		 * the project with maven.
		 */
		EntityManagerHelper.close();
	}

}