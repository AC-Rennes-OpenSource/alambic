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
package fr.gouv.education.acrennes.alambic.jobs.extract.clients;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.gouv.education.acrennes.alambic.jobs.load.StateBaseToLdapDelete;

/**
 * @deprecated
 * The destination {@link StateBaseToLdapDelete} must be prefered.
 * @author mberhaut1
 */
@Deprecated
public class LdapToTrash {
	private static final Log log = LogFactory.getLog(LdapToTrash.class);

	private int countEntriesDeleted = 0;

	private final Hashtable<String, String> confLdap = new Hashtable<>(5);
	private final SearchControls contraintes = new SearchControls();
	protected DirContext ctx = null;
	protected NamingEnumeration<SearchResult> searchRes;

	public LdapToTrash(final String driver, final String url, final String login, final String pwd, final String query) {
		confLdap.put(Context.INITIAL_CONTEXT_FACTORY, driver);
		confLdap.put(Context.PROVIDER_URL, url);
		confLdap.put(Context.SECURITY_PRINCIPAL, login);
		confLdap.put(Context.SECURITY_CREDENTIALS, pwd);
		contraintes.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		contraintes.setReturningAttributes(new String[] { "uid" });
		
		// Execution de la requète LDAP
		try {
			ctx = new InitialDirContext(confLdap);
			searchRes = ctx.search("", query, contraintes);
		} catch (final NamingException e) {
			log.error("Erreur ouverture du contexte : " + e.getMessage());
		}
	}

	public void executeDeleteAction() {
		// tranfo vers format extration générique
		countEntriesDeleted = 0;
		try {
			while (searchRes.hasMore()) {
				final SearchResult sR = searchRes.next();
				final String rdn = new String(sR.getName());
				ctx.unbind(rdn);
				log.info("Effacement de l'entree [" + rdn + "]");
				countEntriesDeleted++;
			}
			searchRes.close();
		} catch (final NamingException e) {
			// Erreur à l'effacement
			log.error("Effacement de l'entree : " + e.getMessage(), e);
		}
	}

	public int getCountEntriesDeleted() {
		return countEntriesDeleted;
	}

	public void close() {
		try {
			searchRes.close();
		} catch (final NamingException e) {
			log.error("Fermeture du contexte : " + e.getMessage(), e);
		}
		try {
			ctx.close();
		} catch (final NamingException e) {
			log.error("Fermeture du contexte : " + e.getMessage(), e);
		}
	}

}