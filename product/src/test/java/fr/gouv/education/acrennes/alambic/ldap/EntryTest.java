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
package fr.gouv.education.acrennes.alambic.ldap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.shared.ldap.message.ArrayNamingEnumeration;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.education.acrennes.alambic.utils.Variables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Entry.class})
public class EntryTest {

    private InitialDirContext mockedDirContext;

    @Before
    public void setUp() {
        mockedDirContext = PowerMockito.mock(InitialDirContext.class);
    }

    // Utility function : converts the input JSON entry string representation into LDAP attributes structure
    private Attributes buildAttributes(final String attrs) {
        Attributes attributes = new BasicAttributes();

        if (StringUtils.isNotBlank(attrs)) {
            JSONObject jsonObj = new JSONObject(attrs);
            Iterator<String> keys = jsonObj.keys();
            while (keys.hasNext()) {
                String attrName = keys.next();
                Attribute attribute = new BasicAttribute(attrName);
                Object attrValues = jsonObj.get(attrName);
                if (attrValues instanceof JSONArray) {
                    List<Object> values = ((JSONArray) attrValues).toList();
                    for (Object value : values) {
                        if (StringUtils.isNotEmpty((String) value)) {
                            attribute.add(value);
                        }
                    }
                } else {
                    if (StringUtils.isNotEmpty((String) attrValues)) {
                        attribute.add(attrValues);
                    }
                }
                attributes.put(attribute);
            }
        }
        return attributes;
    }

    // Utility function : converts the input JSON resultSet string representation into LDAP attributes structure
    private NamingEnumeration<SearchResult> buildResultSet(final String ns, final String[] results) {
        List<SearchResult> resultSet = new ArrayList<>();

        for (String result : results) {
            SearchResult sr = new SearchResult(result, null, buildAttributes(result));
            sr.setNameInNamespace(ns);
            resultSet.add(sr);
        }

        return new ArrayNamingEnumeration<>(resultSet.toArray(new SearchResult[0]));
    }

    private Entry buildFromPivot(final String filePath) throws Exception {
    	return buildFromPivot("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", filePath);
    }
    
    private Entry buildFromPivot(final String nameInNamespace, final String filePath) throws Exception {
        final Element mockedPivotEntry;
        try (final InputStream is = EntryTest.class.getClassLoader().getResourceAsStream(filePath)) {
            mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();
        }

        PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn(nameInNamespace);
        PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

        return new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
    }

    private void setEntryInLdap(final String entryInLdap) throws Exception {
    	setEntryInLdap("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", entryInLdap);
    }

