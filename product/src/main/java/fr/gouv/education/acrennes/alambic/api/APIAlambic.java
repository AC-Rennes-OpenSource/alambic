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
package fr.gouv.education.acrennes.alambic.api;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TARGET_SERVER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.Config;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.persistence.config.TargetServer;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.ExecutorFactory;
import fr.gouv.education.acrennes.alambic.jobs.Jobs;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.utils.Variables;

public class APIAlambic implements IAPIAlambic {

    private static final class AlambicVariables {
        static String ALAMBIC_ADDONPATH = "ALAMBIC_ADDONPATH";
        static String ALAMBIC_ADDON_OUTPUTPATH = "ALAMBIC_ADDON_OUTPUTPATH";

        private AlambicVariables() {
            throw new AssertionError("Classe utilitaire, ne pas instancier");
        }
    }

    private static final String DEFAULT_CONF_FOLDER = "conf";
    private static final String CONFIG_FILE = DEFAULT_CONF_FOLDER + "/config.properties";
    private static final String REPOSITORY_VARIABLES = "repository.variables";
    private static final String REPOSITORY_PATH = "repository.path";
    private static final String PERSISTENCE_UNIT = "PRODUCTION_PERSISTENCE_UNIT";
    private static final String DEFAULT_RUN_ID = "DEFAULT_CONTEXT";

    private static String repositoryPath = "";
    private static String executionPath = "./";
    private static Properties properties;
    private static final Log log = LogFactory.getLog(APIAlambic.class);

    private final RunIdGenerator runIdGenerator;
    private File tempProcessFile;

    /**
     * Constructeur par défaut.
     */
    public APIAlambic() {
        this(null);
    }

    /**
     * Constructeur permettant une utilisation multithreadée du moteur Alambic. Si plusieurs instances de {@link
     * APIAlambic} sont créées, il faut qu'elles partagent une même instance de {@code runIdGenerator}.
     *
     * @param runIdGenerator le générateur d'identifiants, utilisés pour isolation des exécutions ; si {@code null},
     *                       l'appel de ce constructeur est équivalent à l'appel du constructeur par défaut
     */
    public APIAlambic(final RunIdGenerator runIdGenerator) {
        super();
        this.runIdGenerator = runIdGenerator;
        this.tempProcessFile = null;
    }

    public static void init(final String ep) throws IOException, AlambicException {
    	init(ep, null);
    }
    
    public static void init(final String ep, final String threadCount) throws IOException, AlambicException {
        executionPath = ep;
        if (!executionPath.matches(".+/$")) {
            executionPath = executionPath.concat("/");
        }

        // General configuration
        properties = new Properties();
        properties.load(new FileInputStream(executionPath.concat(CONFIG_FILE)));
        
        if (StringUtils.isNotBlank(threadCount)) {
        	properties.setProperty(ExecutorFactory.THREAD_POOL_SIZE, threadCount);
        }

        // Initialize the config object to access properties later
        Config.setProperties(properties);
        
        // Initialize the persistence unit
        Map<String, String> puProperties = new HashMap<>();
        puProperties.put(JDBC_DRIVER, properties.getProperty(CallableContext.ETL_CFG_JDBC_DRIVER));
        puProperties.put(JDBC_URL, properties.getProperty(CallableContext.ETL_CFG_JDBC_URL));
        puProperties.put(JDBC_USER, properties.getProperty(CallableContext.ETL_CFG_JDBC_LOGIN));
        puProperties.put(JDBC_PASSWORD, properties.getProperty(CallableContext.ETL_CFG_JDBC_PASSWORD));
        puProperties.put(TARGET_SERVER, TargetServer.None);
        EntityManagerHelper.getInstance(PERSISTENCE_UNIT, puProperties);
        // this two lines aim to make JPA create all the tables as defined by the persistence unit.
        EntityManager em = EntityManagerHelper.getEntityManager();
        em.close();

        repositoryPath = properties.getProperty(REPOSITORY_PATH);
        if (StringUtils.isNotBlank(repositoryPath)) {
            if (!repositoryPath.matches(".+/$")) {
                repositoryPath = repositoryPath.concat("/");
            }
        } else {
            throw new AlambicException("La propriété '" + REPOSITORY_PATH + "' est absente de la configuration 'config.properties'");
        }

        // Initialize the executor factory (multi-threading engine)
        ExecutorFactory.initialize(properties);
    }

    public static void close() throws AlambicException {
    	// Close the random generator service
    	RandomGeneratorService.close();
    	
        // Close multi-threading factory
        ExecutorFactory.close();

        // Close persistence unit
        EntityManagerHelper.close();
    }

