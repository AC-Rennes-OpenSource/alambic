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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.FSToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Functions;

public class FileExplorerSource extends AbstractSource {

	private static final Log log = LogFactory.getLog(FileExplorerSource.class);

	private int pageSize;

	public FileExplorerSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(final Element sourceNode) {

		try {
			query = sourceNode.getChildText("query");
			if (StringUtils.isBlank(query) && !isDynamic()) {
				log.error("Query is missing");
			} else if (StringUtils.isNotBlank(query)) {
				query = Functions.getInstance().executeAllFunctions(context.resolveString(query));
			}

			String page = sourceNode.getAttributeValue("page");
			if (StringUtils.isNotBlank(page)) {
				pageSize = Integer.parseInt(context.resolveString(page));
			}

			setClient(new FSToStateBase());
		} catch (Exception e) {
			log.error("Failed to instanciate the client of source '" + getName() + "', error:" + e.getMessage());
		}
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException {
		return getClient().getPageIterator(query, null, pageSize, null, null);
	}

}
