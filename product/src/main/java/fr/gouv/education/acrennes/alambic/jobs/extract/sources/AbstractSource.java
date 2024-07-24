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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.IToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import java.util.*;

public abstract class AbstractSource implements Source, Iterable<Map<String, List<String>>> {

    private static final Log log = LogFactory.getLog(AbstractSource.class);
    private static final String ATTRIBUTE_IS_DYNAMIC = "dynamic";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_VERSION = "version";
    private static final String DEFAULT_LEGACY_VERSION = "6.0";

    // Résultat de requête (TODO renommer sans rien casser)
    protected List<Map<String, List<String>>> entries = new ArrayList<>();
    protected IToStateBase client;
    protected String query;
    protected String scope;
    protected String name = "undefined";
    protected boolean isDynamic;
    protected int page;
    protected CallableContext context;
    protected String version;

    public AbstractSource() {
        // Tout est déjà correctement initialisé dans ce cas ?
    }

    public AbstractSource(final List<Map<String, List<String>>> entries) {
        Objects.requireNonNull(entries, "L'argument entries ne doit pas être null. Si vous n'avez pas de valeur à ajouter, passer une liste vide ou" +
                                        " appelez le constructeur sans argument.");
        this.entries = new ArrayList<>(entries);
    }

    public AbstractSource(final CallableContext context, final Element sourceNode) throws AlambicException {
        setDynamic(Boolean.parseBoolean(sourceNode.getAttributeValue(ATTRIBUTE_IS_DYNAMIC)));
        setName(sourceNode.getAttributeValue(ATTRIBUTE_NAME));
        setVersion(sourceNode.getAttributeValue(ATTRIBUTE_VERSION));
        this.context = context;
        initialize(sourceNode);
        if (!isDynamic) {
            query();
        }
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public void setDynamic(final boolean isDynamic) {
        this.isDynamic = isDynamic;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = (StringUtils.isNotBlank(version) ? version : DEFAULT_LEGACY_VERSION);
    }

    @Override
    public List<Map<String, List<String>>> query() throws AlambicException {
        return query(query);
    }

    @Override
    public List<Map<String, List<String>>> query(final String query) throws AlambicException {
        return query(query, null);
    }

    @Override
    public List<Map<String, List<String>>> query(final String query, final String scope) throws AlambicException {
        String resolvedQuery = null;

        if (null == getClient()) {
            throw new AlambicException("Client isn't instanciated. The source might be static.");
        }

        // Resolve possible variables within the query
        if (StringUtils.isNotBlank(query)) {
            resolvedQuery = Functions.getInstance().executeAllFunctions(context.resolveString(query));
        }

        log.debug("Executing query on the resource '" + getName() + "', query '" + resolvedQuery + "'");
        getClient().executeQuery(resolvedQuery, scope);
        entries = getClient().getStateBase();

        return entries;
    }

    public IToStateBase getClient() {
        return client;
    }

    public void setClient(final IToStateBase client) {
        this.client = client;
    }

    @Override
    public List<Map<String, List<String>>> getEntries() {
        return getEntries(false, null, null);
    }

    @Override
    public List<Map<String, List<String>>> getEntries(final boolean distinct, final String orderby, final SourceFilter filter) {
        ArrayList<Map<String, List<String>>> filteredResource = new ArrayList<>();
        for (Map<String, List<String>> item : entries) {
            if (null == filter || filter.accept(item)) {
                ResourceEntryMap rem = new ResourceEntryMap(item, filter);
                if (!distinct || !filteredResource.contains(rem)) {
                    filteredResource.add(rem);
                }
            }
        }

        if (StringUtils.isNotBlank(orderby)) {
            filteredResource.sort(new EntrySorter(orderby));
        }

        return filteredResource;
    }

    @Override
    public Iterator<Map<String, List<String>>> iterator() {
        return entries.iterator();
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public void close() {
        if (null != getClient()) {
            getClient().close();
            setClient(null);
        }
    }

    @Override
    public Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException {
        throw new AlambicException("This source '" + name + "' doesn't support the paged results feature");
    }

    abstract public void initialize(Element sourceNode) throws AlambicException;

    @Override
    public int getPage() {
        return page;
    }

    @Override
    public void setPage(final int page) {
        this.page = page;
    }

    private static class EntrySorter implements Comparator<Map<String, List<String>>> {
        final String orderby;

        public EntrySorter(final String orderby) {
            this.orderby = orderby;
        }

        @Override
        public int compare(final Map<String, List<String>> o1,
                           final Map<String, List<String>> o2) {
            List<String> ov1 = o1.get(orderby);
            List<String> ov2 = o2.get(orderby);
            return Integer.valueOf(ov1.get(0)).compareTo(Integer.valueOf(ov2.get(0)));
        }

    }

    private static class ResourceEntryMap extends HashMap<String, List<String>> {
        private static final long serialVersionUID = 1L;
        private SourceFilter filter;

        public ResourceEntryMap(final Map<String, List<String>> item, final SourceFilter filter) {
            super(item);
            setFilter(filter);
        }

        public SourceFilter getFilter() {
            return filter;
        }

        public void setFilter(final SourceFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean equals(final Object o) {
            boolean status = false;

            ResourceEntryMap remo = (ResourceEntryMap) o;
            if (remo.size() == size()) {
                if (null != remo.getFilter()) {
                    Set<java.util.Map.Entry<String, List<String>>> subEntrySet = new HashSet<>();
                    for (String key : getFilter().getFilters().keySet()) {
                        subEntrySet.add(new SimpleEntry<>(key, get(key)));
                    }
                    status = remo.entrySet().containsAll(subEntrySet);
                } else {
                    status = super.equals(remo);
                }
            }

            return status;
        }

    }

}