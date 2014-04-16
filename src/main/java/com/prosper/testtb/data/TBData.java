package com.prosper.testtb.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TBData {
	private Connection conn;

	public TBData(DBConn conn) {
		this.conn = DBConn.getConn();
	}
	
	public ResultSet get(String sql) throws SQLException {  
		Statement st = (Statement) conn.createStatement();
		ResultSet rs = st.executeQuery(sql);
		return rs;
    }  
	
	public void execute(String sql) throws SQLException {  
		Statement st = (Statement) conn.createStatement();
		int count = st.executeUpdate(sql);  
    }  
	
}
