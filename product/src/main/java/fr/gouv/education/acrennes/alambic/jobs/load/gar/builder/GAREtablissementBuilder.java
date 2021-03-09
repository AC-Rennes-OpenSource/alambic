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
import org.json.JSONObject;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.GARENTEtab;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.GAREtab;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.GARMEF;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.GARMatiere;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

public class GAREtablissementBuilder implements GARTypeBuilder {
	private static final Log log = LogFactory.getLog(GAREtablissementBuilder.class);

	private final int page;
	private final int maxNodesCount;
	private final String output;
	private final String xsdFile;
	private final String version;
	private final ActivityMBean jobActivity;
	private final EntityManager em;
	private final List<Map<String, List<String>>> structures;
	private final Source aafSource;

	public GAREtablissementBuilder(GARBuilderParameters parameters) {
		this.page = parameters.getPage();
		this.jobActivity = parameters.getJobActivity();
		this.maxNodesCount = parameters.getMaxNodesCount();
		this.version = parameters.getVersion();
		this.output = parameters.getOutput();
		this.em = parameters.getEm();
		this.xsdFile = parameters.getXsdFile();
		this.structures = parameters.getResources().get("Entries").getEntries(); // Get the list of involved structures
		this.aafSource = parameters.getResources().get("AAF");
	}

	@Override
	public void execute() throws AlambicException {
		try {
			ObjectFactory factory = new ObjectFactory();
			GAREtablissementWriter writer = new GAREtablissementWriter(factory, version, page, maxNodesCount);

			// Iterates over structures
			for (int index = 0; index < this.structures.size(); index++) {
				// activity monitoring
				jobActivity.setProgress(((index + 1) * 100) / this.structures.size());
				jobActivity.setProcessing("processing entry " + (index + 1) + "/" + this.structures.size());

				Map<String, List<String>> entity = this.structures.get(index);
				GAREtab garEtab = factory.createGAREtab();

				String ENTStructureUAI = null;

				/*
				 * GARStructureUAI
				 */
				List<String> attribute = entity.get("ENTStructureUAI");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					ENTStructureUAI = attribute.get(0).toUpperCase();
					garEtab.setGARStructureUAI(ENTStructureUAI);
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
					log.warn("Skipping entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' as it has no attribute 'ENTStructureUAI' (mandatory)");
					continue; // skip this entry as a missing mandatory field won't allow XML production
				}

				/*
				 * GARStructureNomCourant
				 */
				attribute = entity.get("ENTDisplayName");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEtab.setGARStructureNomCourant(attribute.get(0));
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
					log.warn("Skipping entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' as it has no attribute 'ENTDisplayName' (mandatory)");
					continue; // skip this entry as a missing mandatory field won't allow XML production
				}

				/*
				 * GAREtablissementStructRattachFctl
				 */
				attribute = entity.get("ENTEtablissementStructRattachFctl");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEtab.setGAREtablissementStructRattachFctl(attribute.get(0));
				} else {
					log.debug("Entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' has no attribute 'ENTEtablissementStructRattachFctl'");
				}

