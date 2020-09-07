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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FSToStateBase implements IToStateBase {

	private static final Log log = LogFactory.getLog(FSToStateBase.class);

	private List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private FSResultsPageIterator pageIterator;

	@Override
	public void executeQuery(final String query) {
		executeQuery(query, null);
	}

	@Override
	public void executeQuery(final String jsonquery, final String scope) {
		stateBase = new ArrayList<>();
		if (StringUtils.isNotBlank(jsonquery)) {
			try {
				Map<String, Object> query = new ObjectMapper().readValue(jsonquery, new TypeReference<Map<String, Object>>() {});
				String rootpath = (String) query.get("rootPath");
				String filterRegex = (String) query.get("filterRegex");
				FilenameFilter fileFilter = null;
				if (StringUtils.isNotBlank(filterRegex)) {
					fileFilter = new FilenameRegExFilter(filterRegex);
				}

				File root = new File(rootpath);
				if (root.isDirectory()) {
					File[] list = root.listFiles(fileFilter);
					for (File file : list) {
						Map<String, List<String>> map = new HashMap<String, List<String>>();
						map.put("name", Arrays.asList(file.getName()));
						map.put("path", Arrays.asList(file.getPath()));
						map.put("size", Arrays.asList(Long.toString(file.length())));
						map.put("isFile", Arrays.asList(Boolean.toString(file.isFile())));
						map.put("isDirectory", Arrays.asList(Boolean.toString(file.isDirectory())));
						map.put("isHidden", Arrays.asList(Boolean.toString(file.isHidden())));
						stateBase.add(map);
					}
				} else {
					log.error("Not a directory, cannot explore file system.");
				}
			} catch (IOException e) {
				log.error("Failed to execute the query '" + jsonquery + "', error: " + e.getMessage());
			}
		} else {
			log.error("Empty query, cannot explore file system.");
		}
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
	public void close() {
		if (null != pageIterator) {
			try {
				pageIterator.close();
			} finally {
				pageIterator = null;
			}
		}
	}

	@Override
	public void clear() {
		stateBase.clear();
	}

	public final class FilenameRegExFilter implements FilenameFilter
	{
		private final String regex;

		public FilenameRegExFilter(final String regex)
		{
			this.regex = regex;
		}

		@Override
		public boolean accept(final File dir, final String filename)
		{
			return filename.matches(regex);
		}

		@Override
		public String toString()
		{
			return "FilenameRegExFilter: regex pattern is '" + regex + "'";
		}
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy) throws AlambicException {
		pageIterator = new FSResultsPageIterator(query, pageSize);
		return pageIterator;
	}

	public class FSResultsPageIterator implements Iterator<List<Map<String, List<String>>>> {

		private final Log log = LogFactory.getLog(FSResultsPageIterator.class);

		private List<Map<String, List<String>>> entries;
		private int offset;
		private int total;
		private final int pageSize;

		public FSResultsPageIterator(final String query, final int pageSize) {
			this.offset = 0;
			this.total = 0;
			this.pageSize = pageSize;
			this.entries = Collections.emptyList();

			try {
				/* Perform search */
				executeQuery(query, null);
				entries = getStateBase();
				total = entries.size();
			} catch (Exception e) {
				log.error("Failed to instanciate the FileExplorer source page iterator. error : " + e.getMessage());
			}
		}

		@Override
		public boolean hasNext() {
			return (offset < total);
		}

		@Override
		public List<Map<String, List<String>>> next() {
			List<Map<String, List<String>>> subEntriesList = entries.subList(offset, (((offset + pageSize) < total) ? (offset + pageSize) : total));
			offset += pageSize;
			return subEntriesList;
		}

		@Override
		public void remove() {
			log.error("Not supported operation");
		}

		public void close() {
			entries.clear();
		}

	}

}