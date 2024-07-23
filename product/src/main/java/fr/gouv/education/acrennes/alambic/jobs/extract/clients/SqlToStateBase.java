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

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.*;

public class SqlToStateBase implements IToStateBase {

    private static final Log log = LogFactory.getLog(SqlToStateBase.class);

    private enum PaginationMethod {
        ORACLE11G {
            @Override
            public String paginationQuery(String query, int offSet, int pageSize, int total) {
                return String.format(
                        "SELECT * FROM (SELECT t.*, rownum AS num__ FROM (%s) t) WHERE num__ BETWEEN %d AND %d",
                        query,
                        offSet + 1,
                        Math.min(offSet + pageSize, total));
            }
        },
        OFFSET {
            @Override
            public String paginationQuery(String query, int offSet, int pageSize, int total) throws AlambicException {
                return String.format("SELECT * FROM (%s) LIMIT %d OFFSET %d", query, pageSize, offSet);
            }
        },
        NONE {
            @Override
            public String paginationQuery(String query, int offSet, int pageSize, int total) throws AlambicException {
                throw new AlambicException("Not implemented operation");
            }
        };

        public abstract String paginationQuery(String query, int offSet, int pageSize, int total) throws AlambicException;
    }

    private List<Map<String, List<String>>> stateBase = new ArrayList<>();
    private PreparedStatement pstmt;
    private Connection conn;
    private ResultSet rs;
    private final PaginationMethod paginationMethod;

    public SqlToStateBase(final String driver, final String uri, final String paginationMethod) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        conn = DriverManager.getConnection(uri);
        this.paginationMethod = PaginationMethod.valueOf(paginationMethod.toUpperCase());
    }

    public SqlToStateBase(final String driver, final String uri, final String paginationMethod, final String user, final String password)
            throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        conn = DriverManager.getConnection(uri, user, password);
        this.paginationMethod = PaginationMethod.valueOf(paginationMethod.toUpperCase());
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
    public Iterator<List<Map<String, List<String>>>> getPageIterator(final String query, final String scope, final int pageSize,
                                                                     final String sortBy, final String orderBy)
            throws AlambicException {
        return new SQLResultsPageIterator(query, pageSize, paginationMethod);
    }

    public class SQLResultsPageIterator implements Iterator<List<Map<String, List<String>>>> {
        private final Log log = LogFactory.getLog(SQLResultsPageIterator.class);

        private List<Map<String, List<String>>> entries;
        private final int pageSize;
        private int offset;
        private final int total;
        private final PaginationMethod paginationMethod;
        private final String query;

        public SQLResultsPageIterator(final String query, final int pageSize, final PaginationMethod paginationMethod) throws AlambicException {
            this.pageSize = pageSize;
            this.entries = Collections.emptyList();
            this.offset = 0;
            this.paginationMethod = paginationMethod;
            this.query = query;

            String countQuery = String.format("SELECT COUNT(*) AS COUNT FROM (%s)", query);

            executeQuery(countQuery);
            this.entries = getStateBase();
            if (!this.entries.isEmpty()) {
                String countString = this.entries.get(0).get("COUNT").get(0);
                if (StringUtils.isNotBlank(countString)) {
                    this.total = Integer.parseInt(countString);
                } else {
                    throw new AlambicException("Failed to instanciate the SQL source page iterator. Blank count while getting the resultset total " +
                                               "size (query is '" + countQuery + "')");
                }
            } else {
                throw new AlambicException("Failed to instanciate the SQL source page iterator. Empty result while getting the resultset total size" +
                                           " (query is '" + countQuery + "')");
            }
        }

        @Override
        public boolean hasNext() {
            return (this.offset <= this.total);
        }

        @Override
        public List<Map<String, List<String>>> next() {
            entries.clear();
            try {
                executeQuery(paginationMethod.paginationQuery(query, offset, pageSize, total));
                entries = getStateBase();
                offset += pageSize;
                return entries;
            } catch (AlambicException e) {
                throw new RuntimeException(e);
            }
        }
    }

}