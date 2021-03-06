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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

public class CsvToStateBaseTest extends TestCase {

	@Test
	public void test1() {
		try {
			CsvToStateBase conn = new CsvToStateBase("src/test/resources/data/csvtest/test.csv");
			Iterator<List<Map<String, List<String>>>> itr = conn.getPageIterator(null, null, 2, null, null);
			Assert.assertTrue(itr.hasNext());
			List<Map<String, List<String>>> list = itr.next();
			Assert.assertEquals(2, list.size());
			
			Assert.assertTrue(itr.hasNext());
			List<Map<String, List<String>>> listB = itr.next();
			Assert.assertEquals(2, listB.size());
			Assert.assertFalse(listB.containsAll(list));
			
			Assert.assertTrue(itr.hasNext());
			list = itr.next();
			Assert.assertEquals(1, list.size());
			Assert.assertFalse(list.containsAll(listB));
			
			Assert.assertFalse(itr.hasNext());
			conn.close();
		} catch (AlambicException e) {
			Assert.fail("Exception survenue pendant le test unitaire, erreur : " + e.getMessage());
		}
	}

	@Test
	public void test2() {
		try {
			CsvToStateBase conn = new CsvToStateBase("src/test/resources/data/csvtest/test.csv");
			Iterator<List<Map<String, List<String>>>> itr = conn.getPageIterator(null, null, 10, null, null);
			Assert.assertTrue(itr.hasNext());
			List<Map<String, List<String>>> list = itr.next();
			Assert.assertEquals(5, list.size());
						
			Assert.assertFalse(itr.hasNext());
			conn.close();
		} catch (AlambicException e) {
			Assert.fail("Exception survenue pendant le test unitaire, erreur : " + e.getMessage());
		}
	}

	@Test
	public void test3() {
		try {
			CsvToStateBase conn = new CsvToStateBase("src/test/resources/data/csvtest/test.csv");
			Iterator<List<Map<String, List<String>>>> itr = conn.getPageIterator(null, null, 1, null, null);
			Assert.assertTrue(itr.hasNext());
			List<Map<String, List<String>>> list = itr.next();
			Assert.assertEquals(1, list.size());
			
			Assert.assertTrue(itr.hasNext());
			List<Map<String, List<String>>> listB = itr.next();
			Assert.assertEquals(1, listB.size());
			Assert.assertFalse(listB.containsAll(list));
			
			Assert.assertTrue(itr.hasNext());
			list = itr.next();
			Assert.assertEquals(1, list.size());
			Assert.assertFalse(list.containsAll(listB));
			
			Assert.assertTrue(itr.hasNext());
			listB = itr.next();
			Assert.assertEquals(1, listB.size());
			Assert.assertFalse(listB.containsAll(list));
			
			Assert.assertTrue(itr.hasNext());
			list = itr.next();
			Assert.assertEquals(1, list.size());
			Assert.assertFalse(list.containsAll(listB));
			
			Assert.assertFalse(itr.hasNext());
			conn.close();
		} catch (AlambicException e) {
			Assert.fail("Exception survenue pendant le test unitaire, erreur : " + e.getMessage());
		}
	}

}