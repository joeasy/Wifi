package com.rtk.dmp;

import java.util.ArrayList;
import java.util.Date;
import android.app.Activity;
import android.app.TvManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.Metadata;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ProgressBar;
import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.Utils.AsyncImageLoader;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.DensityUtil;
import com.realtek.Utils.FileUtils;
import com.realtek.Utils.AsyncImageLoader.ImageInfoCallback;

public class MusicActivity extends Activity {

	private Toast toast;
	private int index;
	private ArrayList<DLNAFileInfo> musicList;
	private ArrayList<String> anywhereList;

	private int sta_play = 0;
	private int sta_pause = 1;
	private ProgressBar loading;
	private SeekBar musicBar;
	private ImageView imgIcon;
	private TextView time_now;
	private TextView time_total;
	private TextView music_title;
	private TextView music_artist;
	private ImageButton btn_repeat;
	private ImageButton gui_last;
	private ImageButton gui_next;
	private ImageButton gui_play;
	private ImageButton gui_rw;
	private ImageButton gui_fw;
	private ImageButton gui_back;

	private int repeatIndex = 0;
	private RepeatStatus[] repeats = { RepeatStatus.OFF, RepeatStatus.ALL,
			RepeatStatus.ONE };
	private long lasttime = 0;
	private long banner_timeout = 10000;
	private long gui_timeout = 6000;
	private long quick_timeout = 6000;
	private boolean isBanner = true;
	private boolean showGui = false;
	private RelativeLayout lay_banner = null;
	private RelativeLayout lay_gui = null;
	private PopupWindow popup;
	private PopupMessage msg_hint = null;
	private View popview = null;
	private int mSleepTimeHour = 0, mSleepTimeMin = 0;
	private ImageView imgSta;;
	private SharedPreferences mPerferences = null;
	private TvManager mTv = null;
	private MediaApplication map = null;
	private boolean firstCreate = true;
	private Intent m_intent = null;
	private boolean fromAnywhere = false;
	private int len = 0;
	private long m_duration;
	private int[] forwardDarwable = { R.drawable.dnla_music_bar_icon_ff2,
			R.drawable.dnla_music_bar_icon_ff8,
			R.drawable.dnla_music_bar_icon_ff16,
			R.drawable.dnla_music_bar_icon_ff32,
			R.drawable.dnla_music_bar_icon_ff16,
			R.drawable.dnla_music_bar_icon_ff8 };
	private int[] backDarwable = { R.drawable.dnla_music_bar_icon_rew2,
			R.drawable.dnla_music_bar_icon_rew8,
			R.drawable.dnla_music_bar_icon_rew16,
			R.drawable.dnla_music_bar_icon_rew32,
			R.drawable.dnla_music_bar_icon_rew16,
			R.drawable.dnla_music_bar_icon_rew8  };
	private ServiceReceiver receiver = new ServiceReceiver();

