/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class SourceFilter {

	private Map<String, String> filters;

	public Map<String, String> getFilters() {
		return filters;
	}

	public void setFilters(Map<String, String> filters) {
		this.filters = filters;
	}

	public SourceFilter(String... patterns) {
		setFilters(new HashMap<String, String>());
		for (String pattern : patterns) {
			if (StringUtils.isNotBlank(pattern)) {
				String[] tokens = pattern.split("=");
				String key = tokens[0];
				String value = tokens[1];
				getFilters().put(key, value);
			}
		}
	}

	public boolean accept(Map<String, List<String>> entry) {
		boolean status = true;

		for (String key : getFilters().keySet()) {
			String pattern = Normalizer.normalize(getFilters().get(key), Form.NFD).replaceAll("[^\\p{ASCII}]", ""); // get rid of accentuated characters
			if (entry.containsKey(key)) {
				List<String> entryValues = entry.get(key);
				for (String entryValue : entryValues) {
					String nEntryValue = Normalizer.normalize(entryValue, Form.NFD).replaceAll("[^\\p{ASCII}]", ""); // get rid of accentuated characters
					status = nEntryValue.matches(pattern);
					if (false == status) {
						return false;
					}
				}
			} else {
				status = false;
				break;
			}
		}

		return status;
	}

	public String toString() {
		return getFilters().toString();
	}

}
