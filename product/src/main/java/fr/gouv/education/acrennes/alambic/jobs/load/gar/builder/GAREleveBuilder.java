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
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.EnseignementEntity;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.StaffEntity;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.StaffEntityPK;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
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

public class GAREleveBuilder implements GARTypeBuilder {
    private static final Log log = LogFactory.getLog(GAREleveBuilder.class);
    private static final Pattern GAR_ELE_CLASS_GROUP_VALIDITY_PATTERN = Pattern.compile("[^\\$]+\\$[^\\$]+(\\$.*)?");

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
    private final List<Map<String, List<String>>> students;
    private final List<String> memberStructuresList;
    private final Source aafSource;

    public GAREleveBuilder(GARBuilderParameters parameters) {
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
        this.students = parameters.resources().get("Entries").getEntries(); // Get the list of involved students
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
            List<String> attribute;
            ObjectFactory factory = new ObjectFactory();
            GAREleveWriter writer = new GAREleveWriter(factory, version, page, maxNodesCount);
            List<String> codes = new ArrayList<>();
            List<String> functionsCodes = new ArrayList<>();
            List<String> divsNGrps = new ArrayList<>();

            // Iterate over students
            for (int index = 0; index < this.students.size(); index++) {
                // activity monitoring
                jobActivity.setProgress(((index + 1) * 100) / this.students.size());
                jobActivity.setProcessing("processing entry " + (index + 1) + "/" + this.students.size());

                Map<String, List<String>> entity = this.students.get(index);

                /* Check the current entity belongs to list of those being taken into account.
                 * This control is useful for processing the anonymized data among possibly added
                 * entities via LDAP update daily scripts.
                 */
                if (null != this.exportFiles.get("restrictionList")) {
                    attribute = entity.get("ENTPersonJointure");
                    if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                        Element root = exportFiles.get("restrictionList").getDocumentElement();
                        String matchingEntry = (String) xpath.evaluate("//id[.='" + attribute.get(0) + "']", root, XPathConstants.STRING);
                        if (StringUtils.isBlank(matchingEntry)) {
                            log.debug("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' since it doesn't belong to " +
                                      "the restriction list");
                            continue;
                        }
                    } else {
                        jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                        log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute " +
                                 "'ENTPersonJointure' (mandatory)");
                        continue; // skip this entity as a missing mandatory field won't allow XML production
                    }
                }

                GAREleve garEleve = factory.createGAREleve();
                String ENTPersonStructRattach = null;
                String ENTPersonIdentifiant = null;
                String ENTPersonSourceSI = null;
                Map<String, List<EnseignementEntity>> mapEnseignements = new HashMap<>();

