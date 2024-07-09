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
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GAREnsSpecialitesPostes;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GAREnseignant;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARPersonProfils;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
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

public class GAR1DEnseignantBuilderTest extends GAR1DBuilderTestUtils {

    private GAR1DEnseignantBuilder builder;

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
                PowerMockito.mock(ActivityMBean.class),
                100,
                "1",
                "014",
                "",
                "",
                null,
                null);
        builder = new GAR1DEnseignantBuilder(parameters);
    }

    @Test
    public void testBuildGarEnseignantFullyDefined() throws MissingAttributeException, DatatypeConfigurationException, ParseException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("e34d32b6-d590-449c-a751-a096d88c08fc"));
        entity.put("ENTPersonNomPatro", Collections.singletonList("Vidal"));
        entity.put("sn", Collections.singletonList("Vidal"));
        entity.put("givenName", Collections.singletonList("Angelo"));
        entity.put("ENTPersonAutresPrenoms", Stream.of("Adrien", "Justin").collect(Collectors.toList()));
        entity.put("personalTitle", Collections.singletonList("M."));
        entity.put("ENTPersonStructRattach", Collections.singletonList("0351234A"));
        entity.put("ENTPersonFonctions", Stream.of("0351234A$ENS", "0351234A$DEC$1ORD", "0351234A$DEC", "0354321B$ENS").collect(Collectors.toList()));
        entity.put("ENTPersonDateNaissance", Collections.singletonList("04/03/1967"));
        entity.put("title", Collections.singletonList("ENS"));
        entity.put("ENTPersonNationalProfil", Collections.singletonList("National_3"));
        entity.put("mail", Stream.of("angelo.vidal@mail.fr", "avidal@mail.fr").collect(Collectors.toList()));
        entity.put("ENTAuxEnsDisciplinesPoste", Stream.of("0351234A$D0000", "0351234A$G0000", "0354321B$G0000").collect(Collectors.toList()));

        ObjectFactory factory = new ObjectFactory();
        GAREnseignant expectedGAREnseignant = factory.createGAREnseignant();
        expectedGAREnseignant.setGARPersonIdentifiant("e34d32b6-d590-449c-a751-a096d88c08fc");
        expectedGAREnseignant.getGARPersonProfils().add(createProfil("0351234A", "National_ENS"));
        expectedGAREnseignant.getGARPersonProfils().add(createProfil("0351234A", "National_DIR"));
        expectedGAREnseignant.getGARPersonProfils().add(createProfil("0354321B", "National_ENS"));
        expectedGAREnseignant.setGARPersonNomPatro("Vidal");
        expectedGAREnseignant.setGARPersonNom("Vidal");
        expectedGAREnseignant.setGARPersonPrenom("Angelo");
        expectedGAREnseignant.getGARPersonAutresPrenoms().addAll(Stream.of("Angelo", "Adrien", "Justin").collect(Collectors.toList()));
        expectedGAREnseignant.setGARPersonCivilite("M.");
        expectedGAREnseignant.setGARPersonStructRattach("0351234A");
        expectedGAREnseignant.getGARPersonEtab().add("0351234A");
        expectedGAREnseignant.getGARPersonEtab().add("0354321B");

        XMLGregorianCalendar xmlgc = getXmlGregorianCalendar("04/03/1967");
        expectedGAREnseignant.setGARPersonDateNaissance(xmlgc);

        expectedGAREnseignant.getGAREnsSpecialitesPostes().add(createPoste("0351234A", Stream.of("D0000", "G0000").collect(Collectors.toList())));
        expectedGAREnseignant.getGAREnsSpecialitesPostes().add(createPoste("0354321B", Collections.singletonList("G0000")));

        expectedGAREnseignant.getGARPersonMail().add("angelo.vidal@mail.fr");
        expectedGAREnseignant.getGARPersonMail().add("avidal@mail.fr");

        GAREnseignant garEnseignant = builder.buildGarEnseignant(entity);
        Assert.assertTrue(garEnseignantEquals(expectedGAREnseignant, garEnseignant));
    }

    private XMLGregorianCalendar getXmlGregorianCalendar(String formattedDate) throws ParseException, DatatypeConfigurationException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = dateFormat.parse(formattedDate);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
    }

    @Test
    public void testBuildGarEnseignantMandatoryFields() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("1cd83a84-4c5a-4a24-b0fb-949c9648feb8"));
        entity.put("sn", Collections.singletonList("Leroux"));
        entity.put("givenName", Collections.singletonList("Luna"));
        entity.put("ENTPersonFonctions", Collections.singletonList("0354321B$ENS"));
        entity.put("title", Collections.singletonList("ENS"));

        ObjectFactory factory = new ObjectFactory();
        GAREnseignant expectedGAREnseignant = factory.createGAREnseignant();
        expectedGAREnseignant.setGARPersonIdentifiant("1cd83a84-4c5a-4a24-b0fb-949c9648feb8");
        expectedGAREnseignant.getGARPersonProfils().add(createProfil("0354321B", "National_ENS"));
        expectedGAREnseignant.setGARPersonNom("Leroux");
        expectedGAREnseignant.setGARPersonPrenom("Luna");
        expectedGAREnseignant.getGARPersonAutresPrenoms().add("Luna");
        expectedGAREnseignant.getGARPersonEtab().add("0354321B");

        GAREnseignant garEnseignant = builder.buildGarEnseignant(entity);
        Assert.assertTrue(garEnseignantEquals(expectedGAREnseignant, garEnseignant));
    }

    @Test
    public void testBuildGarEnseignantXFonction() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("1cd83a84-4c5a-4a24-b0fb-949c9648feb8"));
        entity.put("sn", Collections.singletonList("Leroux"));
        entity.put("givenName", Collections.singletonList("Luna"));
        entity.put("ENTPersonFonctions", Stream.of("0354321B$ENS", "0351234A$X").collect(Collectors.toList()));
        entity.put("title", Collections.singletonList("ENS"));

        ObjectFactory factory = new ObjectFactory();
        GAREnseignant expectedGAREnseignant = factory.createGAREnseignant();
        expectedGAREnseignant.setGARPersonIdentifiant("1cd83a84-4c5a-4a24-b0fb-949c9648feb8");
        expectedGAREnseignant.getGARPersonProfils().add(createProfil("0354321B", "National_ENS"));
        expectedGAREnseignant.setGARPersonNom("Leroux");
        expectedGAREnseignant.setGARPersonPrenom("Luna");
        expectedGAREnseignant.getGARPersonAutresPrenoms().add("Luna");
        expectedGAREnseignant.getGARPersonEtab().add("0354321B");

        GAREnseignant garEnseignant = builder.buildGarEnseignant(entity);
        Assert.assertTrue(garEnseignantEquals(expectedGAREnseignant, garEnseignant));
    }

    @Test(expected = MissingAttributeException.class)
    public void testBuildGarEnseignantXFonctionOnly() throws MissingAttributeException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTPersonUid", Collections.singletonList("1cd83a84-4c5a-4a24-b0fb-949c9648feb8"));
        entity.put("sn", Collections.singletonList("Leroux"));
        entity.put("givenName", Collections.singletonList("Luna"));
        entity.put("ENTPersonFonctions", Collections.singletonList("0354321B$X"));
        entity.put("title", Collections.singletonList("ENS"));

        builder.buildGarEnseignant(entity);
    }

    private boolean garEnseignantEquals(GAREnseignant expected, GAREnseignant actual) {
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
                && listsEquals(expected.getGARPersonAutresPrenoms(), actual.getGARPersonAutresPrenoms(), this::stringListsEquals)
                && listsEquals(expected.getGAREnsSpecialitesPostes(), actual.getGAREnsSpecialitesPostes(), this::garPostesEquals)
                && listsEquals(expected.getGARPersonMail(), actual.getGARPersonMail(), this::stringListsEquals);
    }

    private boolean garPostesEquals(List<GAREnsSpecialitesPostes> expected, List<GAREnsSpecialitesPostes> actual) {
        // Par construction on garantit que les listes sont non nulles et ont la même taille
        List<GAREnsSpecialitesPostes> workExpected = expected.stream().sorted(Comparator.comparing(GAREnsSpecialitesPostes::getGARStructureUAI)).collect(Collectors.toList());
        List<GAREnsSpecialitesPostes> workActual = actual.stream().sorted(Comparator.comparing(GAREnsSpecialitesPostes::getGARStructureUAI)).collect(Collectors.toList());
        boolean result = true;
        for (int i = 0; i < workExpected.size(); i++) {
            result = result
                    && StringUtils.equals(workExpected.get(i).getGARStructureUAI(), workActual.get(i).getGARStructureUAI())
                    && listsEquals(workExpected.get(i).getGAREnsSpecialitePosteCode(), workActual.get(i).getGAREnsSpecialitePosteCode(), this::stringListsEquals);
        }
        return result;
    }

    private GARPersonProfils createProfil(String uai, String profil) {
        GARPersonProfils personProfil = new ObjectFactory().createGARPersonProfils();
        personProfil.setGARStructureUAI(uai);
        personProfil.setGARPersonProfil(profil);
        return personProfil;
    }

    private GAREnsSpecialitesPostes createPoste(String uai, List<String> codes) {
        GAREnsSpecialitesPostes poste = new ObjectFactory().createGAREnsSpecialitesPostes();
        poste.setGARStructureUAI(uai);
        poste.getGAREnsSpecialitePosteCode().addAll(codes);
        return poste;
    }
}
