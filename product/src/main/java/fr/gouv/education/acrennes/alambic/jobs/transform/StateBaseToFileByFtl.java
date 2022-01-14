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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class StateBaseToFileByFtl extends AbstractDestination {

	private static final Log log = LogFactory.getLog(StateBaseToFileByFtl.class);

	private static final String DEFAULT_OUTPUT_CHARSET = Charsets.UTF_8.toString();
	private final Configuration cfg;
	private String tplDir;
	private String tplFile;
	private String outputFile;
	private boolean classicMode;
	Map<String, Object> root = new HashMap<>();

	public StateBaseToFileByFtl(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
		super(context, job, jobActivity);
		final String template = context.resolveString(job.getChildText("template"));
		classicMode = Boolean.parseBoolean(context.resolveString(job.getAttributeValue("classicMode")));
		tplDir = template.substring(0, template.lastIndexOf("/"));
		tplFile = template.substring(template.lastIndexOf("/") + 1);
		outputFile = context.resolvePath(job.getChildText("output"));

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
		final BeansWrapper dataloaderObjectWrapper = new AlambicObjectWrapper();
		final TemplateHashModel enumModels = dataloaderObjectWrapper.getEnumModels();
		try {
			activityEnums = (TemplateHashModel) enumModels.get("fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight");
		} catch (final TemplateModelException e1) {
			throw new AlambicException(e1);
		}

		// Add wrapping of the normalization policies enumeration so that it can be accessed in template
		TemplateHashModel normalizationEnums;
		try {
			normalizationEnums = (TemplateHashModel) enumModels.get("fr.gouv.education.acrennes.alambic.freemarker.NormalizationPolicy");
		} catch (final TemplateModelException e1) {
			throw new AlambicException(e1);
		}

		TemplateHashModel staticModels = dataloaderObjectWrapper.getStaticModels();
		
		try {
			root.put("activity", jobActivity);
			root.put("trafficLight", activityEnums);
			root.put("normalizationPolicy", normalizationEnums);
			root.put("variables", context.getVariables().getHashMap());
			root.put("Fn", new FMFunctions(context));
			root.put("Math", (TemplateHashModel) staticModels.get("java.lang.Math"));

			final List<Element> xmlFiles = job.getChildren("xmlfile");
			if (xmlFiles != null) {
				for (final Element xmlFile : xmlFiles) {
					if (StringUtils.isNotBlank(xmlFile.getText())) {
						root.put(xmlFile.getAttributeValue("name"), freemarker.ext.dom.NodeModel.parse(new File(context.resolvePath(xmlFile.getText())), false, true));
					}
				}
			}

			// Initialisation FreeMarker
			cfg = new Configuration(Constants.FREEMARKER_VERSION);
			cfg.setDirectoryForTemplateLoading(new File(tplDir));
			cfg.setObjectWrapper(dataloaderObjectWrapper);
			cfg.setOutputEncoding(outputEncoding);
			cfg.setClassicCompatible(classicMode);
		} catch (SAXException | IOException | ParserConfigurationException | TemplateModelException e) {
			throw new AlambicException(e);
		}
	}

	@Override
	public void execute() throws AlambicException {
		/* initialize both the 'stateBase' and 'resource' root entry either from the source or resource input XML job definition */
		root.put("statebase", (null != source) ? source.getEntries() : null);
		if ((null != source) || (null != resources)) {
			root.put("resources", (null != resources) ? resources : new HashMap<String, Source>().put(source.getName(), source));
		} else {
			log.warn("Neither source or resources are defined by the job");
		}
		try (OutputStream outputStream = new FileOutputStream(outputFile)) {
			final Writer out = new OutputStreamWriter(outputStream);
			cfg.getTemplate(tplFile).process(root, out);
			out.flush();
		} catch (final Exception e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			throw new AlambicException(e);
		}
	}

	@Override
	public void setPage(final int page) {
		super.setPage(page);
		if (NOT_PAGED != page) {
			outputFile = outputFile.concat("." + String.valueOf(page));
		}
	}

}
