package com.realtek.Utils.Tagger;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import android.annotation.SuppressLint;
import android.os.Environment;

@SuppressLint({ "HandlerLeak", "SdCardPath" })
public class MultiThreadDown
{
    public static String url;
	public DownUtil downUtil;
	int flag =0;
	public MultiThreadDown(String uri,String type)
	{
		String path = Environment.getExternalStorageDirectory().getPath();
		url = uri;
		if(type.equals("mp3"))
		     downUtil = new DownUtil(url,path+"/a.mp3");
		else if(type.equals("wma"))
		     downUtil = new DownUtil(url,path+"/c.wma");
		else if(type.equals("mp4"))
		     downUtil = new DownUtil(url,path+"/d.mp4");
		else if(type.equals("flac"))
		     downUtil = new DownUtil(url,path+"/e.flac");
		else if(type.equals("ogg"))
		     downUtil = new DownUtil(url,path+"/f.ogg");
		try
		{
			downUtil.download();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}	
	}
}