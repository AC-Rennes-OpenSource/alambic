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
package fr.gouv.education.acrennes.alambic.jobs.transform;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.freemarker.AlambicObjectWrapper;
import fr.gouv.education.acrennes.alambic.freemarker.FMFunctions;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;
import org.xml.sax.SAXException;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class StateBaseToStringByFtl extends AbstractDestination {

	private static final String DEFAULT_OUTPUT_CHARSET = Charsets.UTF_8.toString();
	private final Configuration cfg;
	private String tplDir;
	private String tplFile;
	private String outputFile;
	private Map<String, Object> root = new HashMap<String, Object>();

	public StateBaseToStringByFtl(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
		super(context, job, jobActivity);
		String template = context.resolveString(job.getChildText("template"));
		tplDir = template.substring(0, template.lastIndexOf("/"));
		tplFile = template.substring(template.lastIndexOf("/") + 1);
		outputFile = "flux API";

		// Get the charset to use for output file encoding
		String outputEncoding = context.resolveString((StringUtils.isNotBlank(job.getChildText("output-charset"))) ? job.getChildText("output-charset") : DEFAULT_OUTPUT_CHARSET);
		if (StringUtils.isNotBlank(outputEncoding)) {
			if (!Charset.isSupported(outputEncoding)) {
				throw new AlambicException("Not supported output encoding charset '" + outputEncoding + "'");
			}
		} else {
			outputEncoding = Charset.defaultCharset().toString();
		}

		// Add wrapping of the Activity traffic light enumeration so that it can be accessed in template
		TemplateHashModel activityEnums;
		BeansWrapper aow = new AlambicObjectWrapper();
		TemplateHashModel enumModels = aow.getEnumModels();
		try {
			activityEnums = (TemplateHashModel) enumModels.get("fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight");
		} catch (TemplateModelException e1) {
			throw new AlambicException(e1);
		}

		// Add wrapping of the normalization policies enumeration so that it can be accessed in template
		TemplateHashModel normalizationEnums;
		try {
			normalizationEnums = (TemplateHashModel) enumModels.get("fr.gouv.education.acrennes.alambic.freemarker.NormalizationPolicy");
		} catch (final TemplateModelException e1) {
			throw new AlambicException(e1);
		}

		// Add wrapping of the HTML encoding formats enumeration so that it can be accessed in template
		TemplateHashModel htmlEncodingEnums;
		try {
			htmlEncodingEnums = (TemplateHashModel) enumModels.get("fr.gouv.education.acrennes.alambic.freemarker.HtmlEncodingFormat");
		} catch (final TemplateModelException e1) {
			throw new AlambicException(e1);
		}

		TemplateHashModel staticModels = aow.getStaticModels();
		
		try {
			root.put("activity", jobActivity);
			root.put("trafficLight", activityEnums);
			root.put("normalizationPolicy", normalizationEnums);
			root.put("htmlEncodingFormats", htmlEncodingEnums);
			root.put("variables", context.getVariables().getHashMap());
			root.put("Fn", new FMFunctions(context));
			root.put("Math", (TemplateHashModel) staticModels.get("java.lang.Math"));

			final List<Element> xmlFiles = job.getChildren("xmlfile");
			if (xmlFiles != null) {
				for (Element xmlFile : xmlFiles) {
					root.put(xmlFile.getAttributeValue("name"), freemarker.ext.dom.NodeModel.parse(new File(context.resolvePath(xmlFile.getText()))));
				}
			}
			
			// Initialization of FreeMarker
			cfg = new Configuration(Constants.FREEMARKER_VERSION);
			cfg.setDirectoryForTemplateLoading(new File(tplDir));
			cfg.setObjectWrapper(aow);
			cfg.setOutputEncoding(outputEncoding);
		} catch (SAXException | IOException | ParserConfigurationException | TemplateModelException e) {
			throw new AlambicException(e);
		}
	}

	@Override
	public void execute() throws AlambicException {
		try {
			root.put("resources", resources);
			root.put("statebase", (null != source) ? source.getEntries() : null);

			StringWriter stringWriter = new StringWriter();
			cfg.getTemplate(tplFile).process(root, stringWriter);
			String result = stringWriter.toString();
			if (StringUtils.isNotBlank(result)) {
				this.jobActivity.setResult(result);
			}
			stringWriter.flush();
		} catch (Exception e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			throw new AlambicException(e);
		}
	}

	@Override
	public void setPage(final int page) {
		super.setPage(page);
		if (NOT_PAGED != page) {
			outputFile = outputFile.concat("." + page);
		}
	}

}
