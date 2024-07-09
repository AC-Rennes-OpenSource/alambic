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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.LDAPSource;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARPersonGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.PersonGroupeEntity;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GAR1DGroupeBuilderTest extends GAR1DBuilderTestUtils {

    private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

    private GAR1DGroupeBuilder builder;

    private List<GARGroupe> builtGroupes;
    private List<GARPersonGroupe> builtPersonGroupes;

    @Before
    public void init() throws AlambicException, FileNotFoundException, JAXBException {
        initBuilder();

        mockWriter();
    }

    @BeforeClass
    public static void persistEntities() throws AlambicException {
        EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);

        List<PersonGroupeEntity> persistedEntities = new ArrayList<>();
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.TEACHER, "0351234A", "b6cb58e9-00c8-48db-b8bf-c02b6726c153", "43770"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "89bed3a9-ce5c-4e0d-8f1c-c5615d40b91b", "43770"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "48cd50d2-1796-4a59-b681-d3cc9d099bbb", "43770"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "6120db3f-bc15-4439-b6fa-c2ace93df676", "43770"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "066d9d0a-f4b4-4adc-a145-f089e7230caa", "43770"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.TEACHER, "0351234A", "29cb4aab-523d-4ca7-9ccf-8a23f3f5e4a2", "43771"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "6a89a236-7e09-4cdf-bfa9-033e160dc3fa", "43771"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "0d0e964f-7d71-475d-ad1f-f30980eab945", "43771"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "092e2723-f85b-4d80-b78e-65b91b062918", "43771"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "a2589231-a9f7-4c35-bd73-4b776ab1fda7", "43771"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.TEACHER, "0351234A", "052f50b2-1ebe-4e70-9b6c-4f641df472bb", "43773"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "28727473-6dc4-4940-928c-03e66d01316f", "43773"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "edf4246d-00a4-45cf-ad4f-1160d5d54419", "43773"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "84f80ee6-4e8f-4ac1-a715-89f78e989e79", "43773"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "0a041af0-fb42-4d92-9dce-61f3dc03a3dc", "43773"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, "0351234A", "0e41c7ae-bb5d-4bf6-abdb-80b991c74de9", "43773"));
        persistedEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.TEACHER, "0354321B", "5624a719-df8d-450f-9747-4f7168f84ca8", "1337"));
        EntityManager em = EntityManagerHelper.getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        persistedEntities.forEach(em::persist);
        transaction.commit();
    }

    private void mockWriter() throws FileNotFoundException, JAXBException {
        GAR1DENTWriter writer = PowerMockito.mock(GAR1DENTWriter.class);
        builder.writer = writer;

        builtGroupes = new ArrayList<>();
        builtPersonGroupes = new ArrayList<>();

        PowerMockito.doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgumentAt(0, Object.class);
            if (argument instanceof GARGroupe) {
                builtGroupes.add((GARGroupe) argument);
            } else if (argument instanceof GARPersonGroupe) {
                builtPersonGroupes.add((GARPersonGroupe) argument);
            }
            return null;
        }).when(writer).add(Matchers.any());
    }

    private void initBuilder() throws AlambicException {
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
                EntityManagerHelper.getEntityManager(),
                null);
        builder = new GAR1DGroupeBuilder(parameters);
    }

    @Test
    public void testGarGroupeFullyDefined() throws FileNotFoundException, MissingAttributeException, JAXBException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTStructureUAI", Collections.singletonList("0351234A"));
        entity.put("ENTStructureClasses", Stream.of(
                "43770$Classe 1",
                "43771$Classe 2",
                "43772$Classe 3",
                "43773$Classe 4"
        ).collect(Collectors.toList()));

        ObjectFactory factory = new ObjectFactory();

        List<GARGroupe> expectedGroupes = new ArrayList<>();
        List<GARPersonGroupe> expectedPersonGroupes = new ArrayList<>();

        Stream.of(
                new AbstractMap.SimpleEntry<>("43770", "Classe 1"),
                new AbstractMap.SimpleEntry<>("43771", "Classe 2"),
                new AbstractMap.SimpleEntry<>("43773", "Classe 4")
        ).collect(Collectors.toList()).forEach(entry -> {
            GARGroupe groupe = factory.createGARGroupe();
            groupe.setGARGroupeCode(entry.getKey());
            groupe.setGARGroupeLibelle(entry.getValue());
            groupe.setGARStructureUAI("0351234A");
            groupe.setGARGroupeStatut("DIVISION");
            expectedGroupes.add(groupe);
        });

        Stream.of(
                new AbstractMap.SimpleEntry<>("43770", "b6cb58e9-00c8-48db-b8bf-c02b6726c153"),
                new AbstractMap.SimpleEntry<>("43770", "89bed3a9-ce5c-4e0d-8f1c-c5615d40b91b"),
                new AbstractMap.SimpleEntry<>("43770", "48cd50d2-1796-4a59-b681-d3cc9d099bbb"),
                new AbstractMap.SimpleEntry<>("43770", "6120db3f-bc15-4439-b6fa-c2ace93df676"),
                new AbstractMap.SimpleEntry<>("43770", "066d9d0a-f4b4-4adc-a145-f089e7230caa"),
                new AbstractMap.SimpleEntry<>("43771", "29cb4aab-523d-4ca7-9ccf-8a23f3f5e4a2"),
                new AbstractMap.SimpleEntry<>("43771", "6a89a236-7e09-4cdf-bfa9-033e160dc3fa"),
                new AbstractMap.SimpleEntry<>("43771", "0d0e964f-7d71-475d-ad1f-f30980eab945"),
                new AbstractMap.SimpleEntry<>("43771", "092e2723-f85b-4d80-b78e-65b91b062918"),
                new AbstractMap.SimpleEntry<>("43771", "a2589231-a9f7-4c35-bd73-4b776ab1fda7"),
                new AbstractMap.SimpleEntry<>("43773", "052f50b2-1ebe-4e70-9b6c-4f641df472bb"),
                new AbstractMap.SimpleEntry<>("43773", "28727473-6dc4-4940-928c-03e66d01316f"),
                new AbstractMap.SimpleEntry<>("43773", "edf4246d-00a4-45cf-ad4f-1160d5d54419"),
                new AbstractMap.SimpleEntry<>("43773", "84f80ee6-4e8f-4ac1-a715-89f78e989e79"),
                new AbstractMap.SimpleEntry<>("43773", "0a041af0-fb42-4d92-9dce-61f3dc03a3dc"),
                new AbstractMap.SimpleEntry<>("43773", "0e41c7ae-bb5d-4bf6-abdb-80b991c74de9")
        ).collect(Collectors.toList()).forEach(entry -> {
            GARPersonGroupe personGroupe = factory.createGARPersonGroupe();
            personGroupe.setGARStructureUAI("0351234A");
            personGroupe.setGARGroupeCode(entry.getKey());
            personGroupe.setGARPersonIdentifiant(entry.getValue());
            expectedPersonGroupes.add(personGroupe);
        });

        builder.buildEntity(entity);

        assertListEquals(expectedGroupes, builtGroupes, this::assertGroupeListEquals);
        assertListEquals(expectedPersonGroupes, builtPersonGroupes, this::assertPersonGroupeEquals);
    }

    @Test
    public void testGarGroupeMandatoryAttributes() throws FileNotFoundException, MissingAttributeException, JAXBException {
        Map<String, List<String>> entity = new HashMap<>();
        entity.put("ENTStructureUAI", Collections.singletonList("0354321B"));
        entity.put("ENTStructureClasses", Collections.singletonList("1337$Classe"));

        ObjectFactory factory = new ObjectFactory();

        List<GARGroupe> expectedGroupes = new ArrayList<>();
        List<GARPersonGroupe> expectedPersonGroupes = new ArrayList<>();

        GARGroupe groupe = factory.createGARGroupe();
        groupe.setGARGroupeCode("1337");
        groupe.setGARGroupeLibelle("Classe");
        groupe.setGARStructureUAI("0354321B");
        groupe.setGARGroupeStatut("DIVISION");
        expectedGroupes.add(groupe);

        GARPersonGroupe personGroupe = factory.createGARPersonGroupe();
        personGroupe.setGARStructureUAI("0354321B");
        personGroupe.setGARGroupeCode("1337");
        personGroupe.setGARPersonIdentifiant("5624a719-df8d-450f-9747-4f7168f84ca8");
        expectedPersonGroupes.add(personGroupe);

        builder.buildEntity(entity);

        assertListEquals(expectedGroupes, builtGroupes, this::assertGroupeListEquals);
        assertListEquals(expectedPersonGroupes, builtPersonGroupes, this::assertPersonGroupeEquals);
    }

    private void assertGroupeListEquals(List<GARGroupe> expected, List<GARGroupe> actual) {
        List<GARGroupe> workExpected = expected.stream().sorted(Comparator.comparing(GARGroupe::getGARStructureUAI).thenComparing(GARGroupe::getGARGroupeCode)).collect(Collectors.toList());
        List<GARGroupe> workActual = actual.stream().sorted(Comparator.comparing(GARGroupe::getGARStructureUAI).thenComparing(GARGroupe::getGARGroupeCode)).collect(Collectors.toList());
        for (int i = 0; i < workExpected.size(); i++) {
            Assert.assertEquals(workExpected.get(i).getGARGroupeCode(), workActual.get(i).getGARGroupeCode());
            Assert.assertEquals(workExpected.get(i).getGARStructureUAI(), workActual.get(i).getGARStructureUAI());
            Assert.assertEquals(workExpected.get(i).getGARGroupeLibelle(), workActual.get(i).getGARGroupeLibelle());
            Assert.assertEquals(workExpected.get(i).getGARGroupeStatut(), workActual.get(i).getGARGroupeStatut());
            assertListEquals(workExpected.get(i).getGARGroupeDivAppartenance(), workActual.get(i).getGARGroupeDivAppartenance(), this::assertStringListEquals);
        }
    }

    private void assertPersonGroupeEquals(List<GARPersonGroupe> expected, List<GARPersonGroupe> actual) {
        List<GARPersonGroupe> workExpected = expected.stream().sorted(Comparator
                .comparing(GARPersonGroupe::getGARStructureUAI)
                .thenComparing(GARPersonGroupe::getGARGroupeCode)
                .thenComparing(GARPersonGroupe::getGARPersonIdentifiant))
                .collect(Collectors.toList());
        List<GARPersonGroupe> workActual = actual.stream().sorted(Comparator
                .comparing(GARPersonGroupe::getGARStructureUAI)
                .thenComparing(GARPersonGroupe::getGARGroupeCode)
                .thenComparing(GARPersonGroupe::getGARPersonIdentifiant))
                .collect(Collectors.toList());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(workExpected.get(i).getGARStructureUAI(), workActual.get(i).getGARStructureUAI());
            Assert.assertEquals(workExpected.get(i).getGARPersonIdentifiant(), workActual.get(i).getGARPersonIdentifiant());
            Assert.assertEquals(workExpected.get(i).getGARGroupeCode(), workActual.get(i).getGARGroupeCode());
        }
    }
}