	private static final float banner_h = 75f;
	private static final long bannerAnimTime = 200;
	public Metadata metadata = null;
	public ArrayList<String> DMSName = new ArrayList<String>();
	private AsyncImageLoader loader;
	private ImageView btn_menu;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (MediaApplication.DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectDiskReads().detectDiskWrites().detectNetwork() //
					.penaltyLog() //
					.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects() //
					.penaltyLog() //
					.penaltyDeath().build());
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.musicplayer);
		msg_hint = new PopupMessage(this, 678, 226);
		rootView =  (View) findViewById(R.id.main);
        //rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		setNavVisibility(true);
		initServiceMsg();
		map = (MediaApplication) getApplication();
		loader = AsyncImageLoader.getInstance();
		m_intent = getIntent();
		initGui();
		musicBar = (SeekBar) findViewById(R.id.playBar);
		musicBar.setOnSeekBarChangeListener(new SeekBarLisener());
		loading = (ProgressBar)findViewById(R.id.loading);
		loading.setVisibility(View.INVISIBLE);
		time_now = (TextView) findViewById(R.id.timeNow);
		time_total = (TextView) findViewById(R.id.timeTotal);
		time_now.setText("00:00:00");
		time_total.setText("00:00:00");
		music_title = (TextView) findViewById(R.id.musicTitle);
		music_artist = (TextView) findViewById(R.id.musicArtist);
		btn_repeat = (ImageButton) findViewById(R.id.btn_repeat);
		btn_repeat.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				repeatIndex++;
				repeatIndex %= 3;
				new Thread(new Runnable() {
					@Override
					public void run() {
						Editor editor = mPerferences.edit();//
						editor.putInt("repeatIndex_audio", repeatIndex);
						editor.commit();
					}
				}).start();
				setRepeatIcon(repeats[repeatIndex]);
				execAction(SET_REPEAT,repeats[repeatIndex].name());
			}			
		});
		mPerferences = PreferenceManager.getDefaultSharedPreferences(this);
		getRepeat();
		setRepeatIcon(repeats[repeatIndex]);
		lay_banner = (RelativeLayout) findViewById(R.id.lay_banner);
		lay_gui = (RelativeLayout) findViewById(R.id.lay_gui);	
		handlerTask.postDelayed(task_play2_runnable, banner_timeout);
		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		popview = layoutInflater.inflate(R.layout.quick_menu, null);
		popview.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					if (keyCode == KeyEvent.KEYCODE_Q || keyCode == 227) {
						popup.dismiss();
						return true;
					}
				}
				return false;
			}
		});
		setRepeatIcon(repeats[repeatIndex]);
		imgSta = (ImageView) findViewById(R.id.imgSta);
		imgIcon = (ImageView) findViewById(R.id.imgIcon);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		quickAutoQuit();
		initMenu();
		setGestureDetector(onGestureListener);
	}

	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (getIntent().getBooleanExtra("isanywhere", false)) {
			fromAnywhere = true;
			len = getIntent().getIntExtra("len", 0);
			if (len < 1) {
				finish();
			}
		}
		if (getIntent().getIntExtra("initPos", -1) == -1) {
			popMsg("playlist is null");
			finish();
		} else {
			startFromIntent(intent);
		}
	}

	private int initPos = 0;
	private void startFromIntent(Intent intent) {
		initPos = intent.getIntExtra("initPos", -1);
		if(initPos<0)
		{
			finish();
			return;
		}
		String[] attr = intent.getStringArrayExtra("filelist");
		if (fromAnywhere) {
			anywhereList = new ArrayList<String>();
			for (String str : attr) {
				anywhereList.add(str);
			}
		} else {
				musicList = map.getPlayList();
		}
		index = initPos;
		Intent it = new Intent(MusicActivity.this,MusicService.class);
		it.setAction(INIT_ACTION);
		it.putExtra("index", index);
		it.putExtra("firstindex", index);
		it.putExtra("serverName", intent.getStringExtra("serverName"));
		startService(it);
		musicBar.setProgress(0);
		execAction(PLAY_ACTION, "");
	}

	public void popMsg(String msg) {
		if (toast == null) {
			toast = Toast.makeText(getApplicationContext(), msg,
					Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
		} else
			toast.setText(msg);
		toast.show();
	}
	private final int MSG_IMAGE_FACE = 5;
	private final int MSG_RESET_FWD = 6;
	private final int MSG_BANNER = 7;
	private final int MSG_SPECTRUM = 8;
	private final int MSG_PLAY = 10;
	private final int MSG_PAUSE = 11;
	private final int UPDATE_SPECTRUM = 12;
	private final int MSG_NEXT = 13;
	private final int MSG_LAST = 14;
	private final int MSG_GUI_SHOW = 16;
	private final int MSG_GUI_HIDE = 17;
	private final int MSG_SET_REPEAT = 19;
	private final int MSG_QUICK_HIDE = 20;
	private final int MSG_QUICKMSG_HIDE = 21;
	private final int MSG_REFRESH_TIMER = 22;
	
	private final String CMD_MSG_TOTAL_TIME = "CMD_MSG_TOTAL_TIME";
	private final String CMD_MSG_PROCESS_TIME = "CMD_MSG_PROCESS_TIME";
	private final String CMD_MSG_RESET = "CMD_MSG_RESET";
	private final String CMD_MSG_REFRESH = "CMD_MSG_REFRESH";
	private final String CMD_MSG_INFO_UI = "CMD_MSG_INFO_UI";
	private final String CMD_MSG_RESET_FWD = "CMD_MSG_RESET_FWD";
	private final String CMD_MSG_RETURN = "CMD_MSG_RETURN";
	private final String CMD_MSG_PLAY = "CMD_MSG_PLAY";
	private final String CMD_MSG_PAUSE = "CMD_MSG_PAUSE";
	private final String CMD_MSG_NEXT = "CMD_MSG_NEXT";
	private final String CMD_MSG_LAST = "CMD_MSG_LAST";
	private final String CMD_MSG_GO_BACK = "CMD_MSG_GO_BACK";
	private final String CMD_MSG_SHOWMESSAGEHINT_PROTOCAL = "CMD_MSG_SHOWMESSAGEHINT_PROTOCAL";
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_IMAGE_FACE: {
				Bitmap bitmap = msg.getData().getParcelable("image");
				if (bitmap!=null) {
					imgIcon.setImageBitmap(bitmap);
				}

			}
				break;
			case MSG_BANNER: {
				if (!isBanner) {
					isBanner = true;
					lay_banner.setVisibility(View.VISIBLE);
					animateShowBanner();
				}

			}
				break;

			case MSG_SPECTRUM: {
				//show play info
				/*if (isBanner) {
					isBanner = false;
					animateHideBanner();
				}*/

			}
				break;
			case MSG_GUI_SHOW: {
				if (!showGui) {
					showGui = true;
					lay_gui.setVisibility(View.VISIBLE);
					gui_play.requestFocus();
				}

			}
				break;

			case MSG_GUI_HIDE: {
				if (showGui) {
					showGui = false;
					lay_gui.setVisibility(View.GONE);
					setFullScreen();
				}

			}
				break;
			case MSG_SET_REPEAT: {
				setRepeatIcon(repeats[repeatIndex]);
			}
				break;
			case MSG_QUICK_HIDE: {
				if(quickmenu!=null)
				quickmenu.dismiss();
			}
			break;
			case MSG_QUICKMSG_HIDE: {
				msg_notavaible.dismiss();
			}
				break;
			case MSG_REFRESH_TIMER:{					
				quickmenuAdapter.notifyDataSetChanged();
			}
				break;
			case MSG_NEXT: 
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.status_skipnext));
				break;
			case MSG_LAST: 
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.status_skipbefore));
				break;
			}
		}
	};


	public void onPause() {
		super.onPause();
		mActivityPauseFlag = 1;
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(SET_BK_PLAY);
		intent.putExtra("bkplay", true);
		startService(intent);
	}

	public void onStop() {
		if(msg_hint != null) {
			msg_hint.dismiss();
		}
		super.onStop();
	}

	public void onResume() {
		super.onResume();
		if (onbackPlay)
			onbackPlay = false;
		else {
			IntentFilter filter = new IntentFilter(
					"com.rtk.dmp.PlayService");
			registerReceiver(receiver, filter);
			if (mTv == null) {
				mTv = (TvManager) getSystemService("tv");
			}
			if (firstCreate) {
				if(!m_intent.getBooleanExtra("fromservice", false))
					startFromIntent(m_intent);
				else{
					musicList = map.getPlayList();
					Intent intent = new Intent(MusicActivity.this,MusicService.class);
					intent.setAction(UPDATE_ACTION);
					startService(intent);
				}
				firstCreate = false;
			}
		}
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(SET_BK_PLAY);
		intent.putExtra("bkplay", false);
		startService(intent);
		showGui();
		new Thread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(mActivityPauseFlag != 1)
				{

					int mins = getSleepTimeValue();
					mSleepTimeHour = mins / 60;
					mSleepTimeMin = mins % 60;
					
					if(quickmenu.isShowing())
					{
						handler.sendEmptyMessage(MSG_REFRESH_TIMER);
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
        	
        }).start();
		quickmenuAdapter.notifyDataSetChanged();
		getInitTimer();
		if(quickmenu != null && quickmenu.isShowing()){
			quickAutoQuit();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(quickmenu !=null)
		{
			quickmenu.dismiss();
			quickmenu =null;
		}
		if(loader!=null)
			AsyncImageLoader.opQueue(0, null);
		unregisterReceiver(serviceReceiver);
		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
		}
		isBanner = true;
		delTimer();
	}

	private void delTimer() {
		handlerTask.removeCallbacks(task_play_runnable);
		handlerTask.removeCallbacks(task_play2_runnable);
		handlerTask.removeCallbacks(task_gui_runnable);
		handlerTask.removeCallbacks(task_gui2_runnable);
	}

	private void onInfo() {
		handlerTask.removeCallbacks(task_play_runnable);
		handlerTask.removeCallbacks(task_play2_runnable);
		if (isBanner == false) {
			banner_timeout = 6000;
			handlerTask.postDelayed(task_play_runnable, 0);
			handlerTask.postDelayed(task_play2_runnable, banner_timeout);
		} else {
			handlerTask.postDelayed(task_play2_runnable, 0);
		}
	}

	private void onGui() {
		handlerTask.removeCallbacks(task_gui_runnable);
		handlerTask.removeCallbacks(task_gui2_runnable);
		if (showGui == false) {
			handlerTask.postDelayed(task_gui_runnable, 0);
			handlerTask.postDelayed(task_gui2_runnable, gui_timeout);
		} else {
			handlerTask.postDelayed(task_gui2_runnable, 0);
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent m) {

		if (m.getAction() == KeyEvent.ACTION_UP) {
			switch (m.getKeyCode()) {
			case KeyEvent.KEYCODE_INFO:
			case KeyEvent.KEYCODE_I: // [i+]
				onInfo();
				return true;
			case 251: // the key in the left of sleep key
			case KeyEvent.KEYCODE_G: // [tsb clickable stick]
				onGui();
				return true;
			case 235: // for L4300 KeyEvent.KEYCODE_NEXT:
			case KeyEvent.KEYCODE_N: // >>|
				if (forwardStatus != ForwardStatus.NONE) {
					cancelForward();
				}
				onNext();
				return true;
			case 234: // for L4300 KeyEvent.KEYCODE_PREVIOUS:
			case KeyEvent.KEYCODE_P: // |<<
				if (forwardStatus != ForwardStatus.NONE) {
					cancelForward();
				}
				onLast();
				return true;
			case 229: // for L4300 KeyEvent.KEYCODE_ZOOM
			case 255: // AUDIO KEY in realteck RCU
			case KeyEvent.KEYCODE_F: // >>]
				//onForward();
				return true;
			case 228: // for L4300 KeyEvent.KEYCODE_HOLD:
			case 256: // VIDEO KEY in realtek RCU
			case KeyEvent.KEYCODE_R: // [<<
				//onRewind();
				return true;
			case 227: // for L4300 KeyEvent.KEYCODE_QUICK_MENU:
			case KeyEvent.KEYCODE_Q: // [QUICk]
			case 0: // PROGINFO KEY in REALTECK RCU
				return true;
			case 232: // for L4300 KeyEvent.KEYCODE_PLAY:
				if (forwardStatus != ForwardStatus.NONE) {
					onOk();
					return true;
				} else
					onlyPlay();
				return true;
			case 233: // for L4300 KeyEvent.KEYCODE_PAUSE:
			case KeyEvent.KEYCODE_S: // [Play/pause]
				if (forwardStatus != ForwardStatus.NONE) {
					onOk();
				}
				onPlay();
				return true;
			case 178: // INPUT KEY in REALTECK RCU
			case KeyEvent.KEYCODE_ESCAPE:
			case KeyEvent.KEYCODE_E: // [exit]
			{
				try {
					ComponentName componetName = new ComponentName(
							"com.rtk.dmp",// another apk name
							"com.rtk.dmp.RTKDMP" // another apk
																// activity name
					);
					Intent intent = new Intent();
					Bundle bundle = new Bundle();
					intent.putExtras(bundle);
					intent.setComponent(componetName);
					startActivity(intent);
				} catch (Exception e) {
				}
				delTimer();
				finish();
			}
				return true;
			case KeyEvent.KEYCODE_ENTER: // [QUICk]
				if (forwardStatus != ForwardStatus.NONE) {
					onOk();
					return true;
				} else
					break;
			case KeyEvent.KEYCODE_X:
				delTimer();
				finish();
				break;
			case 231: // for L4300 KeyEvent.KEYCODE_STOP:
				delTimer();
				finish();
				break;
			case KeyEvent.KEYCODE_BACK:
				delTimer();
				break;
			}

		}
		return super.dispatchKeyEvent(m);
	}

	private void onForward() {
		if (metadata != null && !metadata.getBoolean(Metadata.SEEK_AVAILABLE)) {
			Toast.makeText(getApplicationContext(),
					this.getResources().getString(R.string.msg_seek_forbidden),
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (forwardStatus != ForwardStatus.FORWARD) {
			forwardIndex = 0;
			forwardStatus = ForwardStatus.FORWARD;
		} else {
			forwardIndex++;
			forwardIndex %= 6;
		}
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(SET_FORWARD_INDEX);
		intent.putExtra("forwardIndex", forwardIndex);
		intent.putExtra("forwardStatus",forwardStatus.name());
		startService(intent);
		execAction(SET_FORWARD, "");
		showBannel(false);
		imgSta.setImageDrawable(getApplicationContext().getResources()
				.getDrawable(
						 forwardDarwable[forwardIndex]));
	}

	private void onRewind() {
		if (metadata != null && !metadata.getBoolean(Metadata.SEEK_AVAILABLE)) {
			Toast.makeText(getApplicationContext(),
					this.getResources().getString(R.string.msg_seek_forbidden),
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (forwardStatus != ForwardStatus.REWIND) {
			forwardIndex = 0;
			forwardStatus = ForwardStatus.REWIND;
		} else {
			forwardIndex++;
			forwardIndex %= 6;
		}
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(SET_FORWARD_INDEX);
		intent.putExtra("forwardIndex", forwardIndex);
		intent.putExtra("forwardStatus",forwardStatus.name());
		startService(intent);
		execAction(SET_FORWARD, "");
		showBannel(false);
		imgSta.setImageDrawable(getApplicationContext().getResources()
				.getDrawable(
						 backDarwable[forwardIndex]));
	}

	private void onOk() {
		execAction(STOP_FORWARD, "");
		handlerTask.removeCallbacks(task_play_runnable);
		handlerTask.removeCallbacks(task_play2_runnable);
		banner_timeout = 6000;
		handlerTask.postDelayed(task_play2_runnable, banner_timeout);
		imgSta.setImageDrawable(getApplicationContext().getResources()
				.getDrawable( R.drawable.dnla_music_bar_icon_play));
	}

	private void cancelForward() {
		execAction(CANCEL_FORWARD, "");
		handlerTask.removeCallbacks(task_play_runnable);
		handlerTask.removeCallbacks(task_play2_runnable);
		banner_timeout = 6000;
		handlerTask.postDelayed(task_play2_runnable, banner_timeout);
		imgSta.setImageDrawable(getApplicationContext().getResources()
				.getDrawable(R.drawable.dnla_music_bar_icon_play));
	}

	private boolean seeking = false;
	private void onLast() {
		String act = "";
			if (repeatStatus == RepeatStatus.ONE) {
				act = GO_START;
				seeking = true;
				setSeekOut();
			} else {
				if (repeatStatus == RepeatStatus.OFF && index == 0) {
					return;
				} else {
					act = PREVIOUS_ACTION;
					seeking = false;
				}
			}
		execAction(act, "");
		showBannel(true);
	}
private void setSeekOut(){
	Intent intent = new Intent(MusicActivity.this,MusicService.class);
	intent.setAction(SET_SEEK_OUT);
	intent.putExtra("seeking", seeking);
	startService(intent);
}
	
	private void onNext() {
		if (fromAnywhere) {
			if (repeatStatus == RepeatStatus.OFF && index == len - 1) {
				if (receiver != null) {
					unregisterReceiver(receiver);
					receiver = null;
				}
				finish();
				return;
			}
		} else {
			if (repeatStatus == RepeatStatus.OFF
					&& index == musicList.size() - 1) {
				if (receiver != null) {
					unregisterReceiver(receiver);
					receiver = null;
				}
				finish();
				return;
			}
		}
		String act = NEXT_ACTION;
		if (repeatStatus == RepeatStatus.ONE)
			act = GO_START;
		execAction(act, "");
		showBannel(true);
	}

	public class ServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!fromAnywhere)
				return;
			if (intent.getStringExtra("action").equals("BACK")
					|| intent.getStringExtra("action").equals("FINISH")) {
				if (receiver != null) {
					unregisterReceiver(receiver);
					receiver = null;
				}
				finish();
			}
			if (intent.getStringExtra("action").equals("PAUSE")) {
				pauseOne();
			}
			if (intent.getStringExtra("action").equals("STOP")) {
				stop();
			}
			if (intent.getStringExtra("action").equals("PLAY")) {
				play();
			}
		}
	}

	private void setRepeatIcon(RepeatStatus tag) {
		switch (tag) {
		case OFF:
			if(btn_repeat != null)
				btn_repeat.setImageDrawable(
					this.getResources().getDrawable(R.drawable.dnla_music_bar_icon_repeat_n));
			break;
		case ALL:
			if(btn_repeat != null)
				btn_repeat.setImageDrawable(
					this.getResources().getDrawable(R.drawable.dnla_music_bar_icon_repeat_all));
			break;
		case ONE:
			if(btn_repeat != null)
				btn_repeat.setImageDrawable(
					this.getResources().getDrawable(R.drawable.dnla_music_bar_icon_repeat_one));
			break;
		default:
			break;
		}
	}

	private void onPlay() {
		if (MediaApplication.DEBUG)
			Log.e("Service Action ", "Pause/start");
		execAction(PAUSE_ACTION, "");
		showBannel(true);
	}

	private void onlyPlay() {
		if (MediaApplication.DEBUG)
			Log.e("Service Action ", "only Play");
		execAction(PLAY_ONLY_ACTION, "");
		showBannel(true);
	}

	private void showBannel(boolean miss) {
		handlerTask.removeCallbacks(task_play_runnable);
		handlerTask.removeCallbacks(task_play2_runnable);
		handlerTask.postDelayed(task_play_runnable, 0);
		if (miss) {
			banner_timeout = 6000;
			handlerTask.postDelayed(task_play2_runnable, banner_timeout);
		}
	}
	 class QuickMenuAdapter extends BaseAdapter 
	 {
		 	int[] menu_name = new int[] {
					R.string.quick_menu_detail,
					R.string.quick_menu_help,
					R.string.quick_menu_exit
					};
		 	int[] visibility = new int[]{
		 			View.INVISIBLE,
		 			View.INVISIBLE,
		 			View.INVISIBLE,
		 			
		 	};
			
			private LayoutInflater mInflater;
			private Context mcontext;
			class ViewHolder {
				TextView menu_name;
				ImageView left;
				TextView menu_option;
				ImageView right;
			}
			
			public QuickMenuAdapter(Context context)
	    	{
				mInflater = LayoutInflater.from(context);
				mcontext = context;
	    	}
			public void setVisibility(int position,int isVisible)
			{
				visibility[position]=isVisible;				
			}
			
	        
	        @Override
	        public int getCount() {
	            return menu_name.length;
	        }

	        
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	        	if(position == 0){
	        		DLNAFileInfo info = musicList.get(index);
	        		convertView = mInflater.inflate(R.layout.audio_detail_row, null);
	        		TextView filename = (TextView)convertView.findViewById(R.id.filename_info);
	        		TextView artistname = (TextView)convertView.findViewById(R.id.artistname_info);
	        		TextView albumname = (TextView)convertView.findViewById(R.id.albumname_info);
	        		TextView filedate = (TextView)convertView.findViewById(R.id.date_info);
	        		String infoname = info.getFileName();
	        		String infoartist = info.getPerformer();
	        		String infoalbum = info.getAlbumName();
	        		if(infoname.equalsIgnoreCase("unknown"))
	        			filename.setText("");
	        		else
	        			filename.setText(infoname);
	        		if(infoartist.equalsIgnoreCase("unknown"))
	        			artistname.setText("");
	        		else
	        			artistname.setText(infoartist);
	        		if(infoalbum.equalsIgnoreCase("unknown"))
	        			albumname.setText("");
	        		else
	        			albumname.setText(infoalbum);
	        		filedate.setText(info.getFileDate());
	        		return convertView;
	        	}
	        	 ViewHolder holder;
	        	
	        	if (convertView == null || convertView.getTag()==null) {
	                	convertView = mInflater.inflate(R.layout.quick_list_row, null);
		        	holder = new ViewHolder();
		        	holder.menu_name = (TextView)convertView.findViewById(R.id.menu_name);
		        	holder.menu_option = (TextView)convertView.findViewById(R.id.menu_option);
		        	if(position ==0)
		        	{
		        		holder.left = (ImageView)convertView.findViewById(R.id.left_arrow);
		        		holder.right = (ImageView)convertView.findViewById(R.id.right_arrow);
		        	}
		        	convertView.setTag(holder);
	        	} 
	        	else 
	        	{
	        		holder = (ViewHolder)convertView.getTag();
	        	}
	        	
	            holder.menu_name.setText(menu_name[position]);
	            switch(position)
	            {
		            case 1:
		            {
		            	//holder.menu_option.setText("1.0");
		            	break;
		            }
		            case 2:
		            {
		            	holder.menu_option.setText("");
		            	break;
		            }
		            default:
		            	break;
	            }        	
	        	return convertView;
	        }
	        
	        @Override
	        public Object getItem(int position) {
	        	return null;
	        }
	        	
	        @Override
	        public long getItemId(int position) {
	        	return 0;
	        }
	        
	    }

	private final String PLAY_ACTION = "com.realtek.rtkmusicplayer.PLAY_ACTION";
	private final String PLAY_ONLY_ACTION = "com.realtek.rtkmusicplayer.PLAY_ONLY_ACTION";
	private final String PAUSE_ACTION = "com.realtek.rtkmusicplayer.PAUSE_ACTION";
	private final String PAUSEONE_ACTION = "com.realtek.rtkmusicplayer.PAUSEONE_ACTION";
	private final String NEXT_ACTION = "com.realtek.rtkmusicplayer.NEXT_ACTION";
	private final String PREVIOUS_ACTION = "com.realtek.rtkmusicplayer.PREVIOUS_ACTION";
	private final String STOP_ACTION = "com.realtek.rtkmusicplayer.STOP_ACTION";
	private final String SET_REPEAT = "com.realtek.rtkmusicplayer.SET_REPEAT";
	private final String SET_FORWARD = "com.realtek.rtkmusicplayer.SET_FORWARD";
	private final String STOP_FORWARD = "com.realtek.rtkmusicplayer.STOP_FORWARD";
	private final String GO_START = "com.realtek.rtkmusicplayer.GO_START";
	private final String CANCEL_FORWARD = "com.realtek.rtkmusicplayer.CANCEL_FORWARD";
	private final String INIT_ACTION = "com.realtek.rtkmusicplayer.INIT_ACTION";
	private final String SET_FORWARD_INDEX = "com.realtek.rtkmusicplayer.SET_FORWARD_INDEX";
	private final String SET_SEEK_OUT = "com.realtek.rtkmusicplayer.SET_SEEK_OUT";
	private final String SET_BK_PLAY = "com.realtek.rtkmusicplayer.SET_BK_PLAY";
	private final String SEEK_ACTION = "com.realtek.rtkmusicplayer.SEEK_ACTION";
	private final String UPDATE_ACTION = "com.realtek.rtkmusicplayer.UPDATE_ACTION";
	private ForwardStatus forwardStatus = ForwardStatus.NONE;
	private int forwardIndex = 0;
	private RepeatStatus repeatStatus = RepeatStatus.OFF;
	private int status = -1;

	// play the music
	public void play() {
		metadata = null;
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(PLAY_ACTION);
		startService(intent);
	}

	private void goBack() {
		delTimer();
		finish();
	}

	private void execAction(String action, String repeat) {

		if (action.equals(PLAY_ACTION)) {
			play();
		} else if (action.equals(PAUSE_ACTION)) {
			pause();
		} else if (action.equals(PLAY_ONLY_ACTION)) {
			playOnly();
		} else if (action.equals(PAUSEONE_ACTION)) {
			pauseOne();
		} else if (action.equals(NEXT_ACTION)) {
			next();
		} else if (action.equals(PREVIOUS_ACTION)) {
			previous();
		} else if (action.equals(STOP_ACTION)) {
			stop();
		} else if (action.equals(SET_REPEAT)) {
			repeatStatus = RepeatStatus.valueOf(repeat);
			Intent intent = new Intent(MusicActivity.this,MusicService.class);
			intent.putExtra("repeat", repeat);
			intent.setAction(SET_REPEAT);
			startService(intent);
		} else if (action.equals(SET_FORWARD)) {
			Intent intent = new Intent(MusicActivity.this,MusicService.class);
			intent.setAction(SET_FORWARD);
			startService(intent);
		} else if (action.equals(STOP_FORWARD)) {
				handler.sendEmptyMessage(MSG_RESET_FWD);
				Intent intent = new Intent(MusicActivity.this,MusicService.class);
				intent.setAction(STOP_FORWARD);
				startService(intent);
		} else if (action.equals(GO_START)) {
			goStart();
		} else if (action.equals(CANCEL_FORWARD)) {
				handler.sendEmptyMessage(MSG_RESET_FWD);
				Intent intent = new Intent(MusicActivity.this,MusicService.class);
				intent.setAction(CANCEL_FORWARD);
				startService(intent);
		}
	}

	public void pause() {
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		if (status == sta_play) {
			intent.setAction(PAUSE_ACTION);
			startService(intent);
			status = sta_pause;
			handler.sendEmptyMessage(MSG_PAUSE);
		} else {
			intent.setAction(PLAY_ONLY_ACTION);
			startService(intent);
			status = sta_play;
			handler.sendEmptyMessage(MSG_PLAY);
		}
	}

	public void pauseOne() {
		if (status == sta_play) {
			Intent intent = new Intent(MusicActivity.this,MusicService.class);
			intent.setAction(PAUSE_ACTION);
			startService(intent);
			status = sta_pause;
		}
	}

	public void playOnly() {
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		if (status == sta_pause) {
			intent.setAction(PLAY_ONLY_ACTION);
			startService(intent);
			status = sta_play;
			handler.sendEmptyMessage(MSG_PLAY);
		}
	}

	public void stop() {
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(STOP_ACTION);
		startService(intent);
	}

	public void previous() {
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(PREVIOUS_ACTION);
		intent.putExtra("seeking", seeking);
		startService(intent);
		handler.sendEmptyMessage(MSG_LAST);
	}

	//
	public void next() {
		handler.sendEmptyMessage(MSG_NEXT);
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(NEXT_ACTION);
		startService(intent);
	}

	public void goStart() {
		Intent intent = new Intent(MusicActivity.this,MusicService.class);
		intent.setAction(GO_START);
		startService(intent);
	}

	public enum RepeatStatus {
		OFF, ALL, ONE
	}

	public enum ForwardStatus {
		REWIND, FORWARD, NONE
	}
	private void initGui() {
		gui_fw = (ImageButton) findViewById(R.id.gui_fw);
		gui_fw.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				backGui();
				gui_back = gui_fw;
				LayoutParams lp = gui_fw.getLayoutParams();
				lp.height = 72;
				lp.width = 72;
				gui_fw.setLayoutParams(lp);
				onForward();
				showGui();
			}
		});

		gui_rw = (ImageButton) findViewById(R.id.gui_rw);
		gui_rw.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				backGui();
				gui_back = gui_rw;
				LayoutParams lp = gui_rw.getLayoutParams();
				lp.height = 72;
				lp.width = 72;
				gui_rw.setLayoutParams(lp);
				onRewind();
				showGui();
			}
		});

		gui_play = (ImageButton) findViewById(R.id.gui_play);
		gui_play.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				backGui();
				gui_back = gui_play;
				LayoutParams lp = gui_play.getLayoutParams();
				lp.height = 72;
				lp.width = 72;
				gui_play.setLayoutParams(lp);
				if (forwardStatus != ForwardStatus.NONE) {
					onOk();
					forwardStatus = ForwardStatus.NONE;
				} else {
					onPlay();
					showGui();
				}
			}
		});

		gui_last = (ImageButton) findViewById(R.id.gui_last);
		gui_last.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				backGui();
				gui_back = gui_last;
				LayoutParams lp = gui_last.getLayoutParams();
				lp.height = 72;
				lp.width = 72;
				gui_last.setLayoutParams(lp);
				if (forwardStatus != ForwardStatus.NONE) {
					cancelForward();
				}
				onLast();
				showGui();
			}
		});

		gui_next = (ImageButton) findViewById(R.id.gui_next);
		gui_next.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				backGui();
				gui_back = gui_next;
				LayoutParams lp = gui_next.getLayoutParams();
				lp.height = 72;
				lp.width = 72;
				gui_next.setLayoutParams(lp);
				if (forwardStatus != ForwardStatus.NONE) {
					cancelForward();
				}
				onNext();
				showGui();
			}
		});
		gui_play.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showGui();
				}
			}
		});
		gui_fw.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showGui();
				}
			}
		});
		gui_rw.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showGui();
				}
			}
		});
		gui_next.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showGui();
				}
			}
		});
		gui_last.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showGui();
				}
			}
		});
		
		btn_menu = (ImageView) findViewById(R.id.btn_menu);
		btn_menu.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onMenu();
			}
			
		});
	}

	private void backGui(){
		if(gui_back!=null){
			LayoutParams lp = gui_back.getLayoutParams();
			lp.height = 48;
			lp.width = 48;
			gui_back.setLayoutParams(lp);
		}
	}
	private void showGui() {
		handlerTask.removeCallbacks(task_gui_runnable);
		handlerTask.removeCallbacks(task_gui2_runnable);
		handlerTask.postDelayed(task_gui_runnable, 0);
		handlerTask.postDelayed(task_gui2_runnable, gui_timeout);
		quitFullScreen();
	}
	
	private void dismissGui() {
		handler.sendEmptyMessage(MSG_GUI_HIDE);
		setFullScreen();
	}

	private void animateShowBanner() {
		lay_banner.clearAnimation();
		TranslateAnimation TransAnim;
		TransAnim = new TranslateAnimation(0.0f, 0.0f, banner_h, 0.0f);
		TransAnim.setDuration(bannerAnimTime);
		lay_banner.startAnimation(TransAnim);
	}
