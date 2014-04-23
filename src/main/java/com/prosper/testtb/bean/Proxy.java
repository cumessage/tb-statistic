package com.prosper.testtb.bean;

public class Proxy {
	private String ip;
	private int port;
	private int retryCount;
	private long lastRetryTime;

	public Proxy(String ip, int port, int retryCount) {
		this.setIp(ip);
		this.setPort(port);
		this.setRetryCount(retryCount);
	}

	public Proxy(String ip, int port) {
		this(ip, port, 0);
	}

	public void increaseRetryCount() {
		retryCount ++;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Proxy)) {
			return false;
		}
		Proxy other = (Proxy)o;
		if ( other.getIp().equals(getIp()) && other.getPort() == getPort()) {
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
		return "proxy[ip:" + getIp() + ", port:" + getPort() + "]";
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getLastRetryTime() {
		return lastRetryTime;
	}

	public void setLastRetryTime(long lastRetryTime) {
		this.lastRetryTime = lastRetryTime;
	}
	
	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
}
