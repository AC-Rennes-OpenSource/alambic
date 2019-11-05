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
package fr.gouv.education.acrennes.alambic.nuxeo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.model.FileBlob;

public class OperationGetLocalFile implements AlambicOperation {

	protected static final Log log = LogFactory.getLog(OperationGetLocalFile.class);

	private static final String jsonDescription = "{"
			+ "\"id\":\"Alambic.GetLocalFile\","
			+ "\"label\":\"\","
			+ "\"category\":\"Alambic\","
			+ "\"requires\":null,"
			+ "\"description\":\"description\","
			+ "\"signature\":[\"void\",\"blob\"],"
			+ "\"url\":\"" + OperationGetLocalFile.class.getName() + "\","
			+ "\"params\":["
			+ "{"
			+ "\"name\":\"path\","
			+ "\"description\":\"\","
			+ "\"type\":\"string\","
			+ "\"required\":true,"
			+ "\"widget\":null,"
			+ "\"order\":0,"
			+ "\"values\":[]"
			+ "},"
			+ "{"
			+ "\"name\":\"fileName\","
			+ "\"description\":\"\","
			+ "\"type\":\"string\","
			+ "\"required\":false,"
			+ "\"widget\":null,"
			+ "\"order\":0,"
			+ "\"values\":[]"
			+ "},"
			+ "{"
			+ "\"name\":\"mimeType\","
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
		FileBlob blob = null;

		Map<String, Object> parameters = request.getParameters();
		String paramPath = (String) parameters.get("path");
		String paramFileName = (String) parameters.get("fileName");
		String paramMimeType = (String) parameters.get("mimeType");

		Path path = Paths.get(paramPath);
		if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			blob = new FileBlob(path.toFile());
			if (StringUtils.isNotBlank(paramFileName)) {
				blob.setFileName(paramFileName);
			}

			if (StringUtils.isBlank(paramMimeType)) {
				try {
					String probedMimeType = Files.probeContentType(path);
					blob.setMimeType(probedMimeType);
				} catch (IOException e) {
					log.error("Failed to probe the mime-type of file '" + path + "', error: " + e.getMessage());
				}
			} else {
				blob.setMimeType(paramMimeType);
			}
		} else {
			log.error("The local file '" + path.toString() + "' doesn't exists");
		}

		return blob;
	}

}
