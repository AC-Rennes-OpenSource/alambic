/*******************************************************************************
 * Copyright (C) 2019-2021 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import fr.gouv.education.acrennes.alambic.jobs.extract.sources.LDAPSource;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GAREtab;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.util.*;
import java.util.stream.Stream;

public class GAR1DEtabBuilderTest extends GAR1DBuilderTestUtils {

    private GAR1DEtablissementBuilder builder;

    @Before
    public void init() {
        Source mockedStructures = PowerMockito.mock(LDAPSource.class);
        Source mockedEntries = PowerMockito.mock(LDAPSource.class);
        List<Map<String, List<String>>> structuresMaps = new ArrayList<>();
        Stream.of("0351234A", "0354321B", "0352143C").forEach(uai -> structuresMaps.add(Collections.singletonMap("ENTStructureUAI", Collections.singletonList(uai))));
        PowerMockito.when(mockedStructures.getEntries()).thenReturn(structuresMaps);
        PowerMockito.when(mockedEntries.getEntries()).thenReturn(null);
        Map<String, Source> resources = new HashMap<>();
        resources.put("Structures", mockedStructures);
        resources.put("Entries", mockedEntries);
        GARBuilderParameters parameters = new GARBuilderParameters(
                null,
                resources,
                0,
                null,
                100,
                "1",
                "014",
                "",
                "",
                null,
                null);
        builder = new GAR1DEtablissementBuilder(parameters);
    }

    @Test
    public void testGarEtabFullyDefined() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTStructureUAI", Collections.singletonList("0351234A"));
        entity.put("ENTDisplayName", Collections.singletonList("École A"));
        entity.put("ENTEtablissementContrat", Collections.singletonList("PU"));
        entity.put("telephoneNumber", Collections.singletonList("+33 1 23 45 67 89"));

        GAREtab expectedGarEtab = new ObjectFactory().createGAREtab();

        expectedGarEtab.setGARStructureUAI("0351234A");
        expectedGarEtab.setGARStructureNomCourant("École A");
        expectedGarEtab.setGARStructureContrat("PU");
        expectedGarEtab.setGARStructureTelephone("+33 1 23 45 67 89");
        expectedGarEtab.setGARStructureEmail("ce.0351234A@ac-rennes.fr");

        GAREtab garEtab = builder.buildEtab(entity);

        assertGAREtabEquals(expectedGarEtab, garEtab);
    }

    @Test
    public void testGarEtabMandatoryFields() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTStructureUAI", Collections.singletonList("0354321B"));
        entity.put("ENTDisplayName", Collections.singletonList("École B"));

        GAREtab expectedGarEtab = new ObjectFactory().createGAREtab();

        expectedGarEtab.setGARStructureUAI("0354321B");
        expectedGarEtab.setGARStructureNomCourant("École B");
        expectedGarEtab.setGARStructureEmail("ce.0354321B@ac-rennes.fr");

        GAREtab garEtab = builder.buildEtab(entity);

        assertGAREtabEquals(expectedGarEtab, garEtab);
    }

    private void assertGAREtabEquals(GAREtab expected, GAREtab actual) {
        Assert.assertEquals(expected.getGARStructureUAI(), actual.getGARStructureUAI());
        Assert.assertEquals(expected.getGARStructureNomCourant(), actual.getGARStructureNomCourant());
        Assert.assertEquals(expected.getGARStructureContrat(), actual.getGARStructureContrat());
        Assert.assertEquals(expected.getGARStructureTelephone(), actual.getGARStructureTelephone());
        Assert.assertEquals(expected.getGARStructureEmail(), actual.getGARStructureEmail());
    }
}
