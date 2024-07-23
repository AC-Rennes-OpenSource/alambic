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
package fr.gouv.education.acrennes.alambic.nuxeo;

import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultOperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultSession;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;

import java.util.Map;

public class AlambicOperationRequest extends DefaultOperationRequest {

    public AlambicOperationRequest(final Session session, final OperationDocumentation operation, final Map<String, Object> ctx) {
        super((DefaultSession) session, operation, ctx);
    }

    @Override
    public Object execute() throws Exception {
        Object obj = getInput();

        OperationDocumentation operation = getOperation();
        if (operation.getUrl().matches("fr.gouv.education.acrennes.alambic.*")) {
            String clazz = operation.getUrl();
            AlambicOperation runner = (AlambicOperation) Class.forName(clazz).newInstance();
            obj = runner.execute(this);
        } else {
            obj = super.execute();
        }

        return obj;
    }

}