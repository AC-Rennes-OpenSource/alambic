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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BaseXToStateBase implements IToStateBase {

    private static final Log log = LogFactory.getLog(BaseXToStateBase.class);
    private static final String BASEX_SEARCH_API = "%s:%d/rest/%s";
    private static final String BASEX_SEARCH_API_WITH_QUERY_PARAMS = "%s:%d/rest/%s?query=%s";
    private static final int DEFAULT_TIME_OUT = 5_000; // 5 seconds

    public enum AUTH_SCHEMES {
        BASIC_AUTH
    }

    private List<Map<String, List<String>>> statebase = new ArrayList<>();
    private CloseableHttpClient httpClient;
    private String authHeader;
    private final String host;
    private final int port;
    private final String database;
    private BaseXResultsPageIterator pageIterator;

    public BaseXToStateBase(final String host, final int port, final String database, final String proxy_host, final String proxy_port,
                            final String connection_timeout, final String auth_login, final String auth_password) {
        this.database = (StringUtils.isNotBlank(database)) ? database : "";
        this.host = host;
        this.port = port;

        // Configure & instantiate the http connector
        Builder requestConfig = RequestConfig.custom();
        int timeout = (StringUtils.isNotBlank(connection_timeout)) ? Integer.parseInt(connection_timeout) : DEFAULT_TIME_OUT;
        requestConfig = requestConfig.setConnectionRequestTimeout(timeout).setConnectTimeout(timeout).setSocketTimeout(timeout);

        if (StringUtils.isNotBlank(proxy_host) && StringUtils.isNotBlank(proxy_port)) {
            requestConfig = requestConfig.setProxy(new HttpHost(proxy_host, Integer.parseInt(proxy_port)));
            log.debug(String.format("Configuration de proxy activée : host=%s, port=%s", proxy_host, proxy_port));
        } else {
            log.debug("Pas de configuration de proxy");
        }

        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig.build())
                .build();

        // Authentication (basic supported only)
        if (StringUtils.isNotBlank(auth_login) && StringUtils.isNotBlank(auth_password)) {
            byte[] encodedAuth = Base64.getEncoder().encode(String.format("%s:%s", auth_login, auth_password).getBytes(StandardCharsets.ISO_8859_1));
            this.authHeader = "Basic ".concat(new String(encodedAuth));
            log.debug("Authentification activée.");
        } else {
            log.warn("Authentification non activée.");
        }
    }

    @Override
    public List<Map<String, List<String>>> getStateBase() {
        return statebase;
    }

    @Override
    public int getCountResults() {
        return statebase.size();
    }

    @Override
    public void clear() {
        statebase.clear();
    }

    @Override
    public void close() {
        try {
            if (null != this.httpClient) {
                this.httpClient.close();
            }

            if (null != this.pageIterator) {
                this.pageIterator.close();
            }
        } catch (IOException e) {
            log.error("Echec pour fermer le connecteur, erreur : " + e.getMessage());
        } finally {
            this.httpClient = null;
            this.pageIterator = null;
        }
    }

    @Override
    public void executeQuery(String query) {
        statebase = new ArrayList<>();
        try {
            HttpGet request = new HttpGet(getFullAPIURL(query));
            if (StringUtils.isNotBlank(this.authHeader)) {
                request.setHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
            }

            try (CloseableHttpResponse response = this.httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String body = IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8);
                    if (StringUtils.isNotBlank(body)) {
                        Map<String, List<String>> item = new HashMap<>();
                        item.put("item", List.of(body));
                        statebase.add(item);
                    }
                } else {
                    log.error("erreur lors de l'appel de l'API BaseX, code d'erreur = " + response.getStatusLine().getStatusCode());
                }
                EntityUtils.consume(response.getEntity()); // consume the response content to avoid connection leaks
            }
        } catch (IOException e) {
            log.error("Failed to execute the query '" + query + "', error : " + e.getMessage());
        }
    }

    @Override
    public void executeQuery(String query, String scope) {
        executeQuery(query);
    }

    private String getFullAPIURL(final String query) throws UnsupportedEncodingException {
        String url = "";
        if (StringUtils.isNotBlank(query)) {
            url = String.format(BASEX_SEARCH_API_WITH_QUERY_PARAMS, this.host, this.port, this.database, URLEncoder.encode(query,
                    StandardCharsets.UTF_8));
        } else {
            url = String.format(BASEX_SEARCH_API, this.host, this.port, this.database);
        }
        return url;
    }

    @Override
    public Iterator<List<Map<String, List<String>>>> getPageIterator(String query, String scope, int pageSize, String sortBy, String orderBy)
            throws AlambicException {
        this.pageIterator = new BaseXResultsPageIterator(query, pageSize);
        return this.pageIterator;
    }

    public class BaseXResultsPageIterator implements Iterator<List<Map<String, List<String>>>> {

        private final Log log = LogFactory.getLog(BaseXResultsPageIterator.class);

        private List<Map<String, List<String>>> entries;
        private final String query;
        private final int pageSize;
        private int offset;
        private final int total;

        public BaseXResultsPageIterator(final String query, final int pageSize) throws AlambicException {
            this.query = query;
            this.pageSize = pageSize;
            this.offset = 0;
            this.entries = Collections.emptyList();

            /* Perform query in order to get count value */
            String countQuery = String.format("count(%s)", query);
            try {
                executeQuery(countQuery);
                this.entries = getStateBase();
                if (!this.entries.isEmpty()) {
                    String countStg = this.entries.get(0).get("item").get(0);
                    if (StringUtils.isNotBlank(countStg)) {
                        this.total = Integer.parseInt(countStg);
                    } else {
                        throw new AlambicException("Failed to instanciate the BaseX source page iterator. Blank count while getting the resultset " +
                                                   "total size (query is '" + countQuery + "')");
                    }
                } else {
                    throw new AlambicException("Failed to instanciate the BaseX source page iterator. Empty result while getting the resultset " +
                                               "total size (query is '" + countQuery + "')");
                }
            } catch (Exception e) {
                throw new AlambicException("Failed to instanciate the BaseX source page iterator. error : " + e.getMessage());
            }
        }

        @Override
        public boolean hasNext() {
            return (this.offset < this.total);
        }

        @Override
        public List<Map<String, List<String>>> next() {
            this.entries.clear();

            /* Perform paged query according to the current offset value */
            String pagedQuery = String.format("%s[position() > %d and position() < %d]", query, this.offset, (this.offset + this.pageSize + 1));
            try {
                executeQuery(pagedQuery);
                this.offset += this.pageSize;
                this.entries = getStateBase();
            } catch (Exception e) {
                log.error("BaseX source page iterator - failed to execute the query '" + pagedQuery + "'. error : " + e.getMessage());
            }

            return this.entries;
        }

        public void close() {
            // no-op
        }

    }

}
