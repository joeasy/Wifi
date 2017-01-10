/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.widget;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Metadata;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import java.io.IOException;
import java.util.Map;

public class RtkAtvView extends SurfaceView {
	private String TAG = "RtkAtvView";
	private String STR_ATV_PATH = "Atv://";

	
	private Context mContext=super.getContext();
	// settable by the client
	private Uri mUri;
	private Map<String, String> mHeaders;

	// all possible internal states
	private static final int STATE_ERROR              = -1;
	private static final int STATE_IDLE               = 0;
	private static final int STATE_PREPARING          = 1;
	private static final int STATE_PREPARED           = 2;
	private static final int STATE_PLAYING            = 3;
	private static final int STATE_PAUSED             = 4;
	private static final int STATE_PLAYBACK_COMPLETED = 5;

	// mCurrentState is a RtkAtvView object's current state.
	// mTargetState is the state that a method caller intends to reach.
	// For instance, regardless the RtkVideoView object's current state,
	// calling pause() intends to bring the object to a target state
	// of STATE_PAUSED.
	private int mCurrentState = STATE_IDLE;
	private int mTargetState  = STATE_IDLE;
    	
	// All the stuff we need for showing atv
	private SurfaceHolder mSurfaceHolder = null;
	private MediaPlayer mMediaPlayer = null;
	private int mVideoWidth;
	private int mVideoHeight;
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private OnErrorListener mOnErrorListener;

	// constructor
	public RtkAtvView(Context context) {
		super(context);
		initRtkAtvView();
	}
	
