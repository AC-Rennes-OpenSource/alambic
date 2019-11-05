/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GARENTEnseignant;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GAREnsDisciplinesPostes;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GAREnseignant;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GARPersonMEF;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GARPersonProfils;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.GARHelper.NATIONAL_PROFILE_IDENTIFIER;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.EnseignementEntity;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.StaffEntity;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.StaffEntityPK;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

public class GAREnseignantBuilder implements GARTypeBuilder {
	private static final Log log = LogFactory.getLog(GAREnseignantBuilder.class);

	private final int page;
	private final int maxNodesCount;
	private final String output;
	private final String xsdFile;
	private final String version;
	private final ActivityMBean jobActivity;
	private final EntityManager em;
	private final Map<String, Document> exportFiles;
	private final XPath xpath;
	private final List<Map<String, List<String>>> teachers;
	private final List<String> memberStructuresList;

	public GAREnseignantBuilder(final CallableContext context, final Map<String, Source> resources, final int page, final ActivityMBean jobActivity, final int maxNodesCount, final String version,
			final String output, final String xsdFile, final EntityManager em, final Map<String, Document> exportFiles) {
		this.page = page;
		this.jobActivity = jobActivity;
		this.maxNodesCount = maxNodesCount;
		this.version = version;
		this.output = output;
		this.em = em;
		this.exportFiles = exportFiles;
		XPathFactory xpf = XPathFactory.newInstance();
		this.xpath = xpf.newXPath();		
		this.xsdFile = xsdFile;
		// Get the list of involved teachers
		this.teachers = resources.get("Entries").getEntries();
		// Get the list of involved structures
		Source structuresSource = resources.get("Structures");
		this.memberStructuresList = new ArrayList<String>();
		List<Map<String, List<String>>> structures = structuresSource.getEntries();
		structures.forEach(structure -> { 
			if (null != structure.get("ENTStructureUAI") && 1 == structure.get("ENTStructureUAI").size()) {
				this.memberStructuresList.add(structure.get("ENTStructureUAI").get(0).toUpperCase());
				} 
			} );
	}

	@Override
	public void execute() {
		try {
			List<String> attribute;
			ObjectFactory factory = new ObjectFactory();
			GAREnseignantWriter writer = new GAREnseignantWriter(factory, version, page, maxNodesCount);
			List<String> mefCodes = new ArrayList<String>();
			List<String> functionsCodes = new ArrayList<String>();

			// Iterates over teachers
			for (int index = 0; index < this.teachers.size(); index++) {
				// activity monitoring
				jobActivity.setProgress(((index + 1) * 100) / this.teachers.size());
				jobActivity.setProcessing("processing entry " + (index + 1) + "/" + this.teachers.size());

				Map<String, List<String>> entity = this.teachers.get(index);
				
				/* Check the current entity belongs to list of those being taken into account.
				 * This control is useful for processing the anonymized data among possibly added 
				 * entities via LDAP update daily scripts.
				*/
				if (null != this.exportFiles.get("restrictionList")) {
					attribute = entity.get("ENTPersonJointure");
					if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
						Element root = exportFiles.get("restrictionList").getDocumentElement();
						String matchingEntry = (String) xpath.evaluate("//id[.='" + attribute.get(0) + "']", root, XPathConstants.STRING);
						if (StringUtils.isBlank(matchingEntry)) {
							log.debug("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' since it doesn't belong to the restriction list");
							continue;
						}
					} else {
						jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
						log.warn("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonJointure' (mandatory)");
						continue; // skip this entity as a missing mandatory field won't allow XML production
					}
				}
				
				GAREnseignant garEnseignant = factory.createGAREnseignant();
				String ENTPersonStructRattach = null;
				String ENTPersonIdentifiant = null;
				Map<String, List<EnseignementEntity>> mapEnseignements = new HashMap<String, List<EnseignementEntity>>();

				/*
				 * GARPersonIdentifiant
				 */
				attribute = entity.get("ENTPersonUid");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					ENTPersonIdentifiant = attribute.get(0);
					garEnseignant.setGARPersonIdentifiant(ENTPersonIdentifiant);
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
					log.warn("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonUid' (mandatory)");
					continue; // skip this entity as a missing mandatory field won't allow XML production
				}

				/*
				 * GARPersonIdSecondaire
				 * Not implemented in this version.
				 */

				/*
				 * GARPersonCivilite
				 */
				attribute = entity.get("personalTitle");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEnseignant.setGARPersonCivilite(GARHelper.getSDETCompliantTitleValue(attribute.get(0)));
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'personalTitle'");
				}

