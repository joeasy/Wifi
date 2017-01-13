package com.rtk.android.miracast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;

public class VideoPlayerBroadcastReceiver extends BroadcastReceiver{
	
    private static final String TAG = "VideoPlayerBroadcastReceiver";
	private videoPlayerActivity activity;
	
	public VideoPlayerBroadcastReceiver(videoPlayerActivity activity){
		super();
		this.activity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		/*
		if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

				NetworkInfo networkInfo = (NetworkInfo) intent
						.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

				if (!networkInfo.isConnected()) {
						Log.e(TAG,"videoPlayerActivityBroadcast receive the disconnect intent!");
						activity.shutDown();
				} 

		}
		*/
        if ("MediaPlayerKill".equals(action)){
			Log.i(TAG, "recive MediaPlayerKill");
			activity.shutDown();
		}
	}
	
}
