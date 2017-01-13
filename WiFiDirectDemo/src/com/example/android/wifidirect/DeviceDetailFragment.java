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

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;


/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    public WifiP2pDevice thisDevice = null;
    public WifiP2pInfo connectionInfo = null;
    public String url = null;
	public String name;
	public String groupInfo = null;
    ProgressDialog progressDialog = null;
    private int wpsInfo;
    public enum connectType {
        PBC_CONNECT,
        DISPLAY_CONNECT,
        KEYPAD_CONNECT,
        CONNECT
    }
    connectType mConnectType = connectType.CONNECT;
    
	public boolean sessionIsAviable = true;
	
    private SurfaceView sView = null;
	public MediaPlayer mPlayer = null;

	private int[] AudioInfo = null;
	private int audio_num_stream = 0;
	private int curr_audio_stream_num = 0;
	
	public static final int MEDIA_MODE_UNKOWN = 0;
	public static final int MEDIA_MODE_PLAY = 1;
	public static final int MEDIA_MODE_PAUSE = 2;
	public static final int MEDIA_MODE_STOP = 3;
	public int curMediaMode = MEDIA_MODE_UNKOWN;
	
	public static final int WFD_SESSION_ID = 7;
	
    public Handler mHandler = new Handler(){
		@Override  
		public void handleMessage(Message msg){
			switch (msg.what) {  
				case WiFiDirectActivity.WFD_URL:
						Bundle b = msg.getData();
						url = b.getString("URL");
						name = b.getString("device");
						if(name == null)
								name = "Unkown";
						if(url !=null){
								Log.i("Lynn", "Device device connectDevName is: " + name);
								((WiFiDirectActivity)getActivity()).curState = WiFiDirectActivity.WFD_STATE_CONNECTED;
								((WiFiDirectActivity)getActivity()).fragmentDetails.videoPlayerIntent();
						}
						Log.i("Lynn", "we had send the url and start the videoPlayer: " + url);
						break;
				case WiFiDirectActivity.WFD_CREATEGROUP:
						//activity.createNewGroup();
						break;
				case WiFiDirectActivity.WFD_PINANY:
						//((WiFiDirectActivity)getActivity()).pinAny();
						break;
				case WiFiDirectActivity.WFD_RESET:
						//((WiFiDirectActivity)getActivity()).resume();
						break;
				case WFD_SESSION_ID:
						Bundle b1 = msg.getData();
						String id = b1.getString("SessionId");
						if(id != null)
							setSessionId(id);
						
				default:
						break;
			} 
			super.handleMessage(msg);  
		}  
	};
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        Log.d("Lynn", "mDisplayMetrics.widthPixels: " + mDisplayMetrics.widthPixels); //1920
        Log.d("Lynn", "mDisplayMetrics.heightPixels: " + mDisplayMetrics.heightPixels); //1080
        
        LinearLayout llayout_device_detail_layout2 = (LinearLayout)mContentView.findViewById(R.id.linearLayout2);
		LinearLayout.LayoutParams llayoutparams_device_detail_layout2 = new LinearLayout.LayoutParams(
	              LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llayoutparams_device_detail_layout2 = (LinearLayout.LayoutParams)llayout_device_detail_layout2.getLayoutParams();
		llayoutparams_device_detail_layout2.height  = 360;//mDisplayMetrics.heightPixels * 1/3;
		llayout_device_detail_layout2.setLayoutParams(llayoutparams_device_detail_layout2);	
		Log.e("Lynn", "llayoutparams_device_detail_layout2: " + llayoutparams_device_detail_layout2.height);

        mContentView.findViewById(R.id.btn_connect_pbc).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	if(sessionIsAviable == false) {
            		Log.e("Lynn", "session is not aviable, do not connect");
            		return;
            	}
            	
                WifiP2pConfig config = new WifiP2pConfig();
                
                //PBC mode
				if(device != null)
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                mConnectType = connectType.PBC_CONNECT;
   
                wpsInfo = config.wps.setup;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + config.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                Log.e("Lynn", "pbc mode :" + device);
                ((DeviceActionListener) getActivity()).connect(config);
            }
        });
        
        mContentView.findViewById(R.id.btn_connect_display).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	if(sessionIsAviable == false) {
            		Log.e("Lynn", "session is not aviable, do not connect");
            		return;
            	}
            	
                WifiP2pConfig config = new WifiP2pConfig();

                //Display mode
		if(device != null)
                config.deviceAddress = device.deviceAddress;

                config.wps.setup = WpsInfo.DISPLAY;
		//fix the WFD 4.3.1 failure
		DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
		config.wps.pin=fragment.mPIN;

                mConnectType = connectType.DISPLAY_CONNECT;

                wpsInfo = config.wps.setup;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + config.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                Log.e("Lynn", "display mode :" + device);
                ((DeviceActionListener) getActivity()).connect(config);
            }
        });
        
        mContentView.findViewById(R.id.btn_connect_keypad).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	if(sessionIsAviable == false) {
            		Log.e("Lynn", "session is not aviable, do not connect");
            		return;
            	}
            	
                WifiP2pConfig config = new WifiP2pConfig();
                
                //Keypad mode
				if(device != null)
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.KEYPAD;
                mConnectType = connectType.KEYPAD_CONNECT;
                
                wpsInfo = config.wps.setup;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + config.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                Log.e("Lynn", "keypad mode :" + device);
                ((DeviceActionListener) getActivity()).connect(config);
            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });
        
        // surface view for Miracast
        sView = (SurfaceView) mContentView.findViewById(R.id.surfaceView);
		sView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		sView.getHolder().setKeepScreenOn(true);
		sView.getHolder().setFixedSize(700, 250);
		
		mContentView.findViewById(R.id.btn_play).setOnClickListener(
				new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	if(mPlayer != null) { 
                    		if(curMediaMode == MEDIA_MODE_PAUSE) {
                    			mPlayer.start();
            					curMediaMode = MEDIA_MODE_PLAY;
                    		} else if(curMediaMode == MEDIA_MODE_STOP) {
                    			
                    		}
                    	}
                    }
                });
		
		mContentView.findViewById(R.id.btn_pause).setOnClickListener(
				new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	if(mPlayer != null && curMediaMode == MEDIA_MODE_PLAY) { 
                    		curMediaMode = MEDIA_MODE_PAUSE;
                    		mPlayer.pause();
                    	}
                    }
                });
		
		mContentView.findViewById(R.id.btn_stop).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	if(mPlayer != null && (curMediaMode == MEDIA_MODE_PLAY || curMediaMode == MEDIA_MODE_PAUSE)) { 
                    		curMediaMode = MEDIA_MODE_STOP;
                    		mPlayer.stop();
                    		//((WiFiDirectActivity)getActivity()).removeGroup();
                    		
                    	}
                    }
                });
		
		mContentView.findViewById(R.id.btn_idr).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	if(mPlayer != null) { 
                    		mPlayer.seekTo(-1);                    		
                    	}
                    }
                });
		
        return mContentView;
    }

    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        //TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        //statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().findViewById(R.id.linearLayout1).setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        if(info != null && info.groupOwnerAddress != null){
        	groupInfo = "Group Owner IP - " + info.groupOwnerAddress.getHostAddress();
        	view.setText(groupInfo);
        } 

        new Thread(new Runnable(){
			@Override
			public void run(){
				Log.d("Lynn", "get session id thread run");
				try{
					Thread.sleep(8000);
				}
				catch(InterruptedException e){
					e.printStackTrace();
				}
				
				Log.d("Lynn", "sleep over");
				if(mPlayer==null){
					Log.e("Lynn", "return.......");
					return;
				}
				
				Log.d("Lynn", "mPlayer!=null");
				while(mPlayer!=null && !mPlayer.isPlaying()) {
					Log.d("Lynn", "waiting.......");
				}
				
				Log.d("Lynn", "waiting over");
		        StringBuilder buffer = new StringBuilder();
		        File file = new File("/tmp/RealtekTmp/wfdsessionid");
		        if(file.exists()){
		        	if(file.isFile()){
		        		Log.d("Lynn", "get session id");
		        		try{ 
		        			BufferedReader br = new BufferedReader (new FileReader(file)); 
		        			
		        			String content;
		        			content = br.readLine();
		        			buffer.append(content.trim());
		        			System.out.println("kavin_li session_id is " + buffer);   
		        			Message msg = new Message();
							msg.what = WFD_SESSION_ID;
							Bundle b = new Bundle();
							String str = buffer.toString();
							b.putString("SessionId", str);
							msg.setData(b);
							mHandler.sendMessage(msg);
							
		        			br.close();
		        			return;
		        		}
		        		
		        		catch(IOException ioException){
		        			System.err.println("File Error!");
		        		}
		        	}
		        }
			}
		}).start();

        //((DeviceActionListener) getActivity()).sentGroupMessage(info);

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            //new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
            //        .execute();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            //((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
            //        .getString(R.string.client_text));
        }
        
        // for DialogListener
        /*
        if(wpsInfo == WpsInfo.DISPLAY) {
        	TextView mWpsTextView = (TextView)mContentView.findViewById(R.id.wps_dialog_txt);
        	mWpsTextView.setText(R.string.wifi_wps_complete);
        }
        */
        
        // hide the connect button
        if(mConnectType == connectType.PBC_CONNECT) {
        	mContentView.findViewById(R.id.btn_connect_display).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_connect_keypad).setVisibility(View.GONE);
        }
        if(mConnectType == connectType.DISPLAY_CONNECT) {
        	mContentView.findViewById(R.id.btn_connect_pbc).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_connect_keypad).setVisibility(View.GONE);
        }
        if(mConnectType == connectType.KEYPAD_CONNECT) {
        	mContentView.findViewById(R.id.btn_connect_pbc).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_connect_display).setVisibility(View.GONE);
        }
        
        if(((WiFiDirectActivity)getActivity()).bPinAny) {
        	mContentView.findViewById(R.id.btn_connect_pbc).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_connect_display).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_connect_keypad).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
        }
        
        // getPeerAddr
        Log.i("Lynn", "onConnectionInfoAvailable :" + info);
    	this.connectionInfo = info;
    	if(info.isGroupOwner == true){
    		Log.i("Lynn", "I AM THE OWNER!");
    	}else{
    		Log.i("Lynn", "I AM NOT THE OWNER");
    	}
    	
    	WiFiDirectActivity activity = (WiFiDirectActivity)getActivity();
        DeviceListFragment fragment = (DeviceListFragment)getActivity().getFragmentManager().findFragmentById(R.id.frag_list);     
        for(WifiP2pDevice device:fragment.peers){
        	Log.i("Lynn", "status :" + device.status);
			if(device.status == WifiP2pDevice.CONNECTED || device.status==WifiP2pDevice.INVITED){
				activity.curState = activity.WFD_STATE_CONNECTING;
				activity.mCurConnectedDevice = device;
				//String str = activity.getString(R.string.wifi_wps_success);
				//activity.updateString(str);
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
				if(device.wfdInfo != null)
					Log.i("Lynn", "Connect device wfdInfo: " + device.wfdInfo.toString());   	    			
				else
					Log.i("Lynn", "Connect device wfdInfo is null");   	    			
				break;
			}
		} 
    }

    public void setSessionId(String str) {
    	TextView view = (TextView) mContentView.findViewById(R.id.device_info);
    	if(str!=null)
    		view.setText(groupInfo + ", WFD SessionId: " + str);
    	else 
    		view.setText(groupInfo);
    }
    
    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().findViewById(R.id.linearLayout1).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText("" + device);
        //view.setText(device.deviceAddress);
        //view = (TextView) mContentView.findViewById(R.id.device_info);
        
        if(device.wfdInfo.isSessionAvailable() == false) {
        	sessionIsAviable = false;
        	Log.i("Lynn", "session is not available, disconnect");
        }
        else {
        	sessionIsAviable = true;
        	Log.i("Lynn", "session is available");
        }
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect_pbc).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_connect_display).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_connect_keypad).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        //view = (TextView) mContentView.findViewById(R.id.status_text);
        //view.setText(R.string.empty);
        this.getView().findViewById(R.id.linearLayout1).setVisibility(View.GONE);
    }

    // hide buttons in listenMode 
    public void resetButtons(boolean visible) {
    	if(visible) {
    		mContentView.findViewById(R.id.btn_connect_pbc).setVisibility(View.VISIBLE);
        	mContentView.findViewById(R.id.btn_connect_display).setVisibility(View.VISIBLE);
        	mContentView.findViewById(R.id.btn_connect_keypad).setVisibility(View.VISIBLE);
        	mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
    	} else {
    		mContentView.findViewById(R.id.btn_connect_pbc).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_connect_display).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_connect_keypad).setVisibility(View.GONE);
        	mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.GONE);
    	}
    }
    
    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            //statusText.setText("Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
    
    public void videoPlayerIntent() {
		if(mPlayer == null)
			mPlayer = new MediaPlayer();
		
		Log.i("Lynn", "videoPlayerActivity devname :" + name);
		if (url != null) {
			Log.i("Lynn", "videoPlayerActivity ;the intent data is:" + url);
		}
		if (url != null && !url.isEmpty()) {
			try {
				if (mPlayer != null && curMediaMode == MEDIA_MODE_UNKOWN) {
					Log.i("Lynn", "mPlayer.start");
					mPlayer.reset();
					// mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					mPlayer.setDisplay(sView.getHolder());
					mPlayer.setDataSource(url);
					mPlayer.prepare();
					mPlayer.start();
					curMediaMode = MEDIA_MODE_PLAY;
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

/*
	private String getAudioTrackInfo() {
		AudioInfo = mPlayer.getAudioTrackInfo(-1);
		audio_num_stream = AudioInfo[1];
		curr_audio_stream_num = AudioInfo[2];
		int[] currAudioInfo = mPlayer.getAudioTrackInfo(curr_audio_stream_num);
		String mAudioType = Utility.AUDIO_TYPE_TABLE(currAudioInfo[3]);
		return mAudioType;
	}
*/
}
