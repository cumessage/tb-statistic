package com.prosper.testtb;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;

public class Executor 
{
	private static final String LIST_SOURCE_URL = "http://list.taobao.com/browse/cat-0.htm";
	private static final String LIST_PREFIX = "http://list.taobao.com/itemlist/default.htm?cat=";
	private static final String LIST_REGEX = "http://list.taobao.com/itemlist/default.htm\\?.*?cat=(\\d*)";
	private static final String PRICE_APPEND = "";
	private static final String PAGE_APPEND = "";

	private static final String ITEM_REGEX = "";
	private static final String ITEM_DATAIL_REGEX = "";
	
	private DBConn dbConn = DBConn.getInstance();
	private TBListData tbListData = new TBListData(dbConn);
	
	public void run() throws Exception {
		try {
			runForBaseListUrl(LIST_SOURCE_URL);
			runForListUrl();
			runForItemUrl();
			runForItem();
		} catch (Exception e) {
			e.printStackTrace();
			DBConn.close();
		}
	}
	
	public void runForBaseListUrl(String url) throws Exception {
		String response = HttpUtil.getPage(url);
		Pattern pattern = Pattern.compile(LIST_REGEX);
		Matcher matcher = pattern.matcher(response);
		int count = 0;
		int insertedCount = 0;
		while (matcher.find()) {
			String listUrl = LIST_PREFIX + matcher.group(1);
			if (tbListData.getCountByUrl(listUrl) == 0) {
				tbListData.insertListUrl(listUrl);
			} else {
				System.out.println("inserted: " + listUrl);
				insertedCount++;
			}
			count++;
		}
		System.out.print("count:" + count + ", inserted count:" + insertedCount);
	}
	
	public void runForListUrl() throws Exception {
		// match list regex
		// insert list to db
	}
	
	public void runForItemUrl() throws Exception {
		// get list form db
		// for each list, get page
		// match item regex
		// insert item url to db
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
	}

}














