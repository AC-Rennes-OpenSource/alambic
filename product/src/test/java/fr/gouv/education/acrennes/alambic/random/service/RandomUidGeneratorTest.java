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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class RandomUidGeneratorTest {

    // private EntityManagerFactory emf;
    private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

    private RandomGenerator rg;

    @Before
    public void setUp() throws Exception {
        // Mock the entity manager helper so that the embedded persistence unit (derby) is used
        EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
        rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.UID);
    }

    /**
     * Request one uid (default format is used : long)
     * Control one uid is provided
     * Control UID normalisation policy applied
     */
    @Test
    public void test1() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\"}",
                    "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(1, entities.size());
            Assert.assertEquals("{\"uid\":\"yann.le-cleac-h\"}", entities.get(0).getJson());
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Request a first uid (reuse IS NOT enabled, default format is used : long)
     * Request a second uid  (reuse IS NOT enabled, default format is used : long)
     * Request a third uid  (reuse IS NOT enabled, default format is used : long)
     * Control each uid differs from others
     * Control UID normalisation policy applied
     */
    @Test
    public void test2() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"false\"," +
                                                         "\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String firstEntity = entities.get(0).getJson();

            entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"false\",\"blurid\":\"56\"}",
                    "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String secondEntity = entities.get(0).getJson();

            entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"false\",\"blurid\":\"56\"}",
                    "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String thirdEntity = entities.get(0).getJson();

            Assert.assertEquals("{\"uid\":\"yann.le-cleac-h\"}", firstEntity);
            Assert.assertEquals("{\"uid\":\"yann.le-cleac-h2\"}", secondEntity);
            Assert.assertEquals("{\"uid\":\"yann.le-cleac-h3\"}", thirdEntity);
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Request a first uid (reuse IS enabled, default format is used : long)
     * Request a second uid  (reuse IS enabled, default format is used : long)
     * Request a third uid  (reuse IS enabled, default format is used : long)
     * Control each uid equals others.
     * Control UID normalisation policy applied
     */
    @Test
    public void test3() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"true\"," +
                                                         "\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String firstEntity = entities.get(0).getJson();

            entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"true\",\"blurid\":\"56\"}",
                    "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String secondEntity = entities.get(0).getJson();

            entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"true\",\"blurid\":\"56\"}",
                    "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String thirdEntity = entities.get(0).getJson();

            Assert.assertEquals(firstEntity, secondEntity);
            Assert.assertEquals(secondEntity, thirdEntity);
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Request a first uid (reuse IS NOT enabled & process identifier A, default format is used : long)
     * Request a second uid (reuse IS NOT enabled & process identifier B, default format is used : long)
     * Control the second one equals the first one.
     */
    @Test
    public void test4() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"false\"," +
                                                         "\"blurid\":\"56\"}", "PROCESS_TESTU_A", UNICITY_SCOPE.PROCESS);
            String firstEntity = entities.get(0).getJson();

            entities = rg.getEntities("{\"count\":1,\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"false\",\"blurid\":\"56\"}",
                    "PROCESS_TESTU_B", UNICITY_SCOPE.PROCESS);
            String secondEntity = entities.get(0).getJson();

            Assert.assertEquals(firstEntity, secondEntity);
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Request a first uid (reuse IS NOT enabled, short format is used)
     * Request a second uid  (reuse IS NOT enabled, short format is used)
     * Request a third uid  (reuse IS NOT enabled, short format is used)
     * Control the short format is used
     * Control each uid differs from others
     * Control UID normalisation policy applied
     */
    @Test
    public void test5() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"format\":\"SHORT\",\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\"," +
                                                         "\"reuse\":\"false\",\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String firstEntity = entities.get(0).getJson();

            entities = rg.getEntities("{\"count\":1,\"format\":\"SHORT\",\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"false\"," +
                                      "\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String secondEntity = entities.get(0).getJson();

            entities = rg.getEntities("{\"count\":1,\"format\":\"SHORT\",\"firstName\":\"Yann\",\"lastName\":\"Le Cleac'h\",\"reuse\":\"false\"," +
                                      "\"blurid\":\"56\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            String thirdEntity = entities.get(0).getJson();

            Assert.assertEquals("{\"uid\":\"yle-cleac-h\"}", firstEntity);
            Assert.assertEquals("{\"uid\":\"yle-cleac-h2\"}", secondEntity);
            Assert.assertEquals("{\"uid\":\"yle-cleac-h3\"}", thirdEntity);
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