package com.prosper.testtb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

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

import com.prosper.testtb.exception.ProxyException;

public class HttpProxyOld implements Runnable {

	private static Logger log = LogManager.getLogger(HttpProxyOld.class); 

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

	private BlockingQueue<Proxy> proxyQueue = new LinkedBlockingQueue<Proxy>();

	private Map<Proxy, Integer> proxyMap = new ConcurrentHashMap<Proxy, Integer>();

	private Map<Proxy, Integer> blackList = new ConcurrentHashMap<Proxy, Integer>();

	private long lastLoadTime = 0L;

	private CloseableHttpClient httpclient = HttpClients.createDefault();
	
	private static HttpProxyOld instance = new HttpProxyOld();
	
	public static HttpProxyOld getInstance() {
		return instance;
	}
	
	private HttpProxyOld() {
		
	}

	/**
	 * @return
	 * false not loaded
	 * true loaded
	 */
	public boolean load() throws IOException, InterruptedException  {
		log.debug("load proxy begin ...");
		File file = new File(proxyFilePath);
		if (file.lastModified() < lastLoadTime) {
			log.debug("file not modified");
			return false;
		}

		lastLoadTime = System.currentTimeMillis();
		BufferedReader br = null;
		Set<Proxy> newBlackList = new HashSet<Proxy>();
		int count = 0;
		try {
			br = new BufferedReader(new FileReader(proxyFilePath));
			String line = null;
			proxyMap.clear();
			while((line = br.readLine()) != null) {
				String[] ss = line.split(":");
				if (ss.length < 2) {
					continue;
				}
				String[] pp = ss[1].split("@");
				Proxy proxy = new Proxy(ss[0], Integer.parseInt(pp[0]));
				if (blackList.containsKey(proxy)) {
					log.debug("proxy is in black list, put it into new black list. proxy: " + proxy);
					newBlackList.add(proxy);
					continue;
				}

				try {
					if (proxyMap.containsKey(proxy)) {
						log.debug("proxy exist: " + proxy);
						continue;
					}
					testSingle(proxy.ip, proxy.port);
				} catch (ClientProtocolException e) {
					br.close();
					throw new ProxyException();
				} catch (IOException e) {
					log.debug("proxy test failed, proxy, put it into new black list. proxy: " + proxy);
					newBlackList.add(proxy);
					continue;
				}
				proxyMap.put(new Proxy(ss[0], Integer.parseInt(pp[0])), 0);
				log.debug("proxyMap add: " + proxy);
				count ++;
			}
		} finally {
			br.close();
		}
		if (proxyMap.size() < minRunProxy) {
			throw new ProxyException("proxy is not enough, size:" + proxyMap.size());
		}
		blackList.clear();
		for (Proxy proxy: newBlackList) {
			blackList.put(proxy, 1);
		}
		proxyQueue.clear();
		for (Proxy proxy: proxyMap.keySet()) {
			proxyQueue.put(proxy);
		}
		log.info("load proxy successfully, count:" + count + ", map: " + proxyMap.toString());
		return true;
	}

	public void check() throws InterruptedException {
		log.debug("check proxy begin ...");
		for(Proxy proxy : proxyMap.keySet()) {
			try {
				if (proxyMap.get(proxy) > maxFailCount) {
					testSingle(proxy.ip, proxy.port);
				}
			} catch (ClientProtocolException e) {
				throw new ProxyException(e);
			} catch (IOException e) {
				blackList.put(proxy, 1);
				proxyMap.remove(proxy);
				log.debug("remove proxy into blacklist, proxy: " + proxy);
			}
		}

		if (proxyMap.size() < minRunProxy) {
			throw new ProxyException("proxy is not enough, size:" + proxyMap.size());
		}

		for(Proxy proxy : blackList.keySet()) {
			try {
				testSingle(proxy.ip, proxy.port);
				blackList.remove(proxy);
				proxyMap.put(proxy, 0);
				proxyQueue.put(proxy);
				log.debug("remove proxy into proxy map, proxy: " + proxy);
			} catch (ClientProtocolException e) {
				throw new ProxyException(e);
			} catch (IOException e) {
			}
		}
		log.debug("check proxy end");
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

	/**
	 * get proxy
	 * @return
	 * String[0] ip
	 * String[1] port
	 */
	public String[] getProxy() {
		try {
			while(true) {
				log.info("take proxy from proxy queue, queue: " + proxyQueue.size() + ", map: " + proxyMap.size() + ", blacklist: " + blackList.size());
				Proxy proxy = proxyQueue.take();
				Integer failCount = proxyMap.get(proxy);
				if (failCount != null && failCount <= maxFailCount && !blackList.containsKey(proxy)) {
					proxyQueue.put(proxy);
					log.debug("success get proxy: " + proxy);
					return new String[]{proxy.ip, Integer.toString(proxy.port)};
				} else {
					proxyQueue.put(proxy);
					log.debug("success get proxy: " + proxy);
					continue;
				}
			}
		} catch(InterruptedException e) {
			throw new ProxyException(e);
		}
	}

	/**
	 * when proxy failed, do something
	 */
	public void failProxy(String url, int port) {
		Proxy proxy = new Proxy(url, port);
		Integer failCount = proxyMap.get(proxy);
		if (failCount != null) {
			proxyMap.put(proxy, failCount + 1);
		}
	}
	
	/**
	 * when proxy forbided, put into blacklist
	 */
	public void forbitProxy(String url, int port) {
		Proxy proxy = new Proxy(url, port);
		Integer failCount = proxyMap.get(proxy);
		if (failCount != null) {
			proxyMap.put(proxy, maxFailCount + 1);
		}
	}

	public void run() {
		while(true) {
			try {
				boolean result = false;
				try {
					result = load();
				} catch (IOException e) {
					throw new ProxyException(e);
				}
				if (!result) {
					check();
				}
				Thread.sleep(refreshInterval * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static class Proxy {
		String ip;
		int port;

		public Proxy(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Proxy)) {
				return false;
			}
			Proxy other = (Proxy)o;
			if ( other.ip.equals(ip) && other.port == port) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		@Override
		public String toString() {
			return "proxy[ip:" + ip + ", port:" + port + "]";
		}
	}

	public static void main(String[] args) throws InterruptedException {
		log.info("begin to run ...");
		HttpProxyOld hp = new HttpProxyOld();
		new Thread(hp).start();
		
		Thread.sleep(10000);
		log.info("begin to get ...");
		while(true) {
			String[] addr = hp.getProxy();
			//log.info("get: " + Arrays.toString(addr));
			hp.failProxy(addr[0], Integer.parseInt(addr[1]));
			Thread.sleep(500);
		}
	}

}
