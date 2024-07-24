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

import fr.gouv.education.acrennes.alambic.api.WebServiceApi;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WSToStateBase implements IToStateBase {

    private static final Log LOG = LogFactory.getLog(WSToStateBase.class);
    private static final int DEFAULT_TIME_OUT = 5_000; // 5 seconds
    private final List<Integer> successResponseCodes;
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private List<Map<String, List<String>>> stateBase = new ArrayList<>();
    private CloseableHttpClient httpClient;
    private String authHeader;
    public WSToStateBase(final String url, final String method, final Map<String, String> headers, final String proxy_host, final String proxy_port
            , final String connection_timeout, final String auth_scheme, final String auth_login, final String auth_password,
                         List<Integer> successResponseCodes) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.successResponseCodes = successResponseCodes;

        // Get authentication (basic supported only)
        if (StringUtils.isNotBlank(auth_login) && StringUtils.isNotBlank(auth_password)) {
            byte[] encodedAuth = Base64.getEncoder().encode(String.format("%s:%s", auth_login, auth_password).getBytes(StandardCharsets.ISO_8859_1));
            this.authHeader = "Basic ".concat(new String(encodedAuth));
            LOG.debug("Authentification activée sur le connecteur web service");
        } // else, no authentication

        // Configure & instantiate the http connector
        Builder requestConfig = RequestConfig.custom();

        int timeout = (StringUtils.isNotBlank(connection_timeout)) ? Integer.parseInt(connection_timeout) : DEFAULT_TIME_OUT;
        requestConfig = requestConfig.setConnectionRequestTimeout(timeout).setConnectTimeout(timeout).setSocketTimeout(timeout);
        LOG.debug("Timeout configuré : " + timeout + " ms");

        if (StringUtils.isNotBlank(proxy_host) && StringUtils.isNotBlank(proxy_port)) {
            requestConfig = requestConfig.setProxy(new HttpHost(proxy_host, Integer.parseInt(proxy_port)));
            LOG.debug(String.format("Configuration de proxy activée : host=%s, port=%s", proxy_host, proxy_port));
        } else {
            LOG.debug("Pas de configuration de proxy");
        }

        this.httpClient = HttpClientBuilder.create()
                .useSystemProperties()
                .setDefaultRequestConfig(requestConfig.build())
                .build();
    }

    static String getFullAPIURL(final String url, final String api, final String queryParams)
            throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder(url);
        boolean hasQuestionMark = url.endsWith("?");
        if (StringUtils.isNotBlank(api)) {
            builder.append(api);
            hasQuestionMark = api.endsWith("?");
        }
        if (StringUtils.isNotBlank(queryParams)) {
            if (!hasQuestionMark) {
                builder.append("?");
            }
            builder.append(encodeQueryParams(queryParams));
        }
        final String fullURL = builder.toString();
        LOG.debug("URL complète à requêter : " + fullURL);
        return fullURL;
    }

    /**
     * Échappe les arguments de requête pour qu'ils puissent être utilisés dans l'URL. Par exemple, pour la valeur
     * fournie {@code test=val&autre&ex1=Alè-=R} est retournée {@code test=val&autre&ex1=Al%C3%A8-%3DR}.
     * <p>
     * À noter que du fait de l'usage d'une chaîne de caractère comme paramètre en entrée au lieu d'un type structuré,
     * il n'est pas possible de détecter et encoder les caractères {@literal &} qui feraient partie d'une valeur à
     * transmettre.
     * <p>
     * De même, nous avons choisi que la séparation clef-valeur se ferait au premier caractère {@literal =} rencontré,
     * alors qu'il pourrait en fait faire partie de la clef.
     * <p>
     * Cela ne devrait heureusement pas poser de souci dans la grande majorité des cas.
     *
     * @param queryParams les query params à échapper
     *
     * @return la chaîne échappée
     *
     * @see <a href="https://tools.ietf.org/html/rfc3986#page-23">RFC 3986, section 3.4. Query</a>
     */
    private static String encodeQueryParams(final String queryParams) throws UnsupportedEncodingException {
        final String utf8Charset = Charsets.UTF_8.toString();
        final StringJoiner joiner = new StringJoiner("&");

        for (final String queryParam : queryParams.split("&")) {
            final int firstEqualIndex = queryParam.indexOf("=");
            if (firstEqualIndex == -1) {
                joiner.add(URLEncoder.encode(queryParam, utf8Charset));
            } else {
                joiner.add(String.format("%s=%s",
                        URLEncoder.encode(queryParam.substring(0, firstEqualIndex), utf8Charset),
                        URLEncoder.encode(queryParam.substring(firstEqualIndex + 1), utf8Charset)
                ));
            }
        }

        return joiner.toString();
    }

    @Override
    public List<Map<String, List<String>>> getStateBase() {
        return stateBase;
    }

    @Override
    public int getCountResults() {
        return stateBase.size();
    }

    @Override
    public void clear() {
        stateBase.clear();
    }

    @Override
    public void close() {
        try {
            if (null != this.httpClient) {
                this.httpClient.close();
            }
        } catch (IOException e) {
            LOG.error("Echec pour fermer le connecteur, erreur : " + e.getMessage(), e);
        } finally {
            this.httpClient = null;
        }
    }

    @Override
    public void executeQuery(String jsonquery) {
        stateBase = new ArrayList<>();
        try {
            String query_api = null;
            String query_parameters = null;
            String query_payload = null;

            if (StringUtils.isNotBlank(jsonquery)) {
                JSONObject query = new JSONObject(jsonquery);
                query_api = (query.has("api")) ? query.getString("api") : null;
                query_parameters = (query.has("parameters")) ? query.getString("parameters") : null;
                query_payload = (query.has("payload")) ? query.get("payload").toString() : null;
            }

            WebServiceApi wsapi = new WebServiceApi(getFullAPIURL(this.url, query_api, query_parameters), this.method, this.headers, query_payload,
                    this.successResponseCodes);
            HttpUriRequest request = wsapi.getRequest();
            if (StringUtils.isNotBlank(this.authHeader)) {
                request.setHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
            }

            try (CloseableHttpResponse response = this.httpClient.execute(request)) {
                if (wsapi.isSuccessful(response)) {
                    if (response.getEntity() != null && response.getEntity().getContent() != null) {
                        String body = IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8);
                        if (StringUtils.isNotBlank(body)) {
                            Map<String, List<String>> item = new HashMap<>();
                            item.put("item", Collections.singletonList(body));
                            stateBase.add(item);
                        }
                    }
                } else {
                    LOG.error(String.format("Réponse en erreur sur la requête '%s' (codes attendus : '%s'), réponse reçue : code=%d, phrase=%s",
                            wsapi,
                            wsapi.getSuccessResponseCodes(),
                            response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase()));
                }
                EntityUtils.consume(response.getEntity()); // consume the response content to avoid connection leaks
            }
        } catch (IOException | AlambicException e) {
            LOG.error("Echec de traitement de la requête '" + jsonquery + "', cause : " + e.getMessage(), e);
        }
    }

    @Override
    public void executeQuery(String query, String scope) {
        executeQuery(query);
    }

    @Override
    public Iterator<List<Map<String, List<String>>>> getPageIterator(String query, String scope, int pageSize,
                                                                     String sortBy, String orderBy) throws AlambicException {
        throw new AlambicException("Not implemented operation");
    }

    public enum AUTH_SCHEMES {
        BASIC_AUTH
    }

}
