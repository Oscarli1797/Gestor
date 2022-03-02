package com.GestorProyectos.entity;

public class Email {

	private String userName;
	private String userMail;
	private String verifycode;

	public Email(String userName, String userMail,String verifycode) {		
		this.userName = userName;
		this.userMail = userMail;
		this.verifycode=verifycode;
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
	public String getVerifycode() {
		return verifycode;
	}

	@Override
	public String toString() {
		return "Mail [userName=" + userName + ", userMail=" + userMail + "]";
	}

}
