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

package com.example.android.wifidirect;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.os.CountDownTimer;
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

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements PeerListListener {

    public List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;
	public String mPIN = null;
	WiFiDirectActivity activity = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));

		mPIN = GetPIN();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        activity = (WiFiDirectActivity)getActivity();
        return mContentView;
    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }

    private static String getDeviceStatus(int deviceStatus) {
        Log.d(WiFiDirectActivity.TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }

    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener) getActivity()).showDetails(device);
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                }
            }

            return v;

        }
    }

    /**
     * Update UI for this device.
     * 
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName + " " + ((WiFiDirectActivity)getActivity()).getModeString());
        view = (TextView) mContentView.findViewById(R.id.my_pin);
        view.setText("PIN: " + mPIN);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
        
        
		if(activity.curState == activity.WFD_STATE_READY && device.status == WifiP2pDevice.CONNECTED)
		{
    		Log.i("Lynn", "WFD_STATE_CONNECTING");
    		activity.curState = activity.WFD_STATE_CONNECTING;
    		
			//String str = ((WiFiDirectActivity)getActivity()).getString(R.string.wfd_connecting);
			//activity.updateString(str);
		}
		else if(activity.curState == activity.WFD_STATE_CONNECTING || activity.curState == activity.WFD_STATE_CONNECTED) {
			if(device.status != WifiP2pDevice.CONNECTED) {
    			Log.i("Lynn", "WFD_STATE_CONNECTING failed.");
			}
		}
    }
    
    public void updateMyIp(String str) {
    	TextView view = (TextView) mContentView.findViewById(R.id.my_ip);
        if(str != null)
        	view.setText("IP: " + str);
        else 
        	view.setText(R.string.empty); 	
    }

	public void updateMode(String mode) {
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        if(device!=null)
        	view.setText(device.deviceName + " " + mode);
	}

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(WiFiDirectActivity.TAG, "No devices found");
            return;
        } else{
        	Iterator<WifiP2pDevice> it =peers.iterator();
        	while (it.hasNext()){
        		WifiP2pDevice dev=it.next();
				//Log.i(TAG, "dev status :" + dev.status);
				Log.i("Lynn", "device :" + dev);

				
        		if(dev.status == WifiP2pDevice.CONNECTED){
        			Log.i("Lynn", "ok! we found it!, show connected msg for 2 sec.");
					//String str = activity.getString(R.string.wifi_wps_success);
					//activity.updateString(str);
					activity.mCurConnectedDevice = dev;
					CountDownTimer timer_show = new CountDownTimer(2000, 2000) {  
						public void onTick(long millisUntilFinished) { 
						}  
						public void onFinish() { 
							//String str2 = activity.getString(R.string.wifi_wps_after_success);
							//activity.updateString(str2);
							Log.i("Lynn", "wifi_wps_after_success");
							((WiFiDirectActivity)getActivity()).getPeerAddress();
						}  
					}.start();
        			break;       			
				}else if(((WiFiDirectActivity)getActivity()).mCurConnectedDevice != null && dev.deviceAddress.equals(((WiFiDirectActivity)getActivity()).mCurConnectedDevice.deviceAddress)) {
        			Log.i("Lynn", "peer disconnected.");
					if(activity.curState != activity.WFD_STATE_DISCONNECTED) {
						activity.curState = activity.WFD_STATE_DISCONNECTED;
						activity.mCurConnectedDevice = null;
						//Intent intent = new Intent("MediaPlayerKill");
						//activity.sendBroadcast(intent);
					}
        		}else{
        			Log.i("Lynn", "no peer connected.");
        		}
				
        	}
        }

    }

    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * 
     */
    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        
                    }
                });
    }

	public void updateGroupInfo(WifiP2pGroupList groups) {
        TextView view = (TextView) mContentView.findViewById(R.id.group_message);
		view.setText("" + groups);
	}
    
    /*
    public void showGroupMessage(WifiP2pInfo info) {
    	TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
    }*/

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();
        
        //void sentGroupMessage(WifiP2pInfo info);
    }

	String  GetPIN()
	{   
		//int tmp [] = {1,2,3,4,5,6,7};
		int r=0;
		int snum = 0;
		int checksnum = 0;
		String pin = ""; 
		for (int i=0;i<7;i++) {
			r=(int)(Math.random()*10);
			//r = tmp[i];
			snum = snum*10+r;
			pin += r;
		}   

		checksnum = ComputeCheckSum(snum);
		return pin+checksnum;
	}   

	int ComputeCheckSum ( int PIN)
	{   
		long  acccum = 0;
		PIN *= 10; 
		acccum += 3*((PIN/10000000)%10);
		acccum += 1*((PIN/1000000)%10);
		acccum += 3*((PIN/100000)%10);
		acccum += 1*((PIN/10000)%10);
		acccum += 3*((PIN/1000)%10);
		acccum += 1*((PIN/100)%10);
		acccum += 3*((PIN/10)%10);
		int digit = (int)(acccum %10);
		return ((10-digit)%10);
	} 

}
