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
package fr.gouv.education.acrennes.alambic.jobs;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.custommonkey.xmlunit.XMLTestCase;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.utils.Variables;

public class JobsTest extends XMLTestCase {

	@Test
	public void test1() throws IOException, AlambicException, JDOMException, SAXException, InterruptedException, ExecutionException {
		Jobs jobs = new Jobs("./", "src/test/resources/data/jobs/file1.xml", new Variables(), new Properties());
		List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("all"), "1");
		
		Assert.assertTrue(futures.size() == 1);
		Assert.assertTrue(futures.get(0).get().getTrafficLight().equals(ActivityTrafficLight.GREEN));

		assertXMLEqual(new FileReader("src/test/resources/data/jobs/expected-result-local1.xml"), 
				new FileReader("src/test/resources/data/output/result-local1.xml"));

		assertXMLEqual(new FileReader("src/test/resources/data/jobs/expected-result-local2.xml"), 
				new FileReader("src/test/resources/data/output/result-local2.xml"));

		assertXMLEqual(new FileReader("src/test/resources/data/jobs/expected-result-external1.xml"), 
				new FileReader("src/test/resources/data/output/result-external1.xml"));

		assertXMLEqual(new FileReader("src/test/resources/data/jobs/expected-result-external2.xml"), 
				new FileReader("src/test/resources/data/output/result-external2.xml"));				
	}

	@Test
	public void test2() throws IOException, JDOMException, SAXException, AlambicException, InterruptedException, ExecutionException {
		Jobs jobs = new Jobs("./", "src/test/resources/data/jobs/file3.xml", new Variables(), new Properties());
		List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("all"), "1");
		Assert.assertTrue(futures.size() == 1);
		Assert.assertTrue(futures.get(0).get().getTrafficLight().equals(ActivityTrafficLight.RED));
	}

	@Test
	public void test3() throws IOException, JDOMException, SAXException, AlambicException, InterruptedException, ExecutionException {
			Jobs jobs = new Jobs("./", "src/test/resources/data/jobs/file4.xml", new Variables(), new Properties());
			List<Future<ActivityMBean>> futures = jobs.executeJobList(Arrays.asList("all"), "1");
			Assert.assertTrue(futures.size() == 1);
			Assert.assertTrue(futures.get(0).get().getTrafficLight().equals(ActivityTrafficLight.RED));
	}

}