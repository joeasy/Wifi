package com.realtek.cast.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
	
	public interface Callback {
		public void setImageViewBitmap(ImageView imageView, Bitmap bitmap, boolean immediate);
	}
	
	private static final String TAG = "ImageLoader";
	private static final boolean VERBOSE = false;
	
	private static final int DEFAULT_MAX_THREAD_COUNT = 5;
	private static final int MAX_IMAGE_SIZE = 1024*1024;// pixel
	private static final int CACHE_SIZE;
	private static final long MAX_MEMORY_USAGE;
	
	private static ImageLoader sInstance;
	
	static {
		// Initialize the memory settings.
		final long maxHeap = Runtime.getRuntime().maxMemory();
		if (maxHeap >= 33554432) { // 32MB
			MAX_MEMORY_USAGE = 25165824; // 24MB
			CACHE_SIZE = 12582912; // 12MB
			
		} else if (maxHeap >= 25165824) { // 24MB
			MAX_MEMORY_USAGE = 16777216; // 16MB
			CACHE_SIZE = 8388608; // 8MB
			
		} else {
			MAX_MEMORY_USAGE = 8388608; // 8MB
			CACHE_SIZE = 4194304; // 4MB
			
		}
	}
	
	public static ImageLoader getInstance(){
		if(sInstance==null){
			sInstance = new ImageLoader(DEFAULT_MAX_THREAD_COUNT);
		}
		return sInstance;
	}
	
	public static final Callback DEFAULT_CALLBACK = new Callback() {
		
		@Override
		public void setImageViewBitmap(ImageView imageView, Bitmap bitmap, boolean immediate) {
			imageView.setImageBitmap(bitmap);
		}
	};
	
	public static final Callback CALLBACK_FADE_IN = new FadeCallback();
	
	public static class FadeCallback implements Callback {
		
		@Override
		public void setImageViewBitmap(ImageView imageView, Bitmap bitmap, boolean immediate) {
			imageView.setImageBitmap(bitmap);
			if (!immediate) {
				fadeIn(imageView);
			}
		}
		
		public void fadeIn(View view) {
			fade(view, View.VISIBLE, 0.0f, 1.0f);
		}
		
		public void fadeOut(View view) {
			fade(view, View.INVISIBLE, 1.0f, 0.0f);
		}
		
		public void fade(final View view, final int visibility, float startAlpha, float endAlpha) {
			AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
			anim.setDuration(600);
			view.startAnimation(anim);
			view.setVisibility(visibility);
		}
	};

	private class Argument implements Runnable {
		WeakReference<ImageView> refImageView;
		Uri uri;
		Bitmap bitmap;
		Callback callback = DEFAULT_CALLBACK;
		boolean isVideo = false;
		
		public Argument(ImageView imageView, Uri uri, Callback callback, boolean isVideo){
			refImageView = new WeakReference<ImageView>(imageView);
			this.uri = uri;
			this.callback = callback;
			this.isVideo = isVideo;
		}
		
		@Override
        public void run() {
			ImageView imageView = refImageView.get();
			if (imageView == null) {
				return;
			}
			
			File imgFile = new File(uri.getPath());
			
			//check the task is not canceled
			if (!uri.equals(imageView.getTag())) {
				return;
			}
			
			if (isVideo) {
				mBitmapTracker.waitForSizeUnder(MAX_MEMORY_USAGE);
				bitmap = ThumbnailUtils.createVideoThumbnail(uri.getPath(), Thumbnails.MICRO_KIND);
				if (bitmap != null) {
					mCache.put(uri, bitmap);
					mBitmapTracker.trackBitmap(bitmap);
				}
				
			} else {
				// Decode bounds
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(uri.getPath(), opt);
				
				// calculate sample factor
				int size = opt.outHeight * opt.outHeight;
				int factor = 1;
				while (size > MAX_IMAGE_SIZE){
					factor *= 2;
					size /= 4;
				}
				
				// Setup decode option
				opt.inJustDecodeBounds = false;
				opt.inSampleSize = factor;
				opt.inPreferredConfig = Bitmap.Config.RGB_565;
				
				// Decode
				mBitmapTracker.waitForSizeUnder(MAX_MEMORY_USAGE);
				bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), opt);
				if (bitmap != null) {
					mCache.put(uri, bitmap);
					mBitmapTracker.trackBitmap(bitmap);
				}
				
			}
			
			mHandler.sendMessage(mHandler.obtainMessage(0, this));
        }

		public ImageView getImageView() {
	        return refImageView.get();
        }
	}
	
	private static class CallbackHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Argument arg = (Argument) msg.obj;
			Bitmap bitmap = arg.bitmap;
			ImageView imageView = arg.getImageView();
			
			if (imageView != null && arg.uri.equals(imageView.getTag())) {
				if (bitmap != null) {
					arg.callback.setImageViewBitmap(imageView, bitmap, false);
				}
			}
			msg.obj = null;
		}
		
	}
	
	private static class BitmapCache extends LruCache<Uri, Bitmap> {

		public BitmapCache(int maxSize) {
			super(maxSize);
		}

		@Override
		protected void entryRemoved(boolean evicted, Uri key,
				Bitmap oldValue, Bitmap newValue) {
			super.entryRemoved(evicted, key, oldValue, newValue);
			if (VERBOSE) {
				Log.v(TAG, String.format("Bitmap removed: %s", key));
			}
		}

		@Override
		protected int sizeOf(Uri key, Bitmap value) {
			return value.getHeight() * value.getRowBytes();
		}
		
	}
	
	// Bitmap allocation tracker
	private final BitmapTracker mBitmapTracker;

	// Thread and Callback
	private final ExecutorService mExecutors;
	private final Handler mHandler;
	
	// Cache
	private final BitmapCache mCache;
	
	private ImageLoader(int maxThreadCount){
		mExecutors = Executors.newFixedThreadPool(5);
		mHandler = new CallbackHandler();
		
		mCache = new BitmapCache(CACHE_SIZE);
		mBitmapTracker = new BitmapTracker();
		
		if (VERBOSE) {
			Log.i(TAG, String.format(
					"Cache size = %d, Maximum memory usage = %d",
					CACHE_SIZE, MAX_MEMORY_USAGE));
		}
	}
	
	public void loadImage(ImageView imageView, Uri uri){
		loadImage(imageView, uri, DEFAULT_CALLBACK);
	}
	
	public void loadImage(ImageView imageView, Uri uri, Callback callback){
		internalLoadImage(imageView, uri, callback, false);
	}
	
	public void internalLoadImage(ImageView imageView, Uri uri, Callback callback, boolean isVideo){
		imageView.setTag(uri);
		if (uri == null || uri.equals(Uri.EMPTY)) {
			return;
		}
		
		if (VERBOSE) {
			Log.v(TAG, "Load image: " + uri.toString());
		}
		
		Bitmap bitmap = mCache.get(uri);
		if (bitmap != null) {
			callback.setImageViewBitmap(imageView, bitmap, true);
			if (VERBOSE) {
				Log.d(TAG, "Cache hit!!");
			}
			return;
		}
		callback.setImageViewBitmap(imageView, null, false);
		Argument arg = new Argument(imageView, uri, callback, isVideo);
		mExecutors.submit(arg);
	}
	
	public void loadVideoThumbnail(ImageView imageView, Uri uri, Callback callback) {
	    internalLoadImage(imageView, uri, callback, true);
    }

	public void loadVideoThumbnail(ImageView imageView, Uri uri) {
		internalLoadImage(imageView, uri, DEFAULT_CALLBACK, true);
    }
	
}
