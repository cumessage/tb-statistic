package com.prosper.testtb;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prosper.testtb.bean.TBListItem;
import com.prosper.testtb.data.DBConn;
import com.prosper.testtb.data.TBListData;
import com.prosper.testtb.data.TBPriceListData;

public class PriceListRunner implements Runnable {

	private static Logger log = LogManager.getLogger(PriceListRunner.class);

	private DBConn dbConn = DBConn.getInstance();
	private TBListData tbListData = new TBListData(dbConn);
	private TBPriceListData tbPriceListData = new TBPriceListData(dbConn);

	private String name;

	public PriceListRunner(String name) {
		this.name = name;
	}

	public void run() {
		int id = -1;
		while(true) {
			try {
				TBListItem tbListItem = null;
				synchronized (dbConn) {
					tbListItem = tbListData.getByState(0);
					if (tbListItem == null) {
						tbListItem = tbListData.getByState(1);
						if (tbListItem == null) {
							return;
						} else {
							Thread.sleep(300000);
							continue;
						}
					}
					tbListData.updateState(tbListItem.getId(), 1);
				}
				String url = tbListItem.getUrl();
				id = tbListItem.getId();
				tbPriceListData.deleteByUrl(url);
				log.info(name + " deleting: " + url);
				log.info(name + " proceeding: " + url);
				runForPriceListUrl(url);
				tbListData.updateState(tbListItem.getId(), 2);
			} catch (Exception e) {
				if (id == -1) {
					log.error("get id error");
				} else {
					try {
						tbListData.updateState(id, 0);
					} catch (SQLException e1) {
						log.error("update id error: id:" + id);
					}
				}
				log.error("execute id failed, id=" + id);
				e.printStackTrace();
			}
		} 
	}

	public void runForPriceListUrl(String url) throws Exception {
		int begin = 0, end = 1, lastUnReachPos = 0, lastReachedPos = 0, max = 100000000;
		while (end <= max) {
			String exeUrl = url + "&filter=reserve_price%5B" + begin + "%2C" + end + "%5D";
			log.info(name + " trying: min=" + begin + ", max=" + end + ", url" + exeUrl);
			int totalPage = getTotalPage(exeUrl);
			if (totalPage < 100) {
				if (end == lastUnReachPos) {
					exeUrl = url + "&filter=reserve_price%5B" + begin + "%2C" + end + "%5D";
					totalPage = getTotalPage(exeUrl);
					tbPriceListData.insertListUrl(url, begin, end, totalPage);
					lastUnReachPos = 0; 
					lastReachedPos = 0;
					begin = end ++;
				} else {
					lastUnReachPos = end;
					if (lastReachedPos == 0) {
						end = (end - begin) * 2 + begin;
						if (end > max) {
							end = max;
						}
					} else {
						end = (lastReachedPos - lastUnReachPos) / 2 + lastUnReachPos;
					}
				}
			} else {
				if (end == begin + 1) {
					tbPriceListData.insertListUrl(url, begin, end, totalPage);
					lastUnReachPos = 0; 
					lastReachedPos = 0;
					begin = end ++;
				} else {
					lastReachedPos = end;
					if (lastUnReachPos == 0) {

					} else {
						end = (lastReachedPos - lastUnReachPos) / 2 + lastUnReachPos; 
					}
				}
			}
		}
	}

	private int getTotalPage(String url) throws Exception {
		String pageRegex = "\"totalPage\":\"(\\d*)\"";
		String statusRegex = "\"status\":\\s*?\\{\\s*?\"code\":\\s*?\"(\\d*)\"";
		String response = TBUtil.getPage(url);
		try {
			int totalPage = Integer.parseInt(TBUtil.getSingleMatchByString(pageRegex, response));
			return totalPage;
		} catch (Exception e) {
			int status = -1;
			try {
				status = Integer.parseInt(TBUtil.getSingleMatchByString(statusRegex, response));
			} catch(Exception e1) {
				log.error("match page failed, regex: " + pageRegex + ", url: " + url);
			}
			if (status == 200) {
				return 0;
			} else {
				log.error("match page failed, regex: " + pageRegex + ", url: " + url);
				throw e;
			}
		}
	}

}
