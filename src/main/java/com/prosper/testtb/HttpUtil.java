package com.prosper.testtb;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpUtil {
	
	private static CloseableHttpClient httpclient = HttpClients.createDefault();;
	
	public static String getPage(String url) throws Exception {
		HttpGet httpget = new HttpGet(url);
		RequestConfig requestConfig = RequestConfig.custom().setCircularRedirectsAllowed(true).build();
		httpget.setConfig(requestConfig);
		CloseableHttpResponse response = httpclient.execute(httpget);
		int status = response.getStatusLine().getStatusCode();
		if (status != 200) {
			throw new RuntimeException("http failed");
		}
		
		try {
			InputStream is = response.getEntity().getContent();
			StringBuffer sb = new StringBuffer();
			byte[] b = new byte[1024];
			while (is.read(b) != -1) {
				sb.append(new String(b, Charset.forName("gbk")));
			}
			String body = sb.toString();
			return body;
		} finally {
			response.close();
		}
	}

}
