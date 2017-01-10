package com.realtek.cast.control;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.realtek.cast.airtunes.AirTunes;
import com.realtek.cast.airtunes.raop.RTSPResponder;
import com.realtek.cast.control.PlaybackControl.AudioInformation;
import com.realtek.cast.control.PlaybackControl.OnAudioChangedListener;
import com.realtek.cast.util.Daap;

class AudioContext {
	
	private final OnAudioChangedListener mListener;
	private final Handler mHandler;
	
	// Audio Session
	private RTSPResponder mRtspResponder;
	
	public long mAudioRtpTime;
	
	// progress (msec)
	public long mAudioStart;
	public long mAudioCurr;
	public long mAudioEnd;
	public long mAudioDiff;
	
	// track info
	public String mTrackName;
	public String mTrackAlbum;
	public String mTrackArtist;
	
	// cover art
	private Bitmap mCoverArt;
	private Bitmap mNotificationIcon;
	
	public AudioContext(OnAudioChangedListener listener) {
		mListener = listener;
		mHandler = new Handler(Looper.getMainLooper());
    }
	
	public synchronized Bitmap getCoverArt() {
		return mCoverArt;
	}
	
	public synchronized Bitmap getNotificationIcon(Context context) {
		if (mCoverArt == null) {
			return null;
		}
		if (mNotificationIcon == null) {
			int width = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
			int height = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
			mNotificationIcon = Bitmap.createScaledBitmap(mCoverArt, width, height, false);
		}
		return mNotificationIcon;
	}

	public synchronized void close() {
		mRtspResponder.close();
		mRtspResponder = null;
    }
	
	public synchronized boolean hasAudioSession() {
	    return mRtspResponder != null;
    }

	public synchronized boolean getAudioInformation(AudioInformation info) {
		info.current = mAudioCurr;
		info.end = mAudioEnd;
		info.start = mAudioStart;
		
		info.timeOffset = mAudioDiff;
		
		info.mTrackAlbum = mTrackAlbum;
		info.mTrackArtist = mTrackArtist;
		info.mTrackName = mTrackName;
		
		return mRtspResponder != null;
	}
	
	public synchronized void startAudioSession(RTSPResponder rtspResponder) {
		// stop previous
		if (mRtspResponder != null && mRtspResponder != rtspResponder) {
			mRtspResponder.close();
			mRtspResponder = null;
		}
		
		mRtspResponder = rtspResponder;
		mListener.onAudioStart();
	}
	
	public synchronized void stopAudioSession() {
		if (mRtspResponder != null) {
			mRtspResponder.close();
			mRtspResponder = null;
		}
		mListener.onAudioStop();
    }
	
	public synchronized void updateCoverArt(long rtpTime, byte[] jpeg, int offset, int length) {
		Bitmap bm = BitmapFactory.decodeByteArray(jpeg, offset, length);
		mCoverArt = bm;
		mNotificationIcon = null;
		// TODO
		mListener.onAudioInfoChanged();
    }

	public synchronized void updateTrackInfo(long rtpTime, byte[] data, int offset, int length) {
	    Daap daap = Daap.wrap(data, offset, length);
    	mTrackName= daap.getValue(Daap.CODE_MINM);//dmap.itemname
    	mTrackArtist = daap.getValue(Daap.CODE_ASAR);//"daap.songartist"
    	mTrackAlbum = daap.getValue(Daap.CODE_ASAL);//"daap.songalbum"
    	// TODO
		mListener.onAudioInfoChanged();	
    }

	public synchronized void updateProgress(long rtpTime, long start, long curr, long end) {
			mAudioStart = timestampToMsec(start);
			mAudioCurr = timestampToMsec(curr);
			mAudioEnd = timestampToMsec(end);
			mAudioDiff = mAudioCurr - SystemClock.uptimeMillis();
			
			mListener.onAudioProgressChanged();
			mHandler.postDelayed(mRunUpdateProgress, 1000);
    }
	
	private synchronized boolean increaseProgress() {
		if (mRtspResponder == null) {
			return false;
		}
		
		long current = mAudioDiff + SystemClock.uptimeMillis();
		if (current >= mAudioStart && current <= mAudioEnd) {
			mAudioCurr = current;
			mListener.onAudioProgressChanged();
		}
		return true;
	}
	
	private final Runnable mRunUpdateProgress = new Runnable() {
		
		@Override
		public void run() {
			if (increaseProgress()) {
				mHandler.postDelayed(this, 1000);
			}
		}
	};
	
	private static final long timestampToMsec(long rtptime) {
		return rtptime / AirTunes.SAMPLE_RATE * 1000;
	}
}
