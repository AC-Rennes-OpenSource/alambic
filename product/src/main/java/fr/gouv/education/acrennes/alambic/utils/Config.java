package fr.gouv.education.acrennes.alambic.utils;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;

import java.util.Properties;

public class Config {

    private Config() {
        // pas besoin d'instancier cette classe qui ne contient que des méthodes statiques
    }

    private static Properties props = new Properties();

    public static Properties getProperties() {
        return props;
    }

    public static void setProperties(Properties properties) {
        props = properties;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    /**
     * Lecture d'un paramètre depuis l'attribut "attributName" du nœud "source" ou "destination"
     *
     * @param context            contexte applicatif avec variables
     * @param jobDescriptionNode nœud "source" ou "destination" de description du job
     * @param attributeName      nom de l'attribut qui contient la valeur surchargée
     * @param defaultValue       valeur par défaut si attribut absent ou vide
     * @param errorIfMissing     message d'erreur dans AlambicException si valeur non définie
     * @return la valeur numérique lue
     * @throws AlambicException si la valeur définie en configuration ou sur le nœud "source" ou "destination" n'est définie et default value est null
     */
    public static String getPropertyValue(
            final CallableContext context,
            final Element jobDescriptionNode,
            final String attributeName,
            final String defaultValue,
            final String errorIfMissing
    ) throws AlambicException {
        final String propertyValue = jobDescriptionNode.getChildText(attributeName);
        if (StringUtils.isNotBlank(propertyValue)) {
            return context.resolveString(propertyValue);
        } else if (defaultValue != null) {
            return defaultValue;
        } else {
            throw new AlambicException(errorIfMissing);
        }
    }

    /**
     * Lecture d'un paramètre numérique depuis les variables (chargés à partir du fichier config.properties)
     *
     * @param context     contexte applicatif avec variables
     * @param propertyKey nom de la variable qui contient la valeur par défaut (fichier config.properties)
     * @return la valeur numérique lue
     * @throws AlambicException si la valeur définie en configuration n'est pas correcte
     */
    public static Integer getNumericPropertyValue(final CallableContext context, final String propertyKey) throws AlambicException {
        final String defaultPropertyValue = getDefaultPropertyValue(context, propertyKey);
        return toPositiveInteger(propertyKey, defaultPropertyValue);
    }

    /**
     * Lecture d'un paramètre numérique depuis les variables (chargés à partir du fichier config.properties)
     * ou surchargé par l'attribut "attributName" du nœud "source" ou "destination"
     *
     * @param context            contexte applicatif avec variables
     * @param jobDescriptionNode nœud "source" ou "destination" de description du job
     * @param propertyKey        nom de la variable qui contient la valeur par défaut (fichier config.properties)
     * @param attributeName      nom de l'attribut qui contient la valeur surchargée
     * @return la valeur numérique lue
     * @throws AlambicException si la valeur définie en configuration ou sur le nœud "source" ou "destination" n'est pas correcte
     */
    public static Integer getNumericPropertyValue(final CallableContext context, final Element jobDescriptionNode, final String propertyKey, final String attributeName) throws AlambicException {
        Integer readTimeout = getNumericPropertyValue(context, propertyKey); // valeur par défault
        final String readTimeoutJob = jobDescriptionNode.getAttributeValue(attributeName);
        if (StringUtils.isNotBlank(readTimeoutJob)) {
            readTimeout = toPositiveInteger(attributeName, context.resolveString(readTimeoutJob));
        }
        return readTimeout;
    }

    /**
     * Lire la valeur par défaut depuis les variables (chargés à partir du fichier config.properties)
     * @param context contexte applicatif avec variables
     * @param propertyKey nom de la variable qui contient la valeur par défaut (fichier config.properties)
     * @return la valeur lue
     * @throws AlambicException si la valeur définie en configuration n'est pas correcte
     */
    private static String getDefaultPropertyValue(final CallableContext context, final String propertyKey) throws AlambicException {
        final String variableValue = context.getVariables().getHashMap().get(propertyKey);
        if (StringUtils.isNotBlank(variableValue)) {
            return context.resolveString(variableValue);
        }
        return null;
    }

    /**
     * Retourne la représentation entière pour la valeur donnée en tant que chaîne de caractères, mais uniquement si elle est entière et positive
     * @param field identifiant de la valeur
     * @param value valeur en tant que chaîne de caractères
     * @return valeur entière
     * @throws AlambicException si la valeur n'est pas numérique ou elle est négative
     */
    private static Integer toPositiveInteger(final String field, final String value) throws AlambicException {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            int intValue = Integer.parseInt(value);
            if (intValue < 0) {
                throw new AlambicException("la valeur de " + field + " est négative");
            } else {
                return intValue;
            }
        } catch (NumberFormatException e) {
            throw new AlambicException("la valeur de " + field + " n'est pas numérique");
        }
    }
}
