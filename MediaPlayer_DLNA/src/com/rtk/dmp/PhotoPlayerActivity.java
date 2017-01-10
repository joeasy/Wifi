package com.rtk.dmp;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;


import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5.DeviceStatusListener;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DateStringFormat;

import com.rtk.dmp.DecodeImageState;
import com.rtk.dmp.DecoderInfo;
import com.rtk.dmp.ImagePlaylist;
import com.rtk.dmp.PictureKit;

import android.widget.LinearLayout.LayoutParams;
import android.app.Activity;
import android.app.TvManager;
import com.rtk.dmp.MediaApplication;
public class PhotoPlayerActivity extends FragmentActivity
{
	private final static int MSG_SET_REPEAT     = 19;
	private final static int MSG_SET_INTERVAL   = 20;
	private final static int MSG_REFRESH_TIMER  = 21;
	private static final String DATAFORMAT = "HH:mm EEE,dd MMM yyyy"; 

	private MediaApplication mMediaApplicationMap = null;
	private int mPhotoDirNum=0;
	boolean mIsFromAnywhere=false;
	private BroadcastReceiver mTvawReceiver = null;
	IntentFilter mTvawFilter = null;

	// add by jessie
	private boolean finishThd = false;
	private String serverName = null;
	private DLNA_DMP_1p5 dlna_DMP_1p5;
	public ArrayList<String> DMSName = new ArrayList<String>();
	private ConfirmMessage short_msg = null;
	private int ResultCodeRestartGridViewActivity = 10;
	private int ResultCodeForDMSCLosed = 20;

	private Resources m_ResourceMgr = null;
	private ContentResolver m_ContentMgr = null;
	private static final int NOT_ZOOM_MODE = 0;
	private int mRepeatMode = 0;

	private static final String TAG = "PhotoPlaybackActivity";
	private static PictureKit m_pPictureKit = null;

	private static boolean m_startdecode = false;
	private static int m_decodeImageState = DecodeImageState.STATE_NOT_DECODED;
	private static int m_decodeImageResult = DecodeImageState.STATE_DECODE_DONE;

	private int m_initPos = 0;
	private int m_currentPlayIndex = 0;
	private int m_totalCnt = 0;
	private String[] m_filePathStrArray = null;

	private Handler m_checkResultHandlerTimer = new Handler();
	private Handler m_slideShowHandlerTimer = new Handler();

	private Handler m_checkResultHandlerTimer_forOnKey = new Handler();
	private Handler m_checkFirstPictureHandler = new Handler();

	private long m_checkResultTime = 100;
	private long m_slideShowTime = 5000; // 5 second

	private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
	Handler quickUIhandler = null;
	boolean mIsFullScrean = false;

	private long mLastControlTime = 0l;
	private long mLastUnsupportShowTime = 0l;
	private long mOperationButtonsShowTime = 0l;
	private Message_not_avaible msg_notavaible = null;
	long mLastNotAvailableShowTime = 0;
	
    
    private Handler mCheckTimerHandler;
    private long mBannerShowTime = 6000; 
    private SurfaceView mPhotoPlaybackView = null;
	
    public static LinkedList<ImagePlaylist> playList = new LinkedList<ImagePlaylist>();
    
    public static final String IMAGE_CACHE_DIR = "images";
      
    private Handler mSetBannerHandler 			= null;
    private TextView banner_pic_index  			= null
    		,banner_pic_name           			= null
    		,banner_pic_timeinfo       			= null
    		,banner_pic_resolution     			= null;
    private ImageView banner_slideshow 			= null
    		,banner_repeat             			= null;
    private ImageView move_left_btm    			= null
    		          ,move_right_btm  			= null
    		          ,move_top_btm	   			= null
    		          ,move_bottom_btm 			= null;
    private RelativeLayout guide1_bar  			= null
    		              ,guide2_bar  			= null;
    private RelativeLayout player_button_list   = null;
    private ImageButton player_common_backward_button_list = null,
    					player_common_forward_button_list  = null,
    					photo_player_repeat_on_button_list = null;
    
    private TextView slide_single      = null;
    private	TextView pic_multiple	   = null; 
    private boolean mIsSlideShowModel  = false;
    private int mIsZoomModel		   = 0;
    
    private QuickMenu mQuickmenu=null;
	private QuickMenuPhotoAdapter mQuickmenuAdapter=null;
	ListView quickMenuContent = null;
	Thread mRefreshSleeperTimer = null;
	
	private String[] mIntervalTimeStatus = new String[3];
	private String[] repeats = new String[2];
	
	int [] mIntervalContent = { 5, 30, 60 };
	private int mIntervalIndex = 1;
	private int mIntervalTime = 30;
	private int mSleepTimeHour = 0, mSleepTimeMin = 0;
	
	private int mRepeatIndex  = 0;
	private PopupMessage msg_hint = null;
	
	private SharedPreferences mPerferences = null;

	private TvManager mTv;

	private int mDecodeRetryTimes = 0;

	private int mActivityPauseFlag = 0;
	private int mActivityDestroyFlag = 0;

