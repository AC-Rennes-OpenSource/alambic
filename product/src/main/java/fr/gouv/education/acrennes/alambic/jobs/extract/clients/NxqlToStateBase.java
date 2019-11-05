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
package fr.gouv.education.acrennes.alambic.jobs.extract.clients;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.naming.NamingException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.nuxeo.marshaller.EsMarshaller;

public class NxqlToStateBase implements IToStateBase {

	private static final Log log = LogFactory.getLog(NxqlToStateBase.class);

	private List<Map<String, List<String>>> results = new ArrayList<>();
	private Documents searchResultSet;
	private HttpAutomationClient client;
	private final Session session;
	private boolean useElasticSearch = false;
	private String schemas = "dublincore";
	private NuxeoResultsPageIterator pageIterator;
	private final float version;

	public NxqlToStateBase(final String uri, final String login, final String password, final float version) throws SQLException, ClassNotFoundException {
		this(uri, login, password, false, null, version);
	}

	public NxqlToStateBase(final String uri, final String login, final String password, final boolean useES, final String schemas, final float version) {
		useElasticSearch = useES;
		this.version = version;
		if (StringUtils.isNotBlank(schemas)) {
			this.schemas = schemas;
		}

		// Connextion à Nuxeo
		client = new HttpAutomationClient(uri);
		session = client.getSession(login, password);

		JsonMarshalling.addMarshaller(new EsMarshaller());
	}

	@Override
	public void executeQuery(final String query) {
		executeQuery(query, null);
	}

	@Override
	public void executeQuery(final String query, final String scope) {
		OperationRequest request;

		try {
			if (useElasticSearch) {
				request = session.newRequest("Document.QueryES");
				request.set("X-NXDocumentProperties", schemas);
			} else {
				request = session.newRequest((8 < version) ? "Repository.Query" : "Document.Query");
				request.setHeader("X-NXDocumentProperties", schemas);
			}
			request.set("query", query);
			searchResultSet = (Documents) request.execute();
		} catch (final Exception e) {
			log.error("Failed execute the query ('" + query + "')", e);
		}
	}

	@Override
	public List<Map<String, List<String>>> getStateBase() {
		return getStateBase(searchResultSet);
	}

	public List<Map<String, List<String>>> getStateBase(final Documents documents) {
		// NdKLH : pas fan de reconstruire une liste à chaque appel de cette méthode, même quand executeQuery n'est pas
		//         appelé entre les appels... mais pour une correction rapide, cela devrait suffire.
		results = new ArrayList<>();
		final Iterator<Document> itr = documents.iterator();
		while (itr.hasNext()) {
			final Document document = itr.next();
			final Map<String, List<String>> item = new HashMap<>();
			item.put("UUID", Arrays.asList(document.getId()));
			item.put("PATH", Arrays.asList(document.getPath()));
			item.put("TITLE", Arrays.asList(document.getTitle()));
			item.put("PROPERTIES", Arrays.asList(getSerializedProperties(document)));
			results.add(item);
		}
		return results;
	}

	@Override
	public int getCountResults() {
		return results.size();
	}

	@Override
	public void close() {
		if (client != null) {
			try {
				client.shutdown();
			} catch (final Exception e) {
				log.error("Failed to close NXQL client.", e);
			} finally {
				client = null;
			}
		}

		if (null != pageIterator) {
			try {
				pageIterator.close();
			} catch (final NamingException e) {
				log.error("Failed to close the LDAP page iterator.", e);
			} finally {
				pageIterator = null;
			}
		}
	}

	private String getSerializedProperties(final Document document) {
		String serializedObj = "";

		try {
			final PropertyMap properties = document.getProperties();
			final byte[] bytes = SerializationUtils.serialize(properties);
			final byte[] b64rep = Base64.encodeBase64(bytes);
			serializedObj = new String(b64rep, Charsets.UTF_8);
		} catch (final Exception e) {
			log.error(e.getMessage());
		}

		return serializedObj;
	}

	@Override
	public void clear() {
		results.clear();
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy)
			throws AlambicException {
		if (useElasticSearch) {
			pageIterator = new NuxeoResultsPageIterator(session, query, scope, pageSize, sortBy, orderBy);
		} else {
			/*
			 * The Nuxeo operation Document.Query doesn't support result paging. Fortunately, the ElasticSearch API
			 * Document.QueryES does.
			 */
			throw new AlambicException("The NXQL source doesn't support results paging if ElasticSearch API is not selected");
		}
		return pageIterator;
	}

	public class NuxeoResultsPageIterator implements Iterator<List<Map<String, List<String>>>> {

		private final Log log = LogFactory.getLog(NuxeoResultsPageIterator.class);

		private List<Map<String, List<String>>> entries;
		private int offset;
		// private final int total;
		private final int pageSize;
		private final String query;
		private final String sortBy;
		private final String orderBy;
		private final Session session;

		public NuxeoResultsPageIterator(final Session session, final String query, final String scope, final int pageSize, final String sortBy, final String orderBy) {
			offset = 0;
			// total = 0;
			this.pageSize = pageSize;
			this.query = query;
			this.sortBy = sortBy;
			this.orderBy = orderBy;
			this.session = session;
		}

		@Override
		public boolean hasNext() {
			entries = Collections.emptyList();
			try {
				OperationRequest request;
				if (useElasticSearch) {
					request = session.newRequest("Document.QueryES");
					request.set("X-NXDocumentProperties", schemas);
					request.set("pageSize", pageSize);
					request.set("currentPageIndex", offset);
				} else {
					request = session.newRequest("Document.Query");
					request.setHeader("X-NXDocumentProperties", schemas);
					request.set("sortBy", sortBy);
					request.set("sortOrder", orderBy);
					request.set("pageSize", pageSize);
					request.set("currentPageIndex", offset);
				}
				request.set("query", query);
				entries = getStateBase((Documents) request.execute());
				offset++;
			} catch (final Exception e) {
				log.error("Failed to instanciate the Nuxeo source page iterator." , e);
			}
			return !entries.isEmpty();
		}

		@Override
		public List<Map<String, List<String>>> next() {
			return entries;
		}

		@Override
		public void remove() {
			log.error("Not supported operation");
		}

		public void close() throws NamingException {
			// no-op
		}

	}

}
