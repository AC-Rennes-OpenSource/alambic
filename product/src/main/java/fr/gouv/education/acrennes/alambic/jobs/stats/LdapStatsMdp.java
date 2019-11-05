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
package fr.gouv.education.acrennes.alambic.jobs.stats;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;
import fr.gouv.education.acrennes.alambic.ldap.LdapExtraction;
import fr.gouv.education.acrennes.alambic.utils.EncodeUtils;
import fr.gouv.education.acrennes.alambic.utils.Functions;

public class LdapStatsMdp extends LdapExtraction {

	private int countEntries = 0;
	private int countMdpChanged = 0;
	private int countMdpUnChanged = 0;

	public LdapStatsMdp(final String driver, final String url, final String login, final String pwd, final String query) {
		super(driver, url, login, pwd, query, new String[] { "uid", "ENTPersonDateNaissance", "userPassword" });
	}

	public void executeCountAction() {
		// tranfo vers format extration générique
		try {
			while (searchRes.hasMore()) {
				SearchResult sR = searchRes.next();
				String rdn = new String(sR.getName());
				if (isChanged(sR)) {
					countMdpChanged++;
					logger.debug("Mot de passe de " + rdn + " change : O");
				}
				else {
					countMdpUnChanged++;
					logger.debug("Mot de passe de " + rdn + " change : N");
				}
				countEntries++;
			}
		} catch (NamingException e) {
			logger.error("Comparaison MdP / valeur par defaut : " + e.getMessage());
		}
	}

	private boolean isChanged(final SearchResult sR) {

		try {
			Attribute attrValDef = sR.getAttributes().get("entpersondatenaissance");
			Attribute attrMdp = sR.getAttributes().get("userPassword");

			if (attrMdp == null || attrValDef == null) {
				return false;
			}

			String mdp = Functions.getInstance().valueToString(attrMdp.get(0));
			String valDef = Functions.getInstance().valueToString(attrValDef.get(0));
			valDef = valDef.replace("/", "");
			String mdpDef = "{SHA}" + EncodeUtils.fonctionBase64Sha1(valDef);

			return !mdp.equals(mdpDef);
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
		} catch (NamingException e) {
			logger.error(e.getMessage());
		}
		return false;
	}

	public int getCountMdpChanged() {
		return countMdpChanged;
	}

	public int getCountMdpUnChanged() {
		return countMdpUnChanged;
	}

	public int getCountEntries() {
		return countEntries;
	}

}
