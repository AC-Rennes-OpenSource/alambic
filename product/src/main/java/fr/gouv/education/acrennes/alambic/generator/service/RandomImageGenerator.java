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
package fr.gouv.education.acrennes.alambic.generator.service;

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService.GENERATOR_TYPE;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomEntity;
import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.EntityManager;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RandomImageGenerator extends AbstractRandomGenerator {

    public static final String LOREM_IPSUM_IMAGE_URL_PATTERN = "https://picsum.photos/%s/%s";
    public static final int CONNECTION_TIMEOUT = 5000; // ms
    public static final int READ_TIMEOUT = 60000; // ms
    private static final Log log = LogFactory.getLog(RandomImageGenerator.class);

    public RandomImageGenerator(final EntityManager em) throws AlambicException {
        super(em);
    }

    @Override
    public RandomEntity getEntity(Map<String, Object> query, String processId, UNICITY_SCOPE scope) throws AlambicException {
        RandomEntity entity;
        Path imageFile = null;

        String height = (String) query.get("height");
        String width = (String) query.get("width");
        String path = (String) query.get("path");
        if (StringUtils.isBlank(height) || StringUtils.isBlank(width)) {
            throw new AlambicException("The two parameters 'height' and 'width' must be set");
        }

        File target_directory;
        if (StringUtils.isBlank(path)) {
            target_directory = FileUtils.getTempDirectory();
            log.info("Use the system temporary path '" + target_directory.getPath() + "'");
        } else {
            target_directory = new File(path);
        }

        if (target_directory.exists() && target_directory.isDirectory()) {
            try {
                // create the image file
                imageFile = Files.createTempFile(target_directory.toPath(), null, ".jpg");

                // download a lorem ipsum image
                FileUtils.copyURLToFile(
                        new URL(String.format(LOREM_IPSUM_IMAGE_URL_PATTERN, width, height)),
                        imageFile.toFile(),
                        CONNECTION_TIMEOUT,
                        READ_TIMEOUT);
            } catch (Exception e) {
                throw new AlambicException("Failed to generate a random image, error : " + e.getMessage());
            }
        } else {
            throw new AlambicException("The path '" + target_directory.getPath() + "' doesn't deal with a directory");
        }

        entity = new RandomLambdaEntity("{\"file\":\"" + imageFile + "\"}");
        return entity;
    }

    @Override
    public GENERATOR_TYPE getType(final Map<String, Object> query) {
        return RandomGeneratorService.GENERATOR_TYPE.IMAGE;
    }

    @Override
    public String getCapacityFilter(Map<String, Object> query) {
        return Constants.UNLIMITED_GENERATOR_FILTER;
    }

}