                /*
                 * Determine the source SI
                 */
                attribute = entity.get("ENTPersonSourceSI");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    ENTPersonSourceSI = attribute.get(0);
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute " +
                             "'ENTPersonSourceSI' (mandatory)");
                    continue; // skip this entity as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonIdentifiant
                 */
                attribute = entity.get("ENTPersonUid");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    ENTPersonIdentifiant = attribute.get(0);
                    garEleve.setGARPersonIdentifiant(ENTPersonIdentifiant);
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonUid'" +
                             " (mandatory)");
                    continue; // skip this entity as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonIdSecondaire
                 * Not implemented in this version.
                 */

                /*
                 * GARPersonCivilite
                 */
                attribute = entity.get("ENTPersonSexe");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    garEleve.setGARPersonCivilite(("1".equals(attribute.get(0))) ? "M." : "Mme");
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonSexe'");
                }

                /*
                 * GARPersonStructRattach
                 */
                attribute = entity.get("ENTPersonStructRattach");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    /* To notice : set empty UAI if it doesn't belong to the involved structures list.
                     */
                    String uai = attribute.get(0).toUpperCase();
                    ENTPersonStructRattach = ((this.memberStructuresList.contains(uai)) ? uai : "");
                    garEleve.setGARPersonStructRattach(ENTPersonStructRattach);
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute " +
                             "'ENTPersonStructRattach'");
                    continue; // skip this entry as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonDateNaissance
                 */
                attribute = entity.get("ENTPersonDateNaissance");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        Date date = dateFormat.parse(attribute.get(0));
                        GregorianCalendar gc = new GregorianCalendar();
                        gc.setTime(date);
                        XMLGregorianCalendar xmlgc = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
                        garEleve.setGARPersonDateNaissance(xmlgc);
                    } catch (ParseException | DatatypeConfigurationException e) {
                        jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                        log.warn("Failed to parse the attribute 'ENTPersonDateNaissance', might not match the following expected date format " +
                                 "'dd/MM/yyyy', error: " + e.getMessage());
                    }
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonDateNaissance'");
                }

                /*
                 * GARPersonNom
                 */
                attribute = entity.get("sn");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    garEleve.setGARPersonNom(attribute.get(0));
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'sn' " +
                             "(mandatory)");
                    continue; // skip this entity as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonPrenom
                 */
                attribute = entity.get("givenName");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    garEleve.setGARPersonPrenom(attribute.get(0));
                    garEleve.getGARPersonAutresPrenoms().add(attribute.get(0)); // le prénom usuel doit figurer parmi les "autres" prénoms
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'givenName' " +
                             "(mandatory)");
                    continue; // skip this entity as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonAutresPrenoms
                 */
                attribute = entity.get("ENTPersonAutresPrenoms");
                if (null != attribute && !attribute.isEmpty()) {
                    for (String value : attribute) {
                        if (StringUtils.isNotBlank(value) && !garEleve.getGARPersonAutresPrenoms().contains(value)) {
                            garEleve.getGARPersonAutresPrenoms().add(value);
                        } else {
                            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute " +
                                      "'ENTPersonAutresPrenoms' with blank value");
                        }
                    }
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonAutresPrenoms'");
                }

                /*
                 * GARPersonNomPatro
                 */
                attribute = entity.get("ENTPersonNomPatro");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    garEleve.setGARPersonNomPatro(attribute.get(0));
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonNomPatro'");
                }

                /*
                 * GARPersonEtab & GARPersonProfil
                 *
                 * Le script se base sur les attributs "ENTEleveClasses" et "ENTEleveGroupes" afin de lister les établissements où l'élève
                 * suit des enseignements. À la différence des enseignants, l'attribut "ENTPersonFonctions" présent dans l'annuaire applicatif
                 * Toutatice ne contient pas la liste des établissements "d'exercice" mais rappelle l'UAI de l'établissement d'affectation
                 * administrative (ce qui est le même dans la grande majorité).
                 */
                // Agrégation des classes et groupes dans lesquels l'élève est membre
                divsNGrps.clear();
                if (null != entity.get("ENTEleveClasses")) {
                    divsNGrps.addAll(entity.get("ENTEleveClasses"));
                }
                if (null != entity.get("ENTEleveGroupes")) {
                    divsNGrps.addAll(entity.get("ENTEleveGroupes"));
                }
                if (!divsNGrps.isEmpty()) {
                    functionsCodes.clear();
                    for (String value : divsNGrps) {
                        if (StringUtils.isNotBlank(value)) {
                            String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                            // Control the UAI belongs to the involved structures list
                            if (this.memberStructuresList.contains(uai)) {
                                // Control it was not already processed
                                if (!functionsCodes.contains(uai)) {
                                    functionsCodes.add(uai);

                                    // Add GARPersonEtab structure (if not already present)
                                    if (!garEleve.getGARPersonEtab().contains(uai)) {
                                        garEleve.getGARPersonEtab().add(uai);
                                    }

                                    // Add GARPersonProfil structure

                                    // get SDET compliant national profile value (le titre est utilisé (mono valué) car il est considéré qu'un
                                    // élève ne peut pas avoir d'autre fonction (DOC...))
                                    String sdetcnpv = GARHelper.getInstance().getSDETCompliantProfileValue(entity.get("title").get(0), null);
                                    if (StringUtils.isNotBlank(sdetcnpv)) {
                                        GARPersonProfils pf = factory.createGARPersonProfils();
                                        pf.setGARStructureUAI(uai);
                                        pf.setGARPersonProfil(sdetcnpv);
                                        garEleve.getGARPersonProfils().add(pf);
                                    } else {
                                        jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                                        log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no national profil " +
                                                 "that could be associated with the title '" + entity.get("title").get(0) + "'");
                                    }
                                }
                            } else {
                                log.info("Student with blur identifier '" + ENTPersonIdentifiant + "' is member of a structure ('UAI:" + uai + "') " +
                                         "out of the involved list");
                            }
                        } else {
                            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has either attribute " +
                                      "'ENTEleveClasses' or 'ENTEleveGroupes' with blank value");
                        }
                    }
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has neither attribute " +
                             "'ENTEleveClasses' nor 'ENTEleveGroupes' (mandatory)");
                    continue; // skip this entity as a missing mandatory field won't allow XML production
                }

                // Add GARPersonProfil (not GARPersonEtab) associated to the administrative structure if not already present
                if (StringUtils.isNotBlank(ENTPersonStructRattach) && !functionsCodes.contains(ENTPersonStructRattach)) {
                    attribute = entity.get("ENTPersonNationalProfil");
                    if (null != attribute && StringUtils.isNotBlank(attribute.get(0))) {
                        // get SDET compliant national profile value
                        String sdetcnpv = GARHelper.getInstance().getSDETCompliantProfileValue(entity.get("title").get(0), attribute.get(0));
                        if (StringUtils.isNotBlank(sdetcnpv)) {
                            GARPersonProfils pf = factory.createGARPersonProfils();
                            pf.setGARStructureUAI(ENTPersonStructRattach);
                            pf.setGARPersonProfil(sdetcnpv);
                            if (!garEleve.getGARPersonProfils().contains(pf)) {
                                garEleve.getGARPersonProfils().add(pf);
                            }
                        } else {
                            jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                            log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no national profil that could be " +
                                     "associated with the title '" + entity.get("title").get(0) + "' and ENTPersonNationalProfil '" + attribute.get(0) + "'");
                        }
                    } else {
                        jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                        log.warn("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute " +
                                 "'ENTPersonNationalProfil' (or empty)");
                    }
                }

                /*
                 * Contrôler que les attributs obligatoires GARPersonEtab & GARPersonProfil sont présents.
                 * (pourraient être absents du fait du filtrage réalisé sur les structures pilotes)
                 */
                if (garEleve.getGARPersonEtab().isEmpty() || garEleve.getGARPersonProfils().isEmpty()) {
                    // skip this entity as a missing mandatory field won't allow XML production
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has either no attribute " +
                             "'GARPersonEtab' or 'GARPersonProfils' (mandatory)");
                    continue; // skip this entity as a missing mandatory field won't allow XML production
                }

                writer.add(garEleve);

                /*
                 * GARPersonMEF
                 */
                codes.clear();
                attribute = entity.get("ENTEleveMEF");
                if (null != attribute && !attribute.isEmpty()) {
                    for (String value : attribute) {
                        if (StringUtils.isNotBlank(value) && !codes.contains(value)) {
                            /* Control the code is valid indeed
                             * (Since it has been observed teachers' Toutatice accounts referencing invalid codes (AAF meaning) )
                             */
                            if (GARHelper.getInstance().isCodeValid(this.aafSource, ENTPersonSourceSI, this.territoryCode,
                                    GARHelper.INDEXATION_OBJECT_TYPE.MEF, value)) {
                                GARPersonMEF pmef = factory.createGARPersonMEF();
                                pmef.setGARStructureUAI(ENTPersonStructRattach);
                                pmef.setGARMEFCode(value);
                                pmef.setGARPersonIdentifiant(ENTPersonIdentifiant);
                                writer.add(pmef);

                                // register for persistence
                                if (!mapEnseignements.containsKey(ENTPersonStructRattach)) {
                                    mapEnseignements.put(ENTPersonStructRattach, new ArrayList<>());
                                }

                                List<EnseignementEntity> enseignements = mapEnseignements.get(ENTPersonStructRattach);
                                EnseignementEntity enseignement = new EnseignementEntity(ENTPersonSourceSI, value, "unset" /* will be obtained from
                                 request on AAF */, EnseignementEntity.ENSEIGNEMENT_TYPE.MEF);
                                if (!enseignements.contains(enseignement)) {
                                    enseignements.add(enseignement);
                                }
                            } else {
                                // skip this code since it is not valid
                                log.info("Failed to get the 'MEF' information associated to the code '" + value + "' (ENTPersonUid=" + ENTPersonIdentifiant + ")");
                            }
                            codes.add(value); // make sure the functional key (code / UAI / id) is respected
                        } else {
                            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTEleveMEF' with " +
                                      "blank value");
                        }
                    }
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTEleveMEF'");
                }

                /*
                 * GAREleveEnseignement
                 */
                codes.clear();
                attribute = entity.get("ENTEleveCodeEnseignements");
                if (null != attribute && !attribute.isEmpty()) {
                    for (String value : attribute) {
                        if (StringUtils.isNotBlank(value) && !codes.contains(value)) {
                            /* Control the code is valid indeed
                             * (Since it has been observed teachers' Toutatice accounts referencing invalid codes (AAF meaning) )
                             */
                            if (GARHelper.getInstance().isCodeValid(this.aafSource, ENTPersonSourceSI, this.territoryCode,
                                    GARHelper.INDEXATION_OBJECT_TYPE.Matiere, value)) {
                                GAREleveEnseignement eens = factory.createGAREleveEnseignement();
                                eens.setGARMatiereCode(value);
                                eens.setGARPersonIdentifiant(ENTPersonIdentifiant);
                                eens.setGARStructureUAI(ENTPersonStructRattach);
                                writer.add(eens);

                                // register for persistence
                                if (!mapEnseignements.containsKey(ENTPersonStructRattach)) {
                                    mapEnseignements.put(ENTPersonStructRattach, new ArrayList<>());
                                }

                                List<EnseignementEntity> enseignements = mapEnseignements.get(ENTPersonStructRattach);
                                EnseignementEntity enseignement = new EnseignementEntity(ENTPersonSourceSI, value, "unset" /* not used indeed */,
                                        EnseignementEntity.ENSEIGNEMENT_TYPE.DISCIPLINE);
                                if (!enseignements.contains(enseignement)) {
                                    enseignements.add(enseignement);
                                }
                            } else {
                                // skip this code since it is not valid
                                log.info("Failed to get the 'Discipline' information associated to the code '" + value + "' (ENTPersonUid=" + ENTPersonIdentifiant + ")");
                            }
                            codes.add(value); // make sure the functional key (code / UAI / id) is respected
                        } else {
                            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute " +
                                      "'ENTEleveCodeEnseignements' with blank value");
                        }
                    }
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTEleveCodeEnseignements'");
                }

                /*
                 * Classes (divisions)
                 * Out of GAR grammar. This code purpose is to register the "divisions" associated to
                 * structures so that groups data is made easier to fill later.
                 */
                attribute = entity.get("ENTEleveClasses");
                if (null != attribute && !attribute.isEmpty()) {
                    for (String value : attribute) {
                        if (StringUtils.isNotBlank(value) && GAR_ELE_CLASS_GROUP_VALIDITY_PATTERN.matcher(value).matches()) {
                            String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                            // Control the UAI belongs to the involved structures list
                            if (this.memberStructuresList.contains(uai)) {
                                // Control the UAI belongs to the exercising structures (student has learnings into)
                                if (functionsCodes.contains(uai)) {
                                    String divCode = GARHelper.getInstance().extractCodeGroup(value, 1);
                                    String code = null; // the attribute doesn't specify any "matiere" code (only the division identifier).

                                    // register for persistence
                                    if (!mapEnseignements.containsKey(uai)) {
                                        mapEnseignements.put(uai, new ArrayList<>());
                                    }

                                    List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
                                    EnseignementEntity enseignement = new EnseignementEntity(ENTPersonSourceSI, code, divCode,
                                            EnseignementEntity.ENSEIGNEMENT_TYPE.CLASSE_MATIERE);
                                    if (!enseignements.contains(enseignement)) {
                                        enseignements.add(enseignement);
                                    }
                                } else {
                                    log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute " +
                                             "'ENTEleveClasses' pointing onto structure ('UAI:" + uai + "') that is not referenced by exercising " +
                                             "structures");
                                }
                            } else {
                                log.info("Student with blur identifier '" + ENTPersonIdentifiant + "' belongs to a class within a structure ('UAI:" + uai + "') out of the involved list");
                            }
                        } else {
                            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTEleveClasses' with " +
                                      "blank value or not fitting the pattern '*$*'");
                        }
                    }
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTEleveClasses'");
                }

                /*
                 * Groupes (belong to divisions)
                 * Out of GAR grammar. This code purpose is to register the groups associated to
                 * structures so that groups data is made easier to fill later.
                 */
                attribute = entity.get("ENTEleveGroupes");
                if (null != attribute && !attribute.isEmpty()) {
                    for (String value : attribute) {
                        if (StringUtils.isNotBlank(value) && GAR_ELE_CLASS_GROUP_VALIDITY_PATTERN.matcher(value).matches()) {
                            String uai = GARHelper.getInstance().extractCodeGroup(value, 0).toUpperCase();
                            // Control the UAI belongs to the involved structures list
                            if (this.memberStructuresList.contains(uai)) {
                                // Control the UAI belongs to the exercising structures (student has learnings into)
                                if (functionsCodes.contains(uai)) {
                                    String grpCode = GARHelper.getInstance().extractCodeGroup(value, 1);
                                    String code = null; // the attribute doesn't specify any "matiere" code (only the group identifier).

                                    // register for persistence
                                    if (!mapEnseignements.containsKey(uai)) {
                                        mapEnseignements.put(uai, new ArrayList<>());
                                    }

                                    List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
                                    EnseignementEntity enseignement = new EnseignementEntity(ENTPersonSourceSI, code, grpCode,
                                            EnseignementEntity.ENSEIGNEMENT_TYPE.GROUPE_MATIERE);
                                    if (!enseignements.contains(enseignement)) {
                                        enseignements.add(enseignement);
                                    }
                                } else {
                                    log.info("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute " +
                                             "'ENTEleveGroupes' pointing onto structure ('UAI:" + uai + "') that is not referenced by exercising " +
                                             "structures");
                                }
                            } else {
                                log.info("Student with blur identifier '" + ENTPersonIdentifiant + "' belongs to a group within a structure ('UAI:" + uai + "') out of the involved list");
                            }
                        } else {
                            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has attribute 'ENTEleveGroupes' with " +
                                      "blank value or not fitting the pattern '*$*'");
                        }
                    }
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'ENTEleveGroupes'");
                }

                // Persist the tuples (student, structure) in DB
                for (String uai : mapEnseignements.keySet()) {
                    StaffEntityPK pk = new StaffEntityPK(uai, ENTPersonIdentifiant);
                    StaffEntity enseignant = new StaffEntity(pk, mapEnseignements.get(uai), StaffEntity.STAFF_TYPE.STUDENT);
                    EntityTransaction transac = em.getTransaction();
                    transac.begin();
                    em.persist(enseignant);
                    transac.commit();
                }
            }

            // Flush the possibly remaining entities
            writer.flush();
        } catch (JAXBException | FileNotFoundException | SAXException | XPathExpressionException e) {
            jobActivity.setTrafficLight(ActivityTrafficLight.RED);
            log.error("Failed to execute the GAR loader, error: " + (StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getCause()));
        }

    }

    private class GAREleveWriter {

        private final ObjectFactory factory;
        private final Marshaller marshaller;
        private final int maxNodesCount;
        private final String version;
        private final int page;
        private int nodeCount;
        private GARENTEleve container;

        protected GAREleveWriter(final ObjectFactory factory, final String version, final int page, final int maxNodesCount)
                throws JAXBException, SAXException {
            this.factory = factory;
            this.version = version;
            this.page = page;
            this.maxNodesCount = maxNodesCount;
            nodeCount = 0;
            container = factory.createGARENTEleve();
            container.setVersion(version);
            JAXBContext context = JAXBContext.newInstance(GARENTEleve.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            // Install schema validation
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdFile));
            marshaller.setSchema(schema);
        }

        protected <T> GARENTEleve add(final T item) throws FileNotFoundException, JAXBException, SAXException {
            if (item instanceof GAREleve) {
                container.getGAREleve().add((GAREleve) item);
            } else if (item instanceof GARPersonMEF) {
                container.getGARPersonMEF().add((GARPersonMEF) item);
            } else if (item instanceof GAREleveEnseignement) {
                container.getGAREleveEnseignement().add((GAREleveEnseignement) item);
            }

            // Check the file size limit is reached
            nodeCount++;
            if (0 == (nodeCount % maxNodesCount)) {
                marshal(nodeCount / maxNodesCount);
            }

            return container;
        }

        protected void flush() throws FileNotFoundException, JAXBException, SAXException {
            marshal((nodeCount / maxNodesCount) + 1);
        }

        // Marshal the XML binding
        private void marshal(final int increment) throws FileNotFoundException, JAXBException, SAXException {
            String outputFileName = GARHelper.getInstance().getOutputFileName(output, page, increment);
            JAXBElement<GARENTEleve> jaxbElt = factory.createGARENTEleve(container);
            marshaller.marshal(jaxbElt, new FileOutputStream(outputFileName));
            container = factory.createGARENTEleve();
            container.setVersion(version);
        }

    }

}
