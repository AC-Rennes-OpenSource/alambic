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
package fr.gouv.education.acrennes.alambic.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.freemarker.AlambicObjectWrapper;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.SqlToStateBase;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.security.CipherHelper;
import fr.gouv.education.acrennes.alambic.security.CipherKeyStore;
import fr.gouv.education.acrennes.alambic.utils.geo.GeoConvert;
import fr.gouv.education.acrennes.alambic.utils.geo.GeoConvertException;
import fr.gouv.education.acrennes.alambic.utils.lambert.Lambert;
import fr.gouv.education.acrennes.alambic.utils.lambert.LambertPoint;
import fr.gouv.education.acrennes.alambic.utils.lambert.LambertZone;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sis.geometry.DirectPosition2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class Functions {

    private static final Log LOG = LogFactory.getLog(Functions.class);
    private static final String UNICITY_PATTERN_COMPLIANCY = "(candidate=\"([^\"\\|]+)\"(,login=\"([^\"\\|]+)\")?(,password=\"([^\"\\|]+)\")?\\|search=)?((ldap:\\/\\/.+)\\?\\?sub\\?.*\\([^=\\(\\)]+=[^=\\(\\)]*\\*[^=\\(\\)]*\\).*)";
    private static final String UNICITY_PATTERN_TOKEN = "(\\(([^=\\(\\)]+)=([^=\\(\\)]*\\*[^=\\(\\)]*)\\))";
    private static final String CIPHER_PATTERN = "([^,=]+)=([^,]+)";
    private static final String STRING_FORMAT_PATTERN = "([^;=]+)=([^;]+)";
    private static Configuration cfg;
    private static Functions instance = null;

    private static GeoConvert geoConvertInstance;

    public static synchronized GeoConvert getGeoConvert() {
        /** Est définit une seule fois, car est long à instancier */
        if (geoConvertInstance == null) {
            geoConvertInstance = new GeoConvert();
        }
        return geoConvertInstance;
    }

    private static enum COMPUTE_DATE_OPERAND_FIELD {

        DAY(Calendar.DATE),
        MONTH(Calendar.MONTH),
        YEAR(Calendar.YEAR);

        private final int field;

        private COMPUTE_DATE_OPERAND_FIELD(final int fieldCode) {
            field = fieldCode;
        }

        private int getValue() {
            return field;
        }

    }

    private final Pattern functionPattern;

    private Functions() {
        cfg = new Configuration(Constants.FREEMARKER_VERSION);
        cfg.setURLEscapingCharset(Charsets.UTF_8.toString()); // to allow URL escaping
        cfg.setObjectWrapper(new AlambicObjectWrapper());
        functionPattern = Pattern.compile("\\(([a-zA-Z\\.0-9 ]+)([ ]*mem='([^\\ ']*)?'[ ]*)?\\)(.+?)\\(/\\1\\)");
    }

    public static Functions getInstance() {
        if (null == instance) {
            instance = new Functions();
        }

        return instance;
    }


    private String executeFuntion(final String fonction, final String param, final CallStack callStack) throws AlambicException {
        if ("PRENOM".equals(fonction)) {
            return fonctionPrenom(param);
        }
        if ("NOM".equals(fonction)) {
            return fonctionNom(param);
        }
        if ("SqlDate".equals(fonction)) {
            return fonctionSqlDate();
        }
        if ("LOGIN".equals(fonction)) {
            return fonctionLogin(param);
        }
        if ("B64SHA1".equals(fonction)) {
            try {
                return HashString.base64Sha(String.format("plaintext=%s,algorithm=SHA", param));
            } catch (final AlambicException e) {
                LOG.error("Fonction B64SHA1 : " + e.getMessage(), e);
            }
        }
        if ("B64SHAx".equals(fonction)) {
            try {
                return HashString.base64Sha(param);
            } catch (final AlambicException e) {
                LOG.error("Fonction B64SHAx : " + e.getMessage(), e);
            }
        }
        if ("UUID".equals(fonction)) {
            return UUID.randomUUID().toString();
        }
        if ("2DIGIT".equals(fonction)) {
            return fonction2digit(param);
        }
        if ("NOW".equals(fonction)) {
            return fonctionNow(param);
        }
        if ("FORMAT".equals(fonction)) {
            return fonctionFormat(param);
        }
        if ("STRINGFORMAT".equals(fonction)) {
            return stringFormat(param);
        }
        if ("COPY".equals(fonction)) {
            return fonctionCopy(param);
        }
        if ("FORMATSQLIN".equals(fonction)) {
            return fonctionFormatSqlIn(param);
        }
        if ("LDAPSEARCH".equalsIgnoreCase(fonction)) {
            fonctionLdapSearch(param);
        }

        if ("FrEduRne.Extract.Rne".equalsIgnoreCase(fonction)) {
            return fonctionFrEduRneExtractRne(param);
        }
        if ("FrEduRne.Extract.Fonction".equalsIgnoreCase(fonction)) {
            return fonctionFrEduRneExtractFonction(param);
        }
        if ("sql.count".equalsIgnoreCase(fonction)) {
            try {
                return fonctionSqlCount(param);
            } catch (final SQLException e) {
                LOG.error("Fonction fonctionSqlCount : ", e);
            } catch (final ClassNotFoundException e) {
                LOG.error("Fonction fonctionSqlCount : ", e);
            }
        }

        if ("lambert.to.gps.n".equalsIgnoreCase(fonction)) {
            return fonctionLambertGpsN(param);
        }

        try {
            // Référentiel géographique à déterminer avec le code du département :
            if ("coordos.to.gps".equalsIgnoreCase(fonction)) {
                return toWgs84String(param);
            }

            // La Réunion
            if ("utm40s.to.gps".equalsIgnoreCase(fonction)) {
                return toWgs84String(param, getGeoConvert().getRgr92ToWgs84());
            }
            // Guadeloupe
            if ("utm20n.to.gps".equalsIgnoreCase(fonction)) {
                return toWgs84String(param, getGeoConvert().getGuadeloupeToWgs84());
            }
            // Guyane - RGFG95 - UTM Nord fuseau 22, aussi référencé sous EPSG:2972
            if ("utm22n.to.gps".equalsIgnoreCase(fonction)) {
                return toWgs84String(param, getGeoConvert().getGuyaneToWgs84());
            }
            // Mayotte - UTM Sud fuseau 38
            if ("utm38s.to.gps".equalsIgnoreCase(fonction)) {
                return toWgs84String(param, getGeoConvert().getMayotteToWgs84());
            }
            // Saint Pierre et Miquelon - UTM Nord fuseau 21
            if ("utm21n.to.gps".equalsIgnoreCase(fonction)) {
                return toWgs84String(param, getGeoConvert().getStPierreEtMiquelonToWgs84());
            }
            // France métropolitaine
            if ("lambert93.to.gps".equalsIgnoreCase(fonction)) {
                return toWgs84String(param, getGeoConvert().getLambert93ToWgs84());
            }
        } catch (final Exception ex) {
            throw new GeoConvertException("erreur à la conversion des coordonnées géographiques=" + param, ex);
        }
        if ("attr.count".equalsIgnoreCase(fonction)) {
            try {
                return fonctionLdapAttrCount(param);
            } catch (final NamingException e) {
                LOG.error("Fonction fonctionLdapAttrCount : ", e);
            }
        }
        if ("property".equalsIgnoreCase(fonction)) {
            try {
                return fonctionProperty(param);
            } catch (final FileNotFoundException e) {
                LOG.error("Fonction property, fichier non trouvé : ", e);

            } catch (final IOException e) {
                LOG.error("Fonction property, acces fichier : ", e);
            }
        }
        if ("PART".equals(fonction)) {
            return fonctionPart(param);
        }
        if ("RDN".equals(fonction)) {
            return rdn(param);
        }
        if ("UNICITY".equals(fonction)) {
            return unicity(param, callStack);
        }
        if ("CIPHER".equals(fonction)) {
            return cipher(param);
        }
        if ("TEMPLATE".equals(fonction)) {
            return template(param);
        }
        if ("INCREMENT".equals(fonction)) {
            return increment(param, callStack);
        }
        if ("DECREMENT".equals(fonction)) {
            return decrement(param, callStack);
        }
        if ("PASSWORD".equals(fonction)) {
            return password(param, callStack);
        }
        if ("ADD".equals(fonction)) {
            return addNumbers(param);
        }
        if ("COMPUTEDATE".equals(fonction)) {
            return computeDate(param);
        }
        if ("SALT".equals(fonction)) {
            return generateSalt(param);
        }
        return null; // Aucune fonction exécutée, rien à retourner
    }

    private String toWgs84String(final String param, final CoordinateOperation op) throws TransformException {
        final DirectPosition2D position = paramToPosition(param);
        if (position == null) {
            return "-1";
        }
        return toWsg84String(op, position);
    }

    private String toWsg84String(final CoordinateOperation op, final DirectPosition2D position) throws TransformException {
        final DirectPosition wsg84Coordinates = op.getMathTransform().transform(position, null);
        return wsg84Coordinates.getCoordinate()[0] + "," + wsg84Coordinates.getCoordinate()[1];
    }

    private String toWgs84String(final String param) throws TransformException {
        final String[] splitted = param.split(",");
        if (splitted.length != 3) {
            LOG.error("le paramètre fourni doit être de forme suivante : departement,X,Y - Exemple: 022,644464646.5,64644.001");
            LOG.error("le paramètre fourni était : " + param);
            return "-1";
        }
        final DirectPosition2D directPosition2D = new DirectPosition2D(Double.parseDouble(splitted[1]), Double.parseDouble(splitted[2]));
        final String departement = splitted[0];
        final CoordinateOperation op = getGeoConvert().getOperationFromCodeDepartement(departement);

        return toWsg84String(op, directPosition2D);
    }

    /**
     * @param param coordonnées X,Y
     * @return null si le param entrée n'est pas correctement formaté
     */
    private DirectPosition2D paramToPosition(String param) {
        if (!param.contains(",")) {
            return null;
        }
        final String v[] = param.split(",");
        if (v.length != 2) {
            return null;
        }
        return new DirectPosition2D(Double.parseDouble(v[0]), Double.parseDouble(v[1]));
    }

    private String fonctionLambertGpsN(final String param) {
        if (!param.contains("-p-")) {
            return "-1";
        }
        final String v[] = param.split("-p-");
        if (v.length != 2) {
            return "-1";
        }
        final int X = Integer.parseInt(v[0]);
        final int Y = Integer.parseInt(v[1]);
        final LambertPoint pt = Lambert.convertToWGS84Deg(X, Y, LambertZone.Lambert93);
        return new Double(pt.getY()).toString();
    }

    private String rdn(final String param) {
        if ((param == null) || param.isEmpty()) {
            return param;
        }
        final String rdn = param.substring(param.indexOf('=') + 1, param.indexOf(","));
        return rdn;
    }

    private String unicity(final String searchString, final CallStack callStack) throws AlambicException {
        String uniqueValue = "";
        String shortestCommonValuePattern = "";
        NamingEnumeration<SearchResult> resultSet = null;

        try {
            // Verify the LDAP search string fits the URL format
            final Pattern unicityPattern = Pattern.compile(UNICITY_PATTERN_COMPLIANCY);
            final Matcher unicityMatcher = unicityPattern.matcher(searchString);
            if (unicityMatcher.find()) {
                final Pattern tokensPattern = Pattern.compile(UNICITY_PATTERN_TOKEN);
                final Matcher tokensMatcher = tokensPattern.matcher(unicityMatcher.group(7));
                final Set<String> reqAttributes = new LinkedHashSet<>(); // use a Set to avoid duplicated values
                // validate all patterns in "searchString" and keep the shortest common one, also keep an ordered set of all attribute names in "reqAttributes"
                while (tokensMatcher.find()) {
                    final String attributeName = tokensMatcher.group(2);
                    final String currentValuePattern = tokensMatcher.group(3);
                    reqAttributes.add(attributeName);
                    if (StringUtils.isNotBlank(shortestCommonValuePattern) && !shortestCommonValuePattern.startsWith(currentValuePattern) && !currentValuePattern.startsWith(shortestCommonValuePattern)) {
                        LOG.error("All unicity patterns must share a common root (prefix) within the request. This root usually ends with a star (*)." +
                                "But found two unrelated patterns: '" + shortestCommonValuePattern + "' and '" + attributeName + "=" + currentValuePattern + "' in the request '" + searchString + "'. " +
                                "Pattern '" + currentValuePattern + "' defined for attribute '" + attributeName + "', will be used for LDAP request, but ignored for unique value generation.");
                        continue; // Read the next pattern
                    }
                    // If pattern is not yet defined or a shorter pattern is found, define current pattern as shortestCommonValuePattern
                    if (StringUtils.isBlank(shortestCommonValuePattern) || currentValuePattern.length() < shortestCommonValuePattern.length()) {
                        shortestCommonValuePattern = currentValuePattern;
                    }
                }

                final String generatorValue = unicityMatcher.group(2);
                uniqueValue = shortestCommonValuePattern.replace("*", (StringUtils.isNotBlank(generatorValue) ? generatorValue : ""));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Check unicity criteria on the attribut(s) '" + reqAttributes + "' (candidate value for pattern '" + shortestCommonValuePattern + "' is '" + uniqueValue + "'");
                }

                if (StringUtils.isNotBlank(uniqueValue)) {
                    // Search occurrences within LDAP (sub tree scope is used)
                    final SearchControls searchControls = new SearchControls();
                    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                    final Properties environment = new Properties(); // using Properties allows to ensure the configuration values are typed as String & removes warning java:S1149
                    if (StringUtils.isNotBlank(unicityMatcher.group(5)) && StringUtils.isNotBlank(unicityMatcher.group(6))) {
                        LOG.debug("Using principal " + unicityMatcher.group(4));
                        environment.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                        environment.setProperty(Context.PROVIDER_URL, unicityMatcher.group(8));
                        environment.setProperty(Context.SECURITY_PRINCIPAL, unicityMatcher.group(4));
                        environment.setProperty(Context.SECURITY_CREDENTIALS, unicityMatcher.group(6));
                    } else {
                        LOG.debug("No credential or password provided");
                    }
                    LOG.debug("LDAP search query: " + unicityMatcher.group(7));
                    resultSet = getDirContext(environment).search(unicityMatcher.group(7), "", searchControls);

                    if ((null != resultSet) && resultSet.hasMore()) {
                        // Check whether an inner function was used to build the candidate value
                        final CallStack ifc = (!callStack.getStack().isEmpty()) ? callStack.getStack().get(0) : null;
                        if ((null != ifc) && StringUtils.isBlank(unicityMatcher.group(1))) {
                            // Ask the inner function to give a new candidate value
                            final String newCandidate = searchFunctions(ifc.getFulltext(), new HashMap<>(), callStack);
                            uniqueValue = unicity(searchString.replace(uniqueValue, newCandidate), callStack);
                        } else {
                            // No inner function was used to build the candidate value: compute the new unique value via an
                            // incremental suffix and based on the previous search result
                            // First retrieve all the already existing values in "reqAttributes" fields of the LDAP resultSet, including the multi-values ones
                            final Set<String> existingValuesInLowerCase = new HashSet<>(); // use a Set to avoid duplicated values
                            while (resultSet.hasMore()) {
                                final SearchResult result = resultSet.next();
                                LOG.debug("The following LDAP entity matches the search string '" + searchString + "', entity: " + result.getName());
                                final Attributes attributes = result.getAttributes();
                                for (final String attribute : reqAttributes) {
                                    if (null != attributes.get(attribute)) {
                                        final NamingEnumeration<?> values = attributes.get(attribute).getAll();
                                        while (values.hasMore()) {
                                            final Object value = values.next();
                                            if (value instanceof String) {
                                                existingValuesInLowerCase.add(((String) value).toLowerCase());
                                            }
                                        }
                                    }
                                }
                            }

                            // Compute the unique value, loop at most "existingValuesInLowerCase.size()" times, as the worst case occurs when all candidate values already exist
                            for (int i = 1; i <= existingValuesInLowerCase.size(); i++) {
                                // finalUniqueValue is a local constant copy of uniqueValue to allow its use in the following lambda expression
                                final String finalUniqueValue = uniqueValue;
                                // If there is not a value in "existingValuesInLowerCase" that starts with candidate "uniqueValue", so we can use the latter as result
                                if (existingValuesInLowerCase.stream().noneMatch(existingValue -> existingValue.startsWith(finalUniqueValue.toLowerCase()))) {
                                    // Found the unique value
                                    LOG.debug("Found the unique value '" + uniqueValue + "'");
                                    break;
                                } else {
                                    // build the new candidate value (based-on either the generator passed-in parameters or an index)
                                    String newCandidate = String.valueOf(i);
                                    if (null != ifc) {
                                        // Ask the inner function to give a new candidate value
                                        newCandidate = searchFunctions(ifc.getFulltext(), new HashMap<>(), callStack);
                                    }
                                    // Compute new candidate value
                                    uniqueValue = shortestCommonValuePattern.replace("*", newCandidate);
                                }
                            }
                        }
                    }
                } else {
                    LOG.error("Empty unicity pattern ('*') might be present in the request '" + searchString + "'");
                }
            } else {
                LOG.error("The LDAP search URL ('" + searchString + "') doesn't match URL format RFC 2255 (ex: 'ldap://<host>:<port>/ou=People,o=JNDITutorial??sub?(<attribut name>=<value must contain asterisk * character to specify the possible increment position>)')");
            }
        } catch (final NamingException e) {
            LOG.error("Failed to execute the LDAP search, error: " + e.getMessage(), e);
            throw new AlambicException(e);
        } finally {
            if (null != resultSet) {
                try {
                    resultSet.close();
                } catch (final NamingException e) {
                    LOG.error("Failed to close the LDAP result set, error: " + e.getMessage(), e);
                }
            }
        }

        return uniqueValue;
    }

    private String cipher(final String params) {
        final String FORMAT_ERROR = "The CIPHER function parameters ('" + params + "') dont match the pattern 'mode=<eg:ENCRYPT_MODE|DECRYPT_MODE>,"
                + "algorithm=<eg:RSA,AES>,path=<keystore file absolute path>,"
                + "ksPwd=<keystore password>,"
                + "[ksType=<keystore type (eg: JCEKS or default if missing)>],"
                + "alias=<key alias>,"
                + "[keyPwd=<key password (might be defferent from the keystore's one)>],"
                + "[keyType=<key type (eg: secret, public, private)>],"
                + "plaintext=<the text to cipher>'";

        String result = "";

        try {
            // Verify the parameters fit the pattern
            final Map<String, String> parameters = new HashMap<>();
            final Pattern pattern = Pattern.compile(CIPHER_PATTERN);
            final Matcher matcher = pattern.matcher(params);
            while (matcher.find()) {
                parameters.put(matcher.group(1), matcher.group(2));
            }

            if (StringUtils.isBlank(parameters.get("mode")) ||
                    StringUtils.isBlank(parameters.get("algorithm")) ||
                    StringUtils.isBlank(parameters.get("path")) ||
                    StringUtils.isBlank(parameters.get("ksPwd")) ||
                    StringUtils.isBlank(parameters.get("alias")) ||
                    StringUtils.isBlank(parameters.get("plaintext"))) {
                LOG.error(FORMAT_ERROR);
            } else {
                // execute the ciphering operation
                final CipherKeyStore keystore = new CipherKeyStore(parameters.get("path"),
                        (StringUtils.isNotBlank(parameters.get("ksType"))) ? CipherKeyStore.KEYSTORE_TYPE.valueOf(parameters.get("ksType")) : CipherKeyStore.KEYSTORE_TYPE.DEFAULT,
                        parameters.get("ksPwd"));

                final CipherHelper cipher = new CipherHelper(parameters.get("algorithm"),
                        keystore, parameters.get("alias"),
                        (StringUtils.isNotBlank(parameters.get("keyPwd"))) ? parameters.get("keyPwd") : parameters.get("ksPwd"),
                        (StringUtils.isNotBlank(parameters.get("keyType"))) ? parameters.get("keyType") : "public");

                if (CipherHelper.CIPHER_MODE.ENCRYPT_MODE.toString().equals(parameters.get("mode"))) {
                    final byte[] cipheredBytes = cipher.execute(CipherHelper.CIPHER_MODE.ENCRYPT_MODE, parameters.get("plaintext"));
                    try {
                        result = Base64.encodeBase64String(cipheredBytes);
                    } finally {
                        cipher.close();
                    }
                } else {
                    final byte[] cipheredBytes = Base64.decodeBase64(parameters.get("plaintext"));
                    try {
                        final byte[] plainBytes = cipher.execute(CipherHelper.CIPHER_MODE.DECRYPT_MODE, cipheredBytes);
                        result = new String(plainBytes);
                    } finally {
                        cipher.close();
                    }
                }
            }
        } catch (final AlambicException e) {
            LOG.error("Failed to cipher, error: ", e);
        }

        return result;
    }

    private String template(final String params) {
        String templatedContent = "";

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, String> ctx = mapper.readValue(params, new TypeReference<Map<String, String>>() {
            });
            final String templatePath = ctx.get("path");
            final String templateDir = templatePath.substring(0, templatePath.lastIndexOf("/"));
            final String templateFile = templatePath.substring(templatePath.lastIndexOf("/") + 1);
            cfg.setDirectoryForTemplateLoading(new File(templateDir));

            final OutputStream outputStream = new ByteArrayOutputStream();
            final Writer out = new OutputStreamWriter(outputStream);
            cfg.getTemplate(templateFile).process(ctx, out);
            out.flush();
            templatedContent = outputStream.toString();
        } catch (final IOException e) {
            LOG.error("Exception :", e);
        } catch (final TemplateException e) {
            LOG.error("Exception :", e);
        }

        return templatedContent;
    }

    private String increment(final String params, final CallStack callStack) {
        String value = params;
        if (!callStack.getCtx().containsKey("index")) {
            callStack.getCtx().put("index", params);
        } else {
            value = String.valueOf(Integer.valueOf(callStack.getCtx().get("index")) + 1);
            callStack.getCtx().put("index", value);
        }
        return value;
    }

    private String decrement(final String params, final CallStack callStack) {
        String value = params;
        if (!callStack.getCtx().containsKey("index")) {
            callStack.getCtx().put("index", params);
        } else {
            final Integer valueInt = Integer.valueOf(callStack.getCtx().get("index")) - 1;
            value = String.valueOf((0 > valueInt) ? 0 : valueInt);
            callStack.getCtx().put("index", value);
        }
        return value;
    }

    private String password(final String params, final CallStack callStack) {
        final String FORMAT_ERROR = "The PASSWORD function parameters ('" + params + "') dont match the pattern 'length=<the password length>,"
                + "symbols=<the allowed dictionary symbols eg:LETTER_MAJ, LETTER_MIN, DIGIT, SPECIAL>,"
                + "reuse=<do reuse the same previously provided password from audit logs based-on the blur identifier. e.g. true|false>,"
                + "blurid=<the blur identifier to associate the password(s) to so that it can be retreived>,"
                + "processId=<the process identifier attached to this request>,"
                + "scope=<scope of password unicity resolution. e.g. NONE, PROCESS, PROCESS_ALL>'";

        /** Exemple :
         * "(PASSWORD){\"length\":8,\"symbols\":\"LETTER_MAJ,LETTER_MIN,DIGIT,SPECIAL\",\"processId\":\"EXTACA\",\"scope\":\"PROCESS\",\"reuse\":\"true\",\"blurid\":\"John.Doe@noo.fr\"}(/PASSWORD)"
         */

        String password = "";
        final ObjectMapper mapper = new ObjectMapper();

        try {
            // Verify the parameters fit the pattern
            final Map<String, Object> queryMap = mapper.readValue(params, new TypeReference<Map<String, Object>>() {
            });
            if ((null != (queryMap.get("length")))
                    && (null != queryMap.get("symbols"))
                    && StringUtils.isNotBlank((String) queryMap.get("processId"))
                    && StringUtils.isNotBlank((String) queryMap.get("scope"))
                    && ((StringUtils.isNotBlank((String) queryMap.get("reuse")) && StringUtils.isNotBlank((String) queryMap.get("blurid")))
                    || (StringUtils.isBlank((String) queryMap.get("reuse")) && StringUtils.isBlank((String) queryMap.get("blurid"))))) {

                // Add 'count' parameter to 1 as only one password can be requested & build the query for the random generator
                queryMap.put("count", 1);
                final String query = mapper.writeValueAsString(queryMap);

                // Generate a password
                final RandomGenerator generator = RandomGeneratorService.getRandomGenerator(RandomGeneratorService.GENERATOR_TYPE.PASSWORD);
                try {
                    final List<RandomEntity> passwordEntities = generator.getEntities(query, (String) queryMap.get("processId"), RandomGenerator.UNICITY_SCOPE.valueOf((String) queryMap.get("scope")));
                    if ((null != passwordEntities) && !passwordEntities.isEmpty()) {
                        final String entityJson = passwordEntities.get(0).getJson();
                        final Map<String, Object> entityMap = new ObjectMapper().readValue(entityJson, new TypeReference<Map<String, Object>>() {
                        });
                        password = (String) entityMap.get("password");
                    } else {
                        LOG.error("Failed to get random password, cause : random generator returned no-one");
                    }
                } finally {
                    generator.close();
                }
            } else {
                LOG.error(FORMAT_ERROR);
            }
        } catch (AlambicException | IOException e) {
            LOG.error("Failed to get random password, error : " + e.getMessage(), e);
        }

        return password;
    }

    private String addNumbers(final String params) {
        int sum = 0;
        final String[] numbersStg = params.split(";");
        if ((2 == numbersStg.length) && StringUtils.isNotBlank(numbersStg[0]) && StringUtils.isNotBlank(numbersStg[1])) {
            final int number1 = Integer.parseInt(numbersStg[0]);
            final int number2 = Integer.parseInt(numbersStg[1]);
            sum = number1 + number2;
        } else {
            LOG.error("ADD function requires at least two numbers");
        }
        return String.valueOf(sum);
    }

    private String computeDate(final String params) {
        final String FORMAT_ERROR = "The COMPUTEDATE function parameters ('" + params + "') dont match the pattern 'format=<the date format>,"
                + "value=<the date value with respect of the format>,"
                + "operator=<the operation type to perform : PLUS or MINUS>"
                + "uint=<the date field to as first operand of the operation : DAY, MONTH, YEAR. Optional parameter. As default, DAY field.>"
                + "operand=<the second operand to either add or substract>";

        /** Exemple :
         * "(COMPUTEDATE){\"format\":\"dd/MM/yyyy\",\"value\":\"08/03/2018\",\"operator\":\"MINUS\",\"unit\":\"DAY\",\"operand\":\"5\"}(/COMPUTEDATE)"
         */

        String result = "";
        try {
            final Map<String, Object> paramsMap = new ObjectMapper().readValue(params, new TypeReference<Map<String, Object>>() {
            });
            if ((null != paramsMap)
                    && StringUtils.isNotBlank((String) paramsMap.get("format"))
                    && StringUtils.isNotBlank((String) paramsMap.get("value"))
                    && StringUtils.isNotBlank((String) paramsMap.get("operator"))
                    && StringUtils.isNotBlank((String) paramsMap.get("operand"))) {
                final String unit = (StringUtils.isNotBlank((String) paramsMap.get("unit"))) ? (String) paramsMap.get("unit") : "DAY";
                int operand = Integer.parseInt((String) paramsMap.get("operand"));
                operand = ("PLUS".equals(paramsMap.get("operator"))) ? operand : (-1 * operand);

                final DateFormat dateFormat = new SimpleDateFormat((String) paramsMap.get("format"));
                final Date date = dateFormat.parse((String) paramsMap.get("value"));
                final Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(COMPUTE_DATE_OPERAND_FIELD.valueOf(unit).getValue(), operand);
                result = dateFormat.format(c.getTime());
            } else {
                LOG.error(FORMAT_ERROR);
            }
        } catch (ParseException | IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return result;
    }

    private String fonctionFrEduRneExtractRne(final String param) {
        if (param.contains("$")) {
            return param.substring(0, param.indexOf("$"));
        } else {
            return "";
        }
    }

    private String fonctionPart(final String param) {
        if (!param.contains(";")) {
            return "";
        }
        final String values[] = param.split(";");
        if (values.length != 3) {
            return "";
        }

        final String s = values[0];
        final String delimiter = values[1];
        final int part = Integer.parseInt(values[2]);
        final String parts[] = s.split(delimiter);
        if ((part <= 0) || (part > parts.length)) {
            return "";
        }

        return parts[part - 1];
    }

    private void fonctionLdapSearch(final String param) {
        try {
            final DirContext ctx = getDirContext(null);
            final NamingEnumeration<SearchResult> answer = ctx.search(param, "", null);
            try {
                String currentName = "";
                while (answer.hasMore()) {
                    currentName = currentName + answer.next().getNameInNamespace() + ";";
                }
            } finally {
                answer.close();
            }
        } catch (final NamingException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private String fonctionLdapAttrCount(final String param) throws NamingException {
        if (!param.contains(";")) {
            return "-1";
        }
        final String v[] = param.split(";");
        if (v.length != 2) {
            return "-1";
        }
        final String dn = v[0];
        final String attr = v[1];
        String res = "-1";
        try {
            final Hashtable<String, Object> env = new Hashtable<>(11);
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, dn);
            final DirContext ctx = getDirContext(env);
            try {
                if (ctx != null) {
                    final NamingEnumeration<?> values = ctx.getAttributes("").get(attr).getAll();
                    if (values != null) {
                        try {
                            res = String.valueOf(Collections.list(values).size());
                        } finally {
                            values.close();
                        }
                    }
                }
            } finally {
                ctx.close();
            }
        } catch (final Exception e) {
            LOG.error("Exception :", e);
        }
        return res;
    }

    private String fonctionSqlCount(final String param) throws SQLException, ClassNotFoundException {
        if (!param.contains("-param-")) {
            return "-1";
        }
        final String v[] = param.split("-param-");
        // if (v.length!=2)
        // return "-1";
        final String driver = v[0];
        final String uri = v[1];
        final String query = v[2];
        String res = "-1";
        final SqlToStateBase sb = new SqlToStateBase(driver, uri, "NONE");
        try {
            sb.executeQuery(query);
            final int count = sb.getCountResults();
            res = String.valueOf(count);
        } finally {
            sb.close();
        }
        return res;
    }

    private String fonctionProperty(final String param) throws FileNotFoundException, IOException {
        final String v[] = param.split("-param-");
        // if (v.length!=2)
        // return "-1";
        final String file = v[0];
        final String property = v[1];
        String res = "-1";
        final Properties properties = new Properties();
        properties.load(new FileInputStream(file));

        res = properties.getProperty(property);
        if (res == null) {
            res = "-1";
        }

        return res;

    }

    private String fonctionFormatSqlIn(final String param) {
        final String values[] = param.split(";");
        String res = "";
        for (int i = 0; i < values.length; i++) {
            res = res + "'%s',".replaceAll("%s", values[i]);
        }
        return res.replaceAll(",$", "");
    }

    private String fonctionCopy(final String param) {
        String res = "";
        if (param.contains(";")) {
            final String values[] = param.split(";");
            final int count = values.length;
            if (count == 3) {
                final String valeur = values[0];
                final int debut = Integer.parseInt(values[1]);
                final int fin = Integer.parseInt(values[2]);
                res = valeur.substring(debut, fin);
            }
        }
        return res;
    }

    /**
     * Deprecated function. Use stringFormat() instead.
     */
    @Deprecated
    private String fonctionFormat(final String param) {
        if (param.contains("||")) {
            final String pattern = param.substring(0, param.indexOf("||"));
            final String values[] = param.substring(param.indexOf("||") + 2).split(";");
            String res = "";
            for (int i = 0; i < values.length; i++) {
                res = res + pattern.replaceAll("%s", values[i]);

            }
            return res.replaceAll(",$", "");
        } else {
            return param;
        }
    }

    private String stringFormat(final String params) {
        final String FORMAT_ERROR = "The STRINGFORMAT function parameters ('" + params + "') dont match the pattern 'pattern=<eg:%4s>;values=<values seperated by colon character. eg:val1,val2>;types=<the type of each value seperated by colon character. eg:String,Integer>";

        String result = "";

        // Verify the parameters fit the pattern
        final Map<String, String> parameters = new HashMap<>();
        final Pattern pattern = Pattern.compile(STRING_FORMAT_PATTERN);
        final Matcher matcher = pattern.matcher(params);
        while (matcher.find()) {
            parameters.put(matcher.group(1), matcher.group(2));
        }

        if (StringUtils.isBlank(parameters.get("pattern")) || StringUtils.isBlank(parameters.get("values"))) {
            LOG.error(FORMAT_ERROR);
        } else {
            final List<Object> objects = new ArrayList<>();
            final String[] values = parameters.get("values").split(",");
            final String[] types = parameters.get("types").split(",");
            if (values.length == types.length) {
                for (int i = 0; i < values.length; i++) {
                    Object object = null;
                    switch (types[i]) {
                        case "String":
                            object = values[i];
                            break;
                        case "Integer":
                            object = Integer.valueOf(values[i]);
                            break;
                        case "Float":
                            object = Float.valueOf(values[i]);
                            break;
                        case "Double":
                            object = Double.valueOf(values[i]);
                            break;
                        default:
                            LOG.error("Not supported type '" + types[i] + "'");
                            break;
                    }
                    objects.add(object);
                }
                result = String.format(parameters.get("pattern"), objects.toArray());
            } else {
                LOG.error("The number of types must fit the number of values");
            }
        }

        return result;
    }

    /**
     * Deprecated function. Use stringFormat() instead.
     */
    @Deprecated
    private String fonction2digit(String param) {
        if (param.length() == 1) {
            param = "0" + param;
        }
        return param;
    }

    private String fonctionLogin(String param) {
        // Remplacement des caractéres {'} et {space} par un -
        param = param.replaceAll("[' ]", "-");
        while (param.contains("--")) {
            param = param.replaceAll("--", "-");
        }
        // Nettoyage des {-} en fin de chaine
        param = param.replaceAll("[^\\w-\\.]", "");
        param = param.replaceAll("-$", "");
        return param.toLowerCase();
    }

    private String fonctionNom(String s) {
        s = s.replaceAll("[^\\w' -]", "");
        s = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
        return s;
    }

    private String fonctionPrenom(String s) {
        // Majuscule sur la 1ere lettre
        s = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
        // Suppression des pr�noms secondaires
        s = s.replaceAll("[^\\w' -]", "");
        if (s.indexOf(" ") < 3) {
            s.replaceFirst(" ", "");
        }
        if (s.indexOf(" ") > 0) {
            s = s.substring(0, s.indexOf(" "));
        }
        return s;
    }

    private String fonctionFrEduRneExtractFonction(final String s) {
        if (s.contains("$")) {
            return s.substring(s.lastIndexOf("$") + 1);
        } else {
            return "";
        }
    }

    private String fonctionNow(final String param) {
        String now;
        final Date date = new Date();
        if (StringUtils.isNotBlank(param)) {
            final DateFormat dateFormat = new SimpleDateFormat(param);
            now = dateFormat.format(date);
        } else {
            final Timestamp time = new Timestamp(date.getTime());
            now = time.toString();
        }
        return now;
    }

    private String fonctionSqlDate() {
        final Date date = new Date();
        final java.sql.Date dateSQL = new java.sql.Date(date.getTime());
        return dateSQL.toString();
    }

    private String searchFunctions(String s, final Map<String, String> mapTable, final CallStack parentCallStack) throws AlambicException {
        // execution récursive des fonctions suivant le pattern (NOMFONCTION mem='VARIABLE')PARAM(/NOMFONCTION)
        final Matcher m = functionPattern.matcher(s);
        while (m.find()) {
            if (m.groupCount() == 4) {
                final String chaine = m.group(0);
                final String fonction = m.group(1);
                final String param = m.group(4);
                CallStack callStack = parentCallStack.getInner(chaine);
                if (null == callStack) {
                    callStack = new CallStack(fonction);
                    callStack.setFulltext(chaine);
                    callStack.setParam(param);
                    parentCallStack.add(callStack);
                }
                final String resultat = executeFuntion(fonction, searchFunctions(param, mapTable, callStack), callStack);
                if (resultat != null) {
                    s = s.replace(chaine, resultat);
                }
                if (m.group(2) != null) {
                    mapTable.put(m.group(3), resultat);
                }
            }
        }
        return s;
    }

    public String generateSalt(final String params) {
    	Integer saltLength = Integer.valueOf(StringUtils.isNotBlank(params) ? params.trim() : String.valueOf(Constants.DEFAULT_SALT_LENGTH));
    	SecureRandom random = new SecureRandom();
    	byte[] salt = new byte[saltLength];
    	random.nextBytes(salt);
    	return Base64.encodeBase64String(salt).substring(0, saltLength);
    }

    public String replaceVarsFromMap(final String patternString, String s, final Map<String, String> mapTable) {
        final Matcher m = Pattern.compile(patternString).matcher(s);
        while (m.find()) {
            if (mapTable.containsKey(m.group(1))) {
                s = s.replace(m.group(0), mapTable.get(m.group(1)));
            }
        }
        return s;
    }

    public String executeAllFunctions(String s) throws AlambicException {
        final Map<String, String> mapTable = new HashMap<>();
        s = searchFunctions(s, mapTable, new CallStack("Root"));
        s = replaceVarsFromMap("%([^%]+)%", s, mapTable);
        mapTable.clear();
        return s;
    }

    public String valueToString(final Object value) throws UnsupportedEncodingException {
        if (value == null) {
            return "";
        } else if (value instanceof String) {
            return value.toString();
        } else if (value instanceof byte[]) {
            final byte[] bytes = (byte[]) value;
            return new String(bytes, "UTF-8");
        }
        return "";
    }

    public DirContext getDirContext(final Hashtable<?, ?> environment) throws NamingException {
        return new InitialDirContext(environment);
    }

}