package com.werdnadoow;

import java.util.concurrent.ConcurrentLinkedDeque;

// PlayQueue is a singleton wrapper around a thread safe list of Song objects
public class PlayQueue<Song> extends ConcurrentLinkedDeque<Song> {

	private PlayQueue() {
		
	}
	
	private static PlayQueue playQueue= null;
	
	public static synchronized PlayQueue getInstance() {
		if (playQueue == null) {
			playQueue = new PlayQueue();
		}
		return playQueue;
	}
}
