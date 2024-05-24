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

import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARENTRespAff;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARRespAff;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GAR1DRespAffBuilder extends GAR1DBuilder {
    private static final Log log = LogFactory.getLog(GAR1DRespAffBuilder.class);

    private final List<Map<String, List<String>>> responsables;
    private final Map<String, Document> exportFiles;
    private final List<String> memberStructuresList;
    private final Pattern pattern;
    private final XPath xpath;

    public GAR1DRespAffBuilder(GARBuilderParameters parameters) {
        super(parameters);
        pattern = Pattern.compile("cn=(\\d+\\w)_GAR1dRespAff(Deleg)?,.+", Pattern.CASE_INSENSITIVE);
        exportFiles = parameters.getExportFiles();
        XPathFactory xpf = XPathFactory.newInstance();
		xpath = xpf.newXPath();
        responsables = parameters.getResources().get("Entries").getEntries();
        memberStructuresList = new ArrayList<>();
        // Chaque structure possédant un unique UAI est ajoutée à la liste
        parameters.getResources().get("Structures").getEntries().stream()
                .map(structure -> structure.get("ENTStructureUAI"))
                .filter(listUAI -> listUAI != null && listUAI.size() == 1)
                .map(list -> list.get(0).toUpperCase())
                .forEach(memberStructuresList::add);
    }

    @Override
    protected void setWriter() throws JAXBException, SAXException {
        writer = new GARENTRespAffWriter(factory, version, page, maxNodesCount, output, xsdFile);
    }

    @Override
    protected List<Map<String, List<String>>> getEntries() {
        return responsables;
    }

    @Override
    protected boolean checkRestriction(Map<String, List<String>> entity) throws MissingAttributeException, XPathExpressionException {
        if (null != exportFiles.get("restrictionList")) {
            Element root = exportFiles.get("restrictionList").getDocumentElement();
            String matchingEntry = (String) xpath.evaluate("//id[.='" + getMandatoryAttribute(entity, "ENTPersonJointure") + "']", root, XPathConstants.STRING);
            return StringUtils.isNotBlank(matchingEntry);
        } else {
            return true;
        }
    }

    @Override
    protected void buildEntity(Map<String, List<String>> entity) throws MissingAttributeException, FileNotFoundException, JAXBException {
        writer.add(buildRespAff(entity));
    }

    public GARRespAff buildRespAff(Map<String, List<String>> entity) throws MissingAttributeException {
        GARRespAff garRespAff = factory.createGARRespAff();

        garRespAff.setGARPersonIdentifiant(getMandatoryAttribute(entity, "ENTPersonUid"));
        garRespAff.setGARPersonNom(getMandatoryAttribute(entity, "sn"));
        garRespAff.setGARPersonPrenom(getMandatoryAttribute(entity, "givenName"));
        handleOptionalAttribute(entity, "personalTitle", title -> garRespAff.setGARPersonCivilite(GARHelper.getInstance().getSDETCompliantTitleValue(title)));
        handleOptionalList(entity, "mail", mail -> {
            if (StringUtils.isNotBlank(mail)) {
                garRespAff.getGARPersonMail().add(mail);
            }
        });

        // At least one mail should be present
        if (garRespAff.getGARPersonMail().isEmpty()) {
            throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'mail' (mandatory)");
        }

        handleOptionalList(entity, "ENTPersonProfils", profil -> {
            Matcher matcher = pattern.matcher(profil);
            if (matcher.matches()) {
                String uai = matcher.group(1).toUpperCase();
                if (memberStructuresList.contains(uai)) {
                    garRespAff.getGARRespAffEtab().add(uai);
                } else {
                    log.info("Responsible with blur identifier '"+ GARHelper.getInstance().getPersonEntityBlurId(entity) +"' is affected in structure ('UAI:" + uai + "') out of the involved list");
                }
            }
        });

        // At least one etab should be present
        if (garRespAff.getGARRespAffEtab().isEmpty()) {
            throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it might not have profile 'GARRespAff' (mandatory)");
        }

        return garRespAff;
    }

    private static class GARENTRespAffWriter extends GAR1DENTWriter {

        private GARENTRespAff container;

        protected GARENTRespAffWriter(ObjectFactory factory, String version, int page, int maxNodesCount, String output, String xsdFile) throws JAXBException, SAXException {
            super(factory, version, page, maxNodesCount, output);
            container = factory.createGARENTRespAff();
            container.setVersion(version);
            setMarshallerFrom(JAXBContext.newInstance(GARENTRespAff.class), xsdFile);
        }

        @Override
        protected void add(Object item) throws FileNotFoundException, JAXBException {
            if (item instanceof GARRespAff) {
                container.getGARRespAff().add((GARRespAff) item);
            }
            checkNodeCount();
        }

        @Override
        protected void marshal(int increment) throws FileNotFoundException, JAXBException {
            JAXBElement<GARENTRespAff> jaxbElement = factory.createGARENTRespAff(container);
            marshal(increment, jaxbElement);
            container = factory.createGARENTRespAff();
            container.setVersion(version);
        }
    }
}
