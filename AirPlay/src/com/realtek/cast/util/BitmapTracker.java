package com.realtek.cast.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.util.Log;

public class BitmapTracker {
	private static final String TAG = "BitmapTracker";
	private static final boolean VERBOSE = false;
	
	private class SizeLock {
		private long size = 0L;
		
		public synchronized void waitUnder(long size) {
			while(this.size >= size) {
				System.gc();
				try {
					wait();
				} catch (InterruptedException e) {
					// Ignore.
				}
			}
		}
		
		public synchronized long addAndGet(long delta) {
			long s = size += delta;
			notifyAll();
			return s;
		}
		
	}
	
	// Bitmap allocation trackers
	private final ReferenceQueue<Bitmap> mRefQueue = new ReferenceQueue<Bitmap>();
	private final Map<Reference<Bitmap>, Long> mRef2Size= new HashMap<Reference<Bitmap>, Long>();
	private final SizeLock mTotalBmSize = new SizeLock();
	
	public BitmapTracker(){
		Thread t = new Thread(mTracker);
		t.start();
	}
	
	private final Runnable mTracker = new Runnable() {
		
		@Override
		public void run() {
			while(true) {
				try {
					Reference<? extends Bitmap> ref = mRefQueue.remove();
					Long size = mRef2Size.remove(ref);
					long totalSize = mTotalBmSize.addAndGet(-size.longValue());
					if (VERBOSE) {
						if (size != null) {
							Log.i(TAG, String.format("Bitmap has been recycled. size = %d / %d",
									size.longValue(), totalSize));
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public void trackBitmap(Bitmap bm) {
		final long size = bm.getHeight() * bm.getRowBytes();
		mRef2Size.put(new PhantomReference<Bitmap>(bm, mRefQueue), size);
		long total = mTotalBmSize.addAndGet(size);
		if (VERBOSE) {
			Log.i(TAG, String.format("Bitmap tracked: size = %d / %d", size, total));
		}
	}
	
	public void waitForSizeUnder(long size) {
		mTotalBmSize.waitUnder(size);
	}
}
