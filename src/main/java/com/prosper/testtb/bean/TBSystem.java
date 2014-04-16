package com.prosper.testtb.bean;

public class TBSystem {
	
	/**
	 * 0 begin, 
	 * 1 finish base;
	 * 2 finish price base;
	 * 3 finish paged price base;
	 * 4 finish item;
	 */
	private int state;
	
	public TBSystem(int state) {
		setState(state);
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
	
}
