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
package fr.gouv.education.acrennes.alambic.jobs.load;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;

public class SqlLoader extends AbstractDestination {

	private static final Log log = LogFactory.getLog(SqlLoader.class);

	private Connection conn = null;
	private int requestCount;
	private int processedCount;
	private FileReader pivot;
	private PreparedStatement pstmt;

	public SqlLoader(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
		super(context, job, jobActivity);
		String driver = job.getChildText("driver");
		if (StringUtils.isBlank(driver)) {
			throw new AlambicException("le driver de la base de données n'est pas précisé");
		} else {
			driver = context.resolveString(driver);
		}

		String uri = job.getChildText("uri");
		if (StringUtils.isBlank(uri)) {
			throw new AlambicException("l'uri de la base de données n'est pas précisée");
		} else {
			uri = context.resolveString(uri);
		}

		String login = job.getChildText("login");
		if (StringUtils.isBlank(login)) {
			throw new AlambicException("le login d'accès à la base de données n'est pas précisé");
		} else {
			login = context.resolveString(login);
		}

		String pwd = job.getChildText("passwd");
		if (StringUtils.isBlank(pwd)) {
			throw new AlambicException("le mot de passe d'accès à la base de données n'est pas précisé");
		} else {
			pwd = context.resolveString(pwd);
		}

		String filePivot = job.getChildText("pivot");
		if (StringUtils.isBlank(filePivot)) {
			throw new AlambicException("le pivot n'est pas precisé");
		} else {
			filePivot = context.resolvePath(filePivot);
		}

		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(uri, login, pwd);
			pivot = new FileReader(filePivot);
			processedCount = 0;

			try (LineNumberReader lnr = new LineNumberReader(new FileReader(filePivot))) {
				lnr.skip(Long.MAX_VALUE);
				requestCount = lnr.getLineNumber() + 1;
			}
		} catch (Exception e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			throw new AlambicException("Failed to instanciate the SQL loader, cause: " + e.getMessage());
		}
	}

	public int getProcessedCount() {
		return processedCount;
	}

	@Override
	public void execute() throws AlambicException {

			String line;
		try (BufferedReader br = new BufferedReader(pivot)) {
			while ((line = br.readLine()) != null) {
				// activity monitoring
				jobActivity.setProcessing("Execute the query '" + line + "'");
				log.debug("Execute the query '" + line + "'");
				pstmt = conn.prepareStatement(line);
				pstmt.execute();
				jobActivity.setProcessing("Execute SQL request '" + line + "'");
				jobActivity.setProgress((++processedCount * 100) / requestCount);
			}
		} catch (Exception e) {
			jobActivity.setTrafficLight(ActivityTrafficLight.RED);
			throw new AlambicException("Failed to execute the SQL loader, cause: " + e.getMessage());
		}
	}

	@Override
	public void close() throws AlambicException {
		super.close();
		if (null != conn) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new AlambicException("Failed to close the SQL loader, cause: " + e.getMessage());
			} finally {
				conn = null;
			}
		}
	}

}
