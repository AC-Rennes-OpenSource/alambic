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

import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARENTEtab;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GAREtab;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

public class GAR1DEtablissementBuilder extends GAR1DBuilder {

    private final List<Map<String, List<String>>> structures;

    public GAR1DEtablissementBuilder(GARBuilderParameters parameters) {
        super(parameters);
        structures = parameters.getResources().get("Entries").getEntries();
    }

    @Override
    protected void setWriter() throws JAXBException, SAXException {
        writer = new GARENTEtabWriter(factory, version, page, maxNodesCount, output, xsdFile);
    }

    @Override
    protected List<Map<String, List<String>>> getEntries() {
        return structures;
    }

    @Override
    protected boolean checkRestriction(Map<String, List<String>> entity) {
        return true;
    }

    @Override
    protected void buildEntity(Map<String, List<String>> entity) throws MissingAttributeException, FileNotFoundException, JAXBException {
        writer.add(buildEtab(entity));
    }

    public GAREtab buildEtab(Map<String, List<String>> entity) throws MissingAttributeException {
        GAREtab garEtab = factory.createGAREtab();

        garEtab.setGARStructureUAI(getMandatoryAttribute(entity, "ENTStructureUAI"));
        garEtab.setGARStructureNomCourant(getMandatoryAttribute(entity, "ENTDisplayName"));
        handleOptionalAttribute(entity, "ENTEtablissementContrat",
                contrat -> garEtab.setGARStructureContrat(GARHelper.getInstance().getSDETCompliantContractValue(contrat)));
        handleOptionalAttribute(entity, "telephoneNumber", garEtab::setGARStructureTelephone);
        garEtab.setGARStructureEmail(String.format("ce.%s@ac-rennes.fr", garEtab.getGARStructureUAI()));

        return garEtab;
    }

    private static class GARENTEtabWriter extends GAR1DENTWriter {

        private GARENTEtab container;

        protected GARENTEtabWriter(ObjectFactory factory, String version, int page, int maxNodesCount, String output, String xsdFile)
                throws JAXBException, SAXException {
            super(factory, version, page, maxNodesCount, output);
            container = factory.createGARENTEtab();
            container.setVersion(version);
            setMarshallerFrom(JAXBContext.newInstance(GARENTEtab.class), xsdFile);
        }

        @Override
        protected void add(Object item) throws FileNotFoundException, JAXBException {
            if (item instanceof GAREtab) {
                container.getGAREtab().add((GAREtab) item);
            }
            checkNodeCount();
        }

        @Override
        protected void marshal(int increment) throws FileNotFoundException, JAXBException {
            JAXBElement<GARENTEtab> jaxbElement = factory.createGARENTEtab(container);
            marshal(increment, jaxbElement);
            container = factory.createGARENTEtab();
            container.setVersion(version);
        }
    }
}
