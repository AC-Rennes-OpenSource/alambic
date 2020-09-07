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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.ElasticToStateBase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

public class ElasticSource extends AbstractSource {
	
	private static final Log log = LogFactory.getLog(ElasticSource.class);
	private static Pattern SCROLL_REGEX_PATTERN = Pattern.compile("^\\d+(d|h|m|s|ms|micros|nanos)$");
	private static String SCROLL_DEFAULT_VALUE = "5m";

	private int pageSize;
	private String scroll;

	public ElasticSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(Element sourceNode) throws AlambicException {
		
		String uri = sourceNode.getChildText("uri");
		if (StringUtils.isBlank(uri)) {
			log.error("l'uri du cluster Elastic n'est pas precisée");
		} else {
			uri = context.resolveString(uri);
			log.debug("uri=" + uri);
		}

		String index_alias = sourceNode.getChildText("index_alias");
		if (StringUtils.isNotBlank(index_alias)) {
			index_alias = context.resolveString(index_alias);
		} else {
			log.debug("l'index ou alias cible n'est pas precisé.");
		}

		// Get proxy configuration
		String proxy_host = null;
		String proxy_port = null;
		Element proxyElt = sourceNode.getChild("proxy");
		if (null != proxyElt) {
			proxy_host = context.resolveString(proxyElt.getChildText("host"));
			proxy_port = context.resolveString(proxyElt.getChildText("port"));
		} else {
			log.debug("Pas de configuration de proxy");
		}
		
		// Get authentication configuration
		String auth_login = null;
		String auth_password = null;
		XPathFactory xpf = XPathFactory.instance();
		XPathExpression<Element> xpath = xpf.compile(".//authentication/credentials", Filters.element());
		List<Element> credentialsElts = xpath.evaluate(sourceNode);
		if (null != credentialsElts && !credentialsElts.isEmpty()) {
			auth_login = context.resolveString(credentialsElts.get(0).getChildText("login"));
			auth_password = context.resolveString(credentialsElts.get(0).getChildText("password"));
		} // else, no authentication
		
		// Get the query
		query = context.resolveString(sourceNode.getChildText("query")); // might be empty
		
		// Get the connection timeout
		String timeout = sourceNode.getAttributeValue("connectionTimeout");

		this.scroll = "";
		String page = sourceNode.getAttributeValue("page");
		if (StringUtils.isNotBlank(page)) {
			this.pageSize = Integer.parseInt(context.resolveString(page));
			this.scroll = sourceNode.getChildText("scroll");
			if (StringUtils.isNotBlank(scroll)) {
				this.scroll = context.resolveString(scroll).toLowerCase();
				Matcher m = SCROLL_REGEX_PATTERN.matcher(scroll);
				if (!m.matches()) {
					throw new AlambicException("Le paramètre <scroll> '" + this.scroll + "' ne respecte pas le format de l'API Elastic (https://www.elastic.co/guide/en/elasticsearch/reference/6.8/common-options.html#time-units)");
				}
			} else {
				this.scroll = SCROLL_DEFAULT_VALUE;
				log.info("La durée de conservation du contexte de recherche est positionnée à la valeur par défaut : " + SCROLL_DEFAULT_VALUE);
			}
		} else {
			this.pageSize = 0;
		}

		setClient(new ElasticToStateBase(uri, proxy_host, proxy_port, index_alias, this.scroll, timeout, auth_login, auth_password));
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException {
		return getClient().getPageIterator(this.query, null, this.pageSize, null, null);
	}

}
