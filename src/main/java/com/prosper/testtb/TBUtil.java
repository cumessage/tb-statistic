package com.prosper.testtb;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.prosper.testtb.data.TBListData;

public class TBUtil {
	
	private static Logger log = LogManager.getLogger(TBUtil.class); 
	
	private static DBConn dbConn = DBConn.getInstance();
	private static TBFailedPageData tbFailedPageData = new TBFailedPageData(dbConn);
	
	private static final int RETRY_COUNT = 10;
	private static CloseableHttpClient httpclient = HttpClients.createDefault();;
	
	public static String getPage(String url) throws Exception {
		HttpGet httpget = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom().
				setCircularRedirectsAllowed(true).
				setSocketTimeout(1000).
				setConnectTimeout(1000).
				setConnectionRequestTimeout(1000).build();
		httpget.setConfig(requestConfig);
		
		int reCount = 0;
		boolean isDone = false;
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

				try {
					page = EntityUtils.toString(response.getEntity());
					isDone = true;
				} finally {
					response.close();
				}
			} catch(Exception e) {

			}
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
			tbFailedPageData.insert(content);
			throw new RuntimeException("match failed");
		}
		return matcher.group(1);
	}

}
