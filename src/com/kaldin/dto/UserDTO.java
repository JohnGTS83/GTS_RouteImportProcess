package com.kaldin.dto;

public class UserDTO {

	private int id;
	private String url;
	private String userName;
	private String password;
	private String provider;
	private String tokan;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTokan() {
		return tokan;
	}
	public void setTokan(String tokan) {
		this.tokan = tokan;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	@Override
	public String toString() {
		return provider + ": " + userName + " " + url;
	}
	
	
	
}
