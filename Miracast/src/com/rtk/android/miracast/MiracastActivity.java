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

import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager.DialogListener;
import android.os.Build;
/*added by kavin*/
import android.net.wifi.p2p.WifiP2pManager.PersistentGroupInfoListener;
import android.net.wifi.p2p.WifiP2pGroupList;

import android.widget.TextView.OnEditorActionListener;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;

import android.app.TvManager;
import com.rtk.android.miracast.WFDDeviceList.DeviceActionListener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.CountDownTimer;


public class MiracastActivity extends Activity implements ChannelListener, PersistentGroupInfoListener, DeviceActionListener {

    public static final String TAG = "RTKMiracast";

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

	private static TvManager mTV;
	private static final int MENU_ID_MIRACAST = Menu.FIRST;
    private static final int MENU_ID_SETTING = Menu.FIRST + 1;
    private WifiP2pManager manager;
	public WifiManager mWifiManager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private final IntentFilter intentFilterPlayershutdown = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private BroadcastReceiver PlayershutdownReceiver = null;
    public WifiP2pDevice mCurConnectedDevice;
    private String ipAddr;
    //public TextView text;
    //public TextView myDevName;
	//public TextView myPin;
    //public EditText myShellCmd;
    public WifiP2pDevice thisDev;
	public String PIN = null;

	private static final int DEFAULT_CONTROL_PORT = 8554;
	private static final int MAX_THROUGHPUT = 50;
	public boolean mWfdEnabled = false;
	public boolean mWfdEnabling = false;
	public String devname=null;

	public static int p2pscan_flag = -1;
	public static int disableWfd=1;
	public WFDDeviceList mDeviceList = new WFDDeviceList(this);
	public WFDDeviceDetail mDeviceDetail = new WFDDeviceDetail(this);
	
