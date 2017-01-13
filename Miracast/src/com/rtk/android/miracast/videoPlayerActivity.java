package com.rtk.android.miracast;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnInfoListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class videoPlayerActivity extends Activity implements OnInfoListener{

    private static final String TAG = "videoPlayerActivity";
	private SurfaceView sView = null;
	public MediaPlayer mPlayer = null;
	public int flag = 0;
	public String url = null;
	private final IntentFilter intentFilter = new IntentFilter();
	private BroadcastReceiver receiver = null;
	private String devname;
	private String solution;
	private String videocodec = "H.264";
	private String ss = "\r\n";
	private Intent intent;
	// private boolean bisReceiveRegisted = false;
	int height = 333;
	int width = 444;
	private int[] AudioInfo = null;
	private int audio_num_stream = 0;
	private int curr_audio_stream_num = 0;
	public TextView textname;
	public TextView textresolution;
	public TextView textvideo;
	public TextView textaudio;
	public TextView texthz;
	public View layout;
	private AlertDialog dialog = null;
	Handler handler;
	
	// private AlertDialog isExit;
	// private AlertDialog alertDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,       
                      WindowManager.LayoutParams. FLAG_FULLSCREEN); 
					  
		setContentView(R.layout.video_player);
		intentFilter.addAction("MediaPlayerKill");
		//intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mPlayer = new MediaPlayer();
		sView = (SurfaceView) findViewById(R.id.surfaceView);
		sView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		sView.getHolder().setKeepScreenOn(true);
		sView.getHolder().setFixedSize(1920, 300);
		intent = getIntent();
		String str1 = intent.getStringExtra("URL");
		devname = intent.getStringExtra("device");
		Log.i(TAG, " videoPlayerActivity devname :" + devname);
		if (str1 != null) {
			Log.e(TAG, " videoPlayerActivity ;the intent data is :" + str1);
		}
		if (str1 != null && !str1.isEmpty()) {
			url = str1;
			sView.getHolder().addCallback(new SurfaceLister());
		}
		mediaDialog();
		
	}

	private void mediaDialog(){
		// TODO Auto-generated method stub
				dialog = new AlertDialog.Builder(this).create();
				dialog.setMessage(getString(R.string.media_wait));
				dialog.show();
				mPlayer.setOnInfoListener(this);
				
				handler = new Handler(){
					@Override
					public void handleMessage(Message msg) {
						// TODO Auto-generated method stub
						super.handleMessage(msg);
						switch(msg.what){
						case 1:
							try {
								Log.v("yubo_wu", "handleMessage(Message msg)");
								if(dialog != null)
								{
									dialog.setMessage(getString(R.string.connect_try));
									dialog.show();
								}
								Thread.sleep(2000);
								shutDown();
								
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						case 2:
							if(dialog != null)
							{
								dialog.dismiss();
								dialog = null;
							}
						default:
								break;
						}
					}
					
				};
								
	}

	/** register the BroadcastReceiver with the intent values to be matched */
	@Override
	public void onResume() {
		Log.i(TAG, "videoPlayerActivity->onResume()!");
		super.onResume();
		if (receiver == null) {
			receiver = new VideoPlayerBroadcastReceiver(this);
		Log.i(TAG, "new VideoPlayerBroadcastReceiver(this)");	
		}	
		registerReceiver(receiver, intentFilter);
		Log.i(TAG, "registerReceiver(receiver, intentFilter);");	
	}

	@Override
	protected void onPause() {
		Log.e(TAG, "videoPlayerActivity->onPause()!");
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.setOnPreparedListener(null);
        	mPlayer.setOnCompletionListener(null);
        	mPlayer.setOnInfoListener(null);
        	mPlayer.setDisplay(null);
			mPlayer.reset();
		}
		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
			Log.e(TAG, "unregisterReceiver(receiver);");
		}
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.e(TAG, "videoPlayerActivity->onStop()!");
		if (mPlayer != null) {
			mPlayer.stop();
		
			mPlayer.release();
			mPlayer = null;
			// System.gc();
		}
		if (receiver != null) {
			Log.e(TAG,
					"unregisterReceiver(receiver);");
			unregisterReceiver(receiver);
			receiver = null;
		}
		super.onStop();

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.e(TAG,
				"videoPlayerActivity->onDestroy()!  tiny_li");
		if (mPlayer != null) {
			mPlayer.stop();
			Log.i(TAG,
					"videoPlayerActivity->onDestroy()! mPlayer != null ->mPlayer.stop()");
			mPlayer.release();
			mPlayer = null;
		}
		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
		}
		super.onDestroy();
	}

	public class SurfaceLister implements SurfaceHolder.Callback {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO Auto-generated method stub
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			try {
				if (mPlayer != null) {
					mPlayer.reset();
					// mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					mPlayer.setPlayerType(6);
					mPlayer.setDisplay(sView.getHolder());
					mPlayer.setDataSource(url);
					mPlayer.prepare();
					mPlayer.start();
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

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}

	}

	/*
	 * private OnPreparedListener videoPreparedListener = new
	 * OnPreparedListener(){
	 * 
	 * @Override public void onPrepared(MediaPlayer mp) { // TODO Auto-generated
	 * method stub mPlayer.start();
	 * Log.i(TAG,"player onPrepared "); } };
	 */

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
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		boolean tmp = false;
		if(event.getAction()==KeyEvent.ACTION_UP){
			switch (keyCode) {

			case KeyEvent.KEYCODE_BACK:
			case KeyEvent.KEYCODE_ESCAPE: {

				AlertDialog isExit = new AlertDialog.Builder(this)
				//.setCanceledOnTouchOutside(false)
				.setMessage(getString(R.string.connect_exit))
				.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intent = new Intent("MediaPlayerKill");
						sendBroadcast(intent);
						return;
					}
				})
				.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						return;
					}
				})
			    .create();
			    Window window = isExit.getWindow();       
	            window.setGravity(Gravity.CENTER);  
				isExit.show();
				isExit.getButton(AlertDialog.BUTTON_POSITIVE).clearFocus();
				isExit.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
				break;
			}

			case KeyEvent.KEYCODE_MENU: {
				//showDialog();
				KeyEvent tempEvent = new KeyEvent( event.getAction(), KeyEvent.KEYCODE_BACK );        		
	    		new ConvertThread(tempEvent).start();
	    		
				tmp = true;
				break;
			}
			case KeyEvent.KEYCODE_DPAD_LEFT: {
	    		
	    		KeyEvent tempEvent = new KeyEvent( event.getAction(), KeyEvent.KEYCODE_VOLUME_DOWN );        		
	    		new ConvertThread(tempEvent).start();
	    		
	    		tmp = true;
	    		break;
	    	}
			case KeyEvent.KEYCODE_DPAD_RIGHT: {
	    		
	    		KeyEvent tempEvent = new KeyEvent( event.getAction(), KeyEvent.KEYCODE_VOLUME_UP );        		
	    		new ConvertThread(tempEvent).start();
	    		
	    		tmp = true;
	    		break;
	    		
	    	}
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:{
				tmp = true;
				break;
			}
			}
		}
		if (tmp)
			return tmp;
		return super.onKeyUp(keyCode, event);
	}

	private String getAudioTrackInfo() {
		AudioInfo = mPlayer.getAudioTrackInfo(-1);
		audio_num_stream = AudioInfo[1];
		curr_audio_stream_num = AudioInfo[2];
		int[] currAudioInfo = mPlayer.getAudioTrackInfo(curr_audio_stream_num);
		String mAudioType = Utility.AUDIO_TYPE_TABLE(currAudioInfo[3]);
		return mAudioType;
	}

	private void showDialog() {

		height = mPlayer.getVideoHeight();
		Log.i(TAG, "height= " + height);
		width = mPlayer.getVideoWidth();
		Log.i(TAG, "width= " + width);
		solution = String.valueOf(width) + "X" + String.valueOf(height);
		String mAudio = getAudioTrackInfo();
        String mhz="60Hz";
		AlertDialog.Builder builder;

		Context mContext = videoPlayerActivity.this;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
		String language= this.getResources().getConfiguration().locale.getLanguage();
		if(language.equals("ar")||language.equals("iw")||language.equals("fa"))
			 layout = inflater.inflate(R.layout.toshiba_dialog, null);
		else
		{
			 layout = inflater.inflate(R.layout.dialog, null);
		}

		textname = (TextView) layout.findViewById(R.id.mDev);
		textname.setText(devname);
		textresolution = (TextView) layout.findViewById(R.id.mRes);
		textresolution.setText(solution);
		texthz = (TextView) layout.findViewById(R.id.mHz);
		texthz.setText(mhz);
		textvideo = (TextView) layout.findViewById(R.id.mVid);
		textvideo.setText(videocodec);
		textaudio = (TextView) layout.findViewById(R.id.mAud);
		textaudio.setText(mAudio);

		builder = new AlertDialog.Builder(mContext);
		builder.setView(layout, 0, 0, 0, 0);
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	public void shutDown() {
		Intent intent = new Intent("MediaPlayershutdown");
		sendBroadcast(intent);
		finish();
	}
	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			Log.i(TAG, "MediaPlayer.MEDIA_INFO_BUFFERING_START");
			break;

		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			Log.i(TAG, "MediaPlayer.MEDIA_INFO_BUFFERING_END");
			break;
		default:
			if (dialog != null){
				Message msg=new Message();  
				msg.what=2;  
	            handler.sendMessage(msg);
				}
			break;
		}
		return false;
	}
	
}
