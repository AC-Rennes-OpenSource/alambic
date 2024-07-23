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
package fr.gouv.education.acrennes.alambic.jobs;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.util.List;

public class JobHelper {

    private static final String XPATH_FACTORY_CLASS = "org.jdom2.xpath.jaxen.JaxenXPathFactory";

    private JobHelper() {
    }

    public static Document parse(String file) throws AlambicException {
        try {
            SAXBuilder builder = new SAXBuilder();
            return builder.build(new InputSource(file));
        } catch (JDOMException e) {
            throw new AlambicException("Failed to parse the XML file '" + file + "', error : " + e.getMessage());
        } catch (IOException e) {
            throw new AlambicException("Failed to read the XML file '" + file + "', error : " + e.getMessage());
        }
    }

    public static Attribute getVersion(Document document) {
        return evaluateExpressionForAttributes(document, "/*/@version").get(0);
    }

    public static List<Element> getJobs(Document document) {
        return evaluateExpressionForElements(document, "/*/jobs/job");
    }

    public static List<Element> getTemplates(Document document) {
        return evaluateExpressionForElements(document, "/*/templates");
    }

    public static List<Element> getVariables(Document document) {
        return evaluateExpressionForElements(document, "/*/variables/variable");
    }

    public static Element getJobDefinition(Document document, String jobName) throws AlambicException {
        Element elt = null;

        if (StringUtils.isNotBlank(jobName)) {
            XPathExpression<Element> expr = XPathFactory.newInstance(XPATH_FACTORY_CLASS).compile("/*/jobs/job[@name='" + jobName + "']",
                    Filters.element());
            elt = expr.evaluateFirst(document);
        } else {
            throw new AlambicException("Job search is not possible : the job name must be specified");
        }

        return elt;
    }

    public static JobDefinition getJobDefinition(CallableContext context, Element jobCallDefinition) throws AlambicException {
        JobDefinition jDef = null;
        CallableContext ctxt = context;

        Document jobDefinitionDocument = context.getJobDocument();
        String jobName = jobCallDefinition.getAttributeValue("name");
        String jobFile = jobCallDefinition.getAttributeValue("file");

        // Get the XML file defining the job if not the current context one
        if (StringUtils.isNotBlank(jobFile)) {
            jobFile = context.resolveString(jobFile);
            jobDefinitionDocument = parse(jobFile);
            ctxt = context.clone();
            ctxt.setJobDocument(jobDefinitionDocument);
        }

        // Get the job definition
        Element def = getJobDefinition(jobDefinitionDocument, jobName);
        if (null != def) {
            jDef = new JobDefinition(ctxt, def);
        }

        return jDef;
    }

    public static Element getTemplateDefinition(Document document, String templateName) throws AlambicException {
        Element elt = null;

        if (StringUtils.isNotBlank(templateName)) {
            XPathExpression<Element> expr = XPathFactory.newInstance(XPATH_FACTORY_CLASS).compile("/*/templates/job[@name='" + templateName + "']",
                    Filters.element());
            elt = expr.evaluateFirst(document);
        } else {
            throw new AlambicException("TemplateName search is not possible : the job name must be specified");
        }

        return elt;
    }

    public static List<Element> evaluateExpressionForElements(Document document, String xpath) {
        XPathExpression<Element> expr = XPathFactory.newInstance(XPATH_FACTORY_CLASS).compile(xpath, Filters.element());
        return expr.evaluate(document);
    }

    public static List<Attribute> evaluateExpressionForAttributes(Document document, String xpath) {
        XPathExpression<Attribute> expr = XPathFactory.newInstance(XPATH_FACTORY_CLASS).compile(xpath, Filters.attribute());
        return expr.evaluate(document);
    }

}
