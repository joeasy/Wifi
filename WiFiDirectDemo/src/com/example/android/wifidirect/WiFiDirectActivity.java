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

import java.net.InetAddress;     
import java.net.NetworkInterface;
import java.net.SocketException; 
import java.util.Enumeration;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.net.NetworkInfo;
//import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.example.android.wifidirect.DeviceDetailFragment.connectType;
import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;
import com.example.android.wifidirect.ChannelDetailFragment.ChannelActionListener;
import android.net.wifi.p2p.WifiP2pManager.PersistentGroupInfoListener;
import android.net.wifi.p2p.WifiP2pGroupList;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceActionListener, ChannelActionListener, PersistentGroupInfoListener{

    public static final String TAG = "wifidirectdemo";
    private static final boolean DEBUG = false;
    
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    public DeviceListFragment fragmentList;
    public DeviceDetailFragment fragmentDetails;
    public ChannelDetailFragment fragmentChannelDetails;
    
    public static final int WFD_URL = 0;
	public static final int WFD_CREATEGROUP = 1;
	public static final int WFD_PINANY = 2;
	public static final int WFD_RESET = 3;
	
	public static final int WFD_STATE_INIT = 0;
	public static final int WFD_STATE_READY = 1;
	public static final int WFD_STATE_CONNECTING = 2;
	public static final int WFD_STATE_CONNECTED = 3;
	public static final int WFD_STATE_DISCONNECTED = 4;
	public int curState = WFD_STATE_INIT;
	
	public static final int WFD_MODE_UNKOWN = 0;
	public static final int WFD_MODE_DISCOVER = 1;
	public static final int WFD_MODE_LISTEN = 2;
	public static final int WFD_MODE_AUTOGO = 3;
	public int curMode = WFD_MODE_UNKOWN;

	//public DialogListener dl = null;

    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    WifiP2pGroupList grouplist;
    private BroadcastReceiver receiver = null;
    
    private int groupOwnerIntent = 7;

    private MenuItem mPinAnyItem;
    
	// flag for hiding connect button in pin any mode
    public boolean bPinAny = false;

    public String myDevIp = null;
    public WifiP2pDevice mCurConnectedDevice;
    
    private static final int DEFAULT_CONTROL_PORT = 8554;
	private static final int MAX_THROUGHPUT = 50;
    public boolean mWfdEnabled = false;
	public boolean mWfdEnabling = false;

    private TextView mWpsTextView;
    private Handler mHandler = new Handler();
    public Handler mPinanyHandler;
    private enum WpsState {
        WPS_INIT,
        WPS_START,
        WPS_COMPLETE,
        CONNECTED, //WPS + IP config is done
        WPS_FAILED
    }
    WpsState mWpsState = WpsState.WPS_INIT;
    
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
        updateWfdEnableState(isWifiP2pEnabled);
		if(isWifiP2pEnabled) {
			manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
				@Override
				public void onSuccess() {
					Log.i(TAG, "Discovery Initiated");
				}
				@Override
				public void onFailure(int reasonCode) {
				Log.e(TAG, "Discovery Failed");
				}
			});
		}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        Log.d("Lynn", "mDisplayMetrics.widthPixels: " + mDisplayMetrics.widthPixels); //1920
        Log.d("Lynn", "mDisplayMetrics.heightPixels: " + mDisplayMetrics.heightPixels); 
        
        LinearLayout llayout_device_list = (LinearLayout)findViewById(R.id.linearLayout2);
		LinearLayout.LayoutParams llayoutparams_device_list = new LinearLayout.LayoutParams(
	              LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llayoutparams_device_list.width  = mDisplayMetrics.widthPixels * 1 / 5;
		llayout_device_list.setLayoutParams(llayoutparams_device_list);
		
		LinearLayout llayout_device_detail = (LinearLayout)findViewById(R.id.linearLayout3);
		LinearLayout.LayoutParams llayoutparams_device_detail = new LinearLayout.LayoutParams(
	              LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llayoutparams_device_detail.width  = mDisplayMetrics.widthPixels * 3 / 5;
		llayoutparams_device_detail.height  = mDisplayMetrics.heightPixels;
		llayout_device_detail.setLayoutParams(llayoutparams_device_detail);
        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION);
        
        fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragmentChannelDetails = (ChannelDetailFragment) getFragmentManager().findFragmentById(R.id.channel_detail);
        		
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        /* no DialogListener exits in 4.4
		dl = new DialogListener() {
	    		@Override
	    		public void onShowPinRequested(String pin) {
	    			Log.e("Lynn", "onShowPinRequested :" + pin);
	    			fragmentList.mPIN = pin;
	    			fragmentList.updateThisDevice(fragmentList.getDevice());
	    		}
	    			
	    		@Override
	    		public void onConnectionRequested(WifiP2pDevice device, WifiP2pConfig config) {
	    			Log.e("Lynn", "onConnectionRequested device :" + device);
	    			Log.e("Lynn", "onConnectionRequested config :" + config);
	    			curState = WFD_STATE_CONNECTING;
					if(config.wps.setup == WpsInfo.PBC) {
						if(groupOwnerIntent >= 0 && groupOwnerIntent <= 15)
							config.groupOwnerIntent = groupOwnerIntent;
						Log.i(TAG, "connect to " + config);
						manager.connect(channel, config, new ActionListener() {
							@Override
							public void onSuccess() {
								Log.d(TAG, "connect ok");
							}
							@Override
							public void onFailure(int reason) {
								Log.d(TAG, "connect failed :" + reason);
							}
						});
					} else if(config.wps.setup == WpsInfo.DISPLAY) {
						config.wps.pin = fragmentList.mPIN;
						if(groupOwnerIntent >= 0 && groupOwnerIntent <= 15)
							config.groupOwnerIntent = groupOwnerIntent;
						Log.i(TAG, "connect to " + config);
						manager.connect(channel, config, new ActionListener() {
							@Override
							public void onSuccess() {
								Log.d(TAG, "connect ok");
							}
							@Override
							public void onFailure(int reason) {
								Log.d(TAG, "connect failed :" + reason);
							}
						});
					}
	    		}
	    			
	    		@Override
	    		public void onAttached(){
	    			Log.e("Lynn", "onAttached");
	    		}
	    			
	    		@Override
	    		public void onDetached(int reason) {
	    			Log.e("Lynn", "onDetached :" + reason);
	    		}
	    		
	    	}; */
    }
    
    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        
        mWpsTextView = (TextView)findViewById(R.id.wps_dialog_txt);
        mWpsTextView.setText(R.string.empty);
        fragmentDetails.mConnectType = connectType.CONNECT;
        curState = WFD_STATE_READY;
		//updateMode();
    }

    @Override
    public void onPause() {
    	
    	// set op channel 0
    	cmd("OP_CHANNEL 0", new ActionListener() {
			@Override
			public void onSuccess() {
				Log.d("lynn", "op channel exec cmd onSuccess");
			}
			@Override
			public void onFailure(int reason) {
				Log.e("lynn", "op channel exec cmd onFailure");
			}
		});

    	cmd("SET_AUTH off", new ActionListener() {
			@Override
			public void onSuccess() {
				Log.d("lynn", "auth exec cmd onSuccess");
			}
			@Override
			public void onFailure(int reason) {
				Log.e("lynn", "auth exec cmd onFailure");
			}
		});
    	
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void releasePlayer() {
    	if(curState == WiFiDirectActivity.WFD_STATE_CONNECTED)
		{
			curState = WiFiDirectActivity.WFD_STATE_DISCONNECTED;
			mCurConnectedDevice = null;

			if(fragmentDetails != null && fragmentDetails.mPlayer != null) {
				fragmentDetails.mPlayer.stop();
				Log.d("Lynn", "videoPlayer->onDestroy()! mPlayer != null ->mPlayer.stop()");
				fragmentDetails.mPlayer.release();
				fragmentDetails.mPlayer = null;
				fragmentDetails.curMediaMode = fragmentDetails.MEDIA_MODE_UNKOWN;
			}	
		}
    }
    
    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
    	// release mPlayer
    	releasePlayer();
    	
        if (fragmentList != null) {
        	fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
        	fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        
        // hide "PIN ANY" button
        mPinAnyItem = menu.getItem(4);
        mPinAnyItem.setVisible(false);
        
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	bPinAny = false;
        switch (item.getItemId()) {
        	case R.id.atn_direct_discover:
	            if (!isWifiP2pEnabled) {
	                Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
	                        Toast.LENGTH_SHORT).show();
	                return true;
	            }
				discoverMode();
	            fragmentList.onInitiateDiscovery();
	            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
	
	                @Override
	                public void onSuccess() {
	                    Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
	                            Toast.LENGTH_SHORT).show();
	                }
	
	                @Override
	                public void onFailure(int reasonCode) {
	                    Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
	                            Toast.LENGTH_SHORT).show();
	                }
	            });
	            return true;
            
        	case R.id.listen:
				listenMode();
                cmd("P2P_LISTEN", new ActionListener(){
                	@Override
                	public void onFailure(int reasonCode) {
                		Log.d(TAG, "Listen failed. Reason :" + reasonCode);
                	}

                	@Override
                	public void onSuccess() {
                		Log.d(TAG, "Listen success");
                	}
                });
                return true;
                
            case R.id.group_create:
				autogoMode();
            	if (manager != null && channel != null) {
	            	manager.createGroup(channel, new ActionListener(){
	            		@Override
	            		public void onFailure(int reasonCode) {
	            			Log.d(TAG, "GroupCreate failed. Reason :" + reasonCode);
	            		}
	
	            		@Override
	            		public void onSuccess() {
	            			Log.d(TAG, "GroupCreate success");
	            		}
	                });
	            	mPinAnyItem.setVisible(true);
	            	fragmentDetails.resetButtons(false);
            	} else {
            		Log.e(TAG, "channel or manager is null");
            		return false;
            	}
                return true;
                
            case R.id.group_remove:
                return removeGroup();
                
            case R.id.pin_any:
                if (manager != null && channel != null) {
                	bPinAny = true;
                	WifiP2pConfig config = new WifiP2pConfig();
                    config.wps.setup = WpsInfo.DISPLAY;
                    connect(config);
                } else {
                    Log.e(TAG, "start pin any fail, channel or manager is null");
                }
                return true;

            /*
            case R.id.pin_cancel:
                Log.e(TAG, "pin cancel fail");
                return true;
            */
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*@Override
    public void sentGroupMessage(WifiP2pInfo info) {
    	DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        fragment.showGroupMessage(info);
    }*/
    
    @Override
    public void showDetails(WifiP2pDevice device) {
    	fragmentDetails.showDetails(device);

    }

    private void updateWpsState(final WpsState state, final String msg) {
        if (mWpsState.ordinal() >= state.ordinal()) {
            //ignore.
            //return;
        }
        mWpsState = state;
        mHandler.post(new Runnable() {
        	@Override
        	public void run() {
        		switch(state) {
        		case WPS_COMPLETE:
        			break;
        		case CONNECTED:
        		case WPS_FAILED:
        			break;
        		}
        		mWpsTextView.setText(msg);
        	}
        });
    }
    
    public Channel getChannel() {
    	return channel;
    }
    
    @Override
    public void connect(WifiP2pConfig config) {    	
    	config.groupOwnerIntent = groupOwnerIntent;
    	Log.e("Lynn", "connect");
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            	Log.e("Lynn", "WifiDirectActivity___onSuccess");
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
            	Log.e("Lynn", "WifiDirectActivity___onFailure");
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        
    }

    public void onPersistentGroupInfoAvailable(WifiP2pGroupList groups) {        
       grouplist=groups;
 	   Log.d(TAG, "[ANDY]onPersistentGroupInfoAvailable: " + groups);
 	   if(grouplist!=null)
 	   {
 		   for(WifiP2pGroup group:grouplist.getGroupList()) {
 			   Log.d(TAG, "[ANDY] GROUP: " + group.getNetworkName() + " - " + group.getPassphrase());
 		   }
 	   }
 	   fragmentList.updateGroupInfo(groups);
    }  
    
    public void updateWfdEnableState(boolean isEnable) {
        if (isEnable) {
            // WFD should be enabled.
            if (!mWfdEnabled && !mWfdEnabling) {
                mWfdEnabling = true;

                WifiP2pWfdInfo wfdInfo = new WifiP2pWfdInfo();
                wfdInfo.setWfdEnabled(true);
                wfdInfo.setDeviceType(WifiP2pWfdInfo.PRIMARY_SINK);
                wfdInfo.setSessionAvailable(true);
                wfdInfo.setControlPort(DEFAULT_CONTROL_PORT);
                wfdInfo.setMaxThroughput(MAX_THROUGHPUT);
                manager.setWFDInfo(channel, wfdInfo, new ActionListener() {
                    @Override
                    public void onSuccess() {
						Log.i(TAG, "Successfully set WFD info.");
                        if (mWfdEnabling) {
                            mWfdEnabling = false;
                            mWfdEnabled = true;
                            //reportFeatureState();
                        }
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.i(TAG, "Failed to set WFD info with reason " + reason + ".");
                        mWfdEnabling = false;
                    }
                });
            }
        } else {
            // WFD should be disabled.
            mWfdEnabling = false;
            mWfdEnabled = false;
            //reportFeatureState();
            disconnect();
        }
    }
    
    public boolean removeGroup() {
    	unkownMode();
		removePersistentGroup();
    	if (manager != null && channel != null) {
    		fragmentDetails.resetViews();
    		fragmentList.updateMyIp(null);

        	manager.removeGroup(channel, new ActionListener() {
        		@Override
        		public void onFailure(int reasonCode) {
        			Log.e(TAG, "GroupRemove failed. Reason :" + reasonCode);
        		}

        		@Override
        		public void onSuccess() {
        			//fragmentDetails.getView().setVisibility(View.GONE);
        			fragmentDetails.getView().findViewById(R.id.linearLayout1).setVisibility(View.GONE);
        			Log.d(TAG, "GroupRemove success");
        		}
        	});
        	mPinAnyItem.setVisible(false);
        	return true;
    	} else {
    		Log.e(TAG, "channel or manager is null");
    		return false;
    	}
    }
    
    @Override
    public void disconnect() {
    	Log.e("Lynn", "disconnect");
    	fragmentDetails.resetViews();
    	fragmentList.updateMyIp(null);
    	
    	releasePlayer();
    	
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
            	fragmentDetails.getView().findViewById(R.id.linearLayout1).setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
    	Log.e("Lynn", "onChannelDisconnected");
        // we will try once more
        if (manager != null && !retryChannel) {
        	Log.e("Lynn", "4444444444444444444444");
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {
    	Log.e("Lynn", "cancelDisconnect");

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            if (fragmentList.getDevice() == null
                    || fragmentList.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragmentList.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragmentList.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }
    
    @Override
    public void setGroupOwnerIntent(int num) {
    	groupOwnerIntent = num;
    }
    
    @Override
    public void cmd(String str, ActionListener listener) {
    	if(str!=null)
    		Log.d("lynn", "str: " + str);
    	
    	if (manager != null && channel != null) {
    		manager.execCmd(channel, str, listener);
    	} else {
    		Log.e(TAG, "channel or manager is null");
    		return;
    	}	
    }

    @Override
    public void setWifiP2pChannels(final int lc, final int oc) {
        if (DEBUG) {
        	Log.d("Lynn", "Setting wifi p2p channel: lc=" + lc + ", oc=" + oc);
        }
        manager.setWifiP2pChannels(channel,
                lc, oc, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                	Log.d("Lynn", "Successfully set wifi p2p channels.");
                }
            }

            @Override
            public void onFailure(int reason) {
            	Log.e("Lynn", "Failed to set wifi p2p channels with reason " + reason + ".");
            }
        });
    }
    
	public void removePersistentGroup()
	{
		releasePlayer();
		
		Log.i(TAG, "removePersistentGroup");
		if(grouplist == null)
			return;
		int size = grouplist.getGroupList().size();
		Log.d(TAG, "grouplist size is :" + size);
		for(WifiP2pGroup group:grouplist.getGroupList()) {
				manager.deletePersistentGroup(channel,group.getNetworkId(),
					new WifiP2pManager.ActionListener() {
						public void onSuccess() {
							Log.d(TAG, " delete groupPersistent success");
						}
						public void onFailure(int reason) {
							Log.d(TAG, " delete groupPersistent fail " + reason);
						}
					});
		}
	}

	public String getModeString() {
		String mode;
		switch(curMode) {
				case WFD_MODE_UNKOWN:
						mode = "UNKOWN";
						break;
				case WFD_MODE_DISCOVER:
						mode = "DISCOVER";
						break;
				case WFD_MODE_LISTEN:
						mode = "LISTEN";
						break;
				case WFD_MODE_AUTOGO:
						mode = "AUTOGO";
						break;
				default:
						mode = "UNKOWN";
						break;
		}
		return mode;
	}

	public void unkownMode() {
		Log.d(TAG, "WFD_MODE_UNKOWN");
		curMode = WFD_MODE_UNKOWN;
		fragmentList.updateMode(getModeString());
	   	//manager.setDialogListener(channel, null);
	   	fragmentDetails.resetButtons(true);
	}

	public void discoverMode() {
		Log.d(TAG, "WFD_MODE_DISCOVER");
		curMode = WFD_MODE_DISCOVER;
		fragmentList.updateMode(getModeString());
	   	//manager.setDialogListener(channel, null);
	   	fragmentDetails.resetButtons(true);
	}

	public void listenMode() {
		Log.d(TAG, "WFD_MODE_LISTEN");
		curMode = WFD_MODE_LISTEN;
		fragmentList.updateMode(getModeString());
	   	//manager.setDialogListener(channel, dl);
	   	fragmentDetails.resetButtons(false);
	}

	public void autogoMode() {
		Log.d(TAG, "WFD_MODE_AUTOGO");
		curMode = WFD_MODE_AUTOGO;
		curState = WFD_STATE_READY;
		fragmentList.updateMode(getModeString());
	   	//manager.setDialogListener(channel, null);
	   	fragmentDetails.resetButtons(false);
	}

	public void setDeviceName(String str) {
		Log.d(TAG, "setDeviceName :" + str);
		manager.setDeviceName(channel, str, new ActionListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "setDeviceName ok");
			}
			@Override
			public void onFailure(int reason) {
				Log.d(TAG, "setDeviceName failed :" + reason);
			}
		});
	}
	
	public void getPeerAddress(){
		//new Thread(new Runnable(){
			//@Override
			//public void run(){
	            String Address= null;
				String port = null;
				String ip = null;
				String name = null;
				int count = 0;
				boolean isGO = false;
				DeviceDetailFragment frag = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);

			    if(mCurConnectedDevice!=null)
			    {
					isGO = !(mCurConnectedDevice.isGroupOwner());
					Log.i(TAG, "got GO info from mCurConnectedDevice.isGroupOwner() :" + isGO);
					name = mCurConnectedDevice.deviceName;
					Address=mCurConnectedDevice.deviceAddress;
					Log.i(TAG, "Device device kavin getIpAddress is: " + Address);   
			    }

				if(mCurConnectedDevice != null && mCurConnectedDevice.wfdInfo != null){
					if(mCurConnectedDevice.wfdInfo.getControlPort() > 0)
					{
						port = String.valueOf(mCurConnectedDevice.wfdInfo.getControlPort());
					}
				}
				Log.i(TAG, "Device port is: " + port);

				if(fragmentDetails == null) {Log.i(TAG, "fragmentDetails is null");}
				if(frag == null) {Log.i(TAG, "frag is null");}
				else if(frag.connectionInfo == null) {Log.i(TAG, "connectionInfo is null");}
				else {
					isGO = frag.connectionInfo.isGroupOwner;
					Log.i(TAG, "got GO info from frag.connectionInfo.isGroupOwner :" + isGO);
				}
			    
				if(isGO)
				{
					while(count < 20){
						ip = frag.thisDevice.getPeerIPAddr(Address);
						if(ip != null) break;
						try{
							Thread.sleep(800);
							count++;
						}catch(InterruptedException e){
							e.printStackTrace();
						}
					}
					//myDevIp = frag.connectionInfo.groupOwnerAddress.getHostAddress();
				} else {
					ip = frag.connectionInfo.groupOwnerAddress.getHostAddress();
					/*
					while(count < 20){
						myDevIp = frag.thisDevice.getPeerIPAddr(Address);
						if(myDevIp != null) break;
						try{
							Thread.sleep(800);
							count++;
						}catch(InterruptedException e){
							e.printStackTrace();
						}
					}
					*/
				}
				myDevIp = getLocalIpAddress();
				Log.i("Lynn", "Device ip is: " + ip);
				Log.i("Lynn", "Device name is: " + name);	
				Log.i("Lynn", "myDevIp ip is: " + myDevIp);
				
				if(myDevIp != null)
					fragmentList.updateMyIp(myDevIp);
				/*
				if(ip != null && port != null)
				{
					String url = "miracast://" + ip + ":" + port;
					Message msg=new Message();
					msg.what=WFD_URL;
					Bundle b =new Bundle();
					b.putString("URL", url);
					b.putString("device", name);
					msg.setData(b);
					frag.mHandler.sendMessage(msg);
				}
				else {
					Message msg=new Message();
					msg.what=WFD_RESET;
					frag.mHandler.sendMessage(msg);
				}*/
				
				if(ip != null) {
					String url = "miracast://" + ip + ":" + port;
					Message msg=new Message();
					msg.what=WFD_URL;
					Bundle b =new Bundle();
					b.putString("URL", url);
					b.putString("device", name);
					msg.setData(b);
					frag.mHandler.sendMessage(msg);
				}
			//}
		//}).start();
		
		Log.i(TAG, "after reading ip thread start()");
    }

	public String getLocalIpAddress() {     
			try {     
					Log.i(TAG, "WifiPreference IpAddress");     
					String curIp = null;
					/*
					NetworkInterface en = NetworkInterface.getByName("p2p0");
					if(en != null) {
							for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {     
									InetAddress inetAddress = enumIpAddr.nextElement();     
									if (!inetAddress.isLoopbackAddress()) {     
											Log.i(TAG, "WifiPreference IpAddress: " + inetAddress.getHostAddress().toString());     
											return inetAddress.getHostAddress().toString();     
									}     
							}     
					}
					*/
					for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {     
							NetworkInterface intf = en.nextElement();
							Log.i(TAG, "name :" + intf.getName() +" " + intf.getDisplayName() + " " + intf.toString());     
							if(intf.getName().equals("p2p0")) {
									for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {     
											InetAddress inetAddress = enumIpAddr.nextElement();     
											if (!inetAddress.isLoopbackAddress()) {     
													curIp = inetAddress.getHostAddress().toString();     
													Log.i(TAG, "WifiPreference IpAddress: " + inetAddress.getHostAddress().toString());     
											}     
									}
							}
					}
					return curIp;
			} catch (SocketException ex) {     
					Log.e(TAG, "getLocalIpAddress ex: " + ex.toString());     
			}     
			return null;     
	}     
}
