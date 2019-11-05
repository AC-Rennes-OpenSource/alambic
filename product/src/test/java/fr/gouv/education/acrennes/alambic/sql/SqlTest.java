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
package fr.gouv.education.acrennes.alambic.sql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class SqlTest {

	public static void main(String[] args) {
	        String driver = "com.mysql.jdbc.Driver";
	        String url    = "jdbc:mysql://lxseriah3.in.ac-rennes.fr/sasper"; // Change it to your database name
	        String username = "loc";
	        String password = "***"; // Change it to your Password
	        System.setProperty(driver,"");

	        try {
				Connection connection = DriverManager.getConnection(url,username,password);
				PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM bureau;");
                ResultSet rs = pstmt.executeQuery();
                
              //tranfo vers format extration g�n�rique
        		ResultSetMetaData rsmd = rs.getMetaData();
        	    int numberOfColumns = rsmd.getColumnCount();
        		while(rs.next()){
        			for (int i=1;i<=numberOfColumns;i++){
        				String s = rs.getString(i);
        				if (s==null) s="";
        			
        				System.out.print(rsmd.getColumnLabel(i) +" : "+ s+ "\r");
        			}
        			System.out.print("\n");
        			
                }
                
                rs.close();
                pstmt.close();
                connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

	    }
	}

