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
import com.realtek.cast.control.PlaybackControl.OnMirrorChangedListener;

public class MirrorFragment extends Fragment implements Callback, OnMirrorChangedListener {
	

	private SurfaceView mSurfaceView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mSurfaceView = new SurfaceView(inflater.getContext());
		mSurfaceView.getHolder().addCallback(this);
		return mSurfaceView;
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		PlaybackControl ctrl = PlaybackControl.getInstance();
		ctrl.registerMirrorDisplay(holder);
	}

	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	    
    }

	@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//		PlaybackControl ctrl = PlaybackControl.getInstance();
//		ctrl.unregisterVideoDisplay(holder);
    }
	
	@Override
    public void onStop() {
	    super.onStop();
	    PlaybackControl.getInstance().stopMirror();
    }
	
	@Override
    public void onResume() {
	    super.onResume();
//	    PlaybackControl.getInstance().registerVideoCallback(this, true);
    }

	@Override
    public void onPause() {
	    super.onPause();
//	    PlaybackControl.getInstance().unregisterVideoDisplay(mHolder);
    }

	@Override
    public void onMirrorStateChanged(int state) {
	    
    }

}
