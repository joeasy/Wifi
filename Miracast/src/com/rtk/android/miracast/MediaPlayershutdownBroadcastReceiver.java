package com.rtk.android.miracast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MediaPlayershutdownBroadcastReceiver extends BroadcastReceiver{
	
	private static final String TAG = "MediaPlayershutdownBroadcastReceiver";
	private MiracastActivity activity;
	
	public MediaPlayershutdownBroadcastReceiver(MiracastActivity activity){
		super();
		this.activity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if ("MediaPlayershutdown".equals(action)){
			activity.curState = MiracastActivity.WFD_STATE_DISCONNECTED;
			Log.i(TAG, "MediaPlayershutdown"); 
        	activity.disconnect();
        	//flag=false;
			//String strip=activity.getString(R.string.connect_try);
			//activity.retry(strip);
        	activity.resume();
        	activity.ready();
			activity.WFDStopDiscover();
			//(activity.mWifiManager).setWifiEnabled(false);
			Log.e(TAG, "disconnect");
        }
	}

	
}
