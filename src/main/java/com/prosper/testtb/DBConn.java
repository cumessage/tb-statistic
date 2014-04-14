package com.prosper.testtb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConn {

	private static DBConn dbConn =  new DBConn();

	private static Connection conn;

	private DBConn() {
		try {  
			Class.forName("com.mysql.jdbc.Driver");  
			setConn(DriverManager.getConnection("jdbc:mysql://localhost:3306/tb_statistic", "root", "11110000"));  
		} catch (ClassNotFoundException e) {
			System.out.println("class not found");
		} catch (SQLException e) {  
			System.out.println("get connection failed" + e.getMessage());  
			throw new RuntimeException(e);
		}
	}

	public static DBConn getInstance() {
		return dbConn;
	}
	
	public static void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			System.out.println("db conn is not right closed");
		}
	}

	public static Connection getConn() {
		return conn;
	}

	public static void setConn(Connection conn) {
		DBConn.conn = conn;
	}  
}
