package com.realtek.cast.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.OnVideoChangedListener;

import com.realtek.cast.R;

public class VideoFragment extends Fragment implements Callback, OnVideoChangedListener {
	
//	private TextView mTextTrack;
//	private TextView mTextArtist;
//	private TextView mTextAlbum;

	private SurfaceView mSurfaceView;
//	private ProgressBar mProgress;
	private View mProgressLoad;
	private SurfaceHolder mHolder;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_video, container, false);
		mSurfaceView = (SurfaceView) v.findViewById(R.id.surface);
		mProgressLoad = v.findViewById(R.id.progress_loading);
//		mProgress = (ProgressBar) v.findViewById(R.id.progress);
//		mTextTrack = (TextView) v.findViewById(R.id.text_track);
//		mTextArtist = (TextView) v.findViewById(R.id.text_artist);
//		mTextAlbum = (TextView) v.findViewById(R.id.text_album);
		
		mSurfaceView.getHolder().addCallback(this);
		return v;
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		PlaybackControl ctrl = PlaybackControl.getInstance();
		ctrl.registerVideoDisplay(mHolder);
	}

	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	    
    }

	@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
		PlaybackControl ctrl = PlaybackControl.getInstance();
		ctrl.unregisterVideoDisplay(holder);
    }
	
	@Override
    public void onStop() {
	    super.onStop();
	    PlaybackControl.getInstance().stopVideo();
    }
	
	@Override
    public void onResume() {
	    super.onResume();
	    PlaybackControl.getInstance().registerVideoCallback(this, true);
    }

	@Override
    public void onPause() {
	    super.onPause();
	    PlaybackControl.getInstance().unregisterVideoDisplay(mHolder);
    }

	public void play() {
		if (mHolder != null) {
			PlaybackControl ctrl = PlaybackControl.getInstance();
			ctrl.registerVideoDisplay(mHolder);
		}
	}

	@Override
    public void onVideoStateChanged(int state) {
		switch (state) {
			case PlaybackControl.VIDEO_LOADING:
				mProgressLoad.setVisibility(View.VISIBLE);
				break;
			default:
				mProgressLoad.setVisibility(View.GONE);
				break;
		}
    }
}
