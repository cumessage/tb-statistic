package com.prosper.testtb.bean;

public class TBPriceListItem {

	private int id;
	
	private String url;
	
	private int priceMin;
	
	private int priceMax;
	
	private int totalPage;
	
	private int exePage;
	
	private int state;
	
	public TBPriceListItem(int id, String url, int priceMin, int priceMax, int totalPage, int exePage, int state) {
		setId(id);
		setUrl(url);
		setPriceMin(priceMin);
		setPriceMax(priceMax);
		setTotalPage(totalPage);
		setExePage(exePage);
		setState(state);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getPriceMin() {
		return priceMin;
	}

	public void setPriceMin(int priceMin) {
		this.priceMin = priceMin;
	}

	public int getPriceMax() {
		return priceMax;
	}

	public void setPriceMax(int priceMax) {
		this.priceMax = priceMax;
	}

	public int getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(int totalPage) {
		this.totalPage = totalPage;
	}

	public int getExePage() {
		return exePage;
	}

	public void setExePage(int exePage) {
		this.exePage = exePage;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	
}
