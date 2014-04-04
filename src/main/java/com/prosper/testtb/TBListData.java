package com.prosper.testtb;

public class TBListData extends TBData {

	private static final String DB_TB_LIST = "tb_list";
	
	private static TBListData instance =  new TBListData();
	
	public TBListData() {
		super();
	}
	
	public static TBData getInstance() throws Exception {
		return instance;
	}
	
	public void insertList(String url) {
		String sql = "insert into " + DB_TB_LIST + "(url, state, ctime) "
				+ "values(" + url +", 1, " + System.currentTimeMillis() + ")";
	}
	
}
