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
package fr.gouv.education.acrennes.alambic.api;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

/**
 * Campagne de test pour la génération des identifiants d'exécution.
 */
public class RunIdGeneratorTest {

    @Test
    public void generator_builds_expected_id() {
        final Clock clock = Clock.fixed(Instant.parse("2018-02-06T18:14:20.000Z"), ZoneId.of("UTC"));
        final RunIdGenerator generator = new RunIdGenerator(0, clock);

        final String generatedId = generator.nextId();

        Assert.assertEquals(generatedId, "20180206-181420000-0001");
    }

    @Test
    public void generator_builds_expected_id_consecutive_calls() {
        final Clock clock = Clock.fixed(Instant.parse("2018-02-07T10:31:54.030Z"), ZoneId.of("UTC"));
        final RunIdGenerator generator = new RunIdGenerator(13, clock);

        final List<String> generatedIds = new ArrayList<>();
        generatedIds.add(generator.nextId());
        generatedIds.add(generator.nextId());
        generatedIds.add(generator.nextId());

        final List<String> expected = Arrays.asList(
                "20180207-103154030-000E",
                "20180207-103154030-000F",
                "20180207-103154030-0010"
        );
        
        Assert.assertEquals(String.join(",", expected.stream().sorted().collect(Collectors.toList())), 
        		String.join(",", generatedIds.stream().sorted().collect(Collectors.toList())));
    }

    @Test
    public void generator_builds_counter_that_wraps_after_FFFF() {
        final Clock clock = Clock.fixed(Instant.parse("2018-02-07T10:38:10.871Z"), ZoneId.of("UTC"));
        final RunIdGenerator generator = new RunIdGenerator(65534, clock);

        final List<String> generatedIds = new ArrayList<>();
        generatedIds.add(generator.nextId());
        generatedIds.add(generator.nextId());

        final List<String> expected = Arrays.asList(
                "20180207-103810871-FFFF",
                "20180207-103810871-0000"
        );

        Assert.assertEquals(String.join(",", expected.stream().sorted().collect(Collectors.toList())), 
        		String.join(",", generatedIds.stream().sorted().collect(Collectors.toList())));
    }

    @Test
    public void generator_handles_well_being_initialized_with_max_integer_value() {
        final Clock clock = Clock.fixed(Instant.parse("2018-02-07T10:38:10.871Z"), ZoneId.of("UTC"));
        final RunIdGenerator generator = new RunIdGenerator(Integer.MAX_VALUE, clock);

        final String generatedId = generator.nextId();

        Assert.assertEquals(generatedId, "20180207-103810871-0000");
    }
    
}
