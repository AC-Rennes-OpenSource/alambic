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

import java.util.Map;
import org.nuxeo.ecm.automation.client.OperationRequest;

public class OperationSetInputVar implements AlambicOperation {

	private static final String jsonDescription = "{"
			+ "\"id\":\"Context.SetInputAsVar\","
			+ "\"label\":\"Set Context Variable From Input\","
			+ "\"category\":\"Execution Context\","
			+ "\"requires\":null,"
			+ "\"description\":\"Set a context variable that points to the current input object. You must give a name for the variable. This operation works on any input type and return back the input as the output.\","
			+ "\"url\":\"" + OperationSetInputVar.class.getName() + "\","
			+ "\"signature\":[\"void\",\"void\"],"
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
	public Object execute(final OperationRequest request) {
		Map<String, Object> parameters = request.getParameters();
		request.getContextParameters().put((String) parameters.get("name"), request.getInput());
		return request.getInput();
	}

}