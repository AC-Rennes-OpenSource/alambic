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

import java.util.Properties;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.utils.LdapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class LdapExtraction {
	private static final Log log = LogFactory.getLog(LdapExtraction.class);

	private final Properties confLdap;
	private SearchControls contraintes = new SearchControls();
	protected DirContext ctx = null;
	protected NamingEnumeration<SearchResult> searchRes;

	public LdapExtraction(final CallableContext context, final Element sourceNode, final String query, final String[] attributeList) throws AlambicException {
		confLdap = LdapUtils.getLdapConfiguration(context, sourceNode, false);
		contraintes.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		if (attributeList!=null){
			contraintes.setReturningAttributes(attributeList);
		}
		//Execution de la requète LDAP
		try {
			ctx = new InitialDirContext(confLdap);
			searchRes = ctx.search("",query,contraintes);
		} catch (NamingException e) {
			log.error("Erreur ouverture du contexte : "+e.getMessage(), e);
		}
	}

	public void close(){
		try {
			searchRes.close();
			ctx.close();
		} catch (NamingException e) {
			log.error("Fermeture du contexte : "+e.getMessage(), e);
		}
	}
}

