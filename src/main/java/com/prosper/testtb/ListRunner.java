package com.prosper.testtb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prosper.testtb.data.DBConn;
import com.prosper.testtb.data.TBListData;

public class ListRunner {
	
	private static Logger log = LogManager.getLogger(ListRunner.class); 
	
	private static final String LIST_PREFIX = "http://list.taobao.com/itemlist/default.htm?json=on&cat=";
	private static final String LIST_REGEX = "http://list.taobao.com/itemlist/default.htm\\?.*?cat=(\\d*)";
	
	private DBConn dbConn = DBConn.getInstance();
	private TBListData tbListData = new TBListData(dbConn);

	public void run(String url) throws Exception {
		log.info("list runner begin to run ...");
		String response = TBUtil.getPage(url);
		Pattern pattern = Pattern.compile(LIST_REGEX);
		Matcher matcher = pattern.matcher(response);
		int count = 0;
		int insertedCount = 0;
		while (matcher.find()) {
			String listUrl = LIST_PREFIX + matcher.group(1);
			if (tbListData.getCountByUrl(listUrl) == 0) {
				log.info("insert: " + listUrl);
				tbListData.insertListUrl(listUrl);
			} else {
				log.info("inserted: " + listUrl);
				insertedCount++;
			}
			count++;
		}
		log.info("count:" + count + ", inserted count:" + insertedCount);
		log.info("list runner is done");
	}
}
