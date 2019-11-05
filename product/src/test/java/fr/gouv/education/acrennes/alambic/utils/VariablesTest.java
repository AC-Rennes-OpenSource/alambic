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

import org.junit.Assert;
import org.junit.Test;

public class VariablesTest {

	@Test
	public void testResolvStringCasNominal() {
		final Variables variables = new Variables();
		variables.put("KEY1", "val1");
		variables.put("KEY2", "3");
		variables.put("KEY3", "val3");
		variables.put("KEY3_bis", "%KEY3%");

		String resolvString = variables.resolvString("toto");
		Assert.assertEquals("toto", resolvString);

		resolvString = variables.resolvString("toto %KEY1%");
		Assert.assertEquals("toto val1", resolvString);

		resolvString = variables.resolvString("toto %KEY3_bis%");
		Assert.assertEquals("toto val3", resolvString);

		resolvString = variables.resolvString("toto %KEY3_bis%");
		Assert.assertEquals("toto val3", resolvString);

		resolvString = variables.resolvString("%KEY%KEY2%_bis%");// équivalent à la clé KEY3_bis
		Assert.assertEquals("val3", resolvString);

	}
}
