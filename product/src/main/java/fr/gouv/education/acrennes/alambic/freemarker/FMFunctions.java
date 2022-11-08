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
package fr.gouv.education.acrennes.alambic.freemarker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.SourceFilter;
import fr.gouv.education.acrennes.alambic.security.CipherHelper;
import fr.gouv.education.acrennes.alambic.security.CipherKeyStore;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import freemarker.ext.dom.NodeModel;
import net.htmlparser.jericho.CharacterEntityReference;
import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.NumericCharacterReference;
import net.htmlparser.jericho.Config.CharacterReferenceEncodingBehaviour;

public class FMFunctions {

	private static final Log log = LogFactory.getLog(FMFunctions.class);
	private static final String VOCABULARY_SEPARATOR = "/";
	private static final String DICTIONARY_SEPARATOR = ",";
	private static final String VOCABULARY_KEY_ID = "id";
	private static final String[] XML_SPECIAL_CHARACTERS = new String[] {"&", "'", "<", ">", "\""};
	private final Random randomGenerator;
	private final Map<String, List<Map<String, List<String>>>> cachedResources;
	private final Map<String, List<Object>> cache;
	private CallableContext context = null;
	private JSONParser parser;
	
	public FMFunctions() {
		parser = new JSONParser();
		randomGenerator = new Random();
		cachedResources = new HashMap<String, List<Map<String, List<String>>>>();
		cache = new ConcurrentHashMap<String, List<Object>>();
		Config.CurrentCharacterReferenceEncodingBehaviour=CUSTOM_CHARACTER_REFERENCE_ENCODING_BEHAVIOUR;
		Config.IsApostropheEncoded = false;
	}

	public FMFunctions(final CallableContext context) {
		this();
		this.context = context;
	}

	/* Select the accented characters only to be encoded by the method escapeHTMLAccentedCharacters()
	(XML special characters are excluded)
	 */
	public static final CharacterReferenceEncodingBehaviour CUSTOM_CHARACTER_REFERENCE_ENCODING_BEHAVIOUR=new CharacterReferenceEncodingBehaviour() {
		public boolean isEncoded(final char ch, final boolean insideAttributeValue) {
			return ch>127 && CharacterEntityReference.getName(ch)!=null && !Arrays.stream(XML_SPECIAL_CHARACTERS).anyMatch(x -> x.equals(ch));
		}
	};

