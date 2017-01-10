package com.rtk.dmp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.Metadata;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnHoverListener;
import android.view.View.OnKeyListener;
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
import com.realtek.Utils.AsyncImageLoader;
import com.realtek.Utils.AsyncImageLoader.ImageInfoCallback;
import com.realtek.Utils.Tagger.ParserSingleton;
import com.realtek.Utils.Tagger.ParserSingleton.AudioInfo;

public class DmrMusicPlay extends Activity {

	private Toast toast;
	private Thread barThd = null;
	private int index;
	private int firstindex;
	private ArrayList<String> anywhereList;

	private int sta_play = 0;
	private int sta_pause = 1;

	private SeekBar musicBar;
	private TextView time_now;
	private TextView time_total;
	private TextView music_title;
	private TextView music_artist;
	private ImageButton gui_play;
	private ImageButton gui_rw;
	private ImageButton gui_fw;
	private ImageView imgIcon;
	private long lasttime = 0;
	private long banner_timeout = 10000;
	private long seek_timeout = 3000;
	private long gui_timeout = 6000;
	private long quick_timeout = 6000;
	private boolean isBanner = true;
	private boolean showGui = false;
	private RelativeLayout lay_banner = null;
	private RelativeLayout lay_gui = null;
	private PopupWindow popup;
	private View popview = null;
	private ImageView imgSta;;
	private Intent m_intent = null;
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
	private static final float banner_h = 75f;
	private static final long bannerAnimTime = 200;
	public Metadata metadata = null;
	public ArrayList<String> DMSName = new ArrayList<String>();
	private boolean finishThd = false;
	private MediaPlayer mMediaPlayer = null;
	private  String sTitle;
	private  String sArtist;
	private  String sDate;
	private  String sAlbum;
	private int[] forwardSpeed = { 2, 8, 16, 32, 16, 8 };
	private Object obj = null;
	private boolean canForward = false;
	private int hacktick = 0;
	private int playPosition =0;
	private int prePosition =0;
	private AsyncImageLoader loader;
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
		setContentView(R.layout.musicplayer_dmr);
		View v = (View) findViewById(R.id.main);
		v.setOnHoverListener(new OnHoverListener() {
			@Override
			public boolean onHover(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_HOVER_MOVE:
					showGui();
					break;
				}
				return false;
			}
		});
		obj = new Object();
		loader = AsyncImageLoader.getInstance();
		imgIcon = (ImageView) findViewById(R.id.imgIcon);
		m_intent = getIntent();
		initGui();
		musicBar = (SeekBar) findViewById(R.id.playBar);
		musicBar.setOnSeekBarChangeListener(new SeekBarLisener());
		time_now = (TextView) findViewById(R.id.timeNow);
		time_total = (TextView) findViewById(R.id.timeTotal);
		time_now.setText("00:00:00");
		time_total.setText("00:00:00");
		music_title = (TextView) findViewById(R.id.musicTitle);
		music_artist = (TextView) findViewById(R.id.musicArtist);
        initWorker();
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
		imgSta = (ImageView) findViewById(R.id.imgSta);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		quickAutoQuit();
		initMenu();
		startFromIntent(m_intent);
		initBroadCast();
	}
	private void initBroadCast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.rtk.dmr.seek.broadcast");
        intentFilter.addAction("com.android.DMRService.toplay");
        intentFilter.addAction("com.android.DMRService.pause");
        myBroadcastReciver = new MyBroadcastReciver();
        this.registerReceiver(myBroadcastReciver, intentFilter);
	}
	MyBroadcastReciver myBroadcastReciver;
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (getIntent().getBooleanExtra("isanywhere", false)) {
			//fromAnywhere = true;
			len = getIntent().getIntExtra("len", 0);
			if (len < 1) {
				finish();
			}
		}
		if (getIntent().getIntExtra("initPos", -1) == -1) {
			popMsg("playlist is null");
			finish();
		} else {
			imgIcon.setImageDrawable(getApplicationContext().getResources()
					.getDrawable(R.drawable.dnla_music_icon));
			startFromIntent(intent);
		}
	}

	private int initPos = 0;
	private  int sta_stop = 3;
	private void createPlayer() {
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
					if (anywhereList == null || len == 0) {
						goBack();
						return;
					}
						index++;
					if (index == len) {
							index--;
							goBack();
							return;
						
					}
						handler.sendEmptyMessage(MSG_RESET);
					status = sta_stop;
					//mTv.disableSpectrumData();
					play();
			}
		});  
		mMediaPlayer.setOnErrorListener(mp_error);
		mMediaPlayer.setOnInfoListener(infoListener);
	}
	
	private OnErrorListener mp_error = new OnErrorListener(){
		public boolean onError (MediaPlayer mp, int what, int extra){
			if(what == 1 || what == 0x30000000){
				mp.reset();
				handler.sendEmptyMessage(MSG_PLAY_NEXT);
			}
			return true;
		}
	};
	private OnInfoListener infoListener = new OnInfoListener(){

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.e("TAG", "infoListener" + "---what = "+what+"----");
			switch(what)
			{
			case 0x10000003: //UNKNOWN_FORMAT /* FOR AUDIO */
			{
				mp.reset();
				handler.sendEmptyMessage(MSG_PLAY_NEXT);
			}
			break;
			default:
			break;
			}
			return false;
		}
		
	};
	private void startFromIntent(Intent intent) {
		if(mMediaPlayer == null){
			createPlayer();
		}
		initPos = intent.getIntExtra("initPos", 0);
		sTitle = intent.getStringExtra("sTitle");
		sArtist = intent.getStringExtra("sArtist");
		String[] attr = intent.getStringArrayExtra("filelist");

			anywhereList =new ArrayList<String>();
			for(String str :attr)
			{
				anywhereList.add(str);
			}
			firstindex = 0;
		if (barThd == null) {
			barThd = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!finishThd) {
						if (mMediaPlayer == null) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							continue;
						}
						try {
							if (mMediaPlayer.isPlaying()) {
								handler.sendEmptyMessage(MSG_PROCESS_TIME);
							}
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
			barThd.start();
		}
		index = initPos;
		sTitle = null;
		sArtist = null;
		handler.sendEmptyMessage(MSG_INFO_UI);
		musicBar.setProgress(0);
		execAction(PLAY_ACTION,"");
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
	private  final int MSG_TOTAL_TIME = 1;
	private  final int MSG_PROCESS_TIME = 2;
	private  final int MSG_RESET = 3;
	private  final int MSG_INFO_UI = 4;
	private  final int MSG_HACK_TIME = 5;
	private  final int MSG_RESET_FWD = 6;
	private  final int MSG_BANNER = 7;
	private  final int MSG_SPECTRUM = 8;
	private  final int MSG_RETURN = 9;
	private  final int MSG_PLAY = 10;
	private  final int MSG_PAUSE = 11;
	private  final int MSG_NEXT = 13;
	private  final int MSG_LAST = 14;
	private  final int MSG_PLAY_NEXT = 15;
	private  final int MSG_GUI_SHOW = 16;
	private  final int MSG_GUI_HIDE = 17;
	private  final int MSG_SEEK_OUT = 18;
	private  final int MSG_QUICK_HIDE = 20;
	private  final int MSG_QUICKMSG_HIDE = 21;
	private  final int MSG_IMAGE_FACE = 22;
	private  final int MSG_FINISH = 23;
	private final int MSG_REFRESH_TIMER = 24;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TOTAL_TIME: {
				musicBar.setMax(mMediaPlayer.getDuration());
				m_duration = mMediaPlayer.getDuration(); 
					time_total.setText( Util.toTime(m_duration)); 
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.dnla_music_bar_icon_play));
				gui_play.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.gui_pause));
			}
				break;
			case MSG_PROCESS_TIME: {
				if(mMediaPlayer == null)
					break;
				int timePosition = mMediaPlayer
						.getCurrentPosition();
					if(hacktick >0 && (timePosition<=hacktick ||
							timePosition == prePosition || timePosition - hacktick>1000) ){
						timePosition = hacktick;
					}else{
						hacktick = 0;
						handler.removeMessages(MSG_SEEK_OUT);
					}
					if(hacktick == 0){ 
						if(seeking && timePosition >1000 )					
							break;
						else {
							seeking = false;
							handler.removeMessages(MSG_SEEK_OUT);
					}}
				musicBar.setProgress(timePosition); //
					time_now.setText(Util.toTime(timePosition));
					final Intent intent = new Intent();
					intent.setAction("com.rtk.dmr.position.broadcast");
					intent.putExtra("currentTime", timePosition);
					sendBroadcast(intent);
			}
				break;
			case MSG_RESET: {
				music_title.setText("");
				music_artist.setText("");
				sTitle = null;
				sArtist = null;
				handler.sendEmptyMessage(MSG_INFO_UI);
				musicBar.setProgress(0);
			}
				break;
			case MSG_INFO_UI: {
				setImageFace();
				if (sTitle == null || sTitle.length() == 0) {
						music_title.setText("Unknown");
				} else
					music_title.setText(sTitle);
				if (sArtist == null || sArtist.length() == 0)
					music_artist.setText("Unknown");
				else
					music_artist.setText("" + sArtist);
			}
				break;
			case MSG_HACK_TIME: {
				musicBar.setProgress(hacktick); //
				time_now.setText(Util.toTime(hacktick));

			}
				break;
			case MSG_RESET_FWD: {
				handlerTask.removeCallbacks(task_play_runnable);
				handlerTask.removeCallbacks(task_play2_runnable);
				handlerTask.postDelayed(task_play2_runnable, 4000);
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
				/*if (isBanner) {
					isBanner = false;
					animateHideBanner();
					//lay_banner.setVisibility(View.GONE);
				}*/

			}
				break;
			case MSG_RETURN: 

				break;
			case MSG_PLAY: 
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.dnla_music_bar_icon_play));
				gui_play.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.gui_pause));
				break;
			case MSG_PAUSE: 
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.dnla_music_bar_icon_stop));
				gui_play.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.gui_play));
				break;
			case MSG_NEXT: 
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.player_common_forward));
				break;
			case MSG_LAST: 
				imgSta.setImageDrawable(getApplicationContext().getResources()
						.getDrawable(R.drawable.player_common_backward));
				break;
			case MSG_PLAY_NEXT: 
					if(index == anywhereList.size()-1 )
						finish();
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
				}

			}
				break;
			case MSG_QUICKMSG_HIDE: {
				msg_notavaible.dismiss();
			}
				break;
			case MSG_IMAGE_FACE: {
				Bitmap bitmap = msg.getData().getParcelable("image");
				if (bitmap!=null) {
					imgIcon.setImageBitmap(bitmap);
				}
			}
			break;
			case MSG_FINISH: {
				delTimer();
				finish();
			}
			break;
			case MSG_REFRESH_TIMER:{					
				quickmenuAdapter.notifyDataSetChanged();
			}
			break;
			}
		}
	};

	private void setImageFace(){
			String url = anywhereList.get(index);;
			AsyncImageLoader.opQueue(0, null);
				Bitmap cachedInt = loader.loadImage(url, index,
						new ImageInfoCallback() {
							public void infoLoaded(Bitmap img, String url,
									int pos,boolean cancel) {
								try {
									if (img == null ) {
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
	public void onPause() {
		super.onPause();
		mActivityPauseFlag = 1;
	}

	public void onStop() {
		super.onStop();
	}

	public void onResume() {
		super.onResume();
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
		isBanner = true;
		finishThd = true;
		try {
			// lrcThd.join(); // wait to finish
			if (barThd != null)
				barThd.join();
			barThd = null;
			if (worker != null)
				worker.join();
			worker =null;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		if(mMediaPlayer != null)
		{
			mMediaPlayer.reset();
			mMediaPlayer = null;
		}
		delTimer();
		if(myBroadcastReciver !=null){
			unregisterReceiver(myBroadcastReciver);
			myBroadcastReciver = null;
		}
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
				onForward();
				return true;
			case 228: // for L4300 KeyEvent.KEYCODE_HOLD:
			case 256: // VIDEO KEY in realtek RCU
			case KeyEvent.KEYCODE_R: // [<<
				onRewind();
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
		Intent intent = new Intent(DmrMusicPlay.this,MusicService.class);
		intent.setAction(SET_FORWARD_INDEX);
		intent.putExtra("forwardIndex", forwardIndex);
		intent.putExtra("forwardStatus",forwardStatus.name());
		startService(intent);
		execAction(SET_FORWARD, "");
		showBannel(false);
		imgSta.setImageDrawable(getApplicationContext().getResources()
				.getDrawable(forwardDarwable[forwardIndex]));
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
		Intent intent = new Intent(DmrMusicPlay.this,MusicService.class);
		intent.setAction(SET_FORWARD_INDEX);
		intent.putExtra("forwardIndex", forwardIndex);
		intent.putExtra("forwardStatus",forwardStatus.name());
		startService(intent);
		execAction(SET_FORWARD, "");
		showBannel(false);
		imgSta.setImageDrawable(getApplicationContext().getResources()
				.getDrawable(backDarwable[forwardIndex]));
	}

	private void onOk() {
		execAction(STOP_FORWARD, "");
		handlerTask.removeCallbacks(task_play_runnable);
		handlerTask.removeCallbacks(task_play2_runnable);
		banner_timeout = 6000;
		handlerTask.postDelayed(task_play2_runnable, banner_timeout);
		imgSta.setImageDrawable(getApplicationContext().getResources()
				.getDrawable(R.drawable.dnla_music_bar_icon_play));
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
		if (System.currentTimeMillis() - lasttime <= 2000) {
				if (index == firstindex) {
					lasttime = System.currentTimeMillis();
					return;
				} else {
					act = PREVIOUS_ACTION;
					seeking = false;
				}

		} else {
			act = GO_START;
			seeking = true;
			setSeekOut();
		}
		execAction(act, "");
		lasttime = System.currentTimeMillis();

		showBannel(true);
	}
private void setSeekOut(){
	Intent intent = new Intent(DmrMusicPlay.this,MusicService.class);
	intent.setAction(SET_SEEK_OUT);
	intent.putExtra("seeking", seeking);
	startService(intent);
}
	
	private void onNext() {
			if ( index == len - 1) {
				finish();
				return;
			}
		String act = NEXT_ACTION;
		execAction(act, "");
		showBannel(true);
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


	private final String PLAY_ACTION = "com.realtek.rtkmusicplayer.PLAY_ACTION";
	private final String PLAY_ONLY_ACTION = "com.realtek.rtkmusicplayer.PLAY_ONLY_ACTION";
	private final String PAUSE_ACTION = "com.realtek.rtkmusicplayer.PAUSE_ACTION";
	private final String PAUSEONE_ACTION = "com.realtek.rtkmusicplayer.PAUSEONE_ACTION";
	private final String NEXT_ACTION = "com.realtek.rtkmusicplayer.NEXT_ACTION";
	private final String PREVIOUS_ACTION = "com.realtek.rtkmusicplayer.PREVIOUS_ACTION";
	private final String STOP_ACTION = "com.realtek.rtkmusicplayer.STOP_ACTION";
	private final String SET_FORWARD = "com.realtek.rtkmusicplayer.SET_FORWARD";
	private final String STOP_FORWARD = "com.realtek.rtkmusicplayer.STOP_FORWARD";
	private final String GO_START = "com.realtek.rtkmusicplayer.GO_START";
	private final String CANCEL_FORWARD = "com.realtek.rtkmusicplayer.CANCEL_FORWARD";
	private final String SET_FORWARD_INDEX = "com.realtek.rtkmusicplayer.SET_FORWARD_INDEX";
	private final String SET_SEEK_OUT = "com.realtek.rtkmusicplayer.SET_SEEK_OUT";

	private ForwardStatus forwardStatus = ForwardStatus.NONE;
	private int forwardIndex = 0;
	private Thread worker = null;
	private int status = -1;



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
			//next();
		} else if (action.equals(PREVIOUS_ACTION)) {
			//previous();
		} else if (action.equals(STOP_ACTION)) {
			stop();
		}  else if (action.equals(SET_FORWARD)) {
			if (status == sta_play) {
				try {
					mMediaPlayer.pause();
					status = sta_pause;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			handler.removeMessages(MSG_SEEK_OUT);
			if(hacktick<=0){
				hacktick = mMediaPlayer.getCurrentPosition();
				prePosition = hacktick;
			}
			
			synchronized (obj) {
				canForward = true;
			}
		} else if (action.equals(STOP_FORWARD)) {
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
				handler
				.sendEmptyMessage(MSG_RESET_FWD);
			if (status == sta_pause) {
				try {
					handler.removeMessages(MSG_SEEK_OUT);
					Message msg = handler.obtainMessage(MSG_SEEK_OUT);
					handler.sendMessageDelayed(msg, seek_timeout);
					mMediaPlayer.seekTo(hacktick);
					mMediaPlayer.start();
					//hacktick = 0;
					status = sta_play;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (action.equals(GO_START)) {
			goStart();
		}else if (action.equals(CANCEL_FORWARD)) {
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
				handler
				.sendEmptyMessage(MSG_RESET_FWD);
			if (status == sta_pause) {
				try {
					handler.removeMessages(MSG_SEEK_OUT);
					mMediaPlayer.start();
					hacktick = 0;
					status = sta_play;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
}

	public void pause() {
		if (status == sta_play) {
			mMediaPlayer.pause();
			status = sta_pause;
				handler
						.sendEmptyMessage(MSG_PAUSE);
		} else {
			mMediaPlayer.start();
			status = sta_play;
				handler
						.sendEmptyMessage(MSG_PLAY);
		}
	}

	public void pauseOne() {
		if (status == sta_play) {
			mMediaPlayer.pause();
			status = sta_pause;
		}
	}
	
	public void playOnly() {
		if (status == sta_pause) {
			mMediaPlayer.start();
			status = sta_play;
				handler
						.sendEmptyMessage(MSG_PLAY);
		}
	}


	public void stop() {
		if (mMediaPlayer != null) {
			mMediaPlayer = null;
		}
	}
	

	public void goStart() {
		if (status == sta_play) {
			mMediaPlayer.seekTo(0);
		}
	}
	
	private void initWorker() {
		worker = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!finishThd) {
					if (canForward) {
						int sign = 1;
						if (forwardStatus == ForwardStatus.REWIND)
							sign = -1;
						int position = forwardSpeed[forwardIndex] * 1000 * sign;
						if(MediaApplication.DEBUG)  Log.e("ggggg", "seek :" + position);
						addFakeSeek(position);
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		worker.start();
	}
	
	public void addFakeSeek(int tick) {

		hacktick +=  tick;
		if (hacktick < 0)
		{
			hacktick = 0;
			synchronized (obj) {
				canForward = false;
			}
			try {
				mMediaPlayer.seekTo(hacktick);
				mMediaPlayer.start();
				status = sta_play;
			} catch (Exception e) {
				e.printStackTrace();
			}
			forwardStatus = ForwardStatus.NONE;
				handler
				.sendEmptyMessage(MSG_RESET_FWD);
				handler
				.sendEmptyMessage(MSG_PLAY);
		}
		if (hacktick >= m_duration) {
			hacktick = 0;
			synchronized (obj) {
				canForward = false;
			}
			 {
				index++;
				int total = len;
				if (index == total)
				{

						index --;
						try {
							mMediaPlayer.seekTo((int) (m_duration -10));
							mMediaPlayer.start();
							status = sta_play;
						} catch (Exception e) {
							e.printStackTrace();
						}
						return;
				}
					handler
							.sendEmptyMessage(MSG_RESET);
				hacktick = 0;
				play();
			}
			forwardStatus = ForwardStatus.NONE;

				handler
				.sendEmptyMessage(MSG_RESET_FWD);
				handler
				.sendEmptyMessage(MSG_PLAY);
		}
			handler
					.sendEmptyMessage(MSG_HACK_TIME);
		if(MediaApplication.DEBUG)  Log.e("ggggg", "position = " + hacktick);
	}

	public enum ForwardStatus {
		REWIND, FORWARD, NONE
	}

	private void initGui() {
		gui_fw = (ImageButton) findViewById(R.id.gui_fw);
		gui_fw.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onForward();
				showGui();
			}
		});

		gui_rw = (ImageButton) findViewById(R.id.gui_rw);
		gui_rw.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onRewind();
				showGui();
			}
		});

		gui_play = (ImageButton) findViewById(R.id.gui_play);
		gui_play.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (forwardStatus != ForwardStatus.NONE) {
					onOk();
				} else {
					onPlay();
					showGui();
				}
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
		btn_menu = (ImageView) findViewById(R.id.btn_menu);
		btn_menu.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onMenu();
			}
			
		});
	}

	private void showGui() {
		handlerTask.removeCallbacks(task_gui_runnable);
		handlerTask.removeCallbacks(task_gui2_runnable);
		handlerTask.postDelayed(task_gui_runnable, 0);
		handlerTask.postDelayed(task_gui2_runnable, gui_timeout);
	}

	private void animateShowBanner() {
		lay_banner.clearAnimation();
		TranslateAnimation TransAnim;
		TransAnim = new TranslateAnimation(0.0f, 0.0f, banner_h, 0.0f);
		TransAnim.setDuration(bannerAnimTime);
		lay_banner.startAnimation(TransAnim);
	}

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
    private void clearSeekTag(){
		hacktick = 0;
		seeking = false;
		handler.removeMessages(MSG_SEEK_OUT);			
    }
	private void startInit(){
		clearSeekTag();
		Thread thd =new Thread(){
			public void run(){
				init();
			}
		};
		thd.start();
	}
	private synchronized void init() {
		synchronized (obj) {
			canForward = false;
		}
		mMediaPlayer.reset();
		try {

			String playPath = "";
				playPath = anywhereList.get(index);
			mMediaPlayer.setDataSource(playPath);
			mMediaPlayer.setOnPreparedListener(audioPreparedListener);
			mMediaPlayer.prepareAsync();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		}catch (Exception e) 
		{
			e.printStackTrace();

		}
	}

	// play the music
	public void play() {
		metadata = null;
		startInit();
		getIdinfo();
	}
	
	 private OnPreparedListener audioPreparedListener = new OnPreparedListener(){

			@Override
			public void onPrepared(MediaPlayer mp) {
				if(playPosition>0)
					mMediaPlayer.seekTo(playPosition);
				mMediaPlayer.start();
				playPosition = 0;
				//not standard
				//metadata = mMediaPlayer.getMetadata(false, true);
				status = sta_play;
					handler
							.sendEmptyMessage(MSG_TOTAL_TIME);
			}
	}; 
    private class MyBroadcastReciver extends BroadcastReceiver { 
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if(action.equals("com.rtk.dmr.seek.broadcast")) { 
    			mMediaPlayer.seekTo(intent.getIntExtra("Seekpos", 0));
    		}else if(action.equals("com.android.DMRService.toplay")) { 
    			if(!intent.getStringExtra("cmd").equals("Audio"))
    			{
    				handler.sendEmptyMessage(MSG_FINISH);
    			}
    		}else if(action.equals("com.android.DMRService.pause")) { 
    			pause();
    		}
    	}
    }
    
    //menu cc from musicactivity
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
    				case 0:
    				{
    					if(mSleepTimeMin < 50)
    					{
        					if(0 == mSleepTimeMin && 12 == mSleepTimeHour)
        					{
        						mSleepTimeHour = 0;
        					}
        					else
        					{
        						mSleepTimeMin = (mSleepTimeMin / 10+1)*10;
        					}
    					}
    					else
    					{
    						mSleepTimeMin = 0;
    						mSleepTimeHour++;
    					}
    					
    					SimpleDateFormat df_ori = new SimpleDateFormat("HH mm");
    	        		SimpleDateFormat df_des = new SimpleDateFormat("HH:mm");
    	        	    java.util.Date date_parse = null;
    	        	     try {
    	        	    	 date_parse = df_ori.parse(mSleepTimeHour+" "+mSleepTimeMin);    	
    	        		} catch (ParseException e) {
    	        			// TODO Auto-generated catch block
    	        			e.printStackTrace();
    	        		}
    	        
    	            	String timeFormat = df_des.format(date_parse);
    					
    					TextView OptionText = (TextView)arg1.findViewById(R.id.menu_option);
    					OptionText.setText(timeFormat);
    	            	
    					Intent itent = new Intent();
    					Bundle bundle = new Bundle();
    					if(0 == mSleepTimeHour && 0 == mSleepTimeMin)
        				{
        					bundle.remove("SetTSBSleepHour");
        					bundle.remove("SetTSBSleepMinute");
        					bundle.putString("TSBTimerState","Cancel");
        					bundle.putString("CallingTSBTimer", "MediaBrowserCallingTimer");
        				}
    					else
    					{
    						bundle.putInt("SetTSBSleepHour",mSleepTimeHour);
    						bundle.putInt("SetTSBSleepMinute",mSleepTimeMin);
    						bundle.putString("TSBTimerState","Set");		//Set=1, Cancel=0
    						bundle.putString("CallingTSBTimer", "MediaBrowserCallingTimer");
    					}
    					itent.setAction("com.rtk.TSBTimerSettingMESSAGE");
    					itent.putExtras((Bundle)bundle);
    					sendBroadcast(itent);
    					break;
    				}
    				case 3:
    				{
    			    	handler.removeMessages(MSG_QUICK_HIDE);	
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
    		quickmenu.showAtRTop(14,14,500);
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
    	Log.d("RTK_DEBUG", "SetTimeHour:" + sethour + ",SetTimeMinute:" + setmin +",SetTimeSec:" + setsec + ",TotalMinute:" + totalmin);
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
	private int mSleepTimeHour = 0, mSleepTimeMin = 0;
	 class QuickMenuAdapter extends BaseAdapter 
	 {
		 	int[] menu_name = new int[] {
					R.string.quick_menu_sleep,
					R.string.quick_menu_detail,
					R.string.quick_menu_version,
					R.string.quick_menu_exit
					};
		 	int[] visibility = new int[]{
		 			View.INVISIBLE,
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
	        	if(position == 1){
	        		convertView = mInflater.inflate(R.layout.audio_detail_row, null);
	        		TextView filename = (TextView)convertView.findViewById(R.id.filename_info);
	        		TextView artistname = (TextView)convertView.findViewById(R.id.artistname_info);
	        		TextView albumname = (TextView)convertView.findViewById(R.id.albumname_info);
	        		TextView filedate = (TextView)convertView.findViewById(R.id.date_info);
	        		filename.setText(sTitle);
	        		artistname.setText(sArtist);
	        		albumname.setText(sAlbum);
	        		filedate.setText(sDate);
	        		return convertView;
	        	}
	        	 ViewHolder holder;
	        	
	        	if (convertView == null || convertView.getTag()==null) {
	                	convertView = mInflater.inflate(R.layout.quick_list_row, null);
		        	holder = new ViewHolder();
		        	holder.menu_name = (TextView)convertView.findViewById(R.id.menu_name);
		        	Typeface type= Typeface.createFromFile("/system/fonts/FAUNSGLOBAL3_F_r2.TTF");
	        		holder.menu_name.setTypeface(type);
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
		            case 0:
		            {
		            	SimpleDateFormat df_ori = new SimpleDateFormat("HH mm");
		        		SimpleDateFormat df_des = new SimpleDateFormat("HH:mm");
		        	    java.util.Date date_parse = null;
		        	     try {
		        	    	 date_parse = df_ori.parse(mSleepTimeHour+" "+mSleepTimeMin);    	
		        		} catch (ParseException e) {
		        			// TODO Auto-generated catch block
		        			e.printStackTrace();
		        		}
		        
		            	String timeFormat = df_des.format(date_parse);
		            	holder.menu_option.setText(timeFormat);
		            	break;
		            }
		            case 2:
		            {
		            	holder.menu_option.setText("1.0");
		            	break;
		            }
		            case 3:
		            {
		            	holder.menu_option.setText("");
		            	break;
		            }
		            default:
		            	break;
	            }
	            
	            if(position ==0)
	            {
	            	holder.left.setVisibility(visibility[position]);
	            	holder.right.setVisibility(visibility[position]);
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
		private ImageView btn_menu;
		
		//get id3 info
		private void getIdinfo(){
			new Thread(new Runnable(){
				@Override
				public void run() {
					ParserSingleton  obj = ParserSingleton.getInstance();
					AudioInfo audio = obj.Parser(anywhereList.get(index));
					sTitle = audio.Title;
					sArtist = audio.Artist;
					sAlbum = audio.Album;
					sDate = audio.Year;
					handler.sendEmptyMessage(MSG_INFO_UI);
				}
	        	
	        }).start();
		}
		private class SeekBarLisener implements SeekBar.OnSeekBarChangeListener{

			@Override
			public void onProgressChanged(SeekBar arg0, int progress,
		            boolean fromUser) {
				if(fromUser){
					if(mMediaPlayer!=null){
						mMediaPlayer.seekTo(progress);
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {		
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {		
			}
			
		}
}
