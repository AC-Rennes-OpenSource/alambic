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
package fr.gouv.education.acrennes.alambic.nuxeo.marshaller;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.model.Documents;

public class EsMarshaller implements JsonMarshaller<Documents> {

	@Override
	public String getType() {
		return "esresponse";
	}

	@Override
	public Class<Documents> getJavaType() {
		return Documents.class;
	}

	@Override
	public Documents read(final JsonParser jp) throws Exception {
		jp.nextToken();
		String key = jp.getCurrentName();
		if ("value".equals(key)) {
			jp.nextToken(); // '{'
			jp.nextToken(); // hopefully "entity-type"
			jp.nextToken(); // its value
			String etype = jp.getText();
			JsonMarshaller<?> jm = JsonMarshalling.getMarshaller(etype);
			if (null != jm) {
				return (Documents) jm.read(jp);
			}
		} else {
			throw new Exception("missing 'value' field");
		}

		return null;
	}

	@Override
	public void write(final JsonGenerator jg, final Object value) throws Exception {
		// nothing
	}

}