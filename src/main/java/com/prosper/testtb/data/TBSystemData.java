package com.prosper.testtb.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.prosper.testtb.bean.TBSystem;

public class TBSystemData extends TBData {

	private static final String DB_TB_SYSTEM = "tb_system";
	
	public TBSystemData(DBConn dbConn) {
		super(dbConn);
	}
	
	public TBSystem get() throws SQLException {
		String sql = "select * from " + DB_TB_SYSTEM + " where id = 1";
		ResultSet rs = super.get(sql);
		if (rs.next()) {
			return new TBSystem(rs.getInt(2));
		} else {
			return null;
		}
	}
	
	public void insert(int state) throws SQLException {
		String sql = "insert into " + DB_TB_SYSTEM + "(id, state) values(1, " + state + ")";
		super.execute(sql);
	}
	
	public void update(int state) throws SQLException {
		String sql = "update " + DB_TB_SYSTEM + " set state=" + state + " where id = 1";
		super.execute(sql);
	}
	
}
