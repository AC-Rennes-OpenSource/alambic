/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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
package fr.gouv.education.acrennes.alambic.jobs.load;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.GAREleveBuilder;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.GAREnseignantBuilder;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.GAREtablissementBuilder;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.GARGroupeBuilder;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.GARRespAffBuilder;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.GARTypeBuilder;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;

public class StateBaseToGAR extends AbstractDestination {

	private static final Log log = LogFactory.getLog(StateBaseToGAR.class);
	private static final int MAX_XML_NODES_COUNT = 10000;

	private final String type;
	private final EntityManager em;
	private final Map<String, Document> exportFiles;

	public StateBaseToGAR(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
		super(context, job, jobActivity);
		type = context.resolveString(job.getAttributeValue("GARType"));
		em = EntityManagerHelper.getEntityManager();
		em.setFlushMode(FlushModeType.AUTO);
		exportFiles = new HashMap<String, Document>();

		List<Element> xmlFiles = job.getChildren("xmlfile");
		if (null != xmlFiles && !xmlFiles.isEmpty()) {
			for (Element xmlFile : xmlFiles) {
				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					String xmlFileName = context.resolvePath(xmlFile.getValue());
					if (StringUtils.isNotBlank(xmlFileName)) {
						Document document = builder.parse(xmlFileName);
						exportFiles.put(xmlFile.getAttributeValue("name"), document);						
					}
				} catch (ParserConfigurationException | SAXException | IOException e) {
					throw new AlambicException(e.getMessage());
				}
			}
		}
	}

	@Override
	public void execute() throws AlambicException {
		GARTypeBuilder builder;

		switch (type) {
		case "Eleve":
			builder = new GAREleveBuilder(context, resources, page, jobActivity, MAX_XML_NODES_COUNT,
					context.resolveString(job.getAttributeValue("GARVersion")),
					context.resolvePath(job.getChildText("output")),
					context.resolvePath(job.getChildText("xsd")),
					em,
					exportFiles);
			builder.execute();
			break;

		case "Enseignant":
			builder = new GAREnseignantBuilder(context, resources, page, jobActivity, MAX_XML_NODES_COUNT,
					context.resolveString(job.getAttributeValue("GARVersion")),
					context.resolvePath(job.getChildText("output")),
					context.resolvePath(job.getChildText("xsd")),
					em,
					exportFiles);
			builder.execute();
			break;

		case "Etablissement":
			builder = new GAREtablissementBuilder(context, resources, page, jobActivity, MAX_XML_NODES_COUNT,
					context.resolveString(job.getAttributeValue("GARVersion")),
					context.resolvePath(job.getChildText("output")),
					context.resolvePath(job.getChildText("xsd")),
					em,
					exportFiles);
			builder.execute();
			break;

		case "Groupe":
			builder = new GARGroupeBuilder(context, resources, page, jobActivity, MAX_XML_NODES_COUNT,
					context.resolveString(job.getAttributeValue("GARVersion")),
					context.resolvePath(job.getChildText("output")),
					context.resolvePath(job.getChildText("xsd")),
					em);
			builder.execute();
			break;

		case "Responsable":
			builder = new GARRespAffBuilder(context, resources, page, jobActivity, MAX_XML_NODES_COUNT,
					context.resolveString(job.getAttributeValue("GARVersion")),
					context.resolvePath(job.getChildText("output")),
					context.resolvePath(job.getChildText("xsd")),
					null,
					exportFiles);
			builder.execute();
			break;

		default:
			log.error("Not supported output GAR type '" + type + "'");
			break;
		}
	}
	
	@Override
	public void close() {
		this.em.close();
	}
	
}
