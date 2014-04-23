package com.prosper.testtb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prosper.testtb.bean.Proxy;
import com.prosper.testtb.exception.ProxyException;

public class HttpProxy implements Runnable {

	private static Logger log = LogManager.getLogger(HttpProxy.class); 
	
	/**
	 * proxy文件地址
	 */
	private static final String proxyFilePath = "d:/proxy.txt";

	/**
	 * 测试地址
	 */
	private static final String testUrl = "http://list.taobao.com/browse/cat-0.htm";

	/**
	 * 测试成功时, 包含的字符串
	 */
	private static final String succString = "<h4>女装男装</h4>";

	/**
	 * 被屏蔽时, 包含的字符串
	 */
	private static final String failedString = "";

	/**
	 * 最大失败次数
	 */
	private static final int maxFailCount = 1;

	/**
	 * 最小代理运行数量
	 */
	private static final int minRunProxy = 0;

	/**
	 * Unit: s
	 */
	private static final int refreshInterval = 15; 

	/**
	 * 上次加载配置时间
	 */
	private long lastLoadTime = 0L;

	private CloseableHttpClient httpclient = HttpClients.createDefault();

	private static HttpProxy instance = new HttpProxy();

	private BlockingQueue<Proxy> proxyQueue = new LinkedBlockingQueue<Proxy>();
	
	private BlockingQueue<Proxy> blackQueue = new LinkedBlockingQueue<Proxy>();

	public static HttpProxy getInstance() {
		return instance;
	}
	
	private HttpProxy() {}

	public void run() {
		while(true) {
			try {
				load();
				Thread.sleep(refreshInterval * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean load() throws IOException, InterruptedException {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		log.debug("load proxy begin ...");
		File file = new File(proxyFilePath);
		if (file.lastModified() < lastLoadTime) {
			log.debug("file not modified");
			return false;
		}

		lastLoadTime = System.currentTimeMillis();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(proxyFilePath));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] ss = line.split(":");
				if (ss.length < 2) {
					continue;
				}
				String[] pp = ss[1].split("@");
				final Proxy proxy = new Proxy(ss[0], Integer.parseInt(pp[0]), 0);

				if (proxyQueue.contains(proxy)) {
					log.debug("proxy exist: " + proxy);
					continue;
				}
				
				threadPool.execute(new Runnable() {
					public void run() {
						try {
							testSingle(proxy.getIp(), proxy.getPort());
							proxyQueue.put(proxy);
							log.debug("proxyMap add: " + proxy);
						} catch (ClientProtocolException e) {
							throw new ProxyException();
						} catch (IOException e) {
							log.debug("proxy test failed, proxy, set it unreachable. proxy: " + proxy);
							proxy.setRetryCount(maxFailCount + 1); 
							try {
								blackQueue.put(proxy);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
			}
		} finally {
			br.close();
		}
		threadPool.awaitTermination(30, TimeUnit.DAYS);
		if (proxyQueue.size() < minRunProxy) {
			throw new ProxyException("proxy is not enough, size:" + proxyQueue.size());
		}
		int size = proxyQueue.size() + blackQueue.size();
		log.info("load proxy successfully, count:" + size + ", available: " + proxyQueue.size());
		return true;
	}

	public void testSingle(String url, int port) throws ClientProtocolException, IOException {
		CloseableHttpResponse response = null;
		try {
			HttpGet httpget = new HttpGet(testUrl);
			RequestConfig requestConfig = RequestConfig.custom().
					setProxy(new HttpHost(url, port)).
					setSocketTimeout(1000).
					setConnectTimeout(1000).
					setConnectionRequestTimeout(1000).build();
			httpget.setConfig(requestConfig);
			httpget.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36");
			response = httpclient.execute(httpget);

			String page = EntityUtils.toString(response.getEntity(), "gbk");
			if (!page.contains(succString)) {
				//System.out.println(page);
				throw new IOException("return wrong");
			}
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	public Proxy getProxy() throws InterruptedException {
		Proxy proxy = null;
		if (proxyQueue.size() < 10) {
			proxy = blackQueue.poll(10, TimeUnit.MINUTES);
		} else {
			proxy = proxyQueue.poll(10, TimeUnit.MINUTES);
		}
		if (proxy == null) {
			throw new ProxyException("not enough proxy");
		}
		log.debug("get proxy: " + proxy + ", proxy queue size: " + proxyQueue.size() + ", black queue size: " + blackQueue.size());
		return proxy;
	}

	public void returnProxy(Proxy proxy) throws InterruptedException {
		if (proxy.getRetryCount() > maxFailCount) {
			blackQueue.put(proxy);
		} else {
			proxyQueue.put(proxy);
		}
	}

}
