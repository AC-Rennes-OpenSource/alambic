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
package fr.gouv.education.acrennes.alambic.random.service;


import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGenerator.UNICITY_SCOPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.generator.service.RandomImageGenerator;
import fr.gouv.education.acrennes.alambic.persistence.EntityManagerHelper;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;

import static org.mockito.ArgumentMatchers.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RandomImageGenerator.class, FileUtils.class, Files.class})
@PowerMockIgnore("javax.management.*")
public class RandomImageGeneratorTest {

	// private EntityManagerFactory emf;
	private static final String UNIT_TEST_PERSISTENCE_UNIT = "TEST_PERSISTENCE_UNIT";

	private RandomGenerator rg;

	@Before
	public void setUp() throws AlambicException {
		PowerMockito.mockStatic(Files.class);
		PowerMockito.mockStatic(FileUtils.class);
		
		// Mock the entity manager helper so that the embedded persistence unit (derby) is used
		EntityManagerHelper.getInstance(UNIT_TEST_PERSISTENCE_UNIT, null);
		rg = RandomGeneratorService.getRandomGenerator(GENERATOR_TYPE.IMAGE);
	}

	/**
	 * Request one random image
	 * Path parameter is specified
	 * @throws Exception 
	 */
	@Test
	public void test1() throws Exception {
		File mockedImageFile = new File("./4568290141604.jpg");
		PowerMockito.when(Files.createTempFile(any(Path.class), any(), anyString())).thenReturn(mockedImageFile.toPath());
		PowerMockito.doNothing().when(FileUtils.class, "copyURLToFile", any(URL.class), any(File.class), anyInt(), anyInt());
		
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":1,\"width\":\"600\",\"height\":\"400\",\"path\":\".\"}", "PROCESS_TESTU", UNICITY_SCOPE.PROCESS);
			Assert.assertTrue(1 == entities.size());
			Assert.assertEquals("{\"file\":\"./4568290141604.jpg\"}", entities.get(0).getJson());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Request two random images
	 * Path parameter is not specified
	 * @throws Exception 
	 */
	@Test
	public void test2() throws Exception {
		File mockedImageFile = new File("./87659083456.jpg");
		PowerMockito.when(Files.createTempFile(any(Path.class), any(), anyString())).thenReturn(mockedImageFile.toPath());
		PowerMockito.when(FileUtils.class, "getTempDirectory").thenReturn(new File ("."));
		PowerMockito.doNothing().when(FileUtils.class, "copyURLToFile", any(URL.class), any(File.class), anyInt(), anyInt());
		
		try {
			List<RandomEntity> entities = rg.getEntities("{\"blurid\":\"1\",\"count\":2,\"width\":\"600\",\"height\":\"400\"}", "PROCESS_TESTU", UNICITY_SCOPE.NONE);
			Assert.assertTrue(2 == entities.size());
			Assert.assertEquals("{\"file\":\"./87659083456.jpg\"}", entities.get(0).getJson());
			Assert.assertEquals("{\"file\":\"./87659083456.jpg\"}", entities.get(1).getJson());
		} catch (AlambicException e) {
			Assert.fail(e.getMessage());
		}
	}

}