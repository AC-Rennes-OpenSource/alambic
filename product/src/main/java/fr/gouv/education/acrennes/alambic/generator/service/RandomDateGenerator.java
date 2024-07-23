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
package fr.gouv.education.acrennes.alambic.generator.service;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

import javax.persistence.EntityManager;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

public class RandomDateGenerator extends AbstractRandomGenerator {

    // private static final Log log = LogFactory.getLog(RandomDateGenerator.class);

    public RandomDateGenerator(final EntityManager em) throws AlambicException {
        super(em);
    }

    @Override
    public RandomEntity getEntity(final Map<String, Object> query, final String processId, final UNICITY_SCOPE scope) throws AlambicException {
        RandomEntity entity;

        GregorianCalendar gc = new GregorianCalendar();
        int lowerYear = Integer.parseInt((String) query.get("lowerYear"));
        int upperYear = Integer.parseInt((String) query.get("upperYear"));
        int randomYear = (int) (lowerYear + (Math.random() * (upperYear - lowerYear)));
        int randomDayOfYear = (int) (Math.random() * gc.getActualMaximum(Calendar.DAY_OF_YEAR));
        gc.set(Calendar.YEAR, randomYear);
        gc.set(Calendar.DAY_OF_YEAR, randomDayOfYear);
        entity = new RandomLambdaEntity("{\"timestamp\":" + gc.getTimeInMillis() + ",\"timezone\":\"" + gc.getTimeZone().getID() + "\"}");

        return entity;
    }

    @Override
    public GENERATOR_TYPE getType(final Map<String, Object> query) {
        return RandomGeneratorService.GENERATOR_TYPE.DATE;
    }

    @Override
    public long getCapacity(final Map<String, Object> query) throws AlambicException {
        /*
         * This theoretical dictionary capacity should be computed based-on the range of years
         * as defined by the query parameters lower/upper.
         * Since a random date leads to get a timestamp (whitin this range) expressed in milliseconds
         * since Epoch time 01/01/1973, the number of possible (random) results is huge.
         * Hence, the capacity is quite infinite. The returned value is chosen so that a job
         * to anonymize a population from Brittany succeeds.
         */
        return 1000000;
    }

    @Override
    public String getCapacityFilter(final Map<String, Object> query) {
        String lowerYear = (String) query.get("lowerYear");
        String upperYear = (String) query.get("upperYear");
        return String.format("[%s-%s]", lowerYear, upperYear);
    }

}