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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.directory.shared.ldap.message.ArrayNamingEnumeration;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Functions.class, UUID.class })
public class FunctionsTest {

	private InitialDirContext mockedDirContext;

	@Before
	public void setUp() throws Exception {
		mockedDirContext = PowerMockito.mock(InitialDirContext.class);
		PowerMockito.whenNew(InitialDirContext.class).withAnyArguments().thenReturn(mockedDirContext);
	}

	private NamingEnumeration<SearchResult> getResultSet(final String[] results) {
		final List<SearchResult> resultSet = new ArrayList<>();

		for (final String result : results) {
			final Attributes attributes = new AttributesImpl();
			final String[] mockAttributes = result.split(";");
			for (final String mockAttribute : mockAttributes) {
				final String[] attr = mockAttribute.split("=");
				final String attrName = attr[0];
				final String[] attrValues = attr[1].split(",");
				final Attribute attribute = new AttributeImpl(attrName);
				for (final String attrValue : attrValues) {
					attribute.add(attrValue);
					attributes.put(attribute);
				}
			}
			final SearchResult sr = new SearchResult(result, null, attributes);
			resultSet.add(sr);
		}

		return new ArrayNamingEnumeration<>(resultSet.toArray(new SearchResult[resultSet.size()]));
	}

	/* test use case: one LDAP entity matches the search criterion */
	@Test
	public void test1() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure1@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: two LDAP entities match the search criterion */
	@Test
	public void test2() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr", "mail=gabriel.faure1@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure2@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: one LDAP entity matches the search criterion, the LDAP attribute is multi-valued */
	@Test
	public void test3() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr,gabriel.faure1@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure2@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: two LDAP entities match the search criterion, an intermediary index (nil) is free */
	@Test
	public void test4() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure1@ac-rennes.fr", "mail=gabriel.faure3@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: three LDAP entities match the search criterion, an intermediary index "1" is free */
	@Test
	public void test5() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr", "mail=gabriel.faure2@ac-rennes.fr", "mail=gabriel.faure3@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure1@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: three LDAP entities match the search criterion, no intermediary index is free. */
	@Test
	public void test6() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr", "mail=gabriel.faure1@ac-rennes.fr", "mail=gabriel.faure2@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure3@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: no LDAP entity matches the search criterion */
	@Test
	public void test7() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] {}));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: three LDAP entities match the search criteria, no intermediary index is free, multi-criteria LDAP request. */
	@Test
	public void test8() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr", "mail=gabriel.faure1@ac-rennes.fr", "mail=gabriel.faure2@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(&(objectClass=ENTPerson)(mail=gabriel.faure*@ac-rennes.fr))(/UNICITY)");
			Assert.assertEquals("gabriel.faure3@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: three LDAP entities match the search criteria, no intermediary index is free, multi-criteria LDAP request in other order. */
	@Test
	public void test9() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr", "mail=gabriel.faure1@ac-rennes.fr", "mail=gabriel.faure2@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(&(mail=gabriel.faure*@ac-rennes.fr)(objectClass=ENTPerson))(/UNICITY)");
			Assert.assertEquals("gabriel.faure3@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: mal-formed LDAP search URL. */
	@Test
	public void test10() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr", "mail=gabriel.faure1@ac-rennes.fr", "mail=gabriel.faure2@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr?(&(mail=gabriel.faure*@ac-rennes.fr)(objectClass=ENTPerson))(/UNICITY)");
			Assert.assertEquals("", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: two LDAP entities match the search criteria, multiple attributes unicity control */
	@Test
	public void test11() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=maria-teresa.cancelinha@ac-rennes.fr", "mailEquivalentAddress=maria-teresa.cancelinha1@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap.ac-rennes.fr:389/ou=ac-rennes,ou=education,o=gouv,c=fr??sub?(|(mail=maria-teresa.cancelinha*@ac-rennes.fr)(mailEquivalentAddress=maria-teresa.cancelinha*@ac-rennes.fr)))(/UNICITY)");
			Assert.assertEquals("maria-teresa.cancelinha2@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: two LDAP entities match the search criteria, multiple attributes unicity control, attributs multi-valued */
	@Test
	public void test12() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=maria-teresa.cancelinha@ac-rennes.fr,maria-teresa.cancelinha2@ac-rennes.fr", "mailEquivalentAddress=maria-teresa.cancelinha1@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap.ac-rennes.fr:389/ou=ac-rennes,ou=education,o=gouv,c=fr??sub?(|(mail=maria-teresa.cancelinha*@ac-rennes.fr)(mailEquivalentAddress=maria-teresa.cancelinha*@ac-rennes.fr)))(/UNICITY)");
			Assert.assertEquals("maria-teresa.cancelinha3@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: two LDAP entities match the search criteria, multiple attributes unicity control, attributs multi-valued */
	@Test
	public void test13() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=maria-teresa.cancelinha@ac-rennes.fr,maria-teresa.cancelinha2@ac-rennes.fr;mailEquivalentAddress=maria-teresa.cancelinha3@ac-rennes.fr",
							"mail=maria-teresa.cancelinha1@ac-rennes.fr;mailEquivalentAddress=maria-teresa.cancelinha4@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap.ac-rennes.fr:389/ou=ac-rennes,ou=education,o=gouv,c=fr??sub?(|(mail=maria-teresa.cancelinha*@ac-rennes.fr)(mailEquivalentAddress=maria-teresa.cancelinha*@ac-rennes.fr)))(/UNICITY)");
			Assert.assertEquals("maria-teresa.cancelinha5@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: the text case is ignored */
	@Test
	public void test14() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure1@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: the text case is ignored */
	@Test
	public void test15() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "mail=gabriel.faure@ac-rennes.fr" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(mail=gabriel.faure*@ac-rennes.fr)(/UNICITY)");
			Assert.assertEquals("gabriel.faure1@ac-rennes.fr", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	@Test
	public void test16() {
		try {
			final UUID mockedUUID1 = UUID.randomUUID();
			final UUID mockedUUID2 = UUID.randomUUID();
			final UUID mockedUUID3 = UUID.randomUUID();
			PowerMockito.mockStatic(UUID.class);
			PowerMockito.when(UUID.randomUUID()).thenReturn(mockedUUID1).thenReturn(mockedUUID2).thenReturn(mockedUUID3);

			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "ENTPersonUid=" + mockedUUID1.toString() })).
					thenReturn(getResultSet(new String[] { "ENTPersonUid=" + mockedUUID2.toString() })).
					thenReturn(null);

			final String uuid = Functions.getInstance().executeAllFunctions("(UNICITY)ldap://ldap-int.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(ENTPersonUid=(UUID) (/UUID)*)(/UNICITY)");
			Assert.assertEquals(mockedUUID3.toString(), uuid);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: check unicity function with embedded functions to generate indexes (STRINGFORMAT + INCREMENT) */
	@Test
	public void test17() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "employeeNumber=14V0000001EXT", "employeeNumber=14V0000002EXT", "employeeNumber=14V0000003EXT" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)candidate=\"(STRINGFORMAT)pattern=%07d;values=(INCREMENT)1(/INCREMENT);types=Integer(/STRINGFORMAT)\"|search=ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(employeeNumber=14V*EXT)(/UNICITY)");
			Assert.assertEquals("14V0000004EXT", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: check unicity function with embedded functions to generate indexes (STRINGFORMAT + INCREMENT) & credentials */
	@Test
	public void test18() {
		try {
			PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
					thenReturn(getResultSet(new String[] { "employeeNumber=14V0000001EXT", "employeeNumber=14V0000002EXT" }));

			final String value = Functions.getInstance().executeAllFunctions("(UNICITY)candidate=\"(STRINGFORMAT)pattern=%07d;values=(INCREMENT)1(/INCREMENT);types=Integer(/STRINGFORMAT)\",login=\"cn=TechnicalPrincipal\",password=\"TopSecret\"|search=ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(&amp;(fonctm=EXTACA)(employeeNumber=14V*EXT))(/UNICITY)");
			Assert.assertEquals("14V0000003EXT", value);
		} catch (final Exception e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	/* test use case: check behaviour on LDAP exception */
	@Test(expected=AlambicException.class)
	public void test19() throws NamingException, AlambicException {
		PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).thenThrow(new NamingException());

		Functions.getInstance().executeAllFunctions("(UNICITY)candidate=\"(STRINGFORMAT)pattern=%07d;values=(INCREMENT)1(/INCREMENT);types=Integer(/STRINGFORMAT)\",login=\"cn=TechnicalPrincipal\",password=\"TopSecret\"|search=ldap://ldap-pp.in.ac-rennes.fr:389/ou=personnes,dc=ent-bretagne,dc=fr??sub?(&amp;(fonctm=EXTACA)(employeeNumber=14V*EXT))(/UNICITY)");
	}

}