	public int getRandomNumber(final int min, final int max) {
		int randomNum = randomGenerator.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	public Map<String, List<String>> getRandomEntry(final Map<String, Source> resources, final String resourceName, final SourceFilter filter) {
		Map<String, List<String>> entry = null;
		List<Map<String, List<String>>> filteredResource = null;

		if (resources.containsKey(resourceName)) {
			filteredResource = getCachedResource(resources, resourceName, filter, false, null);
			if (null != filteredResource && 0 < filteredResource.size()) {
				int randomIndex = getRandomNumber(0, filteredResource.size() - 1);
				entry = filteredResource.get(randomIndex);
			}
		} else {
			log.warn("Les ressources ne contiennent pas '" + resourceName + "'");
		}

		return entry;
	}

	public List<Map<String, List<String>>> query(final Map<String, Source> resources, final String resourceName, final String query) {
		return query(resources, resourceName, query, null);
	}

	public List<Map<String, List<String>>> query(final Map<String, Source> resources, final String resourceName, final String query, final String scope) {
		List<Map<String, List<String>>> resultSet = Collections.emptyList();

		if (resources.containsKey(resourceName)) {
			try {
				Source resource = resources.get(resourceName);
				resultSet = resource.query(query, scope);
			} catch (AlambicException e) {
				log.error("Failed to execute the query '" + query + "' on resource '" + resourceName + "', error: " + e.getMessage(), e);
			}
		} else {
			log.warn("Les ressources ne contiennent pas '" + resourceName + "'");
		}

		return resultSet;
	}

	public List<Map<String, List<String>>> getEntries(final Map<String, Source> resources, final String resourceName, final String... filters) {
		return getEntries(resources, resourceName, false, null, filters);
	}

	public List<Map<String, List<String>>> getEntries(final Map<String, Source> resources, final String resourceName, final boolean distinct, final String orderby,
			final String... filters) {
		List<Map<String, List<String>>> filteredResource = Collections.emptyList();

		if (resources.containsKey(resourceName)) {
			filteredResource = getCachedResource(resources, resourceName, (null != filters && 0 < filters.length) ? new SourceFilter(filters) : null, distinct, orderby);
			if (null == filteredResource || 0 == filteredResource.size()) {
				log.info("Aucune entrée de trouvée pour la ressource '" + resourceName + "' (filtre='" + Arrays.toString(filters) + "')");
			}
		} else {
			log.warn("Les ressources ne contiennent pas '" + resourceName + "'");
		}

		return filteredResource;
	}

	public List<Map<String, List<String>>> getRandmonEntries(final int min, final int max, final Map<String, Source> resources, final String resourceName,
			final String... filters) {
		List<Map<String, List<String>>> entries = new ArrayList<Map<String, List<String>>>();

		int count = getRandomNumber(min, max);
		for (int i = 0; i < count; i++) {
			Map<String, List<String>> entry = getRandomEntry(resources, resourceName, (null != filters && 0 < filters.length) ? new SourceFilter(filters) : null);
			if (null != entry) {
				entries.add(entry);
			} else {
				break;
			}
		}

		return entries;
	}

	public String getRandomVocabularyEntry(final Map<String, Source> resources, final String dictionaries) {
		String parent = null;
		List<String> vocabularyEntry = null;

		String[] dictionariesList = dictionaries.split(DICTIONARY_SEPARATOR);
		if (0 < dictionariesList.length) {
			vocabularyEntry = new ArrayList<String>();
			int randomEntryDepth = getRandomNumber(1, dictionariesList.length);
			for (int i = 0; i < randomEntryDepth; i++) {
				String dictionaryName = dictionariesList[i];
				Map<String, List<String>> entry = getRandomEntry(resources, dictionaryName, (StringUtils.isNotBlank(parent)) ? new SourceFilter("parent=" + parent) : null);
				if (null != entry) {
					List<String> id = entry.get(VOCABULARY_KEY_ID);
					parent = id.get(0);
					vocabularyEntry.add(id.get(0));
				}
			}
		}

		return (null != vocabularyEntry) ? StringUtils.join(vocabularyEntry, VOCABULARY_SEPARATOR) : null;
	}

	public List<String> getRandomVocabularyEntries(final int min, final int max, final Map<String, Source> resources, final String dictionaries) {
		List<String> vocabularyEntries = new ArrayList<String>();

		int count = getRandomNumber(min, max);
		for (int i = 0; i < count; i++) {
			String entry = getRandomVocabularyEntry(resources, dictionaries);
			if (null != entry) {
				vocabularyEntries.add(entry);
			} else {
				break;
			}
		}

		return vocabularyEntries;
	}

	public String getVocabularyIdFromLabel(final String label) {
		return normalize(label, NormalizationPolicy.VOCABULARY).toLowerCase();
	}

	public String regexQuoteString(final String s) {
		return Pattern.quote(s);
	}

	public PropertyMap getProperties(final Map<String, List<String>> entity) {
		PropertyMap properties = null;

		List<String> serializedProperties = entity.get("PROPERTIES");
		String serializedObj = serializedProperties.get(0);
		byte[] bytes = Base64.decodeBase64(serializedObj.getBytes());
		properties = (PropertyMap) SerializationUtils.deserialize(bytes);

		return properties;
	}

	public String encrypt(final String algorithm, final String keystorePath, final String keystorePassword, final String keystoreType, final String alias,
			final String keyPassword, final String keyType, final String plaintext) throws AlambicException {
		CipherKeyStore keystore = new CipherKeyStore(keystorePath, CipherKeyStore.KEYSTORE_TYPE.valueOf(keystoreType), keystorePassword);
		CipherHelper cipher = new CipherHelper(algorithm, keystore, alias, keyPassword, keyType);
		try {
			byte[] cipheredBytes = cipher.execute(CipherHelper.CIPHER_MODE.ENCRYPT_MODE, plaintext);
			return Base64.encodeBase64String(cipheredBytes);
		} finally {
			cipher.close();
		}
	}

	public String decrypt(final String algorithm, final String keystorePath, final String keystorePassword, final String keystoreType, final String alias,
			final String keyPassword, final String keyType, final String cipheredtext) throws AlambicException {
		CipherKeyStore keystore = new CipherKeyStore(keystorePath, CipherKeyStore.KEYSTORE_TYPE.valueOf(keystoreType), keystorePassword);
		CipherHelper cipher = new CipherHelper(algorithm, keystore, alias, keyPassword, keyType);
		try {
			byte[] cipheredBytes = Base64.decodeBase64(cipheredtext);
			byte[] plainBytes = cipher.execute(CipherHelper.CIPHER_MODE.DECRYPT_MODE, cipheredBytes);
			return new String(plainBytes);
		} finally {
			cipher.close();
		}
	}
	
	/**
	 * Freemarker ne propose pas de fonction "built-in" pour décoder du base64. Cette méthode permet cela.
	 * @param encodedString : la chaîne de caractères encodée en base64 
	 * @return la chaîne de caractères décodée.
	 */
	public String base64Decode(String encodedString) {
		return new String(Base64.decodeBase64(encodedString));
	}

	public String normalize(final String rawStr) {
		return normalize(rawStr, (NormalizationPolicy) null);
	}

	public String normalize(final String rawStr, final String policy) {
		return normalize(rawStr, NormalizationPolicy.valueOf(policy));
	}

	public String normalize(final String rawStr, NormalizationPolicy policy) {
		return normalize(rawStr, policy, true);
	}
	
	public String normalize(final String rawStr, NormalizationPolicy policy, boolean discardAccents) {
		String str = rawStr.trim();

		// get rid of accentuated characters if required;
		if (discardAccents) {
			str = Normalizer.normalize(str, Form.NFD).replaceAll("[^\\p{ASCII}]", "");
		}

		// escape the special characters according to the policy passed-in parameter
		policy = (null != policy) ? policy : NormalizationPolicy.DEFAULT;
		for (String rules : policy.getRules()) {
			String[] rule = rules.split("=");
			Pattern pattern = Pattern.compile(rule[0]);
			Matcher matcher = pattern.matcher(str);
			while (matcher.find()) {
				if (1 == rule.length) {
					str = str.replaceAll(rule[0], "");
				} else {
					if (rule[1].contains("$")) {
						String dst = rule[1];
						Pattern capturePattern = Pattern.compile("\\$(\\d+)");
						Matcher captureMatcher = capturePattern.matcher(rule[1]);
						while (captureMatcher.find()) {
							Integer index = Integer.valueOf(captureMatcher.group(1));
							dst = dst.replaceAll("\\" + captureMatcher.group(0), matcher.group(index));
						}
						str = str.replaceAll(matcher.group(0), dst);
					} else {
						str = str.replaceAll(rule[0], rule[1]);
					}
				}
			}
		}

		return str;
	}

	public String capitalize(final String rawStr) {
		String str = rawStr.trim().replaceAll("\\s{2,}", " ");

		String[] tokens = str.split("[\\s-\\.]");
		for (String token : tokens) {
			if (StringUtils.isNotBlank(token)) {
				String capToken = token.substring(0, 1).toUpperCase().concat(token.substring(1).toLowerCase());
				str = str.replaceFirst(token, capToken);
			}
		}

		return str;
	}

	public void log(final String level, final String message) {
		if ("INFO".equalsIgnoreCase(level)) {
			log.info(message);
		} else if ("WARNING".equalsIgnoreCase(level) || "WARN".equalsIgnoreCase(level)) {
			log.warn(message);
		} else if ("ERROR".equalsIgnoreCase(level)) {
			log.error(message);
		} else {
			log.debug(message);
		}
	}

	public void logNodeModel(final String level, final NodeModel item) {
		try {
			Transformer tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			Writer out = new StringWriter();
			tf.transform(new DOMSource(item.getNode()), new StreamResult(out));
			log(level, out.toString());
		} catch (TransformerFactoryConfigurationError | TransformerException e) {
			log.error("Failed to log the Freemarker NodeModel object, error : " + e.getMessage());
		}
	}

	public String encodeURI(final String URI) {
		String encodedURI = "";

		try {
			if (StringUtils.isNotBlank(URI)) {
				encodedURI = URLEncoder.encode(String.valueOf(URI), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			log.error("Failed to encode the URI '" + URI + "', error: " + e.getMessage());
		}

		return encodedURI;
	}
	
	public String decodeURI(final String URI) {
		String decodedURI = "";

		try {
			if (StringUtils.isNotBlank(URI)) {
				decodedURI = URLDecoder.decode(String.valueOf(URI), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			log.error("Failed to decode the URI '" + URI + "', error: " + e.getMessage());
		}

		return decodedURI;
	}
	
	public JSONObject getJSONObject(final String json) throws AlambicException {
		JSONObject obj = null;
		try {
			obj = (JSONObject) this.parser.parse(json);
		} catch (ParseException e) {
			throw new AlambicException("Error file parsing the JSON string '" + json + "', error: " + e.getMessage());
		}
		return obj;
	}

	public JSONArray getJSONArray(final String json) throws AlambicException {
		JSONArray obj = null;
		try {
			obj = (JSONArray) this.parser.parse(json);
		} catch (ParseException e) {
			throw new AlambicException("Error file parsing the JSON string '" + json + "', error: " + e.getMessage());
		}
		return obj;
	}

	public JSONObject getJSONObjectFromFile(final String filepath) throws AlambicException {
		JSONObject obj = null;
		try {
			obj = (JSONObject) this.parser.parse(new FileReader(filepath));
		} catch (IOException | ParseException e) {
			throw new AlambicException("Error file parsing the JSON file '" + filepath + "', error: " + e.getMessage());
		}
		return obj;
	}

	public JSONArray getJSONArrayFromFile(final String filepath) throws AlambicException {
		JSONArray obj = null;
		try {
			obj = (JSONArray) this.parser.parse(new FileReader(filepath));
		} catch (IOException | ParseException e) {
			throw new AlambicException("Error file parsing the JSON file '" + filepath + "', error: " + e.getMessage());
		}
		return obj;
	}
	
	public NodeModel getNodeModel(final String filepath) {
		NodeModel node = null;
		
		try {
			node = freemarker.ext.dom.NodeModel.parse(new File(filepath));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			log.error(e.getMessage());
		}
		
		return node;
	}

	public NodeModel getNodeModelFromString(final String body) {
		NodeModel node = null;

		try {
			node = freemarker.ext.dom.NodeModel.parse(new InputSource(new ByteArrayInputStream(body.getBytes())));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			log.error(e.getMessage());
		}

		return node;
	}

	private List<Map<String, List<String>>> getCachedResource(final Map<String, Source> resources, final String resourceName, final SourceFilter filter, final boolean distinct,
			final String orderby) {
		Source source = resources.get(resourceName);
		List<Map<String, List<String>>> cachedResource = source.getEntries();

		if (null != filter) {
			String filterId = filter.toString();
			String key = resourceName + "-" + filterId + "-" + distinct;
			String h = DigestUtils.md5Hex(key);

			if (!cachedResources.containsKey(h)) {
				cachedResources.put(h, source.getEntries(distinct, orderby, filter));
			}

			cachedResource = cachedResources.get(h);
		}

		return cachedResource;
	}

	public String ignoreNoValidXMLCharacters(final String in) {
		StringBuilder out = new StringBuilder(); // Used to hold the output.
		char current; // Used to reference the current character.

		if ((in == null) || ("".equals(in))) {
			return "";
		}
		for (int i = 0; i < in.length(); i++) {
			current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
			if ((current == 0x9)
					|| (current == 0xA)
					|| (current == 0xD)
					|| ((current >= 0x20) && (current <= 0xD7FF))
					|| ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF))) {
				out.append(current);
			}
		}
		return out.toString();
	}

	public String getUUID() {
		return UUID.randomUUID().toString();
	}

	/* Encode string into Numeric Character Reference (either decimal or hexadecimal format) (https://www.w3.org/TR/html401/charset.html#h-5.3) */
	public String escapeHTMLAccentedCharacters(final String str, final HtmlEncodingFormat format) {
		String escapedStr="";
		
		if (HtmlEncodingFormat.DECIMAL.equals(format)) {
			escapedStr = NumericCharacterReference.encodeDecimal(str);
		} else {
			escapedStr =  NumericCharacterReference.encodeHexadecimal(str);
		}
		
		return escapedStr;
	}

	public String unescapeXML(final String str) {
		return StringEscapeUtils.unescapeXml(str);
	}

	public String getRandomSalt(final int length) {
		return Functions.getInstance().generateSalt(String.valueOf(length));
	}

	public String resolveString(String stg) throws AlambicException {
		String resolvedString = null;
		if (null != this.context) {
			if (stg.matches("^%.+%$")) {
				resolvedString = this.context.resolveString(stg);				
			} else {
				log.error("Can't resolve the string '" + stg + "' since it doesn't match the pattern '%<name>%'");
			}
		} else {
			log.error("Can't resolve the string '" + stg + "' since no context is initialized");
		}
		return resolvedString;
	}

	public List<Object> getCacheList(final String key) {
		this.cache.putIfAbsent(key, new ArrayList<Object>());
		return this.cache.get(key);
	}
	
	public int getCacheListSize(final String key) {
		return getCacheList(key).size();
	}
	
	public List<List<Object>> getCacheLists(final String key_pattern) {
		return this.cache.entrySet().stream()
				.filter(entry -> entry.getKey().matches(key_pattern))
				.map(entry -> entry.getValue())
				.collect(Collectors.toList());
	}

	public List<String> getCacheKeys(final String regex) {
		return this.cache.keySet().stream()
				.filter(k -> k.matches(regex))
				.collect(Collectors.toList());
	}

	public boolean addToCacheList(final String key, final Object value) {
		this.cache.putIfAbsent(key, new ArrayList<Object>());
		return this.cache.get(key).add(value);
	}

	public boolean addToCacheList(final String key, final List<Object> values) {
		this.cache.putIfAbsent(key, new ArrayList<Object>());
		return this.cache.get(key).addAll(values);
	}

	public Object setToCacheList(final String key, final int index, final Object value) {
		return this.cache.get(key).set(index, value);
	}

	public void removeFromCacheList(final String key) {
		this.cache.remove(key);
	}

	public void clearCacheList() {
		this.cache.clear();
	}

	public void clearCacheList(final String key) {
		if (this.cache.containsKey(key)) {
			this.cache.get(key).clear();
		}
	}

}
