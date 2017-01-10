package com.rtk.dmp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.log.Logger;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5.DeviceStatusListener;
import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.realtek.DataProvider.DLNADataProvider;
import com.rtk.dmp.VideoBrowser.QuickMenuAdapter.ViewHolder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.Metadata;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.Formatter;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.media.MediaPlayer.OnCompletionListener;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.PopupWindow.OnDismissListener;
import android.app.TvManager;


public class VideoPlayerActivity extends Activity implements OnClickListener, OnTouchListener,OnGestureListener, Observer{
	private String TAG = "VideoPlayerActivity";
	private String tag = "hqmTag";
	private String TAG_ROTATE = "TAG_ROTATE";
	
	private int mLastSystemUiVis = 0;
	private boolean DEBUG_SUBTITLE = false;

	private boolean loading = false;
	
	//////////////////////Data From////////////////////////////////////////
	private ArrayList<String> filePathArray = new ArrayList<String>();
	private ArrayList<String> fileTitleArray = new ArrayList<String>();
	private ArrayList<String> fileCharIDArray = new ArrayList<String>();
	private ArrayList<Integer> filePos = new ArrayList<Integer>();
	
	private ArrayList<DLNAFileInfo> videoList;
	private String serverName = null;
	private DLNA_DMP_1p5 dlna_DMP_1p5 = null;
	public ArrayList<String> DMSName = new ArrayList<String>();
	
	//code for backTo VideoBrowser
	private int ResultCodeRestartVideoBrowser = 10;
	private int FinishVideoBrowser = 1;
	
	
	private int fileArraySize = 0;
	/************Banner Animation********************/
	private View movBannerView = null;
	private boolean isAbleShown = true;
	private float banner_h = 0.0f;
    private long bannerAnimTime = 200;
	
	/************MediaApplication***************/
	private MediaApplication map = null;
	private Resources resourceMgr = null;
	private ContentResolver m_ContentMgr = null;
	
	/******** Handler  parameter initial *******/
	//public Handler handler = null;
	
	/******** Timer and TimerTask***************/
    private Timer timer = null;
    int delay_6s = 6000;
    int delay_100ms = 100;
    int delay_200ms = 200;
	int delay_300ms = 300;
    int delay_500ms = 500;
    int delay_1000ms = 1000;
    int delay_2000ms = 2000;
    int delay_5000ms = 5000;
    int delay_6000ms = 6000;
	
	private TimerTask task_getCurrentPositon = null;
	private TimerTask task_hide_controler = null;
	//private TimerTask task_updateSleepTime = null;
	
	/*********Application And MediaPlayer status*****************/
	
	//private boolean isOnPause = false;	//Application onPause
	private boolean isSupportSeek = true;
	private boolean isSupportResume = false;
	private boolean isSupportJPSeek = true;
	private boolean isJPTS = false;
	private boolean isSupportStalling = false;
	
	/**************Picture Size****************/
	public TvManager mTVService = null;
	private int selected_idx = 0;	
	
	/*************Audio Mode****************/
	private final static int AUDIO_DIGITAL_RAW = 0;
	private final static int AUDIO_DIGITAL_LPCM_DUAL_CH = 1;
	private int AUDIO_DIGITAL_OUTPUT_MODE = 0;
	
	/****************Value For Total Time********/
	private static int Minute = 0;
    private static int Hour = 0;
    private static int Second = 0;
    
    /******** Set Subtitle info *******/
	private int[] subtitleInfo = null;
	
	private int subtitle_num_Stream = 1;
	private int curr_subtitle_stream_num = 0;
	
	private int curr_textEncoding = 1000;
	private int curr_textColor = 0;
	private int curr_fontSize = 19;
	
	private int curr_SPU_ENABLE = 0;
	
	/******** Set Audio info *******/
	private int[] audioInfo = null;
	private int audio_num_stream = 0;
	private int curr_audio_stream_num = 0;
	String defaultAudioType = "UnKnown";
	String curr_audio_type = defaultAudioType;
	
	/**********NavState*************/
	
	
	
	
	public static final int	NAVPROP_INPUT_GET_PLAYBACK_STATUS = 10;
	public static final int NAVPROP_INPUT_GET_NAV_STATE = 11;
	public static final int NAVPROP_INPUT_SET_NAV_STATE = 12;

	
	

	/******************Resources********************/
	private ProgressBar loadingIcon = null;
	private TextView dolby = null;
	private View main_layout = null;
	private ImageView play = null;	
	private ImageButton play_mode = null;	
	private ImageButton pictureSize = null;
	
	private ImageButton playAndPause = null;
	private ImageButton slowForward = null;
	private ImageButton slowBackward = null;
	private ImageButton fastForward = null;
	private ImageButton fastBackward = null;
	private ImageButton skipNext = null;
	private ImageButton skipBefore = null;
	
	private TextView timeNow = null;
	private TextView timeEnd = null;
	private SeekBar timeBar = null;
	private ImageView mainMenu = null;
	
	private M_QuickMenu quickmenu = null;
	private QuickMenuAdapter quickmenuAdapter = null;
	
	//private Dialog transparantDialog = null;	//to show menu details
	//private DialogAdapter dialogAdapter = null;
	
	private Handler uiHandler = null;
	
	GestureDetector mGestureDetector = null;
	private float FLING_MIN_DISTANCE = 0.0f;
	private float FLING_MIN_VELOCITY = 0.0f;
	
	PlaybackControl playbackControl = null;
	
	
	private int ffRate[] = {2, 8, 16, 32};	
	private int fwRate[] = {2, 8, 16, 32};	
	private int sfRate[] = {4, 16};	
	private int swRate[] = {4, 16};	
	
	private int ffIndex = -1;
	private int fwIndex = -1;
	private int sfIndex = -1;
	private int swIndex = -1;
	private int ctr_direction_fw = 0; //0 means raw value, -1, means ctr left , 1 means ctr right
	private int ctr_direction_ff = 0; //0 means raw value, -1, means ctr left , 1 means ctr right
	
	private final static int REPEAT_SINGLE = 1;	//
	private final static int REPEAT_ALL = 2;	//
	private final static int REPEAT_OFF = 3;	//
	private int repeat_mode = REPEAT_OFF;
	

	private boolean isSeeking = false;
	private boolean isOnNotNormalPlay = false;
	

	public int currIndex = 0;
	public int saveCurrIndex = -1;
	public int resumeIndex = -1;
	
	
	/******** control parameter initial *******/
	private final static int MOVIE_BANNER = 1;
	private final static int SUBTILE_LIST = 3;
	private final static int AUDIO_BANNER = 4;
	private final static int WANNING_MESSAGE = 5;
	private final static int TITLE_BANNER = 6;
	


	
	

	/***********flag setting **************************/
	private int rewind_press = 1;
	private int forward_press = 1;
	private boolean isrewind = false;
	private boolean isforward = false;
	
	private boolean isSwitchToNextFile = false;
	
	private boolean isAudio_Error = false;
	private boolean isVideoPlayCompleteInRepeatOFFMode = false;
	private boolean isNeedExecuteOnPause = true;
	
	private boolean isStartPlay = false;
	
	private static int ResultCodeFinishVideoBrowser = 9;
	
	private boolean isShowChapterMetaData = false;
	
	
	
	private boolean isRight2Left = false;
	/********   android component parameter initial *******/
	private SurfaceView sView = null;
	protected MediaPlayer mPlayer = null;
	
	
	
	
	private TextView duration = null;
	private TextView filename = null;
	private TextView file_number = null;
	
	private TextView common_ui = null;
	
	
	private View PictureSize = null;
		
	public TextView spinner_bar_value = null;
	
	public Metadata metadata = null;
	
	public Toast toast = null;
	
	private SharedPreferences mPerferences =null;
	/******** Quick Menu Setting ***************/
	
	private int mSleepTimeHour = 0, mSleepTimeMin = 0;
	
	private int qm_focus_item = 0;
    

	
	public TitleBookMark mTitleBookMark = null;
	public BookMark mVideoBookMark = null;
	
	/******** DivxParser *****************/
	public String ButtonNum = "";
	public int mClick_DPAD_DOWN_NUM = 0;
	/************ConfirmMessage*****************/
	private ConfirmMessage long_msg = null;
	private ConfirmMessage short_msg = null;
	
	private PopupMessage msg_hint = null;
	
	
	
	
	/************Intent*****************/
	public boolean isAnywhere;
	
	
	private AudioManager am = null;
	
	private byte[] navBuff = null;
	
	//long time1, time2;
    
    private boolean actionOK = false;

	DeviceStatusListener mDeviceListener = null; 
	
	private int nowAudioFocus = AudioManager.AUDIOFOCUS_LOSS;	//start not have audio focus
	
	private final int INTERVALFORNOWPOSANDSEEKPOS = 1000;	//ms
	
	SurfaceListener surfaceListener = null;
	
	private final int DELAY_SEEK = 700;	//ms
	
	final String HOMEKEY_PRESS = "com.realtek.NOTIFY_RTKDMP";
	
	private int SKIPFLAG = 0; //0 means when error  do nothing, -1 means when error change to before , 1 means when error change to end
	
	private final int ORIENTATION_LANDSCAPE = 0;
	private final int ORIENTATION_PORTRAIT = 1;
	private Display display = null;
	private final int ROTATE_0 = 0;
	private final int ROTATE_1 = 90;
	private final int ROTATE_2 = 180;
	private final int ROTATE_3 = 270;
	
	// about flow control according spec-----------------------------------
	boolean isRemoved = false;

	private boolean isActivityPause = false;
	private String globalDuration = null;
	private boolean isActivityPostCreate = false;
	private boolean ablePlay = false;
	private boolean isActivityReady = false;
	
	// val used to init menu, popupMessage and so on, which has relationship with screen density or screen resolution
	int popupWindowWith = -1;
	int popupWindowHeight = -1;
	int menuWidth = -1;
	int menuHeight = -1;
	
	private void requestMeasureInfo() {
		int screenWidth = map.getScreenWidth();
		int screenHeight = map.getScreenHeight();
		if((screenWidth <= 1280 && screenHeight <= 720) 
				|| (screenWidth <= 720 && screenHeight <= 1280)) {
			//720p
			popupWindowWith = 447;
			popupWindowHeight = 150;
			menuWidth = 578;
			menuHeight = -1;
		} else if((screenWidth <= 1920 && screenHeight <= 1280) 
				|| (screenWidth <= 1280 && screenHeight <= 1920)) {
			//1080p
			popupWindowWith = 678;
			popupWindowHeight = 226;
			menuWidth = 868;
			menuHeight = -1;
		} else {
			//4k
			popupWindowWith = -1;
			popupWindowHeight = -1;
			menuWidth = -1;
			menuHeight = -1;
		}
	}
	
	public class SurfaceListener implements SurfaceHolder.Callback {
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			Logger.v(tag, "surfaceDestroyed() bb");
			Logger.v(tag, "surfaceDestroyed() ee");
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Logger.v(tag, "SurfaceCreated()");
			// turn on Scaler
			//before setDisplay , get ScreenDim;
			playbackControl.mp_setDisplay(holder);
			
			//rotateVideo(holder);
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO Auto-generated method stub
			//rotateVideo(holder);
		}
		
