/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.realtek.bitmapfun.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


import android.app.TvManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images
 * fetched from a URL.
 */
public class ImageFetcher extends ImageResizer {
    private static final String TAG = "ImageFetcher";
    private static final int HTTP_CACHE_SIZE = 1024*10*1024; // 10MB---->1K
    public static final String HTTP_CACHE_DIR = "http";
     File cacheDir = null;//DiskLruCache.getDiskCacheDir(context, HTTP_CACHE_DIR);
     ExifInterface exif_http = null;
     DiskLruCache cache =null;
     TvManager mTv ;
     Object lock = new Object();

            //DiskLruCache.openCache(context, cacheDir, HTTP_CACHE_SIZE);
     private static int order = 0;
     private final Map<String, String> mLinkedHashMapdownLoading =
             Collections.synchronizedMap(new LinkedHashMap<String, String>(
                     32, 0.75f, true));
     private static int orderFlag = 0;;
     private int[] vAddrForDTCP ={-1,-1};
    
    /**
     * Initialize providing a target image width and height for the processing
     * images.
     * 
     * @param context
     * @param loadingControl
     * @param imageWidth
     * @param imageHeight
     */
    public ImageFetcher(Context context, LoadingControl loadingControl,
            int imageWidth, int imageHeight)
    {
        super(context, loadingControl, imageWidth, imageHeight);
        init(context);
    }

    /**
     * Initialize providing a single target image size (used for both width and
     * height);
     * 
     * @param context
     * @param loadingControl
     * @param imageSize
     */
    public ImageFetcher(Context context, LoadingControl loadingControl,
            int imageSize)
    {
        super(context, loadingControl, imageSize);
        init(context);
    }

    private void init(Context context) {
    	mTv  = (TvManager)mContext.getSystemService("tv");
        checkConnection(context);
        cacheDir = DiskLruCache.getDiskCacheDir(context, HTTP_CACHE_DIR);

        cache = DiskLruCache.openCache(context, cacheDir, HTTP_CACHE_SIZE);
        cache.clearCache();
    	
    }

    /**
     * Simple network connection check.
     * 
     * @param context
     */
    private void checkConnection(Context context) {
    	/*
        final ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            Toast.makeText(context, "No network connection found.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "checkConnection - no connection found");
        }*/
    }

    /**
     * The main process method, which will be called by the ImageWorker in the
     * AsyncTask background thread.
     * 
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @return The downloaded and resized bitmap
     */
    private Bitmap processBitmap(String data) {
        return processBitmap(data, mImageWidth, mImageHeight);
    }

