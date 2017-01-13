package com.rtk.dmp;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;
import android.util.Log;

import com.realtek.Utils.MimeTypeParser;
import com.realtek.Utils.MimeTypes;

public class Util{
	
	/**
	 * 
	 * 
	 * @param time
	 * @return
	 */
	
	static MimeTypes mMimeTypes=null;
	public static String toTime(long time) {

		time /= 1000;
		long minute = time / 60;
		long second = time % 60;
		long hour = minute / 60;
		minute %= 60;
		return String.format("%02d:%02d:%02d",hour, minute, second);
	}
	
	public static String toSecondTime(long time) {
		long minute = time / 60;
		long second = time % 60;
		long hour = minute / 60;
		minute %= 60;
		return String.format("%02d:%02d:%02d",hour, minute, second);
	}
	
	
	public static  MimeTypes GetMimeTypes(XmlResourceParser mimeTypeXml) 
	{
		if(mMimeTypes != null)
			return mMimeTypes;
		
		MimeTypeParser mtp = new MimeTypeParser();
		
		try  
		{
			mMimeTypes = mtp.fromXmlResource(mimeTypeXml);   		 	
   	 	} 
		catch (XmlPullParserException e)
		{
   		 	Log.d("Utils.GetMimeTypes","PreselectedChannelsActivity: XmlPullParserException",e);
	   		throw new RuntimeException("PreselectedChannelsActivity: XmlPullParserException");
		} 
		catch (IOException e) 
		{
			Log.d("Utils.GetMimeTypes", "PreselectedChannelsActivity: IOException", e);
   		 	throw new RuntimeException("PreselectedChannelsActivity: IOException");
   	 	}
		
		return mMimeTypes;
    }
	
	public static void DestoryMimeTypes ()
	{
		mMimeTypes = null;
	}
	
	
}