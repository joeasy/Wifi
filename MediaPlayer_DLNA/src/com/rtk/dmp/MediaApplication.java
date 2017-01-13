/*
 * Copyright (C) 
 *
 * 
 * 
 * 
 *
 * 
 *
 * 
 * 
 * 
 * 
 * 
 */

package com.rtk.dmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5.DeviceStatusListener;
import com.realtek.Utils.ChannelAttr;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.MimeTypes;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.rtk.dmp.ChannelInfo.ChannelAtrr;

import android.app.Activity;
import android.app.Application;
import android.content.res.XmlResourceParser;
import android.media.MediaPlayer;
import android.util.DisplayMetrics;
import android.util.Log;

//import android.util.Log;

public class MediaApplication extends Application implements Observable {
	public String tag = "MediaApplication";
	public static final boolean DEBUG = false;
	public static final String internalStorage = "Internal Memory";
	private int screenWidth;// px
	private int screenHeight;// px
	private float scale;// densityDpi/160

	private boolean is4k2k = false;
	private boolean isx4k2k = false;

	private ArrayList<DLNAFileInfo> fileList = null;
	private ArrayList<DLNAFileInfo> playList = null;
	private ArrayList<ChannelAttr> attrlist = null;
	private ArrayList<DLNAFileInfo> deviceList = null;
	private ChannelAttr attr = null;
	private int fileDirnum = 0;// it is only for audio,do not use for other

	private MediaPlayer mPlayer = null;

	private MimeTypes mMimeTypes = null;

	public String mBookMarkName = null;
	public BookMark mBookMark = null;
	
	public DeviceInfo mDeviceInfo = null;
	public ChannelInfo mChannelInfo = null;
	public ArrayList<ChannelAtrr> mChannelAtrrList = null;

	public boolean isFromVideoPlayer = false;
	public String mStopedFileName = null;
	public String mStopedFileUrl = null;
	private Queue<String> errorVideoList = null;	//limit to 10;
	final int MAXERRORVIDEOLISTSIZE = 50;
	public String subRootPath = null;
	public String mediaServerName = null;
	public String mediaServerUUID = null;
	public String MediaServerIP = null;

	public static final int MAXFILENUM = 4096;
	private List<Activity> activityList = new LinkedList<Activity>();
	
	int[] vAddrForDTCP = {-1,-1};
	@Override
	public void onCreate() {
		super.onCreate();
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		scale = dm.density;
		if (DEBUG)
			Log.e("", "scale =" + scale);
		if (screenWidth >= 3840 || screenHeight >= 3840)
			is4k2k = true;
		if (scale >= 3)
			isx4k2k = true;
		fileList = new ArrayList<DLNAFileInfo>();
		deviceList = new ArrayList<DLNAFileInfo>();
		attrlist = new ArrayList<ChannelAttr>();
		addDeviceListener();
	}
	
	public int[] getAddrForDTCP()
	{
		return vAddrForDTCP;
	}
	public void setAddr(int[] addr)
	{
		vAddrForDTCP = addr;
	}

