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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.education.acrennes.alambic.utils.Variables;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Entry.class})
public class EntryTest2 {

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

    private Entry buildFromPivot(final String nameInNamespace, final String filePath) throws Exception {
        final Element mockedPivotEntry;
        try (final InputStream is = EntryTest2.class.getClassLoader().getResourceAsStream(filePath)) {
            mockedPivotEntry = (new SAXBuilder()).build(is).getRootElement();
        }

        Variables variables = new Variables();
        try (final InputStream is = EntryTest2.class.getClassLoader().getResourceAsStream("conf/variables.xml")) {
            Element variablesElt = (new SAXBuilder()).build(is).getRootElement();
            variables.loadFromXmlNode(variablesElt.getChild("variables").getChildren("variable"));
            variables.put("ALAMBIC_TARGET_ENVIRONMENT", "DEVELOPMENT");
        }

        PowerMockito.when(mockedDirContext.getNameInNamespace())
        	.thenReturn("dc=ent-bretagne,dc=fr")
        	.thenReturn("cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr")
        	.thenReturn("cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr")
        	.thenReturn("dc=ent-bretagne,dc=fr");

        PowerMockito.when(mockedDirContext.lookup(Matchers.anyString()))
        	.thenReturn(mockedDirContext);

        return new Entry(mockedPivotEntry, mockedDirContext, null, variables, null, null);
    }

    /* test use case :
     * - Vérification du fonctionnement de la consolidation fiche 'ENTProfil' Vs fiche 'member' (via requête LDAP).
     **/
    @Test
    public void test25() throws Exception {
    	final Entry mockedPivotEntry = buildFromPivot("dc=ent-bretagne,dc=fr", "data/entry/pivot25.xml");

    	String existingEntryInLDAP = "{" +
    			"\"dn\": \"cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"," +
    			"\"objectClass\": [\"top\", \"groupOfNames\", \"ENTProfil\"]," +
    			"\"cn\": \"profil-lambda\"," +
    			"\"description\": \"Un profil quelconque\"," +
    			"\"ENTDisplayName\": \"Un profil quelconque\"," +
    			"\"ENTProfilPeuplement:\": \"implicite\"," +
    			"\"member\": [\"uid=gbizet,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\", \"uid=cdebussy,ou=personnes,dc=ent-bretagne,dc=fr\"]" +
    			"}";

    	String searchMembersResults = "{" +
    			"\"dn\": \"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\"," +
    			"\"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"inetOrgPerson\", \"ENTPerson\", \"ENTAuxEnseignant\"]," +
    			"\"cn\": \"Camille Saint-Saëns\"," +
    			"\"sn\": \"Saint-Saëns\"," +
    			"\"givenName\": \"Camille\"," +
    			"\"ENTPersonDateNaissance\": \"09/10/1835\"," +
    			"\"ENTPersonProfils\": [\"cn=alpha,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\", \"cn=beta,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"]," +
    			"\"ENTPersonFonctions:\": [\"0350063D$ADF\"]" +
    			"}";

    	PowerMockito.when(mockedDirContext.search(
    			Matchers.anyString(),
    			Matchers.anyString(),
    			Matchers.any(SearchControls.class)
    			))
	    	.thenReturn(buildResultSet("dn=cn=profil-lambda,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr", new String[]{existingEntryInLDAP}))
	    	.thenReturn(buildResultSet("uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr", new String[]{searchMembersResults}));

    	PowerMockito.when(mockedDirContext.getAttributes(Matchers.matches("")))
	    	.thenReturn(buildAttributes(existingEntryInLDAP))
	    	.thenReturn(buildAttributes(existingEntryInLDAP))
	    	.thenReturn(buildAttributes(searchMembersResults));

    	mockedPivotEntry.update();

    	verify(mockedDirContext, VerificationModeFactory.times(2)).modifyAttributes("", 
    			DirContext.REPLACE_ATTRIBUTE, 
    			buildAttributes("{\"ENTPersonProfils\": [\"cn=alpha,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\", \"cn=beta,ou=profils,ou=groupes,dc=ent-bretagne,dc=fr\"]}"));

    	verify(mockedDirContext, VerificationModeFactory.times(1)).modifyAttributes("", 
    			DirContext.REPLACE_ATTRIBUTE, 
    			buildAttributes("{\"member\": [\"uid=cs-saens,ou=personnes,dc=ent-bretagne,dc=fr\"]}"));
    }

}