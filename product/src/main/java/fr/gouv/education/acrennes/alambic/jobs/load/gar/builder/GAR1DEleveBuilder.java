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

import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.*;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.PersonGroupeEntity;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.persistence.EntityTransaction;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GAR1DEleveBuilder extends GAR1DIdentiteBuilder {

    private static final Log log = LogFactory.getLog(GAR1DEleveBuilder.class);

    // Attributes applying to all students
    private final List<Map<String, List<String>>> students;
    private final Map<String, Document> exportFiles;
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    public GAR1DEleveBuilder(GARBuilderParameters parameters) {
        super(parameters);
        students = parameters.getResources().get("Entries").getEntries();
        exportFiles = parameters.getExportFiles();
    }

    private void resetVariables() {
        structRattach = null;
        personIdentifiant = null;
    }

    @Override
    protected void setGARCivilite(GARIdentite garIdentite, Map<String, List<String>> entity) {
        handleOptionalAttribute(entity, "ENTPersonSexe", value -> garIdentite.setGARPersonCivilite("1".equals(value) ? "M." : "Mme"));
    }

    @Override
    protected void setGARDateNaissance(GARIdentite garIdentite, XMLGregorianCalendar xmlgc) {
        ((GAREleve) garIdentite).setGARPersonDateNaissance(xmlgc);
    }

    @Override
    protected void setWriter() throws JAXBException, SAXException {
        writer = new GARENTEleveWriter(factory, version, page, maxNodesCount, output, xsdFile);
    }

    @Override
    protected List<Map<String, List<String>>> getEntries() {
        return students;
    }

    @Override
    protected boolean checkRestriction(Map<String, List<String>> entity) throws MissingAttributeException, XPathExpressionException {
        if (null != exportFiles.get("restrictionList")) {
            Element root = exportFiles.get("restrictionList").getDocumentElement();
            String matchingEntry = (String) xpath.evaluate("//id[.='" + getMandatoryAttribute(entity, "ENTPersonJointure") + "']", root, XPathConstants.STRING);
            return StringUtils.isNotBlank(matchingEntry);
        } else {
            return true;
        }
    }

    @Override
    protected void buildEntity(Map<String, List<String>> entity) throws MissingAttributeException, FileNotFoundException, JAXBException {
        resetVariables();
        writer.add(buildGarEleve(entity));
        List<GARPersonMEFSTAT4> mefList = buildGARPersonMEF(entity);
        for (GARPersonMEFSTAT4 mef: mefList) {
            writer.add(mef);
        }

        // Handle persistence (for GroupeBuilder later)
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        getPersonGroupeEntities(entity).forEach(em::persist);
        transaction.commit();
    }

    public GAREleve buildGarEleve(Map<String, List<String>> entity) throws MissingAttributeException {
        GAREleve garEleve = factory.createGAREleve();

        // Set fields corresponding to GARIdentite (common with GAREnseignant)
        buildIdentite(garEleve, entity);

        // Set GARPersonProfils and GARPersonEtab
        List<String> functionCodes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(entity.get("ENTEleveClasses"))) {
            // Iterate over UAI extracted from ENTEleveClasses
            entity.get("ENTEleveClasses").stream().filter(StringUtils::isNotBlank).map(value -> GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase()).forEach(uai -> {
                // Check if the UAI belongs to the involved structures list
                if (memberStructuresList.contains(uai)) {
                    // Check if it was already processed
                    if (!functionCodes.contains(uai)) {
                        functionCodes.add(uai);

                        addEtabAndProfils(entity, garEleve, uai, null, true);
                    }
                } else {
                    log.info("Student with blur identifier '"+ GARHelper.getInstance().getPersonEntityBlurId(entity) + "' is member of a structure ('UAI:" + uai + "') out of the involved list");
                }
            });
        } else {
			throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTEleveClasses'");
        }

        // Add GARPersonProfil associated to the administrative structure if not already present
        if (StringUtils.isNotBlank(structRattach) && !functionCodes.contains(structRattach)) {
            handleOptionalAttribute(entity, "ENTPersonNationalProfil", nationalProfil -> addEtabAndProfils(entity, garEleve, structRattach, nationalProfil, false), () -> {
                jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonNationalProfil' (or empty)");
            });
        }

        // Make sure at least one GARPersonProfil exists
        if (garEleve.getGARPersonProfils().isEmpty()) {
            throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'GARPersonProfils' (mandatory)");
        }

        return garEleve;
    }

    public List<GARPersonMEFSTAT4> buildGARPersonMEF(Map<String, List<String>> entity) {
        List<String> mefCodes = new ArrayList<>();
        List<GARPersonMEFSTAT4> mefList = new ArrayList<>();
        handleOptionalList(entity, "ENTEleveMEF", mefCode -> {
            if (StringUtils.isNotBlank(mefCode) && !mefCodes.contains(mefCode)) {
                mefCodes.add(mefCode);
                mefList.add(buildPersonMEF(mefCode));
            }
        });
        return mefList;
    }

    public List<PersonGroupeEntity> getPersonGroupeEntities(Map<String, List<String>> entity) {
        List<PersonGroupeEntity> personGroupeEntities = new ArrayList<>();

        handleOptionalList(entity, "ENTEleveClasses", classe -> {
            if (StringUtils.isNotBlank(classe)) {
                String uai = GARHelper.getInstance().extractCodeGroup(classe, 0);
                String groupeCode = GARHelper.getInstance().extractCodeGroup(classe, 1);
                boolean alreadyExists = personGroupeEntities.stream().anyMatch(personGroupeEntity -> personGroupeEntity.getUai().equals(uai) && personGroupeEntity.getGroupeCode().equals(groupeCode));
                if (StringUtils.isNotBlank(uai) && StringUtils.isNotBlank(groupeCode) && !alreadyExists) {
                    personGroupeEntities.add(new PersonGroupeEntity(PersonGroupeEntity.PERSON_TYPE.STUDENT, uai, personIdentifiant, groupeCode));
                }
            }
        });

        return personGroupeEntities;
    }

    private GARPersonMEFSTAT4 buildPersonMEF(String mefCode) {
        GARPersonMEFSTAT4 personMEF = factory.createGARPersonMEFSTAT4();
        personMEF.setGARStructureUAI(structRattach);
        personMEF.setGARMEFSTAT4Code(mefCode);
        personMEF.setGARPersonIdentifiant(personIdentifiant);

        return personMEF;
    }

    private void addEtabAndProfils(Map<String, List<String>> entity, GAREleve garEleve, String uai, String profil, boolean addGarEtab) {
        // Add GARPersonEtab structure if needed
        if (addGarEtab && !garEleve.getGARPersonEtab().contains(uai)) {
            garEleve.getGARPersonEtab().add(uai);
        }

        // Add GARPersonProfil structure
        String sdetcnpv = GARHelper.getInstance().getSDETCompliantProfileValue(entity.get("title").get(0), profil);
        if (StringUtils.isNotBlank(sdetcnpv)) {
            GARPersonProfils pf = factory.createGARPersonProfils();
            pf.setGARStructureUAI(uai);
            pf.setGARPersonProfil(sdetcnpv);
            garEleve.getGARPersonProfils().add(pf);
        }
    }

    private static class GARENTEleveWriter extends GAR1DENTWriter {

        private GARENTEleve container;

        protected GARENTEleveWriter(final ObjectFactory factory, final String version, final int page, final int maxNodesCount, String output, String xsdFile) throws JAXBException, SAXException {
            super(factory, version, page, maxNodesCount, output);
            container = factory.createGARENTEleve();
            container.setVersion(version);
            setMarshallerFrom(JAXBContext.newInstance(GARENTEleve.class), xsdFile);
        }

        @Override
        protected void add(Object item) throws FileNotFoundException, JAXBException {
            if (item instanceof GAREleve) {
                container.getGAREleve().add((GAREleve) item);
            } else if (item instanceof GARPersonMEFSTAT4) {
                container.getGARPersonMEFSTAT4().add((GARPersonMEFSTAT4) item);
            }
            checkNodeCount();
        }

        @Override
        protected void marshal(int increment) throws FileNotFoundException, JAXBException {
            JAXBElement<GARENTEleve> jaxbElement = factory.createGARENTEleve(container);
            marshal(increment, jaxbElement);
            container = factory.createGARENTEleve();
            container.setVersion(version);
        }
    }
}