	public RtkAtvView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		initRtkAtvView();
	}

	public RtkAtvView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initRtkAtvView();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.i("@@@@", "onMeasure");
		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
		if (mVideoWidth > 0 && mVideoHeight > 0) {
			if ( mVideoWidth * height  > width * mVideoHeight ) {
				//Log.i("@@@", "image too tall, correcting");
				height = width * mVideoHeight / mVideoWidth;
			} else if ( mVideoWidth * height  < width * mVideoHeight ) {
				//Log.i("@@@", "image too wide, correcting");
				width = height * mVideoWidth / mVideoHeight;
			} else {
				//Log.i("@@@", "aspect ratio is correct: " +
				//width+"/"+height+"="+
				//mVideoWidth+"/"+mVideoHeight);
			}
		}
		//Log.i("@@@@@@@@@@", "setting size: " + width + 'x' + height);
		setMeasuredDimension(width, height);
	}
	
	public int resolveAdjustedSize(int desiredSize, int measureSpec) {
		int result = desiredSize;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize =  MeasureSpec.getSize(measureSpec);

		switch (specMode) {
			case MeasureSpec.UNSPECIFIED:
				/* Parent says we can be as big as we want. Just don't be larger
				 * than max size imposed on ourselves.
				 */
				result = desiredSize;
				break;

			case MeasureSpec.AT_MOST:
				/* Parent says we can be as big as we want, up to specSize.
				 * Don't be larger than specSize, and don't be larger than
				 * the max size imposed on ourselves.
				 */
				result = Math.min(desiredSize, specSize);
				break;

			case MeasureSpec.EXACTLY:
				// No choice. Do what we are told.
				result = specSize;
				break;
		}
		return result;
	}

	private void initRtkAtvView() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		getHolder().addCallback(mSHCallback);
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
		mCurrentState = STATE_IDLE;
		mTargetState  = STATE_IDLE;
	}
    
	public void setAtvPath() {
		setAtvURI(Uri.parse(STR_ATV_PATH));
	}

	/**
	 * @hide
	 */
	private void setAtvURI(Uri uri) {
		setAtvURI(uri, null);
	}

	private void setAtvURI(Uri uri, Map<String, String> headers) {
		mUri = uri;
		mHeaders = headers;
		openAtv();
		requestLayout();
		invalidate();
	}

	private void openAtv() {
		
		if (mUri == null || mSurfaceHolder == null) {
			// not ready for starting atv just yet, will try again later
			return;
		}
		
		// Tell the music playback service to pause
		// TODO: these constants need to be published somewhere in the framework.
		Intent i = new Intent("com.android.music.musicservicecommand");
		i.putExtra("command", "pause");
		mContext.sendBroadcast(i);

		// we shouldn't clear the target state, because somebody might have
		// called start() previously
		release(false);
		try {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setOnPreparedListener(mPreparedListener);
			mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
			mMediaPlayer.setOnCompletionListener(mCompletionListener);
			mMediaPlayer.setOnErrorListener(mErrorListener);
			mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
			mMediaPlayer.setDataSource(mContext, mUri);
			mMediaPlayer.setDisplay(mSurfaceHolder);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setScreenOnWhilePlaying(true);
			mMediaPlayer.prepareAsync();
			// we don't set the target state here either, but preserve the
			// target state that was there before.
			mCurrentState = STATE_PREPARING;
		} catch (IOException ex) {
			Log.w(TAG, "Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		} catch (IllegalArgumentException ex) {
			Log.w(TAG, "Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		}
	}
	
	/*
	 *	add listener to mediaplayer
	 */
	MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
		new MediaPlayer.OnVideoSizeChangedListener() {
			public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
				mVideoWidth = mp.getVideoWidth();
				mVideoHeight = mp.getVideoHeight();
				if (mVideoWidth != 0 && mVideoHeight != 0) {
					getHolder().setFixedSize(mVideoWidth, mVideoHeight);
				}
			}
		};
	
	MediaPlayer.OnPreparedListener mPreparedListener = 
		new MediaPlayer.OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				mCurrentState = STATE_PREPARED;

				if (mOnPreparedListener != null) {
					mOnPreparedListener.onPrepared(mMediaPlayer);
				}
				
				mVideoWidth = mp.getVideoWidth();
				mVideoHeight = mp.getVideoHeight();

				if (mVideoWidth != 0 && mVideoHeight != 0) {
					//Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
					getHolder().setFixedSize(mVideoWidth, mVideoHeight);
					if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
						// We didn't actually change the size (it was already at the size
						// we need), so we won't get a "surface changed" callback, so
						// start the video here instead of in the callback.
						if (mTargetState == STATE_PLAYING) {
							start();
						}
					}
				} else {
					// We don't know the video size yet, but should start anyway.
					// The video size might be reported to us later.
					if (mTargetState == STATE_PLAYING) {
						start();
					}
				}
			}
		};

	private MediaPlayer.OnCompletionListener mCompletionListener =
		new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				mCurrentState = STATE_PLAYBACK_COMPLETED;
				mTargetState = STATE_PLAYBACK_COMPLETED;
				if (mOnCompletionListener != null) {
					mOnCompletionListener.onCompletion(mMediaPlayer);
				}
			}
		};
    
	private MediaPlayer.OnErrorListener mErrorListener =
		new MediaPlayer.OnErrorListener() {
			public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
				Log.d(TAG, "Error: " + framework_err + "," + impl_err);
				mCurrentState = STATE_ERROR;
				mTargetState = STATE_ERROR;

				/* If an error handler has been supplied, use it and finish. */
				if (mOnErrorListener != null) {
					if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
						return true;
					}
				}

				return  true;
			}
		};

	private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
		new MediaPlayer.OnBufferingUpdateListener() {
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				Log.d(TAG, "BufferingUpdateListener");
			}
		};
    
	SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
	{
		public void surfaceChanged(SurfaceHolder holder, int format,
				int w, int h)
		{
			mSurfaceWidth = w;
			mSurfaceHeight = h;
			boolean isValidState =  (mTargetState == STATE_PLAYING);
			boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
			if (mMediaPlayer != null && isValidState && hasValidSize) {
				start();
			}
		}

		public void surfaceCreated(SurfaceHolder holder)
		{
			mSurfaceHolder = holder;
			openAtv();
		}

		public void surfaceDestroyed(SurfaceHolder holder)
		{
			// after we return from this we can't use the surface any more
			mSurfaceHolder = null;
			release(true);
		}
	};

	/*
	 * release the media player in any state
	 */
	private void release(boolean cleartargetstate) {
		if (mMediaPlayer != null) {
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
			if (cleartargetstate) {
				mTargetState  = STATE_IDLE;
			}
		}
	}
    
	public void start() {
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mCurrentState = STATE_PLAYING;
		}
		mTargetState = STATE_PLAYING;
	}
    
	private boolean isInPlaybackState() {
		return (mMediaPlayer != null &&
				mCurrentState != STATE_ERROR &&
				mCurrentState != STATE_IDLE &&
				mCurrentState != STATE_PREPARING);
	}
}