/*
	private void animateHideBanner() {
		lay_banner.clearAnimation();
		TranslateAnimation TransAnim;
		TransAnim = new TranslateAnimation(0.0f, 0.0f, 0.0f, banner_h);
		TransAnim.setDuration(bannerAnimTime);
		TransAnim.setAnimationListener(new hiderBannerListener());
		lay_banner.startAnimation(TransAnim);
	}*/
/*
	private class hiderBannerListener implements AnimationListener {
		public void onAnimationEnd(Animation animation) {
			// TODO Auto-generated method stub
			lay_banner.clearAnimation();
			lay_banner.setVisibility(View.GONE);
		}

		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub
		}

		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub
		}

	}*/

	private Handler handlerTask = new Handler();

	private Runnable task_play_runnable = new Runnable() {

		public void run() {
			handler.sendEmptyMessage(MSG_BANNER);
		}

	};

	private Runnable task_play2_runnable = new Runnable() {

		public void run() {
			handler.sendEmptyMessage(MSG_SPECTRUM);
		}

	};

	private Runnable task_gui_runnable = new Runnable() {

		public void run() {
			handler.sendEmptyMessage(MSG_GUI_SHOW);
		}

	};

	private Runnable task_gui2_runnable = new Runnable() {

		public void run() {
			handler.sendEmptyMessage(MSG_GUI_HIDE);
		}

	};

	private boolean onbackPlay = false;// while load sound setting,shold keep on
	private void quickAutoQuit() {
		handler.removeMessages(MSG_QUICK_HIDE);
		Message msg = handler.obtainMessage(MSG_QUICK_HIDE);
		handler.sendMessageDelayed(msg, quick_timeout);
	}

	private Message_not_avaible msg_notavaible = null;

	private class Message_not_avaible extends PopupWindow {
		private Activity context;
		private RelativeLayout rp = null;
		public TextView message = null;

		LayoutInflater mInflater = null;

		Message_not_avaible(Activity mContext) {
			super(mContext);

			this.context = mContext;
			mInflater = LayoutInflater.from(context);
			rp = (RelativeLayout) mInflater.inflate(
					R.layout.message_not_available, null);
			message = (TextView) rp.findViewById(R.id.not_available);
			setContentView(rp);
		}

		public void show_msg_notavailable() {
			TextPaint paint = message.getPaint();
			int len = (int) paint.measureText((String) context.getResources()
					.getText(R.string.toast_not_available)) + 102;
			message.setText(context.getResources().getText(
					R.string.toast_not_available));
			setHeight(72);
			setWidth(len);

			this.setFocusable(true);
			this.setOutsideTouchable(true);
			this.showAtLocation(rp, Gravity.LEFT | Gravity.BOTTOM, 18, 18);

		}
	}
	
	
	private void initServiceMsg(){
		IntentFilter filter = new IntentFilter();
		filter.addAction (CMD_MSG_TOTAL_TIME );
		filter.addAction (CMD_MSG_PROCESS_TIME );
		filter.addAction (CMD_MSG_RESET );
		filter.addAction (CMD_MSG_REFRESH );
		filter.addAction (CMD_MSG_INFO_UI );
		filter.addAction (CMD_MSG_RESET_FWD );
		filter.addAction (CMD_MSG_RETURN );
		filter.addAction (CMD_MSG_PLAY );
		filter.addAction (CMD_MSG_PAUSE );
		filter.addAction (CMD_MSG_NEXT );
		filter.addAction (CMD_MSG_LAST );
		filter.addAction (CMD_MSG_GO_BACK);
		filter.addAction(CMD_MSG_SHOWMESSAGEHINT_PROTOCAL);
		registerReceiver(serviceReceiver, filter);
	}
	
	private void setImageFace(){
		if(musicList.get(index).isHasImage())
		{
			String url = musicList.get(index).getFilePath();
			AsyncImageLoader.opQueue(0, null);
				Bitmap cachedInt = loader.loadImage(url, index,
						new ImageInfoCallback() {
							public void infoLoaded(Bitmap img, String url,
									int pos,boolean cancel) {
								try {
									if(cancel)
										return;
									if (img == null ) {
										if(musicList == null && musicList.size() <= pos ||
												!musicList.get(pos).getFilePath().equals(url))
											return;
										musicList.get(pos).setHasImage(false);
										return;
									}
									else{
										Message msg = handler.obtainMessage(MSG_IMAGE_FACE);
										Bundle bun = new Bundle();
										bun.putParcelable("image", img);
										msg.setData(bun);
										handler.sendMessage(msg);
									}
								} catch (Exception e) {
									if (MediaApplication.DEBUG)
										Log.e("reload", "" + e.getMessage());
								}
							}
						});
				if (cachedInt != null) {
					Message msg = handler.obtainMessage(MSG_IMAGE_FACE);
					Bundle bun = new Bundle();
					bun.putParcelable("image", cachedInt);
					msg.setData(bun);
					handler.sendMessage(msg);
				}
			}
	}
	protected BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(CMD_MSG_TOTAL_TIME)){
				loading.setVisibility(View.INVISIBLE);
				m_duration = intent.getIntExtra("totaltime", 0);
				musicBar.setMax(intent.getIntExtra("totaltime", 0));
				musicBar.setProgress(intent.getIntExtra("currentTime", 0));
				time_total.setText( Util.toTime(m_duration));
				if(forwardStatus == ForwardStatus.NONE)
					imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable( R.drawable.dnla_music_bar_icon_play));
				gui_play.setImageDrawable(getApplicationContext()
						.getResources().getDrawable(
								R.drawable.gui_pause));
				status = sta_play;
			} else if (action.equals(CMD_MSG_PROCESS_TIME)){
				status = sta_play;
			int timePosition =  intent.getIntExtra("currentTime", 0);
			int tmpDuration = intent.getIntExtra("duration", 0);
			if(tmpDuration>0)
			{
				m_duration = tmpDuration;
				musicBar.setMax(tmpDuration);
				time_total.setText( Util.toTime(m_duration));
				if(intent.getIntExtra("status", -1) == sta_play){
					if(forwardStatus == ForwardStatus.NONE)
						imgSta.setImageDrawable(getApplicationContext().getResources()
							.getDrawable( R.drawable.dnla_music_bar_icon_play));
					gui_play.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									R.drawable.gui_pause));
					status = sta_play;
				}else if(forwardStatus == ForwardStatus.NONE){
					imgSta.setImageDrawable(getApplicationContext().getResources()
							.getDrawable(R.drawable.dnla_music_bar_icon_stop));
					gui_play.setImageDrawable(getApplicationContext()
							.getResources().getDrawable(
									 R.drawable.gui_play));
					status = sta_pause;
				}
			}
			musicBar.setProgress(timePosition); //
			time_now.setText(Util.toTime(timePosition) );
			} else if (action.equals(CMD_MSG_RESET)){
				loading.setVisibility(View.VISIBLE);
				imgIcon.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.dnla_music_icon_large));
				music_title.setText("");
				music_artist.setText("");
				index = intent.getIntExtra("index", 0);
				setImageFace();
				String str = "";
					str = Integer.toString(index + 1 - map.getFileDirnum(), 10)
							+ "/" + (musicList.size() - map.getFileDirnum());
				DLNAFileInfo info = musicList.get(index);
				str = info.getFileName();
				if(!str.equalsIgnoreCase("unknown"))
					music_title.setText(str);
				String artist = info.getPerformer();
				if(!artist.equalsIgnoreCase("unknown"))
					music_artist.setText("" + artist);
				musicBar.setProgress(0);
				status =sta_pause;
			} else if (action.equals(CMD_MSG_REFRESH)){
				imgIcon.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.dnla_music_icon_large));
				music_title.setText("");
				music_artist.setText("");
				index = intent.getIntExtra("index", 0);
				setImageFace();
				String str = "";
					str = Integer.toString(index + 1 - map.getFileDirnum(), 10)
							+ "/" + (musicList.size() - map.getFileDirnum());
				DLNAFileInfo info = musicList.get(index);
				str = info.getFileName();
				if (!str.equalsIgnoreCase("unknown"))
					music_title.setText(str);
				String artist = info.getPerformer();
				if (!artist.equalsIgnoreCase("unknown"))
					music_artist.setText("" + artist);
				musicBar.setProgress(0);
				status =sta_pause;
			}else if (action.equals(CMD_MSG_INFO_UI)){
				index = intent.getIntExtra("index", 0);
				DLNAFileInfo info = musicList.get(index);
				String str = info.getFileName();
				if (!str.equalsIgnoreCase("unknown"))
					music_title.setText(str);
				String artist = info.getPerformer();
				if (!artist.equalsIgnoreCase("unknown"))
					music_artist.setText("" + artist);
			} else if (action.equals(CMD_MSG_RESET_FWD)){
				handlerTask.removeCallbacks(task_play_runnable);
				handlerTask.removeCallbacks(task_play2_runnable);
				handlerTask.postDelayed(task_play2_runnable, 4000);
			} else if (action.equals(CMD_MSG_RETURN)){

			}else if (action.equals(CMD_MSG_PLAY)){
				if(forwardStatus == ForwardStatus.NONE)
					imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable( R.drawable.dnla_music_bar_icon_play));
				gui_play.setImageDrawable(getApplicationContext()
						.getResources().getDrawable(
								R.drawable.gui_pause));
				status = sta_play;
			} else if (action.equals(CMD_MSG_PAUSE)){
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.dnla_music_bar_icon_stop));
				gui_play.setImageDrawable(getApplicationContext()
						.getResources().getDrawable(
								 R.drawable.gui_play));
				status = sta_pause;
			} else if (action.equals(CMD_MSG_NEXT)){
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable( R.drawable.dnla_music_dd_controll_skip_plus_n));
			}else if (action.equals(CMD_MSG_LAST)){
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(
								 R.drawable.dnla_music_dd_controll_skip_minus_n));
			} else if (action.equals(CMD_MSG_GO_BACK)){
				index = intent.getIntExtra("index", 0);
				goBack();
			} else if (action.equals(CMD_MSG_SHOWMESSAGEHINT_PROTOCAL)) {
				//showMessageHint(MusicActivity.this.getResources().getString(R.string.msg_playback_error));
				showMessageHint(MusicActivity.this.getResources().getString(R.string.msg_unsupport));
				loading.setVisibility(View.INVISIBLE);
				time_now.setText("00:00:00");
				time_total.setText("00:00:00");
			}
		}
	};
	
	   @Override
	   public boolean dispatchTouchEvent(MotionEvent ev) {
		   int action = ev.getAction();
		   switch (action) {
		   case MotionEvent.ACTION_UP:
			   if(!showGui){
					showGui();
			   }else{
				   int[] pos = new int[2];
				   int h,w;
				   int x,y;
				   x = (int) ev.getX();
				   y = (int) ev.getY();
				   h = lay_gui.getHeight();
				   w = lay_gui.getWidth();
				   lay_gui.getLocationOnScreen(pos);
				   if(x<pos[0] || x > pos[0]+w || y<pos[1] || y>pos[1]+h){
					   dismissGui();
				   }
			   }
			   break;
			   }
	           return super.dispatchTouchEvent(ev);
	       }
	   
	   private void setFullScreen(){
		   		//setNavVisibility(false);
		        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		   }
	   
	   private void quitFullScreen(){
		         final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		         attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		         getWindow().setAttributes(attrs);
		         getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		         //setNavVisibility(true);
		   }

