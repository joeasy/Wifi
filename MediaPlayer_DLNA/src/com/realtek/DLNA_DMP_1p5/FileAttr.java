package com.realtek.DLNA_DMP_1p5;

import com.realtek.DataProvider.FileFilterType;

public class FileAttr {
	private int mFileType = FileFilterType.DEVICE_FILE_NONE;
	private int mIndex = -1;
	
	public FileAttr(int fileType, int index)
	{
		mFileType = fileType;
		mIndex = index;
	}
	
	public int GetFileType()
	{
		return mFileType;
	}
	
	public int GetIndex()
	{
		return mIndex;
	}
}
