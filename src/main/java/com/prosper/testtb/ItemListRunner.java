package com.prosper.testtb;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.testtb.bean.TBItem;
import com.prosper.testtb.bean.TBPriceListItem;
import com.prosper.testtb.data.DBConn;
import com.prosper.testtb.data.TBItemData;
import com.prosper.testtb.data.TBPriceListData;
import com.prosper.testtb.exception.EmptyPageException;
import com.prosper.testtb.exception.ExecuteItemDetailException;

public class ItemListRunner implements Runnable {

	private static Logger log = LogManager.getLogger(ItemListRunner.class);
	
	private static int page_size = 96;
	private static String item_base_url = "http://item.taobao.com/item.htm?id=";

	private DBConn dbConn = DBConn.getInstance();
	private TBPriceListData tbPriceListData = new TBPriceListData(dbConn);
	private TBItemData tbItemData = new TBItemData(dbConn);

	private String name;

	public ItemListRunner(String name) {
		setName(name);
	}

	public void run() {
		int id = -1;
		String detail = "";
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
				log.info(getName() + " proceeding: " + url + ", from " + exePage + " to " + totalPage);
				
				for(int processPage = exePage; processPage <= totalPage; processPage++) {
					detail = tbPriceListItem.getUrl() + " page:" + processPage;
					log.info(getName() + " proceeding: " + url + ", page: " + processPage);
					int count = 0;
					int offset = (processPage - 1) * page_size;
					String exeUrl = url + "&s=" + offset;
					String content = TBUtil.getPage(exeUrl);

					ObjectMapper mapper = new ObjectMapper();
					JsonNode rootNode = mapper.readTree(content);
					
					JsonNode itemListNode = rootNode.get("itemList");
					Iterator<JsonNode> iterator =  itemListNode.iterator();
					
					while (iterator.hasNext()) {
						JsonNode itemNode = iterator.next();
						long itemId = Long.parseLong(itemNode.get("itemId").asText());
						log.info(getName() + " proceeded item: " + itemId);
						
						String itemUrl = itemNode.get("href").asText();
						if (!itemUrl.contains("item.taobao.com")) {
							log.info("skip item from other site: " + itemUrl);
							continue;
						}
						TBItem tbItem = tbItemData.getById(itemId);
						if (tbItem == null) {
							try {
								tbItem = getItemDetail(itemId);
							} catch(EmptyPageException e) {
								tbItem = new TBItem();
								tbItem.shopLevel = "";
								tbItem.trade_count = 0;
								tbItem.trade_succ_count = 0;
								tbItem.state = 0;
							}
							tbItem.title = itemNode.get("title").asText();
							tbItem.price = itemNode.get("price").asInt();
							tbItem.url = itemNode.get("href").asText();
							tbItem.itemId = itemId;
							tbItem.state = 1;
							tbItemData.insert(tbItem);
							count ++;
						}
					}
					tbPriceListData.updateExePage(tbPriceListItem.getId(), processPage);
					log.info(getName() + " proceeded: " + exeUrl + ", page: " + processPage + ", count: " + count);
				}
				tbPriceListData.updateState(tbPriceListItem.getId(), 2);
			} catch (Exception e) {
				if (id == -1) {
					log.error("get id error");
				} else {
					try {
						tbPriceListData.updateState(id, 0);
					} catch (SQLException e1) {
						log.error("update id error: id:" + id + " detail:" + detail);
					}
				}
				log.error("execute id failed, id=" + id + " detail:" + detail);
				e.printStackTrace();
			}
		} 
	}
	
	public TBItem getItemDetail(long id) throws ExecuteItemDetailException, EmptyPageException {
		try {
			String item_url = item_base_url + Long.toString(id);
			String pageContent = TBUtil.getPage(item_base_url + Long.toString(id));
			
			System.out.println("url:" + item_url + " page" + pageContent);
			
			//String shopLevelRegex = "class=\"rank\"[^>]*?src=\"([^\"]*?)\"";
			String[] shopLevelRegex = new String[]{
					"(pics.taobaocdn.com/newrank/[^\"]*\")",
					"shop-rank\\s*empty\">[^<]*<[^>]*>(\\d)*<"};

			int shopLevelType = 0;
			String shopLevel = null;
			while (shopLevel == null && shopLevelType < shopLevelRegex.length) {
				try {
					shopLevel = TBUtil.getSingleMatchByString(shopLevelRegex[shopLevelType++], pageContent);
				} catch (Exception e) {
				}
			}
			
			log.info("shop level: " + shopLevel);
			
			String saleUrlRegex = "\"apiItemInfo\":\\s*\"([^\"]*?)\"";
			String saleUrl = TBUtil.getSingleMatchByString(saleUrlRegex, pageContent);
			log.info("sale url: " + saleUrl);

			String saleContent = TBUtil.getPage(saleUrl);
			String tradeCountRegex = "quanity:\\s*(\\d*)";
			String tradeSuccCountRegex = "confirmGoods:\\s*(\\d*)";
			
			int tradeCount = Integer.parseInt(TBUtil.getSingleMatchByString(tradeCountRegex, saleContent));
			int tradeSuccCount = Integer.parseInt(TBUtil.getSingleMatchByString(tradeSuccCountRegex, saleContent));
			
			TBItem tbItem = new TBItem();
			tbItem.shopLevel = shopLevel;
			tbItem.trade_count = tradeCount;
			tbItem.trade_succ_count = tradeSuccCount;
			return tbItem;
		} catch (EmptyPageException epe) {
			log.error("get empty page, item id:" + id);
			throw epe;
		}
		catch (Exception e) {
			log.error("execute item failed, item id:" + id);
			throw new ExecuteItemDetailException(e);
		}
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}