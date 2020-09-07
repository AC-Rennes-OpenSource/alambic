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
package fr.gouv.education.acrennes.alambic.jobs.extract.clients;

import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import junit.framework.TestCase;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BlurIdToStateBase.class})
@PowerMockIgnore("javax.management.*")
public class BlurIdToStateBaseTest extends TestCase {

	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";
	private BlurIdToStateBase bitsb;

	@Override
	@Before
	public void setUp() throws Exception {		
		// Mock the entity manager helper so that the embedded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
		bitsb = new BlurIdToStateBase("b.TY1aGh0Ukm/er=");
	}

	/**
	 * Use case :
	 * - Two blur identifiers are requested
	 * - The two entities are different (not the same person, no common phone number)
	 */
	@Test
	public void test1() throws AlambicException {
		// First query to get a blur identifier
		String jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"06.01.02.03.04\", \"07.01.02.03.04\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId1 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId1.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));

		// Second query to get a blur identifier
		jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"9876\","
				+ "\"firstName\":\"Eve\","
				+ "\"lastName\":\"Idamen\","
				+ "\"civility\":\"Madame\","
				+ "\"phones\":[\"06.02.03.04.05\", \"07.02.03.04.05\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId2 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId2.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
		Assert.assertFalse(blurId1.equalsIgnoreCase(blurId2));
	}

	/**
	 * Use case :
	 * - Two blur identifiers are requested
	 * - The two entities belongs to the same person
	 * - The query is based-on information from the same entity from LDAP
	 * - All phone numbers among entities are common (since query from same LDAP entity).
	 */
	@Test
	public void test2() throws AlambicException {
		// First query to get a blur identifier
		String jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"06.01.02.03.04\", \"07.01.02.03.04\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId1 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId1.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));

		// Second query to get a blur identifier
		jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"06.01.02.03.04\", \"07.01.02.03.04\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId2 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId2.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
		Assert.assertTrue(blurId1.equalsIgnoreCase(blurId2));
	}

	/**
	 * Use case :
	 * - Two blur identifiers are requested
	 * - The two entities belongs to the same person
	 * - The query is based-on information from two different entities from LDAP (could be one person with multiple AAF entities - hence multiple Toutatice entities - as this person is supervisor of multiple pupils in different schools)
	 * - One of the phone numbers among entities is common (the person didn't get the same phone number each time when registering to school services).
	 */
	@Test
	public void test3() throws AlambicException {
		// First query to get a blur identifier
		String jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"06.01.02.03.04\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId1 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId1.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));

		// Second query to get a blur identifier
		jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"54321\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"07.01.02.03.04\", \"06.01.02.03.04\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId2 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId2.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
		Assert.assertTrue(blurId1.equalsIgnoreCase(blurId2));
	}

	/**
	 * Use case :
	 * - Two blur identifiers are requested
	 * - The two entities belong to different persons with same first name, last name and civility (namesake)
	 * - No phone numbers among entities are common.
	 */
	@Test
	public void test4() throws AlambicException {
		// First query to get a blur identifier
		String jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"06.01.02.03.04\", \"07.01.02.03.04\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId1 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId1.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));

		// Second query to get a blur identifier
		jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"processId\":\"TESTU\","
				+ "\"id\":\"120120\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"06.02.03.04.05\", \"07.02.03.04.05\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId2 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId2.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
		Assert.assertFalse(blurId1.equalsIgnoreCase(blurId2));
	}

	/**
	 * Use case :
	 * - Two blur identifiers are requested
	 * - The two entities are different (not the same person - could be husband and wife)
	 * - The unique phone number is the same as it deal with the home phone number.
	 */
	@Test
	public void test5() throws AlambicException {
		// First query to get a blur identifier
		String jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"02.99.01.02.03\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId1 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId1.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));

		// Second query to get a blur identifier
		jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"id\":\"54321\","
				+ "\"firstName\":\"Julie\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"Madame\","
				+ "\"phones\":[\"02.99.01.02.03\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		final String blurId2 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId2.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
		Assert.assertFalse(blurId1.equalsIgnoreCase(blurId2));
	}

	/**
	 * Use case :
	 * - One blur identifier is requested
	 * - Mode : hashed identifier
	 */
	@Test
	public void test6() throws AlambicException {
		final String jsonQuery = "{"
				+ "\"blur_mode\":\"HASHED_ID\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"02.99.01.02.03\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		final List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		Assert.assertTrue(StringUtils.isNotBlank(sb.get(0).get("blurId").get(0)));
		Assert.assertFalse(sb.get(0).get("blurId").get(0).matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
	}

	/**
	 * Use case :
	 * - One blur identifier is requested
	 * - Mode : signature
	 */
	@Test
	public void test7() throws AlambicException {
		final String jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[\"02.99.01.02.03\"],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		final List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		Assert.assertTrue(sb.get(0).get("blurId").get(0).matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
	}

	/**
	 * Use case :
	 * - One blur identifier is requested
	 * - Mode : signature
	 * - the entity has no telephone number and no emails
	 * => Verify the blur identifier generator succeeds nevertheless to find back the previously generated blur identifier
	 */
	@Test
	public void test8() throws AlambicException {
		final String jsonQuery = "{"
				+ "\"blur_mode\":\"SIGNATURE\","
				+ "\"id\":\"12345\","
				+ "\"firstName\":\"Guy\","
				+ "\"lastName\":\"Tariste\","
				+ "\"civility\":\"M.\","
				+ "\"phones\":[],"
				+ "}";
		bitsb.executeQuery(jsonQuery);
		List<Map<String, List<String>>> sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		String blurId1 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId1.matches(".{8}-.{4}-.{4}-.{4}-.{12}"));
		
		bitsb.executeQuery(jsonQuery);
		sb = bitsb.getStateBase();
		Assert.assertEquals(1, sb.size());
		String blurId2 = sb.get(0).get("blurId").get(0);
		Assert.assertTrue(blurId2.equals(blurId1));
	}

	@Override
	@After
	public void tearDown() {
		/**
		 * Shutdown the derby system so that other unit tests don't run into exception because the database was not released.
		 * This is not visible when running the tests within Eclipse environment (launcher) but it is when packaging
		 * the project with maven.
		 */
		EntityManagerHelper.close();
	}

}