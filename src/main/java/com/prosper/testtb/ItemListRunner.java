package com.prosper.testtb;

import java.sql.SQLException;
import java.util.Iterator;

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
import com.prosper.testtb.exception.FailedPageException;
import com.prosper.testtb.exception.WrongPageException;

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
				int priceMin = tbPriceListItem.getPriceMin();
				int priceMax = tbPriceListItem.getPriceMax();
				log.info(getName() + " proceeding: " + url + ", price:" + priceMin + "-" 
							+ priceMax + ", from " + exePage + " to " + totalPage);
				
				for(int processPage = exePage; processPage <= totalPage; processPage++) {
					detail = tbPriceListItem.getUrl() + ", price:" + priceMin + "-" 
							+ priceMax + ", page:" + processPage;
					log.info(getName() + " proceeding: " + url + ", page: " + processPage);
					int count = 0;
					int offset = (processPage - 1) * page_size;
					String exeUrl = url + "&filter=reserve_price%5B" + priceMin + "%2C" + priceMax + "%5D" + "&s=" + offset;
					String content = TBUtil.getPage(exeUrl);

					ObjectMapper mapper = new ObjectMapper();
					JsonNode rootNode = mapper.readTree(content);
					
					JsonNode itemListNode = rootNode.get("itemList");
					if (itemListNode == null) {
						throw new FailedPageException();
					}
					Iterator<JsonNode> iterator =  itemListNode.iterator();
					
					while (iterator.hasNext()) {
						JsonNode itemNode = iterator.next();
						long itemId = Long.parseLong(itemNode.get("itemId").asText());
						log.info(getName() + " proceeded item: " + itemId);
						
						String itemUrl = itemNode.get("href").asText();
						String shopUrl = itemNode.get("storeLink").asText();
						if (!itemUrl.contains("item.taobao.com")) {
							log.info("skip item from other site: " + itemUrl);
							continue;
						}
						TBItem tbItem = tbItemData.getById(itemId);
						if (tbItem == null) {
							try {
								tbItem = new TBItem();
								tbItem = getShopLevel(shopUrl, tbItem);
								tbItem = getItemDetail(itemId, tbItem);
								tbItem.state = 1;
							} catch(EmptyPageException e) {
								log.warn("empty page, item id: " + itemId);
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
						log.error("update state to 0 successfully , id:" + id);
					} catch (SQLException e1) {
						log.error("update id error: id:" + id + " detail:" + detail);
					}
				}
				log.error("execute id failed, id=" + id + " detail:" + detail);
				e.printStackTrace();
			}
		} 
	}
	
	public TBItem getShopLevel(String url, TBItem tbItem) throws WrongPageException {
		String shopLevel = null;
		String pageContent = null;
		int retryMaxCount = 8;
		int retryCount = 0;
		while (shopLevel == null && retryCount <= retryMaxCount) {
			if (retryCount > 0) {
				log.info("retry " + retryCount + " for shop level");
			}
			try {
				pageContent = TBUtil.getPage(url);
				
				//System.out.println("url:" + item_url + " page" + pageContent);
				//String shopLevelRegex = "class=\"rank\"[^>]*?src=\"([^\"]*?)\"";
				String[] shopLevelRegex = new String[]{
						"(pics.taobaocdn.com/newrank/[^\"]*\")",
						"shop-rank\\s*empty\">[^<]*<[^>]*>(\\d)*<"};

				int shopLevelType = 0;
				while (shopLevel == null && shopLevelType < shopLevelRegex.length) {
					try {
						shopLevel = TBUtil.getSingleMatchByString(shopLevelRegex[shopLevelType++], pageContent);
					} catch (Exception e) {
						
					}
				}
				retryCount++;
			} catch (Exception e) {
			}
		}
		
		if (shopLevel == null) {
			throw new WrongPageException();
		}
		tbItem.shopLevel = shopLevel;
		log.info("shop level: " + shopLevel);
		return tbItem;
	}
	
	public TBItem getItemDetail(long id, TBItem tbItem) throws ExecuteItemDetailException, EmptyPageException {
		try {
//			String shopLevel = null;
//			String pageContent = null;
//			int retryMaxCount = 8;
//			int retryCount = 0;
//			while (shopLevel == null && retryCount <= retryMaxCount) {
//				if (retryCount > 0) {
//					log.info("retry " + retryCount + " for shop level");
//					Thread.sleep(2000);
//				}
//				try {
//					pageContent = TBUtil.getPage(item_base_url + Long.toString(id));
//					
//					//System.out.println("url:" + item_url + " page" + pageContent);
//					//String shopLevelRegex = "class=\"rank\"[^>]*?src=\"([^\"]*?)\"";
//					String[] shopLevelRegex = new String[]{
//							"(pics.taobaocdn.com/newrank/[^\"]*\")",
//							"shop-rank\\s*empty\">[^<]*<[^>]*>(\\d)*<"};
//
//					int shopLevelType = 0;
//					while (shopLevel == null && shopLevelType < shopLevelRegex.length) {
//						try {
//							shopLevel = TBUtil.getSingleMatchByString(shopLevelRegex[shopLevelType++], pageContent);
//						} catch (Exception e) {
//							
//						}
//					}
//					retryCount++;
//				} catch (Exception e) {
//				}
//			}
//			
//			if (shopLevel == null) {
//				throw new WrongPageException();
//			}
//			log.info("shop level: " + shopLevel);

			String pageContent = TBUtil.getPage(item_base_url + Long.toString(id));
			String saleUrlRegex = "\"apiItemInfo\":\\s*\"([^\"]*?)\"";
			String saleUrl = TBUtil.getSingleMatchByString(saleUrlRegex, pageContent);

			String saleContent = TBUtil.getPage(saleUrl);
			String tradeCountRegex = "quanity:\\s*(\\d*)";
			String tradeSuccCountRegex = "confirmGoods:\\s*(\\d*)";

			int tradeCount = Integer.parseInt(TBUtil.getSingleMatchByString(tradeCountRegex, saleContent));
			int tradeSuccCount = Integer.parseInt(TBUtil.getSingleMatchByString(tradeSuccCountRegex, saleContent));

			tbItem.trade_count = tradeCount;
			tbItem.trade_succ_count = tradeSuccCount;
			log.info("trade count: " + tradeCount + ", trade succ count: " + tradeSuccCount);
			return tbItem;
		} catch (EmptyPageException epe) {
			log.error("get empty page, item id:" + id);
			throw epe;
		} catch (Exception e) {
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