private View rootView;
private QuickMenu quickmenu=null;
private QuickMenuAdapter quickmenuAdapter=null;
private ContentResolver m_ContentMgr = null;
private int mActivityPauseFlag = 0;
private void initMenu(){
    // createQuickMenu()
    quickmenuAdapter = new QuickMenuAdapter(this);
    m_ContentMgr = getApplicationContext().getContentResolver();
    quickmenu=new QuickMenu(
			this, quickmenuAdapter);
    quickmenu.setAnimationStyle(R.style.QuickAnimation);
    quickAutoQuit();
    OnItemClickListener quickmenuItemClickListener = new OnItemClickListener()
    {
    	@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
    		quickAutoQuit();
			switch(position)
			{
				case 1 : {
					ComponentName componetName = new ComponentName("com.android.emanualreader",
							"com.android.emanualreader.MainActivity");
					Intent intent = new Intent();
					intent.setComponent(componetName);
					startActivity(intent);
				}
				case 2:
				{
	    			Intent it = new Intent();
	    			it.setAction("com.rtk.dmp.clear.broadcast");
	    			sendBroadcast(it);
			    	handler.removeMessages(MSG_QUICK_HIDE);	
			    	map.exitApp();
					finish();
					break;
				}
				default:
					break;
			}
			quickmenuAdapter.notifyDataSetChanged();	
		}     	
    };
    quickmenu.AddOnItemClickListener(quickmenuItemClickListener);
}

