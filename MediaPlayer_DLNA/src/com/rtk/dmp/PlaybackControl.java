package com.rtk.dmp;

import com.rtk.dmp.BookMark;
import com.rtk.dmp.HandlerControlerVariable;

import android.media.MediaPlayer;

import android.os.Handler;

import android.util.Log;
import android.view.SurfaceHolder;

public abstract class PlaybackControl {
	String tag = "PlaybackControl";
	
	MediaPlayer mPlayer = null;
	
	public int state = 0;
	public static final int S_IDLE = 1;
	public static final int S_INITIALIZED = 2;
	public static final int PREPARED = 3;
	public static final int STARTED = 4;
	public static final int PAUSED = 5;
	public static final int STOPPED = 6;
	public static final int PLAYBACKCOMPLETED = 7;
	public static final int END = 8;
	public static final int ERROR = 9;
	
	public static final int	NAVPROP_INPUT_GET_PLAYBACK_STATUS = 10;
	public static final int NAVPROP_INPUT_GET_NAV_STATE = 11;
	public static final int NAVPROP_INPUT_SET_NAV_STATE = 12;
	
	
	public PlaybackControl(MediaPlayer mPlayer) {
		this.mPlayer = mPlayer;
	}
	
	public void mp_reset() {
		if(mPlayer != null) {
			Log.e(tag, "mp_reset flag 1");
			setListenersNull();
			mPlayer.reset();
			state = S_IDLE;
		} else {
			Log.e(tag, "mp_reset error!");
			return ;
		}
	}
	
	public void mp_setDataSource() {
		if(state != S_IDLE) {
			Log.e(tag, "mp_setDataSource error!");
			return ;
		}
		mPlayer.setPlayerType(6);
		if(setDataSource() == false) {
			return ;
		}
		state = S_INITIALIZED;
	}
	
	public void mp_prepare() {
		if(state != S_INITIALIZED) {
			Log.e(tag, "mp_prepare error!");
			return ;
		}
		try {
			setListeners();
			mPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean mp_start() {
		if(state == S_IDLE || state == S_INITIALIZED || state == STOPPED || state == ERROR || state == END) {
			Log.e(tag, "mp_start error!");
			return false;
		}
		mPlayer.start();
		state = STARTED;
		return true;
	}
	
	public boolean mp_pause() {
		if(state == S_IDLE || state == S_INITIALIZED || state == PREPARED || state == STOPPED || state == ERROR || state == END) {
			Log.e(tag, "mp_pause error!");
			return false;
		}
		mPlayer.pause();
		state = PAUSED;
		return true;
	}
	
	public boolean mp_stop() {
		if(state == S_IDLE || state == S_INITIALIZED || state == ERROR || state == END) {
			Log.e(tag, "mp_stop error!");
			return false;
		}
		mPlayer.stop();
		state = STOPPED;
		return true;
	}
	
	public void mp_fastForward(int dyn) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED) {
			mPlayer.fastforward(dyn);
		} else {
			Log.e(tag, "mp_forward error!");
			return ;
		}
	}
	
	public void mp_fastBackward(int dyn) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED) {
			mPlayer.fastrewind(dyn);
		} else {
			Log.e(tag, "mp_forward error!");
			return ;
		}
	}
	public void mp_slowForward(int dyn) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED) {
			//no api
			mPlayer.slowforward(dyn);
		} else {
			Log.e(tag, "mp_forward error!");
			return ;
		}
	}
	public void mp_slowBackward(int dyn) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED) {
			//no api
			mPlayer.slowrewind(dyn);
		} else {
			Log.e(tag, "mp_forward error!");
			return ;
		}
	}
	
	public boolean mp_normalPlay() {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED) {
			mPlayer.fastforward(0);
			return true;
		} else {
			Log.e(tag, "normalPlay error!");
			return false;
		}
	}
	
	public int mp_getCurrentPosition() {
		if(state == ERROR || state == END || state == S_IDLE) {
			return -1;
		} else {
			return mPlayer.getCurrentPosition();
		}
	}
	
	public int mp_getDuration() {
		if(state == S_IDLE || state == S_INITIALIZED || state == ERROR || state == END) {
			return -1;
		} else {
			return mPlayer.getDuration();
		}
		
	}
	
	public boolean mp_seekTo(int msec) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED) {
			mPlayer.seekTo(msec);
			return true;
		}
		return false;
	}
	
	public int[] mp_getSubtitleInfo() {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED || state == STOPPED) {
			return mPlayer.getSubtitleInfo();
		}
		return null;
	}
	
	public void mp_setSubtitleInfo(int streamNum,int enable,int textEncoding,int textColor,int fontSize) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED || state == STOPPED) {
			mPlayer.setSubtitleInfo(streamNum, enable, textEncoding, textColor, fontSize);
		}
	}
	
	public int[] mp_getAudioTrackInfo(int streamNum) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED || state == STOPPED) {
			return mPlayer.getAudioTrackInfo(streamNum);
		}
		return null;
	}
	
	public void mp_setAudioTrackInfo(int streamNum) {
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED || state == STOPPED) {
			mPlayer.setAudioTrackInfo(streamNum);
		}
	}
	
	public boolean isPlaying() {
		if(mPlayer.isPlaying()) {
			return true;
		} else {
			return false;
		}
	}
	
	
	public boolean safeCheck() {
		if(state == S_IDLE || state == ERROR || state == END || state == S_INITIALIZED)
			return false;
		return true;
	}
	
	public void mp_release() {
		if(mPlayer != null) {
			Log.v(tag, "release happen");
			mPlayer.release();
			mPlayer = null;
		}
		state = END;
	}
	
	public void setMediaPlayerNull() {
		mPlayer = null;
		state = END;
	}
	
	public void setListenersNull() {
		mPlayer.setOnBufferingUpdateListener(null);
		mPlayer.setOnPreparedListener(null);
		mPlayer.setOnCompletionListener(null);
		mPlayer.setOnErrorListener(null);
		mPlayer.setOnInfoListener(null);
		mPlayer.setOnSeekCompleteListener(null);
		mPlayer.setOnTimedTextListener(null);
		mPlayer.setOnVideoSizeChangedListener(null);
	}
	
	public abstract void setListeners();	
	public abstract boolean setDataSource();
	
	public void mp_set_nav_state(int propertyID, byte[] inputArray)
	{
		if(inputArray != null) {
			if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED || state == STOPPED) {
				Log.v(tag, "NAVPROP_INPUT_SET_NAV_STATE");
				mPlayer.execSetGetNavProperty(NAVPROP_INPUT_SET_NAV_STATE, inputArray);
			}
		}
	}
	
	public byte[] mp_get_nav_state(int propertyID, byte[] inputArray)
	{
		byte[] outputArray = null;
		if(state == PREPARED || state == STARTED || state == PAUSED || state == PLAYBACKCOMPLETED || state == STOPPED) {
			outputArray = mPlayer.execSetGetNavProperty(NAVPROP_INPUT_GET_NAV_STATE, inputArray);
			if(outputArray == null)
			{
				Log.e(tag, "execSetGetNavProperty(NAVPROP_INPUT_GET_NAV_STATE) return null!");
			}
		}
		return outputArray;
	}
	
	public void mp_setDisplay(SurfaceHolder sh) {
		if(mPlayer == null) {
			Log.e(tag, "error while try to set Display");
			return ;
		}
		mPlayer.setDisplay(sh);
	}
}
