package com.prosper.testtb.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.prosper.testtb.bean.TBItem;

public class TBItemData extends TBData {

	private static final String DB_TB_ITEM = "tb_item";
	
	public TBItemData(DBConn dbConn) {
		super(dbConn);
	}
	
	public void insert(String url) throws SQLException {
		String sql = "insert into " + DB_TB_ITEM + "(url, state) " + "values('" + url +"', 0)";
		super.execute(sql);
	}
	
	public TBItem getByUrl(String url) throws SQLException {
		ResultSet rs = super.get("select * from " + DB_TB_ITEM + " where url = '" + url + "' limit 1");
		if (rs.next()) {
			return new TBItem(rs.getString(2));
		} else {
			return null;
		}
	}
	
}
