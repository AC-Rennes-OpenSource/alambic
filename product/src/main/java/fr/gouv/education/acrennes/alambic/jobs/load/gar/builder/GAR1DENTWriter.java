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

import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public abstract class GAR1DENTWriter {
    protected final ObjectFactory factory;
    protected final String version;
    private final int page;
    private final int maxNodesCount;
    private final String output;
    private int nodeCount;

    private Marshaller marshaller;

    protected GAR1DENTWriter(ObjectFactory factory, String version, int page, int maxNodesCount, String output) {
        this.factory = factory;
        this.version = version;
        this.page = page;
        this.maxNodesCount = maxNodesCount;
        this.output = output;
        nodeCount = 0;
    }

    protected void setMarshallerFrom(JAXBContext context, String xsdFile) throws JAXBException, SAXException {
        marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(xsdFile));
        marshaller.setSchema(schema);
    }

    protected abstract void add(Object item) throws FileNotFoundException, JAXBException;

    protected void checkNodeCount() throws FileNotFoundException, JAXBException {
        nodeCount++;
        if (0 == nodeCount % maxNodesCount) {
            marshal(nodeCount / maxNodesCount);
        }
    }

    protected void flush() throws FileNotFoundException, JAXBException {
        marshal((nodeCount / maxNodesCount) + 1);
    }

    protected abstract void marshal(final int increment) throws FileNotFoundException, JAXBException;

    protected void marshal(final int increment, final JAXBElement<?> jaxbElement) throws FileNotFoundException, JAXBException {
        String outputFileName = GARHelper.getInstance().getOutputFileName(output, page, increment);
        marshaller.marshal(jaxbElement, new FileOutputStream(outputFileName));
    }
}
