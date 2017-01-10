package com.realtek.Utils;


import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import android.webkit.MimeTypeMap;

public class MimeTypes {

	private Map<String, String> mMimeTypes;

	public MimeTypes() {
		mMimeTypes = new HashMap<String,String>();
	}
	
	public void put(String type, String extension) {
		// Convert extensions to lower case letters for easier comparison
		extension = extension.toLowerCase();
		
		mMimeTypes.put(type, extension);
	}
	
	public String getMimeType(String filename) {
		
		//Log.d("FileFilter","getMimeType filename:"+filename);
		String extension = FileUtils.getExtension(filename);
		extension =extension.toLowerCase();
		
		//Log.d("FileFilter","getMimeType extension:"+extension);
		
		// Let's check the official map first. Webkit has a nice extension-to-MIME map.
		// Be sure to remove the first character from the extension, which is the "." character.
		/*if (extension.length() > 0)
		{
			Log.d("FileFilter","getMimeType extension:1");
			String webkitMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1));
		
			Log.d("FileFilter","getMimeType extension:2"+webkitMimeType);
			if (webkitMimeType != null) {
				// Found one. Let's take it!
				
				Log.d("FileFilter","getMimeType webkitMimeType:"+webkitMimeType);
				return webkitMimeType;
			}
		}
		
		Log.d("FileFilter","getMimeType extension:3");*/
		
		// Convert extensions to lower case letters for easier comparison
		extension = extension.toLowerCase();
		
		//Log.d("FileFilter","getMimeType extension:4 "+ extension);
		
		String mimetype = mMimeTypes.get(extension);
		
		if(mimetype==null) return null;
		
		
		//Log.d("FileFilter","getMimeType mimetype:"+mimetype);
		return mimetype;
	}
	
	public boolean isImageFile(String filename)
	{
		if(filename == null) 
			return false;
		
		String mimeType = this.getMimeType(filename);
		
		if(mimeType == null)
			return false;
		
		if((mimeType.substring(0, 6)).equals("image/"))
		{
			return true;
		}
			
		return false;	
	}
	
	public boolean isVideoFile(String filename)
	{
		if(filename == null) 
			return false;
		
		String mimeType = this.getMimeType(filename);
		
		if(mimeType == null)
			return false;
				
		
		if((mimeType.substring(0, 6)).equals("video/") || 
		   (mimeType.length()>=12 && (mimeType.substring(0, 12)).equals("video_audio/"))
		  )
		{
			return true;
		}
			
		return false;	
	}
	
	public boolean isAudioFile(String filename)
	{
		if(filename == null) 
			return false;
		
		String mimeType = this.getMimeType(filename);
		
		if(mimeType == null)
			return false;
		
		
		if((mimeType.substring(0, 6)).equals("audio/") || 
				   (mimeType.length()>=12 && (mimeType.substring(0, 12)).equals("video_audio/"))
		  )
	    {
			return true;
		}
			
		return false;	
	}

	//VDir file
	public boolean isVDirFile(String filename)
	{
		if(filename == null) 
			return false;
		
		String mimeType = this.getMimeType(filename);
		
		if(mimeType == null)
			return false;
		
		
		if(mimeType.contains("vdir/"))
	    {
			return true;
		}
			
		return false;	
	}

}