	/*added by kavin*/
	 WifiP2pGroupList grouplist;
    public  TextView mWpsTextView;
    public ImageView mWpsImg;
    private int ImgProgressCount = 0;
    public boolean bWait = true;
    private Handler mHandler = new Handler();
	public CountDownTimer timerUI_flag=null;
	public CountDownTimer  timer_flag= null;

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
		updateWfdEnableState(isWifiP2pEnabled);
		if(isWifiP2pEnabled)
				WFDDiscover();
		String str = "SET p2p_go_intent " + "0";
		if (manager != null && channel != null) {
			/*	manager.execCmd(channel, str,new ActionListener() {
					@Override
					public void onSuccess() {
						Log.d("lynn", "go intend exec cmd onSuccess");
					}   
					@Override
					public void onFailure(int reason) {
						Log.e("lynn", "go intend exec cmd onFailure");
					}   
				});*/
		}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		mTV = (TvManager) this.getSystemService("tv");
 	    mTV.setSource(TvManager.SOURCE_PLAYBACK);
        //getWindow().setUiOptions(0);
		/*this.requestWindowFeature(Window.FEATURE_NO_TITLE);*/
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,       
                      WindowManager.LayoutParams. FLAG_FULLSCREEN);
		//getRight2Left(this);
 	    getWindow().setUiOptions(0);
		setContentView(R.layout.tcl_wait);
        LayoutInflater flater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE); 
        //tiny_li  enterDialog   start
		 mWpsTextView = (TextView)findViewById(R.id.text_tcl);
		 mWpsImg = (ImageView)findViewById(R.id.progress_tcl);
	     //myPin = (TextView)findViewById(R.id.mPin);
		 ready();
		 Log.e(TAG,"reday");
		 		
        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        /*added by kavin*/
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION);
        //intentFilter.addAction("MediaPlayershutdown");
		intentFilter.addAction("android.intent.action.POWER_DOWN");
		intentFilterPlayershutdown.addAction("MediaPlayershutdown");
		//baili_feng enable wifi if wifi is not enabled
		mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if(mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
			if (mWifiManager.setWifiEnabled(true)) {
				Log.d(TAG, "setWifiEnabled ok");
			} else {
				Log.d(TAG, "setWifiEnabled failed");
			}
		}

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
		
		Log.d("Lynn", "setDialogListener");
		manager.setDialogListener(channel, null);
		manager.setDialogListener(channel, new DialogListener() {
			@Override
			public void onShowPinRequested(String pin) {
				Log.d("Lynn", "onShowPinRequested");
				
			}

			@Override
			public void onConnectionRequested(WifiP2pDevice device, WifiP2pConfig config) {
				Log.d("Lynn", "onConnectionRequested device :" + device);
				Log.d("Lynn", "onConnectionRequested config :" + config);
				Log.i(TAG, "WFD_STATE_CONNECTING");
				curState = MiracastActivity.WFD_STATE_CONNECTING;
				String str = getString(R.string.wfd_connecting);
				bWait = false;
				updateString(str);
				config.groupOwnerIntent = 0;
				if(config.wps.setup == WpsInfo.PBC) {
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
					config.wps.pin = PIN;
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
				Log.d("Lynn", "onAttached");
			}

			@Override
			public void onDetached(int reason) {
				Log.d("Lynn", "onDetached");
			}
		});

    }
    
    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"onResume()");
		disableWfd=1;
		if(receiver == null) {
				receiver = new MiracastBroadcastReceiver(manager, channel, this);
				registerReceiver(receiver, intentFilter);
		}
		if(PlayershutdownReceiver == null){
			PlayershutdownReceiver = new MediaPlayershutdownBroadcastReceiver( this );
			registerReceiver( PlayershutdownReceiver,intentFilterPlayershutdown);
		}
		resumeUI();
		p2pscan_flag = 1;
		WFDDiscover();
		Thread p2pscan_thread = new Thread( new Runnable(){
			@Override
			public void run(){
				while(p2pscan_flag == 1){
					try{
						Thread.sleep(60000);
					}catch(Exception e ){
						
					}
					if(p2pscan_flag == 1)
						WFDDiscover();
				}
			}
		});
		p2pscan_thread.start();
	}
	
    void ready()
	{
		curState = WFD_STATE_INIT;
		mWpsImg.setImageResource( R.drawable.one );
		mWpsTextView.setText(R.string.tcl_miracast_tips1);
		bWait = true;
		ImgProgressCount = 1;
		resumeUI();
	    //resume();			   			
	}
    
    public void resumeUI(){
    	if(timerUI_flag != null) { 
			timerUI_flag.cancel();
			timerUI_flag = null;
		}
		Log.e(TAG,"time4 new");
		timerUI_flag = new CountDownTimer(3000, 3000) { 		
			public void onTick(long millisUntilFinished) {		
			} 		
			public void onFinish() { 
				Log.e(TAG,"onFinish()  tiny_li--------------------s");
				ImgProgressCount++;
				if( ImgProgressCount > 3 )
					ImgProgressCount = 1;
				switch( ImgProgressCount ){
				default:
				case 1:
					mWpsImg.setImageResource( R.drawable.one );
					break;
				case 2:
					mWpsImg.setImageResource( R.drawable.two );
					break;
				case 3:
					mWpsImg.setImageResource( R.drawable.three );
					break;
				}
				refreshUI();
			}  
		}.start();
    }
    
	public void resume()
	{	
		if(timer_flag != null) { 
			timer_flag.cancel();
			timer_flag = null;
		}
		/*
		Log.e(TAG,"time4 new");
		mWifiManager.setWifiEnabled(false);
		timer_flag = new CountDownTimer(2000, 1000) { 		
			public void onTick(long millisUntilFinished) {		
			} 		
			public void onFinish() { 
				//Log.e(TAG,"onFinish()  tiny_li--------------------s");
				
				//retry();
				mWifiManager.setWifiEnabled(true);
			}  
		}.start();
		*/
        //removePersistentGroup();
		if(isWifiP2pEnabled)
				WFDDiscover();
		PIN = GetPIN();
		curState = WFD_STATE_READY;
    }

    @Override
    public void onPause() {
        super.onPause();
        p2pscan_flag = -1;
        //unregisterReceiver(receiver);
	WFDStopDiscover();
        Log.i(TAG,"onPause()");        
    }
    
    @Override
    public void onStop() {
        super.onStop();
	if(disableWfd==1)
        {
         if(receiver!=null)
         {
          unregisterReceiver(receiver);
          receiver=null;
         }
        }
        //unregisterReceiver(receiver);
        if(timerUI_flag != null){
        	timerUI_flag.cancel();
        	timerUI_flag = null;
        }
        /*if(receiver!=null){
        	unregisterReceiver(receiver);
        	receiver = null;
        }*/
        Log.i(TAG,"onStop()");        
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
		manager.setDialogListener(channel, null);
		if(PlayershutdownReceiver != null)
		{
         unregisterReceiver(PlayershutdownReceiver);
         PlayershutdownReceiver = null;
        }
		if(receiver!=null){
        	unregisterReceiver(receiver);
        	receiver = null;
        }
		if(timer_flag != null) {
        Log.i(TAG, "onDestroy(): timer_flag.cancel() enter");		
		timer_flag.cancel(); timer_flag = null;
		Log.i(TAG, "onDestroy() timer_flag.cancel() finish");
		}
		p2pscan_flag = -1;
		if(timerUI_flag != null){
			timerUI_flag.cancel();
			timerUI_flag = null;
		}
    }
    
	public void WFDStopDiscover() {
		manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.i(TAG, "stop Discovery Initiated");
				
			}

			@Override
			public void onFailure(int reasonCode) {
				Log.e(TAG, "stop Discovery Failed");
			}
		});
	}

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
		//setIsWifiP2pEnabled(false);
        if (mDeviceList != null) {
            mDeviceList.clearPeers();
        }
        if (mDeviceDetail != null) {
            //fragmentDetails.resetViews();
        }
    }

	/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// add menu MiracastTest
        menu.add(Menu.NONE, MENU_ID_MIRACAST, Menu.NONE, R.string.miracast_list);
        // add menu settings 
        menu.add(Menu.NONE, MENU_ID_SETTING, Menu.NONE, R.string.miracast_setting);
        return super.onCreateOptionsMenu(menu); 
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()) {
    	case MENU_ID_MIRACAST:
    		Intent intent = new Intent(this, MiracastTestList.class);
    		startActivity(intent);
    		return true;
    	case MENU_ID_SETTING:
    		return true;
    	default:
    		return false;
    	}
    }
	*/

    public void showDetails(WifiP2pDevice device) {
    }

    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MiracastActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void disconnect() {
        //fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {            	
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {            	
                Log.d(TAG, "remove Group is OK");
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
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

    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            if (mDeviceList.getDevice() == null
                    || mDeviceList.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
				  Log.i(TAG, "mDeviceList.getDevice() == null: " );
            } else if (mDeviceList.getDevice().status == WifiP2pDevice.AVAILABLE
                    || mDeviceList.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(MiracastActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(MiracastActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

	public void getPeerAddress(){
		new Thread(new Runnable(){
			@Override
			public void run(){
	            String Address= null;
				String port = null;
				String ip = null;
				String name = null;
				int count = 0;

			    if(mCurConnectedDevice!=null)
			    {
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
			    
				if(mDeviceDetail.connectionInfo.isGroupOwner)
				{
					while(count < 20){
						ip = mDeviceDetail.thisDevice.getPeerIPAddr(Address);
						if(ip != null) break;
						try{
							Thread.sleep(800);
							count++;
						}catch(InterruptedException e){
							e.printStackTrace();
						}
					}
				} else {
					ip = mDeviceDetail.connectionInfo.groupOwnerAddress.getHostAddress();
				}
				Log.i(TAG, "Device ip is: " + ip);
				Log.i(TAG, "Device name is: " + name);

				if(ip != null && port != null)
				{
					disableWfd=0;
					String url = "miracast://" + ip + ":" + port;
					Message msg=new Message();
					msg.what=WFD_URL;
					Bundle b =new Bundle();
					b.putString("URL", url);
					b.putString("device", name);
					msg.setData(b);
					mDeviceDetail.mHandler.sendMessage(msg);
				}
				else {
					Message msg=new Message();
					msg.what=WFD_RESET;
					mDeviceDetail.mHandler.sendMessage(msg);
				}
			}
		}).start();
		
		Log.i(TAG, "after reading ip thread start()");
    }

    public void updateWfdEnableState(boolean isEnable) {
        if (isEnable) {
        	setDeviceName(Build.BRAND+"-LEDTV");
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
		if(mWfdEnabled || mWfdEnabling){
                WifiP2pWfdInfo wfdInfo = new WifiP2pWfdInfo();
                wfdInfo.setWfdEnabled(false);
				manager.setWFDInfo(channel, wfdInfo, new ActionListener(){
                    @Override
                    public void onSuccess() {                     
                            Log.i(TAG, "WFD disabled Successfully set WFD info.");
                        }
                    
                    @Override
                    public void onFailure(int reason) {                   
                            Log.i(TAG, "WFD disabled Failed to set WFD info with reason " + reason + ".");
                    }
                });			
            }
            mWfdEnabling = false;
            mWfdEnabled = false;
            //reportFeatureState();
            disconnect();
        }
    }
    
    /*added by kavin*/
    public void onPersistentGroupInfoAvailable(WifiP2pGroupList groups) {        
       grouplist=groups;
	   Log.d(TAG, "onPersistentGroupInfoAvailable: " + groups);
	   if(grouplist!=null)
	   {
			   for(WifiP2pGroup group:grouplist.getGroupList()) {
					   Log.d(TAG, "GROUP: " + group.getNetworkName() + " - " + group.getPassphrase());
			   }
	   }
    }  
    public void removePersistentGroup()
    {
		Log.i(TAG, "removePersistentGroup");
        if(grouplist!=null)
        {
			int size = grouplist.getGroupList().size();
            Log.d(TAG, "grouplist size is :" + size);
			if(size == 0) {
			   Message msg=new Message();
			   msg.what=WFD_CREATEGROUP;
			   mDeviceDetail.mHandler.sendMessage(msg);
			}
			int count = 0;
			for(WifiP2pGroup group:grouplist.getGroupList()) {
				if(++count < size) {
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
				else {
            	manager.deletePersistentGroup(channel,group.getNetworkId(),
                            new WifiP2pManager.ActionListener() {
                                 public void onSuccess() {
                                      Log.d(TAG, "LAST delete groupPersistent success");
									  Message msg=new Message();
									  msg.what=WFD_CREATEGROUP;
									  mDeviceDetail.mHandler.sendMessage(msg);
                                 }
                                 public void onFailure(int reason) {
                                      Log.d(TAG, "LAST delete groupPersistent fail " + reason);
									  Message msg=new Message();
									  msg.what=WFD_CREATEGROUP;
									  mDeviceDetail.mHandler.sendMessage(msg);
                                 }
                             });
				}
			}
       } else {
               Log.d(TAG, "NO delete groupPersistent");
			   Message msg=new Message();
			   msg.what=WFD_CREATEGROUP;
			   mDeviceDetail.mHandler.sendMessage(msg);
	   }
    }

	public void createNewGroup() {
			Log.i(TAG, "createNewGroup");
			manager.createGroup(channel, new ActionListener() {    	
				@Override
				public void onFailure(int reasonCode) {
					Log.d(TAG, "GroupCreate failed. Reason :" + reasonCode);
					Message msg=new Message();
					msg.what=WFD_PINANY;
					mDeviceDetail.mHandler.sendMessageDelayed(msg,1000);
				}

				@Override
				public void onSuccess() {
					Log.d(TAG, "GroupCreate success");
					Message msg=new Message();
					msg.what=WFD_PINANY;
					mDeviceDetail.mHandler.sendMessageDelayed(msg,1000);
				}
			});
	}

	public void pinAny() {
			Log.i(TAG, "pinAny");
			// start pin any
			WifiP2pConfig config = new WifiP2pConfig();
			config.wps.setup = WpsInfo.DISPLAY;
			curState = WFD_STATE_READY;

			Log.d("Lynn", "start pin any");
			manager.connect(channel, config, new ActionListener() {
				@Override
				public void onSuccess() {
					Log.d("Lynn", "start pin any success");
				}
				@Override
				public void onFailure(int reason) {
					Log.d("Lynn", "start pin any failed :" + reason);
				}
			});
	}

	public void WFDDiscover() {
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
	
	private class ConvertThread extends Thread{
		private KeyEvent mEvent;
		
		public ConvertThread( KeyEvent event ){
			mEvent = event;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			//super.run();
			try{
		        Instrumentation inst = new Instrumentation();
		        inst.sendKeyDownUpSync( mEvent.getKeyCode());
			}catch(Exception e ){
		        Log.e("TAG","Exception when sendKeyDownUpSync : " + e.toString() );
			}
		}
		
	}

	//tiny_li
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (event.getAction() == KeyEvent.ACTION_UP) {
		    Log.i(TAG, "KEYCODE="+keyCode);
        	if (keyCode ==111 /*KeyEvent.KEYCODE_ESCAPE */|| keyCode == KeyEvent.KEYCODE_BACK ) {
        		//mWifiManager.setWifiEnabled(false);
					if(timer_flag != null) {
					Log.i(TAG, "KeyEvent.KEYCODE_ESCAPE: timer_flag.cancel() enter");
					timer_flag.cancel(); timer_flag = null;
					Log.i(TAG, "KeyEvent.KEYCODE_ESCAPE: timer_flag.cancel() finish");
					}
					p2pscan_flag = -1;
					/*
					try{
						Thread.sleep(500);						
					}catch(Exception e){
						
					}
					*/
					if( timerUI_flag != null ){
						timerUI_flag.cancel();
						timerUI_flag = null;
					}
					//mWifiManager.setWifiEnabled(true);
					finish();
        		return true;
			}
        	else if(keyCode == KeyEvent.KEYCODE_MENU ){
        		
        		KeyEvent tempEvent = new KeyEvent( event.getAction(), KeyEvent.KEYCODE_BACK );        		
        		new ConvertThread(tempEvent).start();
        		
        		return true;
        	}
        	else if( keyCode == KeyEvent.KEYCODE_DPAD_LEFT ){
        		
        		KeyEvent tempEvent = new KeyEvent( event.getAction(), KeyEvent.KEYCODE_VOLUME_DOWN );        		
        		new ConvertThread(tempEvent).start();
        		
        		return true;
        	}
        	else if( keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ){
        		
        		KeyEvent tempEvent = new KeyEvent( event.getAction(), KeyEvent.KEYCODE_VOLUME_UP );        		
        		new ConvertThread(tempEvent).start();
        		
        		return true;
        	}else if( keyCode == KeyEvent.KEYCODE_MIR ){
        		//Log.d(TAG,"..........mir.......");
        		if( bWait ){
        			//Log.d(TAG,"..........enter......");
        			KeyEvent tempEvent = new KeyEvent( event.getAction(), KeyEvent.KEYCODE_BACK );        		
            		new ConvertThread(tempEvent).start();
            		
            		return true;
        		}
        		
        	}else if( keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ){
        		return true;
        	}
    	}
    	
    	return super.onKeyUp(keyCode, event);
	}

	public void retry()
	{
		if(timer_flag != null) { 
			timer_flag.cancel();
			timer_flag = null;
		}
		curState = WFD_STATE_INIT;
		String str = getString(R.string.tcl_miracast_tips1);
		bWait = true;
		updateString(str);
		resume();				
	}

	public void refreshUI(){
		if(timerUI_flag != null ){
			timerUI_flag.cancel();
			timerUI_flag = null;
		}
		resumeUI();
	}
	
	public String GetPIN(){
		int r = 0;
		int snum = 0;
		int checksnum = 0;
		String pin = "";
		
		for(int i=0; i<7; i++){
			r = (int)( Math.random() * 10 );
			snum = snum * 10 + r;
			pin += r;
		}
		
		checksnum = ComputeCheckSum( snum );
		return pin+checksnum;
	}
	
	int ComputeCheckSum( int PIN ){
		long acccum = 0;
		PIN *= 10;
		acccum += 3 * ( ( PIN / 10000000 ) % 10 );
		acccum += 1 * ( ( PIN / 1000000 ) % 10 );
		acccum += 3 * ( ( PIN / 100000 ) % 10 );
		acccum += 1 * ( ( PIN / 10000 ) % 10 );
		acccum += 3 * ( ( PIN / 1000 ) % 10 );
		acccum += 1 * ( ( PIN / 100 ) % 10 );
		acccum += 3 * ( ( PIN / 10 ) % 10 );
		int digit = (int)( acccum % 10 );
		return ( ( 10 - digit ) % 10 );
	}
	
	public void updateString(String str) {
		mWpsTextView.setText(str);
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
}
