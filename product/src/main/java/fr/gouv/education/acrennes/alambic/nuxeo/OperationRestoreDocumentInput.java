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

import org.nuxeo.ecm.automation.client.OperationRequest;

import java.util.Map;

public class OperationRestoreDocumentInput implements AlambicOperation {

    private static final String jsonDescription = "{"
                                                  + "\"id\":\"Context.RestoreDocumentInput\","
                                                  + "\"label\":\"Restore Document Input\","
                                                  + "\"category\":\"Execution Context\","
                                                  + "\"requires\":null,"
                                                  + "\"description\":\"Restore the document input from a context variable given its name. Return " +
                                                  "the document.\","
                                                  + "\"url\":\"" + OperationRestoreDocumentInput.class.getName() + "\","
                                                  + "\"signature\":[\"void\",\"document\"],"
                                                  + "\"params\":[{"
                                                  + "\"name\":\"name\","
                                                  + "\"description\":\"\","
                                                  + "\"type\":\"string\","
                                                  + "\"required\":true,"
                                                  + "\"widget\":null,"
                                                  + "\"order\":0,"
                                                  + "\"values\":[]}]"
                                                  + "}";

    public static String getJSONDescription() {
        return jsonDescription;
    }

    @Override
    public Object execute(OperationRequest request) {
        Map<String, Object> parameters = request.getParameters();
        return request.getContextParameters().get(parameters.get("name"));
    }

}