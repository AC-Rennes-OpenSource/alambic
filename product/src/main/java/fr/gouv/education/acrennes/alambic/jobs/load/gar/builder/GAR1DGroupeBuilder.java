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

import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARENTGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARPersonGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.PersonGroupeEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GAR1DGroupeBuilder extends GAR1DBuilder {

    private static final Log log = LogFactory.getLog(GAR1DGroupeBuilder.class);
    private final List<Map<String, List<String>>> structures;
    private List<String> divCodes;

    public GAR1DGroupeBuilder(GARBuilderParameters parameters) {
        super(parameters);
        structures = parameters.getResources().get("Entries").getEntries();
    }

    @Override
    protected void setWriter() throws JAXBException, SAXException {
        writer = new GARENTGroupeWriter(factory, version, page, maxNodesCount, output, xsdFile);
    }

    @Override
    protected List<Map<String, List<String>>> getEntries() {
        return structures;
    }

    @Override
    protected boolean checkRestriction(Map<String, List<String>> entity) {
        return true;
    }

    @Override
    protected void buildEntity(Map<String, List<String>> entity) throws MissingAttributeException, FileNotFoundException, JAXBException {
        List<GARGroupe> groupes = buildGroupes(entity);
        List<GARPersonGroupe> personGroupes = buildPersonGroupes(getMandatoryAttribute(entity, "ENTStructureUAI"));

        // Writing only groups that have associated persons
        List<GARGroupe> filteredGroupes = groupes.stream()
                .filter(garGroupe -> personGroupes.stream().map(GARPersonGroupe::getGARGroupeCode).anyMatch(garGroupe.getGARGroupeCode()::equals))
                .collect(Collectors.toList());
        for (GARGroupe groupe : filteredGroupes) {
            writer.add(groupe);
        }
        for (GARPersonGroupe personGroupe : personGroupes) {
            writer.add(personGroupe);
        }
    }

    public List<GARGroupe> buildGroupes(Map<String, List<String>> entity) throws MissingAttributeException {
        List<GARGroupe> groupes = new ArrayList<>();
        divCodes = new ArrayList<>();
        String uai = getMandatoryAttribute(entity, "ENTStructureUAI");
        handleOptionalList(entity, "ENTStructureClasses", groupe -> {
            if (StringUtils.isNotBlank(groupe)) {
                String code = GARHelper.getInstance().extractCodeGroup(groupe, 0);
                if (!divCodes.contains(code)) {
                    divCodes.add(code);
                    String libelle = GARHelper.getInstance().extractCodeGroup(groupe, 1);
                    GARGroupe garGroupe = factory.createGARGroupe();
                    garGroupe.setGARStructureUAI(uai);
                    garGroupe.setGARGroupeStatut("DIVISION");
                    garGroupe.setGARGroupeCode(code);
                    garGroupe.setGARGroupeLibelle(StringUtils.isNotBlank(libelle) ? libelle : code);
                    groupes.add(garGroupe);
                }
            }
        });

        return groupes;
    }

    public List<GARPersonGroupe> buildPersonGroupes(String uai) {
        List<GARPersonGroupe> garPersonGroupes = new ArrayList<>();
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        Query emQuery = em.createQuery("SELECT pg FROM PersonGroupeEntity pg WHERE pg.uai = :uai");
        emQuery.setParameter("uai", uai);
        @SuppressWarnings("unchecked")
        List<PersonGroupeEntity> personGroupes = emQuery.getResultList();
        transaction.commit();

        personGroupes.forEach(personGroupe -> {
            if (divCodes.contains(personGroupe.getGroupeCode())) {
                GARPersonGroupe garPersonGroupe = factory.createGARPersonGroupe();
                garPersonGroupe.setGARStructureUAI(personGroupe.getUai());
                garPersonGroupe.setGARPersonIdentifiant(personGroupe.getPersonIdentifiant());
                garPersonGroupe.setGARGroupeCode(personGroupe.getGroupeCode());
                garPersonGroupes.add(garPersonGroupe);
            } else {
                log.warn("Filtered groupCode element from entity (ENTPersonUid=" + personGroupe.getPersonIdentifiant() + ") since it references a code '" + personGroupe.getGroupeCode() + "' absent from the UAI '" + uai + "'");
            }
        });

        return garPersonGroupes;
    }

    private static class GARENTGroupeWriter extends GAR1DENTWriter {

        private GARENTGroupe container;

        protected GARENTGroupeWriter(ObjectFactory factory, String version, int page, int maxNodesCount, String output, String xsdFile) throws JAXBException, SAXException {
            super(factory, version, page, maxNodesCount, output);
            container = factory.createGARENTGroupe();
            container.setVersion(version);
            setMarshallerFrom(JAXBContext.newInstance(GARENTGroupe.class), xsdFile);
        }

        @Override
        protected void add(Object item) throws FileNotFoundException, JAXBException {
            if (item instanceof GARGroupe) {
                container.getGARGroupe().add((GARGroupe) item);
            } else if (item instanceof GARPersonGroupe) {
                container.getGARPersonGroupe().add((GARPersonGroupe) item);
            }
            checkNodeCount();
        }

        @Override
        protected void marshal(int increment) throws FileNotFoundException, JAXBException {
            JAXBElement<GARENTGroupe> jaxbElement = factory.createGARENTGroupe(container);
            marshal(increment, jaxbElement);
            container = factory.createGARENTGroupe();
            container.setVersion(version);
        }
    }
}
