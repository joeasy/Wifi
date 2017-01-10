package com.rtk.dmp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5.DeviceStatusListener;

import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.DLNAFileInfoComparator;
import com.realtek.Utils.MimeTypes;
import com.realtek.Utils.AsyncImageLoader.ImageInfoCallback;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.realtek.Utils.widget.VerticalBar;
import com.rtk.dmp.AudioBrowser.QuickMenuAdapter;
import com.rtk.dmp.AudioBrowser.FileListAdapter.ViewHolder;
import com.rtk.dmp.GridViewFragment.UiListener;

import android.app.TvManager;
import android.app.AlertDialog.Builder;


public class VideoBrowser extends Activity implements Observer{
	private String TAG = "VideoBrowser";
	private String tag = "Test_VideoBrowser";
	
	private Drawable loadingDrawable = null;
	//Views
	private ImageView sortDownBG;
	private ImageView sortDownIcon;
	private ImageView sortUpBG;
	private ImageView sortUpIcon;
	
	private GridView videolist;
	private TextView topPath;
	private Animation ad = null;
	private View sortUp = null;
	private View sortDown = null;
	private ImageView mainMenu = null;
	private M_QuickMenu quickmenu = null;
	private QuickMenuAdapter quickmenuAdapter = null;
	ImageView loadingIcon = null;
	Drawable videoImgs[] = null;
	private ConfirmMessage short_msg = null;
	private PopupMessage msg_hint = null;
	//Global vals and resources
	private MediaApplication mediaApp = null;
	private Resources resourceMgr = null;
	private ContentResolver m_ContentMgr = null;
	private MimeTypes mMimeTypes = null;
	private TvManager mTv = null;
	//MSG
	private final int PROGRESS_CHANGED = 1;
	private final int UPDATE_UI_FACE = 2;
	private final int MSG_START_LOADING = 3;
	private final int MSG_STOP_LOADING = 4;
	private final int UPDATE_TIME = 5;
	private final int UPDATE_UI_BROKEN = 6;
	//private final int MSG_LOADING = 7;
	private final int MSG_PLAYING = 8;
	private final int MSG_SET_REPEAT = 11;
	private final int MSG_QUICK_HIDE = 12;
	private final int MSG_QUICKMSG_HIDE = 13;
	private final int MSG_DEVICE_REMOVED = 15;
	private final int MSG_BACK_REFRESH = 16;
	private final int MSG_NORMAL_REFRESH = 17;
	private final int MSG_USUALLY_REFRESH = 101;
	private final int MSG_REFRESH = 18;
	private final int MSG_FIRST_REFRESH = 19;
	private final int MSG_GO_BACK = 14;
	private final int MSG_HIDE_HINT = 20;
	private final int MSG_DEVICED_ADDORREMOVED = 21;
	private final int MSG_UNFOCUS = 22;
	private final int MSG_HIDE_POPUP = 23;
	private final int MSG_HIDE_SHORT = 24;
	private final int MSG_REFRESH_TIMER = 25;
	
	private final int MSG_BROWSERLISTLOADING = 26;
	private final int MSG_BROWSERLISTDISMISSLOADING = 27;
	//Constant
	private final int ORIENTATION_LANDSCAPE = 0;
	private final int ORIENTATION_PORTRAIT = 1;

	private final int ResultCodeFinishVideoBrowser = 9;
	private final int ResultCodeRestartVideoBrowser = 10;
	private final int ResultCodeBackToVideoBrowser = 11;
	
	private long quick_timeout = 6000;
	
	private boolean isFocused = false;
	private int lastnum = 0;
	//for record resumPoint
	
	public static int index = -1;
	
	public static int resumePointLevel = -1;
	
	private int dirLevel = -1;
	private String rootPath = "";
	private String devicePath = "";
	public ArrayList<String> parentPath = null;
	private ArrayList<String> folderPath = null;
	private Path_Info curPathInfo = null;
	private Stack<ArrayList<DLNAFileInfo>> listStack = null;
	private final int maxStack = 10;
	FileListAdapter simpleAdapter = null;
	private boolean loading = false;
	ArrayList<DLNAFileInfo> listItems = null;
	ArrayList<DLNAFileInfo> scanDataItems = null;
	private List<String> mCharIDList = null;
	
	private TimerTask task_getduration = null;
	private TimerTask task_message_time_out = null;
	private Timer timer = null;

	private Handler handler = null;

	private Activity mContext = this;
	private BookMark mVideoBookMark = null;
	
	// add by jessie
	public ArrayList<String> DMSName = null;
	volatile boolean isKeyBack = false;
	boolean acceptKeyBack = true;
	private boolean isSelectPos = true;
	
	/********* Video Recode **********************/
	private int totalLen;
	private int pagesize;
	private int cellHeight;
	private int cols;
	
	private int sortNameMode = 0;// 0 default, 1 up, -1 down
	private int sortDateMode = 0; // 0 default, 1 up, -1 down
	
	/******** Page setting *********************/

	public static boolean changeIndex = false;

	/********** Confirm Message **********************/
	int resumeIndex = -1;
	

	// Very Important Flag
	boolean deviceChanged = false; // NEED TO GIVE saveInstance
	
	private int mSleepTimeHour = 0, mSleepTimeMin = 0;
	
	private int mActivityPauseFlag = 0;
	
	// val used to init menu, popupMessage and so on, which has relationship with screen density or screen resolution
	int popupWindowWith = -1;
	int popupWindowHeight = -1;
	int menuWidth = -1;
	int menuHeight = -1;
	
	private void requestMeasureInfo() {
		int screenWidth = mediaApp.getScreenWidth();
		int screenHeight = mediaApp.getScreenHeight();
		if((screenWidth <= 1280 && screenHeight <= 720) 
				|| (screenWidth <= 720 && screenHeight <= 1280)) {
			//720p
			popupWindowWith = 447;
			popupWindowHeight = 150;
			menuWidth = 320;
			menuHeight = -1;
		} else if((screenWidth <= 1920 && screenHeight <= 1280) 
				|| (screenWidth <= 1280 && screenHeight <= 1920)) {
			//1080p
			popupWindowWith = 678;
			popupWindowHeight = 226;
			menuWidth = 480;
			menuHeight = -1;
		} else {
			//4k
			popupWindowWith = -1;
			popupWindowHeight = -1;
			menuWidth = -1;
			menuHeight = -1;
		}
	}
	
