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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.GrepToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class GREPSource extends AbstractSource {

	private static final Log log = LogFactory.getLog(GREPSource.class);

	public GREPSource(final CallableContext context, final Element sourceNode) throws AlambicException {
		super(context, sourceNode);
	}

	@Override
	public void initialize(final Element sourceNode) throws AlambicException {
		String input = sourceNode.getChildText("input");
		if (StringUtils.isBlank(input)) {
			log.error("'input' parameter cannot be null");
		} else {
			input = context.resolvePath(input);
		}

		query = sourceNode.getChildText("query");
		if (StringUtils.isBlank(query) && !isDynamic()) {
			log.error("regex request is missing");
		} else if (StringUtils.isNotBlank(query)) {
			query = Functions.getInstance().executeAllFunctions(context.resolveString(query));
		}

		setClient((StringUtils.isNotBlank(input)) ? new GrepToStateBase(input) : null);
	}

}