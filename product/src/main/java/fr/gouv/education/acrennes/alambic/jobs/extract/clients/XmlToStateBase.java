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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.NotImplementedException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

// FIXME Cette classe n'est pas utilisée (XMLSource utilise la classe interne XMLClient). La supprimer ?
public class XmlToStateBase implements IToStateBase {
	private final List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private List<Element> itemList;
	private InputSource fXml;
	private final int countResults = 0;

	@SuppressWarnings("unchecked")
	public XmlToStateBase(final String fichierXml) throws JDOMException, IOException {
		fXml = new InputSource(fichierXml);
		Element racine = (new SAXBuilder()).build(fXml).getRootElement();
		// Cr�ation d'une List contenant toutes les entr�es
		itemList = racine.getChildren();
		// Chargement de la liste des entrées

		/*
		 * <statebase>
		 * <item>
		 * <key name="PROPERTY">
		 * <value>VALEUR1</value>
		 * <value>VALEUR2</value>
		 * </key>
		 * </item>
		 * <statebase>
		 */

		for (Element item : itemList) {
			Map<String, List<String>> key = new HashMap<>();
			for (Element keyElement : item.getChildren("key")) {
				List<String> value = new ArrayList<>();
				for (Element valueElement : (List<Element>) keyElement) {
					value.add(valueElement.getValue());
				}
				key.put(keyElement.getAttributeValue("name"), value);
			}
			stateBase.add(key);
		}

	}

	@Override
	public int getCountResults() {
		return countResults;
	}

	@Override
	public List<Map<String, List<String>>> getStateBase() {
		return stateBase;
	}

	@Override
	public void executeQuery(final String query) {
		executeQuery(query, null);
	}

	@Override
	public void executeQuery(final String query, final String scope) {
		throw new NotImplementedException("It is not possible to query XML file yet.");
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
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