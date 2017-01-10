package com.realtek.cast.util;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageLocalStore {

	private static final String DIR = "img_cache";
	
	public static void saveImage(Context context, String key, ByteBuffer buffer) {
		File dst = getImageFile(context, key);
		
		
		try {
			if (dst.exists()) {
				dst.delete();
			}
	        FileOutputStream fos = new FileOutputStream(dst);
	        byte[] buf = new byte[4096];
	        while(buffer.remaining() > 0) {
	        	int count = Math.min(buf.length, buffer.remaining());
	        	buffer.get(buf, 0, count);
	        	fos.write(buf, 0, count);
	        }
	        fos.close();
	        
        } catch (FileNotFoundException e) {
	        e.printStackTrace();
        } catch (IOException e) {
	        e.printStackTrace();
        }
	}
	
	public static File getImageFile(Context context, String key) {
		File cache = new File(context.getCacheDir(), DIR);
		if (!cache.exists()) {
			cache.mkdirs();
		}
		
		File dst = new File(cache, key);
		return dst;
	}
}
