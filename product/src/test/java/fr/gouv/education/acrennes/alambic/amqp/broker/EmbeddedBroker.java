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
package fr.gouv.education.acrennes.alambic.amqp.broker;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.server.SystemLauncher;

/**
 * Embedded AMQP broker implemented by Apache project QPID
 *
 */
public class EmbeddedBroker
{
	private static final String INITIAL_BROKER_CONFIG_PATH = "data/qpid/qpid-broker-config.json";
//	private static final int BROKER_PORT = 15672;
	private final SystemLauncher systemLauncher;
	
	public EmbeddedBroker() {
		systemLauncher = new SystemLauncher();
	}

	public void startBroker() throws Exception {
		systemLauncher.startup(createSystemConfig());
	}

	public void stopBroker() {
		systemLauncher.shutdown();
	}

	private Map<String, Object> createSystemConfig() {		
		Map<String, Object> attributes = new HashMap<>();
		URL initialConfig = EmbeddedBroker.class.getClassLoader().getResource(INITIAL_BROKER_CONFIG_PATH);
		attributes.put("type", "Memory");
//		attributes.put("qpid.amqp_port", String.valueOf(BROKER_PORT));		
//		attributes.put("qpid.work_dir", Files.createTempDir().getAbsolutePath());
		attributes.put("initialConfigurationLocation", initialConfig.toExternalForm());
		attributes.put("startupLoggedToSystemOut", true);
		return attributes;
	}

}