 	private boolean isPlayerButtonListVisible = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "kelly onCreate:");
		super.onCreate(savedInstanceState);

		if (true == RTKDMPConfig.getRight2Left(getApplicationContext()))
			setContentView(R.layout.photosingleview_a);
		else
			setContentView(R.layout.photosingleview);

		m_ResourceMgr = this.getResources();
	m_ContentMgr = getApplicationContext().getContentResolver();

		msg_hint = new PopupMessage(this);

		mIntervalTimeStatus[0] = (String) m_ResourceMgr
				.getText(R.string.qm_interval_fast);
		mIntervalTimeStatus[1] = (String) m_ResourceMgr
				.getText(R.string.qm_interval_normal);
		mIntervalTimeStatus[2] = (String) m_ResourceMgr
				.getText(R.string.qm_interval_slow);

		repeats[0] = (String) m_ResourceMgr.getText(R.string.qm_repeat_off);
		repeats[1] = (String) m_ResourceMgr.getText(R.string.qm_repeat_on);

		banner_slideshow = (ImageView) findViewById(R.id.banner_slideshow);
		banner_repeat = (ImageView) findViewById(R.id.banner_repeat);

		move_left_btm = (ImageView) findViewById(R.id.move_left_btm);
		move_right_btm = (ImageView) findViewById(R.id.move_right_btm);
		move_top_btm = (ImageView) findViewById(R.id.move_top_btm);
		move_bottom_btm = (ImageView) findViewById(R.id.move_bottom_btm);
		guide1_bar = (RelativeLayout) findViewById(R.id.bg_bottom);
		guide2_bar = (RelativeLayout) findViewById(R.id.bg2_bottom);
		player_button_list = (RelativeLayout) findViewById(R.id.player_button_list);
		player_common_backward_button_list = (ImageButton) findViewById(R.id.player_common_backward_button_list);
		player_common_forward_button_list = (ImageButton) findViewById(R.id.player_common_forward_button_list);
		photo_player_repeat_on_button_list = (ImageButton) findViewById(R.id.photo_player_repeat_on_button_list);

		slide_single = (TextView) findViewById(R.id.guide_slide_single);

		pic_multiple = (TextView) findViewById(R.id.pic_multiple);

		mMediaApplicationMap = (MediaApplication) getApplication();
		// mDataProvider = map.getPhotoDataProvider();
		mPhotoPlaybackView = (SurfaceView) findViewById(R.id.m_photoPlaybackSurfaceView);

		getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

		player_common_backward_button_list
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						mOperationButtonsShowTime = (new Date(System
								.currentTimeMillis())).getTime();
						operation_left();
					}
				});
		player_common_forward_button_list
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						mOperationButtonsShowTime = (new Date(System
								.currentTimeMillis())).getTime();
						operation_right();
					}
				});
		photo_player_repeat_on_button_list
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						mOperationButtonsShowTime = (new Date(System
								.currentTimeMillis())).getTime();
						mRepeatMode = (mRepeatMode + 1) % 2;
						check_repeat_mode();
					}
				});

		mCheckTimerHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					dofullscrean();
					mLastControlTime = (new Date(System.currentTimeMillis()))
							.getTime();
					break;
				case 1:
					if (msg_hint.isShowing()) {
						msg_hint.dismiss();
					}
					break;
				case 2:
					if (msg_notavaible.isShowing()) {
						msg_notavaible.dismiss();
					}
					break;
				case 3:
					player_button_list.setVisibility(View.INVISIBLE);
					isPlayerButtonListVisible = false;
					break;
				default:
					break;
				}
				super.handleMessage(msg);
			}
		};

		mSetBannerHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				int position = msg.arg1;
				switch (msg.what) {
				case 0:
					dosetbannerexif(position);
					break;
				default:
					break;
				}

				super.handleMessage(msg);
			}
		};

		mTv = (TvManager) this.getSystemService("tv");

		move_top_btm.setImageResource(R.drawable.photo_player_up);
		Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(
				R.drawable.photo_player_up)).getBitmap();
		int bitmapWidth = bitmap.getWidth();
		int bitmapHeight = bitmap.getHeight();
		Matrix matrix = new Matrix();
		matrix.reset();
		matrix.setRotate(90);
		Bitmap rightDirection = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth,
				bitmapHeight, matrix, true);
		matrix.reset();
		matrix.setRotate(180);
		Bitmap bottomDirection = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth,
				bitmapHeight, matrix, true);
		matrix.reset();
		matrix.setRotate(-90);
		Bitmap leftDirection = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth,
				bitmapHeight, matrix, true);
		move_right_btm.setImageBitmap(rightDirection);
		move_left_btm.setImageBitmap(leftDirection);
		move_bottom_btm.setImageBitmap(bottomDirection);

		mTvawFilter = new IntentFilter();
		mTvawFilter.addAction("com.rtk.mediabrowser.PlayService");
		mTvawReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				if (intent.getStringExtra("action").equals("PAUSE")) {
					m_checkResultHandlerTimer
							.removeCallbacks(m_checkResultTimerCb);
					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);
					banner_slideshow
							.setImageResource(R.drawable.photo_play_off);
				} else if (intent.getStringExtra("action").equals("PLAY")) {
					if (m_totalCnt > 0) {
						m_checkResultHandlerTimer.postDelayed(
								m_checkResultTimerCb, m_checkResultTime);
					}
					banner_slideshow.setImageResource(R.drawable.photo_play_on);
				} else if ((intent.getStringExtra("action").equals("FINISH"))) {
					finish();
					return;
				}

			}
		};
		mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
		new Thread(new Runnable() {
			public void run() {
				long curtime = 0;
				while (!finishThd) {
					if (mActivityDestroyFlag == 1)
						break;
					curtime = (new Date(System.currentTimeMillis())).getTime();
					if (!mIsFullScrean) {
						if (curtime - mLastControlTime > mBannerShowTime) {
							Message msg = new Message();
							msg.what = 0;
							mCheckTimerHandler.sendMessage(msg);
						}
					}
					if (player_button_list.getVisibility() == View.VISIBLE) {
						if (curtime - mOperationButtonsShowTime > 6000) {
							Message msg = new Message();
							msg.what = 3;
							mCheckTimerHandler.sendMessage(msg);
						}
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

		mPerferences = PreferenceManager.getDefaultSharedPreferences(this);

		Intent intent = getIntent();
		m_initPos = intent.getIntExtra("initPos", 0);
		m_totalCnt = intent.getIntExtra("totalCnt", 0);
		serverName = intent.getStringExtra("serverName");
		mIsFromAnywhere = intent.getBooleanExtra("isanywhere", false);

		if (mIsFromAnywhere == false) {
			/*if (mPhotoList == null) {
				mPhotoList = null;
			}
			int photoListSize = mPhotoList.size();
			mPhotoDirNum = photoListSize;
			m_filePathStrArray = new String[photoListSize];
			{
				int tmpj = 0;
				if (m_totalCnt > 0) {
					// get filePathArrayStr
					for (int i = 0; i < photoListSize; i++) {
						if (mPhotoList.get(i).getmFileType() == FileFilterType.DEVICE_FILE_PHOTO) {
							m_filePathStrArray[tmpj] = mPhotoList.get(i).getPath();
							tmpj++;
						}
					}
				}
				mPhotoDirNum = m_filePathStrArray.length - tmpj;
			}*/
		} else {
			m_filePathStrArray = intent.getStringArrayExtra("filelist");
			m_totalCnt = m_filePathStrArray.length;
			mPhotoDirNum = 0;
			registerReceiver(mTvawReceiver, mTvawFilter);
		}
		// mRepeatIndex = intent.getIntExtra("repeatIndex", 0);
		// mRepeatMode = mRepeatIndex;

		if (m_initPos < 0 && m_initPos > m_totalCnt - 1) {
			m_initPos = 0;
		}

		m_currentPlayIndex = m_initPos;

		// InitQuickMenu
		mQuickmenuAdapter = new QuickMenuPhotoAdapter(this);
		mQuickmenu = new QuickMenu(this, mQuickmenuAdapter);
		mQuickmenu.setAnimationStyle(R.style.QuickAnimation);

		final ListView quickMenuContent = mQuickmenu.getListView();
		quickUIhandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {

				case MSG_SET_REPEAT: {
					mRepeatMode = mRepeatIndex;
					check_repeat_mode();
					break;
				}
				case MSG_SET_INTERVAL: {
					mIntervalTime = mIntervalContent[mIntervalIndex];
					m_slideShowTime = mIntervalTime * 1000;
					break;
				}
	            	case MSG_REFRESH_TIMER:
	            	{
	            		mQuickmenuAdapter.notifyDataSetChanged();
	            		break;
	            	}

				default:
					break;
				}

				super.handleMessage(msg);
			}

		};


		OnItemClickListener quickmenuItemClickListener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				// TODO Auto-generated method stub
				mQuickmenu.markOperation();
				switch (position) {
				case 0: {
					showInBackground = true;
					ComponentName componetName = new ComponentName(
							"com.tsb.tv", "com.tsb.tv.Tv_strategy_lite");
					Intent intent = new Intent();
					intent.setComponent(componetName);
					Bundle bundle = new Bundle();
					bundle.putInt("TVMainMenu", 4);
					bundle.putInt("PicCallFrom", 0);
					intent.putExtras(bundle);
					try {
						startActivity(intent);
					} catch (ActivityNotFoundException e) {
						Log.e("Error",
								"ActivityNotFoundException: com.tsb.tv.Tv_strategy_lite");
						Toast.makeText(getApplicationContext(),
								"Can not find the TV App!", Toast.LENGTH_SHORT)
								.show();
					}
					mQuickmenu.dismiss();
					break;
				}
				case 1: {
					mIntervalIndex++;
					mIntervalIndex %= 3;

					mIntervalTime = mIntervalContent[mIntervalIndex];
					TextView OptionText = (TextView) arg1
							.findViewById(R.id.menu_option);
					OptionText.setText(mIntervalTimeStatus[mIntervalIndex]);
					new Thread(new Runnable() {
						@Override
						public void run() {
							Editor editor = mPerferences.edit();//
							editor.putInt("intervalIndex_photo", mIntervalIndex);
							editor.commit();
						}
					}).start();
					m_slideShowTime = mIntervalTime * 1000;
					break;
				}
				case 2: {
					mRepeatIndex++;
					mRepeatIndex %= 2;
					new Thread(new Runnable() {
						@Override
						public void run() {
							Editor editor = mPerferences.edit();//
							editor.putInt("repeatIndex_photo", mRepeatIndex);
							editor.commit();
						}
					}).start();
					TextView OptionText = (TextView) arg1
							.findViewById(R.id.menu_option);
					OptionText.setText(repeats[mRepeatIndex]);
					mRepeatMode = mRepeatIndex;
					check_repeat_mode();
					break;
				}
					case 3:
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
				case 4: {

					ComponentName componetName = new ComponentName(
							"com.tsb.tv", "com.tsb.tv.Tv_strategy");
					Intent intent = new Intent();

					intent.setComponent(componetName);
					startActivity(intent);
					showInBackground = false;
					break;
				}
				case 5: {
					ComponentName componetName = new ComponentName(
							"com.android.settings",
							"com.android.settings.Settings");
					Intent intent = new Intent();
					intent.setComponent(componetName);
					startActivity(intent);
					break;
				}
				default:
					break;
				}
			}
		};
		OnItemSelectedListener quickmenuItemSelectedListener = new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long arg3) {
				// TODO Auto-generated method stub
				Log.d(TAG, "Quick Menu ListView onItemSelected");
				mQuickmenu.markOperation();
				if (mQuickmenuAdapter.LastSelectedItem_View == null)
					mQuickmenuAdapter.LastSelectedItem_View = view;
				ImageView left_arrow = (ImageView) mQuickmenuAdapter.LastSelectedItem_View
						.findViewById(R.id.left_arrow);
				ImageView right_arrow = (ImageView) mQuickmenuAdapter.LastSelectedItem_View
						.findViewById(R.id.right_arrow);
				left_arrow.setVisibility(View.INVISIBLE);
				right_arrow.setVisibility(View.INVISIBLE);

				mQuickmenuAdapter.LastSelectedItem_View = view;

				if (1 == position || 2 == position || 3 == position) {
					left_arrow = (ImageView) mQuickmenuAdapter.LastSelectedItem_View
							.findViewById(R.id.left_arrow);
					right_arrow = (ImageView) mQuickmenuAdapter.LastSelectedItem_View
							.findViewById(R.id.right_arrow);
					left_arrow.setVisibility(View.VISIBLE);
					right_arrow.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				Log.d(TAG, "Quick Menu ListView onNothingSelected");
			}
		};
		OnKeyListener quickmenuKeyClickListener = new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub

				mQuickmenu.markOperation();
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					int position = quickMenuContent.getSelectedItemPosition();
					if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
						// TODO: switch option
						switch (position) {
						case 0: {
							ComponentName componetName = new ComponentName(
									"com.tsb.tv", "com.tsb.tv.Tv_strategy_lite");
							Intent intent = new Intent();
							intent.setComponent(componetName);
							Bundle bundle = new Bundle();
							bundle.putInt("TVMainMenu", 4);
							intent.putExtras(bundle);
							startActivity(intent);
							break;
						}
						case 1: {
							mIntervalIndex++;
							mIntervalIndex %= 3;

							mIntervalTime = mIntervalContent[mIntervalIndex];
							TextView OptionText = (TextView) (quickMenuContent
									.getChildAt(position)
									.findViewById(R.id.menu_option));
							OptionText
									.setText(mIntervalTimeStatus[mIntervalIndex]);
							m_slideShowTime = mIntervalTime * 1000;
							new Thread(new Runnable() {
								@Override
								public void run() {
									Editor editor = mPerferences.edit();//
									editor.putInt("intervalIndex_photo",
											mIntervalIndex);
									editor.commit();
								}
							}).start();
							break;
						}
						case 2: {
							mRepeatIndex++;
							mRepeatIndex %= 2;
							new Thread(new Runnable() {
								@Override
								public void run() {
									Editor editor = mPerferences.edit();//
									editor.putInt("repeatIndex_photo",
											mRepeatIndex);
									editor.commit();
								}
							}).start();
							TextView OptionText = (TextView) (quickMenuContent
									.getChildAt(position)
									.findViewById(R.id.menu_option));
							OptionText.setText(repeats[mRepeatIndex]);
							mRepeatMode = mRepeatIndex;
							check_repeat_mode();
							break;
						}
		        			case 3:
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
		        				
		        				new Thread(new Runnable() {
									@Override
									public void run() {
										Editor editor = mPerferences.edit();//
										editor.putInt("timer_sleep_hour", mSleepTimeHour);
										editor.putInt("timer_sleep_min", mSleepTimeMin);
										editor.commit();
									}
								}).start();
								
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
								
								TextView OptionText = (TextView)(quickMenuContent.getChildAt(position).findViewById(R.id.menu_option));
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
		        		}

		        		return true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
						// TODO: switch option
						switch (position) {
						case 0: {

							ComponentName componetName = new ComponentName(
									"com.tsb.tv", "com.tsb.tv.Tv_strategy_lite");
							Intent intent = new Intent();
							intent.setComponent(componetName);
							Bundle bundle = new Bundle();
							bundle.putInt("TVMainMenu", 4);
							intent.putExtras(bundle);
							startActivity(intent);
							break;

						}
						case 1: {
							mIntervalIndex = mIntervalIndex - 1 < 0 ? 2
									: mIntervalIndex - 1;

							mIntervalTime = mIntervalContent[mIntervalIndex];
							TextView OptionText = (TextView) (quickMenuContent
									.getChildAt(position)
									.findViewById(R.id.menu_option));
							OptionText
									.setText(mIntervalTimeStatus[mIntervalIndex]);
							m_slideShowTime = mIntervalTime * 1000;
							new Thread(new Runnable() {
								@Override
								public void run() {
									Editor editor = mPerferences.edit();//
									editor.putInt("intervalIndex_photo",
											mIntervalIndex);
									editor.commit();
								}
							}).start();
							break;
						}
						case 2: {
							mRepeatIndex = (mRepeatIndex - 1) >= 0 ? (mRepeatIndex - 1)
									: 1;
							new Thread(new Runnable() {
								@Override
								public void run() {
									Editor editor = mPerferences.edit();//
									editor.putInt("repeatIndex_photo",
											mRepeatIndex);
									editor.commit();
								}
							}).start();
							TextView OptionText = (TextView) (quickMenuContent
									.getChildAt(position)
									.findViewById(R.id.menu_option));
							OptionText.setText(repeats[mRepeatIndex]);
							mRepeatMode = mRepeatIndex;
							check_repeat_mode();
							break;
						}
		        			case 3:
		        			{
		        				if(0 == mSleepTimeMin)
		        				{
		        					if(0 == mSleepTimeHour)
		        					{
		        						mSleepTimeHour = 12;
		        					}
		        					else
		        					{
		        						mSleepTimeHour --;
		        						mSleepTimeMin =50;
		        					}
		        				}
		        				else
		        				{
		        					mSleepTimeMin =( (mSleepTimeMin-1) / 10)*10 ;
		        				}
		        				
		        				new Thread(new Runnable() {
									@Override
									public void run() {
										Editor editor = mPerferences.edit();//
										editor.putInt("timer_sleep_hour", mSleepTimeHour);
										editor.putInt("timer_sleep_min", mSleepTimeMin);
										editor.commit();
									}
								}).start();
								
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
								
								TextView OptionText = (TextView)(quickMenuContent.getChildAt(position).findViewById(R.id.menu_option));
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
		        		}

		        		return true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
						if (position == 0) {
							quickMenuContent.setSelection(quickMenuContent
									.getCount() - 1);
						}
						return false;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						if (position == quickMenuContent.getCount() - 1) {
							quickMenuContent.setSelection(0);
						}
						return false;
					}

				}
				if (event.getAction() == KeyEvent.ACTION_UP) {
					if (keyCode == KeyEvent.KEYCODE_Q || keyCode == 227
							|| keyCode == KeyEvent.KEYCODE_BACK) // 268 present
																	// KEYCODE_QUICK_MENU
					{
						mQuickmenu.dismiss();
					} else if (keyCode == KeyEvent.KEYCODE_MENU
							|| keyCode == 220) {
						mQuickmenu.dismiss();
						if (null == msg_notavaible) {
							msg_notavaible = new Message_not_avaible(
									PhotoPlayerActivity.this);
						}

						msg_notavaible.show_msg_notavailable();

						mLastNotAvailableShowTime = (new Date(
								System.currentTimeMillis())).getTime();
						new Thread(new Runnable() {
							public void run() {
								long curtime = 0;
								while (true) {
									if (msg_notavaible.isShowing() == false)
										break;
									curtime = (new Date(System
											.currentTimeMillis())).getTime();
									if (curtime - mLastNotAvailableShowTime > 3000) {
										Message msg = new Message();
										msg.what = 2;
										mCheckTimerHandler.sendMessage(msg);
									}
									try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
						}).start();
					}
				}
				return false;
			}
		};
		mQuickmenu.AddOnItemClickListener(quickmenuItemClickListener);
		mQuickmenu.AddOnItemSelectedListener(quickmenuItemSelectedListener);
		mQuickmenu.AddOnKeyClickListener(quickmenuKeyClickListener);

		player_button_list.setVisibility(View.INVISIBLE);
		View v = (View) findViewById(R.id.main_layout);
		v.setOnHoverListener(new OnHoverListener() {
			@Override
			public boolean onHover(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_HOVER_MOVE:

					player_button_list.setVisibility(View.VISIBLE);
            			isPlayerButtonListVisible = true;
            			mOperationButtonsShowTime = (new Date(System.currentTimeMillis())).getTime();
            			player_button_list.setLayoutParams(player_button_list.getLayoutParams());
            		

					break;
				}
				return false;
			}
		});

		short_msg = new ConfirmMessage(PhotoPlayerActivity.this, 678, 226);

		DeviceStatusListener mDeviceListener = new DeviceStatusListener() {

			@Override
			public void deviceAdded(String dmsName) {
				// TODO Auto-generated method stub
			}

			@Override
			public void deviceRemoved(String dmsName) {
				// TODO Auto-generated method stub
				DMSName.add(dmsName);
			}

		};
		dlna_DMP_1p5 = new DLNA_DMP_1p5();
		dlna_DMP_1p5.setDeviceStatusListener(mDeviceListener);

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
			int len = (int) paint.measureText((String) m_ResourceMgr
					.getText(R.string.toast_not_available)) + 102;
			message.setText(m_ResourceMgr.getText(R.string.toast_not_available));
			setHeight(72);
			setWidth(len);
			message.setTextColor(Color.BLACK);
			this.setFocusable(true);
			this.setOutsideTouchable(true);
			this.showAtLocation(rp, Gravity.LEFT | Gravity.BOTTOM, 18, 18);

		}
	}

	private void dosetmoveicon() {
		if (mIsZoomModel != NOT_ZOOM_MODE) {
			if (mIsZoomModel > 0) {
				move_left_btm.setVisibility(View.VISIBLE);
				move_right_btm.setVisibility(View.VISIBLE);
				move_top_btm.setVisibility(View.VISIBLE);
				move_bottom_btm.setVisibility(View.VISIBLE);
			}
		} else {
			move_left_btm.setVisibility(View.INVISIBLE);
			move_right_btm.setVisibility(View.INVISIBLE);
			move_top_btm.setVisibility(View.INVISIBLE);
			move_bottom_btm.setVisibility(View.INVISIBLE);
		}

		RelativeLayout llayout_banner_photo = (RelativeLayout) findViewById(R.id.banner_photo);
		llayout_banner_photo.setLayoutParams(llayout_banner_photo
				.getLayoutParams());
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.v(TAG, "onNewIntent");
		m_filePathStrArray = intent.getStringArrayExtra("filelist");
		m_totalCnt = m_filePathStrArray.length;
		mPhotoDirNum = 0;
		registerReceiver(mTvawReceiver, mTvawFilter);

		super.onNewIntent(intent);
	}

	private void dosetbannerexif(int position) {
		banner_pic_index = (TextView) findViewById(R.id.banner_pic_index);
		banner_pic_name = (TextView) findViewById(R.id.banner_pic_name);
		banner_pic_timeinfo = (TextView) findViewById(R.id.banner_pic_timeinfo);
		banner_pic_resolution = (TextView) findViewById(R.id.banner_pic_resolution);

		ExifInterface exif = null;
		try {
			exif = new ExifInterface(m_filePathStrArray[position]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String title = null;
		if (mIsFromAnywhere == false) {
			//title = mPhotoList.get(position + mPhotoDirNum).getFileName();// mDataProvider.GetTitleAt(position);
		} else {
			title = m_filePathStrArray[position]
					.substring(m_filePathStrArray[position].lastIndexOf("/") + 1);
		}
		String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
		String length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
		String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
		//DLNABrowser dlnaBrowser = DLNABrowser.GetInstance();
		//date = dlnaBrowser.queryDateByFile(title);
		//String resolution = dlnaBrowser.queryResolutionByFile(title);
		//banner_pic_resolution.setText(resolution);

		banner_pic_index.setText(String.valueOf(position + 1) + "/"
				+ (m_filePathStrArray.length - mPhotoDirNum));// mDataProvider.GetSize());
		banner_pic_name.setText(title);
		banner_pic_timeinfo.setText(date);
	}

	protected void dofullscrean() {
		// TODO Auto-generated method stub

		Log.d(TAG,
				"kelly onCreate: photo atv view W:["
						+ mPhotoPlaybackView.getWidth() + "]");
		Log.d(TAG,
				"kelly onCreate: photo atv view H:["
						+ mPhotoPlaybackView.getHeight() + "]");

		dosetmoveicon();
		RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);
		/*
		RelativeLayout.LayoutParams llayoutparams_banner_photo = new RelativeLayout.LayoutParams(
	              LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	    llayoutparams_banner_photo.width  = mDisplayMetrics.widthPixels;
	    llayoutparams_banner_photo.height = 0;//llayout_params_gallery.height*3/5;
	    llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);
	      */
		llayout_banner_photo.setVisibility(View.INVISIBLE);
		llayout_banner_photo.setLayoutParams(llayout_banner_photo.getLayoutParams());
		/*
       	LinearLayout l_atvview=(LinearLayout)findViewById(R.id.RtkAtvView_LinearLayout);
       	RelativeLayout.LayoutParams llayout_params_atvview = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       	llayout_params_atvview.width=mDisplayMetrics.widthPixels;
        llayout_params_atvview.height=mDisplayMetrics.heightPixels;//-llayout_params_gallery.height;//- rlayout_params_title.height; 
        l_atvview.setLayoutParams(llayout_params_atvview);
 
        SurfaceView sv=(SurfaceView)findViewById(R.id.m_photoPlaybackSurfaceView);
        LinearLayout.LayoutParams   llp   =   (LinearLayout.LayoutParams) sv.getLayoutParams();
        llp.width  = mDisplayMetrics.widthPixels;
        llp.height = mDisplayMetrics.heightPixels;//-llayout_params_gallery.height;//- rlayout_params_title.height;
        mPhotoPlaybackView.getHolder().setFixedSize(llp.width,llp.height); 
        mPhotoPlaybackView.setLayoutParams(llp); 
        */
        
        mIsFullScrean = true;
	}

	@Override
	public void onDestroy() {
		finishThd = true;
		mActivityDestroyFlag = 1;
		if (m_pPictureKit != null && showInBackground) {
			m_startdecode = false;
			m_pPictureKit.stopPictureKit();
			m_pPictureKit = null;
		}
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "kelly onResume:");
		mActivityPauseFlag = 0;
		initLayout();
		if (mQuickmenu.isShowing()) {
			mQuickmenu.setIsActivityPause(mActivityPauseFlag);
			mQuickmenu.setTimeout();
		}

		if (m_startdecode == false || m_pPictureKit == null) {
			StartPictureKit();
			if (m_totalCnt > 0) {
				m_checkFirstPictureHandler.postDelayed(m_checkFirstDecodeCb,
						100);
				// DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
			}
		}

		mPhotoPlaybackView.setFocusable(false);
		mPhotoPlaybackView.setFocusableInTouchMode(false);

		/*
		 * // set source if (mMediaApplicationMap. ()) { mTv.setDisplayWindow(0,
		 * 0, 3840, 2160); } else { mTv.setDisplayWindow(0, 0, 1920, 1080); }
		 */

		showInBackground = false;
		IntentFilter filter2 = new IntentFilter(
				"com.realtek.TVShowInBackGround");
		registerReceiver(OnEngMenupRecever, filter2);

		mIsSlideShowModel = false;
		SetBannerSize();		
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				Editor editor = mPerferences.edit();
				int tmp_index_repeat   = mPerferences.getInt("repeatIndex_photo", -1);
				int tmp_index_interval = mPerferences.getInt("intervalIndex_photo", -1);
				
				if(tmp_index_repeat == -1)
				{
					editor.putInt("repeatIndex_photo", mRepeatIndex);
					editor.commit();
				}else{
					mRepeatIndex = tmp_index_repeat;
				}
				quickUIhandler.sendEmptyMessage(MSG_SET_REPEAT);
				
				if(tmp_index_interval == -1)
				{
					editor.putInt("intervalIndex_photo", mIntervalIndex);
					editor.commit();
				}else{
					mIntervalIndex = tmp_index_interval;
				}
				quickUIhandler.sendEmptyMessage(MSG_SET_INTERVAL);
				

				int mins = getSleepTimeValue();
				mSleepTimeHour = mins / 60;
				mSleepTimeMin = mins % 60;
			
			}
		}).start();	
		new Thread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(mActivityPauseFlag != 1)
				{

					int mins = getSleepTimeValue();
					mSleepTimeHour = mins / 60;
					mSleepTimeMin = mins % 60;
					
					if(mQuickmenu.isShowing())
					{
						quickUIhandler.sendEmptyMessage(PhotoPlayerActivity.MSG_REFRESH_TIMER);
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
		mQuickmenuAdapter.notifyDataSetChanged();
   }
    public void SetBannerSize()
    {
    	float fontScale = 0;//RTKDMPConfig.getFontSize(this);
    	Log.v(TAG, "Font Size Scale : "+fontScale);
		if(fontScale <= 1)
		{
			RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);		    
			RelativeLayout.LayoutParams llayoutparams_banner_photo = new RelativeLayout.LayoutParams(
		            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		    llayoutparams_banner_photo.height=155;
		    llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		    llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);			
		}
		else if(fontScale > 1 && fontScale<1.29)
		{
			RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);	    
			RelativeLayout.LayoutParams llayoutparams_banner_photo = new RelativeLayout.LayoutParams(
		            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		    llayoutparams_banner_photo.height=167;
		    llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		    llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);
		}
		else if(fontScale > 1.29)
    	{
    		RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);	    
			RelativeLayout.LayoutParams llayoutparams_banner_photo = new RelativeLayout.LayoutParams(
		            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		    llayoutparams_banner_photo.height=180;
		    llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		    llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);
    	}
    }
	public class SurfaceListener implements SurfaceHolder.Callback {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
		}

	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.v(TAG, "onPause");
		mActivityPauseFlag = 1;
		if (mQuickmenu.isShowing()) {
			mQuickmenu.setIsActivityPause(mActivityPauseFlag);
		}
		m_checkResultHandlerTimer_forOnKey
				.removeCallbacks(m_checkResultTimerCb_forOnkeyLeft);
		m_checkResultHandlerTimer_forOnKey
				.removeCallbacks(m_checkResultTimerCb_forOnkeyRight);
		m_checkResultHandlerTimer.removeCallbacks(m_checkResultTimerCb);
		m_checkFirstPictureHandler.removeCallbacks(m_checkFirstDecodeCb);
		m_checkResultHandlerTimer.removeCallbacks(m_checkResultFirstTimerCb);

		if (m_pPictureKit != null && !showInBackground) {
			m_startdecode = false;
			m_pPictureKit.stopPictureKit();
			m_pPictureKit = null;
		}
		mIsSlideShowModel = false;
		check_slideshow_mode();

		if (mIsFromAnywhere == true) {
			unregisterReceiver(mTvawReceiver);
		}

		if (OnEngMenupRecever != null) {
			unregisterReceiver(OnEngMenupRecever);
		}
	}

	private boolean showInBackground = false;
	private BroadcastReceiver OnEngMenupRecever = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals("com.realtek.TVShowInBackGround")) {
				showInBackground = true;
			}
		}

	};

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(TAG, "photoactivity onKeyUp : " + event.toString());

		if (keyCode == KeyEvent.KEYCODE_M || keyCode == KeyEvent.KEYCODE_INFO) {
			mBannerShowTime = 60000;
				if(mIsFullScrean)
				{
					dosetmoveicon();
					
					RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);
							 llayout_banner_photo.setVisibility(View.VISIBLE);
				    llayout_banner_photo.setLayoutParams(llayout_banner_photo.getLayoutParams());
					/*
					 * 
					
					RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);
					    
					RelativeLayout.LayoutParams llayoutparams_banner_photo = new RelativeLayout.LayoutParams(
				            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				    llayoutparams_banner_photo.width=mDisplayMetrics.widthPixels;
//				    if(mMediaApplicationMap.getScale() == 1.5){
//				    	llayoutparams_banner_photo.height=129;//llayout_params_gallery.height*3/5;
//				    }else if(mMediaApplicationMap.getScale() == 2){
//				    	llayoutparams_banner_photo.height=129;
//				    }
				    llayoutparams_banner_photo.height=150;
				    llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);
			     						
			       	LinearLayout l_atvview=(LinearLayout)findViewById(R.id.RtkAtvView_LinearLayout);
			       	RelativeLayout.LayoutParams llayout_params_atvview = new RelativeLayout.LayoutParams(
			                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			       	llayout_params_atvview.width=mDisplayMetrics.widthPixels;
			        llayout_params_atvview.height=mDisplayMetrics.heightPixels-llayoutparams_banner_photo.height;//- rlayout_params_title.height;

			        l_atvview.setLayoutParams(llayout_params_atvview);

			        SurfaceView sv=(SurfaceView)findViewById(R.id.m_photoPlaybackSurfaceView);
			        LinearLayout.LayoutParams   llp   =   (LinearLayout.LayoutParams) sv.getLayoutParams();
			        llp.width	=	mDisplayMetrics.widthPixels;
			        llp.weight=0;
			        llp.height   =  mDisplayMetrics.heightPixels-llayoutparams_banner_photo.height;//- rlayout_params_title.height;
			      
			        mPhotoPlaybackView.getHolder().setFixedSize(llp.width,llp.height); 
			        mPhotoPlaybackView.setLayoutParams(llp); 
			        */
				}		   			
				else
				{
					dosetmoveicon();
					
					RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);
					llayout_banner_photo.setVisibility(View.INVISIBLE);
					llayout_banner_photo.setLayoutParams(llayout_banner_photo.getLayoutParams());
					
					/*
					RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.banner_photo);

					RelativeLayout.LayoutParams llayoutparams_banner_photo = new RelativeLayout.LayoutParams(
					              LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
					llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				    llayoutparams_banner_photo.width=0;
				    llayoutparams_banner_photo.height=0;
				    llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);
					LinearLayout l_atvview=(LinearLayout)findViewById(R.id.RtkAtvView_LinearLayout);
				    RelativeLayout.LayoutParams llayout_params_atvview = new RelativeLayout.LayoutParams(
				                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				    
				    llayout_params_atvview.width=mDisplayMetrics.widthPixels;
				    llayout_params_atvview.height=mDisplayMetrics.heightPixels;//- rlayout_params_title.height; 
				    l_atvview.setLayoutParams(llayout_params_atvview);
			
			        SurfaceView sv=(SurfaceView)findViewById(R.id.m_photoPlaybackSurfaceView);
			        LinearLayout.LayoutParams   llp   =   (LinearLayout.LayoutParams) sv.getLayoutParams();
			        llp.width	=	mDisplayMetrics.widthPixels;
			        llp.weight  =   0;
			        llp.height  =   mDisplayMetrics.heightPixels;//-llayout_params_gallery.height;//- rlayout_params_title.height;
			      
			        mPhotoPlaybackView.getHolder().setFixedSize(llp.width,llp.height); 
			        mPhotoPlaybackView.setLayoutParams(llp); 	
			        */
					
			}
			mIsFullScrean = !mIsFullScrean;
		} else if (keyCode == KeyEvent.KEYCODE_A
				|| keyCode == KeyEvent.KEYCODE_ENTER || keyCode == 23) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
				setBannerShowtime(6000);
				mIsSlideShowModel = !mIsSlideShowModel;
				initLayout();
				check_slideshow_mode();
			}
		} else if (keyCode == KeyEvent.KEYCODE_S) {
			mRepeatMode = (mRepeatMode + 1) % 2;
			check_repeat_mode();
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
				if(DMSName.size() != 0){
					Intent mIntent = new Intent(this, GridViewActivity.class);
					mIntent.putStringArrayListExtra("DMSName", DMSName);
					setResult(ResultCodeForDMSCLosed, mIntent);
				}else{
					setResult(m_currentPlayIndex);
				}
				finish();
				return true;
			} else {
				for (; 0 < mIsZoomModel; mIsZoomModel--) {
					mTv.zoomOut();
				}
				check_zoom_model();
			}
		} else if (keyCode == KeyEvent.KEYCODE_E
				|| keyCode == KeyEvent.KEYCODE_ESCAPE) {
			setResult(-10);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_Q || keyCode == 227) // 227
																	// present
																	// KEYCODE_QUICK_MENU
		{
			if (mQuickmenu.isShowing() == true)
				mQuickmenu.dismiss();
			else {
				mQuickmenu.showQuickMenu(14, 14);
				mQuickmenu.setTimeout();
				if (mIsSlideShowModel == true) {
					mIsSlideShowModel = false;
					check_slideshow_mode();
				}
			}
		} 
		else if(keyCode == 82|| keyCode == 220)
		{
			if(null == msg_notavaible)
			{
				msg_notavaible = new Message_not_avaible(PhotoPlayerActivity.this);
			}
			msg_notavaible.show_msg_notavailable();
			
			mLastNotAvailableShowTime = (new Date(System.currentTimeMillis())).getTime();
			new Thread(new Runnable() {
	    		public void run() {
	    			long curtime = 0;
	    			while(true)
	    			{
	    				if(msg_notavaible.isShowing() == false)
	    					break;
	    				curtime = (new Date(System.currentTimeMillis())).getTime();
	    				if(curtime - mLastNotAvailableShowTime > 1000)
		    			{
		    				Message msg = new Message();
		    				msg.what = 2;
		    				mCheckTimerHandler.sendMessage(msg);
		    			}            	    				 
	    				try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}	
	    			}
	    		}
	    	}).start();
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "photoactivity onKeyDown : " + event.toString());
		mLastControlTime = (new Date(System.currentTimeMillis())).getTime();

		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
				mTv.rightRotate();

				// pause ,then play,to simulate reseting inteval time
				if (mIsSlideShowModel) {
					m_checkResultHandlerTimer
							.removeCallbacks(m_checkResultTimerCb);
					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);

					if (m_totalCnt > 0) {
						m_checkResultHandlerTimer.postDelayed(
								m_checkResultTimerCb, m_checkResultTime);
					}
				}
				// end
			} else {
				mTv.onZoomMoveUp();
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
				mTv.leftRotate();

				// pause ,then play,to simulate reseting inteval time
				if (mIsSlideShowModel) {
					m_checkResultHandlerTimer
							.removeCallbacks(m_checkResultTimerCb);
					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);

					if (m_totalCnt > 0) {
						m_checkResultHandlerTimer.postDelayed(
								m_checkResultTimerCb, m_checkResultTime);
					}
				}
				// end
			} else {
				mTv.onZoomMoveDown();
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == 234 /*
																			 * for
																			 * L4300
																			 * KeyEvent
																			 * .
																			 * KEYCODE_PREVIOUS
																			 */) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
