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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;

public class GrepToStateBase implements IToStateBase {
	private static final Log log = LogFactory.getLog(GrepToStateBase.class);

	private List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private File file;

	public GrepToStateBase(final String file) {
		try {
			this.file = new File(file);
		} catch (Exception e) {
			log.error("Failed to instantiate the GREP source client, error: " + e.getMessage());
		}
	}

	@Override
	public int getCountResults() {
		return stateBase.size();
	}

	@Override
	public List<Map<String, List<String>>> getStateBase() {
		return stateBase;
	}

	@Override
	public void executeQuery(final String regex) {
		executeQuery(regex, null);
	}

	@Override
	public void executeQuery(final String regex, final String scope) {
		stateBase = new ArrayList<>();
		LineIterator li = null;
		int resultIndex = 1;

		if (StringUtils.isNotBlank(regex)) {
			try {
				li = FileUtils.lineIterator(file, "UTF-8");
				while (li.hasNext()) {
					String line = li.nextLine();
					if (line.matches(regex)) {
						Map<String, List<String>> item = new HashMap<String, List<String>>();
						item.put(String.valueOf(resultIndex++), Arrays.asList(line));
						stateBase.add(item);
						log.debug("Found XML element: " + item);
					}
				}
			} catch (IOException e) {
				log.error("Failed to execute query '" + regex + "' on file '" + file.getAbsolutePath() + "'");
			} finally {
				if (null != li) {
					LineIterator.closeQuietly(li);
				}
			}
		}
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	public void clear() {
		stateBase.clear();
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy)
			throws AlambicException {
		throw new AlambicException("Not implemented operation");
	}

}