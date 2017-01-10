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
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.realtek.DataProvider.FileFilterType;
import com.rtk.dmp.R;






//import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This class wraps up completing some arbitrary long running work when loading
 * a bitmap to an ImageView. It handles things like using a memory and disk
 * cache, running the work in a background thread and setting a placeholder
 * image.
 */
public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";
    private static final int FADE_IN_TIME = 200;
    
    private static final String HTTP_CACHE_DIR = "images";
    private ImageCache mImageCache=null;
    private Bitmap mLoadingBitmap;
    private boolean mFadeInBitmap = true;
    private boolean mExitTasksEarly = false;

    protected Context mContext;
    protected ImageWorkerAdapter mImageWorkerAdapter;
    protected LoadingControl loadingControl;
    
    protected static boolean bCancel=false;
    private Bitmap bmp= null;
    
    protected int mImageWidth,mImageHeight;

    
    abstract public void setImageSize(int width, int height);
    protected ImageWorker(Context context, LoadingControl loadingControl)
    {
        mContext = context;
        this.loadingControl = loadingControl;
    }

    /**
     * Load an image specified by the data parameter into an ImageView (override
     * {@link ImageWorker#processBitmap(Object)} to define the processing
     * logic). A memory and disk cache will be used if an {@link ImageCache} has
     * been set using {@link ImageWorker#setImageCache(ImageCache)}. If the
     * image is found in the memory cache, it is set immediately, otherwise an
     * {@link AsyncTask} will be created to asynchronously load the bitmap.
     * 
     * @param data The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void loadImage(Object data, ImageView imageView)
    {
        loadImage(data, imageView, loadingControl);
    }

    /**
     * Load an image specified by the data parameter into an ImageView (override
     * {@link ImageWorker#processBitmap(Object)} to define the processing
     * logic). A memory and disk cache will be used if an {@link ImageCache} has
     * been set using {@link ImageWorker#setImageCache(ImageCache)}. If the
     * image is found in the memory cache, it is set immediately, otherwise an
     * {@link AsyncTask} will be created to asynchronously load the bitmap.
     * 
     * @param data The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     * @param loadingControl the loading control.
     */
    public void loadImage(Object data, ImageView imageView, LoadingControl loadingControl)
    {
        Bitmap bitmap = null;
        
        //Log.d(TAG,"IW:loadImage: path:"+String.valueOf(data));
        if (mImageCache != null) {
            bitmap = mImageCache.getBitmapFromMemCache(String.valueOf(data));
        }

        if (bitmap != null) {
            // Bitmap found in memory cache
        	Log.v(TAG, "mem cache hit!"+(String)data);
            imageView.setImageBitmap(bitmap);
//            imageView.setBackgroundResource(R.drawable.dnla_thumnail_focus);
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, loadingControl);
            
            //Log.d("GridViewAdapter","IW:loadImage task  : "+ task.toString());
            
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(), mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            Object[]params={data,false};
            task.execute(params);
        }
    }
    
    //hartley add
    public void loadImage(Object data, ImageView imageView, LoadingControl loadingControl, int type)
    {
    	//if the type of item is device or folder, there is no need to decode picture 
    	if(type == FileFilterType.DEVICE_FILE_DIR)
    	{
    		return;
    	}
        Bitmap bitmap = null;
        
        //Log.d(TAG,"IW:loadImage: path:"+String.valueOf(data));
        if (mImageCache != null) {
            bitmap = mImageCache.getBitmapFromMemCache(String.valueOf(data));
        }

        if (bitmap != null) {
            // Bitmap found in memory cache
        	Log.v(TAG, "mem cache hit!"+(String)data);
            imageView.setImageBitmap(bitmap);
//            imageView.setBackgroundResource(R.drawable.dnla_thumnail_focus);
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, loadingControl);
            
            //Log.d("GridViewAdapter","IW:loadImage task  : "+ task.toString());
            
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(), mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            Object[]params={data,false};
            task.execute(params);
        }
    }
    
    public void loadImage(int position,int totalCnt,String path,ImageView imageView,LoadingControl loadingControl)
    {
    	
    }
    
   
    //hartley add
    public void loadImage(Object data, ImageView imageView, LoadingControl loadingControl,int pos,boolean isExif, int[] vAddrForDTCP)
    {
    	Bitmap bitmap = null;

        //Log.d(TAG,"IW:loadImage: exifpath:"+String.valueOf(data));
        if (mImageCache != null) {
            bitmap = mImageCache.getBitmapFromMemCache(String.valueOf(data));
        }
        if (bitmap != null) {
            // Bitmap found in memory cache
        	Log.v(TAG, "mem cache hit!!!"+ String.valueOf(data));
            imageView.setImageBitmap(bitmap);
            if(loadingControl !=null )
        	{
        		loadingControl.stopLoading(pos,false);
        	}
            //imageView.setBackgroundResource(0);
//            imageView.setBackgroundResource(R.drawable.dnla_thumnail_focus);
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, loadingControl, pos,isExif, vAddrForDTCP);
            
            Log.d("GridViewAdapter","IW:loadImage task  : "+ task.toString());
            
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(), mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
           // imageView.setBackgroundResource(R.drawable.photo_list_item_blank);
            Object[]params={data,isExif};
            task.execute(params);
        }
    }
  //hartley add
    public void loadImage(Object data, ImageView imageView, 
    		LoadingControl loadingControl,int pos,boolean isExif,int viewMode)
    {
    	Bitmap bitmap = null;
        
        //Log.d(TAG,"IW:loadImage: exifpath:"+String.valueOf(data));
        if (mImageCache != null) {
            bitmap = mImageCache.getBitmapFromMemCache(String.valueOf(data));
        }
        if (bitmap != null) {
            // Bitmap found in memory cache
//        	FilterImageView fiv = (FilterImageView)imageView;
//       	fiv.setBitmaps(bitmap);
//        	fiv.invalidate();
            
           // imageView.setBackgroundResource(R.drawable.dnla_thumnail_focus);
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, loadingControl, pos,isExif,viewMode);
            
            Log.d("GridViewAdapter","IW:loadImage task  : "+ task.toString());
            
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(), mLoadingBitmap, task);
           // imageView.setImageDrawable(asyncDrawable);
           // imageView.setBackgroundResource(R.drawable.photo_list_item_blank);
            Object[]params={data,isExif};
            task.execute(params);
        
        }
    }
    public Bitmap getImage(Object data,  
    		LoadingControl loadingControl,int pos,boolean isExif,int viewMode)
    {
    	loadImage(data, null, null,0,false,1);

		int timeout = 0;
	
		while(timeout<=50)
		{
			timeout ++;
			System.out.println("timeout:"+timeout);
			if(bmp != null)
			{
				System.out.println("break"+bmp.toString());
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

    	System.out.println("break"+bmp.toString());
    	final Bitmap tmpbmp = bmp;
    	bmp = null;
    	return tmpbmp;
    }

    public void recycleOldBitmap(Object data, ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable != null &&
                drawable instanceof BitmapDrawable
                && !(drawable instanceof AsyncDrawable))
        {
            if (mImageCache != null
                    && mImageCache.getBitmapFromMemCache(String.valueOf(data)) != null)
            {
                return;
            }
            Bitmap bitmapToRecycle = ((BitmapDrawable) drawable).getBitmap();
            imageView.setImageDrawable(null);
            bitmapToRecycle.recycle();
        }
    }

    /**
     * Load an image specified from a set adapter into an ImageView (override
     * {@link ImageWorker#processBitmap(Object)} to define the processing
     * logic). A memory and disk cache will be used if an {@link ImageCache} has
     * been set using {@link ImageWorker#setImageCache(ImageCache)}. If the
     * image is found in the memory cache, it is set immediately, otherwise an
     * {@link AsyncTask} will be created to asynchronously load the bitmap.
     * {@link ImageWorker#setAdapter(ImageWorkerAdapter)} must be called before
     * using this method.
     * 
     * @param data The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void loadImage(int num, ImageView imageView) {
        if (mImageWorkerAdapter != null) {
            loadImage(mImageWorkerAdapter.getItem(num), imageView);
        } else {
            throw new NullPointerException("Data not set, must call setAdapter() first.");
        }
    }
    
    
     /**
     * Set placeholder bitmap that shows when the the background thread is
     * running.
     * 
     * @param bitmap
     */
    public void setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is
     * running.
     * 
     * @param resId
     */
    public void setLoadingImage(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
    }

    /**
     * Set the {@link ImageCache} object to use with this ImageWorker.
     * 
     * @param cacheCallback
     */
    public void setImageCache(ImageCache cacheCallback) {
        mImageCache = cacheCallback;
    }

    public ImageCache getImageCache() {
        return mImageCache;
    }

    /**
     * If set to true, the image will fade-in once it has been loaded by the
     * background thread.
     * 
     * @param fadeIn
     */
    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
    }

    /**
     * Subclasses should override this to define any processing or work that
     * must happen to produce the final bitmap. This will be executed in a
     * background thread and be long running. For example, you could resize a
     * large bitmap here, or pull down an image from the network.
     * 
     * @param data The data to identify which image to process, as provided by
     *            {@link ImageWorker#loadImage(Object, ImageView)}
     * @return The processed bitmap
     */
    protected abstract Bitmap processBitmap(Object[] data);
    
    public static void cancelWork(ImageView imageView) {
        
        //Log.d(TAG,"IW:cancelWork imageview  :"+ imageView.toString());
        
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
         
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            bCancel = true;
        }
    }

    /**
     * Returns true if the current work has been canceled or if there was no
     * work in progress on this image view. Returns false if the work in
     * progress deals with the same data. The work is not stopped in that case.
     */
    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        //Log.d(TAG,"cancelPotentialWork: imageView: "+imageView.toString());
           
        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.data;
            if (bitmapData == null || !bitmapData.equals(data)) {
            	boolean b =  bitmapWorkerTask.cancel(true);
            	return true;
               //return bitmapWorkerTask.cancel(true);
                //Log.d(TAG, "cancelPotentialWork - cancelled work for " + data);
              
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
    }
    //add by hartley
    public static boolean cancelPotentialWork(Object data, SurfaceView surfaceView) {
    	return false;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active work task (if any) associated with
     *         this imageView. null if there is no such task.
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * The actual AsyncTask that will asynchronously process the image.
     */
    private class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {
    	private final static int VIEWMODE_NORMAL = 0; 
    	private final static int VIEWMODE_FULLSCREEN = 1; 
    	private final static int VIEWMODE_GALLERY = 2; 
        private Object data;
        private int position=0;
        private int totalCnt=0;
        private final WeakReference<ImageView> imageViewReference;
        private int pos = -1;
        private int isSurfaceView = 0;
        LoadingControl loadingControl;
        private int[] vAddrForDTCP;
        
        public BitmapWorkerTask(ImageView imageView, LoadingControl loadingControl)
        {

        	imageViewReference = new WeakReference<ImageView>(imageView);
            this.loadingControl = loadingControl;
        }
     
       
        
        public BitmapWorkerTask(ImageView imageView,LoadingControl loadingControl,int pos, boolean isExif,int[] vAddrForDTCP) {
			// TODO Auto-generated constructor stub

        	imageViewReference = new WeakReference<ImageView>(imageView);
            this.loadingControl = loadingControl; 	
            this.pos = pos;
            this.vAddrForDTCP = vAddrForDTCP;
		}
        public BitmapWorkerTask(ImageView imageView,LoadingControl loadingControl,
        		int pos, boolean isExif, int viewMode) {
			// TODO Auto-generated constructor stub

        	imageViewReference = new WeakReference<ImageView>(imageView);
            this.loadingControl = loadingControl; 	
            this.pos = pos;
            isSurfaceView = viewMode;
		}

		@Override
        protected void onPreExecute()
        {
            super.onPreExecute();
//            Log.v(TAG, "onPreExecute startloading");
            startLoading();
           
        }

        public void startLoading()
        {
            if (loadingControl != null)
            {
                loadingControl.startLoading(pos);
            }
        }

        public void stopLoading(boolean isFromOnCancel)
        {
            if (loadingControl != null)
            {
                loadingControl.stopLoading(pos,isFromOnCancel);
            }
        }

        @Override
        protected void onCancelled()
        {
            super.onCancelled();         
            boolean isFromOnCancel = true;
            stopLoading(isFromOnCancel);
        }
        
        @Override  
        protected void onProgressUpdate(Void... progresses) {  
        	stopLoading(false);
        }  

        /**
         * Background processing.
         */
        @Override
        protected Bitmap doInBackground(Object... params) {
          
        	data = params[0];
        	boolean isExif=(Boolean) params[1];
        	
        	
        	Bitmap bitmap = null;
        	
        	if(isExif !=true)
        	{
        		final String dataString = String.valueOf(data);
                

                // If the image cache is available and this task has not been
                // cancelled by another
                // thread and the ImageView that was originally bound to this task
                // is still bound back
                // to this task and our "exit early" flag is not set then try and
                // fetch the bitmap from
                // the cache
                 
                if (mImageCache != null && !isCancelled() //&& getAttachedImageView() != null
                        && !mExitTasksEarly) {
                	
                    bitmap = mImageCache.getBitmapFromDiskCache(dataString);
                }

                
                // If the bitmap was not found in the cache and this task has not
                // been cancelled by
                // another thread and the ImageView that was originally bound to
                // this task is still
                // bound back to this task and our "exit early" flag is not set,
                // then call the main
                // process method (as implemented by a subclass)
                if (bitmap == null && !isCancelled() //&& getAttachedImageView() != null
                        && !mExitTasksEarly)
                {
                    //Log.d("GridViewAdapter","IM:doInBackground: do ProcessBitmap:"+String.valueOf(params[0]));
                    Object[]params_arg={data,isExif,vAddrForDTCP};
                    bitmap = processBitmap(params_arg);
                }

                // If the bitmap was processed and the image cache is available,
                // then add the processed
                // bitmap to the cache for future use. Note we don't check if the
                // task was cancelled
                // here, if it was, and the thread is still running, we may as well
                // add the processed
                // bitmap to our cache as it might be used again in the future
                if (bitmap != null && mImageCache != null) {
                    mImageCache.addBitmapToCache(dataString, bitmap);
                }
        		
        	}
        	else if(isExif==true)
        	{      
        		//Log.d(TAG, "doingbackgroune()");
        		final String dataString = String.valueOf(data);
        		
        		ExifInterface exif = null;
				try {
					exif = new ExifInterface(dataString);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(exif == null)
					return null;
        		
        		
 	        	if (mImageCache != null && !isCancelled() && getAttachedImageView() != null
                        && !mExitTasksEarly) {
                    bitmap = mImageCache.getBitmapFromDiskCache(dataString);
                }

                // If the bitmap was not found in the cache and this task has not
                // been cancelled by
                // another thread and the ImageView that was originally bound to
                // this task is still
                // bound back to this task and our "exit early" flag is not set,
                // then call the main
                // process method (as implemented by a subclass)
                if (bitmap == null && !isCancelled() && getAttachedImageView() != null
                        && !mExitTasksEarly)
                {
                    //Log.d("GridViewAdapter","IM:doInBackground: do ProcessBitmap:"+String.valueOf(params[0]));
                    bitmap = processBitmap(params);
                }

                // If the bitmap was processed and the image cache is available,
                // then add the processed
                // bitmap to the cache for future use. Note we don't check if the
                // task was cancelled
                // here, if it was, and the thread is still running, we may as well
                // add the processed
                // bitmap to our cache as it might be used again in the future
                if (bitmap != null && mImageCache != null) {
                    mImageCache.addBitmapToCache(dataString,bitmap);
                }

        	}
           
        	publishProgress(); 
        	try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            		
        	

            return bitmap;
        }
        
  //      @Override  
  //      protected void onProgressUpdate() {

 //       }  

        /**
         * Once the image is processed, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            
        	
        	
            // if cancel was called on this task or the "exit early" flag is set
            // then we're done
            if (isCancelled() || mExitTasksEarly) {
                
                bitmap = null;
            }

            if(isSurfaceView == VIEWMODE_NORMAL)
            {
	            final ImageView imageView = getAttachedImageView();
	            if (bitmap != null && imageView != null) {
	                  setImageBitmap(imageView, bitmap);
//	                  imageView.setBackgroundResource(R.drawable.dnla_thumnail_focus);
	            }
	            else if(bitmap == null && imageView != null)
	            {
	            	Log.e(TAG, "R.drawable.others_icon_photo_bad,bitmap == null && imageView != null");
	            	//bitmap is null,decode failed.
//	            	imageView.setImageResource(R.drawable.dlna_photo_icon_missing);
	            }
            }
            else if(isSurfaceView == VIEWMODE_FULLSCREEN)
            {
            	bmp = bitmap;
            }
            else if(isSurfaceView == VIEWMODE_GALLERY)
            {
            	final ImageView imageView = (ImageView)getAttachedImageView();
            	if (bitmap != null && imageView != null) {
//	                  imageView.setBitmaps(bitmap);
//	                  imageView.invalidate();
//	                  imageView.setBackgroundResource(R.drawable.dnla_thumnail_focus);
	            }
            	else if(bitmap == null && imageView != null)
	            {
//	            	imageView.setBackgroundResource(R.drawable.dlna_photo_icon_missing);
//	            	imageView.setImageResource(R.drawable.dlna_photo_icon_missing);
	            }

            }
        }

        /**
         * Returns the ImageView associated with this task as long as the
         * ImageView's task still points to this task as well. Returns null
         * otherwise.
         */
        private ImageView getAttachedImageView() {
            
              final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                 return imageView;
            }
            return null;
        }
        
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work
     * is in progress. Contains a reference to the actual worker task, so that
     * it can be stopped if a new binding is required, and makes sure that only
     * the last started worker process can bind its result, independently of the
     * finish order.
     */
    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);

            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     * Called when the processing is complete and the final bitmap should be set
     * on the ImageView.
     * 
     * @param imageView
     * @param bitmap
     */
    private void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        if (mFadeInBitmap) {
            // Transition drawable with a transparent drwabale and the final
            // bitmap
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[] {
                            new ColorDrawable(android.R.color.transparent),
                            new BitmapDrawable(mContext.getResources(), bitmap)
                    });
            // Set background to loading bitmap
			/*
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN)
            {
                setBackgroundDrawable(imageView);
            } else
            {
                setBackground(imageView);
            }
			*/
			setBackgroundDrawable(imageView);
            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }
    public abstract File getFileFromcache(String url,boolean isFullscreen,int[] vAddrForDTCP);

	
    //@TargetApi(16)
	/*
    public void setBackground(ImageView imageView)
    {
        imageView
                .setBackground(
                new BitmapDrawable(mContext.getResources(), mLoadingBitmap));
    }
	*/

    //@SuppressWarnings("deprecation")
    public void setBackgroundDrawable(ImageView imageView)
    {
        imageView
                .setBackgroundDrawable(
                new BitmapDrawable(mContext.getResources(), mLoadingBitmap));
    }

    /**
     * Set the simple adapter which holds the backing data.
     * 
     * @param adapter
     */
    public void setAdapter(ImageWorkerAdapter adapter) {
        mImageWorkerAdapter = adapter;
    }

    /**
     * Get the current adapter.
     * 
     * @return
     */
    public ImageWorkerAdapter getAdapter() {
        return mImageWorkerAdapter;
    }

    /**
     * A very simple adapter for use with ImageWorker class and subclasses.
     */
    public static abstract class ImageWorkerAdapter {
        public abstract Object getItem(int num);

        public abstract int getSize();
    }
}
