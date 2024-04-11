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

import java.util.Properties;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Attribute;
import org.jdom2.Document;

import fr.gouv.education.acrennes.alambic.utils.Variables;

public class JobContext implements CallableContext {

	private static final Log log = LogFactory.getLog(JobContext.class);
	private static final Float DEFAULT_LEGACY_VERSION = new Float(1.0);

	private Variables variables;
	private String executionPath = "./";
	private Document jobsDocument;

	public JobContext() {}

	public JobContext(final String executionPath, final Document document, final Variables variables, final Properties configuration) throws AlambicException {
		this.jobsDocument = document;
		
		// Initialize the execution path
		if (StringUtils.isNotBlank(executionPath)) {
			this.executionPath = executionPath;
		}

		/** Initialize the execution environment
		 */
		
		// Set the variable dealing with the target environment
		this.variables = variables;
		String targetEnv = System.getenv(TARGET_ENVIRONMENT);
		if (StringUtils.isNotBlank(targetEnv)) {
			this.variables.put(TARGET_ENVIRONMENT, targetEnv);
			log.info("[TARGET ENVIRONMENT : " + targetEnv + "]");
		} else {
			log.error("Target environnement variable is not set '" + TARGET_ENVIRONMENT + "'");
		}

		// Set the variable dealing with the engine keystore path if not already set
		String keystorePath = this.variables.getHashMap().get(KEYSTORE_PATH);
		if (StringUtils.isBlank(keystorePath)) {
			keystorePath = System.getenv(KEYSTORE_PATH);
			if (StringUtils.isNotBlank(keystorePath)) {
				this.variables.put(KEYSTORE_PATH, keystorePath);
			} else {
				keystorePath = executionPath.concat(DEFAULT_KEYSTORE_RELATIVE_PATH);
				this.variables.put(KEYSTORE_PATH, keystorePath);
			}
		}
		log.info("Set the keystore path : " + keystorePath);
			
		// Set the variables dealing with configured database credentials
		if (null != configuration && 0 < configuration.size()) {
			this.variables.put(ETL_JDBC_DRIVER, (String) configuration.get(ETL_CFG_JDBC_DRIVER));
			this.variables.put(ETL_JDBC_URL, (String) configuration.get(ETL_CFG_JDBC_URL));
			this.variables.put(ETL_JDBC_LOGIN, (String) configuration.get(ETL_CFG_JDBC_LOGIN));
			this.variables.put(ETL_JDBC_PASSWORD, (String) configuration.get(ETL_CFG_JDBC_PASSWORD));
			log.debug(String.format("Loaded database configuration - driver:%s, url:%s, login:%s", this.variables.getHashMap().get(ETL_JDBC_DRIVER), this.variables.getHashMap().get(ETL_JDBC_URL), this.variables.getHashMap().get(ETL_JDBC_LOGIN)));
		}
	}

	@Override
	public Float getVersion() {
		Float version = DEFAULT_LEGACY_VERSION;
		
		if (null != this.jobsDocument) {
			Attribute versAttr = JobHelper.getVersion(this.jobsDocument);
			if (null != versAttr && StringUtils.isNotBlank(versAttr.getValue().trim())) {
				version = new Float(versAttr.getValue().trim());
			}
		}
		
		return version;
	}

	@Override
	public void setExecutionPath(String path) {
		this.executionPath = path;
	}

	@Override
	public String getExecutionPath() {
		return executionPath;
	}

	@Override
	public void setVariables(Variables variables) {
		this.variables = variables;
	}

	@Override
	public Variables getVariables() {
		return variables;
	}

	@Override
	public void setJobDocument(Document document) {
		this.jobsDocument = document;
	}
	
	@Override
	public Document getJobDocument() {
		return this.jobsDocument;
	}
	
	@Override
	public String resolvePath(final String path) throws AlambicException {
		return resolveString(path);
	}

	@Override
	public String resolveString(final String str) throws AlambicException {
		return variables.resolvString(str);
	}

	
	@Override
	public CallableContext clone() {
		JobContext clone = new JobContext();
		clone.setExecutionPath(this.executionPath);
		clone.setVariables(this.variables);
		clone.setJobDocument(this.jobsDocument);
		return clone;
	}

}
