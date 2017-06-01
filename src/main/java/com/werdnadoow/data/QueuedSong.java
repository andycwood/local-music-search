package com.werdnadoow.data;

public class QueuedSong  {
	// this is the internal queue item id
	private String id;
	
	// this is the id of the song
	private String songId;
	
	// this is the order of the song in the queue
	private Integer sequence;

	public String getSongId() {
		return songId;
	}

	public void setSongId(String id) {
		this.songId = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getSequence() {
		return sequence;
	}

	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}
}
