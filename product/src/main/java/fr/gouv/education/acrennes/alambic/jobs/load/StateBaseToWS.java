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
package fr.gouv.education.acrennes.alambic.jobs.load;

import fr.gouv.education.acrennes.alambic.api.WebServiceApi;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.utils.JwtUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class StateBaseToWS extends AbstractDestination {

	private static final Log log = LogFactory.getLog(StateBaseToWS.class);

    private Element pivot = null;
	private CloseableHttpClient httpClient;
	private String authHeader;

	public StateBaseToWS(final CallableContext context, final Element destinationNode, final ActivityMBean jobActivity)
			throws AlambicException {
		super(context, destinationNode, jobActivity);

		// Get & parse input file
		String pivotElt = destinationNode.getChildText("pivot");
		if (StringUtils.isNotBlank(pivotElt)) {
			try {
				pivotElt = context.resolvePath(pivotElt);
				this.pivot = (new SAXBuilder()).build(new InputSource(pivotElt)).getRootElement();
			} catch (JDOMException | IOException e) {
				throw new AlambicException("Erreur de parsing du fichier pivot, erreur : " + e.getMessage());
			}
		} else {
			throw new AlambicException("le pivot n'est pas precisé");
		}
		
		// Get authentication (JWT and basic supported only)
		configureAuthentication(context, destinationNode);

		// Get the connection timeout
		String timeoutAttrValue = context.resolveString(destinationNode.getAttributeValue("connectionTimeout"));
        // 5 seconds
        int defaultTimeOut = 5_000;
        int timeout = (StringUtils.isNotBlank(timeoutAttrValue)) ? Integer.parseInt(timeoutAttrValue) :
                defaultTimeOut;

		// Get proxy configuration
		String proxyHostAttrValue = context.resolveString(destinationNode.getAttributeValue("proxyHost"));
		String proxyPortAttrValue = context.resolveString(destinationNode.getAttributeValue("proxyPort"));
		
		// Configure & instantiate the http connector
		Builder requestConfig = RequestConfig.custom();
		requestConfig = requestConfig.setConnectionRequestTimeout(timeout).setConnectTimeout(timeout).setSocketTimeout(timeout);
		if (StringUtils.isNotBlank(proxyHostAttrValue) && StringUtils.isNotBlank(proxyPortAttrValue)) {
			requestConfig = requestConfig.setProxy(new HttpHost(proxyHostAttrValue, Integer.parseInt(proxyPortAttrValue)));
			log.debug(String.format("Configuration de proxy activée : host=%s, port=%s", proxyHostAttrValue, proxyPortAttrValue));
		} else {
			log.debug("Pas de configuration de proxy");
		}

		this.httpClient = HttpClientBuilder.create()
				.useSystemProperties()
				.setDefaultRequestConfig(requestConfig.build())
				.build();
	}

	private void configureAuthentication(CallableContext context, Element destinationNode) throws AlambicException {
		XPathFactory xpf = XPathFactory.instance();
		XPathExpression<Element> xpathCredentials = xpf.compile(".//authentication/credentials", Filters.element());
		List<Element> credentialsElts = xpathCredentials.evaluate(destinationNode); //basic
		XPathExpression<Element> xpathJwt = xpf.compile(".//authentication/jwt", Filters.element());
		List<Element> jwtElts = xpathJwt.evaluate(destinationNode); // jwt
		if (null != credentialsElts && !credentialsElts.isEmpty()) {
			String authLogin = context.resolveString(credentialsElts.get(0).getChildText("login"));
			String authPassword = context.resolveString(credentialsElts.get(0).getChildText("password"));
			if (StringUtils.isNotBlank(authLogin) && StringUtils.isNotBlank(authPassword)) {
				byte[] encodedAuth = Base64.getEncoder().encode(String.format("%s:%s", authLogin, authPassword).getBytes(StandardCharsets.ISO_8859_1));
				this.authHeader = "Basic ".concat(new String(encodedAuth));
				log.debug("Authentification Basic activée sur le connecteur web service");
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
				this.authHeader = JwtUtils.createTokenFrom(kid, secret, expirationTime);
				log.debug("Authentification JWT activée sur le connecteur web service");
			}
		} // else, no authentication
	}

	@Override
	public void execute() {
		int pivotEntriesCount = pivot.getChildren().size();
		int currentPivotEntriesIndex = 1;

		// Iterate over the list of API requests
		for (final Element xmlNode : pivot.getChildren()) {
			// activity monitoring
			this.jobActivity.setProgress((currentPivotEntriesIndex * 100) / pivotEntriesCount);
			this.jobActivity.setProcessing("processing entry " + currentPivotEntriesIndex + "/" + pivotEntriesCount);

			WebServiceApi wsapi = null;
			try {
				wsapi = new WebServiceApi(this.context, xmlNode);
				HttpUriRequest request = wsapi.getRequest();
				if (StringUtils.isNotBlank(this.authHeader)) {
					request.setHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
				}
				HttpResponse response = this.httpClient.execute(request);
				if (!wsapi.isSuccessful(response)) {
					this.jobActivity.setTrafficLight(ActivityTrafficLight.RED);
					log.error(String.format("Réponse en erreur sur la requête '" + wsapi + "' (codes attendus : '%s'), réponse reçue : code=%s, phrase=%s",
									wsapi.getSuccessResponseCodes(),
									response.getStatusLine().getStatusCode(), 
									response.getStatusLine().getReasonPhrase()));
				}
				EntityUtils.consume(response.getEntity()); // consume the response content to avoid connection leaks
			} catch (final Exception e) {
				this.jobActivity.setTrafficLight(ActivityTrafficLight.RED);
				log.error("Echec de traitement de la requête '" + wsapi + "', cause : " + e.getMessage());
			}
			currentPivotEntriesIndex++;
		}
	}

	@Override
	public void close() throws AlambicException {
		try {
			super.close();
			if (null != this.httpClient) {
				this.httpClient.close();
			}
		} catch (IOException e) {
			throw new AlambicException("Echec pour fermer le connecteur, erreur : " + e.getMessage());
		} finally {
			this.httpClient = null;
		}
	}

}
