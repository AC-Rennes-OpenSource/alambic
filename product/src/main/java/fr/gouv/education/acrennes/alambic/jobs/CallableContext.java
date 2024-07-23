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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import org.jdom2.Document;

public interface CallableContext {

    String TARGET_ENVIRONMENT = "ALAMBIC_TARGET_ENVIRONMENT";
    String KEYSTORE_PATH = "ALAMBIC_KEYSTORE_PATH";
    String DEFAULT_KEYSTORE_RELATIVE_PATH = "data/security/alambic.keystore";
    String ETL_JDBC_DRIVER = "ALAMBIC_ETL_JDBC_DRIVER";
    String ETL_JDBC_URL = "ALAMBIC_ETL_JDBC_URL";
    String ETL_JDBC_LOGIN = "ALAMBIC_ETL_JDBC_LOGIN";
    String ETL_JDBC_PASSWORD = "ALAMBIC_ETL_JDBC_PASSWORD";

    String ETL_CFG_JDBC_DRIVER = "etl.jdbc.driver";
    String ETL_CFG_JDBC_URL = "etl.jdbc.url";
    String ETL_CFG_JDBC_LOGIN = "etl.jdbc.user";
    String ETL_CFG_JDBC_PASSWORD = "etl.jdbc.password";
    String ETL_CFG_PERSISTENCE_UNIT = "etl.persistence.unit";

    Float getVersion();

    void setExecutionPath(String path);

    String getExecutionPath();

    void setVariables(Variables variables);

    Variables getVariables();

    void setJobDocument(Document document);

    Document getJobDocument();

    String resolvePath(final String path) throws AlambicException;

    String resolveString(final String name) throws AlambicException;

    CallableContext clone();

}
