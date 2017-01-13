/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.rtk.android.miracast;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;
/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class MiracastBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MiracastBroadcastReceiver";
    private WifiP2pManager manager;
    private Channel channel;
    private MiracastActivity activity;
    private String url = null;
    //public boolean flag = false;
 
    CountDownTimer timer2 = null;
    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public MiracastBroadcastReceiver(WifiP2pManager manager, Channel channel,
            MiracastActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
        //this.flag =false;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
	    if("android.intent.action.POWER_DOWN".equals(action))
		{
		  if((activity.timer_flag) != null) {
			Log.i(TAG, "KeyEvent.KEYCODE_ESCAPE: timer_flag.cancel() enter");
			(activity.timer_flag).cancel(); (activity.timer_flag) = null;
			Log.i(TAG, "KeyEvent.KEYCODE_ESCAPE: timer_flag.cancel() finish");
			}
		  if((activity.timerUI_flag) != null ){
			  (activity.timerUI_flag).cancel();
			  (activity.timerUI_flag) = null;
		  }
		 // (activity.mWifiManager).setWifiEnabled(true);
		  activity.finish(); 
		}else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        	Log.i(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
                Log.i(TAG, "p2p state connect");
            } else {
                activity.setIsWifiP2pEnabled(false);
                activity.resetData();
                Log.i(TAG, "p2p state disconnect, resetdata()");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
        	Log.i(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
            if (manager != null) {
                manager.requestPeers(channel, (PeerListListener)activity.mDeviceList);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        	Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            //WifiP2pInfo p2pInfo = (WifiP2pInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
			//Log.i(TAG, "EXTRA_WIFI_P2P_INFO :" + p2pInfo);
            if (networkInfo.isConnected()) {
				if(activity.curState != MiracastActivity.WFD_STATE_CONNECTED)
					manager.requestConnectionInfo(channel, activity.mDeviceDetail); 
				Log.i(TAG, "p2p connect");
            } else {
				if(activity.curState == MiracastActivity.WFD_STATE_CONNECTED)
				{
					activity.curState = MiracastActivity.WFD_STATE_DISCONNECTED;
					activity.mCurConnectedDevice = null;
					Intent kill = new Intent("MediaPlayerKill");
					activity.sendBroadcast(kill);
				}
                Log.e(TAG, "p2p disconnect");				
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            WifiP2pDevice thisDevice = (WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);            
            //if(flag==false){
            	//TextView myDevName=(TextView)activity.findViewById(R.id.myDevName);
            	//String strid = thisDevice.deviceName+"_"+thisDevice.deviceAddress.toUpperCase(Locale.getDefault());
                //myDevName.setText(strid);
               
                //flag =true;
            //}
            activity.mDeviceList.updateThisDevice(thisDevice);           
            activity.mDeviceDetail.thisDevice = thisDevice;
        }/*else if ("MediaPlayershutdown".equals(action)){
			activity.curState = MiracastActivity.WFD_STATE_DISCONNECTED;
			Log.i(TAG, "MediaPlayershutdown"); 
        	activity.disconnect();
        	//flag=false;
			//String strip=activity.getString(R.string.connect_try);
			//activity.retry(strip);
        	activity.resume();
        	activity.ready();
			//(activity.mWifiManager).setWifiEnabled(false);
			Log.e(TAG, "disconnect");
        }*/else if (WifiP2pManager.WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION.equals(action)) {
			Log.i(TAG, "WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION");
            if (manager!= null) {
                manager.requestPersistentGroupInfo(channel, activity);
            }
        }
    }  
}
