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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.SqlToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SQLSource extends AbstractSource {

	private static final Log log = LogFactory.getLog(SQLSource.class);

	private int pageSize;

	public SQLSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(final Element sourceNode) throws AlambicException {
		String login = sourceNode.getChildText("login");
		if (login != null) {
			login = context.resolveString(login);
		}

		String pwd = sourceNode.getChildText("passwd");
		if (pwd != null) {
			pwd = context.resolveString(pwd);
		}

		String uri = sourceNode.getChildText("uri");
		if (uri == null) {
			log.error("l'uri de la base de donnée n'est pas precisee");
		} else {
			uri = context.resolveString(uri);
		}

		String driver = sourceNode.getChildText("driver");
		if (driver == null) {
			log.error("le driver SQL n'est pas precise");
		} else {
			driver = context.resolveString(sourceNode.getChildText("driver"));
		}

		query = sourceNode.getChildText("query");
		query = Functions.getInstance().executeAllFunctions(context.resolveString(query));
		if (StringUtils.isBlank(query) && !isDynamic()) {
			log.error("Requete non définie.");
		} else {
			query = context.resolveString(query);
		}

		String page = sourceNode.getAttributeValue("page");
		if (StringUtils.isNotBlank(page)) {
			this.pageSize = Integer.parseInt(context.resolveString(page));
		} else {
			this.pageSize = 0;
		}

		String paginationMethod = sourceNode.getAttributeValue("paginationMethod");
		if (StringUtils.isNotBlank(paginationMethod)) {
			paginationMethod = context.resolveString(paginationMethod);
		} else {
			paginationMethod = "NONE";
		}

		try {
			if (login != null && pwd != null) {
				setClient(new SqlToStateBase(driver, uri, paginationMethod, login, pwd));
			} else {
				setClient(new SqlToStateBase(driver, uri, paginationMethod));
			}
		} catch (Exception e) {
			log.error("Failed to instanciate the SQL client, error:" + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException {
		return getClient().getPageIterator(this.query, null, this.pageSize, null, null);
	}

}
