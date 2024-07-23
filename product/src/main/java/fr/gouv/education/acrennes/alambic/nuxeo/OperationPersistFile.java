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
package fr.gouv.education.acrennes.alambic.nuxeo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.model.FileBlob;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

public class OperationPersistFile implements AlambicOperation {

    protected static final Log log = LogFactory.getLog(OperationPersistFile.class);

    private static final String jsonDescription = "{"
                                                  + "\"id\":\"Alambic.PersistFile\","
                                                  + "\"label\":\"\","
                                                  + "\"category\":\"Alambic\","
                                                  + "\"requires\":null,"
                                                  + "\"description\":\"description\","
                                                  + "\"signature\":[\"blob\",\"blob\"],"
                                                  + "\"url\":\"" + OperationPersistFile.class.getName() + "\","
                                                  + "\"params\":["
                                                  + "{"
                                                  + "\"name\":\"destination\","
                                                  + "\"description\":\"\","
                                                  + "\"type\":\"string\","
                                                  + "\"required\":true,"
                                                  + "\"widget\":null,"
                                                  + "\"order\":0,"
                                                  + "\"values\":[]"
                                                  + "},"
                                                  + "{"
                                                  + "\"name\":\"filename\","
                                                  + "\"description\":\"\","
                                                  + "\"type\":\"string\","
                                                  + "\"required\":false,"
                                                  + "\"widget\":null,"
                                                  + "\"order\":0,"
                                                  + "\"values\":[]"
                                                  + "}"
                                                  + "]"
                                                  + "}";

    public static String getJSONDescription() {
        return jsonDescription;
    }

    @Override
    public Object execute(final OperationRequest request) {
        Map<String, Object> parameters = request.getParameters();
        FileBlob srcFile = (FileBlob) request.getInput();
        String paramFileName = (String) parameters.get("filename");
        String filename = (StringUtils.isNotBlank(paramFileName)) ? paramFileName : srcFile.getFileName();
        String paramDest = (String) parameters.get("destination");
        if (!paramDest.endsWith("/")) {
            paramDest = paramDest.concat("/");
        }

        try {
            File directory = new File(paramDest);
            directory.mkdirs();

            Path destPath = Paths.get(paramDest.concat(filename));
            if (!Files.exists(destPath, LinkOption.NOFOLLOW_LINKS)) {
                destPath = Files.createFile(Paths.get(paramDest.concat(filename)));
            }
            Files.copy(srcFile.getStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileAlreadyExistsException e) {
            // no op
        } catch (IOException e) {
            log.error("Failed to persist the file '" + filename + "', error:" + e.getMessage());
        }

        return request.getInput();
    }
}