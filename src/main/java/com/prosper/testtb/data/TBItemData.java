package com.prosper.testtb.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.prosper.testtb.bean.TBItem;

public class TBItemData extends TBData {

	private static final String DB_TB_ITEM = "tb_item";
	
	public TBItemData(DBConn dbConn) {
		super(dbConn);
	}
	
	public int insert(TBItem tbItem) throws SQLException {
		String sql = 
				"insert into " + DB_TB_ITEM + 
				"(item_id, url, title, price, shop_level, trade_count, trade_succ_count, state) " + 
				"values(" 
					+ tbItem.itemId + ",'" 
					+ tbItem.url + "','" 
					+ tbItem.title + "'," 
					+ tbItem.price + ",'" 
					+ tbItem.shopLevel + "'," 
					+ tbItem.trade_count + "," 
					+ tbItem.trade_succ_count + ","
					+ tbItem.state + ")";
		return super.execute(sql);
	}
	
	public TBItem getById(long id) throws SQLException {
		ResultSet rs = super.get("select * from " + DB_TB_ITEM + " where item_id = '" + id + "' limit 1");
		if (rs.next()) {
			TBItem tbItem = new TBItem();
			tbItem.itemId = rs.getLong(2);
			return tbItem;
		} else {
			return null;
		}
	}
	
}
