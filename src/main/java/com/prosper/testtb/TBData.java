package com.prosper.testtb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TBData {
	
	
	private static String db_tb_item = "tb_item";
	
	private static Connection conn;

	public TBData() {
		try {
			open();
		} catch(SQLException e) {
			throw new RuntimeException("init data failed");
		}
	}
	
	public static void update(String sql) throws SQLException {  
		Statement st = (Statement) conn.createStatement();   
		int count = st.executeUpdate(sql);  
    }  
	
	public void insert(String sql) throws SQLException {
		check();
		Statement st = (Statement) conn.createStatement();  
		int count = st.executeUpdate(sql);    
	}
	
	public void open() throws SQLException {  
        try {  
            Class.forName("com.mysql.jdbc.Driver");  
            conn = DriverManager.getConnection(  
                    "jdbc:mysql://localhost:3306/db_tb", "root", "11110000");  
        } catch (ClassNotFoundException e) {
        	System.out.println("class not found");
        } catch (SQLException e) {  
            System.out.println("get connection failed" + e.getMessage());  
            throw e;
        }
    }  
	
	public void close() throws SQLException {
		conn.close();
	}
	
	public void check() throws SQLException {
		if (conn == null) {
			open();
		}
	}
	
}
