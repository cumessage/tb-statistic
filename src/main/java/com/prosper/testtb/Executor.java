package com.prosper.testtb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.ClientProtocolException;
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
			if (tbSystem.getState() <= 0) {
				runForBaseListUrl(LIST_SOURCE_URL);
				tbSystemData.update(1);
			} 
			if (tbSystem.getState() <= 1) {
				runForPriceListUrl();
				//tbSystemData.update(2);
			}
			if (tbSystem.getState() <= 2) {
				runForItemUrl();
				//tbSystemData.update(3);
			}
			if (tbSystem.getState() <= 3) {
				//runForItem();
				//tbSystemData.update(4);
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
	
	public void runForBaseListUrl(String url) throws Exception {
		new ListRunner().run(url);
	}
	
	public void runForPriceListUrl() throws Exception {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		for (int i = 0; i < 10; i++) {
			threadPool.execute(new PriceListRunner("thread-" + i));
		}
	}
	
	public void runForItemUrl() throws Exception {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		for (int i = 0; i < 2; i++) {
			threadPool.execute(new ItemListRunner("thread-" + i));
		}
	}
	
	public void runForItem() throws Exception {
		
	}
	
	public void testTB() throws ClientProtocolException, IOException, InterruptedException {
//		RequestConfig requestConfig = RequestConfig.custom().setCircularRedirectsAllowed(true).build();
//		long time = System.currentTimeMillis();
//		long begin = 19423417866L;
//		long end = begin;
//		for (long id = begin; id < 29423418963L; id++) {
//			end = id;
//			HttpGet httpget = new HttpGet("http://item.taobao.com/item.htm?id=" + id);
//			httpget.setConfig(requestConfig);
//			CloseableHttpResponse response = httpclient.execute(httpget);
//			int status = response.getStatusLine().getStatusCode();
//			System.out.println(status + ", id:" + id);
//			
//			try {
//				InputStream is = response.getEntity().getContent();
//				StringBuffer sb = new StringBuffer();
//				byte[] b = new byte[256];
//				while (is.read(b) != -1) {
//					sb.append(new String(b, Charset.forName("gbk")));
//				}
//				String body = sb.toString();
//				if (body.contains("您查看的宝贝不存在"))
//				System.out.println("index:" + body.indexOf("您查看的宝贝不存在"));
////						replaceAll("script.*script", "").
////						replaceAll("<.*?>", "").
////						replaceAll("\\s+", " "));
//			} finally {
//				response.close();
//			}
////			if (System.currentTimeMillis() - time > 2000000000000000L) {
////				break;
////			}
////			Thread.sleep(2000);
//		}
//		System.out.println(end - begin);
	}
	
	public static void main(String[] args) throws Exception {
		new Executor().run();
		//(new PriceListRunner("1")).runForPriceListUrl("http://list.taobao.com/itemlist/default.htm?json=on&cat=50095933");
	}

}














