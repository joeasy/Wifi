package com.rtk.dmp;

import java.util.ArrayList;
import android.util.Log;


import com.rtk.dmp.Level_Info;

public class Path_Info 
{
	
	public int dirLevel = -1; 
	public ArrayList<Level_Info> curPathArr = new ArrayList<Level_Info>();
	
	// level info handling
	public void addLevelInfo(String path)
	{
	    Level_Info info = new Level_Info();
	    info.path = path;
	    info.position = 0;
	    curPathArr.add(info);
	    dirLevel++;
	}
	    
    public void cleanLevelInfo()
    {
        curPathArr.clear();
	    dirLevel = -1;     		            
	}
		    
    public void backToLastLevel() 
	{
        Log.d("RTKDMP", "backToLastLevel");
        curPathArr.remove(dirLevel);
		dirLevel--;		            	
		//getFileListByPath(curPathArr.get(dirLevel).path, MediaUtils.getFileExt(cur_type));
		Log.d("RTKDMP", "backToLastLevel end");
	}  
    
    //hartley add
    public void backToDeviceLevel()
    {
    	while(dirLevel > 0)
    	{
	    	curPathArr.remove(dirLevel);
			dirLevel--;	
    	}
    }
    public String getDeviceLevelPath()
    {
    	return curPathArr.get(0).path;
    }
    
    public void setLevelFocus(int level, int position)
    {
        curPathArr.get(level).position = position;
    }
    
    public void setLastLevelFocus(int position)
    {
        curPathArr.get(dirLevel).position = position;
    }
    
	    
	public int getLastLevel()
	{
	    return dirLevel;
	}
	    
	public String getLastLevelPath()
	{
	    return curPathArr.get(dirLevel).path;
	}
	    
	   
	public int getLastLevelFocus()
	{
	    return curPathArr.get(dirLevel).position;
	}

}
