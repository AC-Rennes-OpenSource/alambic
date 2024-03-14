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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.WSToStateBase;
import fr.gouv.education.acrennes.alambic.utils.JwtUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

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
		String proxyHost = null;
		String proxyPort = null;
		Element proxyElt = sourceNode.getChild("proxy");
		if (null != proxyElt) {
			proxyHost = context.resolveString(proxyElt.getChildText("host"));
			proxyPort = context.resolveString(proxyElt.getChildText("port"));
			LOG.debug("proxy=" + proxyHost + ":" + proxyPort);
		} else {
			LOG.debug("Pas de configuration de proxy");
		}

		// Get authentication configuration
		String authHeader = authentication(sourceNode);

		// Get the query
		query = context.resolveString(sourceNode.getChildText("query")); // might be empty
		
		// Get the connection timeout
		String timeout = sourceNode.getAttributeValue("connectionTimeout");
		LOG.debug("connectionTimeout=" + timeout);

		// Get the API success codes list
		List<Integer> successResponseCodes = new ArrayList<>();
		XPathFactory xpf = XPathFactory.instance();
		XPathExpression<Element> xpath = xpf.compile("./response_codes/code[@type=\"success\"]", Filters.element());
		List<Element> successCodes = xpath.evaluate(sourceNode);
		if (null != successCodes && !successCodes.isEmpty()) {
			for (Element code : successCodes) {
				successResponseCodes.add(Integer.parseInt(code.getText()));
			}
		} else {
			successResponseCodes.add(HttpStatus.SC_OK);
		}

		// Get the request headers
		Map<String, String> headersMap = new HashMap<>();
		Element headersElts = sourceNode.getChild("headers");
		if (null != headersElts && !headersElts.getChildren().isEmpty()) {
			for (Element headerElt : headersElts.getChildren()) {
				headersMap.put(headerElt.getAttributeValue("name"), headerElt.getText());
			}
		}

		setClient(new WSToStateBase(uri, method, headersMap, proxyHost, proxyPort, timeout, authHeader, successResponseCodes));
	}

	private String authentication(Element sourceNode) throws AlambicException {

		XPathFactory xpf = XPathFactory.instance();
		XPathExpression<Element> xpathCredentials = xpf.compile(".//authentication/credentials", Filters.element());
		List<Element> credentialsElts = xpathCredentials.evaluate(sourceNode); //basic
		XPathExpression<Element> xpathJwt = xpf.compile(".//authentication/jwt", Filters.element());
		List<Element> jwtElts = xpathJwt.evaluate(sourceNode); // jwt
		if (null != credentialsElts && !credentialsElts.isEmpty()) {
			String authLogin = context.resolveString(credentialsElts.get(0).getChildText("login"));
			String authPassword = context.resolveString(credentialsElts.get(0).getChildText("password"));
			if (StringUtils.isNotBlank(authLogin) && StringUtils.isNotBlank(authPassword)) {
				byte[] encodedAuth = Base64.getEncoder().encode(String.format("%s:%s", authLogin, authPassword).getBytes(
						StandardCharsets.ISO_8859_1));
				LOG.debug("authentification.credentials.login=" + authLogin);
				return "Basic ".concat(new String(encodedAuth));
			}
		} else if (null != jwtElts && !jwtElts.isEmpty()) {
			String kid = context.resolveString(jwtElts.get(0).getChildText("kid"));
			String secret = context.resolveString(jwtElts.get(0).getChildText("secret"));
			String customExpirationTime = context.resolveString(jwtElts.get(0).getChildText("expirationTime"));
			int expirationTime;
			try {
				expirationTime = Integer.parseInt(customExpirationTime);
			} catch (NumberFormatException e) {
				expirationTime = 60;
			}
			if (StringUtils.isNotBlank(kid) && StringUtils.isNotBlank(secret)) {
				LOG.debug("authentication.jwt.kid=" + kid);
				return JwtUtils.createTokenFrom(kid, secret, expirationTime);
			}
		} // else no authentication
		return null;
	}

}