    /**
     * The main process method, which will be called by the ImageWorker in the
     * AsyncTask background thread.
     * 
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @param imageWidth
     * @param imageHeight
     * @return The downloaded and resized bitmap
     */
    protected Bitmap processBitmap(String data, int imageWidth, int imageHeight) {

        if (data == null)
        {
            return null;
        }
        
        
    	if(data !=null)
    	{
    		//android.resource:// 

    		String tmpStr=null,
    				httpTmpStr=null;
    		if(data.length()>19)
    			 tmpStr = data.substring(0, 19);
    		
    		if(data.length()>19)
    			 httpTmpStr = data.substring(0, 7);
    		
    		//Log.d(TAG, "httpTmpStr - " + httpTmpStr);
        	
    		
    		
    		if(tmpStr != null && tmpStr.equals("android.resource://"))
    		{
    			return processBitmap(Integer.parseInt( data.substring(tmpStr.length(), data.length())), imageWidth,imageHeight);
    		}
    		else if(httpTmpStr!=null && httpTmpStr.equals("http://"))
    		{
    			bCancel = false;
    			
    			File f = null;
    			
    			f=getFileFromcache(data,false,this.vAddrForDTCP);

    	        if (f != null) {
    	            // Return a sampled down version
    	        	
    	        	if(exif_http != null)
    	        	{
	    	        	byte[] bThumbnail=exif_http.getThumbnail();
	    	        	if(bThumbnail != null)
	    	        	{
		    	        	Bitmap bitmap = BitmapFactory.decodeByteArray(bThumbnail,0, bThumbnail.length);
		    	    	   	Log.d(TAG,"thumbnail: "+bitmap.getHeight()+ "bitmap"+bitmap.getWidth());  
		    	    	   	
		    	    	   	int ori = exif_http.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
		    	    	   	int digree =0; 
		    	    		switch (ori) 
		    	    		{ 
								case ExifInterface.ORIENTATION_ROTATE_90: 
									digree = 90; 
									break; 
								case ExifInterface.ORIENTATION_ROTATE_180: 
									digree = 180; 
									break; 
								case ExifInterface.ORIENTATION_ROTATE_270: 
									digree = 270; 
									break; 
								default: 
									digree = 0; 
									break; 
		    	    		}
		    	    		if (digree != 0) { 
		    	    			Matrix m = new Matrix(); 
		    	    			m.postRotate(digree); 
		    	    			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),bitmap.getHeight(), m, true); 
		    	    		} 
		    	    	   	if(bitmap == null)
		    	    	   	{     	
		    	    	   		Log.d(TAG,"BitmapFactory.decodeByteArray() from thumbnail failed!!");
		    	    	   	}
		    	    	   	else
		    	    	   	{
		    	    	   		return bitmap;
		    	    	   	}
	    	        	}
    	        	}
    	        	Bitmap bitmap = decodeSampledBitmapFromFile(f.toString(), imageWidth, imageHeight);  
    	        	return bitmap;

    	        }
    		}
    		else
    		{
    			 // Download a bitmap, write it to a file
    	        final File f = new File(data);

    	        if (f == null || f.exists() == false)
    	        {	
    	        	return null;
    	        }
    			// Return a sampled down version
    			return decodeSampledBitmapFromFile(data, imageWidth, imageHeight);
    		}	
    			
    	}	
    	
        return null;
    }

    
    @Override
    protected Bitmap processBitmap(Object[] data) {
    	boolean isExif=(Boolean) data[1];
    	this.vAddrForDTCP = (int[])data[2];
    	if(isExif!=true)
    	{
    		String path= String.valueOf(data[0]);
    		return processBitmap(path);
    	}
    	else if(isExif==true)
    	{
    		String exifpath=String.valueOf(data[0]);
    		return processBitmap(exifpath,isExif);
    	}
    	else 
    	{
    		//Log.d(TAG, "processBitmap get arg failed");
    		return null;
    	}
        
    }
    @Override
	public File getFileFromcache(String url,boolean isfullscreen,int[] vAddrForDTCP)
    {

    			String tailStr = null;
    			if(url.contains(" "))
    			{
    				tailStr = url.substring(url.indexOf(" "));
    			}
    				
    			if(tailStr != null&&tailStr.contains("protocolinfo"))
    			{

					if(mLinkedHashMapdownLoading.isEmpty())
					{
						
						return downloadBitmapDTCPIPBuffer(url,isfullscreen,vAddrForDTCP);
						
					}
					else
					{
						try {
							Thread.sleep(400);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

    			}
    			else
    			{
    				return downloadBitmap(mContext,url,isfullscreen);
    			}
    	return null;
    }
   
    /**
     * Download a bitmap from a URL, write it to a disk and return the File
     * pointer. This implementation uses a simple disk cache.
     * 
     * @param context The context to use
     * @param urlString The URL to fetch
     * @return A File pointing to the fetched bitmap
     */

    public File downloadBitmap(Context context, String urlString,boolean isFullscreen)   
    {
    	//Log.d(TAG, "downloadBitmap -n http cache - " + urlString);

        String path =null;
    	if(!isFullscreen)
    	{
    		path = cache.createFilePath(urlString);
    		if (cache.containsFileKey(urlString)) {
                
            	Log.v(TAG,"Http download file Cache hit!");
            //    return new File(path);
            }
    	}
    	else
    		path = DiskLruCache.getDiskCacheDir(context, "tmp").getAbsolutePath();
        
        
        mLinkedHashMapdownLoading.put(urlString,urlString);
        
        File cacheFile = new File(path);
        
        Utils.disableConnectionReuseIfNecessary();
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        
        try {
        	URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);
            in = new BufferedInputStream(urlConnection.getInputStream(), Utils.IO_BUFFER_SIZE);
            out = new BufferedOutputStream(new FileOutputStream(cacheFile),Utils.IO_BUFFER_SIZE);
            
            byte[] buffer = new byte[Utils.IO_BUFFER_SIZE];
            int size = 0;
            while ((size = in.read(buffer)) > 0) 
            {
            	if(bCancel == true&&isFullscreen==false)
            	{
            		cacheFile.delete();
            		break;
            	}
                out.write(buffer, 0, size);
            }
            //cache.putFileToCache(urlString, path);
            return cacheFile;

        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            if (out != null) {
                try {
                	out.flush();
                    out.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Error in downloadBitmap - " + e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Error in downloadBitmap - " + e);
                }
            } 
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
    			exif_http = new ExifInterface(path);
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            mLinkedHashMapdownLoading.remove(urlString);
        }

        return null;
    }
    public synchronized File downloadBitmapDTCPIPBuffer(String url,boolean isFullscreen,int []vAddrForDTCP)
    {
    	if(bCancel == true)
    	{
    		return null;
    	}
        String path =null;
    	if(!isFullscreen)
    	{
    		path = cache.createFilePath(url);
    		if (cache.containsFileKey(url)) 
    		{
    			return new File(path);
    		}
    	}
    	else
    		path = DiskLruCache.getDiskCacheDir(mContext, "tmp").getAbsolutePath();
         
         mLinkedHashMapdownLoading.put(url,url);
         File cacheFile = new File(path);
         
        int size = 64*1024;
    	byte[] buffer = new byte[size];
    	
    	int fd = 0;
		//fd = mTv.OpenImageFile(url);
    	if(fd<0)
    	{
    		mLinkedHashMapdownLoading.remove(url);
    		Log.e(TAG, "error,fd of OpenImageFile is "+fd);
    		return null;
    	}
    	int []AddrsAndFd = {vAddrForDTCP[0],vAddrForDTCP[1],fd};
        if(vAddrForDTCP[0] <= 0 || vAddrForDTCP[1] <= 0)
        {
        	mLinkedHashMapdownLoading.remove(url);
        	Log.e(TAG, "the address is error"+vAddrForDTCP[0]+"-"+vAddrForDTCP[1]);
        	return null;
        }
    	BufferedOutputStream out = null;
    	int totalsize =0;
    	try {

            out = new BufferedOutputStream(new FileOutputStream(cacheFile),size);
            int ret=-10;;
            while (true) 
            {
            	if(bCancel == true&&isFullscreen == false)
            	{
            		cacheFile.delete();
            		break;
            	}
            	//ret = mTv.ReadImageFile(AddrsAndFd,size,buffer);
            	Log.v(TAG,"ret of mTv.ReadImageFile is "+ret);
            	if(ret>=0)
            	{
            		totalsize = totalsize+ret;
            		out.write(buffer, 0, ret);
            	}
            	
        		if(ret<size)
        		{
        			break;
        		}
            }
            //cache.putFileToCache(url, path); 
            
            //if add http file cache.bugs may appear. 
            //photoes from different levels use the same cache path,so level A cache could be covered by level B
            //when this happens and the user enter level A, cache record exists but cache file can not found.
            
            if(ret<0)
            {
            	return null;
            }
            

            return cacheFile;

        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadDTCPBitmap - " + e);
        } finally {
            if (out != null) {
                try {
                	out.flush();
                    out.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Error in downloadDTCPBitmap - " + e);
                }
            }
          //  mTv.CloseImageFile(fd);
            mLinkedHashMapdownLoading.remove(url);
        }
    	return null;
    }
    
    public static boolean downloadURLtoStream(Context context, String urlString,OutputStream os) {
    
    	HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), Utils.IO_BUFFER_SIZE);
            out = new BufferedOutputStream(os, Utils.IO_BUFFER_SIZE);

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {}
        }
        return false;
    }
    
}
   