    private void setEntryInLdap(final String nameInNamespace, final String entryInLdap) throws Exception {
        PowerMockito.when(mockedDirContext.search(
                Matchers.anyString(),
                Matchers.anyString(),
                Matchers.any(SearchControls.class)
        )).thenReturn(
                buildResultSet(nameInNamespace, new String[]{entryInLdap})
        );
        PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(entryInLdap));
    }

    /* test use case :
     * - the entry from LDAP and pivot are the same */
    @Test
    public void test1() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot1.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]," +
                    "\"uid\":\"epoe\"," +
                    "\"ENTPersonUid\":\"0123456789\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext, never()).modifyAttributes(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'ENTPersonUid' (mode is ignore) is not present from LDAP */
    @Test
    public void test2() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot1.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]," +
                    "\"uid\":\"epoe\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ENTPersonUid\":\"0123456789\"}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'ENTPersonUid' (mode is ignore) is not present from LDAP
     * - no other attribute is modified */
    @Test
    public void test3() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot1.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]," +
                    "\"uid\":\"epoe\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext, never()).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ENTPersonUid\":\"0123456789\",\"title\":\"MOD\"}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'ENTPersonUid' (mode is ignore) is present from LDAP
     * - it HAS NO VALUE */
    @Test
    public void test4() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot1.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]," +
                    "\"uid\":\"epoe\"," +
                    "\"ENTPersonUid\":\"\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ENTPersonUid\":\"0123456789\"}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'ENTPersonUid' (mode is ignore) is present from LDAP
     * - it is a scalar
     * - it HAS AN EMPTY VALUE */
    @Test
    public void test5() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot1.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]," +
                    "\"uid\":\"epoe\"," +
                    "\"ENTPersonUid\":\" \"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext, never()).modifyAttributes(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'objectClass' (mode is IGNORE) is present from LDAP
     * - is not a scalar
     * - the target attribute has multiple values different from the pivot
     * */
    @Test
    public void test6() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot2.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"personne\"]," +
                    "\"uid\":\"epoe\"," +
                    "\"ENTPersonUid\":\"0123456789\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext, never()).modifyAttributes(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'objectClass' (mode is REPLACE) is present from LDAP
     * - is not a scalar
     * - the target attribute has multiple values different from the pivot
     * */
    @Test
    public void test7() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot1.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"personne\"]," +
                    "\"uid\":\"epoe\"," +
                    "\"ENTPersonUid\":\"0123456789\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'objectClass' (mode is IGNORE) is present from LDAP
     * - is not a scalar
     * - the target attribute HAS NO VALUE
     * */
    @Test
    public void test8() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot2.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[]," +
                    "\"uid\":\"epoe\"," +
                    "\"ENTPersonUid\":\"0123456789\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - the target attribute 'objectClass' (mode is IGNORE) is NOT present from LDAP
     * */
    @Test
    public void test9() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot2.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"uid\":\"epoe\"," +
                    "\"ENTPersonUid\":\"0123456789\"," +
                    "\"cn\":\"POE Edgar\"," +
                    "\"sn\":\"Poe\"," +
                    "\"givenName\":\"Edgar\"," +
                    "\"codecivilite\":\"M\"," +
                    "\"mail\":\"edgar.poe@litterature.com\"," +
                    "\"title\":\"ECR\"," +
                    "\"typensi\":\"AAF\"," +
                    "\"datenaissance\":\"19/01/1809\"," +
                    "\"Rne\":\"0350063D\""
                    + "}";

            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"objectClass\":[\"top\",\"person\",\"organizationalPerson\",\"educationnationale\",\"inetOrgPerson\"]}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /**
     * Test du mode replace versus append
     */

    /* test use case :
     * - Mode 'replace' (attributes ENTStructureClasses & ENTStructureGroupes)
     * - No difference between the LDAP entry and the input file (pivot)
     **/
    @Test
    public void test10() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot3.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"ENTStructure\",\"ENTOrganisation\",\"ENTEtablissement\"]," +
                    "\"ou\":\"0352318E\"," +
                    "\"ENTEtablissementContrat\":\"PU\"," +
                    "\"street\":\"AVENUE DU BOIS GREFFIER\"," +
                    "\"ENTOrganisationGeoLoc\":\"47.84770462898778,-1.6935174304831626\"," +
                    "\"ENTDisplayName\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureJointure\":\"3681\"," +
                    "\"description\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureNomCourant\":\"JEAN BRITO\"," +
                    "\"ENTEtablissementBassin\":\"035002BU\"," +
                    "\"telephoneNumber\":\"+33 2 99 43 31 31\"," +
                    "\"facsimileTelephoneNumber\":\"+33 2 99 43 31 30\"," +
                    "\"postalCode\":\"35470\"," +
                    "\"ENTManager\":\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                    "\"l\":\"BAIN DE BR\"," +
                    "\"ENTStructureUAI\":\"0352318E\"," +
                    "\"ENTEtablissementMinistereTutelle\":\"MEN\"," +
                    "\"ENTStructureTypeStruct\":\"LYC\"," +
                    "\"ENTStructureClasses\":[\"1 L1$1 L1$20113019110$20113019112\",\"1 STMG1$1 STMG1$21131016110\",\"1 STMG2$1 STMG2$21131016110\"]," +
                    "\"ENTStructureGroupes\":[\"1 L1_ANG RENF$$1 L1\",\"1 L1_DGEMC$$1 L1\",\"1 L1_ESP2$$1 L1\",\"1 STMG1_ESP2$$1 STMG1\"]," +
                    "\"ENTOrganisationProfils\":[\"cn=0352318E_ADF,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_DOC,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_TOUS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_SUR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr" +
                    "\",\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_ENS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"]" +
                    "}";
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            Mockito.verify(mockedDirContext, Mockito.never()).modifyAttributes(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - Mode 'append' (attributes ENTStructureClasses & ENTStructureGroupes)
     * - No difference between the LDAP entry and the input file (pivot)
     **/
    @Test
    public void test11() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot4.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"ENTStructure\",\"ENTOrganisation\",\"ENTEtablissement\"]," +
                    "\"ou\":\"0352318E\"," +
                    "\"ENTEtablissementContrat\":\"PU\"," +
                    "\"street\":\"AVENUE DU BOIS GREFFIER\"," +
                    "\"ENTOrganisationGeoLoc\":\"47.84770462898778,-1.6935174304831626\"," +
                    "\"ENTDisplayName\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureJointure\":\"3681\"," +
                    "\"description\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureNomCourant\":\"JEAN BRITO\"," +
                    "\"ENTEtablissementBassin\":\"035002BU\"," +
                    "\"telephoneNumber\":\"+33 2 99 43 31 31\"," +
                    "\"facsimileTelephoneNumber\":\"+33 2 99 43 31 30\"," +
                    "\"postalCode\":\"35470\"," +
                    "\"ENTManager\":\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                    "\"l\":\"BAIN DE BR\"," +
                    "\"ENTStructureUAI\":\"0352318E\"," +
                    "\"ENTEtablissementMinistereTutelle\":\"MEN\"," +
                    "\"ENTStructureTypeStruct\":\"LYC\"," +
                    "\"ENTStructureClasses\":[\"1 L1$1 L1$20113019110$20113019112\",\"1 STMG1$1 STMG1$21131016110\",\"1 STMG2$1 STMG2$21131016110\"]," +
                    "\"ENTStructureGroupes\":[\"1 L1_ANG RENF$$1 L1\",\"1 L1_DGEMC$$1 L1\",\"1 L1_ESP2$$1 L1\",\"1 STMG1_ESP2$$1 STMG1\"]," +
                    "\"ENTOrganisationProfils\":[\"cn=0352318E_ADF,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_DOC,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_TOUS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_SUR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr" +
                    "\",\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_ENS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"]" +
                    "}";
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            Mockito.verify(mockedDirContext, Mockito.never()).modifyAttributes(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - Mode 'replace' (attributes ENTStructureClasses & ENTStructureGroupes)
     * - Differences exist between the LDAP entry and the input file (pivot)
     *   > ENTStructureClasses : one value is removed from pivot
     *   > ENTStructureGroupes : one value is removed + one is added from pivot
     **/
    @Test
    public void test12() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot5.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"ENTStructure\",\"ENTOrganisation\",\"ENTEtablissement\"]," +
                    "\"ou\":\"0352318E\"," +
                    "\"ENTEtablissementContrat\":\"PU\"," +
                    "\"street\":\"AVENUE DU BOIS GREFFIER\"," +
                    "\"ENTOrganisationGeoLoc\":\"47.84770462898778,-1.6935174304831626\"," +
                    "\"ENTDisplayName\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureJointure\":\"3681\"," +
                    "\"description\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureNomCourant\":\"JEAN BRITO\"," +
                    "\"ENTEtablissementBassin\":\"035002BU\"," +
                    "\"telephoneNumber\":\"+33 2 99 43 31 31\"," +
                    "\"facsimileTelephoneNumber\":\"+33 2 99 43 31 30\"," +
                    "\"postalCode\":\"35470\"," +
                    "\"ENTManager\":\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                    "\"l\":\"BAIN DE BR\"," +
                    "\"ENTStructureUAI\":\"0352318E\"," +
                    "\"ENTEtablissementMinistereTutelle\":\"MEN\"," +
                    "\"ENTStructureTypeStruct\":\"LYC\"," +
                    "\"ENTStructureClasses\":[\"1 L1$1 L1$20113019110$20113019112\",\"1 STMG1$1 STMG1$21131016110\",\"1 STMG2$1 STMG2$21131016110\"]," +
                    "\"ENTStructureGroupes\":[\"1 L1_ANG RENF$$1 L1\",\"1 L1_DGEMC$$1 L1\",\"1 L1_ESP2$$1 L1\",\"1 STMG1_ESP2$$1 STMG1\"]," +
                    "\"ENTOrganisationProfils\":[\"cn=0352318E_ADF,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_DOC,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_TOUS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_SUR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr" +
                    "\",\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_ENS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"]" +
                    "}";
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            Mockito.verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{" +
                    "\"ENTStructureClasses\":[\"1 L1$1 L1$20113019110$20113019112\",\"1 STMG2$1 STMG2$21131016110\"]," +
                    "\"ENTStructureGroupes\":[\"1 L1_ANG RENF$$1 L1\",\"1 L1_DGEMC$$1 L1\",\"2 L2_ESP2$$1 L1\",\"1 STMG1_ESP2$$1 STMG1\"]," +
                    "}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - Mode 'append' (attributes ENTStructureClasses & ENTStructureGroupes)
     * - Differences exist between the LDAP entry and the input file (pivot)
     *   > ENTStructureClasses : one value is removed from pivot => no modification expected
     *   > ENTStructureGroupes : one value is removed + one is added from pivot => merge from existing LDAP & pivot input file (sum of both)
     **/
    @Test
    public void test13() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot6.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"ENTStructure\",\"ENTOrganisation\",\"ENTEtablissement\"]," +
                    "\"ou\":\"0352318E\"," +
                    "\"ENTEtablissementContrat\":\"PU\"," +
                    "\"street\":\"AVENUE DU BOIS GREFFIER\"," +
                    "\"ENTOrganisationGeoLoc\":\"47.84770462898778,-1.6935174304831626\"," +
                    "\"ENTDisplayName\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureJointure\":\"3681\"," +
                    "\"description\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureNomCourant\":\"JEAN BRITO\"," +
                    "\"ENTEtablissementBassin\":\"035002BU\"," +
                    "\"telephoneNumber\":\"+33 2 99 43 31 31\"," +
                    "\"facsimileTelephoneNumber\":\"+33 2 99 43 31 30\"," +
                    "\"postalCode\":\"35470\"," +
                    "\"ENTManager\":\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                    "\"l\":\"BAIN DE BR\"," +
                    "\"ENTStructureUAI\":\"0352318E\"," +
                    "\"ENTEtablissementMinistereTutelle\":\"MEN\"," +
                    "\"ENTStructureTypeStruct\":\"LYC\"," +
                    "\"ENTStructureClasses\":[\"1 L1$1 L1$20113019110$20113019112\",\"1 STMG1$1 STMG1$21131016110\",\"1 STMG2$1 STMG2$21131016110\"]," +
                    "\"ENTStructureGroupes\":[\"1 L1_ANG RENF$$1 L1\",\"1 L1_DGEMC$$1 L1\",\"1 L1_ESP2$$1 L1\",\"1 STMG1_ESP2$$1 STMG1\"]," +
                    "\"ENTOrganisationProfils\":[\"cn=0352318E_ADF,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_DOC,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_TOUS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_SUR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr" +
                    "\",\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_ENS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"]" +
                    "}";
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            Mockito.verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{" +
                    "\"ENTStructureGroupes\":[\"1 L1_ANG RENF$$1 L1\",\"1 L1_DGEMC$$1 L1\",\"2 L2_ESP2$$1 L1\",\"1 STMG1_ESP2$$1 STMG1\",\"1 L1_ESP2$$1 L1\"]" +
                    "}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /* test use case :
     * - Mode 'append' (attributes ENTStructureClasses & ENTStructureGroupes)
     * - Differences exist between the LDAP entry and the input file (pivot)
     *   > ENTStructureClasses : one value is added from pivot => merge from existing LDAP & pivot input file (sum of both)
     *   > ENTStructureGroupes : three values are removed from pivot => no modification expected
     **/
    @Test
    public void test14() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivot7.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"objectClass\":[\"top\",\"ENTStructure\",\"ENTOrganisation\",\"ENTEtablissement\"]," +
                    "\"ou\":\"0352318E\"," +
                    "\"ENTEtablissementContrat\":\"PU\"," +
                    "\"street\":\"AVENUE DU BOIS GREFFIER\"," +
                    "\"ENTOrganisationGeoLoc\":\"47.84770462898778,-1.6935174304831626\"," +
                    "\"ENTDisplayName\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureJointure\":\"3681\"," +
                    "\"description\":\"LYCEE GENERAL ET TECHNOLOGIQUE JEAN BRITO\"," +
                    "\"ENTStructureNomCourant\":\"JEAN BRITO\"," +
                    "\"ENTEtablissementBassin\":\"035002BU\"," +
                    "\"telephoneNumber\":\"+33 2 99 43 31 31\"," +
                    "\"facsimileTelephoneNumber\":\"+33 2 99 43 31 30\"," +
                    "\"postalCode\":\"35470\"," +
                    "\"ENTManager\":\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                    "\"l\":\"BAIN DE BR\"," +
                    "\"ENTStructureUAI\":\"0352318E\"," +
                    "\"ENTEtablissementMinistereTutelle\":\"MEN\"," +
                    "\"ENTStructureTypeStruct\":\"LYC\"," +
                    "\"ENTStructureClasses\":[\"1 L1$1 L1$20113019110$20113019112\",\"1 STMG1$1 STMG1$21131016110\",\"1 STMG2$1 STMG2$21131016110\"]," +
                    "\"ENTStructureGroupes\":[\"1 L1_ANG RENF$$1 L1\",\"1 L1_DGEMC$$1 L1\",\"1 L1_ESP2$$1 L1\",\"1 STMG1_ESP2$$1 STMG1\"]," +
                    "\"ENTOrganisationProfils\":[\"cn=0352318E_ADF,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_DOC,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_TOUS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_SUR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr" +
                    "\",\"cn=0352318E_DIR,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\",\"cn=0352318E_ENS,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"]" +
                    "}";
            PowerMockito.when(mockedDirContext.search(Matchers.anyString(), Matchers.anyString(), Matchers.any(SearchControls.class))).
                    thenReturn(buildResultSet("ou=0352318E,ou=structures,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}));
            PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches(""))).thenReturn(buildAttributes(existingEntryInLDAP));

            entry.update();

            Mockito.verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{" +
                    "\"ENTStructureClasses\":[\"1 L1$1 L1$20113019110$20113019112\",\"1 STMG1$1 STMG1$21131016110\",\"1 STMG2$1 STMG2$21131016110\",\"3 L3$1 L1$20113019110$20113019112\"]," +
                    "}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }
    
    /* test use case :
     * - Mode 'append' (attribute member)
     * - Append values already present in the values list of attributes member & ExplicitMember
     *   > ExplicitMember : the appended values are removed from the list (as the persons become 'usual' members according to their new functions, overall profile)
     *   > member : is kept unchanged
     **/
    @Test
    public void test15() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot15.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gfaure,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ExplicitMember\": []}"));
    }

    /* test use case :
     * - Mode 'append' (attributes ExplicitMember)
     * - Append values NOT already present in the values list of the attribute ExplicitMember
     *   > ExplicitMember : the appended values are added
     *   > member : is kept unchanged (no consistency control from ExplicitMember towards member attribute)
     **/
    @Test
    public void test16() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot16.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=gfaure,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ExplicitMember\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

    /* test use case :
     * - Mode 'append' (attributes ExplicitMember)
     * - Append values ALL already present in the values list of the attribute ExplicitMember
     *   > ExplicitMember : is kept unchanged
     *   > member : is kept unchanged
     **/
    @Test
    public void test17() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot17.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=gfaure,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext, never()).modifyAttributes(anyString(), anyInt(), any(Attributes.class));
    }

    /* test use case :
     * - Mode 'replace' (attributes ExplicitMember)
     * - Replace values of the attribute ExplicitMember with new ones
     *   > ExplicitMember : is updated
     *   > member : is kept unchanged (no consistency control from ExplicitMember towards member attribute)
     **/
    @Test
    public void test18() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot18.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=gfaure,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ExplicitMember\": [\"uid=fpoulenc,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

    /* test use case :
     * - Mode 'replace' (attributes ExplicitMember)
     * - Replace values of the attribute ExplicitMember with SAME ones
     *   > ExplicitMember : is kept unchanged
     *   > member : is kept unchanged
     **/
    @Test
    public void test19() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot19.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=gfaure,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext, never()).modifyAttributes(anyString(), anyInt(), any(Attributes.class));
    }

    /* test use case :
     * - Mode 'remove' (attributes member & ExplicitMember)
     * - The same value is removed from both attributes
     * - Both attributes have the same values list
     *   > ExplicitMember : is updated (the value is removed)
     *   > member : is updated (the value is removed)
     **/
    @Test
    public void test20() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot20.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"member\": [\"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"], \"ExplicitMember\": [\"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

    /* test use case :
     * - Mode 'remove' (attributes member & ExplicitMember)
     * - The same value is removed from both attributes
     * - member attribute has additional values
     *   > ExplicitMember : is updated (the value is removed)
     *   > member : is updated (the value is removed)
     **/
    @Test
    public void test21() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot21.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=fpoulenc,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"member\": [\"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=fpoulenc,ou=personnes,dc=ent-bretagne,dc=fr\"], \"ExplicitMember\": [\"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

    /* test use case :
     * - Mode 'remove' (attributes member)
     * - member attribute has additional values. One of them is removed
     *   > ExplicitMember : is kept unchanged
     *   > member : is updated (the value is removed)
     **/
    @Test
    public void test22() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot22.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=fpoulenc,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"member\": [\"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

    /* test use case :
     * - Mode 'append' (attributes member)
     * - Add one value (not already present neither in member nor ExplicitMember)
     *   > ExplicitMember : is kept unchanged
     *   > member : is updated (the value is appended)
     **/
    @Test
    public void test23() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot23.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"member\": [\"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=esati,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=fpoulenc,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

    /* test use case :
     * - Mode 'append' (attributes member)
     * - Add one value (not already present neither in member nor ExplicitMember)
     *   > ExplicitMember : is kept unchanged
     *   > member : is updated (the value is appended)
     **/
    @Test
    public void test24() throws Exception {
        final Entry mockedPivotEntry = buildFromPivot("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", "data/entry/pivot24.xml");
        
        String existingEntryInLDAP = "{" +
        		"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
                "\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
                "\"cn\": \"profil-lambda\"," +
                "\"description\": \"Un profil quelconque\"," +
                "\"ENTDisplayName\": \"Un profil quelconque\"," +
                "\"ExplicitMember\": [\"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]," +
                "\"member\": [\"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=mlegrand,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
                "}";
        
        setEntryInLdap("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", existingEntryInLDAP);
        
        mockedPivotEntry.update();
        
        verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"member\": [\"uid=pdukas,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

    @Test
    public void update_modifyModeRemove_noValue() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_noValue.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext, never()).modifyAttributes(anyString(), anyInt(), any(Attributes.class));
    }

    @Test
    public void update_modifyModeRemove_noMatch() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_noMatch.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext, never()).modifyAttributes(anyString(), anyInt(), any(Attributes.class));
    }

    @Test
    public void update_modifyModeRemove_oneValue() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_oneValue.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0350063D\", \"0360063E\"] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_manyValues() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_manyValues.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0360063E\"] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_allValues() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_allValues.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REMOVE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_applyTwice() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_applyTwice.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0350064E\"] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_withRegExp() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_withRegExp.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0360063E\"] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_withRegExp_then_modifyModeAppend_manyValues() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_withRegExp_then_modifyModeAppend_manyValues.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [ \"0360063E\", \"0350068I\", \"0351164G\"] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_oneValue_then_modifyModeAppend_otherValue() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_oneValue_then_modifyModeAppend_otherValue.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0350063D\", \"0350170L\", \"0360063E\"] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_allValues_then_modifyModeAppend_oneValue() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeRemove_allValues_then_modifyModeAppend_oneValue.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0350170L\"] }")
    			);
    	verify(mockedDirContext, never()).modifyAttributes(
    			"",
    			DirContext.REMOVE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [] }")
    			);
    }

    @Test
    public void update_modifyModeAppend_oneValue_then_modifyModeRemove_allValues() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeAppend_oneValue_then_modifyModeRemove_allValues.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext, never()).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\", \"0350170L\"] }")
    			);
    	verify(mockedDirContext).modifyAttributes(
    			"",
    			DirContext.REMOVE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [] }")
    			);
    }

    @Test
    public void update_modifyModeAppend_oneValue_then_modifyModeRemove_sameValue() throws Exception {
    	final Entry entry = buildFromPivot("data/entry/modifyModeAppend_oneValue_then_modifyModeRemove_sameValue.xml");
    	setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }");

    	entry.update();

    	verify(mockedDirContext, never()).modifyAttributes(
    			"",
    			DirContext.REPLACE_ATTRIBUTE,
    			buildAttributes("{ \"Rne\": [\"0350063D\", \"0350064E\", \"0360063E\"] }")
    			);
    }

    @Test
    public void update_modifyModeRemove_oneValues_then_modifyModeAppend_sameValue() throws Exception {
        final Entry entry = buildFromPivot("data/entry/modifyModeRemove_oneValue_then_modifyModeAppend_sameValue.xml");
        setEntryInLdap("{ \"uid\": \"epoe\", \"Rne\": [\"0350170L\", \"0360063E\"] }");

        entry.update();

        verify(mockedDirContext, never()).modifyAttributes(anyString(), anyInt(), any(Attributes.class));
    }

    /*
    Test use case :
    - when entry from LDAP and pivot only differ in case, and no attribute specifies that a modification must be case sensitive,
    entry is not updated
     */
    @Test
    public void testReplaceDefaultCase() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivotReplaceDefaultCase.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"uid\":\"id\"," +
                    "\"ENTFoo\":\"foo\"," +
                    "\"ENTBar\":\"bar\"" +
                    "}";

            setEntryInLdap(existingEntryInLDAP);

            entry.update();

            verify(mockedDirContext, never()).modifyAttributes(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /*
    Test use case :
    - when entry from LDAP and pivot only differ in case, and one attribute specifies that a modification must be case sensitive,
    this attribute must be updated
     */
    @Test
    public void testReplaceEntryDefaultCaseAttrOverride() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivotReplaceEntryDefaultCaseAttrOverride.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"uid\":\"id\"," +
                    "\"ENTFoo\":\"foo\"," +
                    "\"ENTBar\":\"bar\"" +
                    "}";

            setEntryInLdap(existingEntryInLDAP);

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ENTBar\":\"Bar\"}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /*
    Test use case :
    - when entry from LDAP and pivot only differ in case, and the entry specifies that modifications must be case sensitive,
    attributes must be updated
     */
    @Test
    public void testReplaceEntryCaseSensitive() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivotReplaceEntryCaseSensitive.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"uid\":\"id\"," +
                    "\"ENTFoo\":\"foo\"," +
                    "\"ENTBar\":\"bar\"" +
                    "}";

            setEntryInLdap(existingEntryInLDAP);

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ENTFoo\":\"Foo\", \"ENTBar\":\"Bar\"}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /*
    Test use case :
    - when entry from LDAP and pivot only differ in case, and the entry specifies that modifications must be case sensitive,
    attributes must be updated, except if they specify otherwise
     */
    @Test
    public void testReplaceEntryCaseSensitiveAttrOverride() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivotReplaceEntryCaseSensitiveAttrOverride.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"uid\":\"id\"," +
                    "\"ENTFoo\":\"foo\"," +
                    "\"ENTBar\":\"bar\"" +
                    "}";

            setEntryInLdap(existingEntryInLDAP);

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ENTFoo\":\"Foo\"}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

    /*
    Test use case :
    - when entry from LDAP and pivot only differ in case, and the entry specifies that modifications must not be case sensitive,
    attributes must not be updated, except if they specify otherwise
     */
    @Test
    public void testReplaceEntryCaseInsensitiveAttrOverride() {
        try {
            InputStream is = EntryTest.class.getClassLoader().getResourceAsStream("data/entry/pivotReplaceEntryCaseInsensitiveAttrOverride.xml");
            Element mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();

            PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn("uid=epoe,ou=personnes,dc=ent-bretagne,dc=fr");
            PowerMockito.when(mockedDirContext.lookup(Matchers.anyString())).thenReturn(mockedDirContext);

            Entry entry = new Entry(mockedPivotEntry, mockedDirContext, null, new Variables(), null, null);
            Assert.assertNotNull(entry);

            String existingEntryInLDAP = "{" +
                    "\"uid\":\"id\"," +
                    "\"ENTFoo\":\"foo\"," +
                    "\"ENTBar\":\"bar\"" +
                    "}";

            setEntryInLdap(existingEntryInLDAP);

            entry.update();

            verify(mockedDirContext).modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, buildAttributes("{\"ENTBar\":\"Bar\"}"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

}
