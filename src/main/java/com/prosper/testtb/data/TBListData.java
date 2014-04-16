package com.prosper.testtb.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.prosper.testtb.bean.TBListItem;

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
				"(url, state, ctime) " + 
				"values('" + url + "', 0, '" + time + "')";
		//System.out.println(sql);
		super.execute(sql);
	}
	
	public int getCountByUrl(String url) throws SQLException {
		ResultSet rs = super.get("select count(*) as count from " + DB_TB_LIST + " where url = '" + url + "'");
		rs.next();
		int count = rs.getInt(1);
		return count;
	}
	
	public TBListItem getByState(int state) throws SQLException {
		ResultSet rs = super.get("select * from " + DB_TB_LIST + " where state = '" + state + "' limit 1");
		if (rs.next()) {
			return new TBListItem(rs.getInt(1), rs.getString(2));
		} else {
			return null;
		}
	}
	
	public void updateState(int id, int state) throws SQLException {
		String sql = "update " + DB_TB_LIST + " set state = " + state + " where id = " + id;
		super.execute(sql);
	}
	
}
