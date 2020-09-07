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

public class RandomMailGeneratorTest {

	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

	private RandomGenerator rg;

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embedded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
		rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.MAIL);
	}

	/**
	 * Request one mail
	 * Control one mail is provided
	 * Control MAIL normalization policy applied
	 */
	@Test
	public void test1() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities.size());
			Assert.assertEquals("{\"mail\":\"yann.le-cleach@noreply.phm.education.gouv.fr\"}", entities.get(0).getJson());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request a first mail (reuse IS NOT enabled)
	 * Request a second mail  (reuse IS NOT enabled)
	 * Request a third mail  (reuse IS NOT enabled)
	 * Unicity scope is the process (same process is used)
	 * Control each mail differs from others
	 * Control MAIL normalisation policy applied
	 */
	@Test
	public void test2() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String firstEntity = entities.get(0).getJson();
			
			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String secondEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String thirdEntity = entities.get(0).getJson();

			Assert.assertEquals("{\"mail\":\"yann.le-cleach@noreply.phm.education.gouv.fr\"}", firstEntity);
			Assert.assertEquals("{\"mail\":\"yann.le-cleach2@noreply.phm.education.gouv.fr\"}", secondEntity);
			Assert.assertEquals("{\"mail\":\"yann.le-cleach3@noreply.phm.education.gouv.fr\"}", thirdEntity);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request a first mail (reuse IS NOT enabled)
	 * Request a second mail  (reuse IS NOT enabled)
	 * Request a third mail  (reuse IS NOT enabled)
	 * Unicity scope is ALL process (NOT same processes are used)
	 * Control each mail differs from others
	 * Control MAIL normalisation policy applied
	 */
	@Test
	public void test3() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU_1", UNICITY_SCOPE.PROCESS_ALL);
			String firstEntity = entities.get(0).getJson();
			
			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU_2", UNICITY_SCOPE.PROCESS_ALL);
			String secondEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU_3", UNICITY_SCOPE.PROCESS_ALL);
			String thirdEntity = entities.get(0).getJson();

			Assert.assertEquals("{\"mail\":\"yann.le-cleach@noreply.phm.education.gouv.fr\"}", firstEntity);
			Assert.assertEquals("{\"mail\":\"yann.le-cleach2@noreply.phm.education.gouv.fr\"}", secondEntity);
			Assert.assertEquals("{\"mail\":\"yann.le-cleach3@noreply.phm.education.gouv.fr\"}", thirdEntity);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request a first mail (reuse IS NOT enabled)
	 * Request a second mail  (reuse IS NOT enabled)
	 * Request a third mail  (reuse IS NOT enabled)
	 * Unicity scope is NONE
	 * Control each mail equals others.
	 * Control MAIL normalisation policy applied
	 */
	@Test
	public void test4() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU_1", UNICITY_SCOPE.NONE);
			String firstEntity = entities.get(0).getJson();
			
			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU_2", UNICITY_SCOPE.NONE);
			String secondEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"false\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU_3", UNICITY_SCOPE.NONE);
			String thirdEntity = entities.get(0).getJson();

			Assert.assertEquals("{\"mail\":\"yann.le-cleach@noreply.phm.education.gouv.fr\"}", firstEntity);
			Assert.assertEquals(firstEntity, secondEntity);
			Assert.assertEquals(firstEntity, thirdEntity);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request a first mail (reuse IS enabled)
	 * Request a second mail  (reuse IS enabled)
	 * Request a third mail  (reuse IS enabled)
	 * Unicity scope is the process (same process is used)
	 * Control each mail equals others.
	 * Control MAIL normalisation policy applied
	 */
	@Test
	public void test5() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"true\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String firstEntity = entities.get(0).getJson();
			
			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"true\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String secondEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"blurid\":\"1\",\"reuse\":\"true\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"domain\":\"noreply.phm.education.gouv.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String thirdEntity = entities.get(0).getJson();

			Assert.assertEquals("{\"mail\":\"yann.le-cleach@noreply.phm.education.gouv.fr\"}", firstEntity);
			Assert.assertEquals(firstEntity, secondEntity);
			Assert.assertEquals(firstEntity, thirdEntity);
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