	class QuickMenuAdapter extends BaseAdapter {
		private LayoutInflater layoutInflater = null;
		int[] menu_name = new int[] {
				R.string.quick_menu_help,
				R.string.quick_menu_exit
		};
		int[] visibility = new int[]{
	 			View.INVISIBLE,
	 			View.INVISIBLE,	
	 	};
		
		class ViewHolder {
			TextView menu_name;
			ImageView left;
			TextView menu_option;
			ImageView right;
		}
		
		public QuickMenuAdapter(Context mContext) {
			// TODO Auto-generated constructor stub
			layoutInflater = LayoutInflater.from(mContext);
		}
		
		public void setVisibility(int position,int isVisible)
		{
			visibility[position]= isVisible;				
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return menu_name.length - 1;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder viewHolder = null;
			if(convertView == null) {
				convertView = layoutInflater.inflate(R.layout.video_browser_menu_list_row, null);
				viewHolder = new ViewHolder();
				viewHolder.menu_name = (TextView)convertView.findViewById(R.id.menu_name);
//				Typeface type= Typeface.createFromFile("/system/fonts/FAUNSGLOBAL3_F_r2.TTF");
//				viewHolder.menu_name.setTypeface(type);
	        	viewHolder.menu_option = (TextView)convertView.findViewById(R.id.menu_option);
	        	convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)convertView.getTag();
			}
			
			viewHolder.menu_name.setText(menu_name[position]);
			
			switch(position)
            {
	            case 0:
	            {
	            	viewHolder.menu_option.setText("");
	            	break;
	            }
	            case 1:
	            {
	            	viewHolder.menu_option.setText("");
	            	break;
	            }
	            default:
	            	break;
            }
			
