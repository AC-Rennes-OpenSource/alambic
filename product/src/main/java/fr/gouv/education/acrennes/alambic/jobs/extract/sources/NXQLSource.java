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
package fr.gouv.education.acrennes.alambic.jobs.extract.sources;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.NxqlToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class NXQLSource extends AbstractSource {

	private static final Log log = LogFactory.getLog(NXQLSource.class);

	private int pageSize;
	private String sortBy;
	private String orderBy;

	public NXQLSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(final Element sourceNode) throws AlambicException {
		String uri = sourceNode.getChildText("uri");
		if (StringUtils.isBlank(uri)) {
			log.error("l'uri de l'instance Nuxeo n'est pas precisée");
		} else {
			uri = context.resolveString(uri);
		}

		String login = sourceNode.getChildText("login");
		if (StringUtils.isNotBlank(login)) {
			login = context.resolveString(login);
		}

		String pwd = sourceNode.getChildText("passwd");
		if (StringUtils.isNotBlank(pwd)) {
			pwd = context.resolveString(pwd);
		}

		String schemas = sourceNode.getChildText("schemas");
		if (StringUtils.isNotBlank(schemas)) {
			schemas = context.resolveString(schemas);
		}

		String useES = sourceNode.getAttributeValue("elasticSearch");
		if (StringUtils.isNotBlank(useES)) {
			useES = context.resolveString(useES);
		}

		final String page = sourceNode.getAttributeValue("page");
		if (StringUtils.isNotBlank(page)) {
			pageSize = Integer.parseInt(context.resolveString(page));
			/*
			 * This sesction is commented since the Nuxeo operation Document.Query doesn't support result paging. Nervertheless, it is kept in source code
			 * for Nuxeo versions that might support it later.
			 * Fortunately, the ElasticSearch API Document.QueryES supports the result paging.
			 * ------------------------------------------------------------------
			 * sortBy = sourceNode.getChildText("sortBy");
			 * if (StringUtils.isNotBlank(sortBy)) {
			 * sortBy = context.resolveString(sortBy);
			 * } else {
			 * log.error("Le critère de tri est obligatoire pour les recherches paginées, ex : <sortBy>sn</sortBy>");
			 * }
			 * orderBy = sourceNode.getChildText("orderBy");
			 * if (StringUtils.isNotBlank(orderBy)) {
			 * orderBy = context.resolveString(orderBy);
			 * } else {
			 * log.error("Le critère de classement est obligatoire pour les recherches paginées, ex : <orderBy>ASC</orderBy>");
			 * }
			 */
		}

		query = sourceNode.getChildText("query");
		query = Functions.getInstance().executeAllFunctions(context.resolveString(query));
		if (StringUtils.isBlank(query) && !isDynamic()) {
			log.error("Requete non définie.");
		} else {
			query = context.resolveString(query);
		}

		try {
			setClient(new NxqlToStateBase(uri, login, pwd, Boolean.parseBoolean(useES), schemas, Float.valueOf(version)));
		} catch (final Exception e) {
			log.error("Failed to instanciate the client of source '" + getName(), e);
		}
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException {
		return getClient().getPageIterator(query, scope, pageSize, sortBy, orderBy);
	}

}
