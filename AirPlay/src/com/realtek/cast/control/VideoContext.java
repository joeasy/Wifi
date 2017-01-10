package com.realtek.cast.control;

import static com.realtek.cast.control.PlaybackControl.VIDEO_LOADING;
import static com.realtek.cast.control.PlaybackControl.VIDEO_PAUSED;
import static com.realtek.cast.control.PlaybackControl.VIDEO_PLAYING;
import static com.realtek.cast.control.PlaybackControl.VIDEO_STOPPED;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.view.SurfaceHolder;

import com.realtek.cast.control.PlaybackControl.OnVideoChangedListener;
import com.realtek.cast.control.PlaybackControl.VideoInformation;


class VideoContext implements OnCompletionListener, OnPreparedListener, OnErrorListener {
	
	private final OnVideoChangedListener listener;
	private MediaPlayer player;
	private boolean prepared = false;
	
	private Uri uri;
	
	private int state = VIDEO_STOPPED;
	
	/**
	 * 0: start
	 * 1: end
	 */
	private float targetPosition = 0F;
	
	private SurfaceHolder display;
	
	VideoContext(OnVideoChangedListener listener) {
		this.listener = listener;
	}

	@Override
    public synchronized void onCompletion(MediaPlayer mp) {
        state = VIDEO_STOPPED;
		listener.onVideoStateChanged(state);
    }

	@Override
    public synchronized void onPrepared(MediaPlayer mp) {
		prepared = true;
		// Start
		if (state == VIDEO_LOADING) {
			// Seek to taraget position
			int sec = (int) (player.getDuration() * targetPosition);
			player.seekTo(sec);
			
			// Start
			state =  VIDEO_PLAYING;
			player.start();
			
			listener.onVideoStateChanged(state);
		}
    }

	@Override
    public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        player.release();
		player = null;
		state = VIDEO_STOPPED;
		listener.onVideoStateChanged(state);
        return false;
    }
	
	public synchronized boolean isVideoSession() {
	    return state != VIDEO_STOPPED;
    }
	
	public synchronized void getVideoInformation(VideoInformation info) {
		info.state = state;
		if (player != null) {
			info.uri = uri;
			if (prepared) {
				info.duration = player.getDuration();
				info.position = player.getCurrentPosition();
			} else {
				info.duration = 0;
				info.position = 0;
			}
		} else {
			info.state = VIDEO_STOPPED;
			info.uri = null;
			info.duration = 0;
			info.position = 0;
		}
    }
	
	public synchronized int playVideo(Context context, String strUri, float position) {
		int stat = -1;
		
		Uri uri = Uri.parse(strUri);
		try {
			// Ensure player
			if (player == null) {
				this.uri = uri;
				player = new MediaPlayer();
				player.setOnCompletionListener(this);
				player.setOnPreparedListener(this);
				player.setOnErrorListener(this);
//				try {
//					player.setPlayerType(6/* REALTEK_PLAYER */);
//				} catch (Exception e) {
//				}
				player.setDataSource(context, uri);
				player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				player.prepareAsync();
				prepared = false;
				state = VIDEO_LOADING;
			} else {
				if (!this.uri.equals(uri)) {
					this.uri = uri;
					player.reset();
					player.setDataSource(context, uri);
					player.setAudioStreamType(AudioManager.STREAM_MUSIC);
					player.setDisplay(display);
					player.prepareAsync();
					prepared = false;
					state = VIDEO_LOADING;
				}
			}
			
			targetPosition = position;


			// Start playback if it's already prepared
			if (prepared) {
				// Seek to taraget position
				int sec = (int) (player.getDuration() * position);
				player.seekTo(sec);
				
				// Start
				state =  VIDEO_PLAYING;
				player.start();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			if (player != null) {
				player.release();
			}
			player = null;
			state = VIDEO_STOPPED;
		}
		
		// Notify callback
		listener.onVideoStateChanged(state);
		return stat;
    }
	
	public synchronized int seekVideoTo(float second) {
		if (player != null && prepared) {
			int sec = (int) (second * 1000F);
			player.seekTo(sec);
		}
		return state;
    }
	
	public synchronized int resumeVideo() {
		if (player != null && state == VIDEO_PAUSED) {
			player.start();
			state = VIDEO_PLAYING;
			listener.onVideoStateChanged(state);
		}
		return state;
	}
	
	public synchronized int pauseVideo() {
		if (player != null && state == VIDEO_PLAYING) {
			player.pause();
			state = VIDEO_PAUSED;
			listener.onVideoStateChanged(state);
		}
		return state;
    }
	
	public synchronized int stopVideo() {
		if (state != VIDEO_STOPPED) {
			player.release();
			player = null;
			display = null;
			state = VIDEO_STOPPED;
			listener.onVideoStateChanged(state);
		}
		return state;
    }
	
	public synchronized boolean registerVideoDisplay(SurfaceHolder holder) {
		if (player == null) {
			return false;
		}
		
		if (display == holder) {
			return true;
		}
		
		display = holder;
		player.setDisplay(holder);
		return true;
    }
	
	public synchronized void unregisterVideoDisplay(SurfaceHolder holder) {
		display = null;
		if (player != null) {
			player.setDisplay(null);
		}
    }
	
	public synchronized int getState() {
		return state;
	}
	
	@Override
    public boolean equals(Object o) {
		if (o instanceof VideoContext) {
			return ((VideoContext) o).uri.equals(uri);
		}
        return super.equals(o);
    }
}
