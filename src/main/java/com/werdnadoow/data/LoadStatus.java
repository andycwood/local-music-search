package com.werdnadoow.data;

import java.time.Instant;

// simple class to communicate basic status to the 
public class LoadStatus {
	private static Boolean isRunning = false;
	private static Integer trackCount = -1;
	private static Integer processedCount = -1;
	private static Instant loadStartTime = null;
	private static Instant loadEndTime = null;
	
	private Object statusLock = new Object();
	private Object tracksLock = new Object();
	private Object processedLock = new Object();
	private Object loadStartLock = new Object();
	private Object loadEndLock = new Object();
	
	public void setIsRunning(Boolean status) {
		synchronized (statusLock)
		{
			LoadStatus.isRunning = status;
		}
	}
	
	public Boolean getIsRunning() {
		synchronized (statusLock)
		{
			return LoadStatus.isRunning;
		}
	}
	
	public void setTrackCount(Integer tc) {
		synchronized(tracksLock) {
			LoadStatus.trackCount = tc;
		}
	}
	
	public Integer getTrackCount() {
		synchronized(tracksLock) {
			return LoadStatus.trackCount;
		}
	}
	
	public void setProcessedCount(Integer tc) {
		synchronized(processedLock) {
			LoadStatus.processedCount = tc;
		}
	}

	public void incrementProcessedCount() {
		synchronized(processedLock) {
			LoadStatus.processedCount++;
		}
	}
	
	public Integer getProcessedCount() {
		synchronized(processedLock) {
			return LoadStatus.processedCount;
		}
	}

	public Instant getLoadStartTime() {
		synchronized(loadStartLock) {
			return loadStartTime;
		}
	}

	public void setLoadStartTime() {
		synchronized(loadStartLock) {
			LoadStatus.loadStartTime = Instant.now();
		}
	}

	public Instant getLoadEndTime() {
		synchronized(loadEndLock) {
			return loadEndTime;
		}
	}

	public void setLoadEndTime() {
		synchronized(loadEndLock) {
			LoadStatus.loadEndTime = Instant.now();
		}
	}
}
