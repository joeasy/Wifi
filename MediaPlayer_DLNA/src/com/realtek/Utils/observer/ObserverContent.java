package com.realtek.Utils.observer;

public class ObserverContent {
public static String ADD_DEVICE = "ADD_DEVICE";
public static String REMOVE_DEVICE = "REMOVE_DEVICE";
public static String EXIT_APP = "EXIT_APP";
	String action;
	String msg;
	public ObserverContent(String action, String msg) {
		this.action = action;
		this.msg = msg;
	}
	public String getAction() {
		return action;
	}
	public String getMsg() {
		return msg;
	}
}
