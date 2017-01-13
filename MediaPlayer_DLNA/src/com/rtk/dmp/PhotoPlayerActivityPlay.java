package com.rtk.dmp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnKeyListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
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
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;

import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5.DeviceStatusListener;
import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.DateStringFormat;
import com.realtek.Utils.DensityUtil;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.realtek.bitmapfun.util.CommonActivityWithImageWorker;
import com.realtek.bitmapfun.util.ImageCache;
import com.realtek.bitmapfun.util.ImageFetcher;
import com.realtek.bitmapfun.util.ImageWorker;
import com.realtek.bitmapfun.util.LoadingControl;
import com.realtek.bitmapfun.util.ReturnSizes;
import com.rtk.dmp.DecodeImageState;
import com.rtk.dmp.DecoderInfo;
import com.rtk.dmp.ImagePlaylist;
import com.rtk.dmp.PictureKit;

import android.widget.LinearLayout.LayoutParams;
import android.app.Activity;
import android.app.TvManager;

import com.rtk.dmp.MediaApplication;
import com.rtk.dmp.PhotoPlayerActivityPause.GridViewLoadingControl;
public class PhotoPlayerActivityPlay extends CommonActivityWithImageWorker implements Observer
{
	private final static int MSG_SET_REPEAT     = 19;
	private final static int MSG_SET_INTERVAL   = 20;
	private final static int MSG_REFRESH_TIMER  = 21;
	private static final String DATAFORMAT = "HH:mm EEE,dd MMM yyyy"; 

	private MediaApplication mMediaApplicationMap = null;
	private ArrayList<DLNAFileInfo> mPhotoList;
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
	//private int mRepeatMode_play = 1;

	private static final String TAG = "PhotoPlaybackActivityPlay";
	private static PictureKit m_pPictureKit = null;
	private int nowClockDecodePicKit = 0;
	private int nowClockCallBack = 0;

	private static boolean m_startdecode = false;
	private static int m_decodeImageState = DecodeImageState.STATE_NOT_DECODED;
	private static int m_decodeImageResult = DecodeImageState.STATE_DECODE_DONE;

	private int m_initPos = 0;
	private int m_currentPlayIndex = 0;
	private int m_totalCnt = 0;
	private int dirsize = 0;
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
    
    private boolean mIsSlideShowModel  = true;
    private int mIsZoomModel		   = 0;
  //player zoom mode
  	private int mZoomMode = 0;
  	private final static int ZOOMMODE_NORMAL = 1;
  	private final static int ZOOMMODE_INX2   = 2;
  	private final static int ZOOMMODE_INX4   = 4;
  	private final static int ZOOMMODE_INX8   = 8;
  	private final static int ZOOMMODE_INX16  = 16;
  	
  	//player rotate mode
  	private int mRotateMode = ROTATEMODE_0D;
  	private final static int ROTATEMODE_0D = 0;
  	private final static int ROTATEMODE_90D = 90;
  	private final static int ROTATEMODE_180D = 180;
  	private final static int ROTATEMODE_270D = 270;
    
    private QuickMenu mQuickmenu=null;
	private QuickMenuPhotoAdapter mQuickmenuAdapter=null;
	private ImageView btn_menu = null;
	ListView quickMenuContent = null;
	Thread mRefreshSleeperTimer = null;
	
	private String[] mIntervalTimeStatus = new String[3];
	private String[] repeats = new String[2];
	
	int [] mIntervalContent = { 3, 5, 10 };
	private int mIntervalIndex = 0;
	private int mIntervalTime = 3;
	private int mSleepTimeHour = 0, mSleepTimeMin = 0;
	
	private int mRepeatIndex  = 0;
	private PopupMessage msg_hint = null;
	
	private SharedPreferences mPerferences = null;

	private TvManager mTv;

	private int mDecodeRetryTimes = 0;

	private int mActivityPauseFlag = 0;
	private int mActivityDestroyFlag = 0;

 	private boolean isPlayerButtonListVisible = false;
 	
 	private ReturnSizes mReturnSizes;
	
	ImageCache mCache_small;
	private ImageButton play_btn   = null;
	private ImageButton repeat_btn = null;
	private ImageButton zoom_btn   = null;
	private ImageButton rotate_btn = null;
	
	private int repeat_btn_statu = -1;
	private final static int REPEAT_OFF_BTN_NOTFOCUS = 0;
	private final static int REPEAT_OFF_BTN_FOCUS = 1;
	private final static int REPEAT_ON_BTN_NOTFOCUS = 2;
	private final static int REPEAT_ON_BTN_FOCUS = 3;
	
	//player repeat mode
		private int mRepeatMode = REPEATMODE_OFF;
		private final static int REPEATMODE_ON  = 0;
		private final static int REPEATMODE_OFF = 1;
	
		
	//for gallery
	private HorizontalScrollView gallery; 
	private RelativeLayout controlbar_photoplayer = null;
	private RelativeLayout item_gallery;
	private int hsv_width;  
	private int child_count;  
	private int child_width;  
	private int child_show_count;  
	private int child_start;  //first show middle item num
	private int last_item =0;
	private boolean noScrollGallery = false;
	final float[] array_null = {1,0,0,0,0, 0,1,0,0,0, 0,0,1,0,0, 0,0,0,1,0};
    final float[] array_grey = {0.5f,0,0,0,58.2f, 0,0.5f,0,0,58.2f, 0,0,0.5f,0,58.2f, 0,0,0,0.5f,58.2f};
    private ColorMatrix colorMatrix = new ColorMatrix(); 
	private int pos_focus = -1;
	
	
	
	
	
	//for surfaceView
	private float rate = 1;
	private float oldRate = 1;
	private boolean isFirst = true;
	private boolean canDrag = false;
	private boolean canRecord = true;
	private boolean canSetScaleCenter = true;
	float oldLineDistance = 0f;
	float oldDistanceX = 0f;
	float oldDistanceY = 0f;
	float moveX = 0f,moveY = 0f;
	float startPointX = 0f, startPointY = 0f;
	boolean disableMove = false;
	boolean hasTranslate = false;
	
	//
	private final static int RESULT_PHOTOPLAY = 100;
	private final static int RESULT_VAL = 101;
	
	private GestureDetector  gestureScanner;
	
	private boolean isFirstEnter = true;
	
	private boolean hasPliSharedMemory = false;
	private int[] vAddrForDTCP;
	
	private int oriention = -1;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "kelly onCreate:");
		super.onCreate(savedInstanceState);

