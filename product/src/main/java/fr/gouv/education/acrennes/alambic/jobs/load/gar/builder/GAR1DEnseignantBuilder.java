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
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GAR1DEnseignantBuilder extends GAR1DIdentiteBuilder {
    private static final Log log = LogFactory.getLog(GAR1DEnseignantBuilder.class);

    // Attributes applying to all teachers
    private final List<Map<String, List<String>>> teachers;
    private final Map<String, Document> exportFiles;
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    public GAR1DEnseignantBuilder(GARBuilderParameters parameters) {
        super(parameters);
        teachers = parameters.resources().get("Entries").getEntries();
        exportFiles = parameters.exportFiles();
    }

    private void resetVariables() {
        structRattach = null;
        personIdentifiant = null;
    }

    @Override
    protected void setGARCivilite(GARIdentite garIdentite, Map<String, List<String>> entity) {
        handleOptionalAttribute(entity, "personalTitle",
                personalTitle -> garIdentite.setGARPersonCivilite(GARHelper.getInstance().getSDETCompliantTitleValue(personalTitle)));
    }

    @Override
    protected void setGARDateNaissance(GARIdentite garIdentite, XMLGregorianCalendar xmlgc) {
        ((GAREnseignant) garIdentite).setGARPersonDateNaissance(xmlgc);
    }

    @Override
    protected void setWriter() throws JAXBException, SAXException {
        writer = new GARENTEnseignantWriter(factory, version, page, maxNodesCount, output, xsdFile);
    }

    @Override
    protected List<Map<String, List<String>>> getEntries() {
        return teachers;
    }

    @Override
    protected boolean checkRestriction(Map<String, List<String>> entity) throws MissingAttributeException, XPathExpressionException {
        if (null != exportFiles.get("restrictionList")) {
            Element root = exportFiles.get("restrictionList").getDocumentElement();
            String matchingEntry = (String) xpath.evaluate("//id[.='" + getMandatoryAttribute(entity, "ENTPersonJointure") + "']", root,
                    XPathConstants.STRING);
            return StringUtils.isNotBlank(matchingEntry);
        } else {
            return true;
        }
    }

    @Override
    protected void buildEntity(Map<String, List<String>> entity) throws MissingAttributeException, FileNotFoundException, JAXBException {
        resetVariables();

        writer.add(buildGarEnseignant(entity));

        //TODO GARMEFStat4
    }

    public GAREnseignant buildGarEnseignant(Map<String, List<String>> entity) throws MissingAttributeException {
        GAREnseignant garEnseignant = factory.createGAREnseignant();

        // Set fields corresponding to GARIdentite (common with GAREleve)
        buildIdentite(garEnseignant, entity);

        handleOptionalList(entity, "mail", mail -> {
            if (StringUtils.isNotBlank(mail)) {
                garEnseignant.getGARPersonMail().add(mail.trim());
            }
        });

        List<String> functionCodes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(entity.get("ENTPersonFonctions"))) {
            entity.get("ENTPersonFonctions").forEach(personFonction -> {
                if (StringUtils.isNotBlank(personFonction) && personFonction.matches("[^\\$]+\\$[^\\$]+.*")) {
                    String uai = GARHelper.getInstance().extractCodeGroup(personFonction, 0);
                    String profile = GARHelper.getInstance().extractCodeGroup(personFonction, 1);
                    addEtabAndProfils(entity, garEnseignant, uai, profile, functionCodes);
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTPersonFonctions' with not " +
                             "regular value");
                }
            });
        } else {
            // Add profile based on "title" attribute
            if (StringUtils.isNotBlank(structRattach)) {
                addEtabAndProfils(entity, garEnseignant, structRattach.toUpperCase(), null, functionCodes);
            } else {
                log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonFonctions' or " +
                         "'ENTPersonStructRattach'");
            }
        }

        // Add GARPersonProfil (not GARPersonEtab) associated to the administrative structure if not already present
        if (StringUtils.isNotBlank(structRattach) && functionCodes.stream().noneMatch(item -> item.matches(structRattach.concat("\\$.+")))) {
            handleOptionalAttribute(entity, "ENTPersonNationalProfil", nationalProfil -> {
                String sdetcnpv = GARHelper.getInstance().getSDETCompliantProfileValue(entity.get("title").get(0), nationalProfil);

                if (StringUtils.isNotBlank(sdetcnpv)) {
                    GARPersonProfils pf = factory.createGARPersonProfils();
                    pf.setGARStructureUAI(structRattach);
                    pf.setGARPersonProfil(sdetcnpv);
                    if (!garEnseignant.getGARPersonProfils().contains(pf)) {
                        garEnseignant.getGARPersonProfils().add(pf);
                    }
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no national profile that could be " +
                             "associated with the title '" +
                             entity.get("title").get(0) + "' and ENTPersonNationalProfil '" + nationalProfil + "'");
                }
            }, () -> {
                jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonNationalProfil' " +
                         "(or empty)");
            });
        }

        // Control presence of GARPersonProfil
        if (garEnseignant.getGARPersonProfils().isEmpty()) {
            throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no " +
                                                "attribute 'GARPersonProfils' (mandatory)");
        }

        // GAREnsSpecialitesPostes
        Map<String, GAREnsSpecialitesPostes> garEnsSpecialitesPostesMap = new HashMap<>();
        handleOptionalList(entity, "ENTAuxEnsDisciplinesPoste", poste -> addSpecialitesPoste(entity, functionCodes, garEnsSpecialitesPostesMap,
                poste));
        garEnseignant.getGAREnsSpecialitesPostes().addAll(garEnsSpecialitesPostesMap.values());
        return garEnseignant;
    }

    private void addSpecialitesPoste(Map<String, List<String>> entity, List<String> functionCodes,
                                     Map<String, GAREnsSpecialitesPostes> garEnsSpecialitesPostesMap, String poste) {
        if (StringUtils.isNotBlank(poste)) {
            String uai = GARHelper.getInstance().extractCodeGroup(poste, 0).toUpperCase();
            if (functionCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
                String code = GARHelper.getInstance().extractCodeGroup(poste, 1);
                if (StringUtils.isNotBlank(code)) {
                    garEnsSpecialitesPostesMap.computeIfAbsent(uai, s -> {
                        GAREnsSpecialitesPostes esp = factory.createGAREnsSpecialitesPostes();
                        esp.setGARStructureUAI(s);
                        return esp;
                    });
                    GAREnsSpecialitesPostes postes = garEnsSpecialitesPostesMap.get(uai);
                    postes.getGAREnsSpecialitePosteCode().add(code);
                } else {
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no code associated to one " +
                             "discipline (value is '" + poste + "') in attribute 'ENTAuxEnsDisciplinesPoste' (mandatory)");
                }
            } else {
                log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsDisciplinesPoste' pointing" +
                         " onto structure ('UAI:" + uai + "') that is not referenced by 'ENTPersonFonctions'");
            }
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsDisciplinesPoste' with blank " +
                      "value");
        }
    }

    private void addEtabAndProfils(Map<String, List<String>> entity, GAREnseignant garEnseignant, String uai, String profile,
                                   List<String> functionCodes) {
        String sdetcnpv = GARHelper.getInstance().getSDETCompliantProfileValue(entity.get("title").get(0), profile);

        if (StringUtils.isNotBlank(sdetcnpv) && !("X".equals(profile))) {
            /*
             * GAR platform does not support all profiles.
             * Hence, the following statements implement security by filtering supported profiles.
             */
            if (!GARHelper.NATIONAL_PROFILE_IDENTIFIER.valueOf(sdetcnpv).isSupported()) {
                log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' is teaching but has a national profile '" + sdetcnpv + "' not supported by GAR. Is replaced by '" +
                         GARHelper.NATIONAL_PROFILE_IDENTIFIER.National_ENS + "'");
                sdetcnpv = GARHelper.NATIONAL_PROFILE_IDENTIFIER.National_ENS.toString();
            }

            // Control the UAI belongs to the involved structures list + was not already processed (functional key is : uai + national profile)
            if (memberStructuresList.contains(uai)) {
                String fctKey = uai + "$" + sdetcnpv;
                if (!functionCodes.contains(fctKey)) {
                    functionCodes.add(fctKey);

                    if (!garEnseignant.getGARPersonEtab().contains(uai)) {
                        garEnseignant.getGARPersonEtab().add(uai);
                    }

                    GARPersonProfils pf = factory.createGARPersonProfils();
                    pf.setGARStructureUAI(uai);
                    pf.setGARPersonProfil(sdetcnpv);
                    garEnseignant.getGARPersonProfils().add(pf);
                }
            } else {
                log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' is member of a structure ('UAI:" + uai + "') out " +
                         "of the involved list");
            }
        } else {
            jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
            log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no national profile that could be associated with " +
                     "the profile '" + profile + "'");
        }
    }

    private static class GARENTEnseignantWriter extends GAR1DENTWriter {
        private GARENTEnseignant container;

        protected GARENTEnseignantWriter(ObjectFactory factory, String version, int page, int maxNodesCount, String output, String xsdFile)
                throws JAXBException, SAXException {
            super(factory, version, page, maxNodesCount, output);
            container = factory.createGARENTEnseignant();
            container.setVersion(version);
            setMarshallerFrom(JAXBContext.newInstance(GARENTEnseignant.class), xsdFile);
        }

        @Override
        protected void add(Object item) throws FileNotFoundException, JAXBException {
            if (item instanceof GAREnseignant) {
                container.getGAREnseignant().add((GAREnseignant) item);
            } else if (item instanceof GARPersonMEFSTAT4) {
                container.getGARPersonMEFSTAT4().add((GARPersonMEFSTAT4) item);
            }
            checkNodeCount();
        }

        @Override
        protected void marshal(int increment) throws FileNotFoundException, JAXBException {
            JAXBElement<GARENTEnseignant> jaxbElement = factory.createGARENTEnseignant(container);
            marshal(increment, jaxbElement);
            container = factory.createGARENTEnseignant();
            container.setVersion(version);
        }
    }
}
