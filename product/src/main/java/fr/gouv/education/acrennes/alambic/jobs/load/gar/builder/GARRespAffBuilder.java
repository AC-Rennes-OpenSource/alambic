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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.GARENTRespAff;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.GARRespAff;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GARRespAffBuilder implements GARTypeBuilder {
    private static final Log log = LogFactory.getLog(GARRespAffBuilder.class);

    private final int page;
    private final int maxNodesCount;
    private final String output;
    private final String xsdFile;
    private final String version;
    private final ActivityMBean jobActivity;
    private final Pattern pattern;
    private final Map<String, Document> exportFiles;
    private final XPath xpath;
    private final List<Map<String, List<String>>> responsables;
    private final List<String> memberStructuresList;

    public GARRespAffBuilder(GARBuilderParameters parameters) {
        this.page = parameters.page();
        this.jobActivity = parameters.jobActivity();
        this.maxNodesCount = parameters.maxNodesCount();
        this.version = parameters.version();
        this.output = parameters.output();
        this.exportFiles = parameters.exportFiles();
        XPathFactory xpf = XPathFactory.newInstance();
        this.xpath = xpf.newXPath();
        this.xsdFile = parameters.xsdFile();
        this.pattern = Pattern.compile("cn=(\\d+\\w)_GARRespAff(Deleg)?,.+", Pattern.CASE_INSENSITIVE);
        // Get the list of involved responsibles
        this.responsables = parameters.resources().get("Entries").getEntries();
        // Get the list of involved structures
        Source structuresSource = parameters.resources().get("Structures");
        this.memberStructuresList = new ArrayList<>();
        List<Map<String, List<String>>> structures = structuresSource.getEntries();
        structures.forEach(structure -> {
            if (null != structure.get("ENTStructureUAI") && 1 == structure.get("ENTStructureUAI").size()) {
                this.memberStructuresList.add(structure.get("ENTStructureUAI").get(0).toUpperCase());
            }
        });
    }

    @Override
    public void execute() {
        try {
            List<String> attribute;
            ObjectFactory factory = new ObjectFactory();
            GARRespAffWriter writer = new GARRespAffWriter(factory, version, page, maxNodesCount);

            for (int index = 0; index < this.responsables.size(); index++) {
                // activity monitoring
                jobActivity.setProgress(((index + 1) * 100) / this.responsables.size());
                jobActivity.setProcessing("processing entry " + (index + 1) + "/" + this.responsables.size());

                Map<String, List<String>> entity = this.responsables.get(index);

                /* Check the current entity belongs to list of those being taken into account.
                 * This control is useful for processing the anonymized data among possibly added
                 * entities via LDAP update daily scripts.
                 */
                if (null != this.exportFiles.get("restrictionList")) {
                    attribute = entity.get("ENTPersonJointure");
                    if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                        Element root = exportFiles.get("restrictionList").getDocumentElement();
                        String matchingEntry = (String) xpath.evaluate("//id[.='" + attribute.get(0) + "']", root, XPathConstants.STRING);
                        if (StringUtils.isBlank(matchingEntry)) {
                            log.debug("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' since it doesn't belong to " +
                                      "the restriction list");
                            continue;
                        }
                    } else {
                        jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                        log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute " +
                                 "'ENTPersonJointure' (mandatory)");
                        continue; // skip this entity as a missing mandatory field won't allow XML production
                    }
                }

                GARRespAff garRespAff = factory.createGARRespAff();
                String ENTPersonIdentifiant = null;

                /*
                 * GARPersonIdentifiant
                 */
                attribute = entity.get("ENTPersonUid");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    ENTPersonIdentifiant = attribute.get(0);
                    garRespAff.setGARPersonIdentifiant(ENTPersonIdentifiant);
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'ENTPersonUid'" +
                             " (mandatory)");
                    continue; // skip this entry as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonNom
                 */
                attribute = entity.get("sn");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    garRespAff.setGARPersonNom(attribute.get(0));
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'sn' " +
                             "(mandatory)");
                    continue; // skip this entry as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonPrenom
                 */
                attribute = entity.get("givenName");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    garRespAff.setGARPersonPrenom(attribute.get(0));
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'givenName' " +
                             "(mandatory)");
                    continue; // skip this entry as a missing mandatory field won't allow XML production
                }

                /*
                 * GARPersonCivilite
                 */
                attribute = entity.get("personalTitle");
                if (null != attribute && !attribute.isEmpty() && StringUtils.isNotBlank(attribute.get(0))) {
                    garRespAff.setGARPersonCivilite(GARHelper.getInstance().getSDETCompliantTitleValue(attribute.get(0)));
                } else {
                    log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute 'personalTitle'");
                }

                /*
                 * GARPersonMail
                 */
                attribute = entity.get("mail");
                if (null != attribute && !attribute.isEmpty()) {
                    for (String mail : attribute) {
                        if (StringUtils.isNotBlank(mail)) {
                            garRespAff.getGARPersonMail().add(mail);
                        }
                    }
                }
                if (garRespAff.getGARPersonMail().isEmpty()) {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute 'mail' " +
                             "(mandatory)");
                    continue; // skip this entry as a missing mandatory field won't allow XML production
                }

                /*
                 * GARRespAffEtab
                 */
                attribute = entity.get("ENTPersonProfils");
                if (null != attribute && !attribute.isEmpty()) {
                    for (String profil : attribute) {
                        Matcher matcher = this.pattern.matcher(profil);
                        if (matcher.matches()) {
                            String uai = matcher.group(1).toUpperCase();
                            // Control the UAI belongs to the involved structures list
                            if (this.memberStructuresList.contains(uai)) {
                                garRespAff.getGARRespAffEtab().add(uai);
                            } else {
                                log.info("Responsible with blur identifier '" + ENTPersonIdentifiant + "' is affected in structure ('UAI:" + uai +
                                         "') out of the involved list");
                            }
                        }
                    }
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute " +
                             "'ENTPersonProfils' (mandatory)");
                    continue; // skip this entry as a missing mandatory field won't allow XML production
                }

                // Verify the mandatory field is present despite the involved structure filtering
                if (null == garRespAff.getGARRespAffEtab() || garRespAff.getGARRespAffEtab().isEmpty()) {
                    jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                    log.warn("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it might not have profile " +
                             "'GARRespAff' (mandatory)");
                    continue; // skip this entry as a missing mandatory field won't allow XML production
                }

                writer.add(garRespAff);
            }

            // Flush the possibly remaining entities
            writer.flush();
        } catch (FileNotFoundException | JAXBException | SAXException | XPathExpressionException e) {
            jobActivity.setTrafficLight(ActivityTrafficLight.RED);
            log.error("Failed to execute the GAR loader, error: " + (StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getCause()));
        }

    }

    private class GARRespAffWriter {

        private int nodeCount;
        private final ObjectFactory factory;
        private GARENTRespAff container;
        private final Marshaller marshaller;
        private final int maxNodesCount;
        private final String version;
        private final int page;

        protected GARRespAffWriter(final ObjectFactory factory, final String version, final int page, final int maxNodesCount)
                throws JAXBException, SAXException {
            this.factory = factory;
            this.version = version;
            this.page = page;
            this.maxNodesCount = maxNodesCount;
            nodeCount = 0;
            container = factory.createGARENTRespAff();
            container.setVersion(version);
            JAXBContext context = JAXBContext.newInstance(GARENTRespAff.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            // Install schema validation
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdFile));
            marshaller.setSchema(schema);
        }

        protected <T> GARENTRespAff add(final T item) throws FileNotFoundException, JAXBException {
            if (item instanceof GARRespAff) {
                container.getGARRespAff().add((GARRespAff) item);
            }

            // Check the file limit size is reached
            nodeCount++;
            if (0 == (nodeCount % maxNodesCount)) {
                marshal(nodeCount / maxNodesCount);
            }

            return container;
        }

        protected void flush() throws FileNotFoundException, JAXBException {
            marshal((nodeCount / maxNodesCount) + 1);
        }

        // Marshal the XML binding
        private void marshal(final int increment) throws FileNotFoundException, JAXBException {
            String outputFileName = GARHelper.getInstance().getOutputFileName(output, page, increment);
            JAXBElement<GARENTRespAff> jaxbElt = factory.createGARENTRespAff(container);
            marshaller.marshal(jaxbElt, new FileOutputStream(outputFileName));
            container = factory.createGARENTRespAff();
            container.setVersion(version);
        }

    }

}
