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
package fr.gouv.education.acrennes.alambic.utils;

import java.io.File;
import junit.framework.Assert;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import org.junit.Before;
import org.junit.Test;

public class XOMTest {

	private File xmlFile;

	@Before
	public void setUp() {
		xmlFile = new File("src/test/resources/data/entry/liste-disciplines.xml");
	}

	@Test
	public void test1() {
		final String EXPECTED_NODE_RESULT = "<discipline clé=\"arts-plastiques\" titre=\"Arts plastiques\"><inspecteur><codes><code value=\"I1800\" /></codes></inspecteur><enseignant><codes><code value=\"L1800\" /></codes></enseignant></discipline>";

		try {
			Document document = new Builder().build(xmlFile);
			Nodes nodes = document.query("//discipline[enseignant/codes/code[@value='L1800']]");
			Assert.assertEquals(1, nodes.size());

			Element elt = (Element) nodes.get(0);
			Assert.assertEquals(EXPECTED_NODE_RESULT.replaceAll("\\s", ""), elt.toXML().replaceAll("[\\s\\n]", ""));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

}
