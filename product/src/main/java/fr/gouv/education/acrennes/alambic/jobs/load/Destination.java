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
package fr.gouv.education.acrennes.alambic.jobs.load;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination.IsAnythingToDoStatus;

import java.util.Map;

public interface Destination {

    String getType();

    int getPage();

    void setPage(final int page);

    boolean isDryMode();

    boolean isDryModeSupported();

    void setSource(Source source);

    void setResources(Map<String, Source> resources);

    void execute() throws AlambicException;

    void close() throws AlambicException;

    IsAnythingToDoStatus isAnythingToDo();
}
