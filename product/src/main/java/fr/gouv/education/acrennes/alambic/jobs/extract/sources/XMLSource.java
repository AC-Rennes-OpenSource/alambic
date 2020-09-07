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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.IToStateBase;
import fr.gouv.education.acrennes.alambic.jobs.transform.XmlToFileByXslt;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class XMLSource extends AbstractSource {

	private static final Log log = LogFactory.getLog(XMLSource.class);

	public XMLSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(final Element sourceNode) throws AlambicException {
		String xml = context.resolvePath(sourceNode.getChildText("xml"));
		String xslt = context.resolvePath(sourceNode.getChildText("xslt"));
		String pivot = context.resolvePath(sourceNode.getChildText("pivot"));

		if (StringUtils.isNotBlank(xml) && StringUtils.isNotBlank(xslt) && StringUtils.isNotBlank(pivot)) {
			log.info("  -> Transfo XSLT du fichier XML [" + xml + "]");
			XmlToFileByXslt.transform(xml, xslt, pivot);
		}

		setClient(new XMLClient(xml));
	}

	private class XMLClient implements IToStateBase {
		
		private final String xml;
		private List<Map<String, List<String>>> stateBase = new ArrayList<>();

		public XMLClient(final String xml) {
			this.xml = xml;
		}

		@Override
		public void executeQuery(final String query) {
			executeQuery(query, null);
		}

		@Override
		public void executeQuery(final String query, final String scope) {
			stateBase = new ArrayList<>();
			Map<String, List<String>> h = new HashMap<String, List<String>>();
			List<String> l = new ArrayList<String>();
			l.add(this.xml);
			h.put("xml", l);
			this.stateBase.add(h);
		}

		@Override
		public List<Map<String, List<String>>> getStateBase() {
			return this.stateBase;
		}

		@Override
		public int getCountResults() {
			return this.stateBase.size();
		}

		@Override
		public void close() {
			// no-operation
		}

		@Override
		public void clear() {
			this.stateBase.clear();
		}

		@Override
		public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy)
				throws AlambicException {
			throw new AlambicException("Not implemented operation");
		}

	}

}