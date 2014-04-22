package com.prosper.testtb;

import org.apache.http.Header;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CookieRefresher implements Runnable {

	private static Logger log = LogManager.getLogger(CookieRefresher.class); 
	
	/**
	 * refresh gap, unit: s
	 */
	private static int interval = 300;

	private static final String url = "http://log.mmstat.com/1.gif?logtype=1&cache=fbfd20c&scr=1600x900&isbeta=7";

	private String cna = null;
	
	private static CookieRefresher cr = new CookieRefresher();
	
	public static CookieRefresher getInstance() {
		return cr;
	}
	
	private CookieRefresher() {}

	public void run() {
		while(true) {
			log.info("refreshing cookie ...");
			String cna = null;
			try {
				Header[] headers = TBUtil.getHeader(url);
				for(Header header: headers) {
					if (header.getName().equals("Set-Cookie")) {
						String value = header.getValue();
						if (value.contains("cna=")) {
							int pos = value.indexOf("cna=");
							String keyValue = value.substring(pos, value.indexOf(";", pos + 1));
							String[] keyValuePair = keyValue.split("=");
							if (keyValuePair.length >= 2) {
								cna = keyValuePair[1];
							}
						}
					}
				}
			} catch(Exception e) {
				log.warn("refresh cookie failed");
				e.printStackTrace();
			}
			
			if (cna == null) {
				log.warn("refresh cookie failed");
			} else {
				this.cna = cna;
				log.info("refresh cna done, cna: " + cna);
			}

			try {
				Thread.sleep(interval * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getCna() {
		while (cna == null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return cna;
	}
	
	public static void main(String[] args) throws InterruptedException {
		log.info("begin to run ...");
		HttpProxy hp = HttpProxy.getInstance();
		new Thread(hp).start();
		
		Thread.sleep(10000);
		CookieRefresher cr = new CookieRefresher();
		cr.run();
	}

}
