package com.prosper.testtb.bean;

public class TBListItem {

	private int id;
	
	private String url;
	
	public TBListItem(int id, String url) {
		setId(id);
		setUrl(url);
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
	
	
}
