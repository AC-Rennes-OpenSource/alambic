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

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
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

		String driver = destinationNode.getChildText("driver");
		if (StringUtils.isNotBlank(driver)) {
			driver = context.resolveString(driver);
		} else {
			driver = "com.sun.jndi.ldap.LdapCtxFactory";
		}

		String uri = destinationNode.getChildText("uri");
		if (StringUtils.isNotBlank(uri)) {
			uri = context.resolveString(uri);
		} else {
			throw new AlambicException("l'uri de l'annuaire n'est pas precisée");
		}

		String login = destinationNode.getChildText("login");
		if (StringUtils.isNotBlank(login)) {
			login = context.resolveString(login);
		} else {
			throw new AlambicException("le login de l'annuaire n'est pas precisé");
		}

		String pwd = destinationNode.getChildText("passwd");
		if (StringUtils.isNotBlank(pwd)) {
			pwd = context.resolveString(pwd);
		} else {
			throw new AlambicException("le mot de passe de l'annuaire n'est pas precisé");
		}

		rdnAttributeName = destinationNode.getChildText("rdnAttrName");
		if (StringUtils.isNotBlank(rdnAttributeName)) {
			rdnAttributeName = context.resolveString(rdnAttributeName);
		} else {
			rdnAttributeName = DEFAULT_RDN_ATTRIBUT_NAME;
		}

		try {
			// LDAP configuration & context initialization
			final Hashtable<String, String> confLdap = new Hashtable<>(5);
			confLdap.put(Context.INITIAL_CONTEXT_FACTORY, driver);
			confLdap.put(Context.PROVIDER_URL, uri);
			confLdap.put(Context.SECURITY_PRINCIPAL, login);
			confLdap.put(Context.SECURITY_CREDENTIALS, pwd);
			confLdap.put("com.sun.jndi.ldap.connect.pool", "true");
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
						log.info("Effacement de l'entree [" + rdn + "]");
					} else {
						jobActivity.setTrafficLight(ActivityTrafficLight.RED);
						log.error("The following entry specifies an empty '" + rdnAttributeName + "' attribut : '" + item.toString());
					}
				} else {
					jobActivity.setTrafficLight(ActivityTrafficLight.RED);
					log.error("The input source '" + source.getName() + "' doesn't contain the '" + rdnAttributeName + "' mandatory attribut");
					return;
				}
			} catch (final NamingException e) {
				jobActivity.setTrafficLight(ActivityTrafficLight.RED);
				log.error("Erreur lors de l'effacement de l'entree : " + e.getMessage());
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
