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

public class RandomUuidGeneratorTest {

	// private EntityManagerFactory emf;
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

	// private EntityManagerFactory emf;
	private RandomGenerator rg;

	@Before
	public void setUp() throws Exception {
		// Mock the entity manager helper so that the embedded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
		rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.UUID);
	}

	/**
	 * Request one uid (default format is used : long)
	 * Control one uid is provided
	 * Control UID normalisation policy applied
	 */
	@Test
	public void test1() {
		try {
			List<RandomEntity> entities = rg.getEntities("{\"count\":2,\"reuse\":\"true\",\"blurid\":\"123456\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(2 == entities.size());
			Assert.assertFalse(entities.get(0).equals(entities.get(1)));
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