		public void rotateVideo(SurfaceHolder holder) {
			
		}
			
	} 
	
	
	final int SUBTITLE_ID = 0;	//On means there has subtitle, off means there is no subtitle, and greyed
	final int AUDIOTRACK_ID = 1;	//No multi audio available , just greyed
	final int DETAILS_ID = 2;
	final int HELP_ID = 3;
	final int ITEMNUM = 4;
	
	final int MAXTYPE_COUNT = 2;
	final int FIRST_TYPE = 0;
	final int SECOND_TYPE = 1;
	
	int[] menu_name = new int[ITEMNUM];
	
	class QuickMenuAdapter extends BaseAdapter {
		private LayoutInflater layoutInflater = null;
		class ViewHolder {
			TextView menu_name;
			ImageView left;
			TextView menu_option;
			ImageView right;
		}
		
		public String dateFormate(String date,String formatString) {
			// TODO Auto-generated method stub				
			SimpleDateFormat df_ori_exif = new SimpleDateFormat("yyyy.MM.dd");
			SimpleDateFormat df_des = new SimpleDateFormat(formatString);
		    java.util.Date date_parse = null;
		     try {
		    	 date_parse = df_ori_exif.parse(date);
		    	
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		     String formateDate1 = null;
		     formateDate1 = df_des.format(date_parse);
		     return formateDate1;
		}
		
		public QuickMenuAdapter(Context mContext) {
			// TODO Auto-generated constructor stub
			layoutInflater = LayoutInflater.from(mContext);
			menu_name[SUBTITLE_ID] = R.string.quick_menu_subtitle;
			menu_name[AUDIOTRACK_ID] = R.string.quick_menu_audio_track;
			menu_name[DETAILS_ID] = R.string.quick_menu_detail;
			menu_name[HELP_ID] = R.string.quick_menu_help;
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return menu_name.length;
		}

		@Override
		public Object getItem(int position) {	//not used
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}
		
		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return MAXTYPE_COUNT;
		}
		@Override
		public int getItemViewType(int position) {
			// TODO Auto-generated method stub
			return position == 2? FIRST_TYPE : SECOND_TYPE;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder viewHolder = null;
			TextView fileNameDataView = null;
			TextView dateDataView = null;
			TextView resolutionDataView = null;
			
			
			int currentType = getItemViewType(position); 
			if(currentType == FIRST_TYPE) {	// for Detail
					convertView = layoutInflater.inflate(R.layout.video_detail, null);
					fileNameDataView = (TextView) convertView.findViewById(R.id.filename_data);
					dateDataView = (TextView)convertView.findViewById(R.id.date_data);
					resolutionDataView = (TextView)convertView.findViewById(R.id.resolution_data);
					//convertView.setBackgroundResource(R.drawable.gray_out);
					convertView.setEnabled(false);
			} else if(currentType == SECOND_TYPE) {	// Others
				if(convertView == null) {
					convertView = layoutInflater.inflate(R.layout.video_browser_menu_list_row, null);
					viewHolder = new ViewHolder();
					viewHolder.left = (ImageView) convertView.findViewById(R.id.left_arrow);
					viewHolder.right = (ImageView) convertView.findViewById(R.id.right_arrow);
					viewHolder.menu_name = (TextView)convertView.findViewById(R.id.menu_name);
					//Typeface type= Typeface.createFromFile("/system/fonts/FAUNSGLOBAL3_F_r2.TTF");
					//viewHolder.menu_name.setTypeface(type);
		        	viewHolder.menu_option = (TextView)convertView.findViewById(R.id.menu_option);
		        	convertView.setTag(viewHolder);
				} else {
					viewHolder = (ViewHolder)convertView.getTag();
				}
				viewHolder.menu_name.setText(menu_name[position]);
			}
			
			switch(position) {
				case SUBTITLE_ID: {
					//default subtitle off
					viewHolder.left.setVisibility(View.INVISIBLE);
					viewHolder.right.setVisibility(View.INVISIBLE);
					if(subtitle_num_Stream <= 0) {
						viewHolder.menu_option.setText("Subtitle Off");
						convertView.setBackgroundResource(R.drawable.gray_out);
						convertView.setEnabled(false);
						
					} else {
						convertView.setBackgroundResource(0);
						convertView.setEnabled(true);
						if(curr_SPU_ENABLE == 0) {
							viewHolder.menu_option.setText("Subtitle Off");
						}
						if(curr_SPU_ENABLE == 1) {
							viewHolder.menu_option.setText("Subtitle On " + curr_subtitle_stream_num);
						}
					}
				} break;
				case AUDIOTRACK_ID: {
					getAudioTrackInfo();
					viewHolder.left.setVisibility(View.INVISIBLE);
					viewHolder.right.setVisibility(View.INVISIBLE);
					if(audio_num_stream < 1) {
						viewHolder.menu_option.setText(defaultAudioType);
						convertView.setBackgroundResource(R.drawable.gray_out);
						convertView.setEnabled(false);
					} else {
						viewHolder.menu_option.setText(curr_audio_type);
						convertView.setEnabled(true);
					}
				} break;
				case DETAILS_ID: {
					if(currIndex >= 0  && currIndex < fileArraySize) {
						fileNameDataView.setText(fileTitleArray.get(currIndex) + (String)getResources()
										.getText(R.string.fullblank));
					} else {
						fileNameDataView.setText("NotKnown");
					}
					
					String date = DLNADataProvider.queryDataByFile(
							fileTitleArray.get(currIndex), DLNADataProvider.UPNP_DMP_RES_DATE, null);
					Log.d(TAG, "DATE=" + date);
					if(date == null || date == "") {
						date = "";
					} else { 
						String language = getApplicationContext()
				        		.getResources().getConfiguration().locale.getLanguage();
						if(language.equals("ja"))
			        	{
							try {
			        		date = dateFormate(date,
			        				"yyyy"+(String) resourceMgr.getText(R.string.year_menuicon)
			        			   +" MMM dd"+(String) resourceMgr.getText(R.string.day_menuicon)
			        			   +" EEEEEE");
							} catch (Exception e) {
								// TODO: handle exception
								date = "";
							}
			        	}
			        	else
			        	{
			        		try {
			        		date = dateFormate(date,"EEE,dd MMM yyyy");
			        		} catch(Exception e) {
			        			date = "";
			        		}
			        	}
					}
				
					dateDataView.setText(date);
					
					if(currIndex >= 0 && currIndex < fileArraySize) {
						String strTmp = DLNADataProvider.queryDataByFile(
								fileTitleArray.get(currIndex),
								DLNADataProvider.UPNP_DMP_RES_RESOLUTION, null);
						resolutionDataView.setText(strTmp);
					} else {
						resolutionDataView.setText("NotKnown");
					}
				} break;
				case HELP_ID: {
					// just do nothing will be OK
					
				} break;
				default : {
					break;
				}	
			}
			return convertView;
		}
	}
	
	OnItemClickListener quickmenuItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			//quickAutoQuit();
			switch(position) {
				case SUBTITLE_ID: {
					getSubtitleInfo();
					if(subtitle_num_Stream < 1) {
						curr_SPU_ENABLE = 0;
						setSubtitle();
						((BaseAdapter)(parent.getAdapter())).notifyDataSetChanged();
						return ;
					}
					
					if(0 == curr_SPU_ENABLE ) {
						curr_SPU_ENABLE = 1;
						curr_subtitle_stream_num = 1;
						setSubtitle();
						((BaseAdapter)(parent.getAdapter())).notifyDataSetChanged();
						return ;
					}
					if(1 == curr_SPU_ENABLE && curr_subtitle_stream_num >= subtitle_num_Stream) {
						curr_SPU_ENABLE = 0;
						curr_subtitle_stream_num = 0;
						setSubtitle();
						((BaseAdapter)(parent.getAdapter())).notifyDataSetChanged();
						return ;
					}
					curr_subtitle_stream_num ++;
					setSubtitle();
					((BaseAdapter)(parent.getAdapter())).notifyDataSetChanged();
				} break;
				case AUDIOTRACK_ID: {
					getAudioTrackInfo();
					if(audio_num_stream < 1) {
						curr_audio_stream_num = 0;
						return ;
					}
					curr_audio_stream_num ++;
					setAudioTrack();
					((BaseAdapter)(parent.getAdapter())).notifyDataSetChanged();
				} break;
				case DETAILS_ID: {
//					quickmenu.dismiss();
//					//load MediaInfo
//					ListView showDetailList = (ListView) transparantDialog.findViewById(R.id.showList);
//					showDetailList.setAdapter(dialogAdapter);
//					showDetailList.setFocusable(false);
//					showDetailList.setClickable(false);
//					showDetailList.setSelected(false);
//					showDetailList.setEnabled(false);
//					isAbleShown = false;
//					DialogInterface.OnDismissListener listener = new DialogInterface.OnDismissListener() {
//
//						@Override
//						public void onDismiss(DialogInterface dialog) {
//							// TODO Auto-generated method stub
//							isAbleShown = true;
//						}
//						
//					};
//					transparantDialog.setOnDismissListener(listener);
//					transparantDialog.show();
//					uiHandler.postDelayed(new Runnable() {
//						
//						@Override
//						public void run() {
//							// TODO Auto-generated method stub
//							if(transparantDialog.isShowing()) {
//								transparantDialog.dismiss();
//							}
//						}
//					}, delay_6000ms);
				} break;
				case HELP_ID: {
					ComponentName componetName = new ComponentName("com.android.emanualreader",
							"com.android.emanualreader.MainActivity");
					quickmenu.dismiss();
					isAbleShown = true;
//					ComponentName componetName = new ComponentName("com.example.testui",
//							"com.example.testui.ListImageActivity");
					Intent intent = new Intent();
					intent.setComponent(componetName);
					startActivity(intent);
				} break;
				default :
					break;
			}
		}
		
	};
	
	private void quickAutoQuit() {
		uiHandler.removeMessages(HandlerControlerVariable.MSG_HIDE_MENU);
		Message msg = uiHandler.obtainMessage(HandlerControlerVariable.MSG_HIDE_MENU);
		uiHandler.sendMessageDelayed(msg, delay_6000ms);
	}
	
	private void onMenu() {
		Logger.v(tag, "Menu Click");
		if(quickmenu.isShowing() == true){
			Logger.v(tag, "close menu");
			uiHandler.removeMessages(HandlerControlerVariable.MSG_HIDE_MENU);
			isAbleShown = true;
			//cancel_task_updateSleepTime();
			quickmenu.dismiss();
		} else
		{
			Logger.v(tag, "show quickMneu");
			// shut banner
			isAbleShown = false;
			
			uiHandler.sendEmptyMessage(HandlerControlerVariable.QUICK_HIDE_BANNER);
			cancel_task_hide_controler();
			//before show Menu, we want data
			getSubtitleInfo();
			if(curr_subtitle_stream_num < 1 || subtitle_num_Stream < 1) {
				curr_SPU_ENABLE = 0;
			} else {
				curr_SPU_ENABLE = 1;
			}
			
			getAudioTrackInfo();
			
			// where to show Menu;
//			int locate[] = new int[2];
//			mainMenu.getLocationOnScreen(locate);
//			int x = locate[0];
//			int y = locate[1];
//			switch(getOrientation()) {
//				case ORIENTATION_LANDSCAPE:
//					quickmenu.showAsDropDown(mainMenu, mainMenu.getRight() + 100, resourceMgr.getDimensionPixelSize(R.dimen.padding_small));
//					break;
//				case ORIENTATION_PORTRAIT :
//					quickmenu.showAsDropDown(mainMenu, mainMenu.getRight(), resourceMgr.getDimensionPixelSize(R.dimen.padding_small));
//					break;
//			}
			//quickmenu.showAtRTop(mainMenu.getLeft(),mainMenu.getBottom() + 30, -1);
			quickmenu.show();
			//---
			//quickAutoQuit();
		}
	}
	
	private void addSleepTime() {
		if(mSleepTimeMin < 50)
		{
			if(0 == mSleepTimeMin && 12 == mSleepTimeHour)
			{
				mSleepTimeHour = 0;
			}
			else
			{
				mSleepTimeMin = (mSleepTimeMin / 10 + 1) * 10;
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
		
		//TextView OptionText = (TextView)view.findViewById(R.id.menu_option);
		//OptionText.setText(timeFormat);
    	
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
	}
	
	private void reduceSleepTime() {
		if(mSleepTimeMin > 10) {
			if(mSleepTimeMin % 10 == 0) {
				mSleepTimeMin -= 10;
			} else {
				mSleepTimeMin = mSleepTimeMin - mSleepTimeMin % 10;
			}
		} else {
			mSleepTimeMin = 0;
			if(mSleepTimeHour > 0) {
				mSleepTimeHour -- ;
			}
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
		
		//TextView OptionText = (TextView)view.findViewById(R.id.menu_option);
		//OptionText.setText(timeFormat);
    	
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
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		Logger.v(tag, "onKeyDown happen!");
		switch(keyCode) {
			//add for test
			case KeyEvent.KEYCODE_0 : {
//				Logger.v(tag, "0 pressed!");
//				Logger.v(tag, "+++" + this.getRequestedOrientation());
//				if(this.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//					Logger.v(tag, "try to set to land");
//					//this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//					this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
//					
//				} else if(this.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
//					Logger.v(tag, "try to set to port");
//					//this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//					this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
//					
//				} else if(this.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
//					Logger.v(tag, "try to set to reverse port");
//					this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
//					
//				} else if(this.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
//					Logger.v(tag, "try to set to reverse land");
//					this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
//				} else {
//					this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//				}
			} break;
			//add for test end
		
			case KeyEvent.KEYCODE_ESCAPE : {
				Logger.v(tag, "ESC press");
			} break;
			case KeyEvent.KEYCODE_BACK : {
				Logger.v(tag, "Back press");
			} break ;
			default :
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
    	Logger.v(tag, "VidePlayerActivity onCreate");
    	super.onCreate(savedInstanceState);
    	
    	//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	//requestWindowFeature(Window.FEATURE_NO_TITLE);
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
    	map = (MediaApplication)getApplication();
    	map.addObserver(this);
    	requestMeasureInfo();
    	
    	resourceMgr = this.getResources();
    	m_ContentMgr = this.getContentResolver();
    	surfaceListener = new SurfaceListener();
    	display = this.getWindowManager().getDefaultDisplay();
    	//according Config to set land or portrait Layout
    	setContentView(R.layout.video_player_dmp);
    	
    	findViews();
    	initWhenOnCreate();
    	setListeners();
    	
		Intent intent= getIntent();
		serverName = intent.getStringExtra("serverName");
	    isAnywhere = intent.getBooleanExtra("isanywhere", false);
	    if (!isAnywhere)
	        captureIntent(intent);
	    else if (isAnywhere)
	    	captureIntent_TvAnywhere(intent);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		Log.e(TAG, "WindwoFocusChanged !---------------" + hasFocus);
		if(hasFocus) {
			isActivityReady = true;
			isAbleShown = true;
			/* DLNA Test close by star_he
			if(!ablePlay) {
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
				showMessageHint(VideoPlayerActivity.this.getResources().getString(R.string.msg_unsupport));
				ablePlay = !ablePlay;
			}
			*/
			if(isRemoved) {
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DMS_CLOSE);
			}
		} else {
			isActivityReady = false;
			isAbleShown = false;
		}
	}

	@Override
	protected void onStart() {
    	Logger.v(TAG, "VidePlayerActivity onStart");
    	
		super.onStart();
	}
	
	
	
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		Logger.v(tag, "VideoPlayerActivity onRestart");
		super.onRestart();
		if(isRemoved) {
			return ;
		}
		
		isAbleShown = true;
		
		if(mVideoBookMark == null) {
			Logger.v(tag, "error mVideoBookMark !");
			resumeIndex = -1;
			return ;
		}
		
		resumeIndex = mVideoBookMark.findBookMarkWithUrl(filePathArray.get(currIndex));
		Logger.v(tag, "onRestart ++ resumeIndex ++" + resumeIndex);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(isRemoved) {
			return ;
		}
		//setNavVisibility(false); 
		mTVService.setAndroidMode(0);
		if(isActivityPause) {
			isActivityPause = false;
		}
		if(timer == null)
			timer = new Timer();
		
		ffIndex = fwIndex = sfIndex = swIndex = -1;
		playCurrentIndexMedia();
    }
	
	@Override
	protected void onPause() {
		Logger.v(tag, "On Pause happen");
		quickHideBanner();
		isAbleShown = false;
		if(isSupportResume)
			recordResumePoint();
		else {
			mVideoBookMark.cleanBookMark();
			map.setStopedFileUrl(null);
		}
		killTimerAndTask();
    	doAbandonAF();
    	f_stop();
    	isActivityPause = true;
    	if(msg_hint != null) {
			msg_hint.dismiss();
		}
		if(quickmenu != null) {
			quickmenu.dismiss();
		}
		if(long_msg != null) {
			long_msg.dismiss();
		}
		if(short_msg != null) {
			short_msg.dismiss();
		}
		super.onPause(); 
    }
	
    @Override
	 public void onStop(){
    	Logger.v(TAG, "VideoPlayerActivity::onStop() bb");
    	//playbackControl.getHandler().sendEmptyMessage(PlaybackFlags.ThreadStop);
    	//sView.getHolder().removeCallback(surfaceListener);
    	//surfaceListener = null;
		Logger.v(TAG, "VideoPlayerActivity::onStop() ee");
    	super.onStop();
    }
    
    @Override
	protected void onDestroy() {
    	Logger.v(TAG, "VidePlayerActivity::onDestroy bb");
    	msg_hint = null;
    	quickmenu = null;
    	long_msg = null;
    	short_msg = null;
    	isRemoved = false;
    	playbackControl.mp_release();
    	map.deleteObserver(this);
    	playbackControl = null;
    	uiHandler = null;
    	Logger.v(TAG, "VidePlayerActivity::onDestroy ee");
		super.onDestroy();
	}
    
    private void initWhenOnCreate() {
    	movBannerView.setVisibility(View.INVISIBLE);
    	play.setImageDrawable(null);
    	playAndPause.setImageDrawable(getResources().getDrawable(R.drawable.v_gui_play));
    	repeat_mode = REPEAT_OFF;
    	selected_idx = 0;
    	isRemoved = false;
    	mTVService = (TvManager)getSystemService("tv");
    	getMediaPlayer();
    	am = (AudioManager) (this.getSystemService(Context.AUDIO_SERVICE));
    	
		String path = getFilesDir().getPath();
		String fileName = path.concat("/VideoBookMark.bin");
		mVideoBookMark = map.getBookMark(fileName);
		
		//transparantDialog = new Dialog(this, R.style.TransparentDialog);
		//transparantDialog.setContentView(R.layout.transparent_dialog);
		
//		if(dialogAdapter == null) {
//			dialogAdapter = new DialogAdapter(this);
//		}
		
		short_msg = new ConfirmMessage(VideoPlayerActivity.this, popupWindowWith, popupWindowHeight);
	    long_msg = new ConfirmMessage(VideoPlayerActivity.this, popupWindowWith, popupWindowHeight);
	    msg_hint = new PopupMessage(this, popupWindowWith, popupWindowHeight);
	    
	    if(quickmenuAdapter == null) {
			quickmenuAdapter = new QuickMenuAdapter(this);
		}
	    // this is Quick Menu
		quickmenu = new M_QuickMenu(mainMenu, quickmenuAdapter, menuWidth);
		//quickmenu.setAnimationStyle(R.style.QuickAnimation);
		quickmenu.AddOnItemClickListener(quickmenuItemClickListener);
		
    	playbackControl = new PlaybackControl(mPlayer) {
			@Override
			public void setListeners() {
				
				mPlayer.setOnPreparedListener(new OnPreparedListener(){

					@Override
					public void onPrepared(MediaPlayer mp) {
						// TODO Auto-generated method stub
						Logger.v(tag, "on Prepared!");
						selected_idx = 3;
						setPicSize();
						
						state = PREPARED;
						uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);

						//need every variable relate with MediaPlayer init
						initMediaPlayerValue();
						//try to get and update Total time, and just this time will update
						uiHandler.sendEmptyMessage(HandlerControlerVariable.UPDATEENDTIME);
						//need resume ? try to resume
						if(resumeIndex >= 0) {
							resumeInit(resumeIndex);
							resumeIndex = -1;
						} 
						//every thing initialized then decide whether to play
						//spec define that when skip+ skip- swipeLeft swipeRight ,the previous state is paused it has to be paused
						
//						if(transparantDialog.isShowing()) {
//							dialogAdapter.notifyDataSetChanged();
//						}
						//The first time to get dolby info
						getAudioTrackInfo();

						String tmpProtolinfo = "";
						boolean bDTCP = false;
						tmpProtolinfo = DLNADataProvider.queryDataByID(fileCharIDArray.get(currIndex),DLNADataProvider.UPNP_DMP_RES_PROTOCOLINFO);
						Log.d(TAG, "protocalinfo= " + tmpProtolinfo);
						if(tmpProtolinfo != null){
							bDTCP = tmpProtolinfo.contains("DTCP1HOST");
							if (bDTCP || (SystemProperties.getBoolean("rtk.source.sendkey", false) == true) ) {
								//mTVService.setProtectionKey("DTCP"); DLNA Test close by star_he
							} else {
								//mTVService.setProtectionKey("NULL"); DLNA Test close by star_he
							}
						}

						f_play();
						
						showAndWaitToHideBanner();
						keepOnUpdateBarAndTime();
					}
					
				});
				
				mPlayer.setOnErrorListener(new OnErrorListener(){

					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						// TODO Auto-generated method stub
						state = ERROR;
						isSupportResume = false;
						Logger.e(tag, "VideoErrorListener" + "---what = "+what+"----");
						mPlayer.reset();
						uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);

						ffIndex = fwIndex = sfIndex = swIndex = -1;
						ctr_direction_ff = ctr_direction_fw = 0;
						isOnNotNormalPlay = false;
						
						curr_SPU_ENABLE = 0;
						curr_subtitle_stream_num = 0;
						subtitle_num_Stream = 0;
						curr_audio_stream_num = -1;
						audio_num_stream = 0;
						switch(what) {
							case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
							{
								Logger.e(TAG, "MediaServer died, finish mySelf!");
								playbackControl.mp_reset();
								long_msg.setMessage(VideoPlayerActivity.this.getResources().
						    	getString(R.string.Media_Server_Die));
								long_msg.setButtonText(VideoPlayerActivity.this.getResources().getString(R.string.msg_yes));
								long_msg.left.setVisibility(View.INVISIBLE);
								long_msg.right.setVisibility(View.INVISIBLE);
								long_msg.confirm_bt.setOnClickListener(new OnClickListener(){
	
									@Override
									public void onClick(View arg0) {
										// TODO Auto-generated method stub
										long_msg.dismiss();
										Intent intent = new Intent(VideoPlayerActivity.this, RTKDMP.class);
										startActivity(intent);
										VideoPlayerActivity.this.finish();
									}		
								});
								long_msg.show();
							} break;
							case MediaPlayer.MEDIA_ERROR_IO:
							case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
							case MediaPlayer.MEDIA_ERROR_TIMED_OUT: 
							case MediaPlayer.MEDIA_ERROR_UNKNOWN: {
								//showMessageHint(VideoPlayerActivity.this.getResources().getString(R.string.msg_playback_error));
								//map.AddErrorVideo(filePathArray.get(currIndex));
							} break;
							case MediaPlayer.MEDIA_ERROR_MALFORMED:
							case MediaPlayer.MEDIA_ERROR_UNSUPPORTED: {
								//showMessageHint(VideoPlayerActivity.this.getResources().getString(R.string.msg_unsupport));
								//map.AddErrorVideo(filePathArray.get(currIndex));
							} break;
							default : {
								
								switch(SKIPFLAG) {
									case 0:
										
										break;
									case 1:
										break;
									case 2:
										break;
								}
							} break;
						}
						return false;
					}
					
				});
				
				mPlayer.setOnCompletionListener(new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer mp) {	
						// TODO Auto-generated method stub
						Logger.v(tag, "play back completed !");
						//check really complete? work around
						Logger.i(tag, "playbackControl.mp_getCurrentPosition()" + playbackControl.mp_getCurrentPosition());
						Logger.i(tag, "playbackControl.mp_getDuration()" + playbackControl.mp_getDuration());
						if(playbackControl.mp_getDuration() == -1) {
							return ;
						}
						curr_SPU_ENABLE = 0;
						curr_subtitle_stream_num = 0;
						subtitle_num_Stream = 0;
						curr_audio_stream_num = -1;
						audio_num_stream = 0;
						
						state = PLAYBACKCOMPLETED;
						ffIndex = fwIndex = sfIndex = swIndex = -1;
						ctr_direction_ff = ctr_direction_fw = 0;
						isOnNotNormalPlay = false;
						switch (repeat_mode) {
							case REPEAT_SINGLE:
								playCurrentIndexMedia();
								break;
							case REPEAT_ALL:
								currIndex++;
						     	if(currIndex > filePathArray.size() -1)
						     		currIndex = 0;
						     	playCurrentIndexMedia();
						     	break;
							case REPEAT_OFF:
								VideoPlayerActivity.this.finish();
								break;
						}
						
					}
					
				});
				
				mPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
					
					@Override
					public void onSeekComplete(MediaPlayer mp) {
						// TODO Auto-generated method stub
						
					}
				});
				
				mPlayer.setOnInfoListener(new OnInfoListener(){

					@Override
					public boolean onInfo(MediaPlayer mp, int what, int extra) {
						// TODO Auto-generated method stub
						Logger.e(tag, "VideoInfoListener" + "---what = "+what+"----");
						switch(what) {
							case 0x10000003: {
								
							} break;
							case 0x20000003:	////FATALERR_VIDEO_MPEG2DEC_UNKNOWN_FRAME_RATE
							case 0x2000000d:{	////FATALERR_VIDEO_MPEG4DEC_UNKNOWN_FRAME_RATE_CODE
								
							} break;
							case 0x20000008: {	//FATALERR_VIDEO_MPEG2DEC_UNSUPPORTED_RESOLUTION
								
							} break;
							case 722: { /*MEDIA_INFO_FE_PB_RESET_SPEED*/
								Logger.v(tag, "MEDIA_INFO_FE_PB_RESET_SPEED");
								ffIndex = fwIndex = sfIndex = swIndex = -1;
								Message msg = uiHandler.obtainMessage();
								msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS;
								msg.arg1 = PlaybackStatus.STATUS_PLAY;
								uiHandler.sendMessage(msg);
							} break;
							case 702:
							case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: {	// MEDIA_INFO_VIDEO_RENDERING_START
								
							} break;
							default : {
								
							}break;
						}
					
						return false;
					}
					
				});
			}

			@Override
			public boolean setDataSource() {
				// TODO Auto-generated method stub
				//when setDataSource , need 
				// 1. device not removed 2. currIndex > 0
				if(currIndex  < 0 || currIndex >= filePathArray.size()) {	// we should make sure currIndex valid
					Logger.e(tag, "not allowed happen");
					return false;
				}
				
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				
				if(currIndex < filePathArray.size())
	        	{
					Logger.v(tag, "setDataSource now");
					Logger.v(tag, filePathArray.get(currIndex));
	        		try {
	        			
	        			if(DEBUG_SUBTITLE) {
	        				mPlayer.setDataSource("/mnt/udisk/sda/dianying/jafeimao.VOB");
	        				return true;
	        			}

	        			if(mPlayer == null) {
	        				Logger.v(tag, "mPlayer null");
	        				return false;
	        			}
						String tmpProtolinfo = "";
						boolean bDTCP = false;
						String tmpFilePath = null;
						tmpFilePath = filePathArray.get(currIndex);
						Logger.v(tag, tmpFilePath);
						
						String tmpContentLength = null;
	        			tmpContentLength = DLNADataProvider.queryDataByID(fileCharIDArray.get(currIndex), DLNADataProvider.UPNP_DMP_RES_SIZE);
	        			if(tmpContentLength != null) {
	        				tmpFilePath = tmpFilePath + " contentlength=" + tmpContentLength;
	        			}
	        			String tmpDuration = null;
	        			tmpDuration = DLNADataProvider.queryDataByID(fileCharIDArray.get(currIndex), DLNADataProvider.UPNP_DMP_RES_DURATION);
	        			globalDuration = tmpDuration;
	        			if(tmpDuration != null) {
	        				tmpFilePath = tmpFilePath + " duration=" + tmpDuration;
	        			}
						
						tmpProtolinfo = DLNADataProvider.queryDataByID(fileCharIDArray.get(currIndex),DLNADataProvider.UPNP_DMP_RES_PROTOCOLINFO);
						// Filter those not able play
						// ablePlay = false; DLNA Test close by star_he
						// ablePlay = checkProtolInfo(tmpProtolinfo); DLNA Test close by star_he
						/*	DLNA Test close by star_he
						if(!ablePlay) {
							//map.AddErrorVideo(filePathArray.get(currIndex));
							if(isActivityReady) {
								uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
								showMessageHint(VideoPlayerActivity.this.getResources().getString(R.string.msg_unsupport));
								ablePlay = !ablePlay;
}
							return false;
						}*/
						// Filter those not able play
						isSupportResume = true;
						if(tmpProtolinfo != null){
							if(tmpProtolinfo.contains("DLNA.ORG_OP")){	
								String operation = null;
								int len = "DLNA.ORG_OP=".length();
								operation = tmpProtolinfo.substring(tmpProtolinfo.indexOf("DLNA.ORG_OP"));
								operation = operation.substring(len);
								if(operation.charAt(0) == '1' || operation.charAt(1) == '1'){
									isSupportSeek = true;
									isSupportResume = true;
								}else {
									isSupportSeek = false;
									isSupportResume = false;
								}
								if(operation.charAt(0) == '0' && operation.charAt(1) == '1'){
									isSupportJPSeek = false;
								}else if(operation.charAt(0) == '1'){
									isSupportJPSeek = true;
								}
							}else {
								isSupportSeek = false;
								isSupportResume = false;
								isSupportJPSeek = false;
							}
							if(tmpProtolinfo.contains("DLNA.ORG_FLAGS")){	
								String flags = null;
								int len = "DLNA.ORG_FLAGS=".length();
								flags = tmpProtolinfo.substring(tmpProtolinfo.indexOf("DLNA.ORG_FLAGS"));
								flags = flags.substring(len,len + 6);
								String binaryFlags = hexString2binaryString(flags);
								char bit21 = binaryFlags.charAt(10);
								if(bit21 == '1'){
									isSupportStalling = true;
								}else{
									isSupportStalling = false;
								}
								char bit16 = binaryFlags.charAt(15);
								char bit15 = binaryFlags.charAt(16);
								char bit14 = binaryFlags.charAt(17);
								char bit23 = binaryFlags.charAt(8);
								if (bit16 == '1') {
				                     // bit 15: cleartextbyteseek-full flag
				                     if ((bit15 == '1') || (bit14 == '1' && bit23 == '0'))
				                    	 isSupportSeek = true;
				                }
							}else {
								isSupportStalling = false;
							}
							bDTCP = tmpProtolinfo.contains("DTCP1HOST");
							if(!bDTCP){
								if(tmpFilePath != null && tmpFilePath.contains("?")){
									String tmpPath = tmpFilePath.substring(tmpFilePath.indexOf("?"));
									if(tmpPath.contains("CONTENTPROTECTIONTYPE=DTCP1")){
										bDTCP = true;
									}
								}
							}
							if(bDTCP){
								tmpProtolinfo = " protocolinfo=" + tmpProtolinfo.substring(0, tmpProtolinfo.lastIndexOf(":") + 1) + "*";
							}else{
								tmpProtolinfo = "";
							}
						}
						
						tmpFilePath = tmpFilePath + tmpProtolinfo;
	        			Log.v(tag, "***" + tmpFilePath);
	        			mPlayer.setDataSource(tmpFilePath);
	        			if(isSupportStalling && isJPTS){
	        				mPlayer.setParameter(2000, 1);
	        			}else {
	        				mPlayer.setParameter(2000, 0);
	        			}
						/*int fileType = DLNADataProvider.getFileType(videoList.get(currIndex).getFileName());
						if(fileType == 34){
							mPlayer.setParameter(1900, fileType);
						}
						Log.v(tag, "Video file type: " + fileType);*/
	        			return true;
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
				
				return false;
			}
			
		};
    }
	
	
	private void captureIntent(Intent intent)
    {
    	int totalCnt = 0;
    	int j = 0;
    	currIndex = intent.getIntExtra("initPos", 0);
    	resumeIndex = intent.getIntExtra("resume_index", -1);
    	Logger.v(tag, "captureIntent()" + "currIndex--" + currIndex);
    	Logger.v(tag, "captureIntent()" + "resumeIndex--" + resumeIndex);
    	
		if (videoList == null) {
			videoList = map.getFileList();
		}
		
		totalCnt = videoList.size();
		Logger.v(tag, "captureIntent()" + "totalCnt--" + totalCnt);
		for(int i = 0; i < totalCnt; i++)
		{
			if(videoList.get(i).getFileType() == FileFilterType.DEVICE_FILE_VIDEO)
			{
				filePathArray.add(j, videoList.get(i).getFilePath());
				fileTitleArray.add(j, videoList.get(i).getFileName());
				fileCharIDArray.add(j, videoList.get(i).getUniqueCharID());
				filePos.add(j, new Integer(i));
				j++;
			}
		}
		fileArraySize = j;
    }
    private void captureIntent_TvAnywhere(Intent intent) {
		// TODO Auto-generated method stub
    	
    	currIndex = intent.getIntExtra("initPos", 0);
    	int len = intent.getIntExtra("len", 1);
    	String [] filelist = intent.getStringArrayExtra("filelist");

    	for(int i = 0; i < len; i++)
    	{
    		filePathArray.add(i,filelist[i]);
    		fileTitleArray.add(i, filelist[i].substring(filelist[i].lastIndexOf("/")+1));
    		filePos.add(i, new Integer(i));
    	}
    	fileArraySize = len;
	}
    
    private void setAudioSpdifOutput(int mode)
    {
    	mTVService.setAudioSpdifOutput(mode);
		
    	if(mode == AUDIO_DIGITAL_RAW) {
    		Toast.makeText(this, this.getResources().getString(R.string.raw), Toast.LENGTH_LONG).show();
    	}
    	else if(mode == AUDIO_DIGITAL_LPCM_DUAL_CH) {
    		Toast.makeText(this, this.getResources().getString(R.string.LPCM), Toast.LENGTH_LONG).show();
    	}
    }
	
	
	private void quickShowBanner() {
		if(movBannerView.getVisibility() != View.VISIBLE) {
			movBannerView.setVisibility(View.VISIBLE);
			movBannerView.bringToFront();
			// setNavVisibility(true);
		}
	}
	
	private void delayShowBanner() {
		if(movBannerView.getVisibility() != View.VISIBLE)
		{	
			movBannerView.setVisibility(View.VISIBLE);
			movBannerView.bringToFront();
			movBannerView.clearAnimation();
			TranslateAnimation TransAnim = null;
			TransAnim = new TranslateAnimation(0.0f,0.0f, banner_h,0.0f);	
			TransAnim.setDuration(bannerAnimTime);
			movBannerView.startAnimation(TransAnim);
			// setNavVisibility(true);
		}
	}
	
	private void quickHideBanner() {
		if(movBannerView.getVisibility() == View.VISIBLE) {
			movBannerView.setVisibility(View.INVISIBLE);
			// setNavVisibility(false);
		}
	}
	
	private void delayHideBanner() {
		if(movBannerView.getVisibility() == View.VISIBLE) {
			Logger.v(tag, "OK");
			movBannerView.clearAnimation();
			TranslateAnimation TransAnim = null;
			TransAnim = new TranslateAnimation(0.0f,0.0f, 0.0f,banner_h);	
			TransAnim.setDuration(bannerAnimTime);
			movBannerView.startAnimation(TransAnim);
			movBannerView.setVisibility(View.INVISIBLE);
			// setNavVisibility(false);
		}
	}
	
	private void copy_task_hide_controler(final int controlVal) {	
		
		if(task_hide_controler != null) {
			task_hide_controler.cancel();
			task_hide_controler = null;
		} 
		
		task_hide_controler = new TimerTask() {
			public void run() {
				// TODO Auto-generated method stub
				switch (controlVal) {
					case HandlerControlerVariable.QUICK_SHOW_BANNER:
						uiHandler.sendEmptyMessage(HandlerControlerVariable.QUICK_SHOW_BANNER);
						break;
					case HandlerControlerVariable.DELAY_SHOW_BANNER:
						uiHandler.sendEmptyMessage(HandlerControlerVariable.DELAY_SHOW_BANNER);
						break;
					case HandlerControlerVariable.QUICK_HIDE_BANNER:
						uiHandler.sendEmptyMessage(HandlerControlerVariable.QUICK_HIDE_BANNER);
						break;
					case HandlerControlerVariable.DELAY_HIDE_BANNER:
						uiHandler.sendEmptyMessage(HandlerControlerVariable.DELAY_HIDE_BANNER);
						break;
				
				} 
			}
		};
	}
	private void cancel_task_hide_controler() {
		if(task_hide_controler != null) {
			task_hide_controler.cancel();
			task_hide_controler = null;
		}
	}
	
	
	
	private void onAndOffBanner() {
		if(!isAbleShown) {
			cancel_task_hide_controler();
			uiHandler.sendEmptyMessage(HandlerControlerVariable.QUICK_HIDE_BANNER);
			return ;
		}
		if(movBannerView.getVisibility() == View.VISIBLE) {
			copy_task_hide_controler(HandlerControlerVariable.QUICK_HIDE_BANNER);
			if(timer != null) {
				timer.schedule(task_hide_controler, 0);
			}
		} else {
			uiHandler.sendEmptyMessage(HandlerControlerVariable.QUICK_SHOW_BANNER);
			copy_task_hide_controler(HandlerControlerVariable.QUICK_HIDE_BANNER);
			if(timer != null) {
				timer.schedule(task_hide_controler, delay_6s);
			}
		}
	}
	
	private void showAndWaitToHideBanner() {
		if(!isAbleShown) {
			cancel_task_hide_controler();
			uiHandler.sendEmptyMessage(HandlerControlerVariable.QUICK_HIDE_BANNER);
			return ;
		}
		if(movBannerView.getVisibility() == View.VISIBLE) {
			copy_task_hide_controler(HandlerControlerVariable.QUICK_HIDE_BANNER);
			if(timer != null) {
				timer.schedule(task_hide_controler, delay_6s);
			}
		} else {
			uiHandler.sendEmptyMessage(HandlerControlerVariable.QUICK_SHOW_BANNER);
			copy_task_hide_controler(HandlerControlerVariable.QUICK_HIDE_BANNER);
			if(timer != null) {
				timer.schedule(task_hide_controler, delay_6s);
			}
		}
	}

	private void copy_task_getCurrentPositon() {	
		
		cancel_task_getCurrentPosition();
		task_getCurrentPositon = new TimerTask() {
			public void run() {
				// TODO Auto-generated method stub
				if(movBannerView.getVisibility() == View.VISIBLE) {
					uiHandler.sendEmptyMessage(HandlerControlerVariable.UPDATETIMEBARANDTIMENOW);
				}
			}
		};
	}
	
	private void cancel_task_getCurrentPosition() {
		if(task_getCurrentPositon != null) {
			task_getCurrentPositon.cancel();
			task_getCurrentPositon = null;
		} 
	}
	
	private void keepOnUpdateBarAndTime() {
		if(movBannerView.getVisibility() == View.VISIBLE) {
			copy_task_getCurrentPositon();
			if(timer != null) {
				timer.schedule(task_getCurrentPositon, 0, delay_300ms);
			}
			return ;
		} else {
			cancel_task_getCurrentPosition();
			return ;
		}
	}
	
	private void updateTimebarAndTimeNow() {	
		int minute = 0;
		int hour = 0; 
		int second = 0;
		
		int i = playbackControl.mp_getCurrentPosition();
		Logger.v("GetCurrentPosition", i + "");
		
		if(i == -1) {
			timeNow.setText(String.format("%02d:%02d:%02d", hour, minute, second));
			timeBar.setProgress(0);
			return ;
		}
		if(isSeeking) {
			return ;
		}
		timeBar.setProgress(i);
		i /= 1000;
		minute = i / 60;
		hour = minute / 60;
		second = i % 60;
		minute %= 60;
		timeNow.setText(String.format("%02d:%02d:%02d", hour, minute, second));
	}
	
	private void updateTimeEnd()	
	{
		Logger.v(tag, "get End Time");
		int max = playbackControl.mp_getDuration();
		
		Logger.v(tag, "End Time" + max);
		if(max == -1) {
			if(globalDuration != null) {
				max = Integer.parseInt(globalDuration);
				globalDuration = null;
			}
		}
		if(max == -1 || max == 0) {
			timeEnd.setText(null);
			disableSeekBar();
			return ;
		}
		enableSeekBar();
		timeBar.setMax(max);
		max /= 1000;	
		Minute = max / 60;
		Hour = Minute / 60;
		Second = max % 60;
		Minute %= 60;
		
		timeEnd.setText(String.format("%02d:%02d:%02d", Hour, Minute, Second));
		timeBar.setProgress(0);
	}
	/*
	private void cancel_task_updateSleepTime() {
		if(task_updateSleepTime != null) {
			task_updateSleepTime.cancel();
			task_updateSleepTime = null;
		} 
	}
	
	private void copy_task_updateSleepTime() {
		cancel_task_updateSleepTime();
		task_updateSleepTime = new TimerTask() {
			public void run() {
				// TODO Auto-generated method stub
				//update SleepTime
				Logger.v(tag, "update SleepTime !!!!!!!!!!!!!!!!!!!!!!!!!");
				int mins = getSleepTimeValue();
				mSleepTimeHour = mins / 60;
				mSleepTimeMin = mins % 60;
			}
		};
	}
	private int getSleepTimeValue()
	{
		int sethour = Settings.Global.getInt(m_ContentMgr, "SetTimeHour", 0);
		int setmin = Settings.Global.getInt(m_ContentMgr, "SetTimeMinute", 0);
		int setsec = Settings.Global.getInt(m_ContentMgr, "SetTimeSecond", 0);
		int totalmin = Settings.Global.getInt(m_ContentMgr, "TotalMinute", 0);
		Logger.d("RTK_DEBUG", "SetTimeHour:" + sethour + ",SetTimeMinute:" + setmin +",SetTimeSec:" + setsec + ",TotalMinute:" + totalmin);
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
	*/
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Logger.v(tag, "click");
		int itemId = v.getId();
		switch(itemId) {
			case R.id.main:
				Logger.v(tag, "R.id.main");
				onAndOffBanner();
				break;
			case R.id.movie_banner:
				Logger.v(tag, "R.id.movBanner");
				showAndWaitToHideBanner();
				break;
			case R.id.c_playandpause:
				Logger.v(tag, "pause or start");
				
				showAndWaitToHideBanner();
				keepOnUpdateBarAndTime();
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_FOCUS);
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_STARTANIMATION);
				playAndPause();
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
				break;
			case R.id.c_slowforward :
				
				showAndWaitToHideBanner();
				keepOnUpdateBarAndTime();
				if((!isJPTS && isSupportSeek) || (!isJPTS && isSupportStalling) || (isJPTS && isSupportJPSeek) || (isJPTS && isSupportStalling)){
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_SLOWFORWARD_FOCUS);
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_STARTANIMATION);
				f_slowForward();
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
				}else {
					showShortToast("Server is not support SlowForward");
				}
				break;
			case R.id.c_slowbackward :
				
				showAndWaitToHideBanner();
				keepOnUpdateBarAndTime();
				if((!isJPTS && isSupportSeek) || (isJPTS && isSupportJPSeek)){
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_SLOWBACKWARD_FOCUS);
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_STARTANIMATION);
					f_slowBackward();
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
				}else {
					showShortToast("Server is not support SlowBackward");
				}
				break;
			case R.id.c_fastforward :
				
				showAndWaitToHideBanner();
				keepOnUpdateBarAndTime();
				if((!isJPTS && isSupportSeek) || (isJPTS && isSupportJPSeek)){
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_FASTFORWARD_FOCUS);
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_STARTANIMATION);
					f_fastForward();
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
				}else {
					showShortToast("Server is not support FastForward");
				}
				break;
			case R.id.c_fastbackward :
				
				showAndWaitToHideBanner();
				keepOnUpdateBarAndTime();
				if((!isJPTS && isSupportSeek) || (isJPTS && isSupportJPSeek)){
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_FASTBACKWARD_FOCUS);
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_STARTANIMATION);
					f_fastBackward();
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
				}else {
					showShortToast("Server is not support FastBackward");
				}
				break;
			case R.id.c_skipnext :
				
				showAndWaitToHideBanner();
				keepOnUpdateBarAndTime();
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_SKIPNEXT_FOCUS);
				f_skipNext();
				break;
			case R.id.c_skipbefore :
				
				showAndWaitToHideBanner();
				keepOnUpdateBarAndTime();
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_SKIPPREVIOUS_FOCUS);
				f_skipBefore();
				break;
			case R.id.play_mode: {
				changePlayMode();
				Message msg = uiHandler.obtainMessage();
				msg.what = HandlerControlerVariable.MSG_SET_REPEATMODE_ICON;
				switch(repeat_mode) {
					case REPEAT_ALL:
						msg.arg1 = REPEAT_ALL;
						break;
					case REPEAT_SINGLE:
						msg.arg1 = REPEAT_SINGLE;
						break;
					case REPEAT_OFF:
						msg.arg1 = REPEAT_OFF;
						break;
				}
				uiHandler.sendMessage(msg);
			} break;
			case R.id.play_picturesize :
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_STARTANIMATION);
				setPicSize();
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DISSMISSANIMATION);
				break;
			case R.id.main_menu :
				onMenu();
				break;
		}
		
	}

	@Override
	public boolean onDown(MotionEvent arg0) {	//G
		// TODO Auto-generated method stub
		Logger.v(tag, "onDown");
		
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {	//G
		// TODO Auto-generated method stub
		Logger.v(tag, "onFling");
		if (e1.getX()-e2.getX() > FLING_MIN_DISTANCE 
				&& Math.abs(velocityX) > FLING_MIN_VELOCITY) { // Fling left      
				
				Toast.makeText(this, "left ", Toast.LENGTH_SHORT).show();
				f_skipNext();
				return true;
		} else if (e2.getX()-e1.getX() > FLING_MIN_DISTANCE 
				&& Math.abs(velocityX) > FLING_MIN_VELOCITY) {   // Fling right    
				f_skipBefore();
				Toast.makeText(this, "right ", Toast.LENGTH_SHORT).show();
			return true;
		}  
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {	//G
		// TODO Auto-generated method stub
		Logger.v(tag, "onLongPress");
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {	//G
		// TODO Auto-generated method stub
		Logger.v(tag, "onScroll");
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {	//G
		// TODO Auto-generated method stub
		Logger.v(tag, "onShowPress");
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		Logger.v(tag, "onSingleTapUp");
		return false;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		Logger.v(tag, "on Touch");
		
		return mGestureDetector.onTouchEvent(event);
	}

	
	
	/*
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		//showAndWaitToHideBanner();
		Logger.v(tag, "on key down");
		switch(keyCode) {
			case KeyEvent.KEYCODE_ESCAPE : 
			case KeyEvent.KEYCODE_HOME :{//for L4300 KeyEvent.KEYCODE_ESCAPE
				Logger.v(tag, "ESC press");
				beforeOnPauseRecordResumePoint = true;
				recordResumePoint();
			} break;
			//case KeyEvent.KEYCODE_J:   // As [Exit] key
			//return true;
			case KeyEvent.KEYCODE_BACK : {
						
			} break;
			case KeyEvent.KEYCODE_INFO :
			case KeyEvent.KEYCODE_M :	
			case 231 :	//for L4300 KeyEvent.KEYCODE_STOP
			case 257 :	//DISPLAY KEY in REALTECK RCU
			case KeyEvent.KEYCODE_K:	// As [Stop] key
			; break;
			case 228: //for L4300 KeyEvent.KEYCODE_HOLD:
			case 256: //VIDEO KEY  in realtek RCU
			case KeyEvent.KEYCODE_R: //rewind
			break;
			case KeyEvent.KEYCODE_O:
				if(AUDIO_DIGITAL_OUTPUT_MODE == AUDIO_DIGITAL_RAW)
	     		{
	     			setAudioSpdifOutput(AUDIO_DIGITAL_LPCM_DUAL_CH);
	     			AUDIO_DIGITAL_OUTPUT_MODE = AUDIO_DIGITAL_LPCM_DUAL_CH;
	     		}else
	     		{
	     			setAudioSpdifOutput(AUDIO_DIGITAL_RAW);
	     			AUDIO_DIGITAL_OUTPUT_MODE = AUDIO_DIGITAL_RAW;
	     		}

		}
		return super.onKeyDown(keyCode, event);
	}
	*/
	
	public void f_play() {
		if(!checkIndexValid()) {
			return ;
		}
		
		if(checkAndRequestAF()) {
			boolean needUpdateUI = false;
			if(playbackControl.isPlaying() && isOnNotNormalPlay) {
				if(playbackControl.mp_normalPlay()) {
					isOnNotNormalPlay = false;
					ctr_direction_ff = ctr_direction_fw = 0;
					ffIndex = fwIndex = sfIndex = swIndex = -1;
					needUpdateUI = true;
				}
			} else {
				if(playbackControl.mp_start()) {
					needUpdateUI = true;
				}
			}
			
			if(needUpdateUI) {
				uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPAUSE);
				Message msg = uiHandler.obtainMessage();
				msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS ;
				msg.arg1 = PlaybackStatus.STATUS_PLAY;
				uiHandler.sendMessage(msg);
			}
		} else {
			uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_HINT_NOTGETAUDIOFOCUS);
		}
	}
	
	public void f_pause() {
		if(!checkIndexValid()) {
			return ;
		}
		if(playbackControl.mp_pause()) {
			uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
			Message msg = uiHandler.obtainMessage();
			msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS ;
			msg.arg1 = PlaybackStatus.STATUS_PAUSE;
			uiHandler.sendMessage(msg);
		}
	}
	
	public void playAndPause() {
		if(!checkIndexValid()) {
			return ;
		}
		
		if(playbackControl.isPlaying()) {
			// do start or pause
			if(isOnNotNormalPlay) {
				f_play();
			} else {
				if((!isJPTS && isSupportSeek) || (isJPTS && isSupportJPSeek)){
					f_pause();
				}else{
					showShortToast("Server is not support pause");
				}
			}
		} else {
			f_play();
		}
	}
	
	public void f_stop() {
		if(!checkIndexValid()) {
			return ;
		}
		if(playbackControl.mp_stop()) {
			//UPDATE UI
			ffIndex = fwIndex = sfIndex = swIndex = -1;
			ctr_direction_ff = ctr_direction_fw = 0;
			isOnNotNormalPlay = false;
			uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
			Message msg = uiHandler.obtainMessage();
			msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS ;
			msg.arg1 = PlaybackStatus.UNKNOWN;
			uiHandler.sendMessage(msg);
		}
	}
	
	public void f_slowForward() {
		if(!checkIndexValid()) {
			return ;
		}
		
		if(!playbackControl.isPlaying()) {
			f_play();
			return;
		}
		
		ffIndex = fwIndex = swIndex = -1;
		ctr_direction_ff = ctr_direction_fw = 0;
		
		isOnNotNormalPlay = true;
		sfIndex++;
		if(sfIndex >= sfRate.length) {
			sfIndex = 0;
		}
		Message msg = uiHandler.obtainMessage();
		msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS ;
		switch(sfIndex) {
			case 0:
				msg.arg1 = PlaybackStatus.STATUS_SF1;
				break;
			case 1:
				msg.arg1 = PlaybackStatus.STATUS_SF2;
				break;
			case 2:
				msg.arg1 = PlaybackStatus.STATUS_SF3;
				break;
			case 3:
				msg.arg1 = PlaybackStatus.STATUS_SF4;
				break;
		}
		uiHandler.sendMessage(msg);
		uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
		playbackControl.mp_slowForward(sfRate[sfIndex]);
		return ;
		
	}
	
	public void f_slowBackward() {
		if(!checkIndexValid()) {
			return ;
		}
		
		if(!playbackControl.isPlaying()) {
			f_play();
			return;
		}
		
		
		ffIndex = fwIndex = sfIndex = -1;
		ctr_direction_ff = ctr_direction_fw = 0;
			
		isOnNotNormalPlay = true;
		swIndex++;
		if(swIndex >= swRate.length) {
			swIndex = 0;
		}
		Message msg = uiHandler.obtainMessage();
		msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS ;
		switch(swIndex) {
			case 0:
				msg.arg1 = PlaybackStatus.STATUS_SW1;
				break;
			case 1:
				msg.arg1 = PlaybackStatus.STATUS_SW2;
				break;
			case 2:
				msg.arg1 = PlaybackStatus.STATUS_SW3;
				break;
			case 3:
				msg.arg1 = PlaybackStatus.STATUS_SW4;
				break;
		}
		uiHandler.sendMessage(msg);
		
		uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
		playbackControl.mp_slowBackward(swRate[swIndex]);
		
	}
	
	public void f_fastForward() {
		if(!checkIndexValid()) {
			return ;
		}
		
		if(!playbackControl.isPlaying()) {
			f_play();
			return;
		}
		
		fwIndex = sfIndex = swIndex = -1;
		ctr_direction_fw = 0;
		
		isOnNotNormalPlay = true;
		if(ctr_direction_ff == 0) {
			ctr_direction_ff = 1;
			ffIndex = 0;
		} else if(ffIndex == 0 && ctr_direction_ff == -1){
			ffIndex = 1;
			ctr_direction_ff = 1;
		} else if(ffIndex == ffRate.length - 1 && ctr_direction_ff == 1) {
			ctr_direction_ff = -1;
			ffIndex = ffRate.length - 2;
		} else {
			if(ctr_direction_ff == -1) {
				ffIndex--;
			}
			if(ctr_direction_ff == 1) {
				ffIndex++;
			}
		}
		
		
		Message msg = uiHandler.obtainMessage();
		msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS ;
		switch(ffIndex) {
			case 0:
				msg.arg1 = PlaybackStatus.STATUS_FF1;
				break;
			case 1:
				msg.arg1 = PlaybackStatus.STATUS_FF2;
				break;
			case 2:
				msg.arg1 = PlaybackStatus.STATUS_FF3;
				break;
			case 3:
				msg.arg1 = PlaybackStatus.STATUS_FF4;
				break;
			default:
				ffIndex = 0;
				Logger.e(tag, "InValidState !!!f_fastForward()");
				break;
		}
		uiHandler.sendMessage(msg);
		uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
		playbackControl.mp_fastForward(ffRate[ffIndex]);
		
	}
	
	public void f_fastBackward() {
		if(!checkIndexValid()) {
			return ;
		}
		
		if(!playbackControl.isPlaying()) {
			f_play();
			return;
		}
		
		ffIndex = sfIndex = swIndex = -1;
		ctr_direction_ff = 0;
		
		isOnNotNormalPlay = true;
		
		
		if(ctr_direction_fw == 0) {
			ctr_direction_fw = 1;
			fwIndex = 0;
		} else if(fwIndex == 0 && ctr_direction_fw == -1){
			fwIndex = 1;
			ctr_direction_fw = 1;
		} else if(fwIndex == fwRate.length - 1 && ctr_direction_fw == 1) {
			ctr_direction_fw = -1;
			fwIndex = fwRate.length - 2;
		} else {
			if(ctr_direction_fw == -1) {
				fwIndex--;
			}
			if(ctr_direction_fw == 1) {
				fwIndex++;
			}
		}
		
		Message msg = uiHandler.obtainMessage();
		msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS ;
		switch(fwIndex) {
			case 0:
				msg.arg1 = PlaybackStatus.STATUS_FW1;
				break;
			case 1:
				msg.arg1 = PlaybackStatus.STATUS_FW2;
				break;
			case 2:
				msg.arg1 = PlaybackStatus.STATUS_FW3;
				break;
			case 3:
				msg.arg1 = PlaybackStatus.STATUS_FW4;
				break;
			default :
				Logger.e(tag, "Invalid State!!!f_fastBackward()");
				fwIndex = 0;
				break;
		}
		uiHandler.sendMessage(msg);
		uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
		playbackControl.mp_fastBackward(fwRate[fwIndex]);
		
	}
	
	public void f_skipNext(){
		playbackControl.setListenersNull();
		Message msg = uiHandler.obtainMessage();
		msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS;
		msg.arg1 = PlaybackStatus.STATUS_SKIPNEXT;
		uiHandler.sendMessage(msg);
		
		ffIndex = fwIndex = sfIndex = swIndex = -1;
		ctr_direction_ff = ctr_direction_fw = 0;
		isOnNotNormalPlay = false;
		isSeeking = false;
		
		switch(repeat_mode) {
			case REPEAT_ALL:
				currIndex++;
				if(currIndex >= filePathArray.size()) {
					currIndex = 0;
				}
				SKIPFLAG = 1;
				break;
			case REPEAT_SINGLE:
				SKIPFLAG = 0;
				break;
			case REPEAT_OFF :
				currIndex++;
				SKIPFLAG = 1;
				if(currIndex >= filePathArray.size()) {
					//send Intent to MediaBrowser
					currIndex = filePathArray.size();
					playbackControl.mp_reset();
					showMessageHintDelayHide("NotAvailabale", delay_2000ms);
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
					Message msg1 = uiHandler.obtainMessage();
					msg1.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS;
					msg1.arg1 = PlaybackStatus.UNKNOWN;
					uiHandler.sendMessage(msg1);
					disableSeekBar();
					return ;
				}
				break;
		}
		
		playCurrentIndexMedia();
		
	}
	
	public void f_skipBefore() {
		playbackControl.setListenersNull();
		Message msg = uiHandler.obtainMessage();
		msg.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS;
		msg.arg1 = PlaybackStatus.STATUS_SKIPBEFORE;
		uiHandler.sendMessage(msg);
		ffIndex = fwIndex = sfIndex = swIndex = -1;
		ctr_direction_ff = ctr_direction_fw = 0;
		isOnNotNormalPlay = false;
		isSeeking = false;
		int currentPos = playbackControl.mp_getCurrentPosition();
		Logger.i(tag, "when skip before goes here " + playbackControl.state);
		
		if(!((currentPos > 2000) && (playbackControl.state == playbackControl.STARTED))) {
			switch(repeat_mode) {
				case REPEAT_ALL:
					currIndex--;
					if(currIndex < 0) {
						currIndex = filePathArray.size() - 1;
					}
					SKIPFLAG = -1;
					break;
				case REPEAT_SINGLE:
					SKIPFLAG = 0;
					break;
				case REPEAT_OFF :
					currIndex--;
					SKIPFLAG = -1;
					if(currIndex < 0) {
						currIndex = -1;
						playbackControl.mp_reset();
						timeNow.setText("00:00:00");
						timeEnd.setText("00:00:00");
						timeBar.setMax(Integer.MAX_VALUE);
						timeBar.setProgress(0);
						showMessageHintDelayHide("NotAvailabale", delay_2000ms);
						uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY);
						Message msg1 = uiHandler.obtainMessage();
						msg1.what = HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS;
						msg1.arg1 = PlaybackStatus.UNKNOWN;
						uiHandler.sendMessage(msg1);
						disableSeekBar();
						return ;
					}
					break;
			}
		}	
		Log.e(tag, "still go here!!");
		playCurrentIndexMedia();
	}
	
	public void playCurrentIndexMedia() {
		uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_STARTANIMATION);
		timeNow.setText("00:00:00");
		timeEnd.setText("00:00:00");
		timeBar.setMax(Integer.MAX_VALUE);
		timeBar.setProgress(0);
		playbackControl.mp_reset();
		playbackControl.setListeners();
		playbackControl.mp_setDataSource();
		playbackControl.mp_prepare();
	}
	
	public void changePlayMode() {
		showAndWaitToHideBanner();
		switch(repeat_mode) {
			case REPEAT_ALL:
				repeat_mode = REPEAT_SINGLE;
				break;
			case REPEAT_SINGLE:
				repeat_mode = REPEAT_OFF;
				//ui set to grey
				break;
			case REPEAT_OFF:
				repeat_mode = REPEAT_ALL;
				break;
		}
	}
	
	public void setPicSize()
    {
		showAndWaitToHideBanner();
    	switch(selected_idx)
    	{
    	case 0:	// from normal to super live
    		mTVService.setAspectRatio(TvManager.SCALER_RATIO_PANORAMA);
    		//showShortToast(this.getResources().getString(R.string.picture_super_live));
    		selected_idx++;
    		break;
    	case 1:	// from super live to zoom
    		mTVService.setAspectRatio(TvManager.SCALER_RATIO_BBY_ZOOM);
    		//showShortToast(this.getResources().getString(R.string.picture_dot_by_dot));
    		selected_idx++;
    		break;
    	case 2:	// from zoom to dot by dot
    		mTVService.setAspectRatio(TvManager.SCALER_RATIO_POINTTOPOINT);
    		//showShortToast(this.getResources().getString(R.string.picture_normal));
    		selected_idx++;
    		break;
    	case 3: // from dot to normal
    		mTVService.setAspectRatio(TvManager.SCALER_RATIO_BBY_AUTO);
    		//showShortToast(this.getResources().getString(R.string.picture_zoom));
    		selected_idx = 0;
    		break;
    	default:
    		break;
    	}
    }
	
	private void getSubtitleInfo()
    {
		subtitleInfo = playbackControl.mp_getSubtitleInfo();
		if(subtitleInfo == null) {
			Logger.e(tag, "can't getSubtitleInfo");
			curr_subtitle_stream_num = 0;
			subtitle_num_Stream = 0;
			return ;
		}
		
    	subtitle_num_Stream = subtitleInfo[1];
		curr_subtitle_stream_num = subtitleInfo[2];
    }
	
	private void setSubtitle()
	{	
		if(curr_SPU_ENABLE == 0) {	// tell to set subtitle notEnable
			Logger.v(tag, "curr_SP_ENABLE = 0");
			if(subtitle_num_Stream > 0) {
				curr_subtitle_stream_num = 1;
				mPlayer.setSubtitleInfo(curr_subtitle_stream_num, curr_SPU_ENABLE, curr_textEncoding, curr_textColor, curr_fontSize);
				curr_subtitle_stream_num = 0;
			}
			return ;
		}
		
		if(curr_SPU_ENABLE == 1) {
			Logger.v(tag, "curr_SP_ENABLE = 1");
			if(subtitle_num_Stream <= 0) {
				curr_subtitle_stream_num = 0;
				return ;
			}
			if(subtitle_num_Stream > 0 && curr_subtitle_stream_num <= 0) {
				curr_subtitle_stream_num = 1;
				mPlayer.setSubtitleInfo(curr_subtitle_stream_num, curr_SPU_ENABLE, curr_textEncoding, curr_textColor, curr_fontSize);
				return ;
			}
			
			if(subtitle_num_Stream > 0 && curr_subtitle_stream_num > subtitle_num_Stream) {
				curr_SPU_ENABLE = 0;
				curr_subtitle_stream_num = 1;
				mPlayer.setSubtitleInfo(curr_subtitle_stream_num, curr_SPU_ENABLE, curr_textEncoding, curr_textColor, curr_fontSize);
				curr_subtitle_stream_num = 0;
				return ;
			}
			mPlayer.setSubtitleInfo(curr_subtitle_stream_num, curr_SPU_ENABLE, curr_textEncoding, curr_textColor, curr_fontSize);
			return ;
		}
	}


	private void getAudioTrackInfo()
    {
		audioInfo = playbackControl.mp_getAudioTrackInfo(-1);
    	if(audioInfo == null) {
    		Logger.e(tag, "get AudioTrack info error");
    		audio_num_stream = 0;
    		curr_audio_stream_num = -1;
    		curr_audio_type = defaultAudioType;
    		return ;
    	}
    	audio_num_stream = audioInfo[1];
    	curr_audio_stream_num = audioInfo[2];
    	curr_audio_type = Utility.AUDIO_TYPE_TABLE(audioInfo[3]);
    	Logger.i(tag, "audio_num_stream = " + audio_num_stream);
    	Logger.i(tag, "curr_audio_stream_num = " + curr_audio_stream_num);
    	
    	if(curr_audio_type.equals("Dolby AC3")) {
    		///Update UI
    		dolby.setText("Dolby AC3");
    	} else if(curr_audio_type.equals("Dolby Digital Plus")) {
    		///Update UI
    		dolby.setText("Dolby Digital Plus");
    	} else {
    		dolby.setText(null);
    	}
    }
	
	private void setAudioTrack()
	{	
		if(audio_num_stream < 1) {
			curr_audio_stream_num = 0;	//Not OK indeed
			return ;
		}
		
		if(audio_num_stream >= 1 && curr_audio_stream_num == 0) {
			curr_audio_stream_num = 1;
			playbackControl.mp_setAudioTrackInfo(curr_audio_stream_num);
			return ;
		}
		
		if(audio_num_stream >= 1 && curr_audio_stream_num > audio_num_stream) 
 		{
			curr_audio_stream_num = 1;
 			playbackControl.mp_setAudioTrackInfo(curr_audio_stream_num);
 			return ;
 		}
		playbackControl.mp_setAudioTrackInfo(curr_audio_stream_num);
	}
	
	public void resumeInit(int index)	
	{	

		if(playbackControl.state == playbackControl.PREPARED ) {
			if(mVideoBookMark == null) 
				return ;
			Logger.v(tag, "Try to resume play");
			if(index >= 0 && index < mVideoBookMark.bookMarkLength())
			{	
				curr_audio_stream_num = mVideoBookMark.getAudioTrack(index);
				playbackControl.mp_setAudioTrackInfo(curr_audio_stream_num);
				curr_subtitle_stream_num = mVideoBookMark.getSubtitleTrack(index);
				curr_SPU_ENABLE = mVideoBookMark.isSubtitleOn(index);
				
				playbackControl.mp_setSubtitleInfo(curr_subtitle_stream_num, curr_SPU_ENABLE, curr_textEncoding, curr_textColor, curr_fontSize);
				
				byte[] inputArray = mVideoBookMark.getNavBuffer(index);

				if(inputArray != null) {
					String tmp = "";
					for(int i = 0; i < inputArray.length; i++) {
						tmp += inputArray[i];
					}
					Logger.v(tag, "inputArray " + tmp);
					
					playbackControl.mp_set_nav_state(NAVPROP_INPUT_SET_NAV_STATE, inputArray);
					Logger.v(tag, "ResumePlay done 2");
				}
				
				//clean and del file	
				mVideoBookMark.removeBookMark(index);
				mVideoBookMark.writeBookMark();

				Log.v(tag, "ResumePlay done 3");
			}
		}
		
	}
	
	private void doRequestAF(){	//try to obtain AUDIOFOCUS_GAIN, nowAudioFocus
		if(am == null) {
			am = (AudioManager) (this.getSystemService(Context.AUDIO_SERVICE));
		}
		
        int result = am.requestAudioFocus(audioFocusChangeListener,  
                AudioManager.STREAM_MUSIC, // Request permanent focus.  
                AudioManager.AUDIOFOCUS_GAIN);  
        
		if(result == AudioManager.AUDIOFOCUS_REQUEST_FAILED){
			uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_HINT_NOTGETAUDIOFOCUS);
			nowAudioFocus = AudioManager.AUDIOFOCUS_LOSS;
		} else {
			nowAudioFocus = AudioManager.AUDIOFOCUS_GAIN;
		}
	}
	
	private boolean checkAndRequestAF() {
		if(nowAudioFocus != AudioManager.AUDIOFOCUS_GAIN) {
			doRequestAF();
			if(nowAudioFocus != AudioManager.AUDIOFOCUS_GAIN) {
				return false;
			}
		}
		return true;
	}
	
	
	private void doAbandonAF(){
		if(nowAudioFocus != AudioManager.AUDIOFOCUS_LOSS) {
			if(am != null) {
				Log.v(tag, "abandon audio focus done!");
				am.abandonAudioFocus(audioFocusChangeListener);
				nowAudioFocus = AudioManager.AUDIOFOCUS_LOSS;
			}
		}
	}
	
	OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {  
        public void onAudioFocusChange(int focusChange) {  
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) { //Temp loss
            	Log.v(tag, "AUDIOFOCUS_LOSS_TRANSIENT");
            	nowAudioFocus = AudioManager.AUDIOFOCUS_LOSS;            	
    			f_pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            	Log.v(tag, "AUDIOFOCUS_GAIN");
            	nowAudioFocus = AudioManager.AUDIOFOCUS_GAIN;
            	if(!isActivityPause ) {
            		if(playbackControl.state == PlaybackControl.STOPPED) {
            			playbackControl.mp_prepare();	//because of AudioFocus_LOSS
            		} else {
            			f_play();
            		}
            	}
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {	//need do Later
            	//should we release resources,and do resume?
            	Log.v(tag, "AUDIOFOCUS_LOSS");
            	nowAudioFocus = AudioManager.AUDIOFOCUS_LOSS;
            	if(!isActivityPause) {
            		f_stop();
            	}
            }   
        }  
    };
	
	public void showShortToast(String showText) {
		if(toast == null) {
			toast = Toast.makeText(this, showText, Toast.LENGTH_SHORT);
		} else {
			toast.setText(showText);
		}
		toast.show();
	}
	
	public void showLongToast(String showText) {
		if(toast == null) {
			toast = Toast.makeText(this, showText, Toast.LENGTH_LONG);
		} else {
			toast.setText(showText);
		}
		toast.show();
	}
	
	public void initMediaPlayerValue() {
		isSeeking = false;
		ffIndex = fwIndex = sfIndex = swIndex = -1;
		isOnNotNormalPlay = false;
		curr_SPU_ENABLE = 0;
		curr_subtitle_stream_num = 0;
		subtitle_num_Stream = 0;
		curr_audio_stream_num = 0;
		audio_num_stream = 0;
	}
	
	@Override
    public synchronized void update(Observable o, Object arg) {
		Log.e("audiobrowser", "update");
		ObserverContent content = (ObserverContent)arg;
		String serverName = content.getMsg();
		String act = content.getAction();
		if(act.equals(ObserverContent.REMOVE_DEVICE)) {
			Log.e("DLNADevice", "videoPlayerActivity "+" removed server name: " + serverName);
			DMSName.add(serverName);	//no matter what , just add it to remove list, later do with it .
			if(map.getMediaServerName().equals(serverName))
			{
				//todo nothing
				//clear every thing shows above Activity
				if(quickmenu != null) {
					quickmenu.dismiss();
				}
				if(msg_hint != null) {
					msg_hint.dismiss();
				}
				if(long_msg != null) {
					long_msg.dismiss();
				}
				if(short_msg != null) {
					short_msg.dismiss();
				}
				
				if(!isActivityReady) {
					isRemoved = true;
					return ;
				} else {
					uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_DMS_CLOSE);
				}
			}

		}
	}
	
	public void getMediaPlayer() {
		if(mPlayer == null) {
			mPlayer = new MediaPlayer();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.e(tag, "onConfigurationChanged !1111111111111111111111111111111111111111111");
		//UI data reserve
		int timeBarMax = timeBar.getMax();
		String timeEndStr = (String) timeEnd.getText();
		//
		Drawable play_drawable = play.getDrawable();
		Drawable play_mode_drawable = play_mode.getDrawable();
		Drawable picture_size_drawable = pictureSize.getDrawable();
		Drawable playAndPause_drawable = playAndPause.getDrawable();
		//refresh views and ...
		setContentView(R.layout.video_player_dmp);
		findViews();
		setListeners();
		//recovery
		timeBar.setMax(timeBarMax);
		timeEnd.setText(timeEndStr);
		play.setImageDrawable(play_drawable);
		play_mode.setImageDrawable(play_mode_drawable);
		pictureSize.setImageDrawable(picture_size_drawable);
		playAndPause.setImageDrawable(playAndPause_drawable);
		showAndWaitToHideBanner();
		if(loading) {
			startLoading();
		}
		//rotateVideo();
	}
	
	public void findViews() {
		loadingIcon = (ProgressBar)findViewById(R.id.loadingIcon);
		loadingIcon.setVisibility(View.INVISIBLE);
		movBannerView = (View)findViewById(R.id.movie_banner);
		main_layout = (View)findViewById(R.id.main);
		play = (ImageView) movBannerView.findViewById(R.id.play);
		play_mode = (ImageButton) movBannerView.findViewById(R.id.play_mode);	
		pictureSize = (ImageButton) findViewById(R.id.play_picturesize);
		playAndPause = (ImageButton) movBannerView.findViewById(R.id.c_playandpause);
		slowForward = (ImageButton) movBannerView.findViewById(R.id.c_slowforward);
		slowBackward = (ImageButton) movBannerView.findViewById(R.id.c_slowbackward);
		fastForward = (ImageButton) movBannerView.findViewById(R.id.c_fastforward);
		fastBackward = (ImageButton) movBannerView.findViewById(R.id.c_fastbackward);
		skipNext = (ImageButton) movBannerView.findViewById(R.id.c_skipnext);
		skipBefore = (ImageButton) movBannerView.findViewById(R.id.c_skipbefore);
		timeNow = (TextView) findViewById(R.id.time_now);
		timeEnd = (TextView) findViewById(R.id.time_end);
		timeBar = (SeekBar) findViewById(R.id.time_bar);
		sView = (SurfaceView)findViewById(R.id.surfaceView);
		mainMenu = (ImageView) findViewById(R.id.main_menu);
		dolby = (TextView)findViewById(R.id.dolby);
		
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
		    FLING_MIN_DISTANCE = (float) (1/20.0 * map.getScreenWidth());
			FLING_MIN_VELOCITY = 0.0f;
					
		} else {
			
		    
		    FLING_MIN_DISTANCE = (float) (1/20.0 * map.getScreenHeight());
			FLING_MIN_VELOCITY = 0.0f;
	
		}
		
		banner_h = movBannerView.getLayoutParams().height;
		
	}
	
	public void setListeners() {
		sView.getHolder().addCallback(surfaceListener);
		
		uiHandler = new Handler() {
			@Override
			public void dispatchMessage(Message msg) {
				// TODO Auto-generated method stub
				switch(msg.what) {
					case HandlerControlerVariable.MSG_STARTANIMATION: {
						if(!isActivityPause) {
							startLoading();
						}
					} break;
					case HandlerControlerVariable.MSG_DISSMISSANIMATION: {
						if(!isActivityPause) {
							dismissLoading();
						}
					} break;
					case HandlerControlerVariable.MSG_RESET_SEEKFLAG : {
						isSeeking = false;
					} break;
					case HandlerControlerVariable.MSG_DMS_CLOSE: {
						isRemoved = false;
						Logger.e(TAG, "This DMS has been closed");
						setResult(ResultCodeFinishVideoBrowser);
						VideoPlayerActivity.this.finish();
						/*short_msg.confirm_title.setVisibility(View.INVISIBLE);
	    				short_msg.setMessage(getResources().getString(R.string.DMS_was_close));
	    				short_msg.setButtonText(getResources().getString(R.string.msg_yes));
	        			short_msg.left.setVisibility(View.INVISIBLE);
	        			short_msg.right.setVisibility(View.INVISIBLE);
	        			short_msg.confirm_bt.setVisibility(View.VISIBLE);
	        			short_msg.setKeyListener(true);
	        			short_msg.confirm_bt.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View arg0) {
								DMSName.clear();
								short_msg.dismiss();
								isAbleShown = true;
								short_msg = null;
								VideoPlayerActivity.this.finish();
							}		
						});
	        			isAbleShown = false;
	        			short_msg.show();*/
					} break;
					case HandlerControlerVariable.MSG_HIDE_MENU: {
						//cancel_task_updateSleepTime();
						if(quickmenu.isShowing()) {
							quickmenu.dismiss();
							isAbleShown = true; 
						}
					} break;
					case HandlerControlerVariable.QUICK_SHOW_BANNER:
						quickShowBanner();
						keepOnUpdateBarAndTime(); 
						break;
					case HandlerControlerVariable.DELAY_SHOW_BANNER:
						delayShowBanner();
						keepOnUpdateBarAndTime();
						break;
					case HandlerControlerVariable.QUICK_HIDE_BANNER:
						quickHideBanner();
						break;
					case HandlerControlerVariable.DELAY_HIDE_BANNER:
						delayHideBanner();
						break;
					case HandlerControlerVariable.UPDATETIMEBARANDTIMENOW:
						updateTimebarAndTimeNow();
						break;
					case HandlerControlerVariable.UPDATEENDTIME:
						updateTimeEnd();
						break;
					case HandlerControlerVariable.MSG_HINT_NOTGETAUDIOFOCUS:
						showShortToast("Can't get audioFocus");
						break;
					case HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_FOCUS:
						playAndPause.requestFocus();
						break;
					case HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPLAY:
						playAndPause.setImageDrawable(getResources().getDrawable(R.drawable.v_gui_play));
						break;
					case HandlerControlerVariable.MSG_SET_PLAYANDPAUSE_ICONTOPAUSE:
						playAndPause.setImageDrawable(getResources().getDrawable(R.drawable.v_gui_pause));
						break;
					case HandlerControlerVariable.MSG_SET_FASTFORWARD_FOCUS:
						fastForward.requestFocus();
						break;
					case HandlerControlerVariable.MSG_SET_FASTBACKWARD_FOCUS:
						fastBackward.requestFocus();
						break;
					case HandlerControlerVariable.MSG_SET_SLOWBACKWARD_FOCUS:
						slowBackward.requestFocus();
						break;
					case HandlerControlerVariable.MSG_SET_SLOWFORWARD_FOCUS:
						slowForward.requestFocus();
						break;
					case HandlerControlerVariable.MSG_SET_SKIPNEXT_FOCUS:
						skipNext.requestFocus();
						break;
					case HandlerControlerVariable.MSG_SET_SKIPPREVIOUS_FOCUS:
						skipBefore.requestFocus();
						break;
					case HandlerControlerVariable.MSG_SET_PLAYBACKSTATUS : {
						switch(msg.arg1) {
							case PlaybackStatus.STATUS_PLAY:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_play));
								break;
							case PlaybackStatus.STATUS_PAUSE:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_pause));
								break;
							case PlaybackStatus.STATUS_FF1:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_ff2));
								break;
							case PlaybackStatus.STATUS_FF2:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_ff8));
								break;
							case PlaybackStatus.STATUS_FF3:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_ff16));
								break;
							case PlaybackStatus.STATUS_FF4:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_ff32));
								break;
							case PlaybackStatus.STATUS_FW1:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_rew2));
								break;
							case PlaybackStatus.STATUS_FW2:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_rew8));
								break;
							case PlaybackStatus.STATUS_FW3:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_rew16));
								break;
							case PlaybackStatus.STATUS_FW4:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_rew32));
								break;
							case PlaybackStatus.STATUS_SW1:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_srew));
								break;
							case PlaybackStatus.STATUS_SW2:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_srew));
								break;
							case PlaybackStatus.STATUS_SW3:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_srew));
								break;
							case PlaybackStatus.STATUS_SW4:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_srew));
								break;
							case PlaybackStatus.STATUS_SF1:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_sff));
								break;
							case PlaybackStatus.STATUS_SF2:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_sff));
								break;
							case PlaybackStatus.STATUS_SF3:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_sff));
								break;
							case PlaybackStatus.STATUS_SF4:
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_sff));
								break;
							case PlaybackStatus.STATUS_SKIPNEXT :
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_skipnext));
								break;
							case PlaybackStatus.STATUS_SKIPBEFORE :
								play.setImageDrawable(getResources().getDrawable(R.drawable.status_skipbefore));
								break;
							case PlaybackStatus.UNKNOWN:
								play.setImageResource(R.color.blank);
								break;
						}
					} break;
					case HandlerControlerVariable.MSG_HINT_MEDIANOTAVAIL: {
						if(isActivityPause) {
							return ;
						}
						showLongToast("The Media is not avail now");
					} break;
					case HandlerControlerVariable.MSG_SET_REPEATMODE_ICON: {
						switch(msg.arg1) {
							case REPEAT_ALL:
								play_mode.setImageDrawable(getResources().getDrawable(R.drawable.v_gui_repeat_all));
								break;
							case REPEAT_SINGLE:
								play_mode.setImageDrawable(getResources().getDrawable(R.drawable.v_gui_repeat_one));
								break;
							case REPEAT_OFF:
								play_mode.setImageDrawable(getResources().getDrawable(R.drawable.v_gui_repeat_off));
								break;
						}
					} break;
					case HandlerControlerVariable.MSG_HINT_SHORTMESSAGE: {
						String strObj = (String)(msg.obj);
						showMessageHint(strObj);
					}
				}
				super.dispatchMessage(msg);
			}	
		};
		
		mGestureDetector = new GestureDetector(this) {
			@Override
			public boolean onTouchEvent(MotionEvent ev) {
				Log.v(tag, "GestureDetector onTouchEvent");
				// TODO Auto-generated method stub
				
				
				return super.onTouchEvent(ev);
				///add by star_he
			}
			
			
		};
		
		timeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				
				if(fromUser) {
					if((!isJPTS && isSupportSeek) || (isJPTS && isSupportJPSeek)){
						;
					}else {
						return;
					}
					if(checkAndRequestAF()) {
						Log.v(tag, "Seek from User happen!");
						if(!playbackControl.isPlaying() || isOnNotNormalPlay) {
							f_play();
						}
						playbackControl.mp_seekTo(progress);
						progress /= 1000;
						int minute = progress / 60;
						int hour = minute / 60;
						int second = progress % 60;
						minute %= 60;
						timeNow.setText(String.format("%02d:%02d:%02d", hour, minute, second));
					}	
				}
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				Log.v(tag, "on Start Track!");
				cancel_task_hide_controler();	// keep banner state
				if((!isJPTS && isSupportSeek) || (isJPTS && isSupportJPSeek)){
						;
				}else {
					showShortToast("Server is not support Seek");
					return ;
				}
				
				isSeeking = true;
				if(!checkIndexValid()) {
					isSeeking = false;
					return ;
				}
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				Log.v(tag, "on Stop Track!");
				showAndWaitToHideBanner();
				if((!isJPTS && isSupportSeek) || (isJPTS && isSupportJPSeek)){
					if(checkAndRequestAF() && seekBar.getMax() > 0) {
						int progress = seekBar.getProgress();
						playbackControl.mp_seekTo(progress);
						uiHandler.sendEmptyMessageDelayed(HandlerControlerVariable.MSG_RESET_SEEKFLAG, delay_500ms);
					}
				}
			}
		});
		
		
		//About banner Control
		main_layout.setOnClickListener(this);
		main_layout.setOnTouchListener(this);
		main_layout.setLongClickable(true);
		movBannerView.setOnClickListener(this);
		
		//About Playback Control
		playAndPause.setOnClickListener(this);
		slowForward.setOnClickListener(this);
		slowBackward.setOnClickListener(this);
		fastForward.setOnClickListener(this);
		fastBackward.setOnClickListener(this);
		skipNext.setOnClickListener(this);
		skipBefore.setOnClickListener(this);
		pictureSize.setOnClickListener(this);
		play_mode.setOnClickListener(this);
		
		mainMenu.setOnClickListener(this);
		//mainMenu.setOnTouchListener(this);
		/*
		getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
			final int navigationBarHeight = getNavigationHeight();
			float y = -1.0f;
			public void onSystemUiVisibilityChange(int visibility) {
				// TODO Auto-generated method stub
				Log.e(TAG, "Change !!!!!!!!!!!!!!!!!!!!!!");
				if(movBannerView == null) {
					return ;
				}
				int diff = mLastSystemUiVis ^ visibility;
				mLastSystemUiVis = visibility;
				
				if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
					&& (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
					showAndWaitToHideBanner();
				}
			}
		});
		*/
	}
	
	void recordResumePoint() {
		if(mVideoBookMark != null && currIndex >=0 && currIndex < fileArraySize) {
    		byte[] outputArray = null;
			byte[] inputArray = new byte[]{0,0,0,0};
			boolean execError = false;
			try {
				outputArray = playbackControl.mp_get_nav_state(NAVPROP_INPUT_GET_NAV_STATE, inputArray);
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
				execError = true;
			} catch(SecurityException e) {
				e.printStackTrace();
				execError = true;
			} catch (IllegalStateException e) {
				e.printStackTrace();
				execError = true;
			}
			
			if(outputArray != null && execError == false) {
				//end
				Logger.v(tag, "fileTitleArray.get(currIndex) " + fileTitleArray.get(currIndex));
				Logger.v(tag, "curr_subtitle_stream_num " + curr_subtitle_stream_num);
				Logger.v(tag, "SPU_ENABLE " + curr_SPU_ENABLE);
				Logger.v(tag, "curr_audio_stream_num " + curr_audio_stream_num);
				String tmp = "";
				for(int i = 0; i < outputArray.length; i++) {
					tmp += outputArray[i];
				}
				Log.v(tag, "outputArray " + tmp);
				mVideoBookMark.addBookMark(filePathArray.get(currIndex), fileTitleArray.get(currIndex), curr_subtitle_stream_num, curr_SPU_ENABLE, curr_audio_stream_num, outputArray);
				mVideoBookMark.writeBookMark();
				
				map.setStopedFileUrl(filePathArray.get(currIndex));
				VideoBrowser.index = filePos.get(currIndex).intValue();
			}
    	}
	}
	
	public void showMessageHintDelayHide(String text, int delay_ms) {
		Log.v(tag, "to show hint");
		//Toast.makeText(this, "nnnnn", 1000).show();
		msg_hint.setMessage(text);
		msg_hint.show();
		
		Log.v(tag, "show hint end");
		
		uiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				msg_hint.dismiss();
			}
		}, delay_ms);
		
	}
	
	public void showMessageHint(String text) {
		msg_hint.setMessage(text);
		msg_hint.show();
	}
	
	public int getOrientation() {
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			return ORIENTATION_LANDSCAPE;
		} else {
			return ORIENTATION_PORTRAIT;
		}
	}
	
	public int getRotate() {
		int rotateVal = display.getRotation();
		Logger.v(TAG_ROTATE, "rotate angle+++" + rotateVal);
		
		switch(rotateVal) {
			case Surface.ROTATION_0:
				return ROTATE_0;
			case Surface.ROTATION_90:
				return ROTATE_1;
			case Surface.ROTATION_180:
				return ROTATE_2;
			case Surface.ROTATION_270:
				return ROTATE_3;
			default:
				return 0;
		}
	}
	
	boolean checkIndexValid() {
		if(currIndex == -1 || currIndex >= filePathArray.size()) {
			uiHandler.sendEmptyMessage(HandlerControlerVariable.MSG_HINT_MEDIANOTAVAIL);
			return false;
		}
		return true;
	}
	
	void killTimerAndTask() {           
		if(task_hide_controler != null) {
			task_hide_controler.cancel();
            task_hide_controler = null;
		}
		if(task_getCurrentPositon != null) {
			task_getCurrentPositon.cancel();
			task_getCurrentPositon = null;
		}
		if(timer != null) {
			timer.cancel();
            timer = null;
		}
	}
	
	public void startLoading() {
		loadingIcon.setVisibility(View.VISIBLE);
		loading = true;
	}

	public void dismissLoading() {
		loadingIcon.setVisibility(View.INVISIBLE);
		loading = false;
	}
	
	public static String hexString2binaryString(String hexString){  
		if (hexString == null || hexString.length() % 2 != 0)  
			return null;  
		String bString = "", tmp;  
		for (int i = 0; i < hexString.length(); i++){  
			tmp = "0000" + Integer.toBinaryString(Integer.parseInt(hexString.substring(i, i + 1), 16)); 
			bString += tmp.substring(tmp.length() - 4);  
		}  
		return bString;  
	}  
	
	public boolean checkProtolInfo(String protolInfo) {
		if(protolInfo != null) {
			if(protolInfo.contains("DLNA.ORG_PN")) {
				return checkNext("DLNA.ORG_PN", protolInfo);
			}
			
			if(protolInfo.contains("ARIB.OR.JP_PN")) {
				return checkNext("ARIB.OR.JP_PN", protolInfo);
			}
			
			if(protolInfo.contains("TOSHIBA.CO.JP_PN")) {
				return checkNext("TOSHIBA.CO.JP_PN", protolInfo);
			}
			
			if(protolInfo.contains("DIGION.COM_PN")) {
				return checkNext("DIGION.COM_PN", protolInfo);
			}	
			return false;
		}
		return false;
	}
	
	boolean checkNext(String prefix, String protolInfo) {
		int len = prefix.length();
		String fromPrefix = protolInfo.substring(protolInfo.indexOf(prefix));
		String noPrefix = fromPrefix.substring(len, fromPrefix.length());
		Log.v(TAG, fromPrefix);
		Log.v(TAG, "noPrefix=" + noPrefix);
		if(prefix.equals("DLNA.ORG_PN")) {
			if(noPrefix.contains("MPEG_TS_JP_T")){
				isJPTS = true;
			}else {
				isJPTS = false;
			}
			if(noPrefix.contains("AVC_MP4_BL_CIF15_AAC_520")
					|| noPrefix.contains("MPEG_PS_PAL") || noPrefix.contains("MPEG_PS_NTSC") 
					|| noPrefix.contains("MPEG_TS_JP_T") ||noPrefix.contains("WMVMED_BASE")
					|| noPrefix.contains("WMVMED_FULL") || noPrefix.contains("WMVHIGH_FULL")) {
				return true;
			} 
			if(noPrefix.contains("AVC_TS_JP_AAC_T") 
					|| noPrefix.contains("AVC_TS_JP_AAC_T") || noPrefix.contains("AVC_TS_HD_60_AC3_T") 
					|| noPrefix.contains("AVC_TS_HD_24_AC3_T") || noPrefix.contains("MPEG_TS_SD_JP_MPEG1_L2_T")
					|| noPrefix.contains("AVC_MP4_MP_HD_1080i_AAC") || noPrefix.contains("AVC_MP4_BL_L32_HD_AAC")
					|| noPrefix.contains("AVC_MP4_BL_L31_HD_AAC") || noPrefix.contains("AVC_MP4_BL_L3L_SD_AAC")
					|| noPrefix.contains("AVC_MP4_BL_CIF30_AAC_940") || noPrefix.contains("DTCP_MPEG_TS_JP_T")
					|| noPrefix.contains("DTCP_AVC_TS_JP_AAC_T") || noPrefix.contains("DTCP_MPEG_TS_SD_JP_MPEG1_L2_T")
					|| noPrefix.contains("DTCP_AVC_TS_HD_60_AC3_T")) {
				return true;
			}
		}
		if(prefix.equals("ARIB.OR.JP_PN")) {
			if(noPrefix.startsWith("=MPEG_TTS_CP")) {
				return true;
			}
		}
		
		if(prefix.equals("TOSHIBA.CO.JP_PN")) {
			if(noPrefix.contains("AVC_TS_DC_AAC_T") 
					|| noPrefix.contains("AVC_TS_SQ_AAC_T") || noPrefix.contains("DTCP_AVC_TS_DC_AAC_T")
					|| noPrefix.contains("DTCP_AVC_TS_SQ_AAC_T")) {
				return true;
			}
		}
		
		if(prefix.equals("DIGION.COM_PN")) {
			if(noPrefix.contains("AVC_3GP_HP_L4_AAC") 
					|| noPrefix.contains("AVC_3GP_HP_L4") || noPrefix.contains("MPEG4_P2_3GP_SP_L6_AAC")
					|| noPrefix.contains("AVC_3GP_BP_L4_AAC") || noPrefix.contains("AVC_3GP_BP_L4")) {
				return true;
			}
		}
		return false;
	}
	
	void setNavVisibility(boolean visible) {
        int newVis = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!visible) {
            newVis |= View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        getWindow().getDecorView().requestFocus();
        // Set the new desired visibility.
        this.getWindow().getDecorView().setSystemUiVisibility(newVis);
    }
	
	private int getStatusBarHeight() {
		Class<?> c = null;
		Object obj = null;
		Field field = null;
		int x = 0, sbar = 0;
		try {
		    c = Class.forName("com.android.internal.R$dimen");
		    obj = c.newInstance();
		    field = c.getField("status_bar_height");
		    x = Integer.parseInt(field.get(obj).toString());
		    sbar = getResources().getDimensionPixelSize(x);
		    return sbar;
		} catch (Exception e) {
		    e.printStackTrace();
		} 
		return 0;
	}
	
	private int getNavigationHeight() {
		Class<?> c = null;
		Object obj = null;
		Field field = null;
		int x = 0, sbar = 0;
		try {
		    c = Class.forName("com.android.internal.R$dimen");
		    obj = c.newInstance();
		    field = c.getField("navigation_bar_height");
		    x = Integer.parseInt(field.get(obj).toString());
		    sbar = getResources().getDimensionPixelSize(x);
		    return sbar;
		} catch (Exception e) {
		    e.printStackTrace();
		} 
		return 0;
	}
	
	private void disableSeekBar() {
		timeBar.setClickable(false);
		timeBar.setEnabled(false);
		timeBar.setFocusable(false);
		timeBar.setSelected(false);
	}
	
	private void enableSeekBar() {
		timeBar.setClickable(true);
		timeBar.setEnabled(true);
		timeBar.setFocusable(true);
		timeBar.setSelected(true);
	}
}
