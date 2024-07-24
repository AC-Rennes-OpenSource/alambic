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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.*;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.EnseignementEntity;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.StaffEntity;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.StaffEntityPK;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class GAREnseignantBuilder implements GARTypeBuilder {
    private static final Log log = LogFactory.getLog(GAREnseignantBuilder.class);
    private static final Pattern GAR_ENS_CLASS_GROUP_VALIDITY_PATTERN = Pattern.compile("[^\\$]+\\$[^\\$]+\\$[^\\$]+");

    private final int page;
    private final int maxNodesCount;
    private final String output;
    private final String xsdFile;
    private final String version;
    private final String territoryCode;
    private final ActivityMBean jobActivity;
    private final EntityManager em;
    private final Map<String, Document> exportFiles;
    private final XPath xpath;
    private final List<Map<String, List<String>>> teachers;
    private final List<String> memberStructuresList;
    private final Source aafSource;

    public GAREnseignantBuilder(GARBuilderParameters parameters) {
        this.page = parameters.page();
        this.jobActivity = parameters.jobActivity();
        this.maxNodesCount = parameters.maxNodesCount();
        this.version = parameters.version();
        this.territoryCode = parameters.territoryCode();
        this.output = parameters.output();
        this.em = parameters.em();
        this.exportFiles = parameters.exportFiles();
        XPathFactory xpf = XPathFactory.newInstance();
        this.xpath = xpf.newXPath();
        this.xsdFile = parameters.xsdFile();
        this.teachers = parameters.resources().get("Entries").getEntries(); // Get the list of involved teachers
        Source structuresSource = parameters.resources().get("Structures"); // Get the list of involved structures
        this.aafSource = parameters.resources().get("AAF");
        this.memberStructuresList = new ArrayList<>();
        List<Map<String, List<String>>> structures = structuresSource.getEntries();
        structures.forEach(structure -> {
            if (null != structure.get("ENTStructureUAI") && 1 == structure.get("ENTStructureUAI").size()) {
                this.memberStructuresList.add(structure.get("ENTStructureUAI").get(0).toUpperCase());
            }
        });
    }

    @Override
    public void execute() throws AlambicException {
        try {
            ObjectFactory factory = new ObjectFactory();
            GAREnseignantWriter writer = new GAREnseignantWriter(factory, version, page, maxNodesCount);

            // Iterates over teachers
            for (int index = 0; index < this.teachers.size(); index++) {
                // activity monitoring
                jobActivity.setProgress(((index + 1) * 100) / this.teachers.size());
                jobActivity.setProcessing("processing entry " + (index + 1) + "/" + this.teachers.size());

                Map<String, List<String>> entity = this.teachers.get(index);

                handleTeacher(entity, factory, writer);
            }

            // Flush the possibly remaining entities
            writer.flush();
        } catch (JAXBException | FileNotFoundException | SAXException | XPathExpressionException e) {
            jobActivity.setTrafficLight(ActivityTrafficLight.RED);
            log.error("Failed to execute the GAR loader, error: " + (StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getCause()));
        }

    }

    private void handleTeacher(Map<String, List<String>> entity, ObjectFactory factory, GAREnseignantWriter writer)
            throws XPathExpressionException, FileNotFoundException, JAXBException, AlambicException {
        List<String> attribute;
        try {
            /* Check the current entity belongs to list of those being taken into account.
             * This control is useful for processing the anonymized data among possibly added
             * entities via LDAP update daily scripts.
             */
            if (null != this.exportFiles.get("restrictionList")) {
                String ENTPersonJointure = getMandatoryAttribute(entity, "ENTPersonJointure");
                Element root = exportFiles.get("restrictionList").getDocumentElement();
                String matchingEntry = (String) xpath.evaluate("//id[.='" + ENTPersonJointure + "']", root, XPathConstants.STRING);
                if (StringUtils.isBlank(matchingEntry)) {
                    log.debug("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' since it doesn't belong to the " +
                              "restriction list");
                    return;
                }
            }

            GAREnseignant garEnseignant = factory.createGAREnseignant();

            /*
             * Determine the source SI
             */
            String ENTPersonSourceSI = getMandatoryAttribute(entity, "ENTPersonSourceSI");

            /*
             * GARPersonIdentifiant
             */
            String ENTPersonIdentifiant = getMandatoryAttribute(entity, "ENTPersonUid");
            garEnseignant.setGARPersonIdentifiant(ENTPersonIdentifiant);


            /*
             * GARPersonIdSecondaire
             * Not implemented in this version.
             */

            List<String> functionsCodes = new ArrayList<>();

            handleTeacherAttributes(garEnseignant, entity, factory, functionsCodes);

            writer.add(garEnseignant);

            Map<String, List<EnseignementEntity>> mapEnseignements = new HashMap<>();

            /*
             * GARMEFCode
             */
            List<String> mefCodes = new ArrayList<>();
            attribute = entity.get("ENTAuxEnsMef");
            if (CollectionUtils.isNotEmpty(attribute)) {
                for (String value : attribute) {
                    if (StringUtils.isNotBlank(value) && !mefCodes.contains(value)) {
                        String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                        // Control the UAI belongs to the exercising structures (teacher has functions into)
                        if (functionsCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
                            /* Control the code is valid indeed
                             * (Since it has been observed teachers' Toutatice accounts referencing invalid codes (AAF meaning) )
                             */
                            String code = GARHelper.getInstance().extractCodeGroup(value, 1);
                            if (GARHelper.getInstance().isCodeValid(this.aafSource, ENTPersonSourceSI, this.territoryCode,
                                    GARHelper.INDEXATION_OBJECT_TYPE.MEF, code)) {
                                // register for persistence
                                if (!mapEnseignements.containsKey(uai)) {
                                    mapEnseignements.put(uai, new ArrayList<>());
                                }
                                List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
                                EnseignementEntity enseignement = new EnseignementEntity(ENTPersonSourceSI, code, "unset" /* will be obtained from
                                request on AAF */, EnseignementEntity.ENSEIGNEMENT_TYPE.MEF);
                                if (!enseignements.contains(enseignement)) {
                                    enseignements.add(enseignement);
                                }

                                GARPersonMEF pmef = factory.createGARPersonMEF();
                                pmef.setGARStructureUAI(uai);
                                pmef.setGARMEFCode(code);
                                pmef.setGARPersonIdentifiant(ENTPersonIdentifiant);
                                writer.add(pmef);
                            } else {
                                // skip this code since it is not valid
                                log.info("Failed to get the 'MEF' information associated to the code '" + code + "' (ENTPersonUid=" + ENTPersonIdentifiant + ", UAI=" + uai + ")");
                            }
                        } else {
                            log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsMef' pointing " +
                                     "onto structure ('UAI:" + uai + "') that is not referenced by 'ENTPersonFonctions'");
                        }
                        mefCodes.add(value); // Make sure the functional key (UAI / code / id) is respected
                    } else {
                        log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsMef' with blank " +
                                  "value");
                    }
                }
            } else {
                log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsMef'");
            }

            /*
             * Classes (divisions) "matières"
             * Out of GAR grammar. This code purpose is to register the "matieres" associated to
             * structures so that structure & groups data is made easier to fill later.
             */
            attribute = entity.get("ENTAuxEnsClassesMatieres");
            if (CollectionUtils.isNotEmpty(attribute)) {
                for (String value : attribute) {
                    if (StringUtils.isNotBlank(value) && GAR_ENS_CLASS_GROUP_VALIDITY_PATTERN.matcher(value).matches()) {
                        String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                        // Control the UAI belongs to the exercising structures (teacher has functions into)
                        if (functionsCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
                            /* Control the code is valid indeed
                             * (Since it has been observed teachers' Toutatice accounts referencing invalid codes (AAF meaning) )
                             */
                            String code = GARHelper.getInstance().extractCodeGroup(value, 2);
                            if (GARHelper.getInstance().isCodeValid(this.aafSource, ENTPersonSourceSI, this.territoryCode,
                                    GARHelper.INDEXATION_OBJECT_TYPE.Matiere, code)) {
                                // register for persistence
                                if (!mapEnseignements.containsKey(uai)) {
                                    mapEnseignements.put(uai, new ArrayList<>());
                                }

                                String divOrGrpCode = GARHelper.getInstance().extractCodeGroup(value, 1);
                                List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
                                EnseignementEntity enseignement = new EnseignementEntity(ENTPersonSourceSI, code, divOrGrpCode,
                                        EnseignementEntity.ENSEIGNEMENT_TYPE.CLASSE_MATIERE);
                                if (!enseignements.contains(enseignement)) {
                                    enseignements.add(enseignement);
                                }
                            } else {
                                // skip this code since it is not valid
                                log.info("Failed to get the 'Matiere' information associated to the code '" + code + "' (ENTPersonUid=" + ENTPersonIdentifiant + ", UAI=" + uai + ")");
                            }
                        } else {
                            log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute " +
                                     "'ENTAuxEnsClassesMatieres' pointing onto structure ('UAI:" + uai + "') that is not referenced by " +
                                     "'ENTPersonFonctions'");
                        }
                    } else {
                        log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsClassesMatieres' " +
                                  "with blank value or not fitting the pattern '*$*$*$*'");
                    }
                }
            } else {
                log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsClassesMatieres'");
            }

            /*
             * Groupes "matières" (belong to divisions)
             * Out of GAR grammar. This programmatic code purpose is to register the "matieres" associated
             * to structures so that structure & groups data is easier to fill.
             */
            attribute = entity.get("ENTAuxEnsGroupesMatieres");
            if (CollectionUtils.isNotEmpty(attribute)) {
                for (String value : attribute) {
                    if (StringUtils.isNotBlank(value) && GAR_ENS_CLASS_GROUP_VALIDITY_PATTERN.matcher(value).matches()) {
                        String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                        // Control the UAI belongs to the exercising structures (teacher has functions into)
                        if (functionsCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
                            /* Control the code is valid indeed
                             * (Since it has been observed teachers' Toutatice accounts referencing invalid codes (AAF meaning) )
                             */
                            String code = GARHelper.getInstance().extractCodeGroup(value, 2);
                            if (GARHelper.getInstance().isCodeValid(this.aafSource, ENTPersonSourceSI, this.territoryCode,
                                    GARHelper.INDEXATION_OBJECT_TYPE.Matiere, code)) {
                                // register for persistence
                                if (!mapEnseignements.containsKey(uai)) {
                                    mapEnseignements.put(uai, new ArrayList<>());
                                }

                                String divOrGrpCode = GARHelper.getInstance().extractCodeGroup(value, 1);
                                List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
                                EnseignementEntity enseignement = new EnseignementEntity(ENTPersonSourceSI, code, divOrGrpCode,
                                        EnseignementEntity.ENSEIGNEMENT_TYPE.GROUPE_MATIERE);
                                if (!enseignements.contains(enseignement)) {
                                    enseignements.add(enseignement);
                                }
                            } else {
                                // skip this code since it is not valid
                                log.info("Failed to get the 'Matiere' information associated to the code '" + code + "' (ENTPersonUid=" + ENTPersonIdentifiant + ", UAI=" + uai + ")");
                            }
                        } else {
                            log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute " +
                                     "'ENTAuxEnsGroupesMatieres' pointing onto structure ('UAI:" + uai + "') that is not referenced by " +
                                     "'ENTPersonFonctions'");
                        }
                    } else {
                        log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsGroupesMatieres' " +
                                  "with blank value or not fitting the pattern '*$*$*$*'");
                    }
                }
            } else {
                log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsGroupesMatieres'");
            }

            // Persist the tuples (teacher, structure) in DB
            for (Map.Entry<String, List<EnseignementEntity>> uai : mapEnseignements.entrySet()) {
                StaffEntityPK pk = new StaffEntityPK(uai.getKey(), ENTPersonIdentifiant);
                StaffEntity enseignant = new StaffEntity(pk, uai.getValue(), StaffEntity.STAFF_TYPE.TEACHER);
                EntityTransaction transac = em.getTransaction();
                transac.begin();
                em.persist(enseignant);
                transac.commit();
            }
        } catch (MissingAttributeException e) {
            jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
            log.warn(e.getMessage()); // skip this entity as a missing mandatory field won't allow XML production
        }
    }

    private void handleTeacherAttributes(GAREnseignant garEnseignant, Map<String, List<String>> entity, ObjectFactory factory,
                                         List<String> functionsCodes)
            throws MissingAttributeException {
        List<String> attribute;
        /*
         * GARPersonCivilite
         */
        attribute = entity.get("personalTitle");
        if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
            garEnseignant.setGARPersonCivilite(GARHelper.getInstance().getSDETCompliantTitleValue(attribute.get(0)));
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'personalTitle'");
        }

        /*
         * GARPersonStructRattach
         */
        String ENTPersonStructRattach = null;
        attribute = entity.get("ENTPersonStructRattach");
        if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
            /* To notice : set empty UAI if it doesn't belong to the involved structures list.
             * Use case : a teacher teaches in (a) structure(s) different from the administrative one.
             */
            String uai = attribute.get(0).toUpperCase();
            ENTPersonStructRattach = ((this.memberStructuresList.contains(uai)) ? uai : "");
            garEnseignant.setGARPersonStructRattach(ENTPersonStructRattach);
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonStructRattach'");
        }

        /*
         * GARPersonDateNaissance
         */
        attribute = entity.get("ENTPersonDateNaissance");
        if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                Date date = dateFormat.parse(attribute.get(0));
                GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(date);
                XMLGregorianCalendar xmlgc = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
                garEnseignant.setGARPersonDateNaissance(xmlgc);
            } catch (ParseException | DatatypeConfigurationException e) {
                jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                log.warn("Failed to parse the attribute 'ENTPersonDateNaissance', might not match the following expected date format 'dd/MM/yyyy', " +
                         "error: " + e.getMessage());
            }
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonDateNaissance'");
        }

        /*
         * GARPersonMail
         */
        attribute = entity.get("mail");
        if (CollectionUtils.isNotEmpty(attribute)) {
            attribute.stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(email -> garEnseignant.getGARPersonMail().add(email.trim()));
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'mail'");
        }

        /*
         * GARPersonNom
         */
        garEnseignant.setGARPersonNom(getMandatoryAttribute(entity, "sn"));

        /*
         * GARPersonPrenom
         */
        String givenName = getMandatoryAttribute(entity, "givenName");
        garEnseignant.setGARPersonPrenom(givenName);
        garEnseignant.getGARPersonAutresPrenoms().add(givenName); // le prénom usuel doit figurer parmi les "autres" prénoms

        /*
         * GARPersonAutresPrenoms
         */
        attribute = entity.get("ENTPersonAutresPrenoms");
        if (CollectionUtils.isNotEmpty(attribute)) {
            for (String value : attribute) {
                if (StringUtils.isNotBlank(value) && !garEnseignant.getGARPersonAutresPrenoms().contains(value)) {
                    garEnseignant.getGARPersonAutresPrenoms().add(value);
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTPersonAutresPrenoms' with " +
                              "blank value");
                }
            }
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonAutresPrenoms'");
        }

        /*
         * GARPersonNomPatro
         */
        attribute = entity.get("ENTPersonNomPatro");
        if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
            garEnseignant.setGARPersonNomPatro(attribute.get(0));
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonNomPatro'");
        }

        /*
         * GARPersonEtab & GARPersonProfil
         */
        functionsCodes.clear();
        attribute = entity.get("ENTPersonFonctions");
        if (CollectionUtils.isNotEmpty(attribute)) {
            for (String value : attribute) {
                // control the attribute value is relevant (not null and well formated)
                if (StringUtils.isNotBlank(value) && value.matches("[^\\$]+\\$[^\\$]+.*")) {
                    String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                    String profile = GARHelper.getInstance().extractCodeGroup(value, 1).toUpperCase();
                    handlePersonFonction(garEnseignant, entity, factory, functionsCodes, uai, profile);
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTPersonFonctions' with not " +
                             "regular value");
                }
            }
        } else {
            /*
             * Ajout du profil basé sur l'attribut title
             */
            if (StringUtils.isNotBlank(ENTPersonStructRattach)) {
                handlePersonFonction(garEnseignant, entity, factory, functionsCodes, ENTPersonStructRattach.toUpperCase(), null);
            } else {
                log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonFonctions' or " +
                         "'ENTPersonStructRattach'");
            }
        }

        // Add GARPersonProfil (not GARPersonEtab) associated to the administrative structure if not already present
        final String controlUAI = ENTPersonStructRattach;
        if (StringUtils.isNotBlank(ENTPersonStructRattach) && functionsCodes.stream().noneMatch(item -> item.matches(controlUAI.concat("\\$.+")))) {
            attribute = entity.get("ENTPersonNationalProfil");
            if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
                // get SDET compliant national profile value
                String sdetcnpv = GARHelper.getInstance().getSDETCompliantProfileValue(entity.get("title").get(0), attribute.get(0));
                if (StringUtils.isNotBlank(sdetcnpv)) {
                    GARPersonProfils pf = factory.createGARPersonProfils();
                    pf.setGARStructureUAI(ENTPersonStructRattach);
                    pf.setGARPersonProfil(sdetcnpv);
                    if (!garEnseignant.getGARPersonProfils().contains(pf)) {
                        garEnseignant.getGARPersonProfils().add(pf);
                    }
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no national profile that could be " +
                             "associated with the title '" + entity.get("title").get(0) + "' and ENTPersonNationalProfil '" + attribute.get(0) +
                             "'");
                }
            } else {
                jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonNationalProfil' " +
                         "(or empty)");
            }
        }

        /*
         * Contrôler que les attributs obligatoires GARPersonEtab & GARPersonProfil sont présents.
         * (pourraient être absents du fait du filtrage réalisé sur les structures pilotes)
         */
        if (garEnseignant.getGARPersonEtab().isEmpty() || garEnseignant.getGARPersonProfils().isEmpty()) {
            throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has either " +
                                                "no attribute 'GARPersonEtab' or 'GARPersonProfils' (mandatory)");
        }

        /*
         * GAREnsDisciplinesPostes
         */
        attribute = entity.get("ENTAuxEnsDisciplinesPoste");
        if (CollectionUtils.isNotEmpty(attribute)) {
            handleEnsDisciplinesPoste(garEnseignant, entity, factory, functionsCodes, attribute);
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsDisciplinesPoste'");
        }
    }

    private void handleEnsDisciplinesPoste(GAREnseignant garEnseignant, Map<String, List<String>> entity, ObjectFactory factory,
                                           List<String> functionsCodes, List<String> attribute)
            throws MissingAttributeException {
        Map<String, GAREnsDisciplinesPostes> map = new HashMap<>();
        for (String value : attribute) {
            if (StringUtils.isNotBlank(value)) {
                String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                // Control the UAI belongs to the exercising structures (teacher has functions into)
                if (functionsCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
                    String code = GARHelper.getInstance().extractCodeGroup(value, 1);
                    if (StringUtils.isNotBlank(code)) {
                        if (!map.containsKey(uai)) {
                            GAREnsDisciplinesPostes edp = factory.createGAREnsDisciplinesPostes();
                            edp.setGARStructureUAI(uai);
                            map.put(uai, edp);
                        }
                        GAREnsDisciplinesPostes postes = map.get(uai);
                        postes.getGAREnsDisciplinePosteCode().add(code);
                    } else {
                        // Since XSD version 1.5.2, code is mandatory
                        throw new MissingAttributeException(
                                "Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no code associated to " +
                                "one discipline (value is '" + value + "') in attribute 'ENTAuxEnsDisciplinesPoste' (mandatory)");
                    }
                } else {
                    log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsDisciplinesPoste' " +
                             "pointing onto structure ('UAI:" + uai + "') that is not referenced by 'ENTPersonFonctions'");
                }
            } else {
                log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsDisciplinesPoste' with " +
                          "blank value");
            }
        }

        for (Map.Entry<String, GAREnsDisciplinesPostes> uai : map.entrySet()) {
            garEnseignant.getGAREnsDisciplinesPostes().add(uai.getValue());
        }
    }

    private void handlePersonFonction(GAREnseignant garEnseignant, Map<String, List<String>> entity, ObjectFactory factory,
                                      List<String> functionsCodes, String uai, String profile) {
        // get SDET compliant national profile value based on both the title and function values
        String sdetcnpv = GARHelper.getInstance().getSDETCompliantProfileValue(entity.get("title").get(0), profile);

        if (StringUtils.isNotBlank(sdetcnpv) && !("X".equals(profile))) {
            /*
             * GAR platform does not support all profiles.
             * Hence, the following statements implement security by filtering supported profiles.
             */
            if (!GARHelper.NATIONAL_PROFILE_IDENTIFIER.valueOf(sdetcnpv).isSupported()) {
                log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' is teaching but has a national profile '" + sdetcnpv + "' not supported by GAR. Is replaced by '" + GARHelper.NATIONAL_PROFILE_IDENTIFIER.National_ENS + "'");
                sdetcnpv = GARHelper.NATIONAL_PROFILE_IDENTIFIER.National_ENS.toString();
            }

            // Control the UAI belongs to the involved structures list + was not already processed (functional key is : uai + national profile)
            if (this.memberStructuresList.contains(uai)) {
                String fctKey = uai + "$" + sdetcnpv;
                if (!functionsCodes.contains(fctKey)) {
                    functionsCodes.add(fctKey);

                    // Add GARPersonEtab structure (if not already present)
                    if (!garEnseignant.getGARPersonEtab().contains(uai)) {
                        garEnseignant.getGARPersonEtab().add(uai);
                    }

                    // Add GARPersonProfil structure
                    GARPersonProfils pf = factory.createGARPersonProfils();
                    pf.setGARStructureUAI(uai);
                    pf.setGARPersonProfil(sdetcnpv);
                    garEnseignant.getGARPersonProfils().add(pf);
                } // functional key respect : don't process the same function twice for the same structure
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

    private String getMandatoryAttribute(Map<String, List<String>> entity, String attributeName) throws MissingAttributeException {
        List<String> attribute = entity.get(attributeName);
        if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
            return attribute.get(0);
        } else {
            throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no " +
                                                "attribute '" + attributeName + "' (mandatory)");
        }
    }

    private class GAREnseignantWriter {

        private final ObjectFactory factory;
        private final Marshaller marshaller;
        private final int maxNodesCount;
        private final String version;
        private final int page;
        private int nodeCount;
        private GARENTEnseignant container;

        protected GAREnseignantWriter(final ObjectFactory factory, final String version, final int page, final int maxNodesCount)
                throws JAXBException, SAXException {
            this.factory = factory;
            this.version = version;
            this.page = page;
            this.maxNodesCount = maxNodesCount;
            nodeCount = 0;
            container = factory.createGARENTEnseignant();
            container.setVersion(version);
            JAXBContext context = JAXBContext.newInstance(GARENTEnseignant.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            // Install schema validation
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdFile));
            marshaller.setSchema(schema);
        }

        protected <T> GARENTEnseignant add(final T item) throws FileNotFoundException, JAXBException {
            if (item instanceof GAREnseignant) {
                container.getGAREnseignant().add((GAREnseignant) item);
            } else if (item instanceof GARPersonMEF) {
                container.getGARPersonMEF().add((GARPersonMEF) item);
            }

            // Check the file limit size is reached
            nodeCount++;
            if (0 == (nodeCount % maxNodesCount)) {
                marshal(nodeCount / maxNodesCount);
            }

            return container;
        }

        protected void flush() throws FileNotFoundException, JAXBException {
            marshal((nodeCount / maxNodesCount) + 1);
        }

        // Marshal the XML binding
        private void marshal(final int increment) throws FileNotFoundException, JAXBException {
            String outputFileName = GARHelper.getInstance().getOutputFileName(output, page, increment);
            JAXBElement<GARENTEnseignant> jaxbElt = factory.createGARENTEnseignant(container);
            marshaller.marshal(jaxbElt, new FileOutputStream(outputFileName));
            container = factory.createGARENTEnseignant();
            container.setVersion(version);
        }

    }

}