//		if (true == RTKDMPConfig.getRight2Left(getApplicationContext()))
//			setContentView(R.layout.photosingleview_a);
//		else
			setContentView(R.layout.photoplayer_gallary_play);

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


		mMediaApplicationMap = (MediaApplication) getApplication();
		// mDataProvider = map.getPhotoDataProvider();
		mPhotoPlaybackView = (SurfaceView) findViewById(R.id.picture_focused);

		getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);


		mCheckTimerHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					setbanner();
					break;
				case 1:
					if (msg_hint.isShowing()||mActivityPauseFlag !=1) {
						msg_hint.dismiss();
					}
					break;
				case 2:
					if (msg_notavaible.isShowing()) {
						msg_notavaible.dismiss();
					}
					break;
				case 3:
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

		mTvawFilter = new IntentFilter();
		mTvawFilter.addAction("com.rtk.mediabrowser.PlayService");
		mTvawReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				if (intent.getStringExtra("action").equals("PAUSE")) {
					m_checkResultHandlerTimer
							.removeCallbacks(m_checkResultTimerCb);
					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);
				} else if (intent.getStringExtra("action").equals("PLAY")) {
					if (m_totalCnt > 0) {
						m_checkResultHandlerTimer.postDelayed(
								m_checkResultTimerCb, m_checkResultTime);
					}
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
		mRepeatMode = intent.getIntExtra("repeat",1);

		repeat_btn_statu = mRepeatMode == REPEATMODE_OFF?REPEAT_OFF_BTN_NOTFOCUS:REPEAT_ON_BTN_NOTFOCUS;
		
		mRotateMode = intent.getIntExtra("rotate", 0);
		
		if (mIsFromAnywhere == false) {
			if (mPhotoList == null) 
			{
				mPhotoList = mMediaApplicationMap.getFileList();
			}
			int photoListSize = mPhotoList.size();
			mPhotoDirNum = photoListSize;
			m_filePathStrArray = new String[photoListSize];
			{
				int tmpj = 0;
				if (m_totalCnt > 0) {
					// get filePathArrayStr
					for (int i = 0; i < photoListSize; i++) {
						if (mPhotoList.get(i).getFileType() == FileFilterType.DEVICE_FILE_PHOTO) {
							
							m_filePathStrArray[tmpj] = mPhotoList.get(i).getFilePath();
							tmpj++;
						}
						else
						{
							dirsize ++;
						}
					}
				}
				mPhotoDirNum = m_filePathStrArray.length - tmpj;
			}
		} else {
			m_filePathStrArray = intent.getStringArrayExtra("filelist");
			m_totalCnt = m_filePathStrArray.length;
			mPhotoDirNum = 0;
			registerReceiver(mTvawReceiver, mTvawFilter);
		}
		// mRepeatIndex = intent.getIntExtra("repeatIndex", 0);
		// mRepeatMode = mRepeatIndex;

		
		//check if dtcp photo.
		for (int i = 0; i < m_filePathStrArray.length - dirsize; i++) {
			if(hasPliSharedMemory ==false)
			{
				String filePath = m_filePathStrArray[i];
				String tailStr = null;
	    		if(filePath.contains(" "))
	    		{
	    			tailStr = filePath.substring(filePath.indexOf(" "));
	    		}

    			if(tailStr != null&&tailStr.contains("protocolinfo"))
    			{
    				vAddrForDTCP = mMediaApplicationMap.getAddrForDTCP();
    				/*if(vAddrForDTCP[0]==-1||vAddrForDTCP[0]==-1)
    		        {
    					vAddrForDTCP = mTv.startDecodeDtcpImageFile(64*1024);
    					mMediaApplicationMap.setAddr(vAddrForDTCP);
    		        }*/
    				hasPliSharedMemory = true;
    				
    			}
				
			}
		}
		
		
		
		
		if (m_initPos < 0 && m_initPos > m_totalCnt -dirsize - 1) {
			m_initPos = 0;
		}

		if(savedInstanceState!=null)
		{
			
			int saveIndex = savedInstanceState.getInt("saveIndex");
			m_currentPlayIndex = saveIndex;
		}
		else
		{
			m_currentPlayIndex = m_initPos;
		}
		
	    oriention = getDisplayRotation(this);
		
		initQuickMenu();


		short_msg = new ConfirmMessage(PhotoPlayerActivityPlay.this, 678, 226);

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
		
		play_btn = (ImageButton)findViewById(R.id.play_btn);
		repeat_btn = (ImageButton)findViewById(R.id.repeat_btn);
		zoom_btn = (ImageButton)findViewById(R.id.info_zoom);
		rotate_btn = (ImageButton)findViewById(R.id.rotate_btn);
		
		if(mRepeatMode == REPEATMODE_ON)
		{
			repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_on_icon_n);
		}
		else if(mRepeatMode == REPEATMODE_OFF)
		{
			repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_off_icon_n);
		}
		mCache_small = ImageCache.createCache(this,"images");

	//	picture_full = (ImageView)findViewById(R.id.picture_focused);
		controlbar_photoplayer = (RelativeLayout)findViewById(R.id.controlbar_photoplayer);
		controlbar_photoplayer.getBackground().setAlpha(50);
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		
	}
	
    private void initQuickMenu() {
		// TODO Auto-generated method stub
    	// InitQuickMenu
    			mQuickmenuAdapter = new QuickMenuPhotoAdapter(this);
    			mQuickmenu = new QuickMenu(this, mQuickmenuAdapter);
    			mQuickmenu.setAnimationStyle(R.style.QuickAnimation);

    			final ListView quickMenuContent = mQuickmenu.getListView();
    			quickUIhandler = new Handler() {
    				public void handleMessage(Message msg) {
    					switch (msg.what) {

    					case MSG_SET_REPEAT: {

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
    										PhotoPlayerActivityPlay.this);
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
	}

	public static int getDisplayRotation(Activity activity) {  
        if(activity == null)  
            return 0;  
          
        int rotation = activity.getWindowManager().getDefaultDisplay()  
                .getRotation();  
        switch (rotation) {  
        case Surface.ROTATION_0:  
            return 0;  
        case Surface.ROTATION_90:  
            return 1;  
        case Surface.ROTATION_180:  
            return 2;  
        case Surface.ROTATION_270:  
            return 3;  
            
        }  
        return -1;  
    } 
	
	public class GridViewLoadingControl implements LoadingControl
    {
    	@Override
    	public void startLoading(int pos) {
    		// TODO Auto-generated method stub

    		ImageView content_bg = new ImageView(PhotoPlayerActivityPlay.this);   
    		content_bg.setTag(pos+"bg");

            RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams
    				(child_width, ViewGroup.LayoutParams.MATCH_PARENT); 
            
            if(pos==0)
            {
            	lp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    			lp1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            }
            else
            {
            	lp1.addRule(RelativeLayout.RIGHT_OF,pos);
            	lp1.addRule(RelativeLayout.ALIGN_TOP,pos);
            }
  	      	content_bg.setBackgroundResource(R.drawable.loadingicon);
	  	    Animation mAnimLoading = AnimationUtils.loadAnimation(
	  	    		PhotoPlayerActivityPlay.this, R.drawable.anim);
	        content_bg.setAnimation(mAnimLoading);
	  	    content_bg.getAnimation().startNow();
	  	    item_gallery.addView(content_bg,lp1); 
    	}

    	@Override
    	public void stopLoading(int pos,boolean isfromoncancel) {
    		// TODO Auto-generated method stub

        	ImageView imageView = (ImageView)gallery.findViewWithTag(pos+"bg");
        	if(imageView == null)
        	{
        		return;
        	}
        	else if(imageView.getAnimation() == null)
        	{
        		return;
        	}
        	
    		imageView.getAnimation().reset();
  	        imageView.getAnimation().cancel();
  	        imageView.setBackgroundResource(0);
  	        item_gallery.removeView(imageView);           	
    	}    	
    }
    public  GridViewLoadingControl gallery_loadingcontrol=new GridViewLoadingControl(); 
	
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
	
			}
		} else {
		
		}

		RelativeLayout llayout_banner_photo = (RelativeLayout) findViewById(R.id.controlbar_photoplayer);
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

		ExifInterface exif = null;
		try {
			exif = new ExifInterface(m_filePathStrArray[position]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String title = null;
		if (mIsFromAnywhere == false) {
			title = mPhotoList.get(position + mPhotoDirNum).getFileName();// mDataProvider.GetTitleAt(position);
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
	}

	private void setbanner()
	{
		if(mIsFullScrean)
		{
			controlbar_photoplayer.setVisibility(View.VISIBLE);
			mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
		}
		else
		{
			controlbar_photoplayer.setVisibility(View.INVISIBLE);
		}
		
		mIsFullScrean = !mIsFullScrean;
	}
	protected void dofullscrean() {
		// TODO Auto-generated method stub


		dosetmoveicon();
		RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.controlbar_photoplayer);

		llayout_banner_photo.setVisibility(View.INVISIBLE);
		llayout_banner_photo.setLayoutParams(llayout_banner_photo.getLayoutParams());
        
        mIsFullScrean = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		finishThd = true;
		mActivityDestroyFlag = 1;

		mMediaApplicationMap.deleteObserver(this);

		if (m_pPictureKit != null && showInBackground) {
			m_startdecode = false;
			m_pPictureKit.stopPictureKit();
			m_pPictureKit = null;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume:");
		mActivityPauseFlag = 0;
		initLayout();
		if (mQuickmenu.isShowing()) {
			mQuickmenu.setIsActivityPause(mActivityPauseFlag);
			mQuickmenu.setTimeout();
		}

		if (m_startdecode == false || m_pPictureKit == null) {
			StartPictureKit();
			if (m_totalCnt -dirsize  > 0) {
				System.out.println("onresume comto post");
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

		mIsSlideShowModel = true;
		check_slideshow_mode();
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
						quickUIhandler.sendEmptyMessage(PhotoPlayerActivityPlay.MSG_REFRESH_TIMER);
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
		
		play_btn.setBackgroundResource(R.drawable.dnla_stop_icon_n);
		play_btn.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				// TODO Auto-generated method stub
				
				play_btn.setBackgroundResource(R.drawable.dnla_stop_icon_f);
				switch (event.getAction()) {   
	                case MotionEvent.ACTION_UP: 

	                	Bundle bundle = new Bundle();
	    				bundle.putInt("playindex", m_currentPlayIndex);
	    				bundle.putInt("repeat", mRepeatMode);
	    				bundle.putInt("rotate", mRotateMode);
	    				Intent it = new Intent();  
	    		        it.putExtras(bundle);  
	    		        setResult(RESULT_VAL, it); 
	    				PhotoPlayerActivityPlay.this.finish();

	                break;
				 }
				 return true;
			}
		});

		repeat_btn.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				// TODO Auto-generated method stub
				mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
				if(mRepeatMode == REPEATMODE_ON&&
						(repeat_btn_statu ==REPEAT_ON_BTN_NOTFOCUS||repeat_btn_statu ==REPEAT_OFF_BTN_NOTFOCUS))
				{
					repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_on_icon_f);
					repeat_btn_statu = REPEAT_ON_BTN_FOCUS;
				}
				else if(mRepeatMode == REPEATMODE_OFF&&
					(repeat_btn_statu ==REPEAT_ON_BTN_NOTFOCUS||repeat_btn_statu ==REPEAT_OFF_BTN_NOTFOCUS))
				{
					repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_off_icon_f);
					repeat_btn_statu = REPEAT_OFF_BTN_FOCUS;
				}
				 switch (event.getAction()) {   
	                case MotionEvent.ACTION_UP: 
	                	if(mRepeatMode == REPEATMODE_ON)
						{
							repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_off_icon_n);
							repeat_btn_statu = REPEAT_ON_BTN_NOTFOCUS;
						}
						else if(mRepeatMode == REPEATMODE_OFF)
						{
							repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_on_icon_n);
							repeat_btn_statu = REPEAT_OFF_BTN_NOTFOCUS;
						}
	                	mRepeatMode = mRepeatMode==0?1:0;


	                break;
				 }
				 return true;
			}
		});
		zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_1_f);
		zoom_btn.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				// TODO Auto-generated method stub
				int divide = arg0.getWidth()/5;
				if(event.getX()>0 && event.getX()<divide)
				{
					zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_1_f);
					mZoomMode = ZOOMMODE_NORMAL;
				}
				else if(event.getX()>divide && event.getX()<divide*2)
				{
					zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_2_f);
					mZoomMode = ZOOMMODE_INX2;
				}
				else if(event.getX()>divide*2 && event.getX()<divide*3)
				{
					zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_4_f);
					mZoomMode = ZOOMMODE_INX4;
				}
				else if(event.getX()>divide*3 && event.getX()<divide*4)
				{
					zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_8_f);
					mZoomMode = ZOOMMODE_INX8;
				}
				else if(event.getX()>divide*4 && event.getX()<divide*5)
				{
					zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_16_f);
					mZoomMode = ZOOMMODE_INX16;
				}
				if(!(event.getX()>0 && event.getX()<divide))
				{
					Bundle bundle = new Bundle();
					bundle.putInt("playindex", m_currentPlayIndex);
					bundle.putInt("repeat", mRepeatMode);
					bundle.putInt("zoommode",mZoomMode);
					bundle.putInt("rotate", mRotateMode);
					//bundle.putFloat("scalerate", rate);
					//return rate through bundle.
					Intent it = new Intent();  
					it.putExtras(bundle);  
					setResult(RESULT_VAL, it); 
					finish();
					return true;
				}
					
				return false;
			}
		
		});
		
		rotate_btn.setBackgroundResource(R.drawable.dnla_rotete_icon_n);
		rotate_btn.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				// TODO Auto-generated method stub
				mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
				rotate_btn.setBackgroundResource(R.drawable.dnla_rotete_icon_f);
				switch (event.getAction()) {   
	                case MotionEvent.ACTION_UP: 
	                {
	                	mRotateMode = mRotateMode + 90 > ROTATEMODE_270D ? ROTATEMODE_0D : mRotateMode + 90;
	                	rotate_btn.setBackgroundResource(R.drawable.dnla_rotete_icon_n);
	                	
	                	mTv.rightRotate();

	    				// pause ,then play,to simulate reseting inteval time
	    				m_checkResultHandlerTimer
	    							.removeCallbacks(m_checkResultTimerCb);
    					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);

    					if (m_totalCnt -dirsize  > 0) {
    						m_checkResultHandlerTimer.postDelayed(
    								m_checkResultTimerCb, m_checkResultTime);
    					}
    					mIntervalIndex = 1;
    					
    					mIntervalTime = mIntervalContent[mIntervalIndex];

    					m_slideShowTime = mIntervalTime*1000;
    					new Thread(new Runnable() {
    						@Override
    						public void run() {
    							Editor editor = mPerferences.edit();//
    							editor.putInt("intervalIndex_photo", mIntervalIndex);
    							editor.commit();
    						}
    					}).start();
	                }

	                break;
				 }
				 return true;
			}
		});
		
		btn_menu = (ImageView) findViewById(R.id.btn_menu);
		btn_menu.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Bundle bundle = new Bundle();
				bundle.putInt("playindex", m_currentPlayIndex);
				bundle.putInt("repeat", mRepeatMode);
				bundle.putInt("rotate", mRotateMode);
				bundle.putBoolean("menu", true);
				Intent it = new Intent();  
		        it.putExtras(bundle);  
		        setResult(RESULT_VAL, it); 
				PhotoPlayerActivityPlay.this.finish();
			}
			
		});
		
		gestureScanner = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
			@Override
		    public boolean onDoubleTap(MotionEvent e) {
		        //TODO ;
				
				Bundle bundle = new Bundle();
				bundle.putInt("playindex", m_currentPlayIndex);
				bundle.putInt("repeat", mRepeatMode);
				bundle.putInt("zoommode",ZOOMMODE_INX2);
				bundle.putInt("rotate", mRotateMode);
				Intent it = new Intent();  
				it.putExtras(bundle);  
				setResult(RESULT_VAL, it); 
				finish();
				return true;
		    }
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {   
				setbanner();
		        return false;   
		    } 
			
		});
		
		
		
   }
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		if(event.getPointerCount() == 1)
        {
			if(!disableMove)
			{
				if(canRecord)
	        	{
	        		startPointX = event.getX();
	        		startPointY = event.getY();
	        		canRecord = false;
	        	}
				else
	        	{
	        		float distanceX = event.getX() - startPointX;
	        		//--notice--begin
	        		//there is a copy of gallery.setonkeylistener() if modify sth here
	        		//please make sync in both side.
	        		if(distanceX > 100 && hasTranslate == false)
	        		//show left pictures(start from left,end to right)
	        		{
	//        			operation_left();
	        			resetIntervelTime();
	        			if(noScrollGallery)
	        			{
	        				PreImage();
	        				changeFocusToItem(m_currentPlayIndex);
	        			}
	        			else if(mRepeatMode == REPEATMODE_ON)
	        			{
		        			PreImage();
		        			int focused_item = m_currentPlayIndex+1;
		        			if (focused_item-3 <= 0)
		            		{
		        				focused_item = focused_item + child_count;
		            		}
		        			else if (focused_item + 3 > child_count * 2 - 1)
		            		{
		            			focused_item = focused_item - child_count;
		            		}
		        			
			        		gallery.smoothScrollTo(child_width  
			        				* focused_item - child_width / 2 - hsv_width / 2,  
			                        gallery.getScrollY());  
	
			        		changeFocusToItem(focused_item-1);
							
		        			for(int i = focused_item- 3;i<= focused_item+3;i++)
		        			{
		        				int tag = i-child_count*2>0?(i-child_count*2):(i-1);
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        			}
	        			else if(mRepeatMode == REPEATMODE_OFF)
	        			{
	        				PreImage();
	        				
	        				int focused_item = m_currentPlayIndex+1;
	        				int index_from,index_to;
		        			if (focused_item-3 <= 0)
		            		{
		        				index_from = 1;
		        				index_to = 7;
		        				gallery.smoothScrollTo(0,  
				                        gallery.getScrollY()); 
		            		}
		        			else if (focused_item + 3 > child_count - 1)
		            		{
		        				index_from = child_count - 6;
		        				index_to = child_count;
		        				gallery.smoothScrollTo(child_width  
				        				* child_count - 7*child_width,  
				                        gallery.getScrollY()); 
		            		}
		        			else
		        			{
		        			
		        				index_from = focused_item - 3;
		        				index_to = focused_item + 3;
				        		gallery.smoothScrollTo(child_width  
				        				* focused_item - child_width / 2 - hsv_width / 2,  
				                        gallery.getScrollY()); 
		        			}
		        			
			        		
			        		
			        		changeFocusToItem(focused_item-1);
							
		        			for(int i = index_from;i<= index_to;i++)
		        			{
		        				int tag = i-1;
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        				
	        			}
		        		hasTranslate = true;
	        		}
	        		else if(distanceX < -100 && hasTranslate == false)
	        		//show right pictures(start from right,end to left)
	        		{
	        			resetIntervelTime();
	        			if(noScrollGallery)
	        			{
	        				NextImage();
	        				changeFocusToItem(m_currentPlayIndex);
	        			}
	        			else if(mRepeatMode == REPEATMODE_ON)
	        			{
		        			NextImage();
		        			int focused_item = m_currentPlayIndex+1;
		        			
		        			if (focused_item-3 <= 0)
		            		{
		        				focused_item = focused_item + child_count;
		            		}
		        			else if (focused_item + 3 > child_count * 2 - 1)
		            		{
		            			focused_item = focused_item - child_count;
		            		}
	
		        			gallery.smoothScrollTo(child_width  
		        					* focused_item - child_width / 2 - hsv_width / 2,  
		        					gallery.getScrollY());
		        			
		        			changeFocusToItem(focused_item-1);
		        			
		        			for(int i = focused_item- 3;i<= focused_item+3;i++)
		        			{
		        				int tag = i-child_count*2>0?(i-child_count*2):(i-1);
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        			}
	        			else if(mRepeatMode == REPEATMODE_OFF)
	        			{
	        				NextImage();
	        				
	        				int focused_item = m_currentPlayIndex+1;
	        				int index_from,index_to;
		        			if (focused_item-3 <= 0)
		            		{
		        				index_from = 1;
		        				index_to = 7;
		        				gallery.smoothScrollTo(0,  
				                        gallery.getScrollY()); 
		            		}
		        			else if (focused_item + 3 > child_count - 1)
		            		{
		        				index_from = child_count - 6;
		        				index_to = child_count;
		        				gallery.smoothScrollTo(child_width  
				        				* child_count - 7*child_width,  
				                        gallery.getScrollY()); 
		            		}
		        			else
		        			{
		        			
		        				index_from = focused_item - 3;
		        				index_to = focused_item + 3;
				        		gallery.smoothScrollTo(child_width  
				        				* focused_item - child_width / 2 - hsv_width / 2,  
				                        gallery.getScrollY()); 
		        			}
		        			
			        		
			        		
			        		changeFocusToItem(focused_item-1);
							
		        			for(int i = index_from;i<= index_to;i++)
		        			{
		        				int tag = i-1;
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(i-1));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        				
	        			}
	        			hasTranslate = true;
	        		}
	        		//--notice--end
	        	}
					
				
			}
        }
		
		if (event.getPointerCount() > 1) {
			disableMove = true;
			
			if (event.getPointerCount() == 2) {
				
				if (isFirst) {

					oldLineDistance = (float) Math.sqrt(Math.pow(event.getX(1) - event.getX(0), 2)
							+ Math.pow(event.getY(1) - event.getY(0), 2));
					isFirst = false;
				} else {

					float newLineDistance = (float) Math.sqrt(Math.pow(event.getX(1) - event.getX(0), 2)
							+ Math.pow(event.getY(1) - event.getY(0), 2));

					rate = oldRate * newLineDistance / oldLineDistance;
					if(rate >1)
					{
						Bundle bundle = new Bundle();
						bundle.putInt("playindex", m_currentPlayIndex);
						//bundle.putInt("repeat", mRepeatMode);
						bundle.putFloat("scalerate", rate);
						//return rate through bundle.
						bundle.putInt("rotate", mRotateMode);
						Intent it = new Intent();  
						it.putExtras(bundle);  
						setResult(RESULT_VAL, it); 
						finish();
						return true;
					}

				}
			}
		}
		
        switch (event.getAction())
        {  
	        case MotionEvent.ACTION_DOWN: 
	        	break;
	        case MotionEvent.ACTION_MOVE:
	            break;  
	        case MotionEvent.ACTION_UP:
	        {
	        	isFirst = true;
	        	canRecord = true;
				canDrag = true;
				oldRate = rate;
				canSetScaleCenter =true;
				oldDistanceX = moveX;
				oldDistanceY = moveY;
				if(event.getPointerCount() == 1)
		        {
					if(disableMove == true)
					{
						disableMove = false;
					}
		        }
				hasTranslate = false;
				break;
	        }
        }

        return gestureScanner.onTouchEvent(event); 
	}
	
	
    private void resetIntervelTime() {
		// TODO Auto-generated method stub
    	mIntervalIndex = 1;
		mIntervalTime = mIntervalContent[mIntervalIndex];
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
		
	}

	public void SetBannerSize()
    {
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
	public void onBackPressed() {
	    super.onBackPressed();
	    Bundle bundle = new Bundle();
		bundle.putInt("playindex", m_currentPlayIndex);
		bundle.putInt("repeat", mRepeatMode);
		bundle.putInt("rotate", mRotateMode);
		Intent it = new Intent();  
        it.putExtras(bundle);  
        setResult(RESULT_VAL, it); 
	}
	
	public void cancelDecode(ImageView view)
    {
        if(view !=null)
        {    
            ImageWorker.cancelWork(view);
        }
    }  

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mActivityPauseFlag = 1;
		
		for(int i = 0; i < 2*child_count; i++) 
	    {
	      	ImageView iv = (ImageView)gallery.findViewWithTag(i);
	        cancelDecode(iv);
	    }
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
		Log.d(TAG, "photoactivityplay onKeyUp : " + event.toString());

		if (keyCode == KeyEvent.KEYCODE_M || keyCode == KeyEvent.KEYCODE_INFO) {
			mBannerShowTime = 60000;
				if(mIsFullScrean)
				{
					dosetmoveicon();
					
					RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.controlbar_photoplayer);
							 llayout_banner_photo.setVisibility(View.VISIBLE);
				    llayout_banner_photo.setLayoutParams(llayout_banner_photo.getLayoutParams());
				}		   			
				else
				{
					dosetmoveicon();
					
					RelativeLayout llayout_banner_photo=(RelativeLayout)findViewById(R.id.controlbar_photoplayer);
					llayout_banner_photo.setVisibility(View.INVISIBLE);
					llayout_banner_photo.setLayoutParams(llayout_banner_photo.getLayoutParams());
					
					
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

			check_repeat_mode();
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
				if(DMSName.size() != 0){
					Intent mIntent = new Intent(this, GridViewActivity.class);
					mIntent.putStringArrayListExtra("DMSName", DMSName);
					setResult(ResultCodeForDMSCLosed, mIntent);
				}else{
					Bundle bundle = new Bundle();
					bundle.putInt("playindex", m_currentPlayIndex);
					bundle.putInt("repeat", mRepeatMode);
					bundle.putInt("rotate", mRotateMode);
					Intent it = new Intent();  
			        it.putExtras(bundle);  
			        setResult(RESULT_VAL, it); 
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
				msg_notavaible = new Message_not_avaible(PhotoPlayerActivityPlay.this);
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
		Log.d(TAG, "photoactivityplay onKeyDown : " + event.toString());
		mLastControlTime = (new Date(System.currentTimeMillis())).getTime();

		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
				mTv.rightRotate();

				// pause ,then play,to simulate reseting inteval time
				if (mIsSlideShowModel) {
					m_checkResultHandlerTimer
							.removeCallbacks(m_checkResultTimerCb);
					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);

					if (m_totalCnt -dirsize  > 0) {
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

					if (m_totalCnt -dirsize  > 0) {
						m_checkResultHandlerTimer.postDelayed(
								m_checkResultTimerCb, m_checkResultTime);
					}
				}
				// end
			} else {
				mTv.onZoomMoveDown();
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == 234 
				// for L4300 KeyEvent.KEYCODE_NEXT
				) {
			
			if (mIsZoomModel == NOT_ZOOM_MODE) {
				/*
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
			*/
				mTv.onZoomMoveLeft();
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == 235
		// for L4300 KeyEvent.KEYCODE_NEXT
				) {
			if (mIsZoomModel == NOT_ZOOM_MODE) {
/*
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
							Bundle bundle = new Bundle();
							bundle.putInt("playindex", m_currentPlayIndex);
							bundle.putInt("repeat", mRepeatMode);
							bundle.putInt("rotate", mRotateMode);
							Intent it = new Intent();  
					        it.putExtras(bundle);  
					        setResult(RESULT_VAL, it); 
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
			*/
				mTv.onZoomMoveRight();
			}
		}

		else if (keyCode == KeyEvent.KEYCODE_Z
				|| keyCode == KeyEvent.KEYCODE_PROG_RED) {
			Bundle bundle = new Bundle();
			bundle.putInt("playindex", m_currentPlayIndex);
			bundle.putInt("repeat", mRepeatMode);
			bundle.putInt("zoommode",ZOOMMODE_INX2);
			bundle.putInt("rotate", mRotateMode);
			Intent it = new Intent();  
			it.putExtras(bundle);  
			setResult(RESULT_VAL, it); 
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_X
				|| keyCode == KeyEvent.KEYCODE_PROG_GREEN) {
			;//abandon function of green button  
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
						m_currentPlayIndex = m_totalCnt -dirsize - 1;
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
					if (m_currentPlayIndex + 1 >= m_totalCnt -dirsize) {
						m_currentPlayIndex = 0;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					} else {
						m_currentPlayIndex++;
						DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
					}
					dosetbannerexif(m_currentPlayIndex);
				} else if (mRepeatMode == 0) {

					if (m_currentPlayIndex + 1 >= m_totalCnt -dirsize) {
						Bundle bundle = new Bundle();
						bundle.putInt("playindex", m_currentPlayIndex);
						bundle.putInt("repeat", mRepeatMode);
						bundle.putInt("rotate", mRotateMode);
						Intent it = new Intent();  
				        it.putExtras(bundle);  
				        setResult(RESULT_VAL, it); 
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
			if (m_totalCnt -dirsize > 0) {
				m_checkResultHandlerTimer.postDelayed(m_checkResultTimerCb,
						m_checkResultTime);
			}
//			banner_slideshow.setImageResource(R.drawable.photo_play_on);
//			slide_single.setText(R.string.guide_single_view);
		} else if (mIsSlideShowModel == false) {
			m_checkResultHandlerTimer.removeCallbacks(m_checkResultTimerCb);
			m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);
//			banner_slideshow.setImageResource(R.drawable.photo_play_off);
//			slide_single.setText(R.string.guide_slide_show);
		}
	}

	private void check_repeat_mode() {
		if (mRepeatMode == 1) {
//			banner_repeat.setImageResource(R.drawable.photo_repeat_all_on);
		} else if (mRepeatMode == 0) {
//			banner_repeat.setImageResource(R.drawable.photo_repeat_all_off);
		}
	}

	private void check_zoom_model() {
		if (mIsZoomModel != NOT_ZOOM_MODE) {
			if (mIsSlideShowModel == true) {
				mIsSlideShowModel = false;
				check_slideshow_mode();
			}
			if (mIsZoomModel > 0) {

			}
			
			switch (mIsZoomModel) {
			
			}
		} else {

		
	

		}
		RelativeLayout llayout_banner_photo = (RelativeLayout) findViewById(R.id.controlbar_photoplayer);
		llayout_banner_photo.setLayoutParams(llayout_banner_photo
				.getLayoutParams());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public void NextImage_Play() {
		
		if (m_totalCnt -dirsize <= 0)
			return;

		if (m_currentPlayIndex < m_totalCnt -dirsize - 1) {
			m_currentPlayIndex++;

			Log.e(TAG, "NextImage m_currentPlayIndex:[" + m_currentPlayIndex
					+ "]"+m_filePathStrArray[m_currentPlayIndex]);

			DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
		} else if (mRepeatMode == 0) {
			m_currentPlayIndex = 0;
			Log.e(TAG, "NextImage m_currentPlayIndex:[" + m_currentPlayIndex
					+ "]??");
			DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
		} else if (mRepeatMode == 1) {
			Log.d(TAG, "NEXT ?? " + m_currentPlayIndex);
			Bundle bundle = new Bundle();
			bundle.putInt("playindex", m_currentPlayIndex);
			bundle.putInt("repeat", mRepeatMode);
			bundle.putInt("rotate", mRotateMode);
			bundle.putBoolean("playtoend",true);
			Intent it = new Intent();  
			it.putExtras(bundle);  
			setResult(RESULT_VAL, it); 
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
					m_pPictureKit = new PictureKit(PhotoPlayerActivityPlay.this);
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
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
	    // TODO Auto-generated method stub  
	    int saveIndex = m_currentPlayIndex;

	    outState.putInt("saveIndex", saveIndex);
	    
	    Log.v(TAG, "onSaveInstanceState,current index is :"+m_currentPlayIndex);  
	    super.onSaveInstanceState(outState);
	}   

	public void DecodePictureKit(final String Url) {
		
		synchronized( this)
    	{
    		nowClockDecodePicKit++;
    	}
		final int clock = nowClockDecodePicKit;
		
//		m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);
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
						PhotoPlayerActivityPlay.this.finish();
					}

				});

				short_msg.show();
			}
		} else if (m_pPictureKit != null) {

			final DecoderInfo di = new DecoderInfo();
			di.decodemode = 7;
			di.bUpnpFile = true;

			m_decodeImageState = DecodeImageState.STATE_DECODEING;
			m_decodeImageResult = DecodeImageState.STATE_DECODE_RESULT_INIT;
			
			new Thread(new Runnable() {
				public void run() {
					PictureKit.loadPicture(Url, di);
					Log.d(TAG, "RTK_PictureKit.loadPicture");
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
						if(clock!=nowClockDecodePicKit)
	    				{
	    					m_decodeImageState = DecodeImageState.STATE_DECODE_CANCEL;
	    					Log.v(TAG, "the clock moved, former DecodePictureKit breaks ");
	    					break;
	    				}	
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

//manully start checkresult thread									
									m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb); 
									m_slideShowHandlerTimer.postDelayed(m_slideShowTimerCb,	m_slideShowTime);
									
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
				Log.e(TAG, "m_checkFirstDecodeCb Not Decode!!!");
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
						int rotate_time =mRotateMode/90;
						for(int i =0;i<rotate_time;i++)
							mTv.rightRotate();
						check_slideshow_mode();
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
									if (msg_hint.isShowing() == false||mActivityPauseFlag ==1)
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
				if(m_decodeImageState == DecodeImageState.STATE_DECODE_CANCEL)
				{
					Log.e(TAG, "Get Result Cancel!!!");
					return;
				}
				else if (m_decodeImageState == DecodeImageState.STATE_DECODEING) {
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
								PhotoPlayerActivityPlay.this,
								m_filePathStrArray[m_currentPlayIndex]
										.substring(m_filePathStrArray[m_currentPlayIndex]
												.lastIndexOf("/") + 1)
										+ " "
										+ m_ResourceMgr
												.getString(R.string.decode_fail),
								Toast.LENGTH_SHORT).show();
						Log.e(TAG, "Decode Failed!!! Go to Next");
						if (mRepeatMode == 0) {
							if (m_currentPlayIndex + 1 >= m_totalCnt -dirsize) {
								m_currentPlayIndex = 0;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							} else {
								m_currentPlayIndex++;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							}
							dosetbannerexif(m_currentPlayIndex);
						} else if (mRepeatMode == 1) {
							if (m_currentPlayIndex + 1 >= m_totalCnt -dirsize) {
								Log.d(TAG, "RIGHT  " + m_currentPlayIndex);
								Bundle bundle = new Bundle();
								bundle.putInt("playindex", m_currentPlayIndex);
								bundle.putInt("repeat", mRepeatMode);
								bundle.putInt("rotate", mRotateMode);
								Intent it = new Intent();  
						        it.putExtras(bundle);  
						        setResult(RESULT_VAL, it); 
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
								PhotoPlayerActivityPlay.this,
								m_filePathStrArray[m_currentPlayIndex]
										.substring(m_filePathStrArray[m_currentPlayIndex]
												.lastIndexOf("/") + 1)
										+ " "
										+ m_ResourceMgr
												.getString(R.string.decode_fail),
								Toast.LENGTH_SHORT).show();
						if (mRepeatMode == 0) {
							if (m_currentPlayIndex - 1 < 0) {
								m_currentPlayIndex = m_totalCnt -dirsize - 1;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							} else {
								m_currentPlayIndex--;
								DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
							}
							dosetbannerexif(m_currentPlayIndex);
						} else if (mRepeatMode == 1) {
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
					m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb); // put this in onDestory
					
					NextImage_Play();
					NextImage_Gallery();
					
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

		RelativeLayout llayout_banner_photo = (RelativeLayout) findViewById(R.id.controlbar_photoplayer);
		llayout_banner_photo.setVisibility(View.VISIBLE);

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
	
	private void init_gsv() {
		// TODO Auto-generated method stub
		gallery= (HorizontalScrollView)findViewById(R.id.gallery);
		item_gallery=(RelativeLayout) findViewById(R.id.item_gallery);
        child_count = m_totalCnt -dirsize;  
        child_show_count = 7;   
        child_start = m_currentPlayIndex+1;  
        if(child_count<=7)
        	noScrollGallery = true;
	}

	@Override
	protected void initImageWorker() {
		// TODO Auto-generated method stub
		if(mImageWorker==null)
		{
			mReturnSizes =  new ReturnSizes(200, 100);
	        mImageWorker = new ImageFetcher(this, null, mReturnSizes.getWidth(),
	                mReturnSizes.getHeight());
	        mImageWorker.setImageCache(mCache_small);
	        mImageWorker.setImageFadeIn(false);
		}
	}
    private void isChecked(int item, boolean isChecked) {  
        ImageView imageview = (ImageView) item_gallery.getChildAt(item - 1);  
        if (isChecked) {  
    //    	imageview.setBackground(this.getResources().getDrawable(R.drawable.bar));  
        } else {  
    //    	imageview.setBackground(this.getResources().getDrawable(R.drawable.av_play)); 
        }  
    }  


    private void changeFocusToItem(int new_focus_index) {
		// TODO Auto-generated method stub
		if(pos_focus>=0)
		{
			ImageView lastFocus = (ImageView)gallery.findViewWithTag(pos_focus);
			colorMatrix.set(array_grey);
			lastFocus.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
			lastFocus.setBackgroundResource(0);
		}
		
		if(noScrollGallery)
		{
			pos_focus = new_focus_index;
		}
		else if(mRepeatMode == REPEATMODE_ON)
		{
			if (new_focus_index-3 < 0)
			{
				pos_focus = new_focus_index + child_count;
			}
			else if (new_focus_index+3 > child_count * 2-1)
			{
				pos_focus = new_focus_index - child_count;
			}
			else
			{
				pos_focus = new_focus_index;
			}
		}
		else if(mRepeatMode == REPEATMODE_OFF)
		{
			pos_focus = new_focus_index;
		}

		colorMatrix.set(array_null);
		ImageView currentFocus = (ImageView)gallery.findViewWithTag(pos_focus);
		currentFocus.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
		currentFocus.setBackgroundResource(R.drawable.dnla_thumnail_focus);
	}
    private void NextImage_Gallery()
    {
    	if(noScrollGallery)
		{
    		int focused_item = m_currentPlayIndex+1;
    		changeFocusToItem(focused_item-1);
    		return;
		}
    	if(mRepeatMode == REPEATMODE_ON)
		{
			int focused_item = m_currentPlayIndex+1;
			if (focused_item-3 <= 0)
    		{
				focused_item = focused_item + child_count;
    		}
			else if (focused_item + 3 > child_count * 2 - 1)
    		{
    			focused_item = focused_item - child_count;
    		}

			gallery.smoothScrollTo(child_width  
					* focused_item - child_width / 2 - hsv_width / 2,  
					gallery.getScrollY());
			
			changeFocusToItem(focused_item-1);
			
			for(int i = focused_item- 3;i<= focused_item+3;i++)
			{
				int tag = i-child_count*2>0?(i-child_count*2):(i-1);
				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
    			synchronized(this)
           	 	{
               		 mImageWorker.setImageSize(200, 100);
               		 mImageWorker.setImageCache(mCache_small);
               		 mImageWorker.loadImage
               		 (
               				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
               						,iv
               						,gallery_loadingcontrol
               						,tag
               						,false
               						,vAddrForDTCP
               		 );
           	 	}	
			}
		}
		else if(mRepeatMode == REPEATMODE_OFF)
		{
			
			
			int focused_item = m_currentPlayIndex+1;
			if(focused_item>child_count)
				return;
				
			int index_from,index_to;
			if (focused_item-3 <= 0)
    		{
				index_from = 1;
				index_to = 7;
				gallery.smoothScrollTo(0,gallery.getScrollY()); 
    		}
			else if (focused_item + 3 > child_count - 1)
    		{
				index_from = child_count - 6;
				index_to = child_count;
				gallery.smoothScrollTo(child_width * child_count - 7*child_width,gallery.getScrollY()); 
    		}
			else
			{
			
				index_from = focused_item - 3;
				index_to = focused_item + 3;
        		gallery.smoothScrollTo(child_width  
        				* focused_item - child_width / 2 - hsv_width / 2,  
                        gallery.getScrollY()); 
			}
			
    		
    		
    		changeFocusToItem(focused_item-1);
			
			for(int i = index_from;i<= index_to;i++)
			{
				int tag = i-1;
				ImageView iv = (ImageView)(gallery.findViewWithTag(i-1));
    			synchronized(this)
           	 	{
               		 mImageWorker.setImageSize(200, 100);
               		 mImageWorker.setImageCache(mCache_small);
               		 mImageWorker.loadImage
               		 (
               				m_filePathStrArray[i-1]
               						,iv
               						,gallery_loadingcontrol
               						,tag
               						,false
               						,vAddrForDTCP
               		 );
           	 	}	
			}
			
		}
    }
    
	private void NextImage() {
		// TODO Auto-generated method stub
		if(mRepeatMode == REPEATMODE_ON)
		{
			if(m_currentPlayIndex == m_totalCnt -dirsize - 1)
			{
				m_currentPlayIndex = 0;
				playPicture(m_currentPlayIndex);
			}
			else 
			{
				m_currentPlayIndex = m_currentPlayIndex+1;
				playPicture(m_currentPlayIndex);
			}
		}
		else if(mRepeatMode == REPEATMODE_OFF)
		{
		
			if(m_currentPlayIndex == m_totalCnt -dirsize - 1)
			{
				playPicture(m_currentPlayIndex);
			}
			else 
			{
				m_currentPlayIndex = m_currentPlayIndex+1;
				playPicture(m_currentPlayIndex);
			}
			
		}
	}
	private void PreImage() {
		// TODO Auto-generated method stub
		if(mRepeatMode == REPEATMODE_ON)
		{
			if(m_currentPlayIndex == 0)
			{
				m_currentPlayIndex = m_totalCnt -dirsize - 1;
				playPicture(m_currentPlayIndex);
			}
			else
			{
				m_currentPlayIndex = m_currentPlayIndex-1;
				playPicture(m_currentPlayIndex);
			}
		}
		else if(mRepeatMode == REPEATMODE_OFF)
		{
			if(m_currentPlayIndex == 0)
			{
				playPicture(m_currentPlayIndex);
			}
			else
			{
				m_currentPlayIndex = m_currentPlayIndex-1;
				playPicture(m_currentPlayIndex);
			}
		}
	}
	private void playPicture(int index)
	{
		
		mRotateMode = 0;
		m_currentPlayIndex = index;
		DecodePictureKit(m_filePathStrArray[m_currentPlayIndex]);
		if (mIsSlideShowModel) {
			m_checkResultHandlerTimer.removeCallbacks(m_checkResultTimerCb);
			m_slideShowHandlerTimer.removeCallbacks(m_slideShowTimerCb);

			if (m_totalCnt -dirsize > 0) {
				m_checkResultHandlerTimer.postDelayed(
						m_checkResultTimerCb, m_checkResultTime);
			}
		}
			
	}
	private void setMiddleItem(int itemTag)
	{
		boolean hasJump = false;
		if (itemTag-3 < 0) {  
        	gallery.scrollBy(child_width * child_count, 0);          	
        	itemTag += child_count;  
        	hasJump = true;
        } 
        else if (itemTag+3 > child_count * 2-1) 
        {  
        	gallery.scrollBy(-child_width * child_count, 0);  
        	itemTag -= child_count;  
        	hasJump = true;
        }  
		int current_item = itemTag+1;
		int tmpScrollX = gallery.getScrollX();
		int tmpDestScorllX = child_width * current_item - child_width / 2 - hsv_width / 2;
		
		gallery.smoothScrollTo(tmpDestScorllX,gallery.getScrollY());  
		int num =  0;
		if(tmpScrollX - tmpDestScorllX>0)
		{
			num = (tmpScrollX - tmpDestScorllX)/child_width;
			for(int i=itemTag-4+num;i >= itemTag-3;i--)// =itemTag- 4;i<=itemTag-num+1;i++)
	        {
				int tag = i;
	        	ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
	        	synchronized(this)
	       	 	{
	           		 mImageWorker.setImageSize(200, 100);
	           		 mImageWorker.setImageCache(mCache_small);
	           		 mImageWorker.loadImage
	           		 (
	           				m_filePathStrArray[i>(child_count-1)?(i-(child_count*2-1)>0?i-(child_count*2-1):i-child_count):i]
	           						,iv
	           						,gallery_loadingcontrol
	           						,tag
	           						,false
	           						,vAddrForDTCP
	           		 );
	       	 	}	
	        }
			if(hasJump)//if has jumped,decode the rest items.
			{
				for(int i=itemTag-2;i <= itemTag+3;i++)// =itemTag- 4;i<=itemTag-num+1;i++)
		        {
					int tag = i;
		        	ImageView iv = (ImageView)(gallery.findViewWithTag(i));
		        	synchronized(this)
		       	 	{
		           		 mImageWorker.setImageSize(200, 100);
		           		 mImageWorker.setImageCache(mCache_small);
		           		 mImageWorker.loadImage
		           		 (
		           				m_filePathStrArray[i>(child_count-1)?(i-(child_count*2-1)>0?i-(child_count*2-1):i-child_count):i]
		           						,iv
		           						,gallery_loadingcontrol
		           						,tag
		           						,false
		           						,vAddrForDTCP
		           		 );
		       	 	}	
		        }
				
			}
		}
		else
		{
			num = (tmpDestScorllX - tmpScrollX)/child_width;
			for(int i =itemTag + 4 - num;i<=itemTag+3;i++)
	        {
				int tag = i;
	        	ImageView iv = (ImageView)(gallery.findViewWithTag(i));
	        	synchronized(this)
	       	 	{
	           		 mImageWorker.setImageSize(200, 100);
	           		 mImageWorker.setImageCache(mCache_small);
	           		 mImageWorker.loadImage
	           		 (
	           				m_filePathStrArray[i>(child_count-1)?(i-(child_count*2-1)>0?i-(child_count*2-1):i-child_count):i]
	           						,iv
	           						,gallery_loadingcontrol
	           						,tag
	           						,false
	           						,vAddrForDTCP

	           		 );
	       	 	}	
	        }
			if(hasJump)
			{
				;//fix me
			}
		}
	}
	private void initHsvData() {
		// TODO Auto-generated method stub
		if(noScrollGallery)
		{
			for (int i = 0; i < child_count; i++) {  
	            final ImageView img_content = new ImageView(this);  
	        	colorMatrix.set(array_grey);
	        	img_content.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
	            img_content.setLayoutParams(new ViewGroup.LayoutParams(child_width,  
	                    ViewGroup.LayoutParams.MATCH_PARENT));  
	            img_content.setPadding(5, 3, 5, 3);
            	img_content.setTag(i);
            	
            	img_content.setId(i+1);
            	
            	RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams
						(child_width, ViewGroup.LayoutParams.MATCH_PARENT); ;

				lp1.addRule(RelativeLayout.RIGHT_OF,i);
				lp1.addRule(RelativeLayout.ALIGN_TOP,i);
	            	
	            item_gallery.addView(img_content,lp1); 
				synchronized(this)
				{
					 mImageWorker.setImageSize(200, 100);
					 mImageWorker.setImageCache(mCache_small);
					 mImageWorker.loadImage
					 (
							m_filePathStrArray[i]
									,img_content
									,gallery_loadingcontrol
									,i
									,false
									,vAddrForDTCP
					 );
				}	

            	final int tmpi= i;
            	img_content.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub

						changeFocusToItem(tmpi);

						m_currentPlayIndex = (Integer) img_content.getTag();
						
						playPicture(m_currentPlayIndex);
						
					}
            	});
	        }
			return;
		}
		
		for (int i = 0; i < child_count; i++) {  
            final ImageView img_content = new ImageView(this);  
        	colorMatrix.set(array_grey);
        	img_content.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            img_content.setLayoutParams(new ViewGroup.LayoutParams(child_width,  
                    ViewGroup.LayoutParams.MATCH_PARENT));  
            img_content.setPadding(5, 3, 5, 3);
        	img_content.setTag(i);
        	img_content.setId(i+1);
        	
        	RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams
					(child_width, ViewGroup.LayoutParams.MATCH_PARENT); ;
			lp1.addRule(RelativeLayout.RIGHT_OF,i);
			lp1.addRule(RelativeLayout.ALIGN_TOP,i);

            item_gallery.addView(img_content,lp1);  
            
        	final int tmpi= i;
        	img_content.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(mRepeatMode == REPEATMODE_ON)
					{
						setMiddleItem(tmpi);

					}
					else if(mRepeatMode == REPEATMODE_OFF)
					{
						if(tmpi +3 >child_count - 1)
						{
							gallery.smoothScrollTo(child_width * child_count - 7*child_width,gallery.getScrollY());
						}
						else if(tmpi-3<0)
						{
							gallery.smoothScrollTo(0,gallery.getScrollY());
						}
						else
						{
							setMiddleItem(tmpi);
						}
					}
					changeFocusToItem(tmpi);
					m_currentPlayIndex =(Integer) img_content.getTag();
					playPicture(m_currentPlayIndex);	
					
				}
        	}); 
        }
		for (int i = 0; i < child_count; i++)
		{
				final ImageView img_content = new ImageView(this);  
				img_content.setLayoutParams(new ViewGroup.LayoutParams(child_width,  
				        ViewGroup.LayoutParams.MATCH_PARENT));  
				img_content.setPadding(5, 3, 5, 3);
				colorMatrix.set(array_grey);
				img_content.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
				img_content.setTag(i+child_count);
				img_content.setId(i+child_count+1);
	        	
	        	RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams
						(child_width, ViewGroup.LayoutParams.MATCH_PARENT); ;
				lp1.addRule(RelativeLayout.RIGHT_OF,i+child_count);
				lp1.addRule(RelativeLayout.ALIGN_TOP,i+child_count);
				
	        	item_gallery.addView(img_content,lp1); 
				
				final int tmpi= i;
				img_content.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
					setMiddleItem(tmpi+child_count);

					changeFocusToItem(tmpi+child_count);
					int index = (Integer) img_content.getTag()-child_count;
					playPicture(index);

				}
			});
		}
	}
	private boolean flag_last_item = false;
	private void initHsvTouch() {
		// TODO Auto-generated method stub
		gallery.setOnTouchListener(new View.OnTouchListener() {  
			  
            private int pre_item;  
  
            @Override  
            public boolean onTouch(View v, MotionEvent event) {  
                // TODO Auto-generated method stub  
            	
            	mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
            	
            	if(noScrollGallery)
            		return true;
            		
                boolean flag = false;  
                int x = gallery.getScrollX();  
                
                int current_item = (x + hsv_width / 2) / child_width + 1;
                if(flag_last_item == false)
                {
                	last_item = current_item ;
                	flag_last_item = true;
                }
                switch (event.getAction()) {  
                case MotionEvent.ACTION_DOWN:  
                	break;
                case MotionEvent.ACTION_MOVE:  
                	System.out.println("ACTION_MOVE X="+gallery.getScrollX()+"item width="+item_gallery.getWidth());

                    flag = false; 
                    if(mRepeatMode == REPEATMODE_ON)
                    {
	                    if (x <= 0) {  
	                    	gallery.scrollBy(child_width * child_count, 0);  
	                        current_item += child_count;  
	                    } 
	                    
	                    else if (x >= (child_width * child_count * 2 - hsv_width)) 
	                    {  
	                    	gallery.scrollBy(-child_width * child_count, 0);  
	                        current_item -= child_count;  
	                    }  
                    
	                    for(int i =current_item- 3;i<=current_item+3;i++)
	                    {
	                    	int tag = i-child_count*2>0?i-child_count*2:(i-1);
	                    	ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
	                    	synchronized(this)
	                   	 	{
		                   		 mImageWorker.setImageSize(200, 100);
		                   		 mImageWorker.setImageCache(mCache_small);
		                   		 mImageWorker.loadImage
		                   		 (
		                   				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
		                   						,iv
		                   						,gallery_loadingcontrol
		                   						,tag
		                   						,false
		                   						,vAddrForDTCP
		                   		 );
	                   	 	}	
	                    }
                    }
                    else if(mRepeatMode == REPEATMODE_OFF)
                    {
                    	if(current_item > child_count -3)
                    	{
                    		current_item = child_count -3;
                    	}
                    	int index_from = 0;
                    	int index_to = 0;
                    	if (x <= 0) {  
                    		index_from = 1;
                    		index_to = 7;
	                    } 
	                    else if (x >= (child_width * child_count - 7*child_width)) 
	                    {  
	                    	index_from = child_count-6;
	                    	index_to = child_count;
	                    	gallery.scrollTo(child_width * child_count - 7*child_width, 0);  
	                    }  
	                    else
	                    {
	                    	index_from = current_item - 3;
	                    	index_to = current_item + 6;
	                    }

	                    for(int i =index_from;i<=index_to;i++)
	                    {
	                    	int tag = i-child_count*2>0?i-child_count*2:(i-1);
	                    	ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
	                    	synchronized(this)
	                   	 	{
		                   		 mImageWorker.setImageSize(200, 100);
		                   		 mImageWorker.setImageCache(mCache_small);
		                   		 if(i-1<m_filePathStrArray.length)
		                   		 {
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
		                   		 }
	                   	 	}	
	                    }
                    	
                    }
                    
                    break;  
                case MotionEvent.ACTION_UP:  
                	
                	//when REPEATMODE_OFF, if scroll to last item, sometimes, blank space appears.
                	//if that happens, scroll back to "child_count -3".
                	if(mRepeatMode == REPEATMODE_OFF)
                	{
	                	if(current_item > child_count -3)
	                	{
	                		current_item = child_count -3;
	                	}
                	}
                	
                	
                	flag_last_item = false;
                    flag = true;  
                    gallery.smoothScrollTo(child_width  
                            * current_item - child_width / 2 - hsv_width / 2,  
                            gallery.getScrollY());  
                    System.out.println("current_item % child_count"+ current_item + " " + child_count+ 
                            " " + (current_item % child_count)+" "+child_width / 2+" "+hsv_width / 2);
                    
                    x = gallery.getScrollX();
                    int current_item_up = (x + hsv_width / 2) / child_width + 1;
                    if(mRepeatMode == REPEATMODE_OFF)
                	{
	                	if(current_item_up > child_count -3)
	                	{
	                		current_item_up = child_count -3;
	                	}
                	}
                    for(int i =current_item_up- 3;i<=current_item_up+3;i++)
                    {
                    	int tag = i-child_count*2>0?i-child_count*2:(i-1);
                    	ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
                    	synchronized(this)
                   	 	{
	                   		 mImageWorker.setImageSize(200, 100);
	                   		 mImageWorker.setImageCache(mCache_small);
	                   		 mImageWorker.loadImage
	                   		 (
                   				 m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
                   						 ,iv
                   						 ,gallery_loadingcontrol
                   						 ,tag
                   						 ,false
                   						,vAddrForDTCP
	                   		 );
                   	 	}	
                    }
                    
                    
                    break;  
                }  
                if (pre_item == 0) {  
                    isChecked(current_item, true);  
                } else if (pre_item != current_item) {  
                    isChecked(pre_item, false);  
                    isChecked(current_item, true);  
                }  
                pre_item = current_item;  
                return flag;  
            }  
        });  
		gallery.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction() == KeyEvent.ACTION_UP)
				{
					//move from  --notice-- 
					if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
					{	
						if(mRepeatMode == REPEATMODE_ON)
	        			{
		        			NextImage();
		        			int focused_item = m_currentPlayIndex+1;
		        			
		        			if (focused_item-3 <= 0)
		            		{
		        				focused_item = focused_item + child_count;
		            		}
		        			else if (focused_item + 3 > child_count * 2 - 1)
		            		{
		            			focused_item = focused_item - child_count;
		            		}
	
		        			gallery.smoothScrollTo(child_width  
		        					* focused_item - child_width / 2 - hsv_width / 2,  
		        					gallery.getScrollY());
		        			
		        			changeFocusToItem(focused_item-1);
		        			
		        			for(int i = focused_item- 3;i<= focused_item+3;i++)
		        			{
		        				int tag = i-child_count*2>0?(i-child_count*2):(i-1);
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        			}
	        			else if(mRepeatMode == REPEATMODE_OFF)
	        			{
	        				NextImage();
	        				
	        				int focused_item = m_currentPlayIndex+1;
	        				int index_from,index_to;
		        			if (focused_item-3 <= 0)
		            		{
		        				index_from = 1;
		        				index_to = 7;
		        				gallery.smoothScrollTo(0,  
				                        gallery.getScrollY()); 
		            		}
		        			else if (focused_item + 3 > child_count - 1)
		            		{
		        				index_from = child_count - 6;
		        				index_to = child_count;
		        				gallery.smoothScrollTo(child_width  
				        				* child_count - 7*child_width,  
				                        gallery.getScrollY()); 
		            		}
		        			else
		        			{
		        			
		        				index_from = focused_item - 3;
		        				index_to = focused_item + 3;
				        		gallery.smoothScrollTo(child_width  
				        				* focused_item - child_width / 2 - hsv_width / 2,  
				                        gallery.getScrollY()); 
		        			}
		        			
			        		
			        		
			        		changeFocusToItem(focused_item-1);
							
		        			for(int i = index_from;i<= index_to;i++)
		        			{
		        				int tag = i-1;
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        				
	        			}
						return true;
					}
					else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
					{
						if(mRepeatMode == REPEATMODE_ON)
	        			{
		        			PreImage();
		        			int focused_item = m_currentPlayIndex+1;
		        			if (focused_item-3 <= 0)
		            		{
		        				focused_item = focused_item + child_count;
		            		}
		        			else if (focused_item + 3 > child_count * 2 - 1)
		            		{
		            			focused_item = focused_item - child_count;
		            		}
		        			
			        		gallery.smoothScrollTo(child_width  
			        				* focused_item - child_width / 2 - hsv_width / 2,  
			                        gallery.getScrollY());  

			        		changeFocusToItem(focused_item-1);
							
		        			for(int i = focused_item- 3;i<= focused_item+3;i++)
		        			{
		        				int tag = i-child_count*2>0?(i-child_count*2):(i-1);
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        			}
	        			else if(mRepeatMode == REPEATMODE_OFF)
	        			{
	        				PreImage();
	        				
	        				int focused_item = m_currentPlayIndex+1;
	        				int index_from,index_to;
		        			if (focused_item-3 <= 0)
		            		{
		        				index_from = 1;
		        				index_to = 7;
		        				gallery.smoothScrollTo(0,  
				                        gallery.getScrollY()); 
		            		}
		        			else if (focused_item + 3 > child_count - 1)
		            		{
		        				index_from = child_count - 6;
		        				index_to = child_count;
		        				gallery.smoothScrollTo(child_width  
				        				* child_count - 7*child_width,  
				                        gallery.getScrollY()); 
		            		}
		        			else
		        			{
		        			
		        				index_from = focused_item - 3;
		        				index_to = focused_item + 3;
				        		gallery.smoothScrollTo(child_width  
				        				* focused_item - child_width / 2 - hsv_width / 2,  
				                        gallery.getScrollY()); 
		        			}
		        			
			        		
			        		
			        		changeFocusToItem(focused_item-1);
							
		        			for(int i = index_from;i<= index_to;i++)
		        			{
		        				int tag = i-1;
		        				ImageView iv = (ImageView)(gallery.findViewWithTag(tag));
			        			synchronized(this)
			               	 	{
			                   		 mImageWorker.setImageSize(200, 100);
			                   		 mImageWorker.setImageCache(mCache_small);
			                   		 mImageWorker.loadImage
			                   		 (
			                   				m_filePathStrArray[i-1]
			                   						,iv
			                   						,gallery_loadingcontrol
			                   						,tag
			                   						,false
			                   						,vAddrForDTCP
			                   		 );
			               	 	}	
		        			}
	        				
	        			}
						return true;
					}
				}
				
				return true;
			}
			
			
			
		});
	
		
	}
	private void initHsvStart() {
		// TODO Auto-generated method stub
		final ViewTreeObserver observer = gallery.getViewTreeObserver();  
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {  
  
            @Override  
            public boolean onPreDraw() {  
                // TODO Auto-generated method stub  
                observer.removeOnPreDrawListener(this);  
                
                if(noScrollGallery)
				{
					changeFocusToItem(m_currentPlayIndex);
				}
				else if(mRepeatMode == REPEATMODE_ON)
                {
	                int pos_mid = child_start;
	        		if (pos_mid-3 <= 0)
	        		{
	        			pos_mid = pos_mid+child_count;
	        		}
	        		else if (pos_mid + 3 > child_count * 2 - 1)
	        		{
	        			pos_mid = pos_mid - child_count;
	        		}
	
	        		int tmpDestScorllX = child_width * pos_mid - child_width / 2 - hsv_width / 2;
	        		gallery.scrollTo(tmpDestScorllX,gallery.getScrollY());  
                }
				else if(mRepeatMode == REPEATMODE_OFF)
                {
                	int pos_mid = child_start;
	        		if (pos_mid-3 <= 0)
	        		{
	        			gallery.scrollTo(0,gallery.getScrollY());
	        		}
	        		else if (pos_mid + 3 > child_count - 1)
	        		{
	        			int tmpDestScorllX = child_width * child_count - 7*child_width;
	        			gallery.scrollTo(tmpDestScorllX,gallery.getScrollY());
	        		}
	        		else
	        		{
	        			int tmpDestScorllX = child_width * pos_mid - child_width / 2 - hsv_width / 2;
	        			gallery.scrollTo(tmpDestScorllX,gallery.getScrollY());
	        		}
                }
                return true;
            }  
        }); 
		if(noScrollGallery)
		{
			changeFocusToItem(m_currentPlayIndex);
		}
		else if(mRepeatMode == REPEATMODE_ON)
        {
            int pos_mid = child_start;
    		if (pos_mid-3 <= 0)
    		{
    			pos_mid = pos_mid+child_count;
    		}
    		else if (pos_mid + 3 > child_count * 2 - 1)
    		{
    			pos_mid = pos_mid - child_count;
    		}
    		
    		int i = pos_mid;
    		int j = -1;
    		ImageView iv = (ImageView)(gallery.findViewWithTag(i-child_count*2>0?i-child_count*2:(i-1)));

			//the middle imageview
    		int tag = i-child_count*2>0?i-child_count*2:(i-1);
    		changeFocusToItem(tag);

        	synchronized(this)
       	 	{
           		 mImageWorker.setImageSize(200, 100);
           		 mImageWorker.setImageCache(mCache_small);
           		 mImageWorker.loadImage
           		 (
           				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
           						,iv
           						,gallery_loadingcontrol
           						,tag
           						,false
           						,vAddrForDTCP
           		 );
       	 	}	
        	
        	
    		
    		for(i = pos_mid - 1,j = pos_mid + 1;(i >= pos_mid -3)&&(j <= pos_mid+3); i--,j++)
            {
    			
    			for (int k = 0;k<2;k++)
    			{
    				int tmp = k == 1?i:j;
    				int _tag = tmp-child_count*2>0?tmp-child_count*2:(tmp-1);
                	iv = (ImageView)(gallery.findViewWithTag(_tag));
                	synchronized(this)
               	 	{
                   		 mImageWorker.setImageSize(200, 100);
                   		 mImageWorker.setImageCache(mCache_small);
                   		 mImageWorker.loadImage
                   		 (
                   				m_filePathStrArray[tmp-1>(child_count-1)?(tmp-child_count*2>0?(tmp-child_count*2):(tmp-child_count-1)):tmp-1]
                   						,iv
                   						,gallery_loadingcontrol
                   						,_tag
                   						,false
                   						,vAddrForDTCP
                   		 );
               	 	}	
    			}
            }
    		if(pos_mid<=child_count-1)
        	{
        		for(i = child_count; i<child_count+6; i++)
        		{
        			int _tag = i-child_count*2>0?i-child_count*2:(i-1);
        			iv = (ImageView)(gallery.findViewWithTag(_tag));
                	synchronized(this)
               	 	{
                   		 mImageWorker.setImageSize(200, 100);
                   		 mImageWorker.setImageCache(mCache_small);
                   		 mImageWorker.loadImage
                   		 (
                   				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
                   						,iv
                   						,gallery_loadingcontrol
                   						,_tag
                   						,false
                   						,vAddrForDTCP
                   		 );
               	 	}	
        		}
        	}
        }
        else if(mRepeatMode == REPEATMODE_OFF)
        {
        	int pos_mid = child_start;
    		int i = pos_mid;
    		int tag = i-child_count*2>0?i-child_count*2:(i-1);
    		ImageView iv = (ImageView)(gallery.findViewWithTag(tag));

			//the middle imageview
    		changeFocusToItem(i-1);
    		
    		synchronized(this)
       	 	{
           		 mImageWorker.setImageSize(200, 100);
           		 mImageWorker.setImageCache(mCache_small);
           		 mImageWorker.loadImage
           		 (
           				m_filePathStrArray[i-1>(child_count-1)?(i-child_count*2>0?(i-child_count*2):(i-child_count-1)):i-1]
           						,iv
           						,gallery_loadingcontrol
           						,tag
           						,false
           						,vAddrForDTCP
           		 );
       	 	}	

    		int left,right;
    		if(pos_mid-3<=0)
    		{
    			left = 1;
    			right = 7;
    		}
    		else if(pos_mid+3>=child_count-1 )
    		{
    			left = child_count-6;
    			right = child_count;
    		}
    		else
    		{
    			left = pos_mid-3;
    			right = pos_mid+3;
    		}
    		int left_start  = pos_mid - 1;
    		int right_start = pos_mid + 1;
    		boolean condition_left  = left_start  >= left  ? true:false;
    		boolean condition_right = right_start <= right ? true:false;
    		boolean condition = condition_right || condition_left;
    		while(condition)
    		{
    			if(condition_left)
    			{
    				int _tag = left_start-1;
    				iv = (ImageView)(gallery.findViewWithTag(_tag));
                	synchronized(this)
               	 	{
                   		 mImageWorker.setImageSize(200, 100);
                   		 mImageWorker.setImageCache(mCache_small);
                   		 mImageWorker.loadImage
                   		 (
                   				m_filePathStrArray[left_start-1]
                   						,iv
                   						,gallery_loadingcontrol
                   						,_tag
                   						,false
                   						,vAddrForDTCP
                   		 );
               	 	}	
                	left_start--;
                	condition_left  = left_start  >= left  ? true:false;
    			}
    			if(condition_right)
    			{
    				int _tag = right_start-1;
    				iv = (ImageView)(gallery.findViewWithTag(_tag));
                	synchronized(this)
               	 	{
                   		 mImageWorker.setImageSize(200, 100);
                   		 mImageWorker.setImageCache(mCache_small);
                   		 mImageWorker.loadImage
                   		 (
                   				m_filePathStrArray[right_start-1]
                   						,iv
                   						,gallery_loadingcontrol
                   						,_tag
                   						,false
                   						,vAddrForDTCP
                   		 );
               	 	}
                	right_start ++;
                	condition_right = right_start <= right ? true:false;
    			}
    			condition = condition_right || condition_left;
    		}
        }
        return;
	}
	public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.srcHeight;
        final int width = options.srcWidth;
        System.out.println("calculateInSampleSize"+height+" "+width);
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

	@Override
    public synchronized void update(Observable o, Object arg) {
		Log.e("audiobrowser", "update");
		ObserverContent content = (ObserverContent)arg;
		String serverName = content.getMsg();
		String act = content.getAction();
		if(act.equals(ObserverContent.REMOVE_DEVICE)) {
			Log.e("DLNADevice", "photoBrowser "+" removed server name: " + serverName);
			if(mMediaApplicationMap.getMediaServerName().equals(serverName))
				{
				//todo
				}

		}
	}
	 public void onWindowFocusChanged(boolean hasFocus) {  
	        // TODO Auto-generated method stub  
	        super.onWindowFocusChanged(hasFocus);  
	        if(isFirstEnter == true)
	        {
		        init_gsv();
		        hsv_width = gallery.getWidth();  
		        int child_width_temp = hsv_width / child_show_count;  
		        if (child_width_temp % 2 != 0) {  
		            child_width_temp++;  
		        }  
		        child_width = child_width_temp;  
		        initHsvData();  
		        initHsvTouch();  
		        initHsvStart();  
		        isFirstEnter = false;
	        }
	    }  
	
}
