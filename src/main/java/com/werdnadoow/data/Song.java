package com.werdnadoow.data;

import org.springframework.data.annotation.Id;

public class Song {

    @Id
    private String id;
    
    private String path;
    
    private String artist, album, title;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String toString() {
		if (title != null && title.length() >0)
		{
			return "{ \"artist\": \"" + artist + "\", \"title\": \"" + title + "\" }";
		}
		return "{ \"artist\": \"" + artist + "\", \"path\": \"" + path + "\" }";
	}
	
	public boolean equals(Song s) {
		if (s == null && this != null)
			return false;
		
		if (s.getId() == this.id)
			return true;
		
		return this.id.equals(s.getId());
	}
	
	public int hashCode() {
		return id.hashCode();
	}
}
