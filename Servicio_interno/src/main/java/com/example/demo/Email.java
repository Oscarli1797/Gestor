package com.example.demo;


public class Email {

	private String userName;
	private String userMail;

	public Email(String userName, String userMail) {		
		this.userName = userName;
		this.userMail = userMail;
	}
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserMail() {
		return userMail;
	}

	public void setUserMail(String userMail) {
		this.userMail = userMail;
	}

	@Override
	public String toString() {
		return "Mail [userName=" + userName + ", userMail=" + userMail + "]";
	}

}
