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
package fr.gouv.education.acrennes.alambic.jobs;

import org.jdom2.Element;

public class JobDefinition {
    private CallableContext context;
    private Element definition;

    public JobDefinition(CallableContext context, Element definition) {
        setContext(context);
        setDefinition(definition);
    }

    public CallableContext getContext() {
        return context;
    }

    public void setContext(CallableContext context) {
        this.context = context;
    }

    public Element getDefinition() {
        return definition;
    }

    public void setDefinition(Element definition) {
        this.definition = definition;
    }

}
