package com.prosper.testtb.data;

import java.sql.SQLException;

public class TBFailedPageData extends TBData {

	private static final String DB_TB_FAILED_PAGE = "tb_failed_page";
	
	public TBFailedPageData(DBConn conn) {
		super(conn);
	}
	
	public void insert(String content) throws SQLException {
		String sql = 
				"insert into " + DB_TB_FAILED_PAGE + 
				"(content) " + 
				"values('" + content + "')";
		super.execute(sql);
	}
	
}
