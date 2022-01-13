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
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GAREleve;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARPersonMEFSTAT4;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARPersonProfils;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GAR1DEleveBuilderTest extends GAR1DBuilderTestUtils {

    private GAR1DEleveBuilder builder;

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
                "",
                "",
                null,
                null);
        builder = new GAR1DEleveBuilder(parameters);
    }

    @Test
    public void testBuildGarEleveFullyDefined() throws MissingAttributeException, DatatypeConfigurationException, ParseException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("e6a4f485-ba7b-4f07-8e6a-23b4e1a04859"));
        entity.put("ENTPersonNomPatro", Collections.singletonList("Laurent"));
        entity.put("sn", Collections.singletonList("Laurent"));
        entity.put("givenName", Collections.singletonList("Eléa"));
        entity.put("ENTPersonAutresPrenoms", Stream.of("Eléa", "Claire", "Faustine").collect(Collectors.toList()));
        entity.put("ENTPersonSexe", Collections.singletonList("2"));
        entity.put("ENTPersonStructRattach", Collections.singletonList("0351234A"));
        entity.put("ENTEleveClasses", Collections.singletonList("0351234A$123456"));
        entity.put("ENTPersonDateNaissance", Collections.singletonList("10/02/2017"));
        entity.put("title", Collections.singletonList("ELE"));
        entity.put("ENTPersonNationalProfil", Collections.singletonList("National_1"));
        entity.put("ENTEleveMEF", Collections.singletonList("1111"));

        ObjectFactory factory = new ObjectFactory();
        GAREleve expectedGAREleve = factory.createGAREleve();
        expectedGAREleve.setGARPersonIdentifiant("e6a4f485-ba7b-4f07-8e6a-23b4e1a04859");
        GARPersonProfils profil = factory.createGARPersonProfils();
        profil.setGARStructureUAI("0351234A");
        profil.setGARPersonProfil("National_ELV");
        expectedGAREleve.getGARPersonProfils().add(profil);
        expectedGAREleve.setGARPersonNomPatro("Laurent");
        expectedGAREleve.setGARPersonNom("Laurent");
        expectedGAREleve.setGARPersonPrenom("Eléa");
        expectedGAREleve.getGARPersonAutresPrenoms().addAll(Stream.of("Eléa", "Claire", "Faustine").collect(Collectors.toList()));
        expectedGAREleve.setGARPersonCivilite("Mme");
        expectedGAREleve.setGARPersonStructRattach("0351234A");
        expectedGAREleve.getGARPersonEtab().add("0351234A");

        XMLGregorianCalendar xmlgc = getXmlGregorianCalendar("10/02/2017");
        expectedGAREleve.setGARPersonDateNaissance(xmlgc);

        GAREleve garEleve = builder.buildGarEleve(entity);
        Assert.assertTrue(garEleveEquals(expectedGAREleve, garEleve));

        List<GARPersonMEFSTAT4> garPersonMEFList = builder.buildGARPersonMEF(entity);
        Assert.assertTrue(garPersonMEFList.size() == 1
            && StringUtils.equals(garPersonMEFList.get(0).getGARPersonIdentifiant(), "e6a4f485-ba7b-4f07-8e6a-23b4e1a04859")
            && StringUtils.equals(garPersonMEFList.get(0).getGARStructureUAI(), "0351234A")
            && StringUtils.equals(garPersonMEFList.get(0).getGARMEFSTAT4Code(), "1111"));
    }

    private XMLGregorianCalendar getXmlGregorianCalendar(String formattedDate) throws ParseException, DatatypeConfigurationException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = dateFormat.parse(formattedDate);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
    }

    @Test
    public void testBuildGarEleveMandatoryFields() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("85d24cf6-02a2-4855-ab66-e4c6bd156c1c"));
        entity.put("sn", Collections.singletonList("Simon"));
        entity.put("givenName", Collections.singletonList("Rémy"));
        entity.put("ENTEleveClasses", Collections.singletonList("0354321B$654321"));
        entity.put("title", Collections.singletonList("ELE"));

        ObjectFactory factory = new ObjectFactory();
        GAREleve expectedGAREleve = factory.createGAREleve();
        expectedGAREleve.setGARPersonIdentifiant("85d24cf6-02a2-4855-ab66-e4c6bd156c1c");
        GARPersonProfils profil = factory.createGARPersonProfils();
        profil.setGARStructureUAI("0354321B");
        profil.setGARPersonProfil("National_ELV");
        expectedGAREleve.getGARPersonProfils().add(profil);
        expectedGAREleve.setGARPersonNom("Simon");
        expectedGAREleve.setGARPersonPrenom("Rémy");
        expectedGAREleve.getGARPersonAutresPrenoms().add("Rémy");
        expectedGAREleve.getGARPersonEtab().add("0354321B");

        GAREleve garEleve = builder.buildGarEleve(entity);
        Assert.assertTrue(garEleveEquals(expectedGAREleve, garEleve));

        Assert.assertEquals(0, builder.buildGARPersonMEF(entity).size());
    }

    private boolean garEleveEquals(GAREleve expected, GAREleve actual) {
        return Objects.equals(expected.getGARPersonIdentifiant(), actual.getGARPersonIdentifiant())
                && Objects.equals(expected.getGARPersonIdSecondaire(), actual.getGARPersonIdSecondaire())
                && Objects.equals(expected.getGARPersonNomPatro(), actual.getGARPersonNomPatro())
                && Objects.equals(expected.getGARPersonNom(), actual.getGARPersonNom())
                && Objects.equals(expected.getGARPersonPrenom(), actual.getGARPersonPrenom())
                && Objects.equals(expected.getGARPersonCivilite(), actual.getGARPersonCivilite())
                && Objects.equals(expected.getGARPersonStructRattach(), actual.getGARPersonStructRattach())
                && Objects.equals(expected.getGARPersonDateNaissance(), actual.getGARPersonDateNaissance())
                && listsEquals(expected.getGARPersonProfils(), actual.getGARPersonProfils(), this::garProfilsEquals)
                && listsEquals(expected.getGARPersonEtab(), actual.getGARPersonEtab(), this::stringListsEquals)
                && listsEquals(expected.getGARPersonAutresPrenoms(), actual.getGARPersonAutresPrenoms(), this::stringListsEquals);
    }
}
