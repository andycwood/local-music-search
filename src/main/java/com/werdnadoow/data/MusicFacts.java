package com.werdnadoow.data;

import org.springframework.data.annotation.Id;

public class MusicFacts {
	
	@Id
	private String id;
	private String value;
	
	public MusicFacts() {
		id = null;
		value = null;
	}
	public MusicFacts(String name) {
		this.id = name;
		value = null;
	}
	
	public MusicFacts(String name, String value) {
		this.id = name;
		this.value = value;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String name) {
		this.id = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String val) {
		this.value = val;
	}
}