private void onMenu() {
	if(quickmenu.isShowing() == true){
		quickmenu.dismiss();
		handler.removeMessages(MSG_QUICK_HIDE);
	}
	else
	{
		quickmenu.showAtRTop(40,90,320);
		quickAutoQuit();
	}
}

private void getInitTimer(){
	new Thread(new Runnable() {
		@Override
		public void run() {


			int mins = getSleepTimeValue();
			mSleepTimeHour = mins / 60;
			mSleepTimeMin = mins % 60;

		}
	}).start();
}

private int getSleepTimeValue()
{
	int sethour = Settings.Global.getInt(m_ContentMgr, "SetTimeHour", 0);
	int setmin = Settings.Global.getInt(m_ContentMgr, "SetTimeMinute", 0);
	int setsec = Settings.Global.getInt(m_ContentMgr, "SetTimeSecond", 0);
	int totalmin = Settings.Global.getInt(m_ContentMgr, "TotalMinute", 0);
	//Log.d("RTK_DEBUG", "SetTimeHour:" + sethour + ",SetTimeMinute:" + setmin +",SetTimeSec:" + setsec + ",TotalMinute:" + totalmin);
	Date curDate = new Date(System.currentTimeMillis()) ;
	int curhours =  curDate.getHours();
	int curminutes = curDate.getMinutes();
	int curSecs = curDate.getSeconds();
	Date setData = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate(), sethour, setmin, setsec);
	
	int diftime = 0;
	if(curDate.after(setData)&&totalmin != 0){
		diftime = totalmin - ((curhours * 60 + curminutes) - (sethour * 60 + setmin));
	}
	return diftime;
}


