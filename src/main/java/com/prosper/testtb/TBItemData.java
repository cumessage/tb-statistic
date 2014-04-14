package com.prosper.testtb;

public class TBItemData extends TBData {

	private static final String DB_TB_ITEM = "tb_item";
	
	public TBItemData(DBConn dbConn) {
		super(dbConn);
	}
	
	public void insertList(String url) {
		String sql = "insert into " + DB_TB_ITEM + "(url, state, ctime) "
				+ "values(" + url +", 1, " + System.currentTimeMillis() + ")";
	}
	
}