	public int getScreenWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		return screenWidth;
	}

	public int getScreenHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		screenHeight = dm.heightPixels;
		return screenHeight;
	}

	public float getScale() {
		return scale;
	}

	public boolean isIs4k2k() {
		return is4k2k;
	}

	public boolean isIsx4k2k() {
		return isx4k2k;
	}

	public ArrayList<DLNAFileInfo> getFileList() {
		return fileList;
	}
	
	public ArrayList<DLNAFileInfo> getDeviceList() {
		return deviceList;
	}

	public ArrayList<ChannelAttr> getChannelAttrList() {
		return attrlist;
	}
	
	public ChannelAttr getChannelAttr() {
		return attr;
	}

	public int getFileDirnum() {
		return fileDirnum;
	}

	public String getSubRootPath() {
		return subRootPath;
	}

	public void setSubRootPath(String path) {
		subRootPath = path;
	}

	public String getMediaServerName() {
		return mediaServerName;
	}

	public void setMediaServerName(String name) {
		if(name==null){
			mediaServerName="";
			return;
		}
		mediaServerName = name;
	}
	
	public void setMediaServerUUID(String uuid) {
		if(uuid == null) {
			mediaServerUUID = "";
		}
		mediaServerUUID = uuid;
	}
	
	public String getMediaServerUUID() {
		return mediaServerUUID;
	} 
	
	public String getMediaServerIP() {
		return MediaServerIP;
	}

	public void setMediaServerIP(String ip) {
		MediaServerIP = ip;
	}

	public void setFileDirnum(int fileDirnum) {
		this.fileDirnum = fileDirnum;
	}

	public MediaPlayer getMediaPlayer() {
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();
			Log.i(tag, "Create MediaPlayer!");
		}

		return mPlayer;
	}

	public void releaseMediaPlayer() {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
			System.gc();
		}
	}

	public void setMediaPlayerNull() {
		if (mPlayer != null) {
			mPlayer = null;
			System.gc();
		}
	}

	public MimeTypes getMimeTypes() {
		if (mMimeTypes == null) {
			XmlResourceParser mMimeTypeXml = getResources().getXml(
					R.xml.mimetypes);
			mMimeTypes = Util.GetMimeTypes(mMimeTypeXml);
		}

		return mMimeTypes;
	}

	public BookMark getBookMark(String name) {
		mBookMarkName = name;

		if (mBookMark == null || mBookMarkName == null
				|| mBookMarkName.compareTo(name) != 0)
			mBookMark = new BookMark(mBookMarkName);

		return mBookMark;
	}

	public DeviceInfo getDeviceInfo(String name) {
		if(mDeviceInfo == null)
			mDeviceInfo = new DeviceInfo(name);
		return mDeviceInfo;
	}
	
	public ChannelInfo getChannelInfo(String name) {
		if(mChannelInfo == null)
			mChannelInfo = new ChannelInfo(name);
		return mChannelInfo;
	}
	
	public ArrayList<ChannelAtrr> getChannelAtrrList(){
		if(mChannelAtrrList == null){
			mChannelAtrrList = new ArrayList<ChannelAtrr>();
		}
		return mChannelAtrrList;
	}

	public boolean getIsFromVideoPlayer() {
		return isFromVideoPlayer;
	}

	public void setIsFromVideoPlayer(boolean isVideoPlayer) {
		this.isFromVideoPlayer = isVideoPlayer;
	}
	
	public Queue<String> getErrorVideoList() {
		if(errorVideoList == null)
			errorVideoList = new LinkedList<String>();
		return errorVideoList;
	}
	
	public void AddErrorVideo(String url) {
		if(errorVideoList.size() < MAXERRORVIDEOLISTSIZE) {
			errorVideoList.add(url);
		} else {
			errorVideoList.poll();
		}
	}
	
	public void clearErrorVideoList() {
		errorVideoList.clear();
		errorVideoList = null;
	}
	
	public String getStopedFileName() {
		return mStopedFileName;
	}

	public void setStopedFileName(String FileName) {
		this.mStopedFileName = FileName;
	}
	
	public String getStopedFileUrl() {
		return mStopedFileUrl;
	}
	
	public void setStopedFileUrl(String FileUrl) {
		this.mStopedFileUrl = FileUrl;
	}

	public ArrayList<DLNAFileInfo> getPlayList() {
		return playList;
	}

	public void setPlayList(ArrayList<DLNAFileInfo> playList) {
		this.playList = playList;
	}

	public void addActivity(Activity activity) {
		activityList.add(activity);
	}

	public void exit() {
		for (Activity activity : activityList) {
			if (activity != null)
				activity.finish();
		}
		System.exit(0);
	}

	private DLNA_DMP_1p5 dlna_DMP_1p5;

	private void addDeviceListener() {
		DeviceStatusListener mDeviceListener = new DeviceStatusListener() {
			@Override
			public void deviceAdded(String serverName) {
				if(serverName == null)
					return;
				ObserverContent content = new ObserverContent(
						ObserverContent.ADD_DEVICE, serverName);
				notifyObservers(content);
			}

			@Override
			public void deviceRemoved(String serverName) {
				if(serverName == null)
					return;
				ObserverContent content = new ObserverContent(
						ObserverContent.REMOVE_DEVICE, serverName);
				notifyObservers(content);
			}
		};
		dlna_DMP_1p5 = new DLNA_DMP_1p5();
		dlna_DMP_1p5.setDeviceStatusListener(mDeviceListener);
	}

	public synchronized void addObserver(Observer obs) {
		Log.i("TAG", "addObserver(" + obs + ")");
		if (mObservers.indexOf(obs) < 0) {
			mObservers.add(obs);
		}
	}

	public synchronized void deleteObserver(Observer obs) {
		Log.i("TAG", "deleteObserver(" + obs + ")");
		mObservers.remove(obs);
	}

	private void notifyObservers(Object arg) {
		Log.i("TAG", "notifyObservers(" + arg + ")");
		for (Observer obs : mObservers) {
			Log.i("TAG", "notify observer = " + obs);
			obs.update(this, arg);
		}
	}

	private List<Observer> mObservers = new ArrayList<Observer>();
	
	public void exitApp(){
		ObserverContent content = new ObserverContent(
				ObserverContent.EXIT_APP, "");
		notifyObservers(content);
	}
}
