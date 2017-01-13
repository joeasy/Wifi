package com.rtk.dmp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class PlaybackBookMark {
	
	private final static String TAG = "PlaybackBookMark";
	
	final String mVideoBookMarkFileName = "VideoBookMark.bin";
	
	public int MaxLength = 1;
	public int TitleMaxLength = 3;
	
	public Activity mContext = null;
	
	public class BookMarkTitle
	{
		int titleNum;
		byte[] inputArray;
		int subtileStreamNum;
		int SPU_ENABLE;
		int audioStreamNum;
	}
	
	public class BookMarkFile
	{	
		int FileNameLen;
		String FileName;
		int inputArrayLen;
		byte[] inputArray;
	}
	
	public ArrayList<BookMarkTitle> nav_state_title = new ArrayList<BookMarkTitle>();
	public ArrayList<BookMarkFile> nav_state_file = new ArrayList<BookMarkFile>();
	
	public PlaybackBookMark(VideoBrowser context)
	{
		mContext = context;
		cleanNavState(nav_state_file);
		readVideoBookMark();
	}
	
	public PlaybackBookMark(VideoPlayerActivity context) {
		mContext = context;
		cleanNavState(nav_state_title);
		cleanNavState(nav_state_file);
		readVideoBookMark();
	}

	public void addNavState(int titleNum, byte[] inputArray, int subtileStreamNum, int SPU_ENABLE, int audioStreamNum)
	{
		BookMarkTitle MarkTitle = new BookMarkTitle();
		MarkTitle.titleNum = titleNum;
		MarkTitle.inputArray = inputArray;
		MarkTitle.subtileStreamNum = subtileStreamNum;
		MarkTitle.SPU_ENABLE = SPU_ENABLE;
		MarkTitle.audioStreamNum = audioStreamNum;
		
		if(nav_state_title.size()< TitleMaxLength)
		{
			nav_state_title.add(MarkTitle);
		}else 
		{
			nav_state_title.remove(0);
			nav_state_title.add(MarkTitle);
		}
	}
	
	public void addNavState(String FileName, byte[] inputArray)
	{
		Log.e(TAG, "addNavState");
		BookMarkFile MarkFile = new BookMarkFile();
		MarkFile.FileNameLen = FileName.getBytes().length;
		MarkFile.FileName = FileName;
		MarkFile.inputArrayLen = inputArray.length;
		MarkFile.inputArray = inputArray;
		
		if(nav_state_file.size() < MaxLength)
		{
			nav_state_file.add(MarkFile);
		}else 
		{
			nav_state_file.remove(0);
			nav_state_file.add(MarkFile);
		}
		
	}
	
	public void removeNavState(ArrayList<?> list, int index)
	{
		if(index >= 0 && list != null)
		{
			Log.e(TAG, "position = "+index);
			list.remove(index);
		}
			
	}
	
	public void cleanNavState(ArrayList<?> list)
	{
		if(list != null)
		{
			Log.e(TAG, "list.clear()");
			list.clear();
		}
	}
	
	public void readVideoBookMark()
	{
		try {
			FileInputStream fis = mContext.openFileInput(mVideoBookMarkFileName);
			int hasRead = 0;
			byte[] tmp1 = new byte[4];
			hasRead = fis.read(tmp1, 0, 4);
			while(hasRead > 0)
			{	
				BookMarkFile MarkFile = new BookMarkFile();
				
				//MarkFile.FileNameLen
				MarkFile.FileNameLen = byte2int(tmp1);
				Log.e(TAG, "MarkFile.FileNameLen = " + MarkFile.FileNameLen);
				
				//MarkFile.FileName
				byte[] tmp2 = new byte[MarkFile.FileNameLen];
				fis.read(tmp2, 0, MarkFile.FileNameLen);
				MarkFile.FileName = new String(tmp2);
				Log.e(TAG, "MarkFile.FileName = "+MarkFile.FileName);
				
				//MarkFile.inputArrayLen
				fis.read(tmp1, 0, 4);
				MarkFile.inputArrayLen = byte2int(tmp1);
				Log.e(TAG, "MarkFile.inputArrayLen = "+MarkFile.inputArrayLen);
				
				//MarkFile.inputArray
				tmp2 = new byte[MarkFile.inputArrayLen];
				fis.read(tmp2, 0, MarkFile.inputArrayLen);
				MarkFile.inputArray = tmp2;
				
				/*for(int i = 0; i < 8; i++)
					Log.e(TAG, "MarkFile.inputArray[i] = "+MarkFile.inputArray[i]);
				int titleNum = ((char)MarkFile.inputArray[0] 
						| (((char)MarkFile.inputArray[1] & 0xFF) << 8)
						| (((char)MarkFile.inputArray[2] & 0xFF) << 16) 
						| (((char)MarkFile.inputArray[3] & 0xFF) << 24));
				int elapsedtime = ((char)MarkFile.inputArray[4] 
						| (((char)MarkFile.inputArray[5] & 0xFF) << 8)
						| (((char)MarkFile.inputArray[6] & 0xFF) << 16) 
						| (((char)MarkFile.inputArray[7] & 0xFF) << 24));
				Log.e(TAG, "titleNum = " +titleNum);
				Log.e(TAG, "elapsedtime = "+elapsedtime);*/ //For MKV
				
				nav_state_file.add(MarkFile);
				
				hasRead = fis.read(tmp1, 0, 4);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void writeVideoBookMark()
	{
		try{
			//delete file 
			mContext.deleteFile(mVideoBookMarkFileName);
			
			FileOutputStream fos = mContext.openFileOutput(mVideoBookMarkFileName, Context.MODE_APPEND);
			int i = 0;
			Log.e(TAG, "nav_state_file.size() = "+nav_state_file.size());
			while(i < nav_state_file.size())
			{
				Log.e(TAG, "nav_state_file.get(i).FileNameLen = " +
						nav_state_file.get(i).FileNameLen);
				fos.write(int2byte(nav_state_file.get(i).FileNameLen));
				fos.write(nav_state_file.get(i).FileName.getBytes());
				fos.write(int2byte(nav_state_file.get(i).inputArrayLen));
				Log.e(TAG, "nav_state_file.get(i).inputArrayLen = " + nav_state_file.get(i).inputArrayLen);
				fos.write(nav_state_file.get(i).inputArray);
				i++;
			}
			fos.close();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public int findBookMark(String name)
	{
		int i = 0;
		int length = nav_state_file.size();
		
		while (i < length)
     	{
			if (nav_state_file.get(i).FileName.compareTo(name)== 0)
     			break;
			
     		i++;
     	}
		
		if (i == length)
			return -1;
		
		return i;
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
