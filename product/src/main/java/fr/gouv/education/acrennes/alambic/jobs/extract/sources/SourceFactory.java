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
package fr.gouv.education.acrennes.alambic.jobs.extract.sources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.LdapToTrash;
import fr.gouv.education.acrennes.alambic.jobs.stats.LdapStatsMdp;
import fr.gouv.education.acrennes.alambic.utils.Functions;

public class SourceFactory {

	private static final Log log = LogFactory.getLog(SourceFactory.class);

	public static Source getSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		Source source = null;

		if (null != sourceNode) {
			String sourceType = sourceNode.getAttributeValue("type");
			if ("xml".equalsIgnoreCase(sourceType)) {
				source = new XMLSource(context, sourceNode);
			} else if ("xom".equalsIgnoreCase(sourceType)) {
				source = new XOMSource(context, sourceNode);
			} else if ("none".equalsIgnoreCase(sourceType)) {
				source = new NoneSource(context, sourceNode);
			} else if ("sql".equals(sourceType)) {
				source = new SQLSource(context, sourceNode);
			} else if ("nxql".equals(sourceType)) {
				source = new NXQLSource(context, sourceNode);
			} else if ("ldap".equals(sourceType)) {
				source = new LDAPSource(context, sourceNode);
			} else if ("LdapToTrash".equals(sourceType)) {
				funcLdapDeleteEntries(context, sourceNode);
			} else if ("LdapStatsMdp".equals(sourceType)) {
				funcLdapStatMdp(context, sourceNode);
			} else if ("csv".equals(sourceType)) {
				source = new CSVSource(context, sourceNode);
			} else if ("grep".equals(sourceType)) {
				source = new GREPSource(context, sourceNode);
			} else if ("randomUserGenerator".equals(sourceType)) {
				source = new RandomUserSource(context, sourceNode);
			} else if ("randomDateGenerator".equals(sourceType)) {
				source = new RandomDateSource(context, sourceNode);
			} else if ("randomUidGenerator".equals(sourceType)) {
				source = new RandomUidSource(context, sourceNode);
			} else if ("randomUUidGenerator".equals(sourceType)) {
				source = new RandomUUidSource(context, sourceNode);
			} else if ("randomIntegerGenerator".equals(sourceType)) {
				source = new RandomIntegerSource(context, sourceNode);
			} else if ("unikGenerator".equals(sourceType)) {
                source = new UnikGeneratorSource(context, sourceNode);
			} else if ("fileExplorer".equals(sourceType)) {
				source = new FileExplorerSource(context, sourceNode);
			} else if ("webService".equals(sourceType)) {
				source = new WebServiceSource(context, sourceNode);
			} else if ("baseX".equals(sourceType)) {
				source = new BaseXSource(context, sourceNode);
			} else {
				throw new AlambicException("Type de source [" + sourceType + "] inconnu.");
			}
		}

		return source;
	}

	private static void funcLdapStatMdp(final CallableContext context, final Element sourceNode) throws AlambicException {
		log.info("  -> Statistique des MdP changes [" + sourceNode.getAttributeValue("name") + "]");
		// Instantiation de l'objet ExtractionSql
		String query = Functions.getInstance().executeAllFunctions(context.resolveString(sourceNode.getChildText("query")));
		LdapStatsMdp etl = new LdapStatsMdp(context.resolveString(sourceNode.getChildText("driver")),
				context.resolveString(sourceNode.getChildText("uri")),
				context.resolveString(sourceNode.getChildText("login")),
				context.resolveString(sourceNode.getChildText("passwd")),
				query);
		try {
			etl.executeCountAction();
			log.info("     --- Nombre de comptes       : " + etl.getCountEntries());
			log.info("     --- Nombre de Mdp changes   : " + etl.getCountMdpChanged());
			log.info("     --- Nombre de Mdp inchanges : " + etl.getCountMdpUnChanged());
		} finally {
			etl.close();
		}
	}

	private static void funcLdapDeleteEntries(final CallableContext context, final Element sourceNode) throws AlambicException {
		log.info("  -> Nettoyage LDAP de l'annuaire [" + sourceNode.getAttributeValue("name") + "]");

		// Instantiation de l'objet ExtractionSql
		String query = Functions.getInstance().executeAllFunctions(context.resolveString(sourceNode.getChildText("query")));
		LdapToTrash etl = new LdapToTrash(context.resolveString(sourceNode.getChildText("driver")),
				context.resolveString(sourceNode.getChildText("uri")),
				context.resolveString(sourceNode.getChildText("login")),
				context.resolveString(sourceNode.getChildText("passwd")),
				query);
		try {
			etl.executeDeleteAction();

			log.info("     ->Nombre d'entrees effacees : " + etl.getCountEntriesDeleted());
		} finally {
			etl.close();
		}
	}

}
