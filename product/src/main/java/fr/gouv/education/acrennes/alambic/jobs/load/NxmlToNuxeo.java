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
package fr.gouv.education.acrennes.alambic.jobs.load;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.xml.sax.InputSource;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.nuxeo.ACE;
import fr.gouv.education.acrennes.alambic.nuxeo.ACL;
import fr.gouv.education.acrennes.alambic.nuxeo.ACP;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class NxmlToNuxeo extends AbstractDestination {

	private static final Log log = LogFactory.getLog(NxmlToNuxeo.class);

	private final HttpAutomationClient client;
	private final Session session;
	private Element pivot;
	private final DocumentService dc;
	private String inputFile;
	private String nuxeoPath;

	private static final String ACE_DELIMITER = ",";
	private static final String ACE_FORMAT = "%s:%s:%s";

	public NxmlToNuxeo(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
		super(context, job, jobActivity);

		String uri = job.getChildText("uri");
		if (uri == null) {
			throw new AlambicException("l'uri de nuxeo n'est pas precisee");
		} else {
			uri = context.resolveString(uri);
		}

		String login = job.getChildText("login");
		if (login == null) {
			throw new AlambicException("le login de nuxeo n'est pas precise");
		} else {
			login = context.resolveString(login);
		}

		String pwd = job.getChildText("passwd");
		if (pwd == null) {
			throw new AlambicException("le mot de passe de nuxeo n'est pas precise");
		} else {
			pwd = context.resolveString(pwd);
		}

		nuxeoPath = job.getChildText("path");
		if (nuxeoPath == null) {
			throw new AlambicException("le chemin nuxeo n'est pas precise");
		} else {
			nuxeoPath = context.resolveString(nuxeoPath);
		}

		inputFile = job.getChildText("input");
		if (inputFile == null) {
			throw new AlambicException("le fichier de generation des contenus nuxeo n'est pas precise");
		} else {
			inputFile = context.resolvePath(inputFile);
		}

		// Connextion à Nuxeo
		client = new HttpAutomationClient(uri);
		session = client.getSession(login, pwd);
		dc = new DocumentService(session);
	}

	@Override
	public void execute() {
		try {
			final InputSource fPivot = new InputSource(inputFile);
			pivot = (new SAXBuilder()).build(fPivot).getRootElement();
			try {
				// Itération sur la liste des entrées du pivot
				loadDocuments(pivot.getChild("documents").getChildren(), dc.getDocument(nuxeoPath));
			} catch (final Exception e) {
				jobActivity.setTrafficLight(ActivityTrafficLight.RED);
				log.error("Document [" + nuxeoPath + "] not found : " + e.getMessage());
			}
		} catch (final JDOMException e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			log.error("Analyse du fichier XML : " + e.getMessage());
		} catch (final IOException e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			log.error("Ouverture fichier XML : " + e.getMessage());
		}
	}

	private void setACP(final String path, final Element acpElement) throws Exception {
		final ACP acp = new ACP(acpElement);
		final boolean b = acp.getTtc();
		final PathRef input = new PathRef(path);

		if (b) {
			// utilisation de l'opération définie dans opentoutatice
			final OperationRequest request = session.newRequest("Document.SetACL");
			request.setInput(input);

			final ACL acl = acp.getACL();
			if (null != acl) {
				request.set("acl", acl.getName());
				request.set("overwrite", acp.getOverwrite());
				request.set("break", acp.getBlock());

				final List<String> entries = new ArrayList<String>();

				for (final ACE ace : acl.getAceList()) {
					entries.add(String.format(ACE_FORMAT, ace.getPrincipal(), ace.getPermission(), ace.getGranted()));
				}
				request.set("entries", StringUtils.join(entries, ACE_DELIMITER));
				request.execute();
			}
		} else {
			// utilisation des opérations natives nuxeo
			final ACL acl = acp.getACL();
			if (null != acl) {
				for (final ACE ace : acl.getAceList()) {
					final OperationRequest request = session.newRequest("Document.SetACE");
					request.setInput(input);
					request.set("permission", ace.getPermission());
					request.set("user", ace.getPrincipal());
					request.set("overwrite", false);
					request.execute();
				}
			}
		}
	}

	private Document setFacets(final Document nxDoc, final Element facetsElement) throws Exception {
		Document res = nxDoc;
		final List<Element> lstFacetElem = (null != facetsElement) ? facetsElement.getChildren("facet") : null;
		if ((null != lstFacetElem) && !lstFacetElem.isEmpty()) {
			// final OperationRequest request = session.newRequest("Document.Save");
			final OperationRequest request = session.newRequest("Document.AddFacets");

			final PropertyList propertyListFacets = nxDoc.getFacets();
			final StringBuilder facets = new StringBuilder();
			for (final Element facetElement : lstFacetElem) {
				final String valueFacet = facetElement.getAttributeValue("name");
				propertyListFacets.add(valueFacet);
				if (facets.length() != 0) {
					facets.append(",");
				}
				facets.append(valueFacet);
			}

			request.setInput(nxDoc);
			request.set("facets", facets);
			res = (Document) request.execute();
		}
		return res;
	}

	private boolean loadDocuments(final List<Element> documents, final Document parent)
			throws Exception {
		int index = 1;
		for (final Element doc : documents) {
			// activity monitoring
			jobActivity.setProgress((index  * 100) / documents.size());
			jobActivity.setProcessing("processing entry " + index++  + "/" + documents.size());

			String name = doc.getAttributeValue("name");
			final String type = doc.getAttributeValue("type");
			final String online = doc.getAttributeValue("online");
			final String setShortName = doc.getAttributeValue("setShortName");
			PropertyMap pMap;

			String parentPath = parent.getPath();
			if (!parentPath.endsWith("/")) {
				parentPath = parentPath + "/";
			}

			// Traitements des attributs du document courant
			pMap = getAttributs(doc.getChild("attributes"));

			if (StringUtils.isBlank(name)) {
				// refer to the class method "fr.toutatice.ecm.platform.core.pathsegment.ToutaticePathSegmentService::generatePathSegment()"
				name = IdUtils.generateId(pMap.getString("dc:title"), "-", true, 24);
			}

			// Vérification de l'existance du document
			Document nxDoc = null;
			try {
				nxDoc = dc.getDocument(parentPath + name);
				nxDoc = dc.update(nxDoc, pMap);
			} catch (final RemoteException e) {
				try {
					nxDoc = dc.createDocument(parent, type, name, pMap);
				} catch (final Exception ex) {
					jobActivity.setTrafficLight(ActivityTrafficLight.RED);
					log.error("Failed to create the document: name='" + name + "', type='" + type + "', parent='" + parent + "'. error: " + ex.getMessage());
					break;
				}
			} finally {
				log.info("Document '" + nxDoc.getTitle() + "' chargé");

				// Modification des droits
				final Element acpElement = doc.getChild("acp");
				if (acpElement != null) {
					setACP(nxDoc.getPath(), acpElement);
				}

				// Routine de nettoyage
				if (doc.getChild("clean") != null) {
					for (final Element docToClean : (List<Element>) doc.getChild("clean").getChildren()) {
						Document NxDocToClean = null;
						try {
							NxDocToClean = dc.getDocument(parentPath + name + "/" + docToClean.getAttributeValue("name"));

						} catch (final RemoteException e) {
							NxDocToClean = null;
						}

						if (NxDocToClean != null) {
							dc.remove(NxDocToClean);
						}
					}
				}

				// Lancement des processus d'import
				for (final Element nxImport : (List<Element>) doc.getChildren("import")) {
					fileImport(nxDoc.getPath(), nxImport);
				}

				// Lancement des processus de copy
				for (final Element path : (List<Element>) doc.getChildren("copy")) {
					copyDocs(nxDoc.getPath(), path);
				}

				// Mise à jour du short name (forcée)
				final boolean doSetShortName = Boolean.parseBoolean(setShortName);
				if (doSetShortName) {
					setDocumentShortName(nxDoc);
				}

				// Mise en ligne
				final boolean doSetOnline = Boolean.parseBoolean(online);
				if (doSetOnline) {
					setOnlineDocument(nxDoc);
				}

				// Mise à jour des facets
				final Element facetsElement = doc.getChild("facets");
				if (facetsElement != null) {
					setFacets(nxDoc, facetsElement);
				}

			}

			// Traitement des sous-documents
			if (doc.getChild("documents") != null) {
				loadDocuments(doc.getChild("documents").getChildren(), nxDoc);
			}
		}
		return true;
	}

	private PropertyMap getAttributs(final Element document) throws Exception {
		final PropertyMap result = new PropertyMap();

		final List<Element> attrList = (null != document) ? document.getChildren("attr") : null;
		if ((null != attrList) && !attrList.isEmpty()) {
			for (final Element attr : attrList) {
				final String attrValue = getAttributValue(attr);
				if (null != attrValue) {
					result.set(attr.getAttributeValue("name"), attrValue);
				}
			}
		}

		return result;
	}

	private String getAttributValue(final Element attr) throws Exception {
		StringBuilder result = null;

		final String mode = attr.getAttributeValue("modifyMode");
		final List<Element> values = attr.getChildren("value");
		if ((null != values) && !values.isEmpty()) {
			result = new StringBuilder();
			for (final Element value : values) {
				final String vval = getValueValue(value, mode);
				if (null != vval) {
					result.append((result.length() == 0) ? vval : "," + vval);
				}
			}
		}

		return ((null != result) && ((result.length() > 0) || "force".equals(mode))) ? result.toString() : null;
	}

	private String getValueValue(final Element value, final String mode) throws Exception {
		Object result = null;

		final List<Element> attrList = value.getChildren("attr");
		if ((null != attrList) && !attrList.isEmpty()) {
			// complex value
			if ("item".equals(attrList.get(0).getAttributeValue("name"))) {
				// list of complex metadata
				final JSONArray jsa = new JSONArray();
				for (final Element attr : attrList) {
					final String attrValue = getAttributValue(attr);
					if (null != attrValue) {
						jsa.add(attrValue);
					}
				}
				result = jsa;
			} else {
				// scalar complex metadata
				final JSONObject jso = new JSONObject();
				for (final Element attr : attrList) {
					final String attrName = attr.getAttributeValue("name");
					final String attrValue = getAttributValue(attr);
					if (null != attrValue) {
						jso.element(attrName, attrValue);
					}
				}
				result = (!jso.isEmpty()) ? jso : null;
			}
		} else {
			// scalar value
			result = value.getText();
		}

		return ((null != result) && (StringUtils.isNotBlank(result.toString().trim()) || "force".equals(mode))) ? result.toString().trim() : null;
	}

	private void copyDocs(final String target, final Element src) {
		// Document srcDoc = dc.getDocument(src);
		// dc.query(query)
	}

	private void fileImport(final String path, final Element nxImport) throws Exception {
		final PathRef docPathRef = new PathRef(path); // emplacement où se fera l'import
		OperationRequest request = session.newRequest("Document.Fetch");
		request.set("value", docPathRef);
		final Document currentDocument = (Document) request.execute();

		request = session.newRequest("FileManager.Import");
		final String filePath = nxImport.getAttributeValue("file");
		if (filePath != null) {
			final File exportFile = new File(filePath);
			final FileBlob exportBlob = new FileBlob(exportFile);
			request.setInput(exportBlob);
			request.setContextProperty("currentDocument", currentDocument.getInputRef().replaceAll("doc:", ""));
			request.execute();
		}
	}

	private Document setOnlineDocument(final Document document) {
		Document result = null;

		try {
			final OperationRequest request = session.newRequest("Document.SetOnLineOperation");
			request.setInput(document);
			result = (Document) request.execute();
		} catch (final Exception e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			log.error("Failed to set document online ('" + document.getTitle() + "'), error: " + e.getMessage());
		}

		return result;
	}

	private Document setDocumentShortName(final Document document) {
		Document result = null;

		try {
			final OperationRequest request = session.newRequest("Document.SetDocumentShortName");
			request.setInput(document);
			request.set("Override", true);
			result = (Document) request.execute();
		} catch (final Exception e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			log.error("Failed to set document short name ('" + document.getTitle() + "'), error: " + e.getMessage());
		}

		return result;
	}

	@Override
	public void close() throws AlambicException {
		super.close();
		if (client != null) {
			client.shutdown();
		}
	}

}
