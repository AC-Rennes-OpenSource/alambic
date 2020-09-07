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
package fr.gouv.education.acrennes.alambic.jobs.ftl;

import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.jobs.extract.clients.CsvToStateBase;
import junit.framework.TestCase;

public class CsvToModelTest extends TestCase {
	private CsvToStateBase cm;

	public void testCsvToModel() {
		cm = new CsvToStateBase("src/test/resources/data/temp/persons-list.csv");
		assertNotNull(cm);

		try {
			cm.executeQuery(null);
			List<Map<String, List<String>>> sb = cm.getStateBase();
			assertEquals(2220, sb.size());
		} finally {
			if (null != cm) {
				cm.close();
			}
		}
	}

}
