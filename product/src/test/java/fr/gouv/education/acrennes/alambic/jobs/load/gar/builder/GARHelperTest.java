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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import org.junit.Assert;
import org.junit.Test;

public class GARHelperTest {

	@Test
	public void test1() {
		String uai = GARHelper.getInstance().extractCodeGroup("0352533N$", 0);
		String code = GARHelper.getInstance().extractCodeGroup("0352533N$", 1);
		Assert.assertEquals("0352533N", uai);
		Assert.assertNull(code);
	}

	@Test
	public void test2() {
		String uai = GARHelper.getInstance().extractCodeGroup("0352533N$ENS", 0);
		String code = GARHelper.getInstance().extractCodeGroup("0352533N$ENS", 1);
		Assert.assertEquals("0352533N", uai);
		Assert.assertEquals("ENS", code);
	}

	@Test
	public void test3() {
		String uai = GARHelper.getInstance().extractCodeGroup("0352533N$ENS$LP", 0);
		String code = GARHelper.getInstance().extractCodeGroup("0352533N$ENS$LP", 1);
		String type = GARHelper.getInstance().extractCodeGroup("0352533N$ENS$LP", 2);
		Assert.assertEquals("0352533N", uai);
		Assert.assertEquals("ENS", code);
		Assert.assertEquals("LP", type);
	}

	@Test
	public void test4() {
		String uai = GARHelper.getInstance().extractCodeGroup("0352533N$$LP", 0);
		String code = GARHelper.getInstance().extractCodeGroup("0352533N$$LP", 1);
		String type = GARHelper.getInstance().extractCodeGroup("0352533N$$LP", 2);
		Assert.assertEquals("0352533N", uai);
		Assert.assertEquals("", code);
		Assert.assertEquals("LP", type);
	}

}