				/*
				 * GARStructureContrat
				 */
				attribute = entity.get("ENTEtablissementContrat");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEtab.setGARStructureContrat(GARHelper.getInstance().getSDETCompliantContractValue(attribute.get(0)));
				} else {
					log.debug("Entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' has no attribute 'ENTEtablissementContrat'");
				}

				/*
				 * GARStructureTelephone
				 */
				attribute = entity.get("telephoneNumber");
				if (null != attribute && 0 < attribute.size() && StringUtils.isNotBlank(attribute.get(0))) {
					garEtab.setGARStructureTelephone(attribute.get(0));
				} else {
					log.debug("Entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' has no attribute 'telephoneNumber");
				}

				/*
				 * GARStructureEmail
				 */
				if (StringUtils.isNotBlank(ENTStructureUAI)) {
					garEtab.setGARStructureEmail(String.format("ce.%s@ac-rennes.fr", ENTStructureUAI));
				} else {
					log.debug("Entity '" + GARHelper.getInstance().getStructEntityBlurId(entity) + "' has no email as no UAI could be found");
				}

				writer.add(garEtab);

				/*
				 * GARMEF & GARMatiere
				 * (Respect controls described by specification "GAR_ENT_Référentiel-technique-fonctionnel-sécurité_v3.0_DF.pdf", §82.4.1.2/3 about <men: GARPersonMEF>)
				 */

				// Get the list of "enseignement" within this structure
				EntityTransaction transaction = em.getTransaction();
				transaction.begin();
				Query emQuery = em.createQuery("SELECT se FROM StaffEntity se WHERE se.primaryKey.uai = '" + ENTStructureUAI + "'");
				@SuppressWarnings("unchecked")
				List<StaffEntity> staff = emQuery.getResultList();
				transaction.commit();

				// Iterate over teachers / students & "enseignements"
				List<String> distinctMEFs = new ArrayList<>();
				List<String> distinctMatieres = new ArrayList<>();

				for (StaffEntity person : staff) {
					for (EnseignementEntity enseignement : person.getEnseignements()) {
						if (EnseignementEntity.ENSEIGNEMENT_TYPE.MEF.equals(enseignement.getType()) && !distinctMEFs.contains(enseignement.getCode())) {
							distinctMEFs.add(enseignement.getCode());
							Map<String, String> mefinfo = getMEFInfo(enseignement.getSourceSI(), enseignement.getCode());
							if (!mefinfo.isEmpty() && StringUtils.isNotBlank(mefinfo.get("MEFLIBELLE"))) {
								GARMEF garMEF = factory.createGARMEF();
								garMEF.setGARMEFCode(enseignement.getCode());
								garMEF.setGARStructureUAI(ENTStructureUAI);
								garMEF.setGARMEFLibelle(mefinfo.get("MEFLIBELLE"));
								garMEF.setGARMEFRattach(mefinfo.get("MEFRATTACH")); // can be null since optional
								garMEF.setGARMEFSTAT11(mefinfo.get("MEFSTAT11")); // can be null since optional
								writer.add(garMEF);
							} else {
								jobActivity.setTrafficLight(ActivityTrafficLight.RED);
								log.error("Failed to get the 'MEF' information (at least the mandatory label is unknown) associated to the code '" + enseignement.getCode() + "' (Type=" + person.getType() + ", ENTPersonUid=" + person.getPrimaryKey().getUuid() + ", UAI=" + ENTStructureUAI + ")");								
								continue;  // skip this 'MEF' code since its label cannot be resolved from AAF
							}
						} else if ((EnseignementEntity.ENSEIGNEMENT_TYPE.CLASSE_MATIERE.equals(enseignement.getType())
								|| EnseignementEntity.ENSEIGNEMENT_TYPE.GROUPE_MATIERE.equals(enseignement.getType())
								|| EnseignementEntity.ENSEIGNEMENT_TYPE.DISCIPLINE.equals(enseignement.getType()))
								&& (StringUtils.isNotBlank(enseignement.getCode())) // This criterion is used to discard recordings of students' divisions and groups (go & see GAREleveBuilder)
								&& !distinctMatieres.contains(enseignement.getCode())) {
							distinctMatieres.add(enseignement.getCode());
							String libelle = getLibelleMatiere(enseignement.getSourceSI(), enseignement.getCode());
							if (StringUtils.isNotBlank(libelle)) {
								GARMatiere garMatiere = factory.createGARMatiere();
								garMatiere.setGARMatiereCode(enseignement.getCode());
								garMatiere.setGARStructureUAI(ENTStructureUAI);
								garMatiere.setGARMatiereLibelle(libelle);
								writer.add(garMatiere);
							} else {
								jobActivity.setTrafficLight(ActivityTrafficLight.RED);
								log.error("Failed to get the 'Matiere' label associated to the code '" + enseignement.getCode() + "' (Type=" + person.getType() + ", ENTPersonUid=" + person.getPrimaryKey().getUuid() + ", UAI=" + ENTStructureUAI + ")");								
								continue;  // skip this 'matière' code since its label cannot be resolved from AAF
							}
						}
					}
				}
			}

			// Flush the possibly remaining entities
			writer.flush();
		} catch (JAXBException | FileNotFoundException | SAXException e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			log.error("Failed to execute the GAR loader, error: " + (StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getCause()));
		}

	}

	private String getLibelleMatiere(final String sourceSI, final String code) throws AlambicException {
		String libelle = null;

		try {
			// query AAF's index
			String query = String.format("{\"api\":\"/%s/_search\",\"parameters\":\"q=identifiant:%s\"}", GARHelper.getInstance().getIndexationAlias(sourceSI, GARHelper.INDEXATION_OBJECT_TYPE.Matiere), code);
			List<Map<String, List<String>>> resultSet = this.aafSource.query(query);
			
			// perform controls
			if (null != resultSet && 0 < resultSet.size()) {
				Map<String, List<String>> item = resultSet.get(0); // a single item is expected
				JSONObject jsonResultSet = new JSONObject(item.get("item").get(0));
				if (0 < jsonResultSet.getJSONObject("hits").getInt("total")) {
					JSONObject mat = jsonResultSet.getJSONObject("hits").getJSONArray("hits").getJSONObject(0);
					libelle = mat.getJSONObject("_source").getString("libelle");
				}
			}
		} catch (Exception e) {
			throw new AlambicException(e.getMessage());
		}

		return libelle;
	}

	private Map<String, String> getMEFInfo(final String sourceSI, final String code) throws AlambicException {
		Map<String, String> info = new HashMap<>();

		try {
			// query AAF's index
			String query = String.format("{\"api\":\"/%s/_search\",\"parameters\":\"q=identifiant:%s\"}", GARHelper.getInstance().getIndexationAlias(sourceSI, GARHelper.INDEXATION_OBJECT_TYPE.MEF), code);
			List<Map<String, List<String>>> resultSet = this.aafSource.query(query);
			
			// perform controls
			if (null != resultSet && 0 < resultSet.size()) {
				Map<String, List<String>> item = resultSet.get(0); // a single item is expected
				JSONObject jsonResultSet = new JSONObject(item.get("item").get(0));
				if (0 < jsonResultSet.getJSONObject("hits").getInt("total")) {
					JSONObject mef = jsonResultSet.getJSONObject("hits").getJSONArray("hits").getJSONObject(0);
					JSONObject mefSource = mef.getJSONObject("_source");
					info.put("MEFLIBELLE", mefSource.getString("libelle"));
					info.put("MEFRATTACH", mefSource.getString("rattachement"));
					info.put("MEFSTAT11", mefSource.getString("stat11"));
				}
			}
		} catch (Exception e) {
			throw new AlambicException(e.getMessage());
		}

		return info;
	}

	private class GAREtablissementWriter {

		private int nodeCount;
		private final ObjectFactory factory;
		private GARENTEtab container;
		private final Marshaller marshaller;
		private final int maxNodesCount;
		private final String version;
		private final int page;

		protected GAREtablissementWriter(final ObjectFactory factory, final String version, final int page, final int maxNodesCount) throws JAXBException, SAXException {
			this.factory = factory;
			this.version = version;
			this.page = page;
			this.maxNodesCount = maxNodesCount;
			nodeCount = 0;
			container = factory.createGARENTEtab();
			container.setVersion(version);
			JAXBContext context = JAXBContext.newInstance(GARENTEtab.class);
			marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

			// Install schema validation
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(new StreamSource(xsdFile));
			marshaller.setSchema(schema);
		}

		protected <T> GARENTEtab add(final T item) throws FileNotFoundException, JAXBException {
			if (item instanceof GAREtab) {
				container.getGAREtab().add((GAREtab) item);
			} else if (item instanceof GARMEF) {
				container.getGARMEF().add((GARMEF) item);
			} else if (item instanceof GARMatiere) {
				container.getGARMatiere().add((GARMatiere) item);
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
			JAXBElement<GARENTEtab> jaxbElt = factory.createGARENTEtab(container);
			marshaller.marshal(jaxbElt, new FileOutputStream(outputFileName));
			container = factory.createGARENTEtab();
			container.setVersion(version);
		}

	}

}
