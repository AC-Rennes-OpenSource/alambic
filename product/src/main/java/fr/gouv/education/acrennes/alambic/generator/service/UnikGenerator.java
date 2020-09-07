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

import java.util.Map;

import javax.persistence.EntityManager;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.freemarker.FMFunctions;
import fr.gouv.education.acrennes.alambic.freemarker.NormalizationPolicy;
import org.apache.commons.lang.StringUtils;

import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;

public class UnikGenerator extends AbstractRandomGenerator {

    private final FMFunctions fcts;
    
    public UnikGenerator(final EntityManager em) throws AlambicException {
        super(em);
        this.fcts = new FMFunctions();
    }

    @Override
    public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope)
            throws AlambicException {
       
    	RandomEntity entity;

    	String firstName = (String) query.get("firstName");
    	String lastName = (String) query.get("lastName");
    	if (StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName)) {
    		throw new AlambicException("Both parameters 'firstName' and 'lastName' must be set");
    	}

    	String unik = this.fcts.normalize(String.format("%s%s", firstName.substring(0, 1), lastName), NormalizationPolicy.UNIK, true).toLowerCase();
    	if(unik.length()>16){
    		unik = unik.substring(0, 16);
    	}

    	// handle random generation iteration
    	int iteration = (int) query.get(Constants.RANDOM_GENERATOR_INNER_ITERATION);
    	if (1 < iteration) {
    		unik = unik.concat(String.valueOf(iteration));
    	}

        entity = new RandomLambdaEntity("{\"unik\":\"" + unik + "\"}");

        return entity;
    }

    @Override
    public RandomGeneratorService.GENERATOR_TYPE getType(final Map<String, Object> query) {
        return RandomGeneratorService.GENERATOR_TYPE.UNIK;
    }

    @Override
    public String getCapacityFilter(Map<String, Object> query) throws AlambicException {
        return Constants.UNLIMITED_GENERATOR_FILTER;
    }

}
