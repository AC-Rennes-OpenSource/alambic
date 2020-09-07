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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SqlToStateBase implements IToStateBase {

	private static final Log log = LogFactory.getLog(SqlToStateBase.class);

	private List<Map<String, List<String>>> stateBase = new ArrayList<>();
	private PreparedStatement pstmt;
	private Connection conn;
	private ResultSet rs;

	public SqlToStateBase(final String driver, final String uri) throws SQLException, ClassNotFoundException {
		Class.forName(driver);
		conn = DriverManager.getConnection(uri);
	}

	public SqlToStateBase(final String driver, final String uri, final String user, final String password) throws SQLException, ClassNotFoundException {
		Class.forName(driver);
		conn = DriverManager.getConnection(uri, user, password);
	}

	@Override
	public void executeQuery(final String query) {
		executeQuery(query, null);
	}

	@Override
	public void executeQuery(final String query, final String scope) {
		stateBase = new ArrayList<>();
		try {
			pstmt = conn.prepareStatement(query);
			try {
				rs = pstmt.executeQuery();
				try {
					ResultSetMetaData rsmd = rs.getMetaData();
					int numberOfColumns = rsmd.getColumnCount();
					while (rs.next()) {
						HashMap<String, List<String>> h = new HashMap<>();
						for (int i = 1; i <= numberOfColumns; i++) {
							String s = rs.getString(i);
							if (s == null) {
								s = "";
							}
							List<String> l = new ArrayList<>();
							l.add(s.trim());
							h.put(rsmd.getColumnLabel(i), l);
						}
						stateBase.add(h);
					}
				} finally {
					rs.close();
				}
			} finally {
				pstmt.close();
			}
		} catch (Exception e) {
			log.error("Failed to execute the query '" + query + "', error: " + e.getMessage());
		}
	}

	@Override
	public List<Map<String, List<String>>> getStateBase() {
		return stateBase;
	}

	@Override
	public void close() {
		if (null != conn) {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("Failed to close the SQL client, error:" + e.getMessage(), e);
			} finally {
				conn = null;
			}
		}
	}

	@Override
	public int getCountResults() {
		return stateBase.size();
	}

	@Override
	public void clear() {
		stateBase.clear();
	}

	@Override
	public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize, final String sortBy, final String orderBy)
			throws AlambicException {
		throw new AlambicException("Not implemented operation");
	}

}