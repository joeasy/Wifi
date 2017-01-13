package com.rtk.dmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class TitleBookMark {
	
	private final static String TAG = "TitleBookMark";
	private int maxCount = 2;
	
	public class BookMarkTitle
	{
		int titleNum;
		int subtitleTrack;
		int subtitleEnable;
		int audioTrack;
		byte[] navBuffer;
	}
	
	public ArrayList<BookMarkTitle> bookMarkList = new ArrayList<BookMarkTitle>();
	
	public TitleBookMark() {
		cleanBookMark();
	}
	
	public void cleanBookMark()
	{
		bookMarkList.clear();
	}
	
	public int bookMarkLength()
	{
		return bookMarkList.size();
	}
	
	public void addBookMark(int title, int subtitleNum, int subtitleOn, int audioNum, byte[] buffer)
	{
		BookMarkTitle mark = new BookMarkTitle();
		
		mark.titleNum = title;
		mark.subtitleTrack = subtitleNum;
		mark.subtitleEnable = subtitleOn;
		mark.audioTrack = audioNum;
		mark.navBuffer = buffer;
		
		Log.v(TAG, "Add a BookMark:");
		Log.v(TAG, "  title:" + mark.titleNum);
		Log.v(TAG, "  subtitle:" + mark.subtitleTrack + " on:" + mark.subtitleEnable);
		Log.v(TAG, "  audio:" + mark.audioTrack);
		
		if (bookMarkList.size() < maxCount)
		{
			bookMarkList.add(mark);
		}else 
		{
			bookMarkList.remove(0);
			bookMarkList.add(mark);
		}
	}
	
	public void removeBookMark(int index)
	{
		if (index >= 0 && index < bookMarkList.size())
			bookMarkList.remove(index);
	}
	
	/*
	public void removeBookMark(String name)
	{
		int index = findBookMark(name);
		if (index >= 0)
			bookMarkList.remove(index);
	}
	*/
	
	public int findBookMark(int titleNum)
	{
		int i = 0;
		int length = bookMarkList.size();
		
		while (i < length)
     	{
			if (bookMarkList.get(i).titleNum == titleNum)
     			break;
			
     		i++;
     	}
		
		if (i == length)
			return -1;
		
		return i;
	}
	
	public int getTitleNum(int index)
	{
		if (index >= 0 && index < bookMarkList.size())
			return bookMarkList.get(index).titleNum;
		
		return -1;
	}
	
	public int getSubtitleTrack(int index)
	{
		if (index >= 0 && index < bookMarkList.size())
			return bookMarkList.get(index).subtitleTrack;
		
		return -1;
	}
	
	public int isSubtitleOn(int index)
	{
		if (index >= 0 && index < bookMarkList.size())
			return bookMarkList.get(index).subtitleEnable;
		
		return 0;
	}
	
	public int getAudioTrack(int index)
	{
		if (index >= 0 && index < bookMarkList.size())
			return bookMarkList.get(index).audioTrack;
		
		return -1;
	}
	
	public byte[] getNavBuffer(int index)
	{
		if (index >= 0 && index < bookMarkList.size())
			return bookMarkList.get(index).navBuffer;
		
		return null;
	}
	
	public static int byte2int(byte[] res)
	{
		int targets = ((char)res[0] | 
					((char)(res[1] & 0xff) << 8)| 
					((char)(res[2] & 0xff) << 16) | 
					((char)(res[3] & 0xff) << 24)); 
		return targets;
	}
	
	public static byte[] int2byte(int data)
	{
		byte [] targets = new byte [4];
		
		targets[0] = (byte)(data & 0xff);
		targets[1] = (byte)((data >> 8) & 0xff);
		targets[2] = (byte)((data >> 16) & 0xff);
		targets[3] = (byte)((data >> 24) & 0xff);
		
		return targets;
	}
}
