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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.EnseignementEntity;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence.StaffEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GARENTGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GAREnsClasseMatiere;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GAREnsGroupeMatiere;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GARGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.GARPersonGroupe;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding.ObjectFactory;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

public class GARGroupeBuilder implements GARTypeBuilder {
	private static final Log log = LogFactory.getLog(GARGroupeBuilder.class);

	private final int page;
	private final int maxNodesCount;
	private final String output;
	private final String xsdFile;
	private final String version;
	private final ActivityMBean jobActivity;
	private final EntityManager em;
	private final List<Map<String, List<String>>> structures;

	public GARGroupeBuilder(final CallableContext context, final Map<String, Source> resources, final int page, final ActivityMBean jobActivity, final int maxNodesCount, final String version,
			final String output, final String xsdFile, final EntityManager em) {
		this.page = page;
		this.jobActivity = jobActivity;
		this.maxNodesCount = maxNodesCount;
		this.version = version;
		this.output = output;
		this.em = em;
		this.xsdFile = xsdFile;
		// Get the list of involved groups
		this.structures = resources.get("Entries").getEntries();
	}

	@Override
	public void execute() throws AlambicException {
		try {
			ObjectFactory factory = new ObjectFactory();
			GARGroupWriter writer = new GARGroupWriter(factory, version, page, maxNodesCount);

			// Iterates over structures
			for (int index = 0; index < this.structures.size(); index++) {
				// activity monitoring
				jobActivity.setProgress(((index + 1) * 100) / this.structures.size());
				jobActivity.setProcessing("processing entry " + (index + 1) + "/" + this.structures.size());
				
				Map<String, List<String>> entity = this.structures.get(index);
				final String ENTStructureUAI;
				
				/*
				 * GARStructureUAI
				 */
				List<String> attribute = entity.get("ENTStructureUAI");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					ENTStructureUAI = attribute.get(0).toUpperCase();
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.RED);
					log.error("Skipping entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' as it has no attribute 'ENTStructureUAI' (mandatory)");
					continue; // skip this entry as a missing mandatory field won't allow XML production
				}
				
				/*
				 * GARGroupe (DIVISION - classe) : GARGroupeCode / GARGroupeLibelle / GARGroupeStatut
				 */
				List<String> listStructDivCodes = new ArrayList<String>();
				List<GARGroupe> listGarGroupes = new ArrayList<>();
				attribute = entity.get("ENTStructureClasses");
				if (null != attribute && 0 < attribute.size()) {
					for (String groupe : attribute) {
						if (StringUtils.isNotBlank(groupe)) {
							String code = GARHelper.getInstance().extractCodeGroup(groupe, 0);
							if (!listStructDivCodes.contains(code)) {
								String libelle = GARHelper.getInstance().extractCodeGroup(groupe, 1);
								GARGroupe garGroup = factory.createGARGroupe();
								garGroup.setGARStructureUAI(ENTStructureUAI);
								garGroup.setGARGroupeStatut("DIVISION");
								garGroup.setGARGroupeCode(code);
								garGroup.setGARGroupeLibelle((StringUtils.isNotBlank(libelle)) ? libelle : code); // get the code as label if no-one is defined
								listGarGroupes.add(garGroup);
								listStructDivCodes.add(code);
							}
						}
					}
				} else {
					log.debug("Entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' has no attribute 'ENTStructureClasses'");
				}

				/*
				 * GARGroupe (GROUPE) : GARGroupeCode / GARGroupeLibelle / GARGroupeStatut / GARGroupeDivAppartenance
				 */
				List<String> listStructConflictingCodes = new ArrayList<String>(); // will contain the problematic group/division code (refer to GAR Support : https://support.gar.education.fr/servicedesk/customer/portal/1/GSN1-1098)
				List<String> listStructGrpCodes = new ArrayList<String>();
				attribute = entity.get("ENTStructureGroupes");
				if (null != attribute && 0 < attribute.size()) {
					for (String groupe : attribute) {
						if (StringUtils.isNotBlank(groupe)) {
							String code = GARHelper.getInstance().extractCodeGroup(groupe, 0);
							/*
							 * Functional key over GARGroupe element deals with the couple {UAI, code}. The status (GROUPE/DIVISION) is
							 * not taken into consideration. Hence, both the statement 'listGrpCodes' and 'listDivCodes' are checked.
							 */
							if (!listStructGrpCodes.contains(code)) {
								if (!listStructDivCodes.contains(code)) {
									String libelle = GARHelper.getInstance().extractCodeGroup(groupe, 1);
									GARGroupe garGroup = factory.createGARGroupe();
									garGroup.setGARStructureUAI(ENTStructureUAI);
									garGroup.setGARGroupeStatut("GROUPE");
									garGroup.setGARGroupeCode(code);
									garGroup.setGARGroupeLibelle((StringUtils.isNotBlank(libelle)) ? libelle : code); // get the code as label if no-one is defined
									int tokenCount = groupe.split("\\$").length;
									if (tokenCount > 2) {
										for (int i = 2; i < tokenCount; i++) {
											garGroup.getGARGroupeDivAppartenance().add(GARHelper.getInstance().extractCodeGroup(groupe, i));
										}
									}
									listGarGroupes.add(garGroup);
									listStructGrpCodes.add(code);
								} else {
									jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
									log.warn("Entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' defines a division and group with same code '" + code + "'. Both will be ignored.");
									listStructConflictingCodes.add(code);
								}
							}
						}
					}
				} else {
					log.debug("Entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' has no attribute 'ENTStructureGroupes'");
				}

				/**
				 * The following filtering treatments will be deprecated as soon as the GARGroupe functional key becomes {UAI, code, statut} instead of simply {UAI, code}.
				 * Maybe in a future version of the grammar.
				 * 
				 * Until then, groups and division sharing the same code must be excluded from the export. Moreover, the references to the excluded divisions must be removed 
				 * from the groups definitions.
				 */
				
				// Filter the conflicting groups and divisions : those having the same code
				listGarGroupes.removeIf(groupe -> listStructConflictingCodes.contains(groupe.getGARGroupeCode()));
				
				// Update groups list to remove references both to the conflicting and missing divisions
				Predicate<String> isGroupeCodeNotRelevant = new Predicate<String>() {
					
					@Override
					public boolean test(String code) {
						boolean isNotRelevant = false;
						
						if (listStructConflictingCodes.contains(code)) {
							isNotRelevant = true;
							jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
							log.warn("Remove the reference to the division with code '" + code + "' from the structure " + ENTStructureUAI + " since it belongs to the conflicting group vs division codes list");
						} else if (!listStructDivCodes.contains(code)) {
							isNotRelevant = true;
							jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
							log.warn("Remove the reference to the division with code '" + code + "' since this division doesn't belong to the structure" + ENTStructureUAI);
						}
						
						return isNotRelevant;
					}
					
				};
				listGarGroupes.stream()
					// filter groups referencing at least one division
					.filter(groupe -> (groupe.getGARGroupeStatut().equals("GROUPE") && !groupe.getGARGroupeDivAppartenance().isEmpty()))
					// for each, remove any reference to a conflicting division (previously excluded)
					.forEach(groupe -> groupe.getGARGroupeDivAppartenance().removeIf(isGroupeCodeNotRelevant));
				
				// Extract the list of the structure's valid groups codes
				List<String> validStructGrpCodesList = listGarGroupes.stream()
						.filter(groupe -> "GROUPE".equals(groupe.getGARGroupeStatut()))
						.map(groupe -> groupe.getGARGroupeCode())
						.collect(Collectors.toList());

				// Extract the list of the structure's valid divisions codes
				List<String> validStructDivCodesList = listGarGroupes.stream()
						.filter(groupe -> "DIVISION".equals(groupe.getGARGroupeStatut()))
						.map(groupe -> groupe.getGARGroupeCode())
						.collect(Collectors.toList());

				// Initialize the list of not empty groups and divisions
				List<String> notEmptyGrpDivCodes = new ArrayList<>();
				
				/*
				 * Teachers : GARPersonGroupe & GAREnsGroupeMatiere & GAREnsClasseMatiere
				 */

				// Get the list of teachers within this structure
				EntityTransaction transaction = em.getTransaction();
				transaction.begin();
				Query emQuery = em.createQuery("SELECT se FROM StaffEntity se WHERE se.primaryKey.uai = '" + ENTStructureUAI + "' AND se.type = :type_parameter");
				emQuery.setParameter("type_parameter", StaffEntity.STAFF_TYPE.TEACHER);
				@SuppressWarnings("unchecked")
				List<StaffEntity> enseignants = emQuery.getResultList();
				transaction.commit();

				Map<String, GAREnsGroupeMatiere> listTeacherGrp = new HashMap<String, GAREnsGroupeMatiere>();
				Map<String, GAREnsClasseMatiere> listTeacherDiv = new HashMap<String, GAREnsClasseMatiere>();

				// For each teacher
				for (StaffEntity enseignant : enseignants) {
					// Initialize groups and divisions of the teacher
					listTeacherGrp.clear();
					listTeacherDiv.clear();
					
					// Iterate over the list of "enseignements" within valid groups and divisions
					for (EnseignementEntity enseignement : enseignant.getEnseignements()) {
						// Only division & groups types of "enseignement" are relevant
						if (EnseignementEntity.ENSEIGNEMENT_TYPE.CLASSE_MATIERE.equals(enseignement.getType()) || EnseignementEntity.ENSEIGNEMENT_TYPE.GROUPE_MATIERE.equals(enseignement.getType())) {
							// Control the overall consistency of divisions & groups against structure definitions (GAREnsGroupeMatiere, GAREnsClasseMatiere & GARPersonGroupe Versus GARGroupeCode)
							String groupCode = enseignement.getDivOrGrpCode();
							if (validStructGrpCodesList.contains(groupCode) || validStructDivCodesList.contains(groupCode)) {
								if (EnseignementEntity.ENSEIGNEMENT_TYPE.GROUPE_MATIERE.equals(enseignement.getType())) {
									if (validStructGrpCodesList.contains(groupCode)) {
										if (!listTeacherGrp.containsKey(groupCode)) {
											listTeacherGrp.put(groupCode, factory.createGAREnsGroupeMatiere());
											listTeacherGrp.get(groupCode).setGARStructureUAI(ENTStructureUAI);
											listTeacherGrp.get(groupCode).setGARPersonIdentifiant(enseignant.getPrimaryKey().getUuid());
											listTeacherGrp.get(groupCode).setGARGroupeCode(groupCode);
											listTeacherGrp.get(groupCode).getGARMatiereCode().add(enseignement.getCode());
										} else {
											listTeacherGrp.get(groupCode).getGARMatiereCode().add(enseignement.getCode());
										}

										// Update the list of "not empty" groups and divisions
										if (!notEmptyGrpDivCodes.contains(groupCode)) notEmptyGrpDivCodes.add(groupCode);
									} else {
										jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
										log.warn("Teacher entity (ENTPersonUid=" + enseignant.getPrimaryKey().getUuid() + ") references a group '" + groupCode + "' that actually is defined as a division within the structure '" + ENTStructureUAI + "'");
									}
								} else {
									if (validStructDivCodesList.contains(groupCode)) {
										if (!listTeacherDiv.containsKey(groupCode)) {
											listTeacherDiv.put(groupCode, factory.createGAREnsClasseMatiere());
											listTeacherDiv.get(groupCode).setGARStructureUAI(ENTStructureUAI);
											listTeacherDiv.get(groupCode).setGARPersonIdentifiant(enseignant.getPrimaryKey().getUuid());
											listTeacherDiv.get(groupCode).setGARGroupeCode(groupCode);
											listTeacherDiv.get(groupCode).getGARMatiereCode().add(enseignement.getCode());
										} else {
											listTeacherDiv.get(groupCode).getGARMatiereCode().add(enseignement.getCode());
										}
										
										// Update the list of "not empty" groups and divisions
										if (!notEmptyGrpDivCodes.contains(groupCode)) notEmptyGrpDivCodes.add(groupCode);
									} else {
										jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
										log.warn("Teacher entity (ENTPersonUid=" + enseignant.getPrimaryKey().getUuid() + ") references a division '" + groupCode + "' that actually is defined as a group within the structure '" + ENTStructureUAI + "'");
									}
								}
							} else {
								log.warn("Filtered GAREnsGroupe/ClasseMatiere element from teacher entity (ENTPersonUid=" + enseignant.getPrimaryKey().getUuid() + ") since it references a code '" + groupCode + "' absent from the UAI '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' (might be filtered earlier)");
							}
						}
					}

					// Write the groups
					for (String groupCode : listTeacherGrp.keySet()) {
						// write "GAREnsGroupeMatiere"
						writer.add(listTeacherGrp.get(groupCode));

						// write "GARPersonGroupe" ("groupe" type)
						GARPersonGroupe personGroupe = factory.createGARPersonGroupe();
						personGroupe.setGARStructureUAI(listTeacherGrp.get(groupCode).getGARStructureUAI());
						personGroupe.setGARPersonIdentifiant(listTeacherGrp.get(groupCode).getGARPersonIdentifiant());
						personGroupe.setGARGroupeCode(groupCode);
						writer.add(personGroupe);
					}

					// Write the divisions
					for (String groupCode : listTeacherDiv.keySet()) {
						// write "GAREnsClasseMatiere"
						writer.add(listTeacherDiv.get(groupCode));

						// write "GARPersonGroupe" ("division" type)
						GARPersonGroupe personGroupe = factory.createGARPersonGroupe();
						personGroupe.setGARStructureUAI(listTeacherDiv.get(groupCode).getGARStructureUAI());
						personGroupe.setGARPersonIdentifiant(listTeacherDiv.get(groupCode).getGARPersonIdentifiant());
						personGroupe.setGARGroupeCode(groupCode);
						writer.add(personGroupe);
					}
				}
				
				/*
				 * Students : GARPersonGroupe
				 */

				// Get the list of students within this structure
				transaction = em.getTransaction();
				transaction.begin();
				emQuery = em.createQuery("SELECT se FROM StaffEntity se WHERE se.primaryKey.uai = '" + ENTStructureUAI + "' AND se.type = :type_parameter");
				emQuery.setParameter("type_parameter", StaffEntity.STAFF_TYPE.STUDENT);
				@SuppressWarnings("unchecked")
				List<StaffEntity> students = emQuery.getResultList();
				transaction.commit();

				// For each student, iterate over the list of "enseignements" within valid groups and divisions
				for (StaffEntity student : students) {
					for (EnseignementEntity enseignement : student.getEnseignements()) {
						// Only division & groups types of "enseignement" are relevant
						if (EnseignementEntity.ENSEIGNEMENT_TYPE.CLASSE_MATIERE.equals(enseignement.getType()) || EnseignementEntity.ENSEIGNEMENT_TYPE.GROUPE_MATIERE.equals(enseignement.getType())) {
							String groupCode = enseignement.getDivOrGrpCode();
							// Control the overall consistency of divisions & groups against structure definitions (GARPersonGroupe Versus GARGroupeCode)
							if (validStructGrpCodesList.contains(groupCode) || validStructDivCodesList.contains(groupCode)) {
								// write "GARPersonGroupe"
								GARPersonGroupe personGroupe = factory.createGARPersonGroupe();
								personGroupe.setGARStructureUAI(ENTStructureUAI);
								personGroupe.setGARPersonIdentifiant(student.getPrimaryKey().getUuid());
								personGroupe.setGARGroupeCode(groupCode);
								writer.add(personGroupe);
								
								// Update the list of "not empty" groups and divisions
								if (!notEmptyGrpDivCodes.contains(groupCode)) notEmptyGrpDivCodes.add(groupCode);
							} else {
								log.warn("Filtered GAREnsGroupe/ClasseMatiere element from student entity (ENTPersonUid=" + student.getPrimaryKey().getUuid() + ") since it references a code '" + groupCode + "' absent from the UAI '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' (might be filtered earlier)");
							}
						}
					}
				}
				
				/* Write "GARGroupe" elements
				 * - discard the groups and divisions having no members,
				 * - do not discard divisions having no members but being referenced by a group of options
				 */
				Consumer<GARGroupe> writeNotEmptyGroupsConsumer = new Consumer<GARGroupe>() {
					
					@Override
					public void accept(GARGroupe groupe) {
						long countOfRefByGrps = 0;
						if ("DIVISION".equals(groupe.getGARGroupeStatut())) {
							countOfRefByGrps = listGarGroupes.stream()
									.filter(grp -> "GROUPE".equals(grp.getGARGroupeStatut()))
									.filter(grp -> grp.getGARGroupeDivAppartenance().contains(groupe.getGARGroupeCode()))
									.count();
						}
						if (notEmptyGrpDivCodes.contains(groupe.getGARGroupeCode())	|| ("DIVISION".equals(groupe.getGARGroupeStatut()) && countOfRefByGrps > 0) ) {
							try {
								// Flush relevant GAR groups (GROUPE & DIVISION)
								writer.add(groupe);
							} catch (FileNotFoundException | JAXBException e) {
								jobActivity.setTrafficLight(ActivityTrafficLight.RED);
								log.error("Failed to execute the GAR loader, error: " + (StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getCause()));
							}
						} else {
							log.info("The GARGroupe (code=" + groupe.getGARGroupeCode() + ", label='" + groupe.getGARGroupeLibelle() + "', status=" + groupe.getGARGroupeStatut()+ ", UAI=" + groupe.getGARStructureUAI() + ") is filtered since it has no member (and is not referenced by any group)");
						}
					}
				};
				listGarGroupes.forEach(writeNotEmptyGroupsConsumer);
				
			}
			
