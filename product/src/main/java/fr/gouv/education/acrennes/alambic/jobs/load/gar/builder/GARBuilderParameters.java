/*******************************************************************************
 * Copyright (C) 2019-2021 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import org.w3c.dom.Document;

import javax.persistence.EntityManager;
import java.util.Map;

public record GARBuilderParameters(CallableContext context, Map<String, Source> resources, int page, ActivityMBean jobActivity, int maxNodesCount,
                                   String version, String territoryCode, String output, String xsdFile, EntityManager em,
                                   Map<String, Document> exportFiles) {
}
