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

public class GARBuilderParameters {
    private final CallableContext context;
    private final Map<String, Source> resources;
    private final int page;
    private final ActivityMBean jobActivity;
    private final int maxNodesCount;
    private final String version;
    private final String territoryCode;
    private final String output;
    private final String xsdFile;
    private final EntityManager em;
    private final Map<String, Document> exportFiles;

    public GARBuilderParameters(CallableContext context, Map<String, Source> resources, int page, ActivityMBean jobActivity, int maxNodesCount,
                                String version, String territoryCode, String output, String xsdFile, EntityManager em,
                                Map<String, Document> exportFiles) {
        this.context = context;
        this.resources = resources;
        this.page = page;
        this.jobActivity = jobActivity;
        this.maxNodesCount = maxNodesCount;
        this.version = version;
        this.territoryCode = territoryCode;
        this.output = output;
        this.xsdFile = xsdFile;
        this.em = em;
        this.exportFiles = exportFiles;
    }

    public CallableContext getContext() {
        return context;
    }

    public Map<String, Source> getResources() {
        return resources;
    }

    public int getPage() {
        return page;
    }

    public ActivityMBean getJobActivity() {
        return jobActivity;
    }

    public int getMaxNodesCount() {
        return maxNodesCount;
    }

    public String getVersion() {
        return version;
    }

    public String getTerritoryCode() {
        return territoryCode;
    }

    public String getOutput() {
        return output;
    }

    public String getXsdFile() {
        return xsdFile;
    }

    public EntityManager getEm() {
        return em;
    }

    public Map<String, Document> getExportFiles() {
        return exportFiles;
    }
}
