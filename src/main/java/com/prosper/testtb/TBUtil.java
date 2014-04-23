package com.prosper.testtb;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prosper.testtb.bean.Proxy;
import com.prosper.testtb.data.DBConn;
import com.prosper.testtb.data.TBFailedPageData;
import com.prosper.testtb.exception.EmptyPageException;
import com.prosper.testtb.exception.HttpException;

public class TBUtil {

	private static Logger log = LogManager.getLogger(TBUtil.class); 

	private static DBConn dbConn = DBConn.getInstance();
	private static TBFailedPageData tbFailedPageData = new TBFailedPageData(dbConn);

	private static HttpProxy httpProxy = HttpProxy.getInstance();
	private static final int RETRY_COUNT = 10;

	private static CloseableHttpClient httpclient = HttpClients.createDefault();

	public static String getPage(String url) throws Exception {
		String cna = CookieRefresher.getInstance().getCna();
		Proxy proxy = null;
		HttpGet httpget = new HttpGet(url);

		int reCount = 0;
		boolean isDone = false;
		boolean isEmpty = false;
		String page = "";
		while (reCount <= RETRY_COUNT && isDone == false) {
			proxy = httpProxy.getProxy();
			CloseableHttpResponse response = null;
			try {
				RequestConfig requestConfig = RequestConfig.custom().
						setProxy(new HttpHost(proxy.getIp(), proxy.getPort())).
						setCircularRedirectsAllowed(true).
						setSocketTimeout(3000).
						setConnectTimeout(3000).
						setConnectionRequestTimeout(3000).build();
				httpget.setConfig(requestConfig);
				httpget.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36");
				httpget.setHeader("Cookie", "cna=" + cna + ";");
				//httpget.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
				//httpget.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate,sdch");
				//httpget.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.8,en;q=0.6,it;q=0.4");
				//httpget.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=0");
				//httpget.setHeader(HttpHeaders.HOST, "list.taobao.com");
				//httpget.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
				//httpget.setHeader(HttpHeaders.PRAGMA, "no-cache");

				if (reCount > 0) {
					log.info("retry: " + reCount);
				}
				reCount++;
				response = httpclient.execute(httpget);
				int status = response.getStatusLine().getStatusCode();
				if (status != 200) {
					throw new RuntimeException("http failed");
				} 
				page = EntityUtils.toString(response.getEntity(), "gbk");
				if (page.length() < 10) {
					isEmpty = true;
				}
				isDone = true;
			} catch(Exception e) {
				proxy.setRetryCount(proxy.getRetryCount() + 1);
				log.warn("get page failed" + e.getClass().getName());
			} finally {
				httpProxy.returnProxy(proxy);
				if(response != null) {
					response.close();
				}
			}
		}

		if (isEmpty) {
			throw new EmptyPageException();
		}
		if (reCount > RETRY_COUNT && isDone == false) {
			throw new HttpException("get page failed after retry for " + reCount + " times");
		}
		return page;
	}

	public static Header[] getHeader(String url) throws Exception {
		int reCount = 0;
		boolean isDone = false;
		boolean isEmpty = false;
		Header[] header = null;
		while (reCount <= RETRY_COUNT && isDone == false) {
			Proxy proxy = httpProxy.getProxy();
			CloseableHttpResponse response = null;
			try {
				HttpGet httpget = new HttpGet(url);
				RequestConfig requestConfig = RequestConfig.custom().
						setProxy(new HttpHost(proxy.getIp(), proxy.getPort())).
						setCircularRedirectsAllowed(true).
						setSocketTimeout(1000).
						setConnectTimeout(1000).
						setConnectionRequestTimeout(1000).build();
				httpget.setConfig(requestConfig);
				httpget.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36");

				if (reCount > 0) {
					log.info("retry: " + reCount);
				}
				reCount++;
				HttpClientContext context = HttpClientContext.create();
				CookieStore cookieStore = new BasicCookieStore();
				context.setCookieStore(cookieStore);
				response = httpclient.execute(httpget, context);
				int status = response.getStatusLine().getStatusCode();
				if (status != 200) {
					throw new RuntimeException("http failed");
				} 
				header = response.getAllHeaders();
				isDone = true;
				proxy.setRetryCount(0);
			} catch(Exception e) {
				proxy.setRetryCount(proxy.getRetryCount() + 1);
				proxy.setLastRetryTime(System.currentTimeMillis());
				log.warn("get page failed" + e.getClass().getName());
			} finally {
				httpProxy.returnProxy(proxy);
				if(response != null) {
					response.close();
				}
			}
		}

		if (isEmpty) {
			throw new EmptyPageException();
		}
		if (reCount > RETRY_COUNT && isDone == false) {
			throw new HttpException("get page failed after retry for " + reCount + " times");
		}
		return header;
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
