package com.realtek.Utils;



import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.realtek.Utils.Tagger.ParserSingleton;
import com.realtek.Utils.Tagger.ParserSingleton.AudioInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class AsyncImageLoader {
	private static HashMap<String, SoftReference<Bitmap>> imageCache;
	private static ArrayList<AsyncData> fileList ;
	private static boolean isQuit = false;
	private static Thread processThd = null;
	ParserSingleton instance;
	private static AsyncImageLoader loader;
	public static AsyncImageLoader getInstance(){  
        synchronized(AsyncImageLoader.class){  
        	loader = loader == null ? new AsyncImageLoader() : loader ;  
        }  
        return loader ;   
    }
	private AsyncImageLoader() {
		instance = ParserSingleton.getInstance();
		if(map==null)
			map = new HashMap<String, ImageInfoCallback>();
		if(imageCache ==null)
			imageCache = new HashMap<String, SoftReference<Bitmap>>();
		if(fileList == null)
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
    						Bitmap img = null;
    						if (imageCache.containsKey(data.gerUrl())) {
    							SoftReference<Bitmap> softReference = imageCache
    									.get(data.gerUrl());
    							img = softReference.get();
    						}
    						if (img == null) {
    							img = loadImageFromUrl(data.gerUrl());
    							if(img!=null)
    								imageCache.put(data.gerUrl(), new SoftReference<Bitmap>(
    									img));
    						}
    						Handler handler = data.getHandler();
    						Message message = handler.obtainMessage(0, img);
    						Bundle bun = new Bundle();
    						bun.putBoolean("cancel", false);
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

	public Bitmap loadImage(final String url,final int pos,
			 ImageInfoCallback infoCallback) {
		if (imageCache.containsKey(url)) {
			SoftReference<Bitmap> softReference = imageCache.get(url);
			Bitmap img = softReference.get();
			if (img != null) {
				return img;
			}
		}
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				ImageInfoCallback callback =map.get(url);
				Bundle bun = message.getData();
				if(callback!=null)
				callback.infoLoaded((Bitmap) message.obj, url,pos,bun.getBoolean("cancel"));
			}
		};
		
		AsyncData data = new AsyncData(url,handler);
		opQueue(2,data);
		map.put(url, infoCallback);
		return null;
	}

	private static Map<String, ImageInfoCallback> map =null;
	public Bitmap loadImageFromUrl(String url) {
		if(url != null)
		{
			AudioInfo info = instance.Parser(url);
			try {
				if(info.artwork!=null)
					return info.artwork.getImage();
				else return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return null;
	}

	public interface ImageInfoCallback {
		public void infoLoaded(Bitmap img, String url,int pos,boolean cancel);
	}
	public static synchronized AsyncData opQueue(int opCode,AsyncData data)
	{
		if(opCode == 0)
		{
			while(!fileList.isEmpty()){
				AsyncData re = fileList.get(0);
				fileList.remove(0);
				Handler handler = re.getHandler();
				Message message = handler.obtainMessage(0, null);
				Bundle bun = new Bundle();
				bun.putBoolean("cancel", true);
				message.setData(bun);
				handler.sendMessage(message);
			}
			map.clear();
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
			processThd = null;
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
