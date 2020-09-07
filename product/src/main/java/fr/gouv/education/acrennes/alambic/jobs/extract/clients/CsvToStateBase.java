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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;

public class CsvToStateBase implements IToStateBase {

	private static final Log log = LogFactory.getLog(CsvToStateBase.class);

	public final static char DEFAULT_SEPARATOR = ';';

	private List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private CsvToStateBaseIterator pageIterator;
	private CSVReader reader;

	public CsvToStateBase(final String fichierCsv) {
		this(fichierCsv, DEFAULT_SEPARATOR);
	}

	public CsvToStateBase(final String fichierCsv, final char defaultSeparator) {
		this(fichierCsv, defaultSeparator, CSVParser.DEFAULT_QUOTE_CHARACTER);
	}

	public CsvToStateBase(final String fichierCsv, final char defaultSeparator, final char defaultQuoteCharacter) {
		this(fichierCsv, defaultSeparator, defaultQuoteCharacter, CSVParser.DEFAULT_ESCAPE_CHARACTER);
	}

	public CsvToStateBase(final String fichierCsv, final char defaultSeparator, final char defaultQuoteCharacter, final char defaultEscapeCharacter) {
		try {
			reader = new CSVReader(new FileReader(fichierCsv), defaultSeparator, defaultQuoteCharacter, defaultEscapeCharacter);
		} catch (FileNotFoundException e) {
			log.error("Cannot open CSV file, error: " + e.getMessage());
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
	public void executeQuery(final String query) {
		executeQuery(query, null);
	}

	@Override
	public void executeQuery(final String query, final String scope) {
		stateBase = new ArrayList<>();
		try {
			if (null != reader) {
				String[] header = reader.readNext();

				String[] nextLine;
				while ((nextLine = reader.readNext()) != null) {
					Map<String, List<String>> node = new HashMap<String, List<String>>();
					for (int j = 0; j < header.length; j++) {
						String key = header[j];
						String value = nextLine[j];
						node.put(key, Arrays.asList(value));
					}
					stateBase.add(node);
				}
			}
		} catch (IOException e) {
			log.error("Failed to extract CSV entries, error: " + e.getMessage());
		}
	}

	@Override
	public void close() {
		try {
			if (null != reader) {
				reader.close();
			}

			if (null != pageIterator) {
				try {
					pageIterator.close();
				} finally {
					pageIterator = null;
				}
			}
		} catch (IOException e) {
			log.error("Failed to close CSV file, error: " + e.getMessage());
		} finally {
			reader = null;
		}
	}
	
	@Override
	public void clear() {
		stateBase.clear();
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy) throws AlambicException {
		pageIterator = new CsvToStateBaseIterator(pageSize);
		return pageIterator;
	}

	public class CsvToStateBaseIterator implements Iterator<List<Map<String, List<String>>>> {

		private final Log log = LogFactory.getLog(CsvToStateBaseIterator.class);

		private List<Map<String, List<String>>> entries;
		private int offset;
		private int total;
		private final int pageSize;

		public CsvToStateBaseIterator(final int pageSize) {
			this.offset = 0;
			this.total = 0;
			this.pageSize = pageSize;
			this.entries = Collections.emptyList();

			try {
				/* Perform search */
				executeQuery(null);
				entries = getStateBase();
				total = entries.size();
			} catch (Exception e) {
				log.error("Failed to instanciate the CSV source page iterator. error : " + e.getMessage());
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