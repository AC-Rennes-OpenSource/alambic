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

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.freemarker.FMFunctions;
import fr.gouv.education.acrennes.alambic.freemarker.NormalizationPolicy;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import java.util.Map;

public class RandomMailGenerator extends AbstractRandomGenerator {

    //	private static final Log log = LogFactory.getLog(RandomMailGenerator.class);
    private final FMFunctions fcts;

    public RandomMailGenerator(final EntityManager em) throws AlambicException {
        super(em);
        this.fcts = new FMFunctions();
    }

    @Override
    public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope) throws AlambicException {
        RandomEntity entity;

        String firstName = (String) query.get("firstName");
        String lastName = (String) query.get("lastName");
        String domain = (String) query.get("domain");
        if (StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName) || StringUtils.isBlank(domain)) {
            throw new AlambicException("All parameters 'firstName', 'lastName' and 'domain' must be set");
        }

        String nFirstName = this.fcts.normalize(firstName, NormalizationPolicy.EMAIL, true);
        String nLastName = this.fcts.normalize(lastName, NormalizationPolicy.EMAIL, true);
        String nDomain = this.fcts.normalize(domain, NormalizationPolicy.DEFAULT, true);
        String mail = this.fcts.normalize(String.format("%s.%s@%s", nFirstName, nLastName, nDomain), NormalizationPolicy.EMAIL).toLowerCase();

        // handle random generation iteration
        int iteration = (int) query.get(Constants.RANDOM_GENERATOR_INNER_ITERATION);
        if (1 < iteration) {
            mail = mail.replace("@", String.valueOf(iteration).concat("@"));
        }

        entity = new RandomLambdaEntity("{\"mail\":\"" + mail + "\"}");
        return entity;
    }

    @Override
    public RandomGeneratorService.GENERATOR_TYPE getType(final Map<String, Object> query) {
        return RandomGeneratorService.GENERATOR_TYPE.MAIL;
    }

    @Override
    public String getCapacityFilter(Map<String, Object> query) throws AlambicException {
        String firstName = (String) query.get("firstName");
        String lastName = (String) query.get("lastName");
        String domain = (String) query.get("domain");

        if (StringUtils.isNotBlank(firstName) && StringUtils.isNotBlank(lastName) && StringUtils.isNotBlank(domain)) {
            firstName = this.fcts.normalize(firstName, NormalizationPolicy.NOM, true);
            lastName = this.fcts.normalize(lastName, NormalizationPolicy.NOM, true);
            domain = this.fcts.normalize(domain, NormalizationPolicy.DEFAULT, true);
        } else {
            throw new AlambicException("All parameters 'firstName', 'lastName' and 'domain' must be set");
        }

        return String.format("[%s-%s-%s]", firstName, lastName, domain);
    }

}
