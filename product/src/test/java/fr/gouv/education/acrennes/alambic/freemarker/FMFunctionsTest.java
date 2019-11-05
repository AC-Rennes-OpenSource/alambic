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
package fr.gouv.education.acrennes.alambic.freemarker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.client.model.PropertyMap;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.JobContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.GREPSource;
import fr.gouv.education.acrennes.alambic.jobs.extract.sources.Source;
import fr.gouv.education.acrennes.alambic.utils.Variables;

public class FMFunctionsTest {

	private FMFunctions Fn;

	@Before
	public void setUp() {
		Fn = new FMFunctions();
	}

	@Test
	public void test1() {
		for (int i = 0; i < 20; i++) {
			final int res = Fn.getRandomNumber(0, 10);
			Assert.assertTrue((0 <= res) && (res <= 10));
		}
	}

	@Test
	public void test2() {
		try {
			// Open the file
			FileInputStream fstream;
			fstream = new FileInputStream("src/test/resources/data/temp/example-serialized-properties.csv");
			final BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			// Read File Line By Line
			String strLine;
			while ((strLine = br.readLine()) != null) {
				if (!strLine.matches("^\"UUID\";.+")) {
					final Map<String, List<String>> entity = new HashMap<>();
					entity.put("PROPERTIES", Arrays.asList(strLine.split(";")[3]));
					final PropertyMap properties = Fn.getProperties(entity);
					Assert.assertEquals("Evaluation diagnostique - Lire et exploiter un graphique - niveau 3 ème - seconde", ((String) properties.get("dc:title")).trim());
					Assert.assertEquals("Les élèves répondent à un questionnaire MOODLE dans le cadre d'une évaluation diagnostique  - seconde (physique chimie - thème santé) possible  en AP seconde", ((String) properties.get("unum:resume")).trim());
					Assert.assertEquals("2015-04-20T13:34:08.21Z", ((String) properties.get("dc:modified")).trim());
				}
			}

			// Close the input stream
			br.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Asymetric ciphering test (RSA 1024 bits key length)
	 */
	@Test
	public void test4() {
		final String PLAIN_TEXT = "La chaîne de texte à chiffrer en RSA";

		try {
			final String cipheredText = Fn.encrypt("RSA",
					"./src/test/resources/data/security/alambic.keystore",
					"testpass",	"JCEKS", "aliasrsa1024", "testpass", "public", PLAIN_TEXT);

			final String plainText = Fn.decrypt("RSA",
					"./src/test/resources/data/security/alambic.keystore",
					"testpass", "JCEKS", "aliasrsa1024", "testpass", "private", cipheredText);
			Assert.assertEquals(PLAIN_TEXT, plainText);

		} catch (final AlambicException e) {
			Assert.fail();
		}
	}

	/**
	 * Normalization tests
	 */
	@Test
	public void test5() {
		Assert.assertEquals("Stephane", Fn.normalize("Stéphane"));
		Assert.assertEquals("Le creac'h", Fn.normalize("Le créac'h"));
		Assert.assertEquals("Loic", Fn.normalize("Loïc"));
		Assert.assertEquals("Le-Goff", Fn.normalize("Le Goff", "UID"));
		Assert.assertEquals("Le-Goff", Fn.normalize("   Le   Goff ", "UID"));
		Assert.assertEquals("Le-Goff", Fn.normalize("Le Goff  ", "EMAIL"));
		Assert.assertEquals("Le-Goff", Fn.normalize(" Le   Goff  ", "EMAIL"));
		Assert.assertEquals("le Goff", Fn.normalize(" le   Goff  "));
		Assert.assertEquals("Guyomarc-h", Fn.normalize("Guyomarc'h", "UID"));
		Assert.assertEquals("Guyomarch", Fn.normalize("Guyomarc'h", "EMAIL"));
		Assert.assertEquals("Guyomarc-h", Fn.normalize("Guyomarc' h", NormalizationPolicy.UID));
		Assert.assertEquals("Guyomarch", Fn.normalize("Guyomarc' h", NormalizationPolicy.EMAIL));
		Assert.assertEquals("le-cleach", Fn.normalize("le cleach'", NormalizationPolicy.UID));
		Assert.assertEquals("le-cleach", Fn.normalize("le  cleach'", NormalizationPolicy.UID));
		Assert.assertEquals("le-cleach", Fn.normalize("le  cleach'", NormalizationPolicy.EMAIL));
		Assert.assertEquals("le-cleach", Fn.normalize("le cléach'", NormalizationPolicy.EMAIL));
		Assert.assertEquals("l-argoat", Fn.normalize("l'argoat'", NormalizationPolicy.UID));
		Assert.assertEquals("MARIE-CHRISTINE", Fn.normalize("MARIE -CHRISTINE"));
		Assert.assertEquals("MARIE-CHRISTINE", Fn.normalize("MARIE -  CHRISTINE"));
		Assert.assertEquals("MARIE-CHRISTINE", Fn.normalize("MARIE- CHRISTINE"));
		Assert.assertEquals("MARIE-CHRISTINE Cotret", Fn.normalize("MARIE- CHRISTINE Cotret "));
		Assert.assertEquals("Anne MARIE-Louise D'Orleans", Fn.normalize("Anne MARIE  - Louise D 'Orleans"));
		Assert.assertEquals("Anne-MARIE-Louise-DOrleans", Fn.normalize("Anne MARIE  - Louise D 'Orleans", NormalizationPolicy.EMAIL));
		Assert.assertEquals("Jacques Le creac'h", Fn.normalize("Jacques Le créac' h"));
		Assert.assertEquals("Jacques Le creac'h", Fn.normalize("Jacques Le créac  'h"));
		Assert.assertEquals("Jacques Le creac'h", Fn.normalize("Jacques  Le créac  ' h"));
		Assert.assertEquals("Jacques-Le-creach", Fn.normalize("Jacques  Le créac  ' h", NormalizationPolicy.EMAIL));
		Assert.assertEquals("Jacques-Le-creac-h", Fn.normalize("Jacques  Le créac  ' h", NormalizationPolicy.UID));
		Assert.assertEquals("DUPUIS ex DULONG", Fn.normalize("DUPUIS (ex DULONG)"));
		Assert.assertEquals("LE-DEAUT", Fn.normalize("LE DEAUT", NormalizationPolicy.NOM));
		Assert.assertEquals("Le-Touzo", Fn.normalize("Le Touzo", NormalizationPolicy.NOM));
		Assert.assertEquals("Le-Touzo", Fn.normalize("Le-Touzo", NormalizationPolicy.EMAIL));
		Assert.assertEquals("Le-Touzo", Fn.normalize("Le-Touzo", NormalizationPolicy.UID));
		Assert.assertEquals("LE-BOULC-H", Fn.normalize("LE-BOULC'H", NormalizationPolicy.NOM));
		Assert.assertEquals("L-argoat-n5", Fn.normalize("L'argoat n°5", NormalizationPolicy.NUXEO_ECM_NAME));
		Assert.assertEquals("L-argoat-n-5", Fn.normalize("L'argoat n:  (5)", NormalizationPolicy.NUXEO_ECM_NAME));
		Assert.assertEquals("L-operation-3-5-4-2-3", Fn.normalize("L'opération 3*5+4,2/3=?", NormalizationPolicy.NUXEO_ECM_NAME));
		Assert.assertEquals("IP-127-0-0-1", Fn.normalize("@IP=[127.0.0.1]", NormalizationPolicy.NUXEO_ECM_NAME));
		Assert.assertEquals("un text avec 'guillemets gênants'", Fn.normalize("un text avec \"guillemets gênants\"", NormalizationPolicy.JSON, false));
	}

	/**
	 * Capitalization tests
	 */
	@Test
	public void test6() {
		Assert.assertEquals("Le Goff", Fn.capitalize("Le goff"));
		Assert.assertEquals("Anne-Marie Guénloé", Fn.capitalize("anne-marie Guénloé"));
		Assert.assertEquals("Arthur.Martin", Fn.capitalize("arthuR.martin"));
		Assert.assertEquals("Jacques L'hervet", Fn.capitalize("jaCques l'Hervet"));
		Assert.assertEquals("Jean-Pierre Le Borgne", Fn.capitalize("   Jean-PIERRE le  BorGNe"));
		Assert.assertEquals("Yann Y", Fn.capitalize("yann y"));
		Assert.assertEquals("Fanchig", Fn.capitalize("fanchig "));
		Assert.assertEquals("Le-Henaff", Fn.capitalize("Le-henaFF"));
		Assert.assertEquals("Bakir Rio", Fn.capitalize("BAKIR  RIO"));
		Assert.assertEquals("Marie -Christine", Fn.capitalize("MARIE -CHRISTINE"));
	}

	/**
	 * Test the resolution of variables within queries
	 */
	@Test
	public void test7() {
		final String RESOURCE_XML_ELEMENT = "<resource type=\"grep\" name=\"test-grep-resource\" dynamic=\"true\"><input>%TEST_FILE_PATH%</input><query/></resource>";
		final Variables variables = new Variables();
		variables.put("ALAMBIC_SCRIPTING_DIR", ".");
		variables.put("ALAMBIC_TARGET_ENVIRONMENT", "DEVELOPMENT");
		variables.put("TOKEN_TO_SEARCH_WORD1", "faucibus");
		variables.put("TOKEN_TO_SEARCH_PHRASE", "%TOKEN_TO_SEARCH_WORD1% quis tortor");
		variables.put("TEST_FILE_PATH", "%ALAMBIC_SCRIPTING_DIR%/src/test/resources/data/alambic/test-grep-file.txt");

		try {
			final SAXBuilder jobsSAXBuilder = new SAXBuilder();
			final Document sourceDescriptor = jobsSAXBuilder.build(new StringReader(RESOURCE_XML_ELEMENT));
			final CallableContext context = new JobContext("../fake/", null, variables, null);
			final Source source = new GREPSource(context, sourceDescriptor.getRootElement());

			final Map<String, Source> resources = new HashMap<>();
			resources.put("test-grep-resource", source);

			final List<Map<String, List<String>>> result = Fn.query(resources, "test-grep-resource", ".*%TOKEN_TO_SEARCH_PHRASE%.*");
			Assert.assertEquals("Morbi faucibus quis tortor sed feugiat.", result.get(0).get("1").get(0).trim());
		} catch (AlambicException | JDOMException | IOException e) {
			Assert.fail("Failed with execution exception error:" + e.getMessage());
		}
	}

	/**
	 * Test the resolution of variables within queries
	 */
	@Test
	public void test8() {
		Assert.assertEquals("Léonor", Fn.unescapeXML("L&#233;onor"));
		Assert.assertEquals("Chaïma", Fn.unescapeXML("Cha&#239;ma"));
		Assert.assertEquals("$$L'Hôtel Hochet$", Fn.unescapeXML("$$L&apos;H&#244;tel Hochet$"));
		Assert.assertEquals(null, Fn.unescapeXML(null));
	}

}
