package com.rtk.dmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class BookMark {
	
	private final static String TAG = "BookMark";

	private String bookMarkFileName = null;
	//public Context mContext = null;
	private int maxCount = 1;
	
	public class BookMarkFile
	{	
		int fileUrlLen;
		String fileUrl;
		int fileNameLen;
		String fileName;
		int subtitleTrack;
		int subtitleEnable;
		int audioTrack;
		int bufferLen;
		byte[] navBuffer;
	}
	
	public ArrayList<BookMarkFile> bookMarkList = new ArrayList<BookMarkFile>();
	
	public BookMark(/*Context context,*/ String fileName) {
		//mContext = context;
		bookMarkFileName = fileName;
		cleanBookMark();
		readBookMark();
	}
	
	public void cleanBookMark()
	{
		bookMarkList.clear();
	}
	
	public int bookMarkLength()
	{
		return bookMarkList.size();
	}
	
	public void addBookMark(String fileName, int subtitleNum, int subtitleOn, int audioNum, byte[] buffer)
	{
		BookMarkFile mark = new BookMarkFile();
		mark.fileUrl = "";
		mark.fileUrlLen = 0;
		mark.fileNameLen = fileName.getBytes().length;
		mark.fileName = fileName;
		mark.subtitleTrack = subtitleNum;
		mark.subtitleEnable = subtitleOn;
		mark.audioTrack = audioNum;
		mark.bufferLen = buffer.length;
		mark.navBuffer = buffer;
		
		Log.v(TAG, "Add a BookMark:");
		Log.v(TAG, "  name:" + mark.fileName + ", length:" + mark.fileNameLen);
		Log.v(TAG, "  buffer length:" + mark.bufferLen);
		
		if (bookMarkList.size() < maxCount)
		{
			bookMarkList.add(mark);
		}else 
		{
			bookMarkList.remove(0);
			bookMarkList.add(mark);
		}
	}
	
	public void addBookMark(String fileUrl, String fileName, int subtitleNum, int subtitleOn, int audioNum, byte[] buffer)
	{
		BookMarkFile mark = new BookMarkFile();
		mark.fileUrlLen = fileUrl.length();
		mark.fileUrl = fileUrl;
		mark.fileNameLen = fileName.getBytes().length;
		mark.fileName = fileName;
		mark.subtitleTrack = subtitleNum;
		mark.subtitleEnable = subtitleOn;
		mark.audioTrack = audioNum;
		mark.bufferLen = buffer.length;
		mark.navBuffer = buffer;
		
		Log.v(TAG, "Add a BookMark:");
		Log.v(TAG, "  name:" + mark.fileName + ", length:" + mark.fileNameLen);
		Log.v(TAG, "  buffer length:" + mark.bufferLen);
		
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
	
	public void removeBookMark(String name)
	{
		if(name != null)
		{
			int index = findBookMark(name);
			if (index >= 0)
				bookMarkList.remove(index);
		}
	}
	
	public int findBookMarkWithUrl(String url) {
		int i = 0;
		int length = bookMarkList.size();
		
		while (i < length) {
			if(bookMarkList.get(i).fileUrl.equals(url))
				break;
			i++;
		}
		if(i == length)
			return -1;
		
		return i;
	}
	
	public int findBookMark(String name)
	{
		int i = 0;
		int length = bookMarkList.size();
		
		while (i < length)
     	{
			if (bookMarkList.get(i).fileName.compareTo(name)== 0)
     			break;
     		i++;
     	}
		
		if (i == length)
			return -1;
		
		return i;
	}
	
	
	public void readBookMark()
	{
		if (bookMarkFileName == null)
		{
			Log.e(TAG, "BookMark file name null!");
			return;
		}
		
		try {
			File f = new File(bookMarkFileName);
			if(!f.exists()) {
				f.createNewFile();
				return ;
			}
			FileInputStream fis = new FileInputStream(f);
			//FileInputStream fis = mContext.openFileInput(bookMarkFileName);
			int hasRead = 0;
			byte[] buf = new byte[4];
			hasRead = fis.read(buf, 0, 4);
			while(hasRead > 0)
			{	
				int length = 0;
				String fileUrl_tmp = "";
				String name_tmp = "";
				length = byte2int(buf);
				if(length > 0) {
					byte[] fileUrlBuf = new byte[length];
					fis.read(fileUrlBuf, 0, length);
					fileUrl_tmp = new String(fileUrlBuf);
				}
				fis.read(buf, 0, 4);
				length = byte2int(buf);
				if(length > 0) {
					byte[] nameBuf = new byte[length];
					fis.read(nameBuf, 0, length);
					name_tmp = new String(nameBuf);
				}
				
				fis.read(buf, 0, 4);
				int subtitleNum = byte2int(buf);
				
				fis.read(buf, 0, 4);
				int subtitleOn = byte2int(buf);
				
				fis.read(buf, 0, 4);
				int audioNum = byte2int(buf);
				
				fis.read(buf, 0, 4);
				length = byte2int(buf);
				byte[] markBuf = new byte[length];
				fis.read(markBuf, 0, length);
				
				addBookMark(fileUrl_tmp, name_tmp, subtitleNum, subtitleOn, audioNum, markBuf);
				
				hasRead = fis.read(buf, 0, 4);
			}
			fis.close();
		} catch (FileNotFoundException e) {
			Log.v("Read BookMark", "BookMark error");
			e.printStackTrace();
		} catch (IOException e) {
			Log.v("Read BookMark", "BookMark error");
			e.printStackTrace();
		}
	}
	
	public void writeBookMark()
	{
		try {
			//mContext.deleteFile(bookMarkFileName);
			
			File f = new File(bookMarkFileName);
			f.delete();
			FileOutputStream fos = new FileOutputStream(f, true);
			//FileOutputStream fos = mContext.openFileOutput(bookMarkFileName, Context.MODE_APPEND);
			int i = 0;
			int count = bookMarkList.size();
			while (i < count)
			{
				BookMarkFile mark = bookMarkList.get(i);
				
				Log.v(TAG, "Write a BookMark: " + mark.fileName);
				fos.write(int2byte(mark.fileUrlLen));
				fos.write(mark.fileUrl.getBytes());
				fos.write(int2byte(mark.fileNameLen));
				fos.write(mark.fileName.getBytes());
				fos.write(int2byte(mark.subtitleTrack));
				fos.write(int2byte(mark.subtitleEnable));
				fos.write(int2byte(mark.audioTrack));
				fos.write(int2byte(mark.bufferLen));
				fos.write(mark.navBuffer);
				i++;
			}
			fos.close();
		} catch(Exception e) {
			Log.v("Write BookMark", "BookMark error");
			e.printStackTrace();
		}
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