			return convertView;
		}
		
	}
	
	private OnClickListener bt_click = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()) {
				case R.id.lay_up: {
					sortUpBG.setImageResource(R.drawable.dnla_sorting_base_up);
					sortUpIcon.setImageResource(R.drawable.dnla_sorting_icon_up);
					sortDownBG.setImageResource(R.drawable.dnla_sorting_base_down);
					sortDownIcon.setImageResource(R.drawable.dnla_sorting_icon_down);
					sortNameMode = 0;
					switch(sortDateMode) {
						case 0:
							sortDateMode = -1;
							break;
						case -1:
							sortDateMode = 1;
							break;
						case 1:
							sortDateMode = -1;
							break;
					}
					sort();
				} break;
				case R.id.lay_down: {
					sortUpBG.setImageResource(R.drawable.dnla_sorting_base_down);
					sortUpIcon.setImageResource(R.drawable.dnla_sorting_icon_down);
					sortDownBG.setImageResource(R.drawable.dnla_sorting_base_up);
					sortDownIcon.setImageResource(R.drawable.dnla_sorting_icon_up);
					sortDateMode = 0;
					switch(sortNameMode) {
						case 0:
							sortNameMode = -1;
							break;
						case -1:
							sortNameMode = 1;
							break;
						case 1:
							sortNameMode = -1;
							break;
					}
					sort();
				} break;
				case R.id.btn_menu: {
					onMenu();
				} break;
				default :
					break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "VideoBrowser onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.video_browser);
		loadingDrawable = (Drawable) this.getResources().getDrawable(R.drawable.dnla_loading_icon);
		mediaApp = (MediaApplication) getApplication();
		requestMeasureInfo();
		resourceMgr = getResources();
		m_ContentMgr = getApplicationContext().getContentResolver();
		
		if(quickmenuAdapter == null) {
			quickmenuAdapter = new QuickMenuAdapter(this);
		}
		short_msg = new ConfirmMessage(mContext, popupWindowWith, popupWindowHeight);
		msg_hint = new PopupMessage(mContext, popupWindowWith, popupWindowHeight);
		findViews();
		setListeners();
		initAccordingConfiguration();
		initWhenOnCreate();
		
		isKeyBack = false;
		acceptKeyBack = true;
		
		listStack = new Stack<ArrayList<DLNAFileInfo>>();
		dirLevel = -1;
		parentPath = new ArrayList<String>();
		curPathInfo = new Path_Info();
		folderPath = new ArrayList<String>();
		mCharIDList = new ArrayList<String>();
		DMSName = new ArrayList<String>();
		
		//to observe device change
		mediaApp.addObserver(this);
		
		//show in List
		parentPath.clear();
		folderPath.clear();
		parentPath.add(rootPath);
		folderPath.add("");
		dirLevel = 0;
		curPathInfo.addLevelInfo(rootPath);
		showRootList();
		
	}

	private void showRootList() {
		if (simpleAdapter != null)
			simpleAdapter.clearSelected();
		firstShowInList();	//clean show
		isKeyBack = false;
		new Thread(new Runnable() {
			public void run() {
				String tmpPath = rootPath;
				Log.e(TAG, "tmpPath = "+tmpPath);
				if(tmpPath != null && !tmpPath.equals("")){
					getFileList(rootPath, null);
					if (!isKeyBack && tmpPath.equals(parentPath.get(dirLevel))) {
						if(scanDataItems != null)
							dumpDateFromDlna();
						handler.sendEmptyMessage(MSG_NORMAL_REFRESH);
					}
				}
			}
		}).start();
	}
	
	private void dumpDateFromDlna(){
		if(listItems== null)
		{
			listItems = mediaApp.getFileList();
		}else {
			listItems.clear();
		}
		
		for(DLNAFileInfo info : scanDataItems){
			listItems.add(info);
		}
	}
	
	private void firstShowInList() {
		if (listItems == null) {
			listItems = mediaApp.getFileList();
		}
		listItems.clear();

		if (simpleAdapter == null) {
			simpleAdapter = new FileListAdapter(this, listItems, videolist);
			videolist.setAdapter(simpleAdapter);
		} else {
			simpleAdapter.notifyDataSetChanged();
		}

		if (dirLevel != -1 && folderPath.size() != 0)
			topPath.setText(devicePath + folderPath.get(dirLevel));
	}

	private void showInList() {
		totalLen = listItems.size();
		
		if (totalLen == 0) {
			if(mActivityPauseFlag == 1) {	// Activity already pause
				return ;
			}
			showNofile();
			return;
		}	
		
		if (totalLen >= MediaApplication.MAXFILENUM) {
			msg_hint.setMessage(resourceMgr.getString(R.string.maxfilenum));
			msg_hint.show();
		}

		if (simpleAdapter == null) {
			simpleAdapter = new FileListAdapter(this, listItems, videolist);
			videolist.setAdapter(simpleAdapter);
		} else
			simpleAdapter.notifyDataSetChanged();

		topPath.setText(devicePath + folderPath.get(dirLevel));
	}

	private void getFileList(String path, String uniqueCharID) {
		handler.sendEmptyMessage(MSG_START_LOADING);
		boolean upnpBrowserSuccess = false;
		int iDirSize = 0, iItemSize = 0;
		if(path == null || path.length() == 0) {
			handler.sendEmptyMessage(MSG_STOP_LOADING);
			return;
		}
		if (path.equals(rootPath)) {
			upnpBrowserSuccess = DLNADataProvider.browseServer(mediaApp
					.getMediaServerUUID());
			//mCharIDList.add(mediaApp.getMediaServerName());
			mCharIDList.add(mediaApp.getMediaServerUUID());
		} else {
			if(uniqueCharID != null)
				upnpBrowserSuccess = DLNADataProvider.browserDirectory(uniqueCharID);
		} 

		if (upnpBrowserSuccess) {
			iDirSize = DLNADataProvider.getDirectorySize();
			iItemSize = DLNADataProvider.getItemSize();
		} else {
			iDirSize = 0;
			iItemSize = 0;
			handler.sendEmptyMessage(MSG_STOP_LOADING);
			return;
		}
		Message msg = handler.obtainMessage();
		msg.what = MSG_BROWSERLISTLOADING;
		msg.arg1 = iDirSize + iItemSize;
		handler.sendMessage(msg);
		
		CreateTotalFileList(iDirSize, iItemSize);
		handler.sendEmptyMessage(MSG_STOP_LOADING);
		handler.sendEmptyMessage(MSG_BROWSERLISTDISMISSLOADING);
		return ;
	}
	
	public void CreateTotalFileList(int iDirSize, int iItemSize) {
		Log.v(tag, "CreateTotalFileList start");
		Log.v(tag, "iDirSize++" + iDirSize + "iItemSize++" + iItemSize);
		if (scanDataItems == null) {
			scanDataItems = new ArrayList<DLNAFileInfo>();
		}
		scanDataItems.clear();

		if (iDirSize > 0) {
			for (int i = 0; i < iDirSize; i++) {
				DLNAFileInfo finfo = new DLNAFileInfo(i,
						DLNADataProvider.getDirectoryTitle(i),
						DLNADataProvider.getDirectoryCharID(i),
						FileFilterType.DEVICE_FILE_DIR,
						parentPath.get(dirLevel)+DLNADataProvider.getDirectoryTitle(i) + "/",
						DLNADataProvider.queryDataByFile(
								DLNADataProvider.getDirectoryTitle(i),
								DLNADataProvider.UPNP_DMP_RES_DATE, null), null, null);
				scanDataItems.add(finfo);
			}
		}

		if (iItemSize > 0) {
			for (int i = 0; i < iItemSize; i++) {
				String FileName = DLNADataProvider.getItemTitle(i);
				if (FileName.length() == 0
						|| DLNADataProvider.GetMediaType(FileName) == 0) {
					return;
				}
				
				if(DLNADataProvider.GetMediaType(FileName) == FileFilterType.DEVICE_FILE_VIDEO) {
					String itemCharID = DLNADataProvider.getItemCharID(i);
					DLNAFileInfo finfo = new DLNAFileInfo(i, FileName, itemCharID,
							FileFilterType.DEVICE_FILE_VIDEO,
							DLNADataProvider.getItemUrl(itemCharID),
							DLNADataProvider.queryDataByFile(FileName,
									DLNADataProvider.UPNP_DMP_RES_DATE, null),
							null,
							null);
					scanDataItems.add(finfo);
				}
			}
		}
		Log.v(tag, "CreateTotalFileList end");
	}
	
	private void sort() {
		if(sortDateMode != 0) {
			Collections.sort(listItems, new DLNAFileInfoComparatorByDate(sortDateMode));
		}
		if(sortNameMode != 0) {
			Collections.sort(listItems, new DLNAFileInfoComparatorByName(sortNameMode));
		}
		simpleAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		Log.v(TAG, "VideoBrowser onPause");
		mActivityPauseFlag = 1;
		
		
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.v(TAG, "VideoBrowser onStop");
		super.onStop();
	}

	@Override
	public void onStart() {
		Log.v(TAG, "VideoBrowser onStart");
		super.onStart();
	}

	@Override
	public void onResume() {
		Log.v(TAG, "VideoBrowser onResume");
		//mActivityPauseFlag = 0;
		
		String path = getFilesDir().getPath();
		String fileName = path.concat("/VideoBookMark.bin");
		mVideoBookMark = mediaApp.getBookMark(fileName); 
		
		if(mVideoBookMark.bookMarkList != null && mVideoBookMark.bookMarkList.size() > 0) {
			mediaApp.setStopedFileUrl(mVideoBookMark.bookMarkList.get(0).fileUrl);
		} 
		
		mActivityPauseFlag = 0;
		
		if (timer == null)
			timer = new Timer(true);
		
		super.onResume();
	}

	@Override
	public void onRestart() {
		Log.v(TAG, "VideoBrowser onRestart");
		super.onRestart();
		simpleAdapter.notifyDataSetChanged();
		/*
		 * int pos = videolist.getSelectedItemPosition(); if
		 * (mDataProvider.GetFileTypeAt(pos) ==
		 * FileFilterType.DEVICE_FILE_VIDEO) { if (timer == null) timer = new
		 * Timer(true);
		 * 
		 * resumeIndex =
		 * mVideoBookMark.findBookMark(mDataProvider.GetTitleAt(pos));
		 * 
		 * if (resumeIndex >= 0)
		 * PopupMessageShow(mContext.getResources().getString
		 * (R.string.resumePlay_hint), TimerDelay.delay_6s); }
		 */
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "VideoBrowser onDestroy");

		super.onDestroy();
		loadingDrawable = null;
		if (curPathInfo != null)
			curPathInfo.cleanLevelInfo();
		
		mediaApp.deleteObserver(this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(tag, "onActivityResult happen");

		if (requestCode == 0) {
			if (resultCode == ResultCodeFinishVideoBrowser) {
				this.finish();
			} else if (resultCode == ResultCodeRestartVideoBrowser) {
				parentPath.clear();
				folderPath.clear();
				curPathInfo.cleanLevelInfo();
				dirLevel = -1;
				devicePath = "";
				simpleAdapter = null;
				firstShowInList();
				isKeyBack = false;
				new Thread(new Runnable() {
					public void run() {
						String tmpPath = rootPath;
						getFileList(rootPath, null);
						if (!isKeyBack
								&& tmpPath.equals(parentPath.get(dirLevel))) {
							if(scanDataItems!=null)
								dumpDateFromDlna();
							handler.sendEmptyMessage(MSG_FIRST_REFRESH);
						}
					}
				}).start();
			} else if(resultCode == ResultCodeBackToVideoBrowser) {
				
			} 
			
			//default
			Log.v(tag, "onActivityResult excute default code");
			videolist.setSelection(index);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.e(TAG, "keyCode = " + keyCode);
		switch (keyCode) {
			//add for Test
		
			case KeyEvent.KEYCODE_0 : {

			} break;
			//add for Test end
		
			case KeyEvent.KEYCODE_BACK: {
				if(parentPath.get(dirLevel).equals(rootPath)) {	//check parentPath is it rootPath
					Log.v(tag, "when press back, return to device list!");
					acceptKeyBack = false;
					DLNA_DMP_1p5.DLNA_DMP_1p5_stopWatingResponse();
					return super.onKeyDown(keyCode, event);
				}
				if(acceptKeyBack) {
					isKeyBack = true;
					if(loading){
						Log.v(tag, "process back while loading");
						//processOnLoading();
						DLNA_DMP_1p5.DLNA_DMP_1p5_stopWatingResponse();
						processBackLevel();
						return true;
					}else{
						Log.v(tag, "now processBackLevele");
						processBackLevel();
						return true;
					}
				}
			}
			case 232: // for L4300 KeyEvent.KEYCODE_PLAY
			{
				int position = videolist.getSelectedItemPosition();
				if (listItems.get(position).getFileType() == FileFilterType.DEVICE_FILE_VIDEO)
					playVideo(position);
			}
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void processBackLevel() {
		Log.v(tag, "processBackLevel()");
		if (curPathInfo.dirLevel > 0
				&& !parentPath.get(dirLevel).equals(rootPath)) {
			parentPath.remove(dirLevel);
			folderPath.remove(dirLevel);
			dirLevel--;
			if (dirLevel == 0)
				devicePath = "";
			curPathInfo.backToLastLevel();
			final int currentPos = curPathInfo.getLastLevelFocus();
			if(mCharIDList.size() > 0)
				mCharIDList.remove(mCharIDList.size()-1);
			if (dirLevel < maxStack) {
				Log.v(tag, "processBackLevel() from stack!");
				resetPlaylist();
				Message msg = new Message();
				msg.what = MSG_BACK_REFRESH;
				Bundle bundle = new Bundle();
				bundle.putBoolean("isFocused", isFocused);
				bundle.putBoolean("reset", true);
				bundle.putInt("currentPos", currentPos);
				msg.setData(bundle);
				handler.sendMessage(msg);
			} else {
				Log.v(tag, "processBackLevel() from getFrom FileList!");
				firstShowInList();
				isKeyBack = false;
				new Thread(new Runnable() {
					public void run() {
						if(mCharIDList.size() > 0) {
							String tmpPath = parentPath.get(dirLevel);
							getFileList(parentPath.get(dirLevel), mCharIDList.get(mCharIDList.size()-1));
							if (!isKeyBack
									&& tmpPath.equals(parentPath.get(dirLevel))) {
								if(scanDataItems!=null)
									dumpDateFromDlna();
								Message msg = new Message();
								msg.what = MSG_BACK_REFRESH;
								Bundle bundle = new Bundle();
								bundle.putBoolean("isFocused", isFocused);
								bundle.putBoolean("reset", false);
								bundle.putInt("currentPos", currentPos);
								msg.setData(bundle);
								handler.sendMessage(msg);
							}
						}
					}
				}).start();
			}
	   }
	}
	
	private void setPlaylist(){
		//ArrayList<FileInfo> playlistItems = new  ArrayList<FileInfo>(listItems);
		//mediaApp.setPlayListItems(playlistItems);
	}
	
	private void storePlaylist(){
		if(dirLevel>= maxStack)
			return;
		ArrayList<DLNAFileInfo> playlistItems = new  ArrayList<DLNAFileInfo>(listItems);
		listStack.push(playlistItems);
		Log.e("push", "push");
	}
	
	private void resetPlaylist(){
		Log.e("pop", "pop");
		ArrayList<DLNAFileInfo> playlistItems = listStack.pop();
		listItems.clear();
		for(DLNAFileInfo info : playlistItems)
			listItems.add(info);
	}
	
	private void processOnLoading() {
		AlertDialog.Builder builder = new Builder(VideoBrowser.this);
		builder.setMessage(getResources().getString(R.string.note_back));
		builder.setTitle(getResources().getString(R.string.note_title));
		builder.setPositiveButton(R.string.note_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						DLNA_DMP_1p5.DLNA_DMP_1p5_stopWatingResponse();
						processBackLevel();
					}
				});
		builder.setNegativeButton(R.string.note_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}
	
	private OnItemSelectedListener itemSelectListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View v, int pos,
				long id) {
			// TODO Auto-generated method stub
			Log.v(tag, "item Selected ++++++++" + pos);
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
			Log.v(tag, "nothing Selected ++++++++++++++++++!!!!!!!!!!!!");
		}
	};
	
	private OnItemClickListener itemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v,
				final int position, long id) {
			if(simpleAdapter == null) {
				return ;
			}
			if(simpleAdapter.fakeCnt) {
				return ;
			}
			int pos = position;
			index = pos;
			String tmpPath = parentPath.get(dirLevel);
			Log.v(tag, "OnItemClick++++tmpPath++++" + tmpPath);
			boolean isRemoved = false;
			
			if (DMSName.size() != 0) {
				for (int i = 0; i < DMSName.size(); i++) {
					isRemoved = tmpPath.contains(DMSName.get(i));
					if (isRemoved) {
						break;
					}
				}
				if (isRemoved) {
					VideoBrowser.this.finish();
					/*short_msg.confirm_title.setVisibility(View.INVISIBLE);
					short_msg.setMessage(resourceMgr.getString(R.string.DMS_was_close));
					short_msg.setButtonText(resourceMgr.getString(R.string.msg_yes));
					short_msg.left.setVisibility(View.VISIBLE);
					short_msg.right.setVisibility(View.VISIBLE);
					short_msg.confirm_bt.setVisibility(View.VISIBLE);
					short_msg.setKeyListener(true);
					short_msg.left.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							// TODO Auto-generated method stub
							if(short_msg.confirm_text.getText().equals(resourceMgr.getString(R.string.msg_yes))) {
								short_msg.setButtonText(resourceMgr.getString(R.string.msg_no));
							} else {
								short_msg.setButtonText(resourceMgr.getString(R.string.msg_yes));
							}
						}
						
					});
					short_msg.right.setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
							if(short_msg.confirm_text.getText().equals(resourceMgr.getString(R.string.msg_yes))) {
								short_msg.setButtonText(resourceMgr.getString(R.string.msg_no));
							} else {
								short_msg.setButtonText(resourceMgr.getString(R.string.msg_yes));
							}
						}
					});
					short_msg.confirm_bt.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View arg0) {
							if(short_msg.confirm_text.getText().equals(resourceMgr.getString(R.string.msg_yes))) {
								VideoBrowser.this.finish();
							} else {
								short_msg.dismiss();
							}
						}
	
					});

					short_msg.setOnDismissListener(new OnDismissListener() {

						@Override
						public void onDismiss() {
							
//							simpleAdapter.clearSelected();
//							parentPath.clear();
//							folderPath.clear();
//							parentPath.add(rootPath);
//							folderPath.add("");
//							dirLevel = 0;
//							devicePath = "";
//							curPathInfo.backToDeviceLevel();
//							DMSName.clear();
//							firstShowInList();
//							isKeyBack = false;
//							new Thread(new Runnable() {
//								public void run() {
//									String tmpPath = rootPath;
//									getFileList(rootPath, null);
//									if (!isKeyBack
//											&& tmpPath.equals(parentPath
//													.get(dirLevel))) {
//										if(scanDataItems!=null)
//											dumpDateFromDlna();
//										handler.sendEmptyMessage(MSG_NORMAL_REFRESH);
//									}
//								}
//							}).start();
						}
					});
					short_msg.show();*/
					return;
				}
			}
			
			if (listItems.get(pos).getFileType() == FileFilterType.DEVICE_FILE_VIDEO) {
				String serverName = null;
				serverName = mediaApp.getMediaServerName();
				Log.v("serverName is$$$$$$$$$", serverName);
				
				playVideo(pos);
				
			} else if (listItems.get(pos).getFileType() == FileFilterType.DEVICE_FILE_DIR) {
				Log.v(tag, "OnItemClick++++INNNNN");
				mCharIDList.add(listItems.get(pos).getUniqueCharID());
				storePlaylist();
				String pathTitle = listItems.get(pos).getFileName();
				String header = parentPath.get(dirLevel);
				String pathTag = "";
				for (int i = 1; i <= dirLevel; i++)
					pathTag += "/";
				folderPath.add(pathTag + listItems.get(pos).getFileName());
				simpleAdapter.clearSelected();
				curPathInfo.addLevelInfo(parentPath.get(dirLevel));
				curPathInfo.setLevelFocus(curPathInfo.dirLevel, pos);
				parentPath.add(header + pathTitle + "/");
				dirLevel++;
				System.out.println("parentPath is " + parentPath.get(dirLevel));
				firstShowInList();
				isKeyBack = false;
				new Thread(new Runnable() {
					public void run() {
						String tmpPath = parentPath.get(dirLevel);
						getFileList(parentPath.get(dirLevel), mCharIDList.get(mCharIDList.size()-1));
						if (!isKeyBack
								&& tmpPath.equals(parentPath.get(dirLevel))) {
							if(scanDataItems!=null)
								dumpDateFromDlna();
							handler.sendEmptyMessage(MSG_REFRESH);
						}
					}
				}).start();
			}
		}
	};

