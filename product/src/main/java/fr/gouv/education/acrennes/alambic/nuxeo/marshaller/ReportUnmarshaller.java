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

import org.eclipse.persistence.jaxb.UnmarshallerProperties;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

public class ReportUnmarshaller {

    private final Unmarshaller unmarshaller;

    public ReportUnmarshaller() throws JAXBException {
        // Create a JaxBContext
        JAXBContext jc = JAXBContext.newInstance(Report.class);

        // Create the Unmarshaller Object using the JaxB Context
        this.unmarshaller = jc.createUnmarshaller();

        // Set the Unmarshaller media type to JSON
        this.unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);

        // Set it to true if you need to include the JSON root element in the JSON input
        this.unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, true);
    }


    public Report unmarshall(String content_json) throws JAXBException {
        // Create the StreamSource by creating StringReader using the JSON input
        StreamSource json = new StreamSource(new StringReader(content_json));

        // Getting the Report pojo from the json representation
        return this.unmarshaller.unmarshal(json, Report.class).getValue();
    }

}