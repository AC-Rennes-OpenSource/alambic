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
import javax.naming.directory.SearchControls;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.LdapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.JobHelper;
import fr.gouv.education.acrennes.alambic.ldap.Datasources;
import fr.gouv.education.acrennes.alambic.ldap.Entry;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.utils.Variables;

/**
 * ETL export Générique -&gt; LDAP Paramétres pivot de transformation extraction générique LDAP info connexion (url,
 * login, pwd)
 */

public class StateBaseToLdap extends AbstractDestination {

	private static final Log log = LogFactory.getLog(StateBaseToLdap.class);

	private List<Map<String, List<String>>> extraction = null;
	private Datasources datasources;
	private Element pivot;
	private SearchControls contraintes = new SearchControls();
	private DirContext ctx;
	private String fichierPivot;

	public Variables variables = new Variables();

	public StateBaseToLdap(final CallableContext context, final Element destinationNode, final ActivityMBean jobActivity) throws AlambicException {
		super(context, destinationNode, jobActivity);

        final Properties confLdap = LdapUtils.getLdapConfiguration(context, destinationNode, true);
		this.fichierPivot = destinationNode.getChildText("pivot");
		if (StringUtils.isNotBlank(this.fichierPivot)) {
			this.fichierPivot = context.resolvePath(this.fichierPivot);
		} else {
			throw new AlambicException("le pivot n'est pas precisé");
		}

		// LDAP configuration & context initialization (uniquement si nécessaire)
		try {
			if (isAnythingToDo().equals(IsAnythingToDoStatus.YES)) {
				ctx = new InitialDirContext(confLdap);
				contraintes.setSearchScope(SearchControls.ONELEVEL_SCOPE);
				// Configuration du pivot
				final InputSource fPivot = new InputSource(fichierPivot);
				pivot = (new SAXBuilder()).build(fPivot).getRootElement();
				// Chargement des variables de la liste de jobs
				reloadVariablesList();
				// Lecture des sources de données du pivot
				datasources = new Datasources(pivot, context.getVariables());
			}
		} catch (final Exception e) {
			throw new AlambicException(e);
		}
	}

	/*
	 * Methode pour nettoyer les datasources ouverts
	 */
	@Override
	public void close() throws AlambicException {
		super.close();
		
        if (null != datasources) {
        	datasources.close();
        }

		// Fermeture du contexte LDAP ouvert
		if (null != ctx) {
			try {
				ctx.close();
				ctx = null;
			} catch (final NamingException e) {
				log.error("Erreur", e);
			}
		}
	}

	/*
	 * Chargement ds l'annuaire
	 */
	@Override
	public void execute() throws AlambicException {
		extraction = source.getEntries();
		final int pivotEntriesCount = pivot.getChild("entries").getChildren().size();
		int currentPivotEntriesIndex = 1;

		// Itération sur l'exportation générique
		for (final Map<String, List<String>> currentResult : extraction) {
			// MAJ de la liste de variables par rapport à l'extract courant
			loadVariablesList(currentResult);

			// Itération sur la liste des entrées du pivot
			for (final Element xmlNode : pivot.getChild("entries").getChildren()) {
				// activity monitoring
				jobActivity.setProgress((currentPivotEntriesIndex * 100) / (pivotEntriesCount * extraction.size()));
				jobActivity.setProcessing("processing entry " + currentPivotEntriesIndex + "/" + (pivotEntriesCount * extraction.size()));

				// Création d'un objet Entrée de pivot
				try {
					final Entry entry = new Entry(xmlNode, ctx, contraintes, variables, currentResult, datasources);
					try {
						entry.update();
					} finally {
						entry.close();
					}
				} catch (final Exception e) {
					jobActivity.setTrafficLight(ActivityTrafficLight.RED);
					log.error("MAJ de l'entrée [" + currentResult.toString() + "] ERREUR LDAP");
					log.error("Erreur ldap sur l'élément " + xmlNode.getText(), e);
				}
				currentPivotEntriesIndex++;
			}
		}
	}

    @Override
    public IsAnythingToDoStatus isAnythingToDo() {
    	if (this.isAnythingToDo.equals(IsAnythingToDoStatus.UNDEFINED)) {
    		this.isAnythingToDo = IsAnythingToDoStatus.NO;
    		try {
    			org.jdom2.Document inputFileXMLDocument = JobHelper.parse(this.fichierPivot);
    			List<Element> rset = JobHelper.evaluateExpressionForElements(inputFileXMLDocument, "/*/entries/entry");
    			if (rset.size() != 0) {
    				this.isAnythingToDo = IsAnythingToDoStatus.YES;
    			}
    		} catch (AlambicException e) {
    			log.error("Failed to check whether anything has to be done, error : " + e.getMessage());
    		}
    	}
    	return this.isAnythingToDo;
    }

	private void loadVariablesList(final Map<String, List<String>> map) throws AlambicException {
		reloadVariablesList();
		variables.loadFromExtraction(map);
	}

	private void reloadVariablesList() throws AlambicException {
		variables.clearTable();
		if (context.getVariables() != null) {
			variables.loadFromMap(context.getVariables().getHashMap());
		}

		if (pivot.getChild("variables") != null) {
			variables.loadFromXmlNode(pivot.getChild("variables").getChildren());
		}
		// execution des éventuelles fonctions chargées dans la table de
		variables.executeFunctions();
	}

	public SearchControls getContraintes() {
		return contraintes;
	}

	public void setContraintes(final SearchControls contraintes) {
		this.contraintes = contraintes;
	}

	public DirContext getCtx() {
		return ctx;
	}

	public void setCtx(final DirContext ctx) {
		this.ctx = ctx;
	}

}
