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
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import java.util.Map;

public abstract class AbstractDestination implements Destination {

    private static final Log log = LogFactory.getLog(AbstractDestination.class);
    public static final int NOT_PAGED = -1;

    public enum IsAnythingToDoStatus {
        UNDEFINED,
        YES,
        NO
    }

    protected Element job;
    protected ActivityMBean jobActivity;
    protected Source source;
    protected Map<String, Source> resources;
    protected int page;
    protected boolean isDryMode;
    protected IsAnythingToDoStatus isAnythingToDo = IsAnythingToDoStatus.UNDEFINED;
    protected String type;
    protected CallableContext context;

    public AbstractDestination(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
        this.jobActivity = jobActivity;
        this.job = job;
        this.context = context;
        source = null;
        resources = null;
        page = NOT_PAGED;
        type = job.getAttributeValue("type");
        String dry = job.getAttributeValue("dry");
        isDryMode = Boolean.parseBoolean(context.resolveString(dry));
        if (isDryMode) {
            if (isDryModeSupported()) {
                log.warn("Le mode 'dry' est activé (la cible n'est pas impactée, seul un log reflétant l'activité est produit)");
            } else {
                throw new AlambicException("La destination de type '" + job.getAttributeValue("type") + "' ne supporte pas la mode 'dry'");
            }
        }
    }

    public AbstractDestination() {
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setSource(final Source source) {
        this.source = source;
    }

    @Override
    public void setResources(final Map<String, Source> resources) {
        this.resources = resources;
    }

    @Override
    public void close() throws AlambicException {
    }

    @Override
    public int getPage() {
        return page;
    }

    @Override
    public void setPage(final int page) {
        this.page = page;
    }

    @Override
    public boolean isDryMode() {
        return isDryMode;
    }

    @Override
    public boolean isDryModeSupported() {
        return false; // As default, dry mode is not supported by destination
    }

    @Override
    public IsAnythingToDoStatus isAnythingToDo() {
        return IsAnythingToDoStatus.YES;
    }

    @Override
    abstract public void execute() throws AlambicException;

}
