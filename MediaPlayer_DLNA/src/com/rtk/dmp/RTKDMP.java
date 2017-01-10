package com.rtk.dmp;

import java.util.Timer;
import java.util.TimerTask;

import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;

public class RTKDMP extends Activity implements Observer{

	private final String TAG = "RTKDMP"; 
    public Activity mContext = this;
    
    /*************************************//////////ImageButton    
    
    private Timer timer = null;
    private TimerTask task_back_key_click_delay = null;
    
    private final int HIDE_POPUP_MESSAGE = 0;
    private Handler handler;
    
    private PopupMessage msg_hint = null;
    
    private boolean isRight2Left = false;
    private int mBackKeyClickNum =0;
    private ImageButton btn_photo;
    private ImageButton btn_audio;
    private ImageButton btn_video;
    
    private MediaApplication mediaApp;
    
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_browser);
        intiLayout();
        msg_hint = new PopupMessage(RTKDMP.this);
        mediaApp = (MediaApplication) getApplication();
        mediaApp.addObserver(this);
        timer = new Timer(true);
        DLNADataProvider.Init();
        handler = new Handler(){
        	@Override
    		public void handleMessage(Message msg) {
        		switch(msg.what){
        		case HIDE_POPUP_MESSAGE:
					if(msg_hint!=null && msg_hint.isShowing()){
						msg_hint.dismiss();
						msg_hint = null;
					}
        		break;
        		}
        	}
        };

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
	
	
	private void intiLayout(){
		btn_photo = (ImageButton) findViewById(R.id.mb_photo_img);
		btn_photo.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				ComponentName componetName = new ComponentName("com.rtk.dmp", 
						"com.rtk.dmp.DMSListActivity");
				Intent intent= new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("source", "photo");
				intent.putExtras(bundle);
				intent.setComponent(componetName);
				startActivity(intent);
			}			

		});
		
		btn_audio = (ImageButton) findViewById(R.id.mb_music_img);
		btn_audio.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				ComponentName componetName = new ComponentName("com.rtk.dmp",
						"com.rtk.dmp.DMSListActivity");
				Intent intent= new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("source", "audio");
				intent.putExtras(bundle);
				intent.setComponent(componetName);
				startActivity(intent);
			}			

		});
		
		
		btn_video = (ImageButton) findViewById(R.id.mb_videos_img);
		btn_video.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				ComponentName componetName = new ComponentName("com.rtk.dmp",
						"com.rtk.dmp.DMSListActivity");
				Intent intent= new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("source", "video");
				intent.putExtras(bundle);
				intent.setComponent(componetName);
				startActivity(intent);
			}			

		});
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		DLNADataProvider.DeInit();
		mediaApp.deleteObserver(this);
	}		
		
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stubGridViewActivity
		/*if (event.getAction() == KeyEvent.ACTION_DOWN)
    	{
    		
        	 if(keyCode == KeyEvent.KEYCODE_BACK)
			{
				mBackKeyClickNum++;
				if(mBackKeyClickNum == 1)
				{
					msg_hint.setMessage(mContext.getResources().getString(R.string.msg_exit));
        			msg_hint.show();
        			handler.sendEmptyMessageDelayed(HIDE_POPUP_MESSAGE, TimerDelay.delay_4s);
					
					if(task_back_key_click_delay != null)
					{
						task_back_key_click_delay.cancel();
						task_back_key_click_delay = null;
					}
					task_back_key_click_delay = new TimerTask(){

						@Override
						public void run() {
							mBackKeyClickNum = 0;
							Log.e(TAG, "mBackKeyClickNum = 0");
						}
						
					};
					timer.schedule(task_back_key_click_delay, TimerDelay.delay_8s);
					
					return true;
				}else if(mBackKeyClickNum == 2)
				{

					if(timer!=null)
					{
						timer.cancel();
						timer = null;
					}
					
					if(msg_hint!=null && msg_hint.isShowing()){
						msg_hint.dismiss();
						msg_hint = null;
					}
					//goToLauncher();
					this.finish();
				}
			}
   		}	*/
		return super.onKeyDown(keyCode, event);
	}			
	private void goToLauncher(){
		Intent it =new Intent();
		it.setAction("android.intent.action.MAIN");
		it.addCategory("android.intent.category.HOME");
		it.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(it);
	}
	
	@Override
    public synchronized void update(Observable o, Object arg) {
		Log.e("audiobrowser", "update");
		ObserverContent content = (ObserverContent)arg;
		String act = content.getAction();
		if(act.equals(ObserverContent.EXIT_APP)){
			finish();
		}
	}
}
