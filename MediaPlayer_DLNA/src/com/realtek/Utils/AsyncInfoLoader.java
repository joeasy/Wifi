package com.realtek.Utils;



import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import android.app.TvManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class AsyncInfoLoader {
	private static final int MAX_AVAILABLE = 1;
	private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
	private HashMap<String, SoftReference<Long>> timeCache;
	private TvManager mTv = null;
	private static ArrayList<AsyncData> fileList ;
	private static boolean isQuit = false;
	private Thread processThd = null;
	//MediaPlayer mp = new MediaPlayer();
	public AsyncInfoLoader(TvManager mTv) {
		timeCache = new HashMap<String, SoftReference<Long>>();
    		this.mTv = mTv;
    		fileList = new ArrayList<AsyncData>(); 
    		if(processThd == null)
    		{
    			processThd = new Thread() {
    				@Override
    				public void run() {

    					while(!isQuit(1))
    					{
    						AsyncData data = opQueue(1,null);
    						if(data == null)
    						{
    							try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
    							continue;
    						}
    					try {
       						Long time = null;
    						if (timeCache.containsKey(data.gerUrl())) {
    							SoftReference<Long> softReference = timeCache
    									.get(data.gerUrl());
    							time = softReference.get();
    						}
    						if (time == null) {
    							time = loadTimeFromUrl(data.gerUrl());
    							if(time!=null && time.longValue()>0)
    								timeCache.put(data.gerUrl(), new SoftReference<Long>(
    									time));
    						}
    						Handler handler = data.getHandler();
    						Message message = handler.obtainMessage(0, time);
    						handler.sendMessage(message);
    					} catch (Exception e) {
    						e.printStackTrace();
    					}
    					}
    				}
    			};
    		}
    		processThd.start();
	}

	public Long loadTime(final String url,final int pos,
			final TimeInfoCallback infoCallback) {
		if (timeCache.containsKey(url)) {
			SoftReference<Long> softReference = timeCache.get(url);
			Long time = softReference.get();
			if (time != null) {
				return time;
			}
		}
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				infoCallback.infoLoaded((Long) message.obj, url,pos);
			}
		};
		/*new Thread() {
			@Override
			public void run() {
				try {
					available.acquire();
					Long time = null;
					if (timeCache.containsKey(url)) {
						SoftReference<Long> softReference = timeCache
								.get(url);
						time = softReference.get();
					}
					if (time == null) {
						time = loadTimeFromUrl(url);
						timeCache.put(url, new SoftReference<Long>(
								time));
					}
					Message message = handler.obtainMessage(0, time);
					handler.sendMessage(message);
					available.release();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();*/
		
		////
		AsyncData data = new AsyncData(url,handler);
		opQueue(2,data);
		return null;
	}

	public Long loadTimeFromUrl(String url) {
		return mTv.getMediaTotalTime("file://" +url);
	}

	public interface TimeInfoCallback {
		public void infoLoaded(Long time, String url,int pos);
	}
	public static synchronized AsyncData opQueue(int opCode,AsyncData data)
	{
		if(opCode == 0)
		{
			fileList.clear();
			return null;
		}
		else if(opCode == 1) {
			if(fileList.size() == 0)
			{
				return null;
			}
			else
			{
				AsyncData re = fileList.get(0);
				fileList.remove(0);
				return re;
			}
		}else
		{
			fileList.add(data);
			return null;
		}
			
	}
	
	public static synchronized boolean isQuit(int opCode)
	{
		if(opCode == -1)
		{
			isQuit = false;
			return true;
		}
		if(opCode == 0)
		{
			fileList.clear();
			isQuit = true;
			return true;
		}
		else {
			return isQuit;
		}
			
	}
	
	class AsyncData{
		private String url;
		private Handler handler;
		public AsyncData(String url,
				Handler handler){
			this.url = url;
			this.handler = handler;
		}
		public String gerUrl(){
			return url;
		}
		
		public Handler getHandler()
		{
			return handler;
		}
	}
}
