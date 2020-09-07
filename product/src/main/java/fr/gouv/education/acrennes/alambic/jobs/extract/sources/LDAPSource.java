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
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.LdapToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class LDAPSource extends AbstractSource {

	private static final Log log = LogFactory.getLog(LDAPSource.class);

	private int pageSize;
	private String sortBy;

	public LDAPSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(final Element sourceNode) throws AlambicException {
		query = sourceNode.getChildText("query");
		query = Functions.getInstance().executeAllFunctions(context.resolveString(query));
		if (StringUtils.isBlank(query) && !isDynamic()) {
			log.error("Requete non définie.");
		} else if (!isDynamic()) {
			log.info("  Requete LDAP : " + query);
			log.info("  -> lecture de l'annuaire [" + sourceNode.getAttributeValue("name") + "], requete = " + query);
			query = context.resolveString(query);
		}

		String[] listeAttributs = null;
		if (sourceNode.getChild("attributeList") != null) {
			String attributesString = context.resolveString(sourceNode.getChildText("attributeList"));
			listeAttributs = attributesString.split(",");
		}

		String driver = sourceNode.getChildText("driver") == null ? "com.sun.jndi.ldap.LdapCtxFactory" : context.resolveString(sourceNode.getChildText("driver"));

		String uri = sourceNode.getChildText("uri");
		if (uri == null) {
			log.error("l'uri de l'annuaire n'est pas precisee");
		} else {
			uri = context.resolveString(uri);
		}

		String login = sourceNode.getChildText("login");
		if (login == null) {
			log.error("le login de l'annuaire n'est pas precise");
		} else {
			login = context.resolveString(login);
		}

		String pwd = sourceNode.getChildText("passwd");
		if (pwd == null) {
			log.error("le mot de passe de l'annuaire n'est pas precise");
		} else {
			pwd = context.resolveString(pwd);
		}

		scope = sourceNode.getChildText("scope");
		if (StringUtils.isNotBlank(scope)) {
			scope = context.resolveString(scope);
		}

		String page = sourceNode.getAttributeValue("page");
		if (StringUtils.isNotBlank(page)) {
			pageSize = Integer.parseInt(context.resolveString(page));
			sortBy = sourceNode.getChildText("sortBy");
			if (StringUtils.isNotBlank(sortBy)) {
				sortBy = context.resolveString(sortBy);
			} else {
				log.error("Le critère de tri est obligatoire pour les recherches paginées, ex : <sortBy>sn</sortBy>");
			}
		}

		setClient(new LdapToStateBase(driver, uri, login, pwd, listeAttributs));
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException {
		return getClient().getPageIterator(query, scope, pageSize, sortBy, null);
	}

}
