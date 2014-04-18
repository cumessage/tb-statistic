package com.prosper.testtb;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prosper.testtb.data.DBConn;
import com.prosper.testtb.data.TBFailedPageData;
import com.prosper.testtb.exception.EmptyPageException;

public class TBUtil {
	
	private static Logger log = LogManager.getLogger(TBUtil.class); 
	
	private static DBConn dbConn = DBConn.getInstance();
	private static TBFailedPageData tbFailedPageData = new TBFailedPageData(dbConn);
	
	private static final int RETRY_COUNT = 10;
	private static CloseableHttpClient httpclient = HttpClients.createDefault();;
	
	public static String getPage(String url) throws Exception {
		HttpGet httpget = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom().
				//setProxy(new HttpHost("211.138.121.36", 81)).
				setCircularRedirectsAllowed(true).
				setSocketTimeout(1000).
				setConnectTimeout(1000).
				setConnectionRequestTimeout(1000).build();
		httpget.setConfig(requestConfig);
		httpget.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36");
		httpget.setHeader("Cookie", "cna=DtTXC5tUAAwCAdoetLN2hL2I;");
		//httpget.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		//httpget.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate,sdch");
		//httpget.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.8,en;q=0.6,it;q=0.4");
		//httpget.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=0");
		//httpget.setHeader(HttpHeaders.HOST, "list.taobao.com");
		//httpget.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		//httpget.setHeader(HttpHeaders.PRAGMA, "no-cache");
		
		int reCount = 0;
		boolean isDone = false;
		boolean isEmpty = false;
		String page = "";
		while (reCount <= RETRY_COUNT && isDone == false) {
			if (reCount > 0) {
				log.info("retry: " + reCount);
			}
			reCount++;
			try {
				CloseableHttpResponse response = httpclient.execute(httpget);
				int status = response.getStatusLine().getStatusCode();
				if (status != 200) {
					throw new RuntimeException("http failed");
				} 
//				if (response.getEntity().getContentLength() == -1) {
//					isEmpty = true;
//					isDone = true;
//				}
				
				try {
					page = EntityUtils.toString(response.getEntity(), "gbk");
					if (page.length() < 10) {
						isEmpty = true;
					}
					isDone = true;
				} finally {
					response.close();
				}
			} catch(Exception e) {

			}
		}
		
		if (isEmpty) {
			throw new EmptyPageException();
		}
		if (reCount > RETRY_COUNT && isDone == false) {
			throw new RuntimeException("get page failed after retry for " + reCount + " times");
		}
		return page;
	}
	
	public static String getSingleMatchByHttp(String regex, String url) throws Exception {
		String response = null;
		try {
			response = getPage(url);
			return getSingleMatchByString(regex, response);
		} catch (Exception e) {
			log.error("match page failed, regex: " + regex + ", url: " + url);
			throw e;
		}
	}
	
	public static String getSingleMatchByString(String regex, String content) throws SQLException {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		if (!matcher.find()) {
			//tbFailedPageData.insert(content);
			//System.out.println(content);
			throw new RuntimeException("match failed, regex: " + regex);
		}
		return matcher.group(1).trim();
	}

}
