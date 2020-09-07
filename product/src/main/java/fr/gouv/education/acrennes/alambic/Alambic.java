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
package fr.gouv.education.acrennes.alambic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.gouv.education.acrennes.alambic.api.APIAlambic;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;

public class Alambic {

	private static final Log log = LogFactory.getLog(Alambic.class);

	public static void main(final String[] args) throws InterruptedException, AlambicException {
		String jobFileName = "";
		Map<String, String> jobParameters = null;
		final List<String> listeTaches = new ArrayList<>();
		String executionPath = "./";
		String threadCount = null;
		boolean executeAllJobs = false;
		
		// Traitement des arguments
		for (int i = 0; i < args.length; i++) {
			System.out.println("Arguments = " + args[i]);
			final String arg = args[i].toLowerCase();
			
			// Chemin d'exécution
			if (arg.startsWith("--execution-path") || arg.startsWith("--ep")) {
				executionPath = args[i].substring(args[i].indexOf("=") + 1).trim();
			}
			// Liste des tâches
			else if (arg.startsWith("--execute-list") || arg.startsWith("-el")) {
				final String jobsListParameter = args[i].substring(args[i].indexOf("=") + 1).trim();
				if (StringUtils.isNotBlank(jobsListParameter)) {
					listeTaches.addAll(Arrays.asList(jobsListParameter.split("[ :]")));
				}
			}
			// Nom du fichier de définition du(es) job(s)
			else if (arg.startsWith("--jobs-repository") || arg.startsWith("-j")) {
				jobFileName = args[i].substring(args[i].indexOf("=") + 1).trim();
			}
			// Booléen pour indiquer qu'il faut exécuter l'ensemble des jobs d'un fichier
			else if (arg.startsWith("--execute-all") || arg.startsWith("-ea")) {
				executeAllJobs = true;
				listeTaches.clear();
			}
			// Nombre de threads à utiliser (prévaut sur le fichier de configuration)
			else if (arg.startsWith("--thread-count") || arg.startsWith("-tc")) {
				threadCount = args[i].substring(args[i].indexOf("=") + 1).trim();
			}
			// Paramètres passés au(x) job(s)
			else if (arg.startsWith("--params")	|| arg.startsWith("-p")) {
				final String p = args[i].substring(args[i].indexOf("=") + 1).trim();
				if (StringUtils.isNotBlank(p)) {
					System.out.println("Paramètres = " + p);
					jobParameters = new HashMap<>();
					String[] pList = p.split(" ");
					for (int index=0; index < pList.length; index++) {
						jobParameters.put("c".concat(String.valueOf(index+1)), pList[index]);
					}
					if (jobParameters.containsValue("debug_mode")) {
						System.out.println("Debug mode is enabled : remote application connection is available.");
					}
				}
			}
			// Aide
			else if (arg.startsWith("--help") || arg.startsWith("-h")) {
				help();
				System.exit(0);
			}
			// Gestion des erreurs
			else {
				System.out.println("Paramètre [" + arg + "] inconnu. Vérifiez la syntaxe.");
				help();
				System.exit(0);
			}
		}
		
		// Exécution par instanciation de l'API java
		if (executeAllJobs || ! listeTaches.isEmpty()) {
			APIAlambic apiInstance = null;
			try {
				// Initialisation de l'API (librairie)
				APIAlambic.init(executionPath, threadCount);
				apiInstance = new APIAlambic();
				
				// Exécution du(es) job(s)
				List<Future<ActivityMBean>> jobsFuturelist = apiInstance.run(null, jobFileName, false, listeTaches, jobParameters);
				
				// Attente de la fin de l'exécution du(es) job(s) et clôture de l'API
				for (final Future<ActivityMBean> jobFuture : jobsFuturelist) {
					ActivityMBean future = jobFuture.get(); // pas d'exploitation du résultat retourné (accessible via l'instance ActivityMBean)
					log.debug("Got the returned future : " + future);
				}
			} catch (ExecutionException | IOException | AlambicException e) {
				System.out.println("Erreur détectée pendant l'exécution du job, cause : " + e.getMessage());
			} finally {
				// Clôture de l'instance
				if (null != apiInstance) {
					apiInstance.closeInstance();
				}

				// Clôture de l'API (librairie)
				APIAlambic.close();
			}
		} else {
			System.out.println("Vous n'avez pas précisé de tâches à executer.");
		}

		System.exit(0);
	}

	private static void help() {
		System.out.println("" +
				"-j  (--jobs-repository) pour préciser le nom du fichier de jobs. Ex : -j=job-users.xml" +
				"\n-ea (--execute-all) pour lancer tous les jobs du fichier" +
				"\n-el (--execute-list) pour lancer un liste de job. Ex : -el=job1:job2:job4" +
				"\n-p  (--params) pour passer charger des valeurs dans la liste des variables. Ex : -p=\"oadam arupin mallain5 ssimenel\"");
	}

}
