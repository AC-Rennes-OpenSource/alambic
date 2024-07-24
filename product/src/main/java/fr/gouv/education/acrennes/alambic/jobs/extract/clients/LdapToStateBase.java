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

import com.sun.jndi.ldap.ctl.SortControl;
import com.sun.jndi.ldap.ctl.VirtualListViewControl;
import com.sun.jndi.ldap.ctl.VirtualListViewResponseControl;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class LdapToStateBase implements IToStateBase {

    private static final Log log = LogFactory.getLog(LdapToStateBase.class);
    private final Hashtable<String, String> confLdap = new Hashtable<>();
    private final SearchControls contraintes = new SearchControls();
    private List<Map<String, List<String>>> results = new ArrayList<>();
    private NamingEnumeration<SearchResult> searchResultSet;
    private DirContext ctx = null;
    private LDAPResultsPageIterator pageIterator;

    public LdapToStateBase(final String driver, final String url, final String login, final String pwd, final String[] attributeList) {
        confLdap.put(Context.INITIAL_CONTEXT_FACTORY, driver);
        confLdap.put(Context.PROVIDER_URL, url);
        confLdap.put(Context.SECURITY_PRINCIPAL, login);
        confLdap.put(Context.SECURITY_CREDENTIALS, pwd);

        try {
            ctx = new InitialDirContext(confLdap);
        } catch (NamingException e) {
            log.error("Failed to instantiate the LDAP client, error: " + e.getMessage(), e);
        }
        contraintes.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        if (attributeList != null) {
            contraintes.setReturningAttributes(attributeList);
        }
    }

    @Override
    public List<Map<String, List<String>>> getStateBase() {
        return getStateBase(searchResultSet);
    }

    public List<Map<String, List<String>>> getStateBase(final NamingEnumeration<SearchResult> searchResult) {
        // FIXME close searchResult outside ?
        // NdKLH : pas fan de reconstruire une liste à chaque appel de cette méthode, même quand executeQuery n'est pas
        //         appelé entre les appels... mais pour une correction rapide, cela devrait suffire.
        results = new ArrayList<>();
        if (null != searchResult) {
            try {
                while (searchResult.hasMore()) {
                    Map<String, List<String>> entry = new HashMap<>();
                    SearchResult sR = searchResult.next();

                    Attributes attrs = sR.getAttributes();
                    NamingEnumeration<? extends Attribute> listAttrs = attrs.getAll();
                    try {
                        while (listAttrs.hasMore()) {
                            Attribute curAttr = listAttrs.next();
                            String attrName = curAttr.getID();
                            NamingEnumeration<?> values = curAttr.getAll();
                            List<String> attrValues = new ArrayList<>();
                            try {
                                while (values.hasMore()) {
                                    String value = Functions.getInstance().valueToString(values.next());
                                    attrValues.add(value);
                                }
                            } catch (UnsupportedEncodingException e) {
                                log.error("Conversion de caracteres" + e.getMessage(), e);
                            }
                            entry.put(attrName, attrValues);
                        }
                    } finally {
                        listAttrs.close();
                    }
                    results.add(entry);
                }
                searchResult.close();
            } catch (NamingException e) {
                log.error("Failed to perform LDAP search, error: " + e.getMessage(), e);
            }
        }

        return results;
    }

    @Override
    public int getCountResults() {
        return results.size();
    }

    @Override
    public void executeQuery(final String query) {
        executeQuery(query, null);
    }

    @Override
    public void executeQuery(final String query, final String scope) {
        try {
            contraintes.setSearchScope(getSearchScope(scope));
            searchResultSet = ctx.search("", query, contraintes);
        } catch (NamingException e) {
            log.error("Connexion a l'annuaire : " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (null != ctx) {
            try {
                ctx.close();
            } catch (NamingException e) {
                log.error("Failed to close the LDAP client, error:" + e.getMessage(), e);
            } finally {
                ctx = null;
            }
        }

        if (null != pageIterator) {
            try {
                pageIterator.close();
            } catch (NamingException e) {
                log.error("Failed to close the LDAP page iterator, error:" + e.getMessage(), e);
            } finally {
                pageIterator = null;
            }
        }
    }

    private int getSearchScope(final String scope) {
        int searchscope = SearchControls.ONELEVEL_SCOPE;

        if ("SUBTREE_SCOPE".equalsIgnoreCase(scope)) {
            searchscope = SearchControls.SUBTREE_SCOPE;
        } else if ("OBJECT_SCOPE".equalsIgnoreCase(scope)) {
            searchscope = SearchControls.OBJECT_SCOPE;
        }

        return searchscope;
    }

    @Override
    public void clear() {
        results.clear();
    }

    @Override
    public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize,
                                                                     final String sortBy, final String orderBy)
            throws AlambicException {
        pageIterator = new LDAPResultsPageIterator(confLdap, contraintes, query, scope, pageSize, sortBy);
        return pageIterator;
    }

    public class LDAPResultsPageIterator implements Iterator<List<Map<String, List<String>>>> {

        private final Log log = LogFactory.getLog(LDAPResultsPageIterator.class);
        private final int pageSize;
        private final String query;
        private final SearchControls controls;
        private final String sortBy;
        private List<Map<String, List<String>>> entries;
        private int offset;
        private int total;
        private LdapContext itrCtx;
        private boolean isInitialization;

        public LDAPResultsPageIterator(final Hashtable<String, String> environment, final SearchControls controls, final String query,
                                       final String scope, final int pageSize,
                                       final String sortBy) {
            offset = 1;
            total = 0;
            this.pageSize = pageSize;
            this.query = query;
            this.controls = controls;
            isInitialization = true;
            entries = Collections.emptyList();
            this.sortBy = sortBy;

            try {
                itrCtx = new InitialLdapContext(environment, null);

                /* Sort Control is required for VLV to work */
                SortControl sctl = new SortControl(new String[] { this.sortBy }, Control.CRITICAL);

                /* VLV that returns the first page of result */
                VirtualListViewControl vctl = new VirtualListViewControl(offset, 0, 0, (pageSize - 1), Control.CRITICAL);

                /* Set context's request controls */
                itrCtx.setRequestControls(new Control[] { sctl, vctl });

                /* Perform search */
                entries = getStateBase(itrCtx.search("", query, controls));

                updateControls(itrCtx.getResponseControls());
            } catch (Exception e) {
                log.error("Failed to instanciate the LDAP source page iterator. The LDAP server might not support simple page result requests, " +
                          "error: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasNext() {
            boolean doHaveNext = false;

            if (isInitialization) {
                isInitialization = false;
                doHaveNext = !entries.isEmpty();
            } else if (offset <= total) {
                try {
                    /* Sort Control is required for VLV to work */
                    SortControl sctl = new SortControl(new String[] { sortBy }, Control.CRITICAL);

                    /* VLV that returns the first page of result */
                    VirtualListViewControl vctl = new VirtualListViewControl(offset, 0, 0, (pageSize - 1), Control.CRITICAL);

                    /* Set context's request controls */
                    itrCtx.setRequestControls(new Control[] { sctl, vctl });

                    /* Perform search */
                    entries.clear();
                    entries = getStateBase(itrCtx.search("", query, controls));
                    updateControls(itrCtx.getResponseControls());
                    doHaveNext = !entries.isEmpty();
                } catch (IOException | NamingException e) {
                    log.error("Failed to evaluate has next page, error : " + e.getMessage(), e);
                }
            }

            return doHaveNext;
        }

        @Override
        public List<Map<String, List<String>>> next() {
            return entries;
        }

        @Override
        public void remove() {
            log.error("Not supported operation");
        }

        public void close() throws NamingException {
            itrCtx.close();
        }

        private void updateControls(final Control[] controls) {
            if (controls == null) {
                return;
            }

            for (final Control control : controls) {
                if (control instanceof final VirtualListViewResponseControl vlv) {
                    if (vlv.getResultCode() == 0) {
                        total = vlv.getListSize();
                        offset += pageSize;
                    } else {
                        log.error("Sorted-View did not complete successfully, error : " + vlv.getResultCode());
                    }
                }
            }
        }

    }

}