			// Flush the possibly remaining entities
			writer.flush();
		} catch (JAXBException | FileNotFoundException | SAXException e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			log.error("Failed to execute the GAR loader, error: " + (StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getCause()));
		}

	}

	private class GARGroupWriter {

		private int nodeCount;
		private final ObjectFactory factory;
		private GARENTGroupe container;
		private final Marshaller marshaller;
		private final int maxNodesCount;
		private final String version;
		private final int page;

		protected GARGroupWriter(final ObjectFactory factory, final String version, final int page, final int maxNodesCount) throws JAXBException, SAXException {
			this.factory = factory;
			this.version = version;
			this.page = page;
			this.maxNodesCount = maxNodesCount;
			nodeCount = 0;
			container = factory.createGARENTGroupe();
			container.setVersion(version);
			JAXBContext context = JAXBContext.newInstance(GARENTGroupe.class);
			marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

			// Install schema validation
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(new StreamSource(xsdFile));
			marshaller.setSchema(schema);
		}
	
		protected <T> GARENTGroupe add(final T item) throws FileNotFoundException, JAXBException {
			if (item instanceof GARGroupe) {
				container.getGARGroupe().add((GARGroupe) item);
			} else if (item instanceof GARPersonGroupe) {
				container.getGARPersonGroupe().add((GARPersonGroupe) item);
			} else if (item instanceof GAREnsGroupeMatiere) {
				container.getGAREnsGroupeMatiere().add((GAREnsGroupeMatiere) item);
			} else if (item instanceof GAREnsClasseMatiere) {
				container.getGAREnsClasseMatiere().add((GAREnsClasseMatiere) item);
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
			JAXBElement<GARENTGroupe> jaxbElt = factory.createGARENTGroupe(container);
			marshaller.marshal(jaxbElt, new FileOutputStream(outputFileName));
			container = factory.createGARENTGroupe();
			container.setVersion(version);
		}

	}

}
