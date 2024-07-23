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

public class RandomPasswordGeneratorTest {

    // private EntityManagerFactory emf;
    private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

    private RandomGenerator rg;

    @Before
    public void setUp() throws Exception {
        // Mock the entity manager helper so that the embedded persistence unit (derby) is used
        EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
        rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.PASSWORD);
    }

    /**
     * Request 1 random password of length 8 characters.
     * All dictionaries type are used: letters (upper/lower case), digits and special characters.
     */
    @Test
    public void test1() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":1,\"symbols\":\"letter_maj,letter_min,special,digit\"," +
                                                         "\"length\":8}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(1, entities.size());
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
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
            rg.getEntities("{\"count\":27,\"symbols\":\"letter_maj\",\"length\":1}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            Assert.fail("An exception should have been raised: capacity is exceeded.");
        } catch (AlambicException e) {
            Assert.assertTrue(true);
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
            List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":27,\"symbols\":\"letter_maj\",\"length\":2}", "PROCESS_TESTU"
                    , UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(27, entities.size());
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Request 2 random passwords of length 8 characters with same blur id (seed) + 1 password for a second blur id (seed).
     * Check NO exception is raised and the password of same blur id are same.
     */
    @Test
    public void test4() {
        try {
            // get password with blur id A
            List<RandomEntity> entities = rg.getEntities("{\"count\":1,\"length\":8,\"symbols\":\"LETTER_MAJ,LETTER_MIN,DIGIT,SPECIAL\"," +
                                                         "\"reuse\":\"true\",\"blurid\":\"John.Doe@noo.fr\"}", "PROCESS_TESTU",
                    UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(1, entities.size());
            String valueA = entities.get(0).getJson();

            // get password with blur id B
            entities = rg.getEntities("{\"count\":1,\"length\":8,\"symbols\":\"LETTER_MAJ,LETTER_MIN,DIGIT,SPECIAL\",\"reuse\":\"true\"," +
                                      "\"blurid\":\"Lucie.fer@hell.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(1, entities.size());
            String valueB = entities.get(0).getJson();

            // get again password with blur id A
            entities = rg.getEntities("{\"count\":1,\"length\":8,\"symbols\":\"LETTER_MAJ,LETTER_MIN,DIGIT,SPECIAL\",\"reuse\":\"true\"," +
                                      "\"blurid\":\"John.Doe@noo.fr\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(1, entities.size());
            String valueC = entities.get(0).getJson();

            // check the initial password is given again (A == C != B)
            Assert.assertEquals(valueA, valueC);
            Assert.assertNotSame(valueA, valueB);
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