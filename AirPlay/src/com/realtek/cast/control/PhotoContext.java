package com.realtek.cast.control;

import android.util.Log;
import android.util.SparseArray;

import com.realtek.cast.control.PlaybackControl.OnPhotoChangedListener;
import com.realtek.cast.control.PlaybackControl.PhotoInformation;
import com.realtek.cast.control.PlaybackControl.PhotoRequester;

import java.util.Timer;
import java.util.TimerTask;

class PhotoContext {
	
	private static final String TAG = "PhotoContext";
	private static final boolean VERBOSE = true;

	// Callback
	private final OnPhotoChangedListener mListener;

	// Current status
	private int photoStat = PlaybackControl.PHOTO_STOPPED;
	private int slideStat = PlaybackControl.SLIDESHOW_STOPPED;
	
	// Photo
	private String mAssetKey;
	
	// SlideShow
	private int mAssetId;
	private int mLastAssetId;
	private int mDuration;
	private SparseArray<String> mAssetId2Key = new SparseArray<String>();
	
	// 
	private PhotoRequester mPhotoRequester;
	
	private Timer mTimer;
	
	PhotoContext(OnPhotoChangedListener listener) {
		mListener = listener;
    }
	
	public synchronized void showPhoto(String assetKey) {
		// Stop slide show
		stopSlideShow_l();
		
		// Initialize variables.
		mAssetKey = assetKey;
		
		// Start photo
		photoStat = PlaybackControl.PHOTO_PLAYING;
		mListener.onPhotoStateChange(photoStat);
	}
	
	public synchronized void startSlideShow(String theme, int durationSec) {
		// Initialize variables.
		mAssetId = 1;
		mLastAssetId = 1;
		mAssetKey = null;
		mAssetId2Key.clear();
		mDuration = durationSec * 1000;
		
		// Start loading
		slideStat = PlaybackControl.SLIDESHOW_LOADING;
		mListener.onSlideShowStateChange(slideStat, mAssetId, mLastAssetId);
		requestSlideShowImage_l(mAssetId);
		
		// Start timer
		if (mTimer != null) {
			mTimer.cancel();
		}
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerSlideShow(), mDuration, mDuration);
    }
	
	private class TimerSlideShow extends TimerTask {
		
		@Override
		public void run() {
			if (!nextImage() && mTimer != null) {
				mTimer.cancel();
			}
		}
	};

	public synchronized void stopSession() {
		stopSlideShow_l();
		stopPhoto_l();
    }
	
	public synchronized void stopPhoto() {
	    stopPhoto_l();
    }
	
	public synchronized void stopSlideShow() {
	    stopSlideShow_l();
    }
	
	private void stopPhoto_l() {
		if (photoStat == PlaybackControl.PHOTO_STOPPED) {
			return;
		}
		
		photoStat = PlaybackControl.PHOTO_STOPPED;
		mAssetKey = null;
		mListener.onPhotoStateChange(photoStat);
	}
	
	private void stopSlideShow_l() {
		if (slideStat == PlaybackControl.SLIDESHOW_STOPPED) {
			return;
		}
		
		slideStat = PlaybackControl.SLIDESHOW_STOPPED;
		mAssetId = -1;
		mLastAssetId = -1;
		mDuration = 0;
		mAssetId2Key.clear();
		if (mPhotoRequester != null) {
			mPhotoRequester.close();
		}
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		mListener.onSlideShowStateChange(slideStat, mAssetId, mLastAssetId);
	}
	
	public synchronized boolean hasSession() {
		return photoStat == PlaybackControl.PHOTO_PLAYING || slideStat != PlaybackControl.SLIDESHOW_STOPPED;
	}

	public synchronized void getInformation(PhotoInformation info) {
		info.assetId = mAssetId;
	    info.assetKey = mAssetKey;
	    info.duration = mDuration;
	    info.photoStat = photoStat;
	    info.slideStat = slideStat;
	    info.lastAssetId = mLastAssetId;
    }
	
	private boolean requestSlideShowImage_l(int assetId) {
		boolean requested = false;
		if (mPhotoRequester != null) {
			requested = mPhotoRequester.requestPhoto(assetId);
		}
		return requested;
	}

	public synchronized void registerPhotoRequester(PhotoRequester requester) {
		mPhotoRequester = requester;
	}
	
	public synchronized void unregisterPhotoRequester(PhotoRequester requester) {
		if (requester == mPhotoRequester) {
			mPhotoRequester = null;
		}
	}

	public synchronized void notifySlideShowImage(int id, String key) {
		if (slideStat != PlaybackControl.SLIDESHOW_LOADING) {
			Log.w(TAG, "Got slideshow image in a illeagal state");
		}
		
		// Put image
		mAssetId2Key.put(id, key);
		
		// Update state
		if (slideStat == PlaybackControl.SLIDESHOW_LOADING) {
			slideStat = PlaybackControl.SLIDESHOW_PLAYING;
			mAssetId = id;
			mLastAssetId = id;
			mAssetKey = key;
			mListener.onSlideShowStateChange(slideStat, mAssetId, mLastAssetId);
		}
    }
	
	private synchronized boolean nextImage() {
		if (VERBOSE) {
			Log.v(TAG, String.format("nextImage: curretn asset ID = %d", mAssetId));
		}
		
		// Check slide show status
		if (slideStat == PlaybackControl.SLIDESHOW_STOPPED) {
			Log.w(TAG, "nextImage: session has been closed");
			return false;
		}
		
		if (slideStat == PlaybackControl.SLIDESHOW_LOADING) {
			Log.w(TAG, "nextImage: already loading");
		}
		
		mAssetKey = mAssetId2Key.get(++mAssetId);
		if (mAssetKey == null) {
			slideStat = requestSlideShowImage_l(mAssetId) ?
					PlaybackControl.SLIDESHOW_LOADING : PlaybackControl.SLIDESHOW_STOPPED;
		} else {
			slideStat = PlaybackControl.SLIDESHOW_PLAYING;
			mLastAssetId = mAssetId;
		}
		mListener.onSlideShowStateChange(slideStat, mAssetId, mLastAssetId);
		return true;
	}

}
