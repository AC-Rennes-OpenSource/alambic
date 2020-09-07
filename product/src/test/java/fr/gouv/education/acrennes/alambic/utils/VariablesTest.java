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

import static org.junit.Assert.fail;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.junit.Assert;
import org.junit.Test;

public class VariablesTest {

	@Test
	public void testResolvStringCasNominal() throws AlambicException {
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

	@Test
	public void testResolvStringCasNominal2() throws AlambicException {
		final Variables variables = new Variables();
		variables.put("DEV", "DEV_VAR");
		variables.put("VAR_DEV_VAR", "BINGO!");

		String resolvString = variables.resolvString("%VAR_%DEV%%");
		Assert.assertEquals("BINGO!", resolvString);
	}

	@Test
	public void testResolvStringLoopCase1() throws AlambicException {
		final Variables variables = new Variables();
		variables.put("KEY1", "%KEY2%");
		variables.put("KEY2", "%KEY1%");

		try {
			variables.resolvString("toto %KEY1%");
			fail("An exception should be raised !");
		} catch (AlambicException e) {
			System.out.println(String.format("Correctly catched expected exception : %s", e.getMessage()));
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testResolvStringLoopCase2() throws AlambicException {
		final Variables variables = new Variables();
		variables.put("KEY1", "%KEY1%");

		try {
			variables.resolvString("toto %KEY1%");
			fail("An exception should be raised !");
		} catch (AlambicException e) {
			System.out.println(String.format("Correctly catched expected exception : %s", e.getMessage()));
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testResolvStringLoopCase3() throws AlambicException {
		final Variables variables = new Variables();
		variables.put("KEY1", "%KEY1%");

		try {
			variables.resolvString("%KEY1%");
			fail("An exception should be raised !");
		} catch (AlambicException e) {
			System.out.println(String.format("Correctly catched expected exception : %s", e.getMessage()));
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testResolvStringLoopCase4() throws AlambicException {
		final Variables variables = new Variables();
		variables.put("KEY1", "%KEY2%");
		variables.put("KEY2", "%KEY4%");
		variables.put("KEY3", "%KEY2%");
		variables.put("KEY4", "%KEY3%");
		
		try {
			variables.resolvString("%KEY1%");
			fail("An exception should be raised !");
		} catch (AlambicException e) {
			System.out.println(String.format("Correctly catched expected exception : %s", e.getMessage()));
			Assert.assertTrue(true);
		}
	}
}
