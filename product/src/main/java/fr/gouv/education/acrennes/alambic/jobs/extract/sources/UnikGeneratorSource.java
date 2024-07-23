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
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.UnikGeneratorToStateBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class UnikGeneratorSource extends AbstractSource {

    private static final Log log = LogFactory.getLog(UnikGeneratorSource.class);

    private final String DEFAULT_PROCESS_ID = "SOURCE_ID_%s_%s";

    public UnikGeneratorSource(final CallableContext context, final Element sourceNode) throws AlambicException {
        super(context, sourceNode);
    }

    @Override
    public void initialize(Element sourceNode) {

        try {
            long currentThread = Thread.currentThread().getId();
            setClient(new UnikGeneratorToStateBase(String.format(DEFAULT_PROCESS_ID, getName(), currentThread)));
        } catch (Exception e) {
            log.error("Failed to instanciate the client of source '" + getName() + "', error:" + e.getMessage());
        }
    }

}
