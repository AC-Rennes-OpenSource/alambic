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
package fr.gouv.education.acrennes.alambic.jobs;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.Variables;

public class JobContext implements CallableContext {

	private static final Log log = LogFactory.getLog(JobContext.class);
	private static final Float DEFAULT_LEGACY_VERSION = new Float(1.0);

	private final Variables variables;
	private String executionPath = "./";
	private Float version = DEFAULT_LEGACY_VERSION;

	public JobContext(final String executionPath, final String jobFile, final Variables variables, final Properties configuration) throws AlambicException {
		if (StringUtils.isNotBlank(jobFile)) {
			// Initialize the execution version
			try {
				SAXBuilder saxBuilder = new SAXBuilder();
				Element root = saxBuilder.build(new File(jobFile)).getRootElement();
				String attrValue = root.getAttributeValue("version");
				if (StringUtils.isNotBlank(attrValue)) {
					version = new Float(attrValue);
				}
			} catch (JDOMException | IOException e) {
				log.error("Failed to initialize the job execution context, error : " + e.getMessage());
			}
		} // reserved for unit testing purpose

		// Initialize the execution path
		if (StringUtils.isNotBlank(executionPath)) {
			this.executionPath = executionPath;
		}

		/** Initialize the execution environment
		 */
		
		// Set the variable dealing with the target environment
		this.variables = variables;
		String targetEnv = System.getenv(CallableContext.TARGET_ENVIRONMENT);
		if (StringUtils.isNotBlank(targetEnv)) {
			this.variables.put(CallableContext.TARGET_ENVIRONMENT, targetEnv);
			log.info("[TARGET ENVIRONMENT : " + targetEnv + "]");
		} else {
			log.error("Target environnement variable is not set '" + CallableContext.TARGET_ENVIRONMENT + "'");
		}

		// Set the variable dealing with the engine keystore
		String keystorePath = System.getenv(CallableContext.KEYSTORE_PATH);
		if (StringUtils.isNotBlank(keystorePath)) {
			this.variables.put(CallableContext.KEYSTORE_PATH, keystorePath);
		} else {
			keystorePath = executionPath.concat(CallableContext.DEFAULT_KEYSTORE_RELATIVE_PATH);
			log.info("Set the default keystore path : " + keystorePath);
			this.variables.put(CallableContext.KEYSTORE_PATH, keystorePath);
		}

		// Set the variables dealing with configured database credentials
		if (null != configuration && 0 < configuration.size()) {
			this.variables.put(CallableContext.ETL_JDBC_DRIVER, (String) configuration.get(CallableContext.ETL_CFG_JDBC_DRIVER));
			this.variables.put(CallableContext.ETL_JDBC_URL, (String) configuration.get(CallableContext.ETL_CFG_JDBC_URL));
			this.variables.put(CallableContext.ETL_JDBC_LOGIN, (String) configuration.get(CallableContext.ETL_CFG_JDBC_LOGIN));
			this.variables.put(CallableContext.ETL_JDBC_PASSWORD, (String) configuration.get(CallableContext.ETL_CFG_JDBC_PASSWORD));
			log.debug(String.format("Loaded database configuration - driver:%s, url:%s, login:%s", this.variables.getHashMap().get(CallableContext.ETL_JDBC_DRIVER), this.variables.getHashMap().get(CallableContext.ETL_JDBC_URL), this.variables.getHashMap().get(CallableContext.ETL_JDBC_LOGIN)));
		}
	}

	@Override
	public Float getVersion() {
		return version;
	}

	@Override
	public String getExecutionPath() {
		return executionPath;
	}

	@Override
	public Variables getVariables() {
		return variables;
	}

	@Override
	public String resolvePath(final String path) throws AlambicException {
		return resolveString(path);
	}

	@Override
	public String resolveString(final String str) throws AlambicException {
		return variables.resolvString(str);
	}

}
