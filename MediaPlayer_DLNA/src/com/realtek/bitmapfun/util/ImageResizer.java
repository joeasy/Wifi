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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

/**
 * A simple subclass of {@link ImageWorker} that resizes images from resources
 * given a target width and height. Useful for when the input images might be
 * too large to simply load directly into memory.
 */
public class ImageResizer extends ImageWorker {
    private static final String TAG = "ImageWorker";

    /**
     * Initialize providing a single target image size (used for both width and
     * height);
     * 
     * @param context
     * @param loadingControl
     * @param imageWidth
     * @param imageHeight
     */
    public ImageResizer(Context context, LoadingControl loadingControl,
            int imageWidth, int imageHeight)
    {
        super(context, loadingControl);
        setImageSize(imageWidth, imageHeight);
    }

    /**
     * Initialize providing a single target image size (used for both width and
     * height);
     * 
     * @param context
     * @param loadingControl
     * @param imageSize
     */
    public ImageResizer(Context context, LoadingControl loadingControl,
            int imageSize)
    {
        super(context, loadingControl);
        setImageSize(imageSize);
    }

    /**
     * Set the target image width and height.
     * 
     * @param width
     * @param height
     */
    @Override
    public void setImageSize(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    /**
     * Set the target image size (width and height will be the same).
     * 
     * @param size
     */
    public void setImageSize(int size) {
        setImageSize(size, size);
    }

    /**
     * The main processing method. This happens in a background task. In this
     * case we are just sampling down the bitmap and returning it from a
     * resource.
     * 
     * @param resId
     * @return
     */
    private Bitmap processBitmap(int resId) {
        return processBitmap(resId, mImageWidth, mImageHeight);
    }

    /**
     * The main processing method. This happens in a background task. In this
     * case we are just sampling down the bitmap and returning it from a
     * resource.
     * 
     * @param resId
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    protected Bitmap processBitmap(int resId, int imageWidth, int imageHeight) {
        /*if (BuildConfig.DEBUG) {
            CommonUtils.debug(TAG, "processBitmap - " + resId);
        }*/
    	Log.d(TAG, "processBitmap - " + resId);
        return decodeSampledBitmapFromResource(
                mContext.getResources(), resId, imageWidth, imageHeight);
    }

    @Override
    protected Bitmap processBitmap(Object[] data) {
    	
    	boolean isExif=(Boolean) data[1];
    	if(isExif!=true)
    		return processBitmap(Integer.parseInt(String.valueOf(data)));
    	
    	else if(isExif==true)
    	{
    		String exifpath=(String)data[0];
    		return processBitmap(exifpath,isExif);
    	}
    	else 
    	{
    		Log.d(TAG, "processBitmap get arg failed");
    		return null;
    	}
        
    }
    
    protected Bitmap processBitmap(String exifpath,boolean isExif)
    {
    	ExifInterface exif=null;
		try {
			exif = new ExifInterface(exifpath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	byte[] bThumbnail=exif.getThumbnail();
    	Bitmap bmp = BitmapFactory.decodeByteArray(bThumbnail,0, bThumbnail.length);
	   	Log.d(TAG,"thumbnail: "+bmp.getHeight()+ "bitmap"+bmp.getWidth());  
	   	
	   	int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED); 
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
			bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), 
					bmp.getHeight(), m, true); 
		} 
	   	if(bmp == null)
	   	{     	
	   		Log.d(TAG,"BitmapFactory.decodeByteArray() failed!!");
	   	}
		return bmp;//BitmapFactory.decodeByteArray(bytes,0, bytes.length);;
    	
    }
    
    @Override
    public File getFileFromcache(String url,boolean isfullscreen,int[] vAddrForDTCP)
    {
    	Log.v("ImageResizer", "ImageResizer getFileFromcache");
    	return null;
    }

    /**
     * Decode and sample down a bitmap from resources to the requested width and
     * height.
     * 
     * @param res The resources object containing the image data
     * @param resId The resource id of the image data
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect
     *         ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Decode and sample down a bitmap from a file to the requested width and
     * height.
     * 
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect
     *         ratio and dimensions that are equal to or greater than the
     *         requested width and height
     */
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = calculateImageSize(filename);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        //return BitmapFactory.decodeFile(filename, options);
        return decode(filename,options);
    }
    
    public static synchronized Bitmap decodeHttpImage(String url)
   	{
    	Bitmap bmp = null;
    	try
    	{   
    		URL ulrn = new URL(url);
    	    HttpURLConnection con = (HttpURLConnection)ulrn.openConnection();
    	    InputStream is = con.getInputStream();
    	    bmp = BitmapFactory.decodeStream(is);
    	   

    	}
    	catch(Exception e) 
        {
        	e.printStackTrace(); 
        } 
    	
    	return bmp;
    	
   	}

    
    
    
    public static synchronized Bitmap decode(String path,BitmapFactory.Options options)
	{
    	FileDescriptor fd;
    	Bitmap bm = null;
			
    	try
    	{
    		FileInputStream fis = new FileInputStream(path); 
			fd = fis.getFD();
			options.inPurgeable = true; 
			options.inInputShareable = true; 
			options.inJustDecodeBounds = true; 
			BitmapFactory.decodeFileDescriptor(fd, null, options); 
			options.inJustDecodeBounds = false; 
			bm = BitmapFactory.decodeFileDescriptor(fd, null, options); 
			fis.close();
    	} 
    	catch(Exception e) 
    	{
    		e.printStackTrace(); 
    	} 
    	return bm;
	}

    /**
     * Calculate the image size for the given file name.
     * 
     * @param filename
     * @return
     */
    public static BitmapFactory.Options calculateImageSize(String filename) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        
        Log.d(TAG,"calculateImageSize :"+ filename);
        return options;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options}
     * object when decoding bitmaps using the decode* methods from
     * {@link BitmapFactory}. This implementation calculates the closest
     * inSampleSize that will result in the final decoded bitmap having a width
     * and height equal to or larger than the requested width and height. This
     * implementation does not ensure a power of 2 is returned for inSampleSize
     * which can be faster when decoding but results in a larger bitmap which
     * isn't as useful for caching purposes.
     * 
     * @param options An options object with out* params already populated (run
     *            through a decode* method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.srcHeight;
        final int width = options.srcWidth;
        System.out.println("calculateInSampleSize"+height+" "+width);
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }


}
