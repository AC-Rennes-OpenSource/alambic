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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;

public interface IToStateBase {

	public void executeQuery(String query) throws AlambicException;

	public void executeQuery(String query, String scope) throws AlambicException;

	public List<Map<String, List<String>>> getStateBase();

	public int getCountResults();

	public void clear();

	public void close();

	public Iterator<List<Map<String, List<String>>>> getPageIterator(String query, String scope, int pageSize, String sortBy, String orderBy) throws AlambicException;

}