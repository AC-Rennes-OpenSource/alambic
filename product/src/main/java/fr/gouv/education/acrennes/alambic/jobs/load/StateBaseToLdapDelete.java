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
package fr.gouv.education.acrennes.alambic.jobs.load;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.LdapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

/**
 * ETL export Générique -&gt; LDAP Paramétres pivot de transformation extraction
 * générique LDAP info connexion (url, login, pwd)
 */

public class StateBaseToLdapDelete extends AbstractDestination {

	private static final Log log = LogFactory.getLog(StateBaseToLdapDelete.class);
	private static final String DEFAULT_RDN_ATTRIBUT_NAME = "uid";

	private int count;
	private String rdnAttributeName;
	private DirContext ctx = null;
	private List<Map<String, List<String>>> stateBase = null;

	public StateBaseToLdapDelete(final CallableContext context, final Element destinationNode, final ActivityMBean jobActivity) throws AlambicException {
		super(context, destinationNode, jobActivity);
		count = 0;
        // LDAP configuration
        final Properties confLdap = LdapUtils.getLdapConfiguration(context, destinationNode, true);

		rdnAttributeName = destinationNode.getChildText("rdnAttrName");
		if (StringUtils.isNotBlank(rdnAttributeName)) {
			rdnAttributeName = context.resolveString(rdnAttributeName);
		} else {
			rdnAttributeName = DEFAULT_RDN_ATTRIBUT_NAME;
		}

		try {
			// LDAP context initialization
			ctx = new InitialDirContext(confLdap);
		} catch (final Exception e) {
			throw new AlambicException(e.getMessage());
		}
	}

	@Override
	public void execute() {
		stateBase = source.getEntries();

		// Itération sur l'exportation générique
		for (final Map<String, List<String>> item : stateBase) {
			try {
				// activity monitoring
				jobActivity.setProgress(((count + 1) * 100) / stateBase.size());
				jobActivity.setProcessing("processing entry " + (count + 1) + "/" + stateBase.size());

				/* build RDN of entity to delete */
				if ((null != item.get(rdnAttributeName)) && (0 < item.get(rdnAttributeName).size())) {
					String rdn = (item.get(rdnAttributeName)).get(0);
					if (StringUtils.isNotBlank(rdn)) {
						rdn = String.format(rdnAttributeName.concat("=%s"), rdn);
						if (!isDryMode) {
							ctx.unbind(rdn);
						}
						log.info("Removing the entry [" + rdn + "]");
					} else {
						jobActivity.setTrafficLight(ActivityTrafficLight.RED);
						log.error("The following entry specifies an empty '" + rdnAttributeName + "' attribut : '" + item.toString());
					}
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.RED);
					log.error("The input source '" + source.getName() + "' doesn't contain the '" + rdnAttributeName + "' mandatory attribute");
					return;
				}
			} catch (final NamingException e) {
				jobActivity.setTrafficLight(ActivityTrafficLight.RED);
				log.error("Error while attempting to remove the entry : " + e.getMessage());
			}
		}

		count++;
	}

	@Override
	public boolean isDryModeSupported() {
		return true; // This destination type supports the dry mode
	}

	@Override
	public void close() throws AlambicException {
		super.close();

		// Fermeture du contexte LDAP ouvert
		if (null != ctx) {
			try {
				ctx.close();
				ctx = null;
			} catch (final NamingException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
