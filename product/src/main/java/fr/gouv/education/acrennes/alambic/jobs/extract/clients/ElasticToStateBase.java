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

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ElasticToStateBase implements IToStateBase {

    private static final Log log = LogFactory.getLog(ElasticToStateBase.class);
    private static final String ELASTIC_SEARCH_API = "%s/%s/_search";
    private static final String ELASTIC_COUNT_API = "%s/%s/_count";
    private static final String ELASTIC_SCROLL_SEARCH_INITIAL_API = "%s/%s/_search?scroll=%s";
    private static final String ELASTIC_SCROLL_SEARCH_SUBSEQUENT_API = "%s/_search/scroll";
    private static final int DEFAULT_TIME_OUT = 300_000; // 5 minutes (scroll search request might be slow due to large volume of data)
    private static final Map<String, String> headers;

    static {
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
    }

    private final String uri;
    private final String scroll;
    private final String index_alias;
    private List<Map<String, List<String>>> statebase = new ArrayList<>();
    private CloseableHttpClient httpClient;
    private String authHeader;
    private ElsaticResultsPageIterator pageIterator;
    public ElasticToStateBase(final String uri, final String proxy_host, final String proxy_port, final String index_alias, final String scroll,
                              final String connection_timeout, final String auth_login, final String auth_password) {
        this.uri = uri;
        this.scroll = scroll;
        this.index_alias = index_alias;

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
            log.debug("Authentification non activée.");
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
    public void executeQuery(final String query, final String scope) {
        executeQuery(query);
    }

    @Override
    public void executeQuery(final String query) {
        if (StringUtils.isNotBlank(query)) {
            String api = String.format(ELASTIC_SEARCH_API, this.uri, this.index_alias);
            executeInnerQuery(api, query);
        } else {
            log.error("The query must not be empty");
        }
    }

    private void executeInnerQuery(final String uri, final String query) {
        statebase = new ArrayList<>();
        try {
            WebServiceApi wsapi = new WebServiceApi(uri, HttpMethod.POST, ElasticToStateBase.headers, query, List.of(200));
            HttpUriRequest request = wsapi.getRequest();
            if (StringUtils.isNotBlank(this.authHeader)) {
                request.setHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
            }

            log.debug(String.format(">>>>> Execute the query '%s' with payload '%s'", wsapi, query));

            try (CloseableHttpResponse response = this.httpClient.execute(request)) {
                if (wsapi.isSuccessful(response)) {
                    String body = IOUtils.toString(response.getEntity().getContent(), Charsets.UTF_8);
                    if (StringUtils.isNotBlank(body)) {
                        Map<String, List<String>> item = new HashMap<>();
                        item.put("item", Collections.singletonList(body));
                        statebase.add(item);
                    }
                } else {
                    log.error(String.format("Failed to execute the query '%s' (expected response codes : '%s'), received response : code=%d, " +
                                            "phrase=%s",
                            wsapi,
                            wsapi.getSuccessResponseCodes(),
                            response.getStatusLine().getStatusCode(),
                            response.getStatusLine().getReasonPhrase()));
                }
                EntityUtils.consume(response.getEntity()); // consume the response content to avoid connection leaks
            }
        } catch (IOException | AlambicException e) {
            log.error("Failed to execute the query '" + query + "', error : " + e.getMessage());
        }
    }

    @Override
    public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize,
                                                                     final String sortBy, final String orderBy)
            throws AlambicException {
        this.pageIterator = new ElsaticResultsPageIterator(query, pageSize);
        return this.pageIterator;
    }

    public enum AUTH_SCHEMES {
        BASIC_AUTH
    }

    public class ElsaticResultsPageIterator implements Iterator<List<Map<String, List<String>>>> {

        private final Log log = LogFactory.getLog(ElsaticResultsPageIterator.class);
        private final String query;
        private final int pageSize;
        private List<Map<String, List<String>>> entries;
        private String scroll_id;
        private int total;
        private int count;

        public ElsaticResultsPageIterator(final String query, final int pageSize) throws AlambicException {
            this.query = query;
            this.entries = Collections.emptyList();
            this.pageSize = pageSize;
            this.total = 0;
            this.count = 0;

            try {
                JSONObject query_json_obj = new JSONObject(query);
                query_json_obj.remove("_source");
                String updated_query = query_json_obj.toString();

                String api = String.format(ELASTIC_COUNT_API, uri, index_alias);
                executeInnerQuery(api, updated_query);
                this.entries = getStateBase();
                if (!this.entries.isEmpty()) {
                    String rset = this.entries.get(0).get("item").get(0);
                    if (StringUtils.isNotBlank(rset)) {
                        this.total = new JSONObject(rset).getInt("count");
                    } else {
                        throw new AlambicException("Failed to instanciate the Elastic source page iterator. Blank response while executing the " +
                                                   "initial query : '" + query + "'\"");
                    }
                } else {
                    throw new AlambicException("Failed to instanciate the Elastic source page iterator. Empty result while executing the initial " +
                                               "query : '" + query + "'");
                }
            } catch (Exception e) {
                throw new AlambicException("Failed to instanciate the Elastic source page iterator. error : " + e.getMessage());
            }
        }

        @Override
        public boolean hasNext() {
            return (this.count < this.total);
        }

        @Override
        public List<Map<String, List<String>>> next() {
            String api;
            String updated_query;

            this.entries.clear();

            if (this.count == 0) {
                // Initial scroll API call
                JSONObject query_json_obj = new JSONObject(this.query);
                query_json_obj.put("size", this.pageSize);
                updated_query = query_json_obj.toString();
                api = String.format(ELASTIC_SCROLL_SEARCH_INITIAL_API, uri, index_alias, scroll);
            } else {
                // Subsequent scroll API call
                api = String.format(ELASTIC_SCROLL_SEARCH_SUBSEQUENT_API, uri);
                updated_query = String.format("{\"scroll\": \"%s\", \"scroll_id\": \"%s\"}", scroll, this.scroll_id);
            }

            try {
                executeInnerQuery(api, updated_query);
                this.entries = getStateBase();
                if (!this.entries.isEmpty()) {
                    String rset = this.entries.get(0).get("item").get(0);
                    if (StringUtils.isNotBlank(rset)) {
                        JSONObject jsonRst = new JSONObject(rset);
                        int hits_count = jsonRst.getJSONObject("hits").getJSONArray("hits").length();
                        if (0 == hits_count) {
                            if (this.count < this.total) {
                                log.error("Abnormal scroll ending (count is only : " + this.count + "despite total is : " + this.total);
                            }
                            this.count = this.total;
                        } else {
                            this.count += hits_count;
                        }
                        this.scroll_id = jsonRst.getString("_scroll_id");
                    } else {
                        throw new AlambicException("Failed to execute the Elastic source page iterator. Blank response while executing the initial " +
                                                   "query : '" + query + "'\"");
                    }
                } else {
                    throw new AlambicException("Failed to execute the Elastic source page iterator. Empty result while executing the initial query " +
                                               ": '" + query + "'");
                }
            } catch (Exception e) {
                log.error("Failed to execute the Elastic source page iterator. error : " + e.getMessage());
            }

            return this.entries;
        }

        public void close() {
            this.entries.clear();
        }

    }

}
