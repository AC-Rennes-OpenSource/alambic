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
package fr.gouv.education.acrennes.alambic.jobs.transform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.log4j.Logger;

import fr.gouv.education.acrennes.alambic.Constants;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class XmlToFileByFtl {
	private final Logger logger;
	private final Configuration cfg;
	Template tpl;

	public XmlToFileByFtl(final String templatePath) {
		super();
		// initialisation du logger
		logger = Logger.getLogger(this.getClass());

		cfg = new Configuration(Constants.FREEMARKER_VERSION);
		try {
			cfg.setDirectoryForTemplateLoading(new File(templatePath));
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		// Specify how templates will see the data-model. This is an advanced topic...
		// but just use this:
		cfg.setObjectWrapper(new DefaultObjectWrapper());
	}

	public boolean convert(final Map<?, ?> model, final String templateFile, final String outputFile) {
		Writer out;
		if (outputFile != null) {
			OutputStream outputStream;
			try {
				outputStream = new FileOutputStream(outputFile);
				out = new OutputStreamWriter(outputStream);
			} catch (FileNotFoundException e) {
				logger.error("Opening outputFile [" + outputFile + "] " + e.getMessage());
				out = new OutputStreamWriter(System.out);
			}
		}
		else {
			out = new OutputStreamWriter(System.out);
		}
		try {
			tpl = cfg.getTemplate(templateFile);

			try {
				tpl.process(model, out);
			} catch (TemplateException e) {
				logger.error("Process convertion : " + e.getMessage());
			}
			out.flush();
			return true;
		} catch (IOException e) {
			logger.error("Opening template file : " + e.getMessage());
		}
		return false;
	}

}
