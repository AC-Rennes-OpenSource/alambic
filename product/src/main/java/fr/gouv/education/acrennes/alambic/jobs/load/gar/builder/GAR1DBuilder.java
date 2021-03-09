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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.ObjectFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class GAR1DBuilder implements GARTypeBuilder {
    private static final Log log = LogFactory.getLog(GAR1DBuilder.class);

    protected final int page;
    protected final ActivityMBean jobActivity;
    protected final int maxNodesCount;
    protected final String version;
    protected final String output;
    protected final EntityManager em;
    protected final String xsdFile;
    protected final ObjectFactory factory;
    protected GAR1DENTWriter writer;

    protected GAR1DBuilder(GARBuilderParameters parameters) {
        this.page = parameters.getPage();
        this.jobActivity = parameters.getJobActivity();
        this.maxNodesCount = parameters.getMaxNodesCount();
        this.version = parameters.getVersion();
        this.output = parameters.getOutput();
        this.em = parameters.getEm();
        this.xsdFile = parameters.getXsdFile();
        factory = new ObjectFactory();
    }

    @Override
    public void execute() throws AlambicException {
        try {
            setWriter();

            for (int index = 0; index < getEntries().size(); index++) {
                setProgress(index, getEntries().size());

                Map<String, List<String>> entity = getEntries().get(index);

                handleEntity(entity);
            }

            // Flush the possibly remaining entities
			writer.flush();

        } catch (JAXBException | SAXException | FileNotFoundException | XPathExpressionException e) {
            failGARExecution(e);
        }
    }

    private void handleEntity(Map<String, List<String>> entity) throws XPathExpressionException, FileNotFoundException, JAXBException {
        try {
            if (checkRestriction(entity)) {
                buildEntity(entity);
            } else {
                log.debug("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' since it doesn't belong to the restriction list");
            }
        } catch (MissingAttributeException e) {
            jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
            log.warn(e.getMessage()); // skip this entity as a missing mandatory field won't allow XML production
        }
    }

    protected abstract void setWriter() throws JAXBException, SAXException;

    protected abstract List<Map<String, List<String>>> getEntries();

    protected abstract boolean checkRestriction(Map<String, List<String>> entity) throws MissingAttributeException, XPathExpressionException;

    protected abstract void buildEntity(Map<String, List<String>> entity) throws MissingAttributeException, FileNotFoundException, JAXBException;

    protected void setProgress(int index, int entriesSize) {
        jobActivity.setProgress(((index +1) * 100) / entriesSize);
        jobActivity.setProcessing("processing entry " + (index + 1) + "/" + entriesSize);
    }

    protected void failGARExecution(Exception e) {
        jobActivity.setTrafficLight(ActivityTrafficLight.RED);
        log.error("Failed to execute the GAR loader, error: " + (StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : e.getCause()));
    }

    protected String getMandatoryAttribute(Map<String, List<String>> entity, String attributeName) throws MissingAttributeException {
		List<String> attribute = entity.get(attributeName);
		if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
			return attribute.get(0);
		} else {
			throw new MissingAttributeException("Skipping entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' as it has no attribute '" + attributeName + "' (mandatory)");
		}
	}

    protected void handleOptionalAttribute(Map<String, List<String>> entity, String attributeName, Consumer<String> method) {
        handleOptionalAttribute(entity, attributeName, method, null);
    }

    protected void handleOptionalAttribute(Map<String, List<String>> entity, String attributeName, Consumer<String> method, Runnable elseMethod) {
        List<String> attribute = entity.get(attributeName);
        if (CollectionUtils.isNotEmpty(attribute) && StringUtils.isNotBlank(attribute.get(0))) {
            method.accept(attribute.get(0));
        } else if (null != elseMethod) {
            elseMethod.run();
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute '" + attributeName +"'");
        }
    }

    protected void handleOptionalList(Map<String, List<String>> entity, String attributeName, Consumer<String> method) {
        List<String> attribute = entity.get(attributeName);
        if (CollectionUtils.isNotEmpty(attribute)) {
            attribute.forEach(method);
        } else {
            log.debug("Entity '" + GARHelper.getInstance().getPersonEntityBlurId(entity) + "' has no attribute '" + attributeName + "'");
        }
    }
}
