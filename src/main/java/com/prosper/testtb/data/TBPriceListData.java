package com.prosper.testtb.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.prosper.testtb.bean.TBPriceListItem;

public class TBPriceListData extends TBData {

	private static final String DB_TB_PRICE_LIST = "tb_price_list";
	
	public TBPriceListData(DBConn conn) {
		super(conn);
	}
	
	public TBPriceListItem getByState(int state) throws SQLException {
		ResultSet rs = super.get("select * from " + DB_TB_PRICE_LIST + " where state = '" + state + "' limit 1");
		if (rs.next()) {
			return new TBPriceListItem(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6),rs.getInt(7));
		} else {
			return null;
		}
	}
	
	public void insertListUrl(String url, int minPrice, int maxPrice, int totalPage) throws SQLException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		String sql = 
				"insert into " + DB_TB_PRICE_LIST + 
				"(url, state, price_min, price_max, total_page, exe_page, ctime, etime) " + 
				"values('" + url + "', 0, " + minPrice + ", " + maxPrice + ", " + totalPage + ", 1, '" + time + "', 0)";
		super.execute(sql);
	}
	
	public void updateState(int id, int state) throws SQLException {
		String sql = "update " + DB_TB_PRICE_LIST + " set state = " + state + " where id = " + id;
		super.execute(sql);
	}
	
	public void updateExePage(int id, int page) throws SQLException {
		String sql = "update " + DB_TB_PRICE_LIST + " set exe_page = " + page + " where id = " + id;
		super.execute(sql);
	}
	
	public void deleteByUrl(String url) throws SQLException {
		String sql = "delete from " + DB_TB_PRICE_LIST + " where url = '" + url + "'";
		super.execute(sql);
	}
	
}