private class SeekBarLisener implements SeekBar.OnSeekBarChangeListener{

	@Override
	public void onProgressChanged(SeekBar arg0, int progress,
            boolean fromUser) {
		if(fromUser){
			Intent intent = new Intent(MusicActivity.this,MusicService.class);
			intent.setAction(SEEK_ACTION);
			intent.putExtra("progress", progress);
			startService(intent);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {		
	}
	
}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.musicplayer);
		rootView = (View) findViewById(R.id.main);
		//rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				//| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		setNavVisibility(true);
		initGui();
		musicBar = (SeekBar) findViewById(R.id.playBar);
		musicBar.setOnSeekBarChangeListener(new SeekBarLisener());
		time_now = (TextView) findViewById(R.id.timeNow);
		time_total = (TextView) findViewById(R.id.timeTotal);
		music_title = (TextView) findViewById(R.id.musicTitle);
		music_artist = (TextView) findViewById(R.id.musicArtist);
		btn_repeat = (ImageButton) findViewById(R.id.btn_repeat);
		btn_repeat.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				repeatIndex++;
				repeatIndex %= 3;
				new Thread(new Runnable() {
					@Override
					public void run() {
						Editor editor = mPerferences.edit();//
						editor.putInt("repeatIndex_audio", repeatIndex);
						editor.commit();
					}
				}).start();
				setRepeatIcon(repeats[repeatIndex]);
				execAction(SET_REPEAT,repeats[repeatIndex].name());
			}			
		});
		lay_banner = (RelativeLayout) findViewById(R.id.lay_banner);
		lay_gui = (RelativeLayout) findViewById(R.id.lay_gui);
		imgSta = (ImageView) findViewById(R.id.imgSta);
		imgIcon = (ImageView) findViewById(R.id.imgIcon);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent intent = new Intent(MusicActivity.this, MusicService.class);
		intent.setAction(UPDATE_ACTION);
		startService(intent);
	}
	
	private void getRepeat(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				int tmp_index = mPerferences.getInt("repeatIndex_audio", -1);
				if(tmp_index == -1)
				{
					Editor editor = mPerferences.edit();//
					editor.putInt("repeatIndex_audio", repeatIndex);
					editor.commit();
				}else{
					repeatIndex = tmp_index;
					execAction(SET_REPEAT,repeats[repeatIndex].name());
				}
				handler.sendEmptyMessage(MSG_SET_REPEAT);
			}
		}).start();	
	}
	
    void setNavVisibility(boolean visible) {

        int newVis = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!visible) {
            newVis |= View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }


        //getWindow().getDecorView().requestFocus();
        // Set the new desired visibility.
        this.getWindow().getDecorView().setSystemUiVisibility(newVis);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev); 
        return super.onTouchEvent(ev);
    }
    private void setGestureDetector(GestureDetector.OnGestureListener onGestureListener){
    	gestureDetector = new GestureDetector(getApplicationContext(),onGestureListener);
    }
    private GestureDetector gestureDetector;  
    private GestureDetector.OnGestureListener onGestureListener =   
            new GestureDetector.SimpleOnGestureListener() {  
            @Override  
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,  
                    float velocityY) {  
                float x = e2.getX() - e1.getX();  
                float y = e2.getY() - e1.getY(); 
                if(Math.abs(x)>Math.abs(y))
                	updateMusic((int)x/-100);
                else
                	updateMusic((int)y/-100);
                return true;  
            }  
        };
        
    private void updateMusic(int op){
    	if(op>0)
    		next();
    	else if(op<0)
    		previous();
    }
    
    private void showMessageHint(String text) {
		msg_hint.setMessage(text);
		msg_hint.show();
	}
}