//	private void refreshUI() {
//		showInList();
//		videolist.setSelection(0);
//	}

	private void showNofile() {
		msg_hint.setMessage(resourceMgr.getString(
				R.string.msg_noFile));
		msg_hint.show();
		msg_hint.setFocusable(true);
		msg_hint.update();
		msg_hint.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
//				handler.sendEmptyMessage(MSG_GO_BACK);
			}
		});
	}

	private void gotoLastLevel() {
		if (parentPath.get(dirLevel).equals(rootPath))
			return;
		isFocused = videolist.isFocused();
		if (curPathInfo.dirLevel > 0
				&& !parentPath.get(dirLevel).equals(rootPath)) {
			parentPath.remove(dirLevel);
			folderPath.remove(dirLevel);
			dirLevel--;
			if (dirLevel == 0)
				devicePath = "";
			curPathInfo.backToLastLevel();
			final int currentPos = curPathInfo.getLastLevelFocus();
			new Thread(new Runnable() {
				public void run() {
					if(mCharIDList.size() > 0) {
						String tmpPath = parentPath.get(dirLevel);
						getFileList(parentPath.get(dirLevel), mCharIDList.get(mCharIDList.size()-1));
						if (!isKeyBack && tmpPath.equals(parentPath.get(dirLevel))) {
							if(scanDataItems!=null)
								dumpDateFromDlna();
							Message msg = new Message();
							msg.what = MSG_BACK_REFRESH;
							Bundle bundle = new Bundle();
							bundle.putBoolean("isFocused", isFocused);
							bundle.putInt("currentPos", currentPos);
							msg.setData(bundle);
							handler.sendMessage(msg);
						}
					}
				}
			}).start();
		} else if (!isFocused) {
			videolist.requestFocus();
		}
	}


	private void playVideo(int position) {
		if (msg_hint.isShowing())
			handler.sendEmptyMessage(MSG_HIDE_POPUP);

		final int playPosition = position;
		
		String fileUrl = listItems.get(playPosition).getFilePath();
		resumeIndex = mVideoBookMark.findBookMarkWithUrl(fileUrl);
		if (resumeIndex < 0) {
			Log.v(TAG, "No bookmark, play directly");
			sendIntent(playPosition, -1);
		} else {
			Log.v(TAG, "Find bookmark, resume play?");
			short_msg.confirm_title.setVisibility(View.INVISIBLE);
			short_msg.setMessage(resourceMgr.getString(R.string.msg_resumePlay));
			short_msg.setButtonText(resourceMgr.getString(R.string.msg_yes));
			short_msg.left.setVisibility(View.VISIBLE);
			short_msg.right.setVisibility(View.VISIBLE);
			short_msg.confirm_bt.setVisibility(View.VISIBLE);
			short_msg.setKeyListener(true);
			short_msg.left.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (short_msg.confirm_text.getText().toString()
							.compareTo(resourceMgr.getString(R.string.msg_yes)) == 0) {
						short_msg.confirm_text.setText(R.string.msg_no);
					} else {
						short_msg.confirm_text.setText(R.string.msg_yes);
					}
				}
			});
			short_msg.right.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if (short_msg.confirm_text.getText().toString()
							.compareTo(resourceMgr.getString(R.string.msg_yes)) == 0) {
						short_msg.confirm_text.setText(R.string.msg_no);
					} else {
						short_msg.confirm_text.setText(R.string.msg_yes);
					}
				}
			});
			short_msg.confirm_bt.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (short_msg.confirm_text.getText().toString()
							.compareTo(resourceMgr.getString(R.string.msg_yes)) == 0) {
						sendIntent(playPosition, resumeIndex);
					} else {
						mVideoBookMark.removeBookMark(resumeIndex);
						mVideoBookMark.writeBookMark();
						sendIntent(playPosition, -1);
					}

					short_msg.dismiss();
					handler.removeMessages(MSG_HIDE_SHORT);
				}

			});

			short_msg.setMessageLeft();
			short_msg.show();

			handler.sendEmptyMessageDelayed(MSG_HIDE_SHORT,
					TimerDelay.delay_60s);
		}
	}

	public class FileListAdapter extends BaseAdapter {
		private List<DLNAFileInfo> theItems;
		private GridView gridView;
		private LayoutInflater mInflater;
		private int selected = -1;
		private int focused = -1;
		private boolean fakeCnt = false;
		private int fakeCount = 0;
		private int animDuration = 1000;
		
		public FileListAdapter(Context context, List<DLNAFileInfo> mData,
				GridView gridView) {
			mInflater = LayoutInflater.from(context);
			this.theItems = mData;
			this.gridView = gridView;
		}

		public void notifyDataSetChanged(int id) {
			selected = id;
			super.notifyDataSetChanged();
		}

		public void notifyFocused(int id) {
			focused = id;
			super.notifyDataSetChanged();
		}

		public void clearSelected() {
			selected = -1;
		}

		public void setCnt(int fakeCount) {
			fakeCnt = true;
			this.fakeCount = fakeCount;
		}

		public void resetCnt() {
			fakeCnt = false;
			fakeCount = 0;
		}

		@Override
		public int getCount() {
			if(fakeCnt) {
				return fakeCount;
			}
			return theItems.size();
		}

		@Override
		public Object getItem(int position) {
			if(fakeCnt) {
				return null;
			}
			return theItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder tag = null;
			int ab_pos = position;
			DLNAFileInfo info = null;
			if(ab_pos < theItems.size()) {
				info = theItems.get(ab_pos);
			}
			boolean isSelected = false;
			boolean isFocus = false;
			if (selected == ab_pos) {
				isSelected = true;
			}
			if (focused  == ab_pos) {
				isFocus = true;
			}
			
			if (convertView == null) {
				tag = new ViewHolder();
				convertView = mInflater.inflate(R.layout.videocell, null);
				tag.imageView = (ImageView) convertView.findViewById(R.id.ItemImage);
				tag.title = (TextView) convertView.findViewById(R.id.ItemTitle);
				tag.time = (TextView) convertView.findViewById(R.id.ItemTime);
				tag.playView = (ImageView) convertView.findViewById(R.id.playImage);
				convertView.setTag(tag);
			} else {
				tag = (ViewHolder) convertView.getTag();
			}
			if(fakeCnt) {
				tag.imageView.setImageDrawable(loadingDrawable);
				tag.imageView.clearAnimation();
				final RotateAnimation animation = new RotateAnimation(0f, 360f,
						Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);
				tag.imageView.setAnimation(animation);
				animation.setDuration(animDuration);
				animation.setRepeatCount(Animation.INFINITE);
				animation.startNow();
				tag.title.setText(null);
				tag.time.setText(null);
				tag.playView.setImageDrawable(null);
				return convertView;
			}
			
			tag.imageView.clearAnimation();
			if (info.getFileType() == FileFilterType.DEVICE_FILE_DIR) {
				tag.imageView.setImageDrawable(videoImgs[0]);
				tag.playView.setImageDrawable(null);
			} else if (info.getFileType() == FileFilterType.DEVICE_FILE_VIDEO) {
				tag.imageView.setImageDrawable(videoImgs[1]);
				if(mediaApp.getStopedFileUrl() != null && mediaApp.getStopedFileUrl().equals(info.getFilePath())) {
					tag.playView.setImageDrawable(videoImgs[2]);
				} else {
					tag.playView.setImageDrawable(null);
				}
				if(checkInErrorList(info.getFilePath())) {
					tag.imageView.setImageDrawable(videoImgs[3]);
				}
			} 
			
			if (isFocus) {
				tag.title.setEllipsize(TruncateAt.MARQUEE);
				// tag.title.setTextColor(Color.WHITE);
			} else {
				tag.title.setEllipsize(TruncateAt.END);
				// tag.title.setTextColor(Color.BLACK);
			}
			tag.title.setText(info.getFileName() + (String)getResources()
							.getText(R.string.fullblank));

			// disabled for the gettotaltime api is not ready
			if (info.getFileType() == FileFilterType.DEVICE_FILE_VIDEO) {
				tag.time.setText(info.getFileDate());
			} else {
				tag.time.setText(null);
			}
			return convertView;
		}
		
		public final class ViewHolder {
			ImageView imageView;
			TextView title;
			TextView time;
			ImageView playView;
		}
	}

	class MyView {
		ImageView imageView;
		TextView title;
		TextView time;
	}

	public void sendIntent(int position, int resume_index) {

		if (task_getduration != null)
			task_getduration.cancel();

		ComponentName componetName = new ComponentName("com.rtk.dmp",
				"com.rtk.dmp.VideoPlayerActivity");
		Intent intent = new Intent();
		Bundle bundle = new Bundle();

		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		int initPos = 0;
		int totalCnt = 0;

		for (int i = 0; i < totalLen; i++) {
			if (listItems.get(i).getFileType() == FileFilterType.DEVICE_FILE_VIDEO) {
				if (listItems.get(position).getFilePath().compareTo(
						listItems.get(i).getFilePath()) == 0) {
					initPos = totalCnt;
					break;
				}
				totalCnt++;
			}
		}
		
		String serverName = null;
		serverName = mediaApp.getMediaServerName();
		Log.v("serverName is$$$$$$$$$", serverName);
		bundle.putBoolean("isanywhere", false);
		bundle.putInt("initPos", initPos);
		bundle.putInt("resume_index", resume_index);
		bundle.putString("serverName", serverName);
		bundle.putString("devicePath", devicePath);
		intent.putExtras(bundle);
		intent.setComponent(componetName);
		startActivityForResult(intent, 0);
	}

	private void PopupMessageShow(String msg, int dismiss_time) {
		msg_hint.setMessage(msg);
		msg_hint.show();

		if (task_message_time_out != null) {
			task_message_time_out.cancel();
			task_message_time_out = null;
		}

		task_message_time_out = new TimerTask() {

			@Override
			public void run() {
				handler.sendEmptyMessage(MSG_HIDE_POPUP);
			}

		};
		if (timer == null)
			timer = new Timer(true);
		timer.schedule(task_message_time_out, dismiss_time);
	}

	private void PopupMessageShow(String msg, int resid, int height, int width,
			int gravity, int x, int y, int dismiss_time) {
		msg_hint.setMessage(msg);
		msg_hint.setMessageCenterHorizotal();
		msg_hint.show(resid, height, width, gravity, x, y);

		if (task_message_time_out != null) {
			task_message_time_out.cancel();
			task_message_time_out = null;
		}

		task_message_time_out = new TimerTask() {

			@Override
			public void run() {
				handler.sendEmptyMessage(MSG_HIDE_POPUP);
			}

		};
		if (timer == null)
			timer = new Timer(true);
		timer.schedule(task_message_time_out, dismiss_time);
	}

	private void dismissLoading() {
		Log.v("hh", "hhhhhhhhh bb");
		ad.cancel();
		ad.reset();
		loadingIcon.setVisibility(View.INVISIBLE);
		Log.v("hh", "hhhhhhhhh ee");
	}

	public void onStartLoadingList() {
		Log.v("xx", "xxxxxxx bb");
		loadingIcon.setVisibility(View.VISIBLE);
		loadingIcon.startAnimation(ad);
		Log.v("xx", "xxxxxxx ee");
	}

	private void backRefresh(boolean isFocused, int currentPos, boolean reset) {
		simpleAdapter.clearSelected();
		if (reset)
			backShowInList();
		else {
			showInList();
		}
		if (listItems.size() == 0) {
			return;
		}
		if (!isFocused)
			videolist.requestFocus();
	}

	private void backShowInList() {
		totalLen = listItems.size();
		simpleAdapter.notifyDataSetChanged();
		topPath.setText(devicePath + folderPath.get(dirLevel));
		
	}

	AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView absListView,
				int scrollState) {
			switch(scrollState) {
			
			}
		}

		@Override
		public void onScroll(AbsListView absListView, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			//progress.setProgress(firstVisibleItem + pagesize);
			
		}
	};

	public void doInit() {
		
	
	}

	public void reInit() {
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		outState.putBoolean("s_DEVICECHANGED", deviceChanged);

		super.onSaveInstanceState(outState);
	}
	
	@Override
    public synchronized void update(Observable o, Object arg) {
		Log.e("videoBrowser", "update||||||||||||||||||||||||||||");
		ObserverContent content = (ObserverContent)arg;
		String serverName = content.getMsg();
		String act = content.getAction();
		if(act.equals(ObserverContent.REMOVE_DEVICE)) {
			Log.e("DLNADevice", "videoBrowser "+" removed server name: " + serverName);
			if(mediaApp.getMediaServerName().equals(serverName))
			{
				DMSName.add(serverName);
			}
		}
	}
	
	public int getOrientation() {
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			return ORIENTATION_LANDSCAPE;
		} else {
			return ORIENTATION_PORTRAIT;
		}
	}
	
	public void findViews() {
		Log.v(tag, "findViews begin");
		sortDownBG = (ImageView)(findViewById(R.id.sortdown_bg));
		sortDownIcon = (ImageView)(findViewById(R.id.sortdown));
		sortUpBG = (ImageView)(findViewById(R.id.sortup_bg));
		sortUpIcon = (ImageView)(findViewById(R.id.sortup));
		videolist = (GridView) findViewById(R.id.video_list);
		topPath = (TextView) findViewById(R.id.video_path_top);
		loadingIcon = (ImageView) findViewById(R.id.loadingIcon);
		sortUp = (View)findViewById(R.id.lay_up);
		sortDown = (View)findViewById(R.id.lay_down);
		mainMenu = (ImageView)findViewById(R.id.btn_menu);
		
		videoImgs = new Drawable[4];
		videoImgs[0] = resourceMgr.getDrawable(R.drawable.dnla_folder_icon_s);
		videoImgs[1] = resourceMgr.getDrawable(R.drawable.dnla_video_icon);
		videoImgs[2] = resourceMgr.getDrawable(R.drawable.dnla_video_playing_icon);
		videoImgs[3] = resourceMgr.getDrawable(R.drawable.broken_video_icon);
		ad = AnimationUtils.loadAnimation(this, R.drawable.video_anim);
		//init QuickMenu
	}
	
	public void initAccordingConfiguration() {	// generally only constant
		int orientation = getOrientation();
		switch(orientation) {
			case ORIENTATION_LANDSCAPE: {
				cellHeight = resourceMgr.getDimensionPixelSize(R.dimen.landscape_listItemHeight) + getResources().getDimensionPixelSize(R.dimen.landscape_list_verticalSpacing);
				cols = videolist.getNumColumns();
			}break;
			case ORIENTATION_PORTRAIT: {
				cellHeight = resourceMgr.getDimensionPixelOffset(R.dimen.portrait_listItemHeight) + getResources().getDimensionPixelSize(R.dimen.portrait_list_verticalSpacing);
				cols = videolist.getNumColumns();
			}break;
		}
	}
	
	public void initWhenOnCreate() {
		if (mTv == null) {
			mTv = (TvManager) getSystemService("tv");
		}
		rootPath = mediaApp.getSubRootPath();
		if (rootPath != null && !rootPath.endsWith("/")) {
			rootPath += "/";
		}
		Log.v("init--", "rootPath ++++++++++++"+ rootPath);
		mMimeTypes = mediaApp.getMimeTypes();
		quickmenu = new M_QuickMenu(mainMenu, quickmenuAdapter, menuWidth);
		OnItemClickListener quickmenuItemClickListener = new OnItemClickListener()
        {
        	@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
        		//quickAutoQuit();	//delay hide menu
				switch(position)
				{
					case 0:
					{
						ComponentName componetName = new ComponentName("com.android.emanualreader",
								"com.android.emanualreader.MainActivity");
						quickmenu.dismiss();					
						Intent intent = new Intent();
						intent.setComponent(componetName);
						startActivity(intent);
						break;
					}
					case 1:
					{
				    	handler.removeMessages(MSG_QUICK_HIDE);	
						finish();
						break;
					}
					default:
						break;
				}
			}     	
        };
        quickmenu.AddOnItemClickListener(quickmenuItemClickListener);
	}
	
	public void setListeners() {
		videolist.setOnItemClickListener(itemClickListener);
		videolist.setOnScrollListener(onScrollListener);
		videolist.setOnItemSelectedListener(itemSelectListener);
		sortUp.setOnClickListener(bt_click);
		sortDown.setOnClickListener(bt_click);
		mainMenu.setOnClickListener(bt_click);
		ad.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub
				if(loading) {
					loadingIcon.startAnimation(ad);
				}
			}
		});
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_REFRESH_TIMER : {
					quickmenuAdapter.notifyDataSetChanged();
				} break;
				case MSG_QUICK_HIDE: {	// hide Menu
					if(quickmenu.isShowing()) {
						quickmenu.dismiss();
					}
				} break;
				case MSG_HIDE_POPUP:
					msg_hint.dismiss();
					break;
				case MSG_HIDE_SHORT:
					short_msg.dismiss();
					break;
				// add by jessie
				case MSG_DEVICED_ADDORREMOVED:
					if (parentPath.size() != 0 && dirLevel != -1
							&& parentPath.get(dirLevel) == rootPath) {
						showRootList();
					}
					break;
				case MSG_REFRESH:
					simpleAdapter.clearSelected();
					showInList();
					break;
				case MSG_FIRST_REFRESH:
					Log.v(tag, "MSG_FIRST_REFRESH");
					parentPath.add(rootPath);
					folderPath.add("");
					dirLevel++;
					curPathInfo.addLevelInfo(rootPath);
					showInList();
					videolist.setSelection(0);
					break;
				case MSG_BACK_REFRESH:
					boolean isFocused = msg.getData().getBoolean("isFocused");
					int currentPos = msg.getData().getInt("currentPos", 0);
					loading = false;
					backRefresh(isFocused, currentPos, msg.getData().getBoolean("reset"));
					break;
				case MSG_NORMAL_REFRESH:
					showInList();
					videolist.setSelection(0);
					break;
				// end add
				case MSG_START_LOADING:
					loading = true;
					onStartLoadingList();
					break;
				case MSG_STOP_LOADING:
					loading = false;
					dismissLoading();
					break;
				case MSG_GO_BACK: {
					gotoLastLevel();
				} break;
				case MSG_BROWSERLISTLOADING: {
					if(simpleAdapter != null) {
						simpleAdapter.setCnt(msg.arg1);
						simpleAdapter.notifyDataSetChanged();
					}
				} break;
				case MSG_BROWSERLISTDISMISSLOADING: {
					if(simpleAdapter != null) {
						simpleAdapter.resetCnt();
						simpleAdapter.notifyDataSetChanged();
					}
				}
				default:
					break;
				}
				super.handleMessage(msg);
			}
		};	
		
	}
	
	private void onMenu() {
		Log.v(tag, "Menu Click");
		if(quickmenu.isShowing()){
			quickmenu.dismiss();
			handler.removeMessages(MSG_QUICK_HIDE);
		}
		else
		{
			Log.v(tag, "show quickMneu");
			// where to show Menu;
			quickmenu.show();
		}
	}
	
	private void quickAutoQuit() {
		handler.removeMessages(MSG_QUICK_HIDE);
		Message msg = handler.obtainMessage(MSG_QUICK_HIDE);
		handler.sendMessageDelayed(msg, quick_timeout);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		//UI data reserve
		int selectId = videolist.getSelectedItemPosition(); 		//AdapterView.INVALID_POSITION;
		Log.v(tag, "selectId when change configuration +++" + selectId);
		boolean loadingIconVisible = false;		//get LoadingIcon visibility
		if(loadingIcon.isShown()) {
			loadingIconVisible = true;
		}
		
		//refresh views and ..
		setContentView(R.layout.video_browser);
		findViews();
		setListeners();
		initAccordingConfiguration();
		// recovery
		videolist.setAdapter(simpleAdapter);
		videolist.requestFocus();
		if(selectId == AdapterView.INVALID_POSITION) {
			selectId = 0;
		} 
		videolist.setSelection(selectId);
		simpleAdapter.notifyDataSetChanged(selectId);
		
		if(loadingIconVisible) {
			loadingIcon.setVisibility(View.VISIBLE);
		} else {
			loadingIcon.setVisibility(View.INVISIBLE);
		}
	}
	
	boolean checkInErrorList(String url) {
		if(mediaApp.getErrorVideoList().contains(url)) {
			return true;
		} else {
			return false;
		}
	}
}


class DLNAFileInfoComparatorByDate extends DLNAFileInfoComparator {

	public DLNAFileInfoComparatorByDate(int mode) {
		super(mode);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int compare(DLNAFileInfo object1, DLNAFileInfo object2) {
		// TODO Auto-generated method stub
		String  m1=object1.getFileDate();
        String  m2=object2.getFileDate();
        int result=0;
        if(m1.compareToIgnoreCase(m2)>0)
        {
            result = super.mode;
        }
        if(m1.compareToIgnoreCase(m2)<0)
        {
            result = -super.mode;
        }
        return result;
	}
	
}

class DLNAFileInfoComparatorByName extends DLNAFileInfoComparator {

	public DLNAFileInfoComparatorByName(int mode) {
		super(mode);
		// TODO Auto-generated constructor stub
	}
	
}
