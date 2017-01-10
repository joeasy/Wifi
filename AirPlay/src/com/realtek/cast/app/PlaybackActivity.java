
package com.realtek.cast.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.realtek.cast.R;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.OnAudioChangedListener;
import com.realtek.cast.control.PlaybackControl.OnPhotoChangedListener;
import com.realtek.cast.control.PlaybackControl.OnVideoChangedListener;

public class PlaybackActivity extends Activity implements OnAudioChangedListener, OnVideoChangedListener, OnPhotoChangedListener {
	
	public static boolean isActive() {
		return sIsResumed;
	}
	
	private static boolean sIsResumed = false;
	
	public static Intent createIntent(Context context) {
		Intent intent = new Intent(context, PlaybackActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}
	
	public static void start(Context context) {
		context.startActivity(createIntent(context));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playback);

		refresh();
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
	    super.onNewIntent(intent);
	    refresh();
    }
	
	private void refresh() {
		PlaybackControl ctrl = PlaybackControl.getInstance();
		
		if (ctrl.hasMirrorSession()) {
			showFragment(MirrorFragment.class);
			
		} else if (ctrl.hasVideoSession()) {
			VideoFragment vf = (VideoFragment) showFragment(VideoFragment.class);
			vf.play();
			
		} else if (ctrl.hasPhotoSession()) {
			showFragment(PhotoFragment.class);
			
		} else if (ctrl.hasAudioSession()) {
			showFragment(AudioFragment.class);
			
		} else {
			FragmentManager fm = getFragmentManager();
			Fragment f = fm.findFragmentById(android.R.id.content);
			if (f != null) {
				fm.beginTransaction().remove(f).commit();
			}
			finish();
		}
		
	}
	
	private Fragment showFragment(Class<? extends Fragment> clazz) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();
		Fragment f = fm.findFragmentById(android.R.id.content);
		if (f != null && f.getClass() != clazz) {
			transaction.remove(f);
			f = null;
		}
		
		if (f == null) {
			f = Fragment.instantiate(this, clazz.getName());
			transaction.add(android.R.id.content, f);
		}
		
		if (!transaction.isEmpty()) {
			transaction.commit();
		}
		
		return f;
	}

	@Override
    protected void onResume() {
	    super.onResume();
	    PlaybackControl.getInstance().registerAudioCallback(this, true);
	    PlaybackControl.getInstance().registerVideoCallback(this, true);
	    PlaybackControl.getInstance().registerPhotoCallback(this, true);
	    sIsResumed = true;
    }

	@Override
    protected void onPause() {
	    super.onPause();
	    PlaybackControl.getInstance().unregisterAudioCallback(this);
	    PlaybackControl.getInstance().unregisterVideoCallback(this);
	    PlaybackControl.getInstance().unregisterPhotoCallback(this);
	    sIsResumed = false;
    }

	@Override
	public void onAudioStart() {
		showFragment(AudioFragment.class);
	}

	@Override
	public void onAudioStop() {
		refresh();
	}

	@Override
	public void onAudioProgressChanged() {

	}

	@Override
	public void onAudioInfoChanged() {

	}

	@Override
    public void onVideoStateChanged(int state) {
	    refresh();
    }

	@Override
    public void onPhotoStateChange(int stat) {
		refresh();
    }

	@Override
    public void onSlideShowStateChange(int stat, int assetId, int lastAssetId) {
	    refresh();
    }

}
