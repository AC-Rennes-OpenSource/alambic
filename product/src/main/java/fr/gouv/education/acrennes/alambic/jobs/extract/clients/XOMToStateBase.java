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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;

public class XOMToStateBase implements IToStateBase {
	private static final Log log = LogFactory.getLog(XOMToStateBase.class);

	private List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private Document root;

	public XOMToStateBase(final String xmlfile) {
		try {
			root = new Builder().build(xmlfile);
		} catch (Exception e) {
			log.error("Failed to instantiate the XOM source client, error: " + e.getMessage());
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
	public void executeQuery(final String query) {
		executeQuery(query, null);
	}

	@Override
	public void executeQuery(final String xpath, final String scope) {
		stateBase = new ArrayList<>();
		if (StringUtils.isNotBlank(xpath)) {
			Nodes nodes = root.query(xpath);
			log.debug("Found " + nodes.size() + " results");
			for (int i = 0; i < nodes.size(); i++) {
				Element elt = (Element) nodes.get(i);
				Map<String, List<String>> item = new HashMap<String, List<String>>();
				item.put("name", Arrays.asList(elt.getLocalName()));
				item.put("value", Arrays.asList(elt.getValue()));
				item.put("xml", Arrays.asList(elt.toXML()));
				for (int j = 0; j < elt.getAttributeCount(); j++) {
					Attribute attribute = elt.getAttribute(j);
					item.put("@" + attribute.getLocalName(), Arrays.asList(attribute.getValue()));
				}
				stateBase.add(item);
				log.debug("Found XML element: " + item);
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