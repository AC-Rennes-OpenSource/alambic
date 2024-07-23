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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RandomDateGeneratorTest {

    private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

    private RandomGenerator rg;

    @Before
    public void setUp() throws Exception {
        // Mock the entity manager helper so that the embedded persistence unit (derby) is used
        EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
        rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.DATE);
    }

    /**
     * Request one random date with same lower & upper years
     */
    @Test
    public void test1() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":1,\"lowerYear\":\"2017\",\"upperYear\":\"2017\"}",
                    "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(1, entities.size());
            Map<String, List<String>> sb = RandomGeneratorService.toStateBaseEntry(entities.get(0));
            Date d = new Date(Long.parseLong(sb.get("timestamp").get(0)));
            DateFormat f = new SimpleDateFormat("dd-MM-yyyy");
            String dateStg = f.format(d);
            Assert.assertTrue(dateStg.matches(".+-2017$"));
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Request one random date with different lower & upper years
     */
    @Test
    public void test2() {
        try {
            List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":1,\"lowerYear\":\"1973\",\"upperYear\":\"1977\"}",
                    "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
            Assert.assertEquals(1, entities.size());
            Map<String, List<String>> sb = RandomGeneratorService.toStateBaseEntry(entities.get(0));
            Date d = new Date(Long.parseLong(sb.get("timestamp").get(0)));
            DateFormat f = new SimpleDateFormat("dd-MM-yyyy");
            String dateStg = f.format(d);
            Assert.assertTrue(dateStg.matches(".+-197[34567]$"));
        } catch (AlambicException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Request one random date with same lower & upper years
     */
    @Test
    public void test3() {
        try {
            List<RandomEntity> entitiesA = rg.getEntities("{\"count\":1,\"lowerYear\":\"2000\",\"upperYear\":\"2100\",\"blurid\":\"56\"," +
                                                          "\"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
            Assert.assertEquals(1, entitiesA.size());

            List<RandomEntity> entitiesB = rg.getEntities("{\"count\":1,\"lowerYear\":\"2000\",\"upperYear\":\"2100\",\"blurid\":\"56\"," +
                                                          "\"reuse\":\"true\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
            Assert.assertEquals(entitiesB, entitiesA);
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