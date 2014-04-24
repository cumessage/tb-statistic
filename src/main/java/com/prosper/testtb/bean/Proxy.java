package com.prosper.testtb.bean;

public class Proxy {
	private String ip;
	private int port;
	
	private int testCount;
	private int succCount;

	public Proxy(String ip, int port) {
		this.setIp(ip);
		this.setPort(port);
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
		if (testCount != 0) {
			int succRate = succCount * 100 / testCount;
			return "proxy[ip:" + getIp() + ", port:" + getPort() + ", succRate:" + succRate + "]";
		} else {
			return "proxy[ip:" + getIp() + ", port:" + getPort() + "]";
		}
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

	public int getTestCount() {
		return testCount;
	}

	public void setTestCount(int testCount) {
		this.testCount = testCount;
	}

	public int getSuccCount() {
		return succCount;
	}

	public void setSuccCount(int succCount) {
		this.succCount = succCount;
	}

}
