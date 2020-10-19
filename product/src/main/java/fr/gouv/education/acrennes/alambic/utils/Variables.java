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
package fr.gouv.education.acrennes.alambic.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.security.CipherHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class Variables {

	protected static final Log log = LogFactory.getLog(Variables.class);

	private static final String VARIABLE_DELIMITER = "%";
	private static final String VARIABLE_PATTERN = "(?s).*(" + VARIABLE_DELIMITER + ".+" + VARIABLE_DELIMITER + "){1,}.*";
	private Map<String, String> tableVars = new HashMap<>();

	public void loadFromXmlNode(final List<Element> listeVars) {
		Properties keystoreProperties = getKeystoreProperties();

		Map<String, String> mapAliasAlgorithms = new HashMap<>();
		Map<String, Map<String, String>> mapEncryptedValues = new HashMap<>();

		// Récupération des variables
		for (final Element variable : listeVars) {
			loadVariable(mapAliasAlgorithms, mapEncryptedValues, variable);
		}

		// Insertion des variables chiffrées
		for (Entry<String, Map<String, String>> encryptedSet: mapEncryptedValues.entrySet()) {
			String alias = encryptedSet.getKey();
			Map<String, String> variables = encryptedSet.getValue();
			try {
				CipherHelper cipherHelper = new CipherHelper(keystoreProperties, mapAliasAlgorithms.get(alias), alias);
				for (Entry<String, String> variable : variables.entrySet()) {
					if (variable.getKey() != null && variable.getValue() != null) {
						tableVars.put(variable.getKey(), new String(cipherHelper.execute(CipherHelper.CIPHER_MODE.DECRYPT_MODE, Base64.decodeBase64(variable.getValue()))));
					}
				}
			} catch (AlambicException e) {
				log.error("Error while deciphering value : " + e.getMessage());
				log.error("Variables encrypted with alias " + alias + " will not be loaded");
			}
		}
	}

	private void loadVariable(Map<String, String> mapAliasAlgorithms, Map<String, Map<String, String>> mapEncryptedValues, Element variable) {
		final String value = variable.getText();
		final String key = variable.getAttributeValue("name");
		if (variable.getAttribute("encrypted") != null && variable.getAttribute("alias") != null) {
			final String algorithm = variable.getAttributeValue("encrypted");
			final String alias = variable.getAttributeValue("alias");

			if (algorithm.equals("RSA") || algorithm.equals("AES")) {
				mapAliasAlgorithms.put(alias, algorithm);
				if (!mapEncryptedValues.containsKey(alias)) {
					mapEncryptedValues.put(alias, new HashMap<>());
				}
				mapEncryptedValues.get(alias).put(key, value);
			} else {
				log.error(algorithm + " is not a supported encryption algorithm, variable " + key + " will not be loaded");
			}
		} else {
			if (key != null && value != null) {
				tableVars.put(key, value);
			}
		}
	}

	private Properties getKeystoreProperties() {
		// Chargement des propriétés de chiffrement
		Properties keystoreProperties = new Properties();
		if (Config.getProperty("repository.security.properties") != null) {
			try(FileInputStream securityPropertiesStream = new FileInputStream(new File(Config.getProperty("repository.security.properties")))) {
				keystoreProperties.load(securityPropertiesStream);
			} catch (IOException e) {
				log.error("Error while loading security properties file : " + e.getMessage());
				log.error("Encrypted variables will not be loaded");
			}
		}
		return keystoreProperties;
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

	public String resolvString(String sR) throws AlambicException {
		List<String> history = new ArrayList<String>();
		history.add(sR);
		return resolvString(sR, history);
	}

	public String resolvString(String sR, List<String> history) throws AlambicException {
		if (StringUtils.isNotBlank(sR)) {
			if (sR.matches(VARIABLE_PATTERN)) {
				for (final Entry<String, String> entry : tableVars.entrySet()) {
					// Dans le cas ou la valeur contient des valeurs séparées par le délimiteur de variable
					if (sR.contains(VARIABLE_DELIMITER + entry.getKey() + VARIABLE_DELIMITER)) {
						sR = sR.replace(VARIABLE_DELIMITER + entry.getKey() + VARIABLE_DELIMITER, entry.getValue());
						if (sR.matches(VARIABLE_PATTERN)) {
							// use case: variables that references variables
							if (!history.contains(sR)) {
								history.add(sR);
								sR = resolvString(sR, history);
							} else {
								throw new AlambicException("Infinite recursive loop detected while resolving the variable '" + history.get(0) + "' (resolution history is '" + String.join(",", history)+ "')");
							}
						}
						break;
					}
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
