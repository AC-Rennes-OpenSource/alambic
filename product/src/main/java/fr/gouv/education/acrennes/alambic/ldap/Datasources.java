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
package fr.gouv.education.acrennes.alambic.ldap;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.SqlToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import org.jdom2.Element;

public class Datasources {
	private final Map<String, Object> dataSourceList = new HashMap<String, Object>();
	private Variables variables = null;
	private Element xmlNode = null;

	/*
	 * Methode pour nettoyer les datasources ouverts
	 */
	public Datasources(final Element xmlNode, final Variables variables) throws SQLException, ClassNotFoundException, AlambicException {
		this.variables = variables;
		this.xmlNode = xmlNode;
		loadDataSourceList();
	}

	public void close() {
		// Fermeture des datasources ouverts

		for (Object objet : dataSourceList.values()) {
			// Traitement des datasources SQL
			if (objet instanceof SqlToStateBase) {
				((SqlToStateBase) objet).close();
			}
		}

	}

	private void loadDataSourceList() throws SQLException, ClassNotFoundException, AlambicException {
		if (xmlNode.getChild("datasources") != null) {
			List<Element> children = xmlNode.getChild("datasources").getChildren();
			for (Element dataSource : children) {
				// Instantiation de l'objet ExtractionSql
				if ("sql".equalsIgnoreCase(dataSource.getAttributeValue("type"))) {
					String login = dataSource.getChildText("login");
					if (login != null) {
						login = variables.resolvString(login);
					}

					String pwd = dataSource.getChildText("passwd");
					if (pwd != null) {
						pwd = variables.resolvString(pwd);
					}
					SqlToStateBase sb = null;
					if (pwd != null && login != null) {
						sb = new SqlToStateBase(variables.resolvString(dataSource.getChildText("driver")),
								variables.resolvString(dataSource.getChildText("uri")), "NONE", login, pwd);
					}
					else {
						sb = new SqlToStateBase(variables.resolvString(dataSource.getChildText("driver")),
								variables.resolvString(dataSource.getChildText("uri")), "NONE");
					}
					dataSourceList.put(dataSource.getAttributeValue("name"), sb);
				}
			}
		}
	}

	public Object getDatasource(final String name) {
		return dataSourceList.get(name);
	}
}
