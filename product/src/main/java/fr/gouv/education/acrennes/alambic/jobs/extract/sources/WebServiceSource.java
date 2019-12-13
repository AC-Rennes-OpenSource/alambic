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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.WSToStateBase;

public class WebServiceSource extends AbstractSource{
	
	private static final Log LOG = LogFactory.getLog(WebServiceSource.class);
	
	public WebServiceSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(Element sourceNode) throws AlambicException {
		
		String uri = sourceNode.getChildText("uri");
		if (StringUtils.isBlank(uri)) {
			LOG.error("l'uri du webservice n'est pas precisée");
		} else {
			uri = context.resolveString(uri);
			LOG.debug("uri=" + uri);
		}

		String method = sourceNode.getChildText("method");
		if (StringUtils.isNotBlank(method)) {
			method = context.resolveString(method);
			LOG.debug("method=" + method);
		} else {
			method = HttpMethod.GET;
			LOG.warn("la methode à utiliser pour l'appel du webservice n'est pas precisée. Par défaut = GET");
		}
		
		// Get proxy configuration
		String proxy_host = null;
		String proxy_port = null;
		Element proxyElt = sourceNode.getChild("proxy");
		if (null != proxyElt) {
			proxy_host = context.resolveString(proxyElt.getChildText("host"));
			proxy_port = context.resolveString(proxyElt.getChildText("port"));
			LOG.debug("proxy=" + proxy_host + ":" + proxy_port);
		} else {
			LOG.debug("Pas de configuration de proxy");
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
			LOG.debug("authentification.credentials.login=" + auth_login);
		} else {
			LOG.debug("Pas d'authentification configurée");
		}
		
		// Get the query
		query = context.resolveString(sourceNode.getChildText("query")); // might be empty
		
		// Get the connection timeout
		String timeout = sourceNode.getAttributeValue("connectionTimeout");
		LOG.debug("connectionTimeout=" + timeout);

		// Get the API success codes list
		List<Integer> successResponseCodes = new ArrayList<>();
		xpf = XPathFactory.instance();
		xpath = xpf.compile("./response_codes/code[@type=\"success\"]", Filters.element());
		List<Element> successCodes = xpath.evaluate(sourceNode);
		if (null != successCodes && !successCodes.isEmpty()) {
			for (Element code : successCodes) {
				successResponseCodes.add(Integer.parseInt(code.getText()));
			}
		} else {
			successResponseCodes.add(HttpStatus.SC_OK);
		}

		// Get the request headers
		Map<String, String> headers_map = new HashMap<>();
		Element headersElts = sourceNode.getChild("headers");
		if (null != headersElts && 0 < headersElts.getChildren().size()) {
			for (Element headerElt : headersElts.getChildren()) {
				headers_map.put(headerElt.getAttributeValue("name"), headerElt.getText());
			}
		}

		setClient(new WSToStateBase(uri, method, headers_map, proxy_host, proxy_port, timeout, null, auth_login, auth_password, successResponseCodes));
	}

}