				/*
				 * GARPersonStructRattach
				 */
				attribute = entity.get("ENTPersonStructRattach");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					/* To notice : set empty UAI if it doesn't belong to the involved structures list.
					* Use case : a teacher teaches in (a) structure(s) different from the administrative one.
					*/
					String uai = attribute.get(0).toUpperCase();
					ENTPersonStructRattach = ((this.memberStructuresList.contains(uai)) ? uai : "");					
					garEnseignant.setGARPersonStructRattach(ENTPersonStructRattach);
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonStructRattach'");
				}

				/*
				 * GARPersonDateNaissance
				 */
				attribute = entity.get("ENTPersonDateNaissance");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					try {
						DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
						Date date = dateFormat.parse(attribute.get(0));
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(date);
						XMLGregorianCalendar xmlgc = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
						garEnseignant.setGARPersonDateNaissance(xmlgc);
					} catch (ParseException | DatatypeConfigurationException e) {
						jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
						log.warn("Failed to parse the attribute 'ENTPersonDateNaissance', might not match the following expected date format 'dd/MM/yyyy', error: " + e.getMessage());
					}
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonDateNaissance'");
				}

				/*
				 * GARPersonMail
				 */
				attribute = entity.get("mail");
				if (null != attribute && 0 < attribute.size()) {
					attribute.stream()
						.filter(email -> StringUtils.isNotBlank(email))
						.forEach(email -> garEnseignant.getGARPersonMail().add(email.trim()));
					;
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'mail'");
				}

				/*
				 * GARPersonNom
				 */
				attribute = entity.get("sn");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEnseignant.setGARPersonNom(attribute.get(0));
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
					log.warn("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no attribute 'sn' (mandatory)");
					continue; // skip this entity as a missing mandatory field won't allow XML production
				}

				/*
				 * GARPersonPrenom
				 */
				attribute = entity.get("givenName");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEnseignant.setGARPersonPrenom(attribute.get(0));
					garEnseignant.getGARPersonAutresPrenoms().add(attribute.get(0)); // le prénom usuel doit figurer parmi les "autres" prénoms
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
					log.warn("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no attribute 'givenName' (mandatory)");
					continue; // skip this entity as a missing mandatory field won't allow XML production
				}

				/*
				 * GARPersonAutresPrenoms
				 */
				attribute = entity.get("ENTPersonAutresPrenoms");
				if (null != attribute && 0 < attribute.size()) {
					for (String value : attribute) {
						if (StringUtils.isNotBlank(value) && !garEnseignant.getGARPersonAutresPrenoms().contains(value)) {
							garEnseignant.getGARPersonAutresPrenoms().add(value);
						} else {
							log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTPersonAutresPrenoms' with blank value");
						}
					}
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonAutresPrenoms'");
				}

				/*
				 * GARPersonNomPatro
				 */
				attribute = entity.get("ENTPersonNomPatro");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEnseignant.setGARPersonNomPatro(attribute.get(0));
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'ENTPersonNomPatro'");
				}

				/*
				 * GARPersonEtab & GARPersonProfil
				 */
				attribute = entity.get("ENTPersonFonctions");
				if (null != attribute && 0 < attribute.size()) {
					functionsCodes.clear();
					for (String value : attribute) {
						// control the attribute value is relevant (not null and well formated)
						if (StringUtils.isNotBlank(value) && value.matches("[^\\$]+\\$[^\\$]+.*")) {
							String uai = GARHelper.extractCodeGroup(value, 0).toUpperCase();
							String profile = GARHelper.extractCodeGroup(value, 1).toUpperCase();
							// get SDET compliant national profile value based on both the title and function values
							String sdetcnpv = GARHelper.getSDETCompliantProfileValue(entity.get("title").get(0), profile);							
							
							if (StringUtils.isNotBlank(sdetcnpv)) {
								/**
								 * Up to now, GAR platform supports only the two profiles ENS & DOC. But situations exist where school directors
								 * are also teaching (in both domains : private & public).
								 * Hence, the following statements implement security by filtering either ENS or DOC profiles.
								 */
								if ( !sdetcnpv.equalsIgnoreCase(NATIONAL_PROFILE_IDENTIFIER.National_ENS.toString())
										&& !sdetcnpv.equalsIgnoreCase(NATIONAL_PROFILE_IDENTIFIER.National_DOC.toString()) ) {
									log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' is teaching but has a national profile '" + sdetcnpv + "' not supported by GAR. Is replaced by '" + NATIONAL_PROFILE_IDENTIFIER.National_ENS.toString() + "'");
									sdetcnpv = NATIONAL_PROFILE_IDENTIFIER.National_ENS.toString();
								}
								
								if (this.memberStructuresList.contains(uai)) {
									// Control the UAI belongs to the involved structures list + was not already processed (functional key is : uai + national profile)
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
									log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' is member of a structure ('UAI:" + uai + "') out of the involved list");
								}
							} else {
								jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
								log.warn("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no national profil that could be associated with the profile '" + profile + "'");
							}
						} else {
							jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
							log.warn("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTPersonFonctions' with not regular value");
						}
					}
				} else {
					/*
					 * Il ne s'agit pas d'une erreur sévère (code ActivityTrafficLight.RED) car il peut s'agir d'une personne non encore désactivée
					 * dans l'annuaire applicatif Toutatice mais malgré tout en fin de fonction. Dans ce cas, l'attribut n'est pas renseigné.
					 */
					jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
					log.warn("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonFonctions' (mandatory)");
					continue; // skip this entity as a missing mandatory field won't allow XML production
				}

				// Add GARPersonProfil (not GARPersonEtab) associated to the administrative structure if not already present
				final String controlUAI = ENTPersonStructRattach;
				if (StringUtils.isNotBlank(ENTPersonStructRattach) && !functionsCodes.stream().anyMatch(item -> item.matches(controlUAI.concat("\\$.+")))) {
					attribute = entity.get("ENTPersonNationalProfil");
					if (null != attribute && StringUtils.isNotBlank(attribute.get(0))) {
						// get SDET compliant national profile value
						String sdetcnpv = GARHelper.getSDETCompliantProfileValue(entity.get("title").get(0), attribute.get(0));
						if (StringUtils.isNotBlank(sdetcnpv)) {
							GARPersonProfils pf = factory.createGARPersonProfils();
							pf.setGARStructureUAI(ENTPersonStructRattach);
							pf.setGARPersonProfil(sdetcnpv);
							if (!garEnseignant.getGARPersonProfils().contains(pf)) {
								garEnseignant.getGARPersonProfils().add(pf);
							}
						} else {
							jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
							log.warn("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no national profil that could be associated with the title '" + entity.get("title").get(0) + "' and ENTPersonNationalProfil '" + attribute.get(0) + "'");
						}
					} else {
						jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
						log.warn("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonNationalProfil' (or empty)");						
					}
				}
				
				/*
				 * Contrôler que les attributs obligatoires GARPersonEtab & GARPersonProfil sont présents.
				 * (pourraient être absents du fait du filtrage réalisé sur les structures pilotes)
				 */
				if (0 == garEnseignant.getGARPersonEtab().size() || 0 == garEnseignant.getGARPersonProfils().size()) {
					jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
					log.warn("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has either no attribute 'GARPersonEtab' or 'GARPersonProfils' (mandatory)");
					continue; // skip this entity as a missing mandatory field won't allow XML production					
				}

				/*
				 * GAREnsDisciplinesPostes
				 */
				attribute = entity.get("ENTAuxEnsDisciplinesPoste");
				if (null != attribute && 0 < attribute.size()) {
					Map<String, GAREnsDisciplinesPostes> map = new HashMap<String, GAREnsDisciplinesPostes>();
					for (String value : attribute) {
						if (StringUtils.isNotBlank(value)) {
							String uai = GARHelper.extractCodeGroup(value, 0).toUpperCase();
							// Control the UAI belongs to the involved structures list
							if (this.memberStructuresList.contains(uai)) {
								String code = GARHelper.extractCodeGroup(value, 1);
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
									jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
									log.warn("Skipping entity '" + GARHelper.getPersonEntityBlurId(entity) + "' as it has no code associated to one discipline (value is '" + value + "') in attribute 'ENTAuxEnsDisciplinesPoste' (mandatory)");
									continue; // skip this entity as a missing mandatory field won't allow XML production
								}
							} else {
								log.info("Entity '"+ GARHelper.getPersonEntityBlurId(entity) +"' teaches subject in structure ('UAI:" + uai + "') out of the involved list");
							}
						} else {
							log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsDisciplinesPoste' with blank value");
						}
					}

					for (String uai : map.keySet()) {
						garEnseignant.getGAREnsDisciplinesPostes().add(map.get(uai));
					}
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsDisciplinesPoste'");
				}

				writer.add(garEnseignant);

				/*
				 * GARMEFCode
				 */
				mefCodes.clear();
				attribute = entity.get("ENTAuxEnsMef");
				if (null != attribute && 0 < attribute.size()) {
					for (String value : attribute) {
						if (StringUtils.isNotBlank(value) && !mefCodes.contains(value)) {
							String uai = GARHelper.extractCodeGroup(value, 0).toUpperCase();
							// Control the UAI belongs to the involved structures list
							if (this.memberStructuresList.contains(uai)) {
								// Control the UAI belongs to the exercising structures (teacher has functions into)
								if (functionsCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
									String code = GARHelper.extractCodeGroup(value, 1);
									
									// register for persistence
									if (!mapEnseignements.containsKey(uai)) {
										mapEnseignements.put(uai, new ArrayList<EnseignementEntity>());
									}
									List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
									EnseignementEntity enseignement = new EnseignementEntity(code, "unset" /* will be obtained from request on AAF */, EnseignementEntity.ENSEIGNEMENT_TYPE.MEF);
									if (!enseignements.contains(enseignement)) {
										enseignements.add(enseignement);
									}
									
									GARPersonMEF pmef = factory.createGARPersonMEF();
									pmef.setGARStructureUAI(uai);
									pmef.setGARMEFCode(code);
									pmef.setGARPersonIdentifiant(ENTPersonIdentifiant);
									writer.add(pmef);
									mefCodes.add(value); // Make sure the functional key (UAI / code / id) is respected
								} else {
									log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsMef' pointing onto structure ('UAI:" + uai + "') that is not referenced by 'ENTPersonFonctions'");
								}
							} else {
								log.info("Entity '"+ GARHelper.getPersonEntityBlurId(entity) +"' teaches MEF in structure ('UAI:" + uai + "') out of the involved list");
							}
						} else {
							log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsMef' with blank value");
						}
					}
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsMef'");
				}

				/*
				 * Classes (divisions) "matières"
				 * Out of GAR grammar. This code purpose is to register the "matieres" associated to
				 * structures so that structure & groups data is made easier to fill later.
				 */
				attribute = entity.get("ENTAuxEnsClassesMatieres");
				if (null != attribute && 0 < attribute.size()) {
					for (String value : attribute) {
						if (StringUtils.isNotBlank(value)) {
							String uai = GARHelper.extractCodeGroup(value, 0).toUpperCase();
							// Control the UAI belongs to the involved structures list
							if (this.memberStructuresList.contains(uai)) {
								// Control the UAI belongs to the exercising structures (teacher has functions into)
								if (functionsCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
									String divOrGrpCode = GARHelper.extractCodeGroup(value, 1);
									String code = GARHelper.extractCodeGroup(value, 2);

									// register for persistence
									if (!mapEnseignements.containsKey(uai)) {
										mapEnseignements.put(uai, new ArrayList<EnseignementEntity>());
									}

									List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
									EnseignementEntity enseignement = new EnseignementEntity(code, divOrGrpCode, EnseignementEntity.ENSEIGNEMENT_TYPE.CLASSE_MATIERE);
									if (!enseignements.contains(enseignement)) {
										enseignements.add(enseignement);
									}
								} else {
									log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsClassesMatieres' pointing onto structure ('UAI:" + uai + "') that is not referenced by 'ENTPersonFonctions'");
								}
							} else {
								log.info("Entity '"+ GARHelper.getPersonEntityBlurId(entity) +"' belongs to a division in structure ('UAI:" + uai + "') out of the involved list");
							}
						} else {
							log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsClassesMatieres' with blank value");
						}
					}
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsClassesMatieres'");
				}

				/*
				 * Groupes "matières" (belong to divisions)
				 * Out of GAR grammar. This programmatic code purpose is to register the "matieres" associated
				 * to structures so that structure & groups data is easier to fill.
				 */
				attribute = entity.get("ENTAuxEnsGroupesMatieres");
				if (null != attribute && 0 < attribute.size()) {
					for (String value : attribute) {
						if (StringUtils.isNotBlank(value)) {
							String uai = GARHelper.extractCodeGroup(value, 0).toUpperCase();
							// Control the UAI belongs to the involved structures list
							if (this.memberStructuresList.contains(uai)) {
								// Control the UAI belongs to the exercising structures (teacher has functions into)
								if (functionsCodes.stream().anyMatch(item -> item.matches(uai.concat("\\$.+")))) {
									String divOrGrpCode = GARHelper.extractCodeGroup(value, 1);
									String code = GARHelper.extractCodeGroup(value, 2);
									
									// register for persistence
									if (!mapEnseignements.containsKey(uai)) {
										mapEnseignements.put(uai, new ArrayList<EnseignementEntity>());
									}
									
									List<EnseignementEntity> enseignements = mapEnseignements.get(uai);
									EnseignementEntity enseignement = new EnseignementEntity(code, divOrGrpCode, EnseignementEntity.ENSEIGNEMENT_TYPE.GROUPE_MATIERE);
									if (!enseignements.contains(enseignement)) {
										enseignements.add(enseignement);
									}
								} else {
									log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsGroupesMatieres' pointing onto structure ('UAI:" + uai + "') that is not referenced by 'ENTPersonFonctions'");
								}
							} else {
								log.info("Entity '"+ GARHelper.getPersonEntityBlurId(entity) +"' belongs to a group in structure ('UAI:" + uai + "') out of the involved list");
							}
						} else {
							log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has attribute 'ENTAuxEnsGroupesMatieres' with blank value");
						}
					}
				} else {
					log.info("Entity '" + GARHelper.getPersonEntityBlurId(entity) + "' has no attribute 'ENTAuxEnsGroupesMatieres'");
				}

				// Persist the tuples (teacher, structure) in DB
				for (String uai : mapEnseignements.keySet()) {
					StaffEntityPK pk = new StaffEntityPK(uai, ENTPersonIdentifiant);
					StaffEntity enseignant = new StaffEntity(pk, mapEnseignements.get(uai), StaffEntity.STAFF_TYPE.TEACHER);
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
	
	private class GAREnseignantWriter {

		private int nodeCount;
		private final ObjectFactory factory;
		private GARENTEnseignant container;
		private final Marshaller marshaller;
		private final int maxNodesCount;
		private final String version;
		private final int page;

		protected GAREnseignantWriter(final ObjectFactory factory, final String version, final int page, final int maxNodesCount) throws JAXBException, SAXException {
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
			String outputFileName = GARHelper.getOutputFileName(output, page, increment);
			JAXBElement<GARENTEnseignant> jaxbElt = factory.createGARENTEnseignant(container);
			marshaller.marshal(jaxbElt, new FileOutputStream(outputFileName));
			container = factory.createGARENTEnseignant();
			container.setVersion(version);
		}

	}

}
