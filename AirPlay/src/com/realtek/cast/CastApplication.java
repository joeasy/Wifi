package com.realtek.cast;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import com.realtek.cast.app.AudioNotification;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.OnAudioChangedListener;

public class CastApplication extends Application {
	
	private static final String TAG = "Cast";
	
	public static final int NOTIFICATION_ID = 1;

	@Override
    public void onCreate() {
	    super.onCreate();
	    PlaybackControl.initialize(this);
	    
	    PlaybackControl pb = PlaybackControl.getInstance();
	    pb.registerAudioCallback(new OnAudioChangedListener() {
			
			@Override
			public void onAudioStop() {}
			@Override
			public void onAudioProgressChanged() {}
			@Override
			public void onAudioInfoChanged() {}
			
			@Override
			public void onAudioStart() {
//				PlaybackActivity.start(CastApplication.this);
			}
			
		}, false);
	    pb.registerAudioCallback(new AudioNotification(this), false);
	    
	    // Ensure the service is started
	    AppPreference pref = AppPreference.getInstance(this);
	    if(pref.isAirPlayEnabled()) {
	    	AirService.startService(this);
	    }
	    
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	    BroadcastReceiver br = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(TAG, "Receive action: " + intent.getAction());
				AirService.refreshService(CastApplication.this);
			}
		};
		registerReceiver(br, filter);
    }

}
