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
import org.jdom2.Element;

import java.util.*;

public class NoneSource extends AbstractSource {

    // private static final Log log = LogFactory.getLog(NoneSource.class);

    public NoneSource(final CallableContext context, final Element sourceNode) throws AlambicException {
        super(context, sourceNode);
    }

    @Override
    public void initialize(final Element sourceNode) {
        setClient(new NoneClient());
    }

    private static class NoneClient implements IToStateBase {

        private final List<Map<String, List<String>>> resultat;

        public NoneClient() {
            resultat = new ArrayList<>();
        }

        @Override
        public void executeQuery(final String query) {
            executeQuery(query, null);
        }

        @Override
        public void executeQuery(final String query, final String scope) {
            Map<String, List<String>> h = new HashMap<>();
            List<String> l = new ArrayList<>();
            l.add("hop");
            h.put("none", l);
            resultat.add(h);
        }

        @Override
        public List<Map<String, List<String>>> getStateBase() {
            return resultat;
        }

        @Override
        public int getCountResults() {
            return resultat.size();
        }

        @Override
        public void close() {
            // no-operation
        }

        @Override
        public void clear() {
            resultat.clear();
        }

        @Override
        public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize,
                                                                         final String sortBy, final String orderBy)
                throws AlambicException {
            throw new AlambicException("Not implemented operation");
        }

    }

}