package com.prosper.testtb;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prosper.testtb.bean.TBItem;
import com.prosper.testtb.bean.TBPriceListItem;
import com.prosper.testtb.data.DBConn;
import com.prosper.testtb.data.TBItemData;
import com.prosper.testtb.data.TBPriceListData;

public class ItemListRunner implements Runnable {

	private static Logger log = LogManager.getLogger(ItemListRunner.class);
	
	private static int page_size = 96;

	private DBConn dbConn = DBConn.getInstance();
	private TBPriceListData tbPriceListData = new TBPriceListData(dbConn);
	private TBItemData tbItemData = new TBItemData(dbConn);

	private String name;

	public ItemListRunner(String name) {
		this.name = name;
	}

	public void run() {
		int id = -1;
		while(true) {
			try {
				TBPriceListItem tbPriceListItem = null;
				synchronized (dbConn) {
					tbPriceListItem = tbPriceListData.getByState(0);
					if (tbPriceListItem == null) {
						tbPriceListItem = tbPriceListData.getByState(1);
						if (tbPriceListItem == null) {
							return;
						} else {
							Thread.sleep(300000);
							continue;
						}
					}
					tbPriceListData.updateState(tbPriceListItem.getId(), 1);
				}
				id = tbPriceListItem.getId();
				String url = tbPriceListItem.getUrl();
				int exePage = tbPriceListItem.getExePage();
				int totalPage = tbPriceListItem.getTotalPage();
				log.info(name + " proceeding: " + url + ", from " + exePage + " to " + totalPage);
				
				for(int i = exePage; i <= totalPage; i++) {
					int insert = 0;
					int exist = 0;
					int offset = (i - 1) * page_size;
					String exeUrl = url + "&s=" + offset;
					String content = TBUtil.getPage(exeUrl);
					
					String itemRegex = "item.taobao.com/item.htm\\?id=(\\d*)";
					Pattern pattern = Pattern.compile(itemRegex);
					Matcher matcher = pattern.matcher(content);
					while (matcher.find()) {
						String matchedUrl = "http://" + matcher.group(0);
						TBItem tbItem = tbItemData.getByUrl(matchedUrl);
						if (tbItem != null) {
							exist ++;
							continue;
						} else {
							tbItemData.insert(matchedUrl);
							insert ++;
						}
					}
					log.info(name + " proceeded: " + exeUrl + ", page: " + i + ", insert: " + insert + ", exist: " + exist);
				}
				tbPriceListData.updateState(tbPriceListItem.getId(), 2);
			} catch (Exception e) {
				if (id == -1) {
					log.error("get id error");
				} else {
					try {
						tbPriceListData.updateState(id, 0);
					} catch (SQLException e1) {
						log.error("update id error: id:" + id);
					}
				}
				log.error("execute id failed, id=" + id);
				e.printStackTrace();
			}
		} 
	}
}