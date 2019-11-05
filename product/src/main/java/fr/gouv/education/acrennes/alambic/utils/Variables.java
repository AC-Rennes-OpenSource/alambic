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
package fr.gouv.education.acrennes.alambic.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;

public class Variables {

	private Map<String, String> tableVars = new HashMap<>();
	private String delimiter = "%";

	public void loadFromXmlNode(final List<Element> listeVars) {
		// Récupération des variables
		for (final Element element : listeVars) {
			final String value = element.getText();
			final String key = element.getAttributeValue("name");
			if ((key != null) && (value != null)) {
				tableVars.put(key, value);
			}
		}
	}

	public void loadFromMap(final Map<String, String> map) {
		// Récupération des variables
		for (final Entry<String, String> entry : map.entrySet()) {
			if ((entry.getKey() != null) && (entry.getValue() != null)) {
				tableVars.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public void executeFunctions() throws AlambicException {
		final Map<String, String> tableTemp = new HashMap<>();
		for (final Entry<String, String> entry : tableVars.entrySet()) {
			final String value = Functions.getInstance().executeAllFunctions(resolvString(entry.getValue()));
			if ((entry.getKey() != null) && (value != null)) {
				tableTemp.put(entry.getKey(), value);
			}
		}
		tableVars = tableTemp;
	}

	public void clearTable() {
		tableVars.clear();
	}

	public String resolvString(String sR) {
		if (StringUtils.isNotBlank(sR)) {
			for (final Entry<String, String> entry : tableVars.entrySet()) {
				// Dans le cas ou la valeur contient des valeurs séparées par ":"
				if (sR.contains(delimiter + entry.getKey() + delimiter)) {
					sR = sR.replace(delimiter + entry.getKey() + delimiter, entry.getValue());
					if (sR.matches("(?s).*" + delimiter + ".+" + delimiter + ".*")) {
						// use case: variables that references variables
						sR = resolvString(sR);
					}
					break;
				}
			}


			// Effacement des paramètres non valorisés
			if (sR.matches("(.*(%[p|i|c]([0-9])*%).*)+")) {
				sR = sR.replaceAll("(%[p|i|c]([0-9])*%)+", "");
			}
		} else {
			sR = "";
		}

		return sR.trim();
	}

	public void setDelimiter(final String delimiter) {
		this.delimiter = delimiter;
	}

	public void put(final String key, final String value) {
		tableVars.put(key, value);

	}

	public Map<String, String> getHashMap() {
		return tableVars;
	}

	public void loadFromExtraction(final Map<String, List<String>> map) {
		// Récupération des variables
		for (final Entry<String, List<String>> entry : map.entrySet()) {
			if ((entry.getKey() != null) && CollectionUtils.isNotEmpty(entry.getValue())) {
				tableVars.put(entry.getKey(), entry.getValue().get(0));
			}
		}
	}

}
