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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import com.prosper.testtb.bean.Proxy;
import com.prosper.testtb.exception.BlockException;
import com.prosper.testtb.exception.ProxyException;

public class HttpProxy implements Runnable {

	private static Logger log = LogManager.getLogger(HttpProxy.class); 

	/**
	 * proxy文件地址
	 */
	private static final String proxyFilePath = "d:/proxy-";


	private static final int proxyFileCount = 2;

	/**
	 * 测试地址
	 */
	private static String testUrl = "http://list.taobao.com/browse/cat-0.htm";
	private static final String test1Url = "http://alisec.taobao.com/checkcodev3.php?apply=hesper&http_referer=http://list.taobao.com/itemlist/default.htm?json=on&cat=50003875&filter=reserve_price%5B1%2C2%5D";

	/**
	 * 最小成功率
	 */
	private static final int minSuccRate = 80;

	/**
	 * 测试成功时, 包含的字符串
	 */
	private static final String succString = "<h4>女装男装</h4>";

	/**
	 * 被屏蔽时, 包含的字符串
	 */
	private static final String blockString = "<title>亲，访问受限了</title>";

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
	private static final int refreshInterval = 1 * 60; 

	private int lastProxyFileIndex = 0;

	/**
	 * 是否需要重新加载
	 */
	private int needLoad = 1;

	/**
	 * 是否需要检查
	 */
	private int needCheck = 0;

	/**
	 * 是否正在加载
	 */
	private int isLoading = 0;


	private CloseableHttpClient httpclient = HttpClients.createDefault();

	private static HttpProxy instance = new HttpProxy();

	private List<Proxy> proxyList = new ArrayList<Proxy>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock(); 

	public static HttpProxy getInstance() {
		return instance;
	}

	private HttpProxy() {}

	public void run() {
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					needLoad = 1;
					log.info("auto set need-load flag on");
					try {
						Thread.sleep(refreshInterval * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		while(true) {
			try {
				if (needLoad == 1) {
					load();
				}
				if (needCheck == 1) {
					check();
				}
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean load() throws IOException, InterruptedException {
		isLoading = 1;
		lock.writeLock().lock(); 
		try {

			List<Proxy> testProxyList = new ArrayList<Proxy>();
			this.proxyList.clear();
			String fileName = proxyFilePath + nextProxyIndex();
			log.debug("load proxy begin, proxy file: " + fileName);

			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(fileName));
				String line = null;
				while((line = br.readLine()) != null) {
					String[] ss = line.split(":");
					if (ss.length < 2) {
						continue;
					}
					String[] pp = ss[1].split("@");
					final Proxy proxy = new Proxy(ss[0], Integer.parseInt(pp[0]));

					if (testProxyList.contains(proxy)) {
						log.debug("proxy exist: " + proxy);
						continue;
					}
					testProxyList.add(proxy);
				}
			} finally {
				br.close();
			}

			for (int i = 1; i <= 10; i++) {
				ExecutorService threadPool = Executors.newCachedThreadPool();
				log.info("test proxy, round " + i);
				for(final Proxy proxy: testProxyList) {
					threadPool.execute(new Runnable() {
						public void run() {
							try {
								proxy.setTestCount(proxy.getTestCount() + 1);
								testSingle(proxy.getIp(), proxy.getPort());
								//log.debug("proxy test success, proxy: " + proxy);
								proxy.setSuccCount(proxy.getSuccCount() + 1); 
							} catch (ClientProtocolException e) {
								throw new ProxyException(e);
							} catch (Exception e) {
								//log.debug("proxy test failed, proxy: " + proxy);
							}
						}
					});	
				}
				threadPool.shutdown();
				threadPool.awaitTermination(1, TimeUnit.DAYS);
			}

			for (Proxy proxy: testProxyList) {
				if (((proxy.getSuccCount() * 100) / proxy.getTestCount()) > minSuccRate) {
					proxyList.add(proxy);
				}
			}

			if (proxyList.size() < minRunProxy) {
				throw new ProxyException("proxy is not enough, size:" + proxyList.size());
			}
			log.info("load proxy successfully, count:" + proxyList.size() + ", proxy list:" + proxyList);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.writeLock().unlock();
		}
		isLoading = 0;
		needLoad = 0;
		return true;
	}

	private void check() {
		if (needCheck == 1) {
			try {
				log.info("begin to check, proxy list: " + proxyList);
				List<Proxy> blockedProxyList = new ArrayList<Proxy>();
				for (Proxy proxy: proxyList) {
					try {
						testSingle(proxy.getIp(), proxy.getPort());
						log.debug("checking... proxy test good, proxy: " + proxy);
					} catch (BlockException e) {
						log.debug("checking... proxy test blocked, proxy: " + proxy);
						blockedProxyList.add(proxy);
					} catch (ClientProtocolException e) {
						throw new ProxyException(e);
					} catch (IOException e) {
						log.debug("checking... proxy test failed, proxy: " + proxy);
					}
				}
				for (Proxy proxy: blockedProxyList) {
					proxyList.remove(proxy);
				}
				log.info("check done, proxy list: " + proxyList);
			} finally {
				needCheck = 0;
			}
		}
	}

	private int nextProxyIndex() {
		if (lastProxyFileIndex >= proxyFileCount) {
			lastProxyFileIndex = 0;
		} 
		return ++lastProxyFileIndex;
	}

	public void testSingle(String url, int port) throws ClientProtocolException, IOException, BlockException {
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
			if (page.contains(blockString)) {
				throw new BlockException();
			}
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
		while (true) {
			if (isLoading == 1) {
				log.info("loading proxy file, waiting ...");
				Thread.sleep(10000);
			}
			lock.readLock().lock(); 
			try {
				int size = proxyList.size();
				if (size < 10) {
					log.warn("proxy size is low, set need-load flag on");
					needLoad = 1;
					Thread.sleep(10000);
					continue;
				}
				if (size <= 0) {
					Thread.sleep(60000);
				}
				Random r = new Random();
				int index = r.nextInt(proxyList.size());
				return proxyList.get(index); 
			} finally { 
				lock.readLock().unlock(); 
			}	
		}
	}

	public void removeProxy(Proxy proxy) {
		lock.writeLock().lock();
		try {
			proxyList.remove(proxy);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void setNeedCheck() {
		this.needCheck = 1;
	}

	public static void main(String... args) throws InterruptedException {
		new Thread(HttpProxy.getInstance()).start();
		HttpProxy.getInstance().setNeedCheck();
	}

}
