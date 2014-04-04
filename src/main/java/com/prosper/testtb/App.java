package com.prosper.testtb;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Hello world!
 *
 */
public class App 
{
	public static void main( String[] args ) throws ClientProtocolException, IOException, FailingHttpStatusCodeException, InterruptedException
	{
		testTB();
	}

	public static void testTB() throws ClientProtocolException, IOException, InterruptedException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		RequestConfig requestConfig = RequestConfig.custom()
				.setCircularRedirectsAllowed(true).build();
		
		long time = System.currentTimeMillis();
		long begin = 19423417866L;
		long end = begin;
		for (long id = begin; id < 29423418963L; id++) {
			end = id;
			HttpGet httpget = new HttpGet("http://item.taobao.com/item.htm?id=" + id);
			httpget.setConfig(requestConfig);
			CloseableHttpResponse response = httpclient.execute(httpget);
			int status = response.getStatusLine().getStatusCode();
			System.out.println(status + ", id:" + id);
			
			try {
				InputStream is = response.getEntity().getContent();
				StringBuffer sb = new StringBuffer();
				byte[] b = new byte[256];
				while (is.read(b) != -1) {
					sb.append(new String(b, Charset.forName("gbk")));
				}
				String body = sb.toString();
				if (body.contains("您查看的宝贝不存在"))
				System.out.println("index:" + body.indexOf("您查看的宝贝不存在"));
//						replaceAll("script.*script", "").
//						replaceAll("<.*?>", "").
//						replaceAll("\\s+", " "));
			} finally {
				response.close();
			}
//			if (System.currentTimeMillis() - time > 2000000000000000L) {
//				break;
//			}
//			Thread.sleep(2000);
		}
		System.out.println(end - begin);
	}

	public static void testTBWithHtmlUnit() throws FailingHttpStatusCodeException, MalformedURLException, IOException, InterruptedException {
		final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_24);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setCssEnabled(false);  
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		final HtmlPage page = webClient.getPage("http://detailskip.taobao.com/json/ifq.htm?stm=1396522591000&id=36056176273&sid=1771566932&sbn=ca57c4ca601c5faf47f1cd776a439c79&q=1&ex=0&exs=0&shid=&at=b&ct=1");
		webClient.waitForBackgroundJavaScript(10000);
		//        Thread.sleep(30000);
		//    	final HtmlPage page = webClient.getPage("http://www.baidu.com");
		//    	final String pageAsText = page.asXml();
		final String pageAsText = page.asText();
		System.out.println(pageAsText);
		webClient.closeAllWindows();
	}
}














