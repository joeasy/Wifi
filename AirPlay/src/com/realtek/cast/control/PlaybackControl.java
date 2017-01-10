package com.realtek.cast.control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import com.realtek.cast.airtunes.raop.RTSPResponder;
import com.realtek.cast.app.PlaybackActivity;
import com.realtek.cast.util.ImageLocalStore;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaybackControl {

	private static final String TAG = "PlaybackControl";
	private static final boolean VERBOSE = true;
	
	private static PlaybackControl sInstance;
	
	public static final int VIDEO_LOADING = 0;
	public static final int VIDEO_PLAYING = 1;
	public static final int VIDEO_PAUSED = 2;
	public static final int VIDEO_STOPPED = 3;
	
	public static final int SLIDESHOW_LOADING = 0;
	public static final int SLIDESHOW_PLAYING = 1;
	public static final int SLIDESHOW_STOPPED = 2;
	
	public static final int PHOTO_PLAYING = 0;
	public static final int PHOTO_STOPPED = 1;
	
	public static final int MIRROR_STOPPED = 0;
	public static final int MIRROR_INIT = 1;
	public static final int MIRROR_PLAYING = 2;
	
	private static final int MSG_PHOTO = 0x00;
	private static final int MSG_SLIDE = 0x01;
	private static final int MSG_VIDEO = 0x02;
	
	private static final int MSG_AUDIO_START = 0x10;
	private static final int MSG_AUDIO_STOP = 0x11;
	private static final int MSG_AUDIO_PROGRESS = 0x12;
	private static final int MSG_AUDIO_INFO = 0x13;

	public static void initialize(Context context) {
		if (sInstance == null) {
			sInstance = new PlaybackControl(context.getApplicationContext());
		}
	}

	public static PlaybackControl getInstance() {
		if (sInstance == null) {
			throw new IllegalStateException("The class hasn't been initialized");
		}
		return sInstance;
	}
	
	public interface OnAudioChangedListener {
		public void onAudioStart();
		public void onAudioStop();
		public void onAudioProgressChanged();
		public void onAudioInfoChanged();
	}
	
	public interface OnVideoChangedListener {
		public void onVideoStateChanged(int state);
	}
	
	public interface OnMirrorChangedListener {
		public void onMirrorStateChanged(int state);
	}
	
	public interface OnPhotoChangedListener {
		public void onPhotoStateChange(int stat);
		public void onSlideShowStateChange(int stat, int assetId, int lastAssetId);
	}
	
	public interface PhotoRequester {
		public boolean requestPhoto(int assetId);

		public void close();
	}
	
	public static class AudioInformation {
		// progress (msec)
		public long start;
		public long current;
		public long end;
		public long timeOffset;
		
		// track info
		public String mTrackName;
		public String mTrackAlbum;
		public String mTrackArtist;

		private final RTSPResponder mRtspResponder;
		
		public AudioInformation(RTSPResponder rtspResponder) {
			mRtspResponder = rtspResponder;
        }
		
		public AudioInformation() {
			mRtspResponder = null;
        }

		public void close() {
			mRtspResponder.close();
        }
	}
	
	public static class VideoInformation {
		public Uri uri;
		
		public int state;
		
		/**
		 * the duration in milliseconds, if no duration is available (for example, if streaming live content), -1 is returned.
		 */
		public int duration;
		
		/**
		 * the current position in milliseconds
		 */
		public int position;
	}
	
	public static class PhotoInformation {
		
		public int assetId;
		public int lastAssetId;
		public String assetKey;
		
		public int photoStat;
		public int slideStat;
		public int duration;
		
	}
	
	private final Context mContext;
	private final Handler mHandler;
	
	// Callback
	private final List<OnAudioChangedListener> mAudioCallback = new ArrayList<OnAudioChangedListener>();
	private final List<OnVideoChangedListener> mVideoCallback = new ArrayList<OnVideoChangedListener>();
	private final List<OnPhotoChangedListener> mPhotoCallback = new ArrayList<OnPhotoChangedListener>();
	
	// Media Context
	private final AudioContext mAudioContext = new AudioContext(new InternalAudioListener());
	private final VideoContext mVideoContext = new VideoContext(new InternalVideoListener());
	private final PhotoContext mPhotoContext = new PhotoContext(new InternalPhotoChangedListener());
	private final MirrorContext mMirrorContext = new MirrorContext(new InternalMirrorListener());
	
	public PlaybackControl(Context context) {
		mContext = context;
		mHandler = new CallbackHandler();
	}
	
	public void adjustVolume(double vol, double min, double max, double mute) {
		AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		if (vol <= mute) {
			am.setStreamMute(AudioManager.STREAM_MUSIC, true);
		} else {
			am.setStreamMute(AudioManager.STREAM_MUSIC, false);
			int m= am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int idx = (int) (m * (vol - min) / (max - min));
			int i = (int) idx;
			if (i >= 0 && i <= m) {
				am.setStreamVolume(AudioManager.STREAM_MUSIC, i, AudioManager.FLAG_SHOW_UI);
			} else {
				Log.w(TAG, String.format("invalid volume: %f", vol));
			}
		}
	    
    }
	
	/* * * * * * * * Photo Control * * * * * * * * *
	 * Slide show request connections:
	 * 1. Reverse connection (Callback):
	 *    C -> S: /reverse
	 *    S -> C: /event
	 * 
	 * 2. Reverse connection (Request image):
	 *    C -> S: /reverse
	 *    S -> C: Get images
	 * 
	 * 3. AirPlay Request
	 *    C -> S: /slideshow 
	 */
	
	public boolean hasPhotoSession() {
	    return mPhotoContext.hasSession();
    }
	
	public void getPhotoInformation(PhotoInformation info) {
	    mPhotoContext.getInformation(info);
    }
	
	public void showPhoto(String assetKey, boolean show, ByteBuffer data) {
		if (VERBOSE) {
			Log.v(TAG, String.format("showPhoto: key=%s, show=%b, data=%s", assetKey, show, data));
		}
		
		// Stop video first
		mVideoContext.stopVideo();
		
		// Always save it
		if (data != null && data.remaining() > 0) {
			ImageLocalStore.saveImage(mContext, assetKey, data);
		}
		
		//
		if (show) {
			mPhotoContext.showPhoto(assetKey);
			PlaybackActivity.start(mContext);
		}
	}
	
	public void stopPhoto() {
		if (VERBOSE) {
			Log.v(TAG, "stopPhoto");
		}
		mPhotoContext.stopPhoto();
    }
	
	public void stopSlideShow() {
		mPhotoContext.stopSlideShow();
	}

	public void startSlideShow(String theme, int duration) {
		if (VERBOSE) {
			Log.v(TAG, String.format("startSlideShow: theme=%s, duration=%d", theme, duration));
		}
		
		// Stop video first
		mVideoContext.stopVideo();
		
		// Start slide show. The state are set to loading.
	    mPhotoContext.startSlideShow(theme, duration);
	    
	    // Start activity.
	    PlaybackActivity.start(mContext);
    }
	
	public void notifySlideShowImage(int id, String key, byte[] data) {
		if (VERBOSE) {
			Log.v(TAG, String.format("notifySlideShowImage: id=%d, key=%s", id, key));
		}
		
		String assetKey = String.format(Locale.US, "slideshow_%d_%s", id, key);
		
		// Save it
		ImageLocalStore.saveImage(mContext, assetKey, ByteBuffer.wrap(data));
		
		mPhotoContext.notifySlideShowImage(id, assetKey);
    }
	
	public void registerPhotoRequester(PhotoRequester requester) {
		mPhotoContext.registerPhotoRequester(requester);
	}
	
	public void unregisterPhotoRequester(PhotoRequester requester) {
		mPhotoContext.unregisterPhotoRequester(requester);
	}
	
	private class InternalPhotoChangedListener implements OnPhotoChangedListener {
		
		@Override
		public void onSlideShowStateChange(int stat, int assetId, int lastAssetId) {
			mHandler.obtainMessage(MSG_SLIDE, stat, assetId, Integer.valueOf(lastAssetId)).sendToTarget();
		}
		
		@Override
		public void onPhotoStateChange(int stat) {
			mHandler.obtainMessage(MSG_PHOTO, stat, 0).sendToTarget();
		}
	};
	
	// =================================================================
	
	/**
	 * Stop video/photo/slideshow playbacks
	 */
	public void stopPlayback() {
		mVideoContext.stopVideo();
		mPhotoContext.stopSession();
    }
	
	/*
	 * * * * * * * * * * Video Control * * * * * * * * * 
	 * 
	 * 
	 */
	private class InternalVideoListener implements OnVideoChangedListener {
		
		@Override
		public void onVideoStateChanged(int stat) {
			mHandler.obtainMessage(MSG_VIDEO, stat, 0).sendToTarget();
		}
	}

	public boolean hasVideoSession() {
	    return mVideoContext.getState() != VIDEO_STOPPED;
    }
	
	public void getVideoInformation(VideoInformation info) {
		mVideoContext.getVideoInformation(info);
    }
	
	public int playVideo(String strUri, float position) {
		if (VERBOSE) {
			Log.v(TAG, String.format("playVideo: uri=%s, position=%f", strUri, position));
		}
		
		// TODO: Stop audio playback
		stopAudioSession();
		
		// Stop photo session
		mPhotoContext.stopSession();
		
		// Start video
		int stat = mVideoContext.playVideo(mContext, strUri, position);
		
		// Invoke UI
		PlaybackActivity.start(mContext);
		return stat;
    }
	
	public int seekVideoTo(float second) {
		return mVideoContext.seekVideoTo(second);
    }
	
	public int resumeVideo() {
		return mVideoContext.resumeVideo();
	}
	
	public int pauseVideo() {
		return mVideoContext.pauseVideo();
    }
	
	public void stopVideo() {
		mVideoContext.stopVideo();
    }
	
	public boolean registerVideoDisplay(SurfaceHolder holder) {
		return mVideoContext.registerVideoDisplay(holder);
    }
	
	public void unregisterVideoDisplay(SurfaceHolder holder) {
		mVideoContext.unregisterVideoDisplay(holder);
    }
	
	public int getVideoState() {
		return mVideoContext.getState();
	}
	
	/*
	 * * * * * * * * * Audio Sessions * * * * * * * * * *
	 */
	private class InternalAudioListener implements OnAudioChangedListener {

		@Override
        public void onAudioStart() {
	        mHandler.obtainMessage(MSG_AUDIO_START).sendToTarget();
        }

		@Override
        public void onAudioStop() {
	        mHandler.obtainMessage(MSG_AUDIO_STOP).sendToTarget();
        }

		@Override
        public void onAudioProgressChanged() {
	        mHandler.obtainMessage(MSG_AUDIO_INFO).sendToTarget();
        }

		@Override
        public void onAudioInfoChanged() {
	        mHandler.obtainMessage(MSG_AUDIO_PROGRESS).sendToTarget();
        }
		
	}

	public boolean hasAudioSession() {
	    return mAudioContext.hasAudioSession();
    }

	public boolean getAudioInformation(AudioInformation info) {
		return mAudioContext.getAudioInformation(info);
	}
	
	public Bitmap getAudioCoverArt() {
		return mAudioContext.getCoverArt();
	}
	
	public Bitmap getAudioCoverArtForNotification() {
		return mAudioContext.getNotificationIcon(mContext);
	}
	
	public void startAudioSession(RTSPResponder rtspResponder) {
		mAudioContext.startAudioSession(rtspResponder);
	}
	
	public void stopAudioSession() {
		mAudioContext.stopAudioSession();
    }
	
	public void updateCoverArt(long rtpTime, byte[] jpeg, int offset, int length) {
		mAudioContext.updateCoverArt(rtpTime, jpeg, offset, length);
    }

	public void updateTrackInfo(long rtpTime, byte[] data, int offset, int length) {
		mAudioContext.updateTrackInfo(rtpTime, data, offset, length);
    }

	public void updateProgress(long rtpTime, long start, long curr, long end) {
		mAudioContext.updateProgress(rtpTime, start, curr, end);
	}
	
	/*
	 * Mirroring Control
	 */
	
	public boolean hasMirrorSession() {
	    return mMirrorContext.hasSession();
    }
	
	public void startMirroring() {
		mMirrorContext.initialize();
		
		PlaybackActivity.start(mContext);
	}
	
	public void registerMirrorDisplay(SurfaceHolder holder) {
		mMirrorContext.registerDisplay(holder);
    }
	

	public void setupMirror(byte[] sps, byte[] pps) {
	    mMirrorContext.setup(1920, 1080, sps, pps);
    }
	
	public void writeMirrorStream(ByteBuffer buffer) {
		mMirrorContext.writeData(buffer);
	}

	
	public void stopMirror() {
	    
    }
	
	private class InternalMirrorListener implements OnMirrorChangedListener {

		@Override
        public void onMirrorStateChanged(int state) {
	        
        }
		
	}
	//
	
	@SuppressLint("HandlerLeak")
	private class CallbackHandler extends Handler {
		
		final List<OnVideoChangedListener> videos = new ArrayList<PlaybackControl.OnVideoChangedListener>();
		
        public CallbackHandler() {
			super(Looper.getMainLooper());
		}

		@Override
        public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			// TODO: Before invoking callback,
			// we should copy the listeners into another list to prevent CurrentModificationExceptions.
			int stat, id, id2;
	        switch(msg.what) {
	        	// Photo
	        	case MSG_PHOTO:
	        		stat = msg.arg1;
	        		for (OnPhotoChangedListener l : mPhotoCallback) {
	        			l.onPhotoStateChange(stat);
	        		}
	        		break;
	        	case MSG_SLIDE:
	        		stat = msg.arg1;
	        		id = msg.arg2;
	        		id2 = (Integer) msg.obj;
	        		for (OnPhotoChangedListener l : mPhotoCallback) {
	        			l.onSlideShowStateChange(stat, id, id2);
	        		}
	        		break;
	        	// Video
	        	case MSG_VIDEO:
	        		videos.clear();
	        		synchronized(PlaybackControl.this) {
	        			videos.addAll(mVideoCallback);
	    			}
	        		stat = msg.arg1;
	        		for (OnVideoChangedListener l : videos) {
	    				l.onVideoStateChanged(stat);
	    			}
	        		videos.clear();
	        		break;
	        	// Audio
	        	case MSG_AUDIO_START:
	        		for (OnAudioChangedListener l : mAudioCallback) {
	        			l.onAudioStart();
	        		}
	        	case MSG_AUDIO_STOP:
	        		for (OnAudioChangedListener l : mAudioCallback) {
	        			l.onAudioStart();
	        		}
	        	case MSG_AUDIO_PROGRESS:
	        		for (OnAudioChangedListener l : mAudioCallback) {
	        			l.onAudioProgressChanged();
	        		}
	        	case MSG_AUDIO_INFO:
	        		for (OnAudioChangedListener l : mAudioCallback) {
	        			l.onAudioInfoChanged();
	        		}
	        		break;
	        	default:
	        		break;
	        }
        }
		
	}
	
	// ====================== Callback register functions =========================================
	public synchronized void registerAudioCallback(OnAudioChangedListener listener, boolean notify) {
		mAudioCallback.add(listener);
		if (notify) {
			listener.onAudioInfoChanged();
		}
	}

	public synchronized void unregisterAudioCallback(OnAudioChangedListener listener) {
		mAudioCallback.remove(listener);
    }
	
	public synchronized void registerVideoCallback(OnVideoChangedListener listener, boolean notify) {
		mVideoCallback.add(listener);
		if (notify) {
			int s = mVideoContext.getState();
			listener.onVideoStateChanged(s);
		}
	}

	public synchronized void unregisterVideoCallback(OnVideoChangedListener listener) {
		mVideoCallback.remove(listener);
    }
	
	public synchronized void registerPhotoCallback(OnPhotoChangedListener listener, boolean notify) {
		mPhotoCallback.add(listener);
		PhotoInformation info = new PhotoInformation();
		mPhotoContext.getInformation(info);
		if (notify) {
			listener.onPhotoStateChange(info.photoStat);
			listener.onSlideShowStateChange(info.slideStat, info.assetId, info.lastAssetId);
		}
	}

	public synchronized void unregisterPhotoCallback(OnPhotoChangedListener listener) {
		mPhotoCallback.remove(listener);
    }

}
