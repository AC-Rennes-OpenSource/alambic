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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface Source {

    String getName();

    int getPage();

    void setPage(final int page);

    List<Map<String, List<String>>> query() throws AlambicException;

    List<Map<String, List<String>>> query(String query) throws AlambicException;

    List<Map<String, List<String>>> query(String query, String scope) throws AlambicException;

    List<Map<String, List<String>>> getEntries();

    List<Map<String, List<String>>> getEntries(boolean distinct, String orderby, SourceFilter filter);

    Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException;

    int size();

    void close();

}