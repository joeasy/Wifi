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

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.os.CountDownTimer;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class WFDDeviceList implements PeerListListener {

    private static final String TAG = "WFDDeviceList";
	private MiracastActivity activity;

	public WFDDeviceList(MiracastActivity activity){
			this.activity = activity;
	}
    
    public List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private WifiP2pDevice device;


    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }


    /**
     * Update UI for this device.
     * 
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice dev) {
    	Log.i(TAG, "updateThisDevice :" + dev);
        this.device = dev;
		/*
		if(activity.curState == MiracastActivity.WFD_STATE_READY && dev.status == WifiP2pDevice.CONNECTED)
		{
    		Log.i(TAG, "WFD_STATE_CONNECTING");
			activity.curState = MiracastActivity.WFD_STATE_CONNECTING;
			String str = activity.getString(R.string.wfd_connecting);
			activity.updateString(str);
		}
		else if(activity.curState == MiracastActivity.WFD_STATE_CONNECTING || activity.curState == MiracastActivity.WFD_STATE_CONNECTED) {
			if(dev.status != WifiP2pDevice.CONNECTED) {
    			Log.i(TAG, "WFD_STATE_CONNECTING failed.");
			}
		}
		*/
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        Log.i(TAG, "onPeersAvailable");
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if (peers.size() == 0) {
            Log.i(TAG, "No devices found");
            return;
        } else{
        	Iterator<WifiP2pDevice> it =peers.iterator();
        	while (it.hasNext()){
        		WifiP2pDevice dev=it.next();
				//Log.i(TAG, "dev status :" + dev.status);
				Log.i(TAG, "device :" + dev);

				/*
        		if(dev.status == WifiP2pDevice.CONNECTED){
        			Log.i(TAG, "ok! we found it!, show connected msg for 2 sec.");
					String str = activity.getString(R.string.wifi_wps_success);
					activity.updateString(str);
					activity.mCurConnectedDevice = dev;
					CountDownTimer timer_show = new CountDownTimer(2000, 2000) {  
						public void onTick(long millisUntilFinished) { 
						}  
						public void onFinish() { 
							String str2 = activity.getString(R.string.wifi_wps_after_success);
							activity.updateString(str2);
							Log.i(TAG, "wifi_wps_after_success");
							activity.getPeerAddress();
						}  
					}.start();
        			break;       			
				}else if(activity.mCurConnectedDevice != null && dev.deviceAddress.equals(activity.mCurConnectedDevice.deviceAddress)) {
        			Log.i(TAG, "peer disconnected.");
					if(activity.curState != MiracastActivity.WFD_STATE_DISCONNECTED) {
						activity.curState = MiracastActivity.WFD_STATE_DISCONNECTED;
						activity.mCurConnectedDevice = null;
						Intent intent = new Intent("MediaPlayerKill");
						activity.sendBroadcast(intent);
					}
        		}else{
        			Log.i(TAG, "no peer connected.");
        		}
				*/
        	}
        }
    }

    public void clearPeers() {
        peers.clear();
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();
    }

}
