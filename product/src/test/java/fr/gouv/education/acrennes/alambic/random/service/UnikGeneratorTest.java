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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

public class UnikGeneratorTest {

	// private EntityManagerFactory emf;
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

	// private EntityManagerFactory emf;
	private RandomGenerator rg;

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embedded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
		rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.UNIK);
	}

	/**
	 * Request one uid (default format is used : long)
	 * Control one uid is provided
	 * Control UID normalisation policy applied
	 */
	@Test
	public void test1() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"reuse\":\"true\",\"firstName\":\"Yann\",\"lastName\":\"Andrianirinaharivelo\",\"blurid\":\"123456\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities.size());
			Assert.assertEquals("{\"unik\":\"yandrianirinahar\"}", entities.get(0).getJson());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request a first uid (reuse IS ALWAYS enabled for UNIK)
	 * Request a second uid with the same blurid
	 * Request a third uid 
	 * Control first ans second uid are the same
	 * Control the third uid is diffrent from the others
	 * Control UID normalisation policy applied
	 */
	@Test
	public void test2() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"true\",\"blurid\":\"123456\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String firstEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"true\",\"blurid\":\"123456\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String secondEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"true\",\"blurid\":\"789123\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String thirdEntity = entities.get(0).getJson();


			Assert.assertEquals("{\"unik\":\"ylecleach\"}", firstEntity);
			Assert.assertEquals("{\"unik\":\"ylecleach\"}", secondEntity);
			Assert.assertEquals("{\"unik\":\"ylecleach2\"}", thirdEntity);
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test3() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"firstName\":\"Jean Marc\",\"lastName\":\"Le Meur - Bellec\",\"reuse\":\"true\",\"blurid\":\"5723456\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String firstEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"count\":1,\"firstName\":\"Louis-André\",\"lastName\":\"Martin--Coïc\",\"reuse\":\"true\",\"blurid\":\"123411\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String secondEntity = entities.get(0).getJson();

			entities = rg.getEntities("{\"count\":1,\"firstName\":\"Théo\",\"lastName\":\"L'hélias\",\"reuse\":\"true\",\"blurid\":\"785523\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			String thirdEntity = entities.get(0).getJson();


			Assert.assertEquals("{\"unik\":\"jlemeurbellec\"}", firstEntity);
			Assert.assertEquals("{\"unik\":\"lmartincoic\"}", secondEntity);
			Assert.assertEquals("{\"unik\":\"tlhelias\"}", thirdEntity);
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