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
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARRespAff;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GAR1DRespAffBuilderTest extends GAR1DBuilderTestUtils {

    private GAR1DRespAffBuilder builder;

    @Before
    public void init() {
        Source mockedStructures = PowerMockito.mock(LDAPSource.class);
        Source mockedEntries = PowerMockito.mock(LDAPSource.class);
        List<Map<String, List<String>>> structuresMaps = new ArrayList<>();
        Stream.of("0351234A", "0354321B", "0352143C").forEach(uai -> structuresMaps.add(Collections.singletonMap("ENTStructureUAI",
                Collections.singletonList(uai))));
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
        builder = new GAR1DRespAffBuilder(parameters);
    }

    @Test
    public void testBuildGarRespAffFullyDefined() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("eaf65dbb-800e-47dd-99e8-044c3fe7f6c1"));
        entity.put("sn", Collections.singletonList("Garcia"));
        entity.put("givenName", Collections.singletonList("Fabien"));
        entity.put("personalTitle", Collections.singletonList("M."));
        entity.put("ENTPersonProfils", Stream.of(
                "cn=0351234A_GAR1DRespAff,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr",
                "cn=0351234A_ens,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr",
                "cn=0354321B_GAR1DRespAff,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr",
                "cn=0354321B_ens,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr",
                "cn=0352143C_ens,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr",
                "cn=0356789D_GAR1DRespAff,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr"
        ).collect(Collectors.toList()));
        entity.put("mail", Stream.of(
                "fabien.garcia@example.com",
                "fgarcia@example.com"
        ).collect(Collectors.toList()));

        GARRespAff expectedGarRespAff = new ObjectFactory().createGARRespAff();

        expectedGarRespAff.setGARPersonIdentifiant("eaf65dbb-800e-47dd-99e8-044c3fe7f6c1");
        expectedGarRespAff.setGARPersonNom("Garcia");
        expectedGarRespAff.setGARPersonPrenom("Fabien");
        expectedGarRespAff.setGARPersonCivilite("M.");
        expectedGarRespAff.getGARRespAffEtab().addAll(Stream.of("0351234A", "0354321B").collect(Collectors.toList()));
        expectedGarRespAff.getGARPersonMail().addAll(Stream.of("fabien.garcia@example.com", "fgarcia@example.com").collect(Collectors.toList()));

        GARRespAff garRespAff = builder.buildRespAff(entity);

        Assert.assertTrue(garRespAffEquals(expectedGarRespAff, garRespAff));
    }

    @Test
    public void testBuildGarRespAffMandatoryFields() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("993053f3-58ec-4291-85c8-640945247e57"));
        entity.put("sn", Collections.singletonList("Willis"));
        entity.put("givenName", Collections.singletonList("Debra"));
        entity.put("ENTPersonProfils", Collections.singletonList("cn=0351234A_GAR1DRespAff,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr"));
        entity.put("mail", Collections.singletonList("debra.willis@example.com"));

        GARRespAff expectedGarRespAff = new ObjectFactory().createGARRespAff();

        expectedGarRespAff.setGARPersonIdentifiant("993053f3-58ec-4291-85c8-640945247e57");
        expectedGarRespAff.setGARPersonNom("Willis");
        expectedGarRespAff.setGARPersonPrenom("Debra");
        expectedGarRespAff.getGARRespAffEtab().add("0351234A");
        expectedGarRespAff.getGARPersonMail().add("debra.willis@example.com");

        GARRespAff garRespAff = builder.buildRespAff(entity);

        Assert.assertTrue(garRespAffEquals(expectedGarRespAff, garRespAff));
    }

    private boolean garRespAffEquals(GARRespAff expected, GARRespAff actual) {
        return StringUtils.equals(expected.getGARPersonIdentifiant(), actual.getGARPersonIdentifiant()) &&
               StringUtils.equals(expected.getGARPersonNom(), actual.getGARPersonNom()) &&
               StringUtils.equals(expected.getGARPersonPrenom(), actual.getGARPersonPrenom()) &&
               StringUtils.equals(expected.getGARPersonCivilite(), actual.getGARPersonCivilite()) &&
               listsEquals(expected.getGARRespAffEtab(), actual.getGARRespAffEtab(), this::stringListsEquals) &&
               listsEquals(expected.getGARPersonMail(), actual.getGARPersonMail(), this::stringListsEquals);
    }

}
