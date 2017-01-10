package com.realtek.Utils;

public class MusicMsg {
int currentTime = -1;
int duration =-1;
int status = -1;
int index =-1;
int totaltime = -1;
String trackname ="";
String artistalbum = "";
String timeNow = "";
String timeTotal ="";
int forwardDrawableIndex = -1;
String action ="";
String repeatStatus="";
private final static String tag ="rtkaudio";
public MusicMsg() {

}

public int getCurrentTime() {
	return currentTime;
}

public void setCurrentTime(int currentTime) {
	this.currentTime = currentTime;
}

public int getDuration() {
	return duration;
}

public void setDuration(int duration) {
	this.duration = duration;
}

public int getStatus() {
	return status;
}

public void setStatus(int status) {
	this.status = status;
}

public int getIndex() {
	return index;
}

public void setIndex(int index) {
	this.index = index;
}

public int getTotaltime() {
	return totaltime;
}

public void setTotaltime(int totaltime) {
	this.totaltime = totaltime;
}

public String getTrackname() {
	return trackname;
}

public void setTrackname(String trackname) {
	this.trackname = trackname;
}

public String getArtistalbum() {
	return artistalbum;
}

public void setArtistalbum(String artistalbum) {
	this.artistalbum = artistalbum;
}

public String getTimeNow() {
	return timeNow;
}

public void setTimeNow(String timeNow) {
	this.timeNow = timeNow;
}

public String getTimeTotal() {
	return timeTotal;
}

public void setTimeTotal(String timeTotal) {
	this.timeTotal = timeTotal;
}

public int getForwardDrawableIndex() {
	return forwardDrawableIndex;
}

public void setForwardDrawableIndex(int forwardDrawableIndex) {
	this.forwardDrawableIndex = forwardDrawableIndex;
}

public String getAction() {
	return action;
}

public void setAction(String action) {
	this.action = action;
}

public String getRepeatStatus() {
	return repeatStatus;
}

public void setRepeatStatus(String repeatStatus) {
	this.repeatStatus = repeatStatus;
}

public static String valString(MusicMsg msg){
	
	return "" + msg.currentTime +tag
	+ msg.duration +tag
	+ msg.status  +tag
	+ msg.index  +tag
	+ msg.totaltime  +tag
	+ msg.trackname  +tag
	+ msg.artistalbum +tag
	+ msg.timeNow  +tag
	+ msg.timeTotal  +tag
	+ msg.forwardDrawableIndex  +tag
	+ msg.action +tag
	+ msg.repeatStatus+tag
	+"endstring";
}

public static MusicMsg valClass(String str){
	MusicMsg msg = new MusicMsg();
	String[] strs = str.split(tag);
	msg.currentTime =Integer.parseInt(strs[0]);
	msg.duration =Integer.parseInt(strs[1]);
	msg.status =Integer.parseInt(strs[2]);
	msg.index =Integer.parseInt(strs[3]);
	msg.totaltime  =Integer.parseInt(strs[4]);
	msg.trackname  =strs[5];
	msg.artistalbum=strs[6];
	msg.timeNow  =strs[7];
	msg.timeTotal  =strs[8];
	msg.forwardDrawableIndex  =Integer.parseInt(strs[9]);
	msg.action  =strs[10];
	msg.repeatStatus  =strs[11];
	return msg;
}
}
