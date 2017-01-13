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

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class WFDDeviceDetail implements ConnectionInfoListener {

    private static final String TAG = "WFDDeviceDetail";
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    public WifiP2pDevice device;
    public WifiP2pDevice thisDevice = null;
    public WifiP2pInfo connectionInfo=null;
    public String url;
	public String name;
    ProgressDialog progressDialog = null;
    public Handler mHandler;
	private MiracastActivity activity;
    private CountDownTimer timer_show = null;
	public WFDDeviceDetail(MiracastActivity act){
			this.activity = act;
			mHandler = new Handler(){
				@Override  
				public void handleMessage(Message msg){
					switch (msg.what) {  
						case MiracastActivity.WFD_URL:
								Bundle b = msg.getData();
								url = b.getString("URL");
								name = b.getString("device");
								if(name == null)
										name = "Unkown";
								if(url !=null){
										ComponentName componetName = new ComponentName("com.rtk.android.miracast",
														"com.rtk.android.miracast.videoPlayerActivity");
										Intent it= new Intent();
										Bundle bundle = new Bundle();    	  
										bundle.putString("URL", url);
										bundle.putString("device", name);
										Log.i(TAG, "Device device connectDevName is tiny_li: " + name);  
										it.putExtras(bundle);
										it.setComponent(componetName);
										activity.curState = MiracastActivity.WFD_STATE_CONNECTED;
										if(activity.timer_flag != null) { 
											activity.timer_flag.cancel();
											activity.timer_flag = null;
										}
										if(activity.timerUI_flag != null ){
											activity.timerUI_flag.cancel();
											activity.timerUI_flag = null;
										}
										activity.startActivityForResult(it,0);
								}
								Log.i(TAG, "we had send the url and start the videoPlayer: " + url);
								break;
						case MiracastActivity.WFD_CREATEGROUP:
								//activity.createNewGroup();
								break;
						case MiracastActivity.WFD_PINANY:
								activity.pinAny();
								break;
						case MiracastActivity.WFD_RESET:
								activity.resume();
								break;
						default:
								break;
					} 
					super.handleMessage(msg);  
				}  
			};
	}

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		Log.i(TAG, "onConnectionInfoAvailable :" + info);
    	this.connectionInfo = info;
    	
    	if(info.isGroupOwner == true){
    		Log.i(TAG, "I AM THE OWNER!");
    	}else{
    		Log.i(TAG, "I AM NOT THE OWNER");
    	}

		for(WifiP2pDevice device:activity.mDeviceList.peers){
			Log.i(TAG, "status :" + device.status);
			if(device.status == WifiP2pDevice.CONNECTED || device.status==WifiP2pDevice.INVITED){
				//if(device.status==WifiP2pDevice.CONNECTED){
				activity.curState = MiracastActivity.WFD_STATE_CONNECTING;
				activity.mCurConnectedDevice = device;
				String str = activity.getString(R.string.tcl_miracast_tips2);
				activity.updateString(str);
				activity.bWait = false;
				CountDownTimer timer_show = new CountDownTimer(2000, 2000) {  
					public void onTick(long millisUntilFinished) { 
					}  
					public void onFinish() { 
						//String str2 = activity.getString(R.string.wifi_wps_after_success);
						//activity.updateString(str2);
						Log.i(TAG, "wifi_wps_after_success");
						activity.getPeerAddress();
					}  
				}.start();
				if(device.wfdInfo != null)
					Log.i(TAG, "Connect device wfdInfo: " + device.wfdInfo.toString());   	    			
				else
					Log.i(TAG, "Connect device wfdInfo is null");   	    			
				break;
			}
		}
		/*
		//((TextView)activity.findViewById(R.id.wps_dialog_txt)).setText(R.string.wifi_wps_success);
		((TextView)activity.findViewById(R.id.wps_dialog_txt)).setText(R.string.wifi_wps_after_success);
        timer_show = new CountDownTimer(1000, 1000) {  
        //timer_show = new CountDownTimer(2000, 1000) {  
			    public void onTick(long millisUntilFinished) { 
				}  
				public void onFinish() { 
				((TextView)activity.findViewById(R.id.wps_dialog_txt)).setText(R.string.wifi_wps_after_success);
					Log.i(TAG, "wifi_wps_after_success");
					 activity.getIpAddress();
				}  
			}.start();
			*/
       
        //UI CHAHGE HERE
        //activity.mWpsTextView.setText(R.string.wfd_ip_get);
        //this.info = info;        
    }  

}
