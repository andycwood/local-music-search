package com.werdnadoow;

// simple class to communicate basic status to the 
public class LoadStatus {
	private static Boolean isRunning = false;
	private static Integer trackCount = -1;
	private static Integer processedCount = -1;
	
	private Object statusLock = new Object();
	private Object tracksLock = new Object();
	private Object processedLock = new Object();
	
	public void setIsRunning(Boolean status) {
		synchronized (statusLock)
		{
			isRunning = status;
		}
	}
	
	public Boolean getIsRunning() {
		synchronized (statusLock)
		{
			return isRunning;
		}
	}
	
	public void setTrackCount(Integer tc) {
		synchronized(tracksLock) {
			trackCount = tc;
		}
	}
	
	public Integer getTrackCount() {
		synchronized(tracksLock) {
			return trackCount;
		}
	}
	
	public void setProcessedCount(Integer tc) {
		synchronized(processedLock) {
			processedCount = tc;
		}
	}

	public void incrementProcessedCount() {
		synchronized(processedLock) {
			processedCount++;
		}
	}
	
	public Integer getProcessedCount() {
		synchronized(processedLock) {
			return processedCount;
		}
	}
}
