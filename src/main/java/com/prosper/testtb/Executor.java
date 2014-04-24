package com.prosper.testtb;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prosper.testtb.bean.TBSystem;
import com.prosper.testtb.data.DBConn;
import com.prosper.testtb.data.TBListData;
import com.prosper.testtb.data.TBPriceListData;
import com.prosper.testtb.data.TBSystemData;

public class Executor {
	
	private static Logger log = LogManager.getLogger(Executor.class); 
	
	private static final String LIST_SOURCE_URL = "http://list.taobao.com/browse/cat-0.htm";
	
	private static final String PRICE_APPEND = "";
	private static final String PAGE_APPEND = "";

	private static final String ITEM_REGEX = "";
	private static final String ITEM_DATAIL_REGEX = "";
	
	private DBConn dbConn = DBConn.getInstance();
	private TBListData tbListData = new TBListData(dbConn);
	private TBPriceListData tbPriceListData = new TBPriceListData(dbConn);
	private TBSystemData tbSystemData = new TBSystemData(dbConn);
	
	public void run() throws Exception {
		try {
			log.info("begin ...");
			TBSystem tbSystem = init();
			prepare();
			if (tbSystem.getState() <= 0) {
				runForBaseListUrl(LIST_SOURCE_URL);
				tbSystemData.update(1);
			} 
			if (tbSystem.getState() <= 1) {
				runForPriceListUrl();
			}
			if (tbSystem.getState() <= 2) {
				runForItem();
			}
			if (tbSystem.getState() <= 3) {
				runForFailedItem();
			}
			log.info("done");
		} catch (Exception e) {
			e.printStackTrace();
			DBConn.close();
		}
	}
	
	private TBSystem init() throws SQLException {
		TBSystem tbSystem = tbSystemData.get();
		if (tbSystem == null) {
			tbSystemData.insert(0);
			tbSystem = tbSystemData.get();
		}
		return tbSystem;
	}
	
	private void prepare() throws InterruptedException {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		threadPool.execute(HttpProxy.getInstance());
		Thread.sleep(4000000);
		threadPool.execute(CookieRefresher.getInstance());
	}
	
	public void runForBaseListUrl(String url) throws Exception {
		new ListRunner().run(url);
	}
	
	public void runForPriceListUrl() throws Exception {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		for (int i = 1; i <= 10; i++) {
			threadPool.execute(new PriceListRunner("thread-" + i));
		}
		threadPool.awaitTermination(30, TimeUnit.DAYS);
		tbSystemData.update(2);
	}
	
	public void runForItem() throws Exception {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		for (int i = 1; i <= 10; i++) {
			threadPool.execute(new ItemListRunner("thread-" + i));
		}
		threadPool.awaitTermination(30, TimeUnit.DAYS);
		tbSystemData.update(3);
	}
	
	public void runForFailedItem() throws Exception {
//		ExecutorService threadPool = Executors.newCachedThreadPool();
//		for (int i = 1; i <= 10; i++) {
//			threadPool.execute(new ItemListRunner("thread-" + i));
//		}
//		threadPool.awaitTermination(30, TimeUnit.DAYS);
//		tbSystemData.update(4);
	}
	
	public static void main(String[] args) throws Exception {
		new Executor().run();
		//(new PriceListRunner("1")).runForPriceListUrl("http://list.taobao.com/itemlist/default.htm?json=on&cat=50095933");
		//(new ItemListRunner("1")).getItemDetail(37495393188L, new TBItem());
	}

}














