package com.prosper.testtb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TBListData extends TBData {

	private static final String DB_TB_LIST = "tb_list";
	
	public TBListData(DBConn conn) {
		super(conn);
	}
	
	public void insertListUrl(String url) throws SQLException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		String sql = 
				"insert into " + DB_TB_LIST + 
				"(url, state, is_root, ctime) " + 
				"values('" + url + "', 1, 0, '" + time + "')";
		//System.out.println(sql);
		super.insert(sql);
	}
	
	public int getCountByUrl(String url) throws SQLException {
		ResultSet rs = super.get("select count(*) as count from " + DB_TB_LIST + " where url = '" + url + "'");
		rs.next();
		int count = rs.getInt(1);
		return count;
	}
	
}