    private boolean isRunnableJob(final String job) {
        boolean isRunnable = false;

        try {
            File jobFile = new File(job); // path relative to current directory
            String jobAbsolutePath = jobFile.getAbsolutePath();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(jobAbsolutePath.getBytes());
            String jobAbsolutePathHash = Base64.encodeBase64String(md.digest()).replaceAll("/", "-"); // Linux OS file name compliancy
            String tempFileName = executionPath.concat("/.process-" + jobAbsolutePathHash);
            this.tempProcessFile = new File(tempFileName);
            if (!this.tempProcessFile.exists()) {
                this.tempProcessFile.createNewFile();
                isRunnable = true;
            } else {
                log.error("A similar process is already running (job file '" + jobAbsolutePath + "', locking file is: " + this.tempProcessFile.getCanonicalPath() + ")");
                this.tempProcessFile = null; // so that this instance of job doesn't delete a process file created by its previous iteration
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to control runnable status of the job, error : " + e.getMessage(), e);
        }

        return isRunnable;
    }

    @Override
    public void closeInstance() {
        if (null != this.tempProcessFile) {
            this.tempProcessFile.delete();
            this.tempProcessFile = null;
        }
    }

    @Override
    public List<Future<ActivityMBean>> run(
            String addonName,
            String jobFileName,
            boolean isReadOnlyJob,
            List<String> tasksList,
            Map<String, String> parameters
    ) {
        List<Future<ActivityMBean>> jobsFuturelist = new ArrayList<>();

        if (StringUtils.isNotBlank(jobFileName)) {
            if (!jobFileName.matches(".+\\.xml$")) {
                jobFileName = jobFileName.concat(".xml");
            }

            String addonJobFilePath = (StringUtils.isNotBlank(addonName)) ? String.format("%s%s/%s", repositoryPath, addonName, jobFileName) : String.format("%s%s", repositoryPath, jobFileName);

            // Exécuter le job s'il est défini en mode "read only" ou si aucun autre processus ETL correspondant est en cours
            if (isReadOnlyJob || isRunnableJob(addonJobFilePath)) {
                try {
                    // Chargement de la liste des variables statiques d'exécution (fichier 'variables.xml')
                    InputSource fVariablesXml = new InputSource(properties.getProperty(REPOSITORY_VARIABLES));
                    SAXBuilder saxBuilder = new SAXBuilder();
                    Element racineVariables = saxBuilder.build(fVariablesXml).getRootElement();

                    Variables variables = new Variables();
                    Element varEntries = racineVariables.getChild("variables");
                    if (varEntries != null) {
                        variables.loadFromXmlNode(varEntries.getChildren());
                    }

                    final String runId = initAddonContext(variables, addonName);

                    // Chargement des paramètres d'exécution
                    if (null != parameters) {
                        variables.loadFromMap(parameters);
                    }

                    // Instantiation et exécution
                    Jobs jobs = new Jobs(executionPath, addonJobFilePath, variables, properties);
                    if (tasksList != null && !tasksList.isEmpty()) {
                        jobsFuturelist = jobs.executeJobList(tasksList, runId);
                    } else {
                        log.info("Execution de toutes les taches");
                        jobsFuturelist = jobs.executeAllJobs(runId);
                    }
                } catch (JDOMException e) {
                    log.error("Lecture XML " + e.getMessage(), e);
                } catch (IOException e) {
                    log.error("Lecture/ecriture de fichier " + e.getMessage(), e);
                } catch (AlambicException e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.error("Job non executable :" + jobFileName);
            }
        } else {
            log.error("Aucun fichier de définition de job de précisé");
        }

        return jobsFuturelist;
    }

    private String initAddonContext(final Variables variables, final String addonName) throws IOException {

        final String runId = (runIdGenerator == null ? DEFAULT_RUN_ID : runIdGenerator.nextId());
        if (log.isDebugEnabled()) {
            log.debug("Identifiant d'exécution généré : " + runId);
        }

        if (StringUtils.isNotBlank(addonName)) {
        	final String addonPath = repositoryPath.concat(addonName);
        	variables.put(AlambicVariables.ALAMBIC_ADDONPATH, addonPath);
        	final Path outputPath = Paths.get(addonPath, "output", runId);
        	variables.put(AlambicVariables.ALAMBIC_ADDON_OUTPUTPATH, outputPath.toString());
        	
        	if (!Files.exists(outputPath)) {
        		// TODO les permissions ne semblent pas appliquées sur le dossier créé ?
        		final Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
        		Files.createDirectory(outputPath, PosixFilePermissions.asFileAttribute(permissions));
        	}
        }

        return runId;
    }

}