//				if (mIsFullScrean == false)
					if(isPlayerButtonListVisible == true)
						return false;
				if (m_pPictureKit != null) {

					if (mRepeatMode == 1) {
						if (m_currentPlayIndex - 1 < 0) {
							m_currentPlayIndex = m_totalCnt - 1;
							DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
						} else {
							m_currentPlayIndex--;
							DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
						}
						dosetbannerexif(m_currentPlayIndex);
					} else if (mRepeatMode == 0) {
						if (m_currentPlayIndex - 1 >= 0) {
							m_currentPlayIndex--;
							DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
						}
						dosetbannerexif(m_currentPlayIndex);
					}
				}
				if (!mIsSlideShowModel)
					m_checkResultHandlerTimer_forOnKey.postDelayed(
							m_checkResultTimerCb_forOnkeyLeft, 200);
				check_slideshow_mode();
			} else {
				mTv.onZoomMoveLeft();
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == 235/*
																			 * for
																			 * L4300
																			 * KeyEvent
																			 * .
																			 * KEYCODE_NEXT
																			 */) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {

				if(isPlayerButtonListVisible == true)
					return false;
				if (m_pPictureKit != null) {
					if (mRepeatMode == 1) {
						if (m_currentPlayIndex + 1 >= m_totalCnt) {
							m_currentPlayIndex = 0;
							DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
						} else {
							m_currentPlayIndex++;
							DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
						}
						dosetbannerexif(m_currentPlayIndex);
					} else if (mRepeatMode == 0) {

						if (m_currentPlayIndex + 1 >= m_totalCnt) {
							setResult(m_currentPlayIndex);
							finish();
							return true;
						} else {
							m_currentPlayIndex++;
							DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
						}
						dosetbannerexif(m_currentPlayIndex);
					}
					check_slideshow_mode();
				}
				if (!mIsSlideShowModel)
					m_checkResultHandlerTimer_forOnKey.postDelayed(
							m_checkResultTimerCb_forOnkeyRight, 200);
			} else {
				mTv.onZoomMoveRight();
			}
		}

		else if (keyCode == KeyEvent.KEYCODE_Z
				|| keyCode == KeyEvent.KEYCODE_PROG_RED) {
			if (mIsZoomModel < 4) {
				mIsZoomModel++;
				mTv.zoomIn();
				check_zoom_model();
				initLayout();
			}
		} else if (keyCode == KeyEvent.KEYCODE_X
				|| keyCode == KeyEvent.KEYCODE_PROG_GREEN) {
			if (mIsZoomModel > 0) {
				mIsZoomModel--;
				mTv.zoomOut();
				check_zoom_model();
				initLayout();
			}
		} else if (keyCode == KeyEvent.KEYCODE_C) {
			mTv.leftRotate();
		} else if (keyCode == KeyEvent.KEYCODE_V) {
			mTv.rightRotate();
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void operation_left() {
		if (mIsZoomModel == NOT_ZOOM_MODE) {
			if (m_pPictureKit != null) {

				if (mRepeatMode == 1) {
					if (m_currentPlayIndex - 1 < 0) {
						m_currentPlayIndex = m_totalCnt - 1;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					} else {
						m_currentPlayIndex--;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					}
					dosetbannerexif(m_currentPlayIndex);
				} else if (mRepeatMode == 0) {
					if (m_currentPlayIndex - 1 >= 0) {
						m_currentPlayIndex--;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					}
					dosetbannerexif(m_currentPlayIndex);
				}
			}
			if (!mIsSlideShowModel)
				m_checkResultHandlerTimer_forOnKey.postDelayed(
						m_checkResultTimerCb_forOnkeyLeft, 200);
			check_slideshow_mode();
		} else {
			mTv.onZoomMoveLeft();
		}
	}

	private void operation_right() {
		if (mIsZoomModel == NOT_ZOOM_MODE) {
			if (m_pPictureKit != null) {
				if (mRepeatMode == 1) {
					if (m_currentPlayIndex + 1 >= m_totalCnt) {
						m_currentPlayIndex = 0;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					} else {
						m_currentPlayIndex++;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					}
					dosetbannerexif(m_currentPlayIndex);
				} else if (mRepeatMode == 0) {

					if (m_currentPlayIndex + 1 >= m_totalCnt) {
						setResult(m_currentPlayIndex);
						finish();
						return;
					} else {
						m_currentPlayIndex++;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					}
					dosetbannerexif(m_currentPlayIndex);
				}
				check_slideshow_mode();
			}
			if (!mIsSlideShowModel)
				m_checkResultHandlerTimer_forOnKey.postDelayed(
						m_checkResultTimerCb_forOnkeyRight, 200);
		} else {
			mTv.onZoomMoveRight();
		}
	}

	private void check_slideshow_mode() {
		// TODO Auto-generated method stub
		if (mIsSlideShowModel == true) {
			if (m_totalCnt > 0) {
				m_checkResultHandlerTimer.postDelayed(m_checkResultTimerCb,
						m_checkResultTime);
			}
			banner_slideshow.setImageResource(R.drawable.photo_play_on);
			slide_single.setText(R.string.guide_single_view);
		} else if (mIsSlideShowModel == false) {
			m_checkResultHandlerTimer.removeCallbacks(m_checkResultTimerCb);
			m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);
			banner_slideshow.setImageResource(R.drawable.photo_play_off);
			slide_single.setText(R.string.guide_slide_show);
		}
	}

	private void check_repeat_mode() {
		if (mRepeatMode == 1) {
			banner_repeat.setImageResource(R.drawable.photo_repeat_all_on);
		} else if (mRepeatMode == 0) {
			banner_repeat.setImageResource(R.drawable.photo_repeat_all_off);
		}
	}

	private void check_zoom_model() {
		if (mIsZoomModel != NOT_ZOOM_MODE) {
			if (mIsSlideShowModel == true) {
				mIsSlideShowModel = false;
				check_slideshow_mode();
			}
			if (mIsZoomModel > 0) {
				move_left_btm.setVisibility(View.VISIBLE);
				move_right_btm.setVisibility(View.VISIBLE);
				move_top_btm.setVisibility(View.VISIBLE);
				move_bottom_btm.setVisibility(View.VISIBLE);

				guide1_bar.setVisibility(View.INVISIBLE);
				guide2_bar.setVisibility(View.VISIBLE);
				player_common_backward_button_list.setFocusable(false);
				player_common_forward_button_list.setFocusable(false);
				photo_player_repeat_on_button_list.setFocusable(false);

			}
			pic_multiple.setVisibility(View.VISIBLE);
			switch (mIsZoomModel) {
			case 1: {
				pic_multiple.setText("x2");
				break;
			}
			case 2: {
				pic_multiple.setText("x4");
				break;
			}
			case 3: {
				pic_multiple.setText("x8");
				break;
			}
			case 4: {
				pic_multiple.setText("x16");
				break;
			}
			case -1: {
				pic_multiple.setText("x1/2");
				break;
			}
			case -2: {
				pic_multiple.setText("x1/4");
				break;
			}
			case -3: {
				pic_multiple.setText("x1/8");
				break;
			}
			}
		} else {

			move_left_btm.setVisibility(View.INVISIBLE);
			move_right_btm.setVisibility(View.INVISIBLE);
			move_top_btm.setVisibility(View.INVISIBLE);
			move_bottom_btm.setVisibility(View.INVISIBLE);
			pic_multiple.setVisibility(View.INVISIBLE);
			guide1_bar.setVisibility(View.VISIBLE);
			guide2_bar.setVisibility(View.INVISIBLE);
			player_common_backward_button_list.setFocusable(true);
			player_common_forward_button_list.setFocusable(true);
			photo_player_repeat_on_button_list.setFocusable(true);

		}
		RelativeLayout llayout_banner_photo = (RelativeLayout) findViewById(R.id.banner_photo);
		llayout_banner_photo.setLayoutParams(llayout_banner_photo
				.getLayoutParams());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public void NextImage() {
		if (m_totalCnt <= 0)
			return;

		if (m_currentPlayIndex < m_totalCnt - 1) {
			m_currentPlayIndex++;

			Log.e(TAG, "NextImage m_currentPlayIndex:[" + m_currentPlayIndex
					+ "]");

			DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
		} else if (mRepeatMode == 1) {
			m_currentPlayIndex = 0;
			Log.e(TAG, "NextImage m_currentPlayIndex:[" + m_currentPlayIndex
					+ "]");
			DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
		} else if (mRepeatMode == 0) {
			Log.d(TAG, "NEXT  " + m_currentPlayIndex);
			setResult(m_currentPlayIndex);
			finish();
			return;
		}
		m_checkResultHandlerTimer.postDelayed(m_checkResultTimerCb,
				m_checkResultTime);

		// TSB spec
		Message msg = new Message();
		msg.arg1 = m_currentPlayIndex;
		msg.what = 0;
		mSetBannerHandler.sendMessage(msg);
	}

	public void StartPictureKit() {
		new Thread(new Runnable() {
			public void run() {
				long ret;
				int timeout = 0;
				if (m_pPictureKit == null) {
					m_pPictureKit = new PictureKit(PhotoPlayerActivity.this);
					Log.d(TAG, "New RTK_PictureKit in StartPictureKit");
				}
				while (m_startdecode == false) {
					if (m_pPictureKit != null) {
						ret = m_pPictureKit.startPictureKit();
						if (ret == -1) {
							Log.e(TAG,
									"New RTK_PictureKit memory is not enough => fail decode");
							break;
						} else if ((ret == 1) || (ret == -2)) {
							try {
								Thread.sleep(100);
								Log.d(TAG, "StartPictureKit need to do again :"
										+ ret);
								Log.e(TAG, "startdecode do :" + timeout);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							timeout++;
							if (timeout > 50) {
								Log.e(TAG, "startdecode failed timeout > 5sec ");
								break;
							}
						} else {
							m_startdecode = true;
							Log.d(TAG, "StartPictureKit done :" + ret);
						}
					} else {
						Log.e(TAG, "RTK_PictureKit is null");
						break;
					}
				}
			}
		}).start();
	}

	public void DecodePictureKit(String Url) {
		m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);
		boolean isRemoved = false;
		if (DMSName.size() != 0) {
			for (int i = 0; i < DMSName.size(); i++) {
				isRemoved = DMSName.get(i).equals(serverName);
				if (isRemoved) {
					break;
				}
			}
			if (isRemoved) {
				short_msg.confirm_title.setVisibility(View.INVISIBLE);
				short_msg.setMessage(getResources().getString(
						R.string.DMS_was_close));
				short_msg.setButtonText(getResources().getString(
						R.string.msg_yes));
				short_msg.left.setVisibility(View.VISIBLE);
				short_msg.right.setVisibility(View.VISIBLE);
				short_msg.confirm_bt.setVisibility(View.VISIBLE);
				short_msg.setKeyListener(true);
				short_msg.confirm_bt.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						DMSName.clear();
						short_msg.dismiss();
						setResult(ResultCodeRestartGridViewActivity);
						PhotoPlayerActivity.this.finish();
					}

				});

				short_msg.show();
			}
		} else if (m_pPictureKit != null) {

			DecoderInfo di = new DecoderInfo();
			di.decodemode = 7;

			di.bUpnpFile = true;

			PictureKit.loadPicture(Url, di);
			Log.d(TAG, "RTK_PictureKit.loadPicture");

			m_decodeImageState = DecodeImageState.STATE_DECODEING;
			m_decodeImageResult = DecodeImageState.STATE_DECODE_RESULT_INIT;
			new Thread(new Runnable() {
				public void run() {
					int timeout = 0;
					while (m_startdecode == false) {
						if (m_pPictureKit != null) {
							try {
								Thread.sleep(100);
								Log.e(TAG, "startdecode is failed :" + timeout);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							timeout++;
							if (timeout > 50) {
								Log.e(TAG, "startdecode failed timeout > 5sec ");
								break;
							}
						} else {
							Log.e(TAG, "RTK_PictureKit is null");
							break;
						}
					}
					timeout = 0;
					while (m_startdecode) {
						if (m_pPictureKit != null) {
							long ret;
							try {
								Log.e(TAG, "kelly picturekit: Get Result......");
								ret = m_pPictureKit.GetDecodeImageResult();
								if (ret == 1) // DEC_IMG_DECODING
								{
									Log.e(TAG,
											"kelly picturekit DecodeImage decoding");
									Thread.sleep(100);
								} else if (ret == -5 || ret == -1) {
									// if(decodeFirstPicture)
									if (mDecodeRetryTimes > 8) {
										Log.e(TAG,
												"DecodeImage failed retry timeout > 8 times");
										Log.e(TAG,
												"picturekit DecodeImage failed");
										mDecodeRetryTimes = 0;
										m_decodeImageResult = DecodeImageState.STATE_DECODE_RESULT_FAIL;
										m_decodeImageState = DecodeImageState.STATE_DECODE_DONE;
										break;
									}

									Log.e(TAG, "ret is: " + ret
											+ ",DecodeImage failed now retry! ");
									mDecodeRetryTimes++;
									if (m_pPictureKit != null) {
										m_startdecode = false;
										m_pPictureKit.stopPictureKit();
										m_pPictureKit = null;
									}
									StartPictureKit();
									m_checkFirstPictureHandler.postDelayed(
											m_checkFirstDecodeCb, 100);
									m_decodeImageState = DecodeImageState.STATE_DECODE_RETRY;
									break;
								} 
								else if(ret == -9)
    							{
    								Log.e(TAG,"kelly picturekit DecodeImage TRANSITTING!");
    								Thread.sleep(100);
    							}
    							else if(ret < 0 && ret !=-5 && ret != -1 && ret != -9) //Decode Fail
								{
									Thread.sleep(100);
									timeout++;
									if (timeout > 20) {
										Log.e(TAG,
												"DecodeImage failed timeout > 5sec ");
										Log.e(TAG,
												"kelly picturekit DecodeImage failed");
										m_decodeImageResult = DecodeImageState.STATE_DECODE_RESULT_FAIL;
										mDecodeRetryTimes = 0;
										m_decodeImageState = DecodeImageState.STATE_DECODE_DONE;
										break;
									}
								} else {
									Log.e(TAG,
											"kelly picturekit DecodeImage success");
									m_decodeImageResult = DecodeImageState.STATE_DECODE_RESULT_SUCCESS;
									mDecodeRetryTimes = 0;
									m_decodeImageState = DecodeImageState.STATE_DECODE_DONE;
									break;
								}

							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							Log.e(TAG, "RTK_PictureKit is null");
							m_decodeImageResult = DecodeImageState.STATE_DECODE_RESULT_FAIL;
							m_decodeImageState = DecodeImageState.STATE_DECODE_DONE;
							break;
						}
					}
				}
			}).start();

		} else {
			Log.e(TAG, "PicDecodeRTK is not ready!!!");
			m_decodeImageResult = DecodeImageState.STATE_DECODE_RESULT_FAIL;
		}

	}

	public void StopPictureKit() {
		if (m_pPictureKit == null || m_startdecode == false)
			return;

		m_pPictureKit.stopPictureKit();
	}

	private final Runnable m_checkFirstDecodeCb = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			if (mActivityPauseFlag == 1)
				return;
			if (m_startdecode == false || m_pPictureKit == null) {
				Log.e(TAG, "Not Decode!!!");
				m_checkFirstPictureHandler.postDelayed(this, 100);
			} else // if( m_decodeImageState == DecodeImageState.STATE_DECODEING
					// )
			{
				/*
				 * Log.e(TAG,"m_checkFirstDecodeCb: Decoding now ????!");
				 * m_checkFirstPictureHandler.postDelayed(this, 100); } else {
				 */
				Log.e(TAG, "m_checkFirstDecodeCb:not Decoding");
				m_checkFirstPictureHandler.removeCallbacks(this);
				// make sure m_decodeImageState is not STATE_DECODEING,then we
				// begin to Decode,
				// or first picture may often fail decoding (mantis 0039273)
				DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
				m_checkResultHandlerTimer.postDelayed(
						m_checkResultFirstTimerCb, m_checkResultTime);
			}
		}
	};
	private final Runnable m_checkResultFirstTimerCb = new Runnable() {
		public void run() {
			if (m_startdecode == false || m_pPictureKit == null) {
				Log.e(TAG, "m_checkResultFirstTimerCb Not Decode!!!");
				// m_checkResultHandlerTimer.removeCallbacks(this); //put this
				// in onDestory
				m_checkResultHandlerTimer.postDelayed(this, m_checkResultTime);
			} else {
				if (m_decodeImageState == DecodeImageState.STATE_DECODEING) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "FirstTimerCb Get Result decodeing!!!");
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_DONE) {
					Log.e(TAG, "m_checkResultFirstTimerCb Decode Done!!!");
					if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_SUCCESS) {
						Log.e(TAG,
								"m_checkResultFirstTimerCb Decode Success!!!");
						m_checkResultHandlerTimer.removeCallbacks(this);
						;
					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_FAIL) {
						Log.e(TAG, "m_checkResultFirstTimerCb Decode Fail!!!");
						msg_hint.setMessage(m_ResourceMgr
								.getString(R.string.msg_unsupport));
						msg_hint.show();
						mLastUnsupportShowTime = (new Date(
								System.currentTimeMillis())).getTime();
						new Thread(new Runnable() {
							public void run() {
								long curtime = 0;
								while (true) {
									if (msg_hint.isShowing() == false)
										break;
									curtime = (new Date(System
											.currentTimeMillis())).getTime();
									if (curtime - mLastUnsupportShowTime > 2000) {
										Message msg = new Message();
										msg.what = 1;
										mCheckTimerHandler.sendMessage(msg);
									}
									try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
						}).start();
						m_checkResultHandlerTimer.removeCallbacks(this);
					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_TRANSITTING) {
						Log.e(TAG, "First Decode TRANSITTING! "
								+ m_decodeImageResult);
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					} else {
						Log.e(TAG, "First Decode Unknown Result: "
								+ m_decodeImageResult);
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					}
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_RETRY) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "Get State Retry!!!");
				}
			}
		}
	};
	private final Runnable m_checkResultTimerCb = new Runnable() {
		public void run() {
			if (m_startdecode == false || m_pPictureKit == null) {
				Log.e(TAG, "Not Decode!!!");
				// m_checkResultHandlerTimer.removeCallbacks(this); //put this
				// in onDestory
				m_checkResultHandlerTimer.postDelayed(this, m_checkResultTime);
			} else {
				if (m_decodeImageState == DecodeImageState.STATE_DECODEING) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "Get Result decodeing!!!");
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_DONE) {
					Log.e(TAG, "Decode Done!!!");
					if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_SUCCESS) {
						Log.e(TAG, "Decode Success!!!");
						m_checkResultHandlerTimer.removeCallbacks(this);
						Log.e(TAG,
								"m_checkResultTimerCb:start m_slideShowTimerCb:!!!");
						m_slideShowHandlerTimer.postDelayed(m_slideShowTimerCb,
								m_slideShowTime);
					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_FAIL) {
						Log.e(TAG, "Decode Fail!!!");
						/*
						 * Toast.makeText(PhotoPlayerActivity.this
						 * ,m_filePathStrArray
						 * [m_currentPlayIndex].substring(m_filePathStrArray
						 * [m_currentPlayIndex].lastIndexOf("/")+1)
						 * +" "+m_ResourceMgr.getString(R.string.decode_fail)
						 * ,Toast.LENGTH_SHORT).show();
						 */
						m_checkResultHandlerTimer.removeCallbacks(this);
						Log.e(TAG,
								"m_checkResultTimerCb:start m_slideShowTimerCb:!!!");
						m_slideShowHandlerTimer.postDelayed(m_slideShowTimerCb,
								0);
					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_TRANSITTING) {
						Log.e(TAG, "Decode TRANSITTING! " + m_decodeImageResult);
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					} else {
						Log.e(TAG, "Decode Unknown Result: "
								+ m_decodeImageResult);
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					}
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_RETRY) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "Get State Retry!!!");
				}
			}
		}
	};

	private final Runnable m_checkResultTimerCb_forOnkeyRight = new Runnable() {
		public void run() {
			if (mActivityPauseFlag == 1)
				return;
			if (m_startdecode == false || m_pPictureKit == null) {
				Log.e(TAG, "OnKey Not Decode!!!");
				m_checkResultHandlerTimer.postDelayed(this, m_checkResultTime);
			} else {
				if (m_decodeImageState == DecodeImageState.STATE_DECODEING) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "OnKey Get Result decodeing!!!");
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_RETRY) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "OnKey Get State Retry");
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_DONE) {
					Log.e(TAG, "OnKey Decode Done!!!" + m_decodeImageResult);

					if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_FAIL) {
						Toast.makeText(
								PhotoPlayerActivity.this,
								m_filePathStrArray[m_currentPlayIndex]
										.substring(m_filePathStrArray[m_currentPlayIndex]
												.lastIndexOf("/") + 1)
										+ " "
										+ m_ResourceMgr
												.getString(R.string.decode_fail),
								Toast.LENGTH_SHORT).show();
						Log.e(TAG, "Decode Failed!!! Go to Next");
						if (mRepeatMode == 1) {
							if (m_currentPlayIndex + 1 >= m_totalCnt) {
								m_currentPlayIndex = 0;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							} else {
								m_currentPlayIndex++;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							}
							dosetbannerexif(m_currentPlayIndex);
						} else if (mRepeatMode == 0) {
							if (m_currentPlayIndex + 1 >= m_totalCnt) {
								Log.d(TAG, "RIGHT  " + m_currentPlayIndex);
								setResult(m_currentPlayIndex);
								finish();
								return;
							} else {
								m_currentPlayIndex++;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							}
							dosetbannerexif(m_currentPlayIndex);
						}
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);

					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_SUCCESS) {
						check_slideshow_mode();
						;// thread end here
					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_TRANSITTING) {
						Log.e(TAG, "Right Decode TRANSITTING! "
								+ m_decodeImageResult);
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					} else {
						Log.e(TAG, "Right Decode Unknown Result: "
								+ m_decodeImageResult);
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					}

				}
			}
		}
	};
	private final Runnable m_checkResultTimerCb_forOnkeyLeft = new Runnable() {
		public void run() {
			if (mActivityPauseFlag == 1)
				return;
			if (m_startdecode == false || m_pPictureKit == null) {
				Log.e(TAG, "OnKey Not Decode!!!");
				m_checkResultHandlerTimer.postDelayed(this, m_checkResultTime);
			} else {
				if (m_decodeImageState == DecodeImageState.STATE_DECODEING) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "Get Result decodeing!!!");
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_RETRY) {
					m_checkResultHandlerTimer.postDelayed(this,
							m_checkResultTime);
					Log.e(TAG, "OnKey Get State Retry");
				} else if (m_decodeImageState == DecodeImageState.STATE_DECODE_DONE) {
					Log.e(TAG, "Decode Done!!!");

					if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_FAIL) {
						Toast.makeText(
								PhotoPlayerActivity.this,
								m_filePathStrArray[m_currentPlayIndex]
										.substring(m_filePathStrArray[m_currentPlayIndex]
												.lastIndexOf("/") + 1)
										+ " "
										+ m_ResourceMgr
												.getString(R.string.decode_fail),
								Toast.LENGTH_SHORT).show();
						if (mRepeatMode == 1) {
							if (m_currentPlayIndex - 1 < 0) {
								m_currentPlayIndex = m_totalCnt - 1;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							} else {
								m_currentPlayIndex--;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							}
							dosetbannerexif(m_currentPlayIndex);
						} else if (mRepeatMode == 0) {
							if (m_currentPlayIndex - 1 > 0) {
								m_currentPlayIndex--;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							}
							dosetbannerexif(m_currentPlayIndex);
						}
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_RESULT_SUCCESS) {
						check_slideshow_mode();
						;// thread end here
					} else if (m_decodeImageResult == DecodeImageState.STATE_DECODE_TRANSITTING) {
						Log.e(TAG, "Left Decode TRANSITTING!!! ");
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					} else {
						Log.e(TAG, "Left Decode Unknown Result:"
								+ m_decodeImageResult);
						m_checkResultHandlerTimer.postDelayed(this,
								m_checkResultTime);
					}
				}
			}
		}
	};

	private final Runnable m_slideShowTimerCb = new Runnable() {
		public void run() {
			Log.e(TAG, "m_slideShowTimerCb:!!!");

			if (mActivityPauseFlag == 1)
				return;
			if (m_startdecode == false || m_pPictureKit == null) {
				Log.e(TAG, "Not Decode!!!");
				m_checkResultHandlerTimer.postDelayed(m_slideShowTimerCb, 100);
			} else if (m_decodeImageState == DecodeImageState.STATE_DECODEING) {
				Log.e(TAG, "m_slideShowTimerCb: Decoding now !!!");
				// Log.e(TAG,"m_slideShowTimerCb: Decoding now !!!");
				m_checkResultHandlerTimer.postDelayed(m_slideShowTimerCb, 100);
				// m_slideShowHandlerTimer.postDelayed(m_slideShowTimerCb,
				// m_slideShowTime);
				// m_checkResultHandlerTimer.removeCallbacks(this); //put this
				// in onDestory
			} else {
				if (m_decodeImageState != DecodeImageState.STATE_DECODEING) {
					Log.e(TAG, "m_slideShowTimerCb:Next Image");
					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb); // put
																					// this
																					// in
																					// onDestory
					NextImage();
				}
			}
		}
	};

	private void setBannerShowtime(int time) {
		mBannerShowTime = time;
	}

	private void initLayout() {

		Log.v(TAG, "initLayout");
		mIsFullScrean = false;
		dosetbannerexif(m_currentPlayIndex);
		check_slideshow_mode();
		check_repeat_mode();
		check_zoom_model();
		mLastControlTime = (new Date(System.currentTimeMillis())).getTime();

		RelativeLayout llayout_banner_photo = (RelativeLayout) findViewById(R.id.banner_photo);
/*		 RelativeLayout.LayoutParams llayoutparams_banner_photo = new RelativeLayout.LayoutParams(
				 LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
	            );
		 llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		 llayoutparams_banner_photo.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	     llayoutparams_banner_photo.width=mDisplayMetrics.widthPixels;
	     llayoutparams_banner_photo.height=0;//set the height to 109 after RTKAtvView is finished coding

	     llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);
*/
		 llayout_banner_photo.setVisibility(View.VISIBLE);
/*		 
	     LinearLayout l_atvview=(LinearLayout)findViewById(R.id.RtkAtvView_LinearLayout);
	     RelativeLayout.LayoutParams llayout_params_atvview = new RelativeLayout.LayoutParams(
	            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	     llayout_params_atvview.width=mDisplayMetrics.widthPixels;
	     llayout_params_atvview.height=mDisplayMetrics.heightPixels;//-llayoutparams_banner_photo.height;//- rlayout_params_title.height; 
	     l_atvview.setLayoutParams(llayout_params_atvview);

	     SurfaceView sv=(SurfaceView)findViewById(R.id.m_photoPlaybackSurfaceView);
	     LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) sv.getLayoutParams();
	     llp.width	= mDisplayMetrics.widthPixels;
	     llp.height = mDisplayMetrics.heightPixels-llayoutparams_banner_photo.height;//- rlayout_params_title.height;
	     llp.weight = 0;
	     mPhotoPlaybackView.getHolder().setFixedSize(llp.width,llp.height); 
	     mPhotoPlaybackView.setLayoutParams(llp); 
*/	        
		banner_slideshow.setFocusable(true);
		banner_slideshow.setFocusableInTouchMode(true);
		banner_slideshow.requestFocus();

		/*
		 * set banner height to 109 AFTER RTKAtvView height is to screen height
		 * in order to make sure the height of RTKAtvView take effect;if set
		 * banner heitht to 109 BEFORE set RTKAtvView height to screen heightthe
		 * height of RTKAtvView will become as screen height minus banner
		 * height( 720 - 109=611)why that happens?
		 */
		 /*
		if(mMediaApplicationMap.getScale() == 1.5){
			llayoutparams_banner_photo.height=150;
		}else if(mMediaApplicationMap.getScale() == 2){
			llayoutparams_banner_photo.height=150;
		}
		llayout_banner_photo.setLayoutParams(llayoutparams_banner_photo);
		*/
	}

	class QuickMenuPhotoAdapter extends BaseAdapter {
		public View LastSelectedItem_View = null;
		int[] menu_name = new int[] { R.string.quick_menu_picture_mode,
				R.string.quick_menu_photo_intervalTime,
				R.string.quick_menu_repeat, R.string.quick_menu_sleep,
				R.string.quick_menu_tvapp, R.string.quick_menu_sysset, };

		private LayoutInflater mInflater;

		class ViewHolder {
			TextView menu_name;
			ImageView left;
			TextView menu_option;
			ImageView right;
		}

		public QuickMenuPhotoAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return menu_name.length;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				if (true == RTKDMPConfig
						.getRight2Left(getApplicationContext()))
					convertView = mInflater.inflate(R.layout.quick_list_row_a,
							null);
				else
					convertView = mInflater.inflate(R.layout.quick_list_row,
							null);
				holder = new ViewHolder();
				holder.menu_name = (TextView) convertView
						.findViewById(R.id.menu_name);
				holder.menu_option = (TextView) convertView
						.findViewById(R.id.menu_option);
				holder.left = (ImageView) convertView
						.findViewById(R.id.left_arrow);
				holder.right = (ImageView) convertView
						.findViewById(R.id.right_arrow);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.menu_name.setText(menu_name[position]);

			switch (position) {
			case 0: {
				holder.menu_option.setText(null);
				break;
			}
			case 1: {
				holder.menu_option.setText(mIntervalTimeStatus[mIntervalIndex]);
				break;
			}
			case 2: {
				holder.menu_option.setText(repeats[mRepeatIndex]);
				break;
			}
			case 3: {
				SimpleDateFormat df_ori = new SimpleDateFormat("HH mm");
				SimpleDateFormat df_des = new SimpleDateFormat("HH:mm");
				java.util.Date date_parse = null;
				try {
					date_parse = df_ori.parse(mSleepTimeHour + " "
							+ mSleepTimeMin);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String timeFormat = df_des.format(date_parse);
				holder.menu_option.setText(timeFormat);
				break;
			}
			case 4: {
				break;
			}
			case 5: {
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
}
