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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.transform.StateBaseToFile;
import fr.gouv.education.acrennes.alambic.jobs.transform.StateBaseToFileByFtl;
import fr.gouv.education.acrennes.alambic.jobs.transform.StateBaseToStringByFtl;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.nuxeo.AutomationChainRunner;
import fr.gouv.education.acrennes.alambic.security.CipherHelper;

public class DestinationFactory {

	private static final Log log = LogFactory.getLog(DestinationFactory.class);

	public static Destination getDestination(final CallableContext context, final Element destinationNode, final ActivityMBean jobActivity)
			throws AlambicException {
		Destination destination = null;

		if (null != destinationNode) {
			log.info("  Ecriture dans [" + destinationNode.getAttributeValue("name") + "]");

			String destinationType = destinationNode.getAttributeValue("type");
			if ("csv".equals(destinationType)) {
				destination = new StateBaseToFile(context, destinationNode, jobActivity);
			} else if ("ldap".equals(destinationType)) {
				destination = new StateBaseToLdap(context, destinationNode, jobActivity);
			} else if ("ldapDelete".equals(destinationType)) {
				destination = new StateBaseToLdapDelete(context, destinationNode, jobActivity);
			} else if ("file".equals(destinationType)) {
				destination = new StateBaseToFileByFtl(context, destinationNode, jobActivity);
			} else if ("api".equals(destinationType)) {
				destination = new StateBaseToStringByFtl(context, destinationNode, jobActivity);
			} else if ("nuxeo".equals(destinationType)) {
				destination = new NxmlToNuxeo(context, destinationNode, jobActivity);
			} else if ("nuxeo-chain".equals(destinationType)) {
				destination = new AutomationChainRunner(context, destinationNode, jobActivity);
			} else if ("cipher".equals(destinationType)) {
				destination = new CipherHelper(context, destinationNode, jobActivity);
			} else if ("notification".equals(destinationType)) {
				destination = new XmlToEmail(context, destinationNode, jobActivity);
			} else if ("sql".equals(destinationType)) {
				destination = new SqlLoader(context, destinationNode, jobActivity);
			} else if ("GAR".equals(destinationType)) {
				destination = new StateBaseToGAR(context, destinationNode, jobActivity);
			} else if ("webService".equals(destinationType)) {
				destination = new StateBaseToWS(context, destinationNode, jobActivity);
			} else {
				throw new AlambicException("Type de destination [" + destinationType + "] inconnu.");
			}
		}

		return destination;
	}

}
