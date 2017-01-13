package com.rtk.dmp;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.TvManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.AsyncImageLoader;
import com.realtek.Utils.AsyncImageLoader.ImageInfoCallback;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.DLNAFileInfoComparator;
import com.realtek.Utils.DensityUtil;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.realtek.Utils.widget.VerticalBar;

public class AudioBrowser extends Activity  implements Observer{
	private GridView audiolist;
	private TextView topPath;
	private static final int maxStack = 10;
	private final int PROGRESS_CHANGED = 1;
	private final int UPDATE_UI_FACE = 2;
	private final int START_LOADING = 3;
	private final int STOP_LOADING = 4;
	private final int UPDATE_TIME = 5;
	private final int UPDATE_UI_BROKEN = 6;
	private final int MSG_LOADING = 7;
	private final int MSG_PLAYING = 8;
	private final int MSG_SET_REPEAT = 11;
	private final int MSG_QUICK_HIDE = 12;
	private final int MSG_QUICKMSG_HIDE = 13;
	private final int MSG_DEVICE_REMOVED = 15;
	private final int MSG_BACK_REFRESH = 16;
	private final int MSG_NORMAL_REFRESH = 17;
	private final int MSG_REFRESH = 18;
	private final int MSG_FIRST_REFRESH = 19;
	private final int MSG_GO_BACK = 14;
	private final int MSG_HIDE_HINT = 20;
	private final int MSG_REFRESH_TIMER = 21;
	private long quick_timeout = 6000;
	private boolean isFocused = true;
	private Path_Info curPathInfo;
	private int dirLevel = -1;
	private ArrayList<String> parentPath = new ArrayList<String>();
	private String rootPath;
	private Timer timer = null;
	private Timer timer2 = null;
	private Handler handler;
	private Runnable PlayingRunnable;
	private int totalLen;
	private int pagesize;
	private QuickMenu quickmenu=null;
	private QuickMenuAdapter quickmenuAdapter=null;
	String date = null;
	Drawable audioImgs[] = new Drawable[5];
	Drawable loadingImg;
	ArrayList<DLNAFileInfo> listItems = null;
	ArrayList<DLNAFileInfo> scanDataItems = null;
	ImageView loadingIcon = null;
	Animation ad = null;
	FileListAdapter simpleAdapter = null;
	public static boolean changeIndex = false;
	public static boolean reset = false;
	private int repeatIndex = 0;
	private SharedPreferences mPerferences = null;
	private MediaApplication map = null;
	private TvManager mTv = null;
	int[] positions = new int[2];
	private String devicePath = "";
	private ArrayList<String> folderPath = new ArrayList<String>();
	private ArrayList<String> DMSName = new ArrayList<String>();
	private DLNA_DMP_1p5 dlna_DMP_1p5;
	public Activity mContext = this;
	private ConfirmMessage short_msg = null;
	private int ResultCodeRestartAudioBrowser = 10;
	private boolean isKeyBack = false;
	private PopupMessage msg_hint = null;
	Message_not_avaible msg_notavaible = null;
	private int sortMode = 1;//0 default, -1 up, 1 down
	private int dateSortMode = 0;//0 default, -1 up, 1 down
	private ImageView sortUp;
	private ImageView sortDown;
	private boolean loading = false;
	private int orientation;
	private AsyncImageLoader loader;
	private Stack<StackContent> listStack;
	private List<String> mCharIDList = new ArrayList<String>();
	private ImageView btn_menu;
	private Context context;
	private ImageView sortDownIcon;
	private ImageView sortUpIcon;
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
		context = this;
		orientation = getResources().getConfiguration().orientation;//LANDSCAPE or PORTRAIT
		initVariable();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.music);
		map = (MediaApplication) getApplication();
		map.addObserver(this);
		initAudioImg();
		initLoading();
		initServiceMsg();
		rootPath = map.getSubRootPath();
		if(rootPath==null)
			finish();
		if(!rootPath.endsWith("/")){
			rootPath += "/";
		}
		if (mTv == null) {
			mTv = (TvManager) getSystemService("tv");
		}
		listStack = new Stack<StackContent>();
		reset = false;
		audiolist = (GridView) findViewById(R.id.audio_list);
		topPath = (TextView) findViewById(R.id.music_path_top);
		initLayout();
		mPerferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (curPathInfo == null)
			curPathInfo = new Path_Info();
		short_msg = new ConfirmMessage(mContext, 678, 226);
		audiolist.setOnItemSelectedListener(itemSelectedListener);
		audiolist.setOnItemClickListener(itemClickListener);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		initHandler();
		showRootList();
		parentPath.add(rootPath);
		folderPath.add("");
		dirLevel++;
		curPathInfo.addLevelInfo(rootPath);
		initMenu();
	}
	private int cellHeight;
	private void initVariable(){
		Resources r=getResources(); 
		cellHeight = r.getDimensionPixelSize(R.dimen.listItemHeight) +
		r.getDimensionPixelSize(R.dimen.dividerHeight);
		if(orientation == Configuration.ORIENTATION_LANDSCAPE){
			cols = 2;
		}else if(orientation == Configuration.ORIENTATION_PORTRAIT){
			cols = 1;
		}
	}
	
	private int cols;
	private void initHandler(){
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case PROGRESS_CHANGED:
					handler.post(PlayingRunnable);
					break;
				case UPDATE_UI_FACE:
					loadingIcon.getAnimation().cancel();
					loadingIcon.setVisibility(View.INVISIBLE);
					loadingIcon.setImageResource(R.drawable.blank);
					break;
				case START_LOADING:
					loadingIcon.getAnimation().reset();
					loadingIcon.getAnimation().startNow();
					loadingIcon.setVisibility(View.VISIBLE);
					loadingIcon
							.setImageResource(R.drawable.others_icons_loading);
					break;
				case STOP_LOADING:
					if(loadingIcon != null && loadingIcon.getAnimation() != null)
					{
						loadingIcon.getAnimation().cancel();          	
	            		loadingIcon.setVisibility(View.INVISIBLE);
	            		loadingIcon.setImageResource(R.drawable.blank);
					}
					break;
				case UPDATE_TIME:
					int index = msg.getData().getInt("index");
					String time = msg.getData().getString("time");
					updateList(index, time);
					break;
				case UPDATE_UI_BROKEN:
					loadingIcon.getAnimation().cancel();
					loadingIcon.setVisibility(View.INVISIBLE);
					loadingIcon.setImageResource(R.drawable.blank);
					simpleAdapter.clearSelected();
					simpleAdapter.notifyDataSetChanged();
					break;
				case MSG_LOADING:
					loading = true;
					break;
				case MSG_PLAYING:
					Bundle bun = msg.getData();
					playindex = bun.getInt("playindex");
					ArrayList<DLNAFileInfo> playListItems = map
							.getPlayList();
					if ((listItems.size() > playindex)
							&& listItems
									.get(playindex)
									.getFilePath()
									.equals(playListItems.get(playindex)
											.getFilePath())) {
						simpleAdapter.notifyDataSetChanged(playindex);
					}
					break;
				case MSG_SET_REPEAT:

					break;
				case MSG_QUICK_HIDE: {
					if(quickmenu!=null)
					quickmenu.dismiss();
				}
				break;
				case MSG_QUICKMSG_HIDE: {
					if(msg_notavaible!=null)
					msg_notavaible.dismiss();
				}
					break;
				case MSG_DEVICE_REMOVED: {
					beforeFinish();
					finish();
				}
					break;
				case MSG_BACK_REFRESH:
					boolean isFocused = msg.getData().getBoolean("isFocused");
					int currentPos = msg.getData().getInt("currentPos", 0);
					loading = false;
					backRefresh(isFocused, currentPos, msg.getData()
							.getBoolean("reset"));
					break;
				case MSG_NORMAL_REFRESH:
					loading = false;
					dismissLoading();
					showInList();
					audiolist.setSelection(0);
					break;
				case MSG_REFRESH:
					loading = false;
					dismissLoading();
					simpleAdapter.clearSelected();
					showInList();
					break;
				case MSG_FIRST_REFRESH:
					loading = false;
					dismissLoading();
					parentPath.add(rootPath);
					folderPath.add("");
					dirLevel++;
					curPathInfo.addLevelInfo(rootPath);
					showInList();
					audiolist.setSelection(0);
					break;
				case MSG_GO_BACK: {
					gotoLastLevel();
				}
					break;
				case MSG_HIDE_HINT: {
					if (msg_hint != null) {
						msg_hint.setOnDismissListener(null);
						msg_hint.dismiss();
					}
				}
					break;
				case MSG_REFRESH_TIMER:{					
					quickmenuAdapter.notifyDataSetChanged();
				}
				break;
				default:
					break;
				}
				super.handleMessage(msg);
			}
		};
	}
	int playindex =0;
	private void showRootList(){
		if(simpleAdapter!=null)
			simpleAdapter.clearSelected();
		firstShowInList();
		onStartLoadingList();
		isKeyBack = false;
		handler.sendEmptyMessage(MSG_LOADING);
		new Thread(new Runnable() {
			public void run() {
				String tmpPath = rootPath;
				getFileList(rootPath, null);
				if (!isKeyBack && tmpPath.equals(parentPath.get(dirLevel))) {
					if(scanDataItems!=null)
						dumpDateFromDlna();
					handler.sendEmptyMessage(MSG_NORMAL_REFRESH);
				}
			}
		}).start();
	}
	
	private void dumpDateFromDlna(){
		if(listItems== null)
		{
			listItems = map.getFileList();
		}else
			listItems.clear();
		for(DLNAFileInfo info : scanDataItems){
			listItems.add(info);
		}
	}
	private void showNotAvail(){
		if(msg_notavaible == null)
			msg_notavaible = new Message_not_avaible(AudioBrowser.this);
		msg_notavaible.show_msg_notavailable();	
		quickMsgAutoQuit();
	}

	public void onResume() {
		super.onResume();
		mActivityPauseFlag = 0;
		changeIndex = false;
		new Thread(new Runnable() {
			@Override
			public void run() {
				int tmp_index = mPerferences.getInt("repeatIndex", -1);
				if (tmp_index == -1) {
					Editor editor = mPerferences.edit();//
					editor.putInt("repeatIndex", repeatIndex);
					editor.commit();
				} else {
					repeatIndex = tmp_index;
				}
				handler.sendEmptyMessage(MSG_SET_REPEAT);
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
	private void initLayout(){
		btn_menu = (ImageView) findViewById(R.id.btn_menu);
		btn_menu.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onMenu();
			}
			
		});
		sortUp = (ImageView) findViewById(R.id.sortup_bg);
		sortDown = (ImageView) findViewById(R.id.sortdown_bg);
		sortUp.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				sortUp.setImageResource(R.drawable.dnla_sorting_base_up);
				sortUpIcon.setImageResource(R.drawable.dnla_sorting_icon_up);
				sortDown.setImageResource(R.drawable.dnla_sorting_base_down);
				sortDownIcon.setImageResource(R.drawable.dnla_sorting_icon_down);
				if(dateSortMode ==0)
					sort(1,-1);
				else
					sort(1,dateSortMode*-1);
			}
			
		});	
		sortDown.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				sortUp.setImageResource(R.drawable.dnla_sorting_base_down);
				sortUpIcon.setImageResource(R.drawable.dnla_sorting_icon_down);
				sortDown.setImageResource(R.drawable.dnla_sorting_base_up);
				sortDownIcon.setImageResource(R.drawable.dnla_sorting_icon_up);
				sort(0,sortMode*-1);
			}
			
		});	
		progress = (VerticalBar)findViewById(R.id.seek_progressBar);
		progress.setVisibility(View.INVISIBLE);
		sortDownIcon = (ImageView)(findViewById(R.id.sortdown));
		sortUpIcon = (ImageView)(findViewById(R.id.sortup));
	}
	@Override
	public void onPause() {
		mActivityPauseFlag = 1;
		if (timer2 != null) {
			timer2.cancel();
			timer2 = null;
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		super.onPause();
	}
	private void beforeFinish() {
		if (timer2 != null) {
			timer2.cancel();
			timer2 = null;
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	@Override
	public void onStop() {
		super.onStop();
		if(loader!=null)
			AsyncImageLoader.opQueue(0, null);
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(quickmenu !=null)
		{
			quickmenu.dismiss();
			quickmenu =null;
		}
		if(msg_hint !=null)
		{
			msg_hint.dismiss();
			msg_hint =null;
		}
		unregisterReceiver(serviceReceiver);
		if (curPathInfo != null)
			curPathInfo.cleanLevelInfo();
		map.deleteObserver(this);
		//AsyncImageLoader.isQuit(0);
	}
	@Override
	public void onRestart() {
		simpleAdapter.notifyDataSetChanged();	
		super.onRestart();
	}
	private void gotoLastLevel() {
		dismissLoading();
		if (parentPath.get(dirLevel).equals(rootPath))
			return;
		isFocused = audiolist.isFocused();
		if (curPathInfo.dirLevel > 0
				&& !parentPath.get(dirLevel).equals(rootPath)) {
			parentPath.remove(dirLevel);
			folderPath.remove(dirLevel);
			dirLevel--;
			if (dirLevel == 0)
				devicePath = "";
			curPathInfo.backToLastLevel();
			final int currentPos = curPathInfo.getLastLevelFocus();
			handler.sendEmptyMessage(MSG_LOADING);
			if(mCharIDList.size() > 0)
				mCharIDList.remove(mCharIDList.size()-1);
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
			audiolist.requestFocus();
		}
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			if (parentPath.get(dirLevel).equals(rootPath)){
				DLNA_DMP_1p5.DLNA_DMP_1p5_stopWatingResponse();
				dismissLoading();
				return super.onKeyDown(keyCode, event);
			}
			isKeyBack = true;
			if(loading){
				processOnLoading();
				return true;
			}else{
				processBackLevel();
			}
			return true;
		}
		case KeyEvent.KEYCODE_ESCAPE:
			beforeFinish();
			finish();
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
	private void processBackLevel(){
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
				firstShowInList();
				onStartLoadingList();
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
	private void processOnLoading(){
		DLNA_DMP_1p5.DLNA_DMP_1p5_stopWatingResponse();
		dismissLoading();
		processBackLevel();
	}
	private OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {

		public void onItemSelected(AdapterView<?> parent, View v, int position,
				long id) {
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}

	};

	private void dismissLoading() {
		if (loadingIcon != null && loadingIcon.getAnimation() != null) {
			loadingIcon.getAnimation().cancel();
			loadingIcon.setVisibility(View.INVISIBLE);
			loadingIcon.setImageResource(R.drawable.blank);
		}
	}
	private OnItemClickListener itemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			dismissLoading();
			final int pos = position;
			String tmpPath = parentPath.get(dirLevel);
			boolean isRemoved = false;
			if (DMSName.size() != 0) {
				for (int i = 0; i < DMSName.size(); i++) {
					isRemoved = tmpPath.contains(DMSName.get(i));
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
					short_msg.confirm_bt
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									short_msg.dismiss();
								}

							});

					short_msg.setOnDismissListener(new OnDismissListener() {

						@Override
						public void onDismiss() {
							simpleAdapter.clearSelected();
							parentPath.clear();
							folderPath.clear();
							parentPath.add(rootPath);
							folderPath.add("");
							dirLevel = 0;
							devicePath = "";
							curPathInfo.backToDeviceLevel();
							DMSName.clear();
							firstShowInList();
							onStartLoadingList();
							isKeyBack = false;
							handler.sendEmptyMessage(MSG_LOADING);
							new Thread(new Runnable() {
								public void run() {
									String tmpPath = rootPath;
									getFileList(rootPath, null);
									if (!isKeyBack
											&& tmpPath.equals(parentPath
													.get(dirLevel))) {
										if(scanDataItems!=null)
											dumpDateFromDlna();
										handler.sendEmptyMessage(MSG_NORMAL_REFRESH);
									}
								}
							}).start();

						}
					});

					short_msg.show();
					return;
				}
			}
			if (listItems.get(pos).getFileType() == FileFilterType.DEVICE_FILE_AUDIO) {
				if (listItems.get(pos).getCanPlay() == -1) {
					/*Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getResources().getString(
									R.string.unsupport_file),
							Toast.LENGTH_SHORT).show();*/
					return;
				}

				String serverName = null;
				serverName = map.getMediaServerName();
				setPlaylist();
				ComponentName componetName = new ComponentName(
						"com.rtk.dmp",
						"com.rtk.dmp.MusicActivity");
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("currPath", parentPath.get(dirLevel));
				bundle.putString("devicePath", devicePath);
				bundle.putInt("initPos", pos);
				bundle.putString("serverName", serverName);
				intent.putExtras(bundle);
				intent.setComponent(componetName);
				startActivityForResult(intent, 0);
			} else if (listItems.get(pos).getFileType() == FileFilterType.DEVICE_FILE_DIR) {
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
				onStartLoadingList();
				isKeyBack = false;
				handler.sendEmptyMessage(MSG_LOADING);
				new Thread(new Runnable() {
					public void run() {
						String tmpPath = parentPath.get(dirLevel);
						getFileList(parentPath.get(dirLevel), mCharIDList.get(mCharIDList.size()-1));
						if (!isKeyBack
								&& tmpPath.equals(parentPath.get(dirLevel))) {
							if(scanDataItems!=null)
								dumpDateFromDlna();
							Message msg = new Message();
							msg.what = MSG_REFRESH;
							Bundle bundle = new Bundle();
							msg.setData(bundle);
							handler.sendMessage(msg);
						}
					}
				}).start();
			}
		}
	};

	private void setPlaylist() {
		ArrayList<DLNAFileInfo> playlistItems = new ArrayList<DLNAFileInfo>(listItems);
		map.setPlayList(playlistItems);
		map.setFileDirnum(dirNum);
	}

	private void updatePlaylist() {
		ArrayList<DLNAFileInfo> playlistItems = map.getPlayList();
		if (playlistItems == null || listItems.size() != playlistItems.size() || playindex < 0)
			return;
		String playPath = playlistItems.get(playindex).getFilePath();
		Intent intent = new Intent();
		intent.setAction("com.rtk.dmp.updatelist.broadcast");
		sendBroadcast(intent);
		boolean find = false;
		int size = playlistItems.size();
		for (int i = 0; i < size; i++) {
			DLNAFileInfo B;
			B = listItems.get(i);
			if (!find && B.getFilePath().equals(playPath)) {
				playindex = i;
				find = true;
			}
		}
		if(find){
			simpleAdapter.notifyDataSetChanged(playindex);
		}
	}
	
	private void storePlaylist(){
		if(dirLevel>= maxStack)
			return;
		ArrayList<DLNAFileInfo> playlistItems = new ArrayList<DLNAFileInfo>(listItems);
		StackContent data = new StackContent(playlistItems,dirNum);
		listStack.push(data);
	}
	private int dirNum;
	private void resetPlaylist(){
		StackContent data = listStack.pop();
		ArrayList<DLNAFileInfo> playlistItems = data.getData();
		dirNum = data.getDirNum();
		listItems.clear();
		for (DLNAFileInfo info : playlistItems)
			listItems.add(info);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0)// plug out usb when ;
		{
			if (resultCode == 1)//
			{
				beforeFinish();
				finish();
				return;
			} else if (resultCode == ResultCodeRestartAudioBrowser) {
				parentPath.clear();
				folderPath.clear();
				curPathInfo.cleanLevelInfo();
				dirLevel = -1;
				devicePath = "";
				simpleAdapter = null;
				firstShowInList();
				onStartLoadingList();
				isKeyBack = false;
				handler.sendEmptyMessage(MSG_LOADING);
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
			}
		}
	}

	public synchronized void getFileList(String path, String uniqueCharID) {
		boolean upnpBrowserSuccess = false;
		int iDirSize = 0, iItemSize = 0;
		if(path == null || path.length() == 0)
			return;
		if (path.equals(rootPath)) {
			upnpBrowserSuccess = DLNADataProvider.browseServer(map
					.getMediaServerUUID());
			mCharIDList.add(map.getMediaServerUUID());
		} else {
			if(uniqueCharID != null)
				upnpBrowserSuccess = DLNADataProvider
					.browserDirectory(uniqueCharID);
		} 

		if (upnpBrowserSuccess) {
			iDirSize = DLNADataProvider.getDirectorySize();
			iItemSize = DLNADataProvider.getItemSize();
		} else {
			iDirSize = 0;
			iItemSize = 0;
			return;
		}
		dirNum = iDirSize;
		CreateTotalFileList(iDirSize, iItemSize);
	}

	public void CreateTotalFileList(int iDirSize, int iItemSize) {
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
				
				if(DLNADataProvider.GetMediaType(FileName) == FileFilterType.DEVICE_FILE_AUDIO) {
					String itemCharID = DLNADataProvider.getItemCharID(i);
					DLNAFileInfo finfo = new DLNAFileInfo(i, FileName, itemCharID,
							FileFilterType.DEVICE_FILE_AUDIO,
							DLNADataProvider.getItemUrl(itemCharID),
							DLNADataProvider.queryDataByFile(FileName,
									DLNADataProvider.UPNP_DMP_RES_DATE, null),
							DLNADataProvider.queryDataByFile(FileName,
									DLNADataProvider.UPNP_DMP_RES_ARTIST, null),
							DLNADataProvider.queryDataByFile(FileName,
									DLNADataProvider.UPNP_DMP_RES_ALBUM, null));
					scanDataItems.add(finfo);
				}
			}
		}
	}

	private void showNofile(){
		if(msg_hint == null){
			msg_hint = new PopupMessage(context);
		}
		msg_hint.setMessage(getApplicationContext().getResources()
				.getString(R.string.msg_noFile));
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

	private void showInList() {
		totalLen = listItems.size();
		if (totalLen == 0) {
			showNofile();
			return;
		}
		if (totalLen >= MediaApplication.MAXFILENUM) {
			PopupMessageShow(
					(String) getApplicationContext().getText(
							R.string.maxfilenum), R.drawable.message_box_bg,
					260, 678, Gravity.CENTER, 0, 0, 5000);
		}

		if (MediaApplication.DEBUG)
			Log.e("AudioBrowser", "showInList: size=" + totalLen);

		if (simpleAdapter == null) {
			simpleAdapter = new FileListAdapter(this, listItems, audiolist);
			audiolist.setAdapter(simpleAdapter);
		} else
			simpleAdapter.notifyDataSetChanged();
		topPath.setText(devicePath + folderPath.get(dirLevel));
		int totalH = audiolist.getHeight();
		pagesize = (totalH / cellHeight)*cols;
		if(totalLen>pagesize){
			progress.setVisibility(View.VISIBLE);
		progress.setMax(totalLen);
		progress.setProgress(totalLen>pagesize?pagesize:totalLen);
		//initSeekProgressBar();
		}else{
			progress.setVisibility(View.INVISIBLE);
		}
		boolean updatePlaylist = false;
		ArrayList<DLNAFileInfo> playlistItems = map.getPlayList();
		if(playlistItems !=null && playlistItems.size() >0 && playlistItems.size()==listItems.size()
				&& listItems.get(0).getFilePath().equals(playlistItems.get(0).getFilePath()))
			updatePlaylist = true;
		if(updatePlaylist)
			updatePlaylist();
	}
	
	
	private void backShowInList() {
		totalLen = listItems.size();
		simpleAdapter.notifyDataSetChanged();
		topPath.setText(devicePath + folderPath.get(dirLevel));
		int totalH = audiolist.getHeight();
		pagesize = (totalH / cellHeight)*cols;
		if(totalLen>pagesize){
			progress.setVisibility(View.VISIBLE);
		progress.setMax(totalLen);
		progress.setProgress(totalLen>pagesize?pagesize:totalLen);
		//initSeekProgressBar();
		}else{
			progress.setVisibility(View.INVISIBLE);
		}
	}

	private void PopupMessageShow(String msg, int resid, int height, int width,
			int gravity, int x, int y, final int dismiss_time) {
		if(msg_hint == null){
			msg_hint = new PopupMessage(context);
		}
		if (msg_hint.isShowing() == true) {
			msg_hint.dismiss();
		}
		msg_hint.setMessage(msg);
		msg_hint.show(resid, height, width, gravity, x, y);
		autoQuithint(dismiss_time);
	}

	private void autoQuithint(final int dismiss_time) {
		handler.removeMessages(MSG_HIDE_HINT);
		Message msg = handler.obtainMessage(MSG_HIDE_HINT);
		handler.sendMessageDelayed(msg, dismiss_time);
	}

	private void initAudioImg() {

			audioImgs[0] = this.getResources().getDrawable(
					R.drawable.dnla_folder_icon_m);
			audioImgs[1] = this.getResources().getDrawable(
					R.drawable.dnla_music_icon);
			audioImgs[2] = this.getResources().getDrawable(
					R.drawable.list_common_icon_vfolder);
			audioImgs[3] = this.getResources().getDrawable(
					R.drawable.dnla_playing_icon);
			audioImgs[4] = this.getResources().getDrawable(
					R.drawable.dlna_music_icon_missing);
		loadingImg = this.getResources().getDrawable(R.drawable.dnla_loading_icon);
		blanktime = this.getResources().getString(R.string.blanktime);
	}
	private String blanktime = "";
	private void initLoading() {
		loadingIcon = (ImageView) findViewById(R.id.loadingIcon);
		ad = AnimationUtils.loadAnimation(this, R.drawable.anim);
		loadingIcon.setAnimation(ad);
		loadingIcon.getAnimation().cancel();
		loadingIcon.setVisibility(View.INVISIBLE);
		loadingIcon.setImageResource(R.drawable.blank);
	}

	private void updateList(int index, String time) {
		listItems.get(index).setTime(time);
		simpleAdapter.notifyDataSetChanged();
	}

	private int animDuration = 1000;

	public class FileListAdapter extends BaseAdapter {
		private List<DLNAFileInfo> theItems;
		private GridView listView;
		private LayoutInflater mInflater;
		private int selected = -1;
		private int focused = -1;

		public FileListAdapter(Context context, List<DLNAFileInfo> mData,
				GridView listView) {
			mInflater = LayoutInflater.from(context);
			loader = AsyncImageLoader.getInstance();
			this.theItems = mData;
			this.listView = listView;
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

		@Override
		public int getCount() {
				return theItems.size();

		}

		@Override
		public Object getItem(int position) {

			return null;

		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder tag = null;
			int ab_pos = position;
			final DLNAFileInfo info = theItems.get(ab_pos);
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
				convertView = mInflater.inflate(R.layout.audiocell, null);
				tag.imageView = (ImageView) convertView
						.findViewById(R.id.ItemImage);
				tag.title = (TextView) convertView.findViewById(R.id.ItemTitle);
				tag.time = (TextView) convertView.findViewById(R.id.ItemTime);
				tag.playView = (ImageView)convertView.findViewById(R.id.playImage);
				convertView.setTag(tag);
			} else {
				tag = (ViewHolder) convertView.getTag();
			}
			if(isSelected){
				tag.playView.setImageDrawable(audioImgs[3]);
			}else{
				tag.playView.setImageDrawable(null);
			}
			if (info.getFileType() == FileFilterType.DEVICE_FILE_DIR)
				tag.imageView.setImageDrawable(audioImgs[0]);
			else if (info.getFileType() == FileFilterType.DEVICE_FILE_AUDIO) {
				tag.imageView.setImageDrawable(audioImgs[1]);
			} else if (info.getFileType() == FileFilterType.DEVICE_FILE_VDIR) {
				tag.imageView.setImageDrawable(audioImgs[2]);
			}
			final RotateAnimation animation = new RotateAnimation(0f, 360f,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(animDuration);
			animation.setRepeatCount(Animation.INFINITE);
			if(theItems.get(ab_pos).getCanPlay()==-1){
				tag.imageView.setImageDrawable(audioImgs[4]);
			}
			else if (!theItems.get(ab_pos).isLoading()
					&& theItems.get(ab_pos).isHasImage()
					&& info.getFileType() == FileFilterType.DEVICE_FILE_AUDIO) {
				tag.imageView.clearAnimation();
				theItems.get(ab_pos).setLoading(true);
				String url = theItems.get(ab_pos).getFilePath();

					Bitmap cachedInt = loader.loadImage(url, ab_pos,
							new ImageInfoCallback() {
								public void infoLoaded(Bitmap img, String url,
										int pos,boolean cancel) {
									try {
										if(cancel){
											if(theItems.get(pos).getFilePath().equals(url))
												updateView(pos); 
											return;
										}
										if (img == null ) {
											theItems.get(pos).setHasImage(false);
										}
										theItems.get(pos).setLoading(false);
										if(theItems.get(pos).getFilePath().equals(url))
											updateView(pos); 
										//notifyDataSetChanged();
									} catch (Exception e) {
										if (MediaApplication.DEBUG)
											Log.e("reload", "" + e.getMessage());
									}
								}
							});
					if (cachedInt != null) {
						tag.imageView.setImageBitmap(cachedInt);
						theItems.get(ab_pos).setLoading(false);
						tag.title.setText(info.getFileName()+ (String)getResources().getText(R.string.fullblank));
						String strDate = info.getFileDate();
						if(strDate!=null && strDate.length()>1)
							tag.time.setText(strDate);
						else
							tag.time.setText(blanktime);
					}else{
						theItems.get(ab_pos).setLoading(true);
						tag.imageView.setImageDrawable(loadingImg);
						tag.imageView.clearAnimation();
						tag.imageView.setAnimation(animation);
						animation.startNow();
					}
				}else{
					tag.imageView.clearAnimation();
					theItems.get(ab_pos).setLoading(false);
					tag.title.setText(info.getFileName()+ (String)getResources().getText(R.string.fullblank));
					String strDate = info.getFileDate();
					if(!(info.getFileType() == FileFilterType.DEVICE_FILE_AUDIO))
						tag.time.setText("");
					else if(strDate!=null && strDate.length()>1)
						tag.time.setText(strDate);
					else
						tag.time.setText(blanktime);
				}

			if (isFocus) {
				tag.title.setEllipsize(TruncateAt.MARQUEE);
				// tag.title.setTextColor(Color.WHITE);
			} else {
				tag.title.setEllipsize(TruncateAt.END);
				// tag.title.setTextColor(Color.BLACK);
			}
			return convertView;
		}
		public void updateView(int itemIndex) { 
	        int visiblePosition = listView.getFirstVisiblePosition();  
	        if (itemIndex - visiblePosition >= 0) {
	            View view = listView.getChildAt(itemIndex - visiblePosition);  
	            ViewHolder holder = (ViewHolder) view.getTag(); 
	            holder.imageView = (ImageView) view
						.findViewById(R.id.ItemImage);
				holder.imageView.clearAnimation();
				holder.imageView.setImageDrawable(audioImgs[1]);
				DLNAFileInfo info = theItems.get(itemIndex);
				holder.title.setText(info.getFileName()+ (String)getResources().getText(R.string.fullblank));
				String strDate = info.getFileDate();
				if(strDate!=null && strDate.length()>1)
					holder.time.setText(strDate);
				else
					holder.time.setText(blanktime);
				if (theItems.get(itemIndex).isHasImage()) {
					String url = theItems.get(itemIndex).getFilePath();

					Bitmap cachedInt = loader.loadImage(url, itemIndex,
							new ImageInfoCallback() {
								public void infoLoaded(Bitmap img, String url,
										int pos,boolean cancel) {
								}
							});
					if (cachedInt != null) {
						holder.imageView.setImageBitmap(cachedInt);
					}
				}
	        }
		}

		public final class ViewHolder {
			ImageView imageView;
			TextView title;
			TextView time;
			ImageView playView;
		}
	}

	private void quickAutoQuit() {
		handler.removeMessages(MSG_QUICK_HIDE);
		Message msg = handler.obtainMessage(MSG_QUICK_HIDE);
		handler.sendMessageDelayed(msg, quick_timeout);
	}

	private void quickMsgAutoQuit() {
		handler.removeMessages(MSG_QUICKMSG_HIDE);
		Message msg = handler.obtainMessage(MSG_QUICKMSG_HIDE);
		handler.sendMessageDelayed(msg, 1000);
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

	private void firstShowInList() {
		if (listItems == null) {
			listItems = map.getFileList();
		}
		listItems.clear();
		if (simpleAdapter == null) {
			simpleAdapter = new FileListAdapter(this, listItems, audiolist);
			audiolist.setAdapter(simpleAdapter);
		} else {
			simpleAdapter.notifyDataSetChanged();
		}
		if(dirLevel != -1 && folderPath.size() != 0)
			topPath.setText(devicePath + folderPath.get(dirLevel));
	}

	public void onStartLoadingList() {
		if (loadingIcon != null && loadingIcon.getAnimation() != null) {
			loadingIcon.getAnimation().reset();
			loadingIcon.getAnimation().startNow();
			loadingIcon.setVisibility(View.VISIBLE);
			loadingIcon.setImageResource(R.drawable.others_icons_loading);
		}
	}

	private void backRefresh(boolean isFocused, int currentPos,boolean reset) {
		
		dismissLoading();
		simpleAdapter.clearSelected();
		if(reset)
			backShowInList();
		else
			showInList();
		if(listItems.size() == 0){
			return;
		}
		if (!isFocused)
			audiolist.requestFocus();
	}
	
	private void sort(int type,int sortMode){
		if(type == 0)
			this.sortMode = sortMode;
		else
			this.dateSortMode = sortMode;
		sortByMode(type);
		simpleAdapter.notifyDataSetChanged();
	}
	
	private void sortByMode(int type){
		boolean updatePlaylist = false;
		ArrayList<DLNAFileInfo> playlistItems = map.getPlayList();
		if (playlistItems != null
				&& playlistItems.size() > 0
				&& playlistItems.size() == listItems.size()
				&& listItems.get(0).getFilePath()
						.equals(playlistItems.get(0).getFilePath()))
			updatePlaylist = true;
		if(type == 0)
			Collections.sort(listItems, new DLNAFileInfoComparator(type,sortMode));
		else
			Collections.sort(listItems, new DLNAFileInfoComparator(type,dateSortMode));
		if (updatePlaylist)
			updatePlaylist();
	}

	private VerticalBar progress;
	private void initSeekProgressBar(){
		progress.setSize(32, 828);
		try { 
			Field f = AbsListView.class.getDeclaredField("mFastScroller"); 
			f.setAccessible(true); 
			Object o=f.get(audiolist); 
			f=f.getType().getDeclaredField("mThumbDrawable"); 
			f.setAccessible(true); 
			Drawable drawable=(Drawable) f.get(o); 
			drawable=getResources().getDrawable(R.drawable.thumb); 
			f.set(o,drawable); 
			} catch (Exception e) { 
			throw new RuntimeException(e); 
			}
		try { 
			Field f = AbsListView.class.getDeclaredField("mFastScroller"); 
			f.setAccessible(true); 
			Object o=f.get(audiolist); 
			f=f.getType().getDeclaredField("mThumbH"); 
			f.setAccessible(true); 
			Integer value=(Integer) f.get(o); 
			value=Integer.valueOf(bmp.getHeight()); 
			f.set(o,value); 
			} catch (Exception e) { 
			throw new RuntimeException(e); 
			}
		
		try { 
			Field f = AbsListView.class.getDeclaredField("mFastScroller"); 
			f.setAccessible(true); 
			Object o=f.get(audiolist); 
			f=f.getType().getDeclaredField("mThumbW"); 
			f.setAccessible(true); 
			Integer value=(Integer) f.get(o); 
			value=Integer.valueOf(bmp.getWidth()); 
			f.set(o,value); 
			} catch (Exception e) { 
			throw new RuntimeException(e); 
			}
		audiolist.setOnScrollListener(onScrollListener);
	}
	  private void getbmp(){
		  BitmapFactory.Options options = new BitmapFactory.Options();   
	      options.inJustDecodeBounds = true;
	      bmp = BitmapFactory.decodeResource(getResources(), R.drawable.thumb);
	  }	  
	  private Bitmap bmp;	  
	  AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {  
	      @Override  
	      public void onScrollStateChanged(AbsListView absListView, int scrollState) {  
	          if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {      //  

	          }  
	      }  
	      @Override  
	      public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {  
	      	progress.setProgress(firstVisibleItem + pagesize);
	      }  
	  };
	  
		protected BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				 if (action.equals(CMD_MSG_RESET)){
					int playindex = intent.getIntExtra("index", 0);
					Message msg = handler.obtainMessage(MSG_PLAYING);
					Bundle bun = new Bundle();
					bun.putInt("playindex", playindex);
					msg.setData(bun);
					handler.sendMessage(msg);
				}else if (action.equals(CMD_MSG_CLOSE)){
					map.exit();
				}
			}};
			private final String CMD_MSG_RESET = "CMD_MSG_RESET";
			private final String CMD_MSG_CLOSE = "CMD_MSG_CLOSE";
			private void initServiceMsg(){
				IntentFilter filter = new IntentFilter();
				filter.addAction (CMD_MSG_RESET );
				filter.addAction (CMD_MSG_CLOSE );
				registerReceiver(serviceReceiver, filter);
			}
			
			 class QuickMenuAdapter extends BaseAdapter 
			 {
				 	int[] menu_name = new int[] {
							R.string.quick_menu_help,
							R.string.quick_menu_exit
							};
				 	int[] visibility = new int[]{
				 			View.INVISIBLE,
				 			View.INVISIBLE,
				 			
				 	};
					
					private LayoutInflater mInflater;
					
					class ViewHolder {
						TextView menu_name;
						ImageView left;
						TextView menu_option;
						ImageView right;
					}
					
					public QuickMenuAdapter(Context context)
			    	{
						mInflater = LayoutInflater.from(context);
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
			        	 ViewHolder holder;
			        	
			        	if (convertView == null) {
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
				            case 0:
				            {
				            	//holder.menu_option.setText("1.0");
				            	break;
				            }
				            case 1:
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
		private int mSleepTimeHour = 0, mSleepTimeMin = 0;
		private ContentResolver m_ContentMgr = null;
		private int mActivityPauseFlag = 0;
		private void initMenu(){
	        // createQuickMenu()
	        quickmenuAdapter = new QuickMenuAdapter(this);
	        m_ContentMgr = getApplicationContext().getContentResolver();
	        quickmenu=new QuickMenu(
					this, quickmenuAdapter);
	        quickmenu.setSize(0, 125);
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
						case 0 : {
							ComponentName componetName = new ComponentName("com.android.emanualreader",
									"com.android.emanualreader.MainActivity");
							Intent intent = new Intent();
							intent.setComponent(componetName);
							startActivity(intent);
						} break;
						case 1:
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
				if(statusHeight <=0)
					getStatusHeight();
				quickmenu.showAtRTop(40,90+statusHeight,-1);
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
		
		private class PopupMessage extends PopupWindow{
			
			private Context context;
			private RelativeLayout rp = null;
			public TextView message = null;
			
			LayoutInflater mInflater=null;
			
			PopupMessage(Context mContext)
			{
				super(mContext);				
				this.context=mContext;
				
				mInflater = LayoutInflater.from(context);
			    rp=(RelativeLayout) mInflater.inflate(R.layout.message, null);
			    
			    message = (TextView)rp.findViewById(R.id.msg);
			    
			    setContentView(rp);	
			}
			
			public void setMessage(String s)
			{
				message.setText(s);
			}
			
			
			public void setMessageLeft()
			{
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) message.getLayoutParams();
				lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
				lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				message.setGravity(Gravity.LEFT);
				message.setLayoutParams(lp);
			}		
			public void show()//40,850
			{
				 if (this != null){
					 this.dismiss();
				 }
				
				rp.setBackgroundResource(R.drawable.message_box_bg);
				
				setMessageLeft();
				
				int height = 260;
				int width = 678;
				setHeight(height);
				setWidth(width);
				
				this.setFocusable(false);
				this.setOutsideTouchable(true);
				this.showAtLocation(rp, Gravity.CENTER, 0, 0);
			}
			
			public void show(int resid, int height, int width, int gravity, int x, int y)
			{
				this.dismiss();
				
				rp.setBackgroundResource(resid);
				setHeight(height);
				setWidth(width);
				
				this.setFocusable(false);
				this.setOutsideTouchable(true);
				this.showAtLocation(rp, gravity, x, y);
			}
			
		}
		
		public class StackContent{
			private ArrayList<DLNAFileInfo> data;
			private int dirNum;
			public StackContent(ArrayList<DLNAFileInfo> data,int num){
				this.data = data;
			}
			public int getDirNum(){
				return dirNum;
			}
			public ArrayList<DLNAFileInfo> getData(){
				return data;
			}
		}

		@Override
	    public synchronized void update(Observable o, Object arg) {
			Log.e("audiobrowser", "update");
			ObserverContent content = (ObserverContent)arg;
			String serverName = content.getMsg();
			String act = content.getAction();
			if(act.equals(ObserverContent.REMOVE_DEVICE)) {
				Log.e("DLNADevice", "audioBrowser "+" removed server name: " + serverName);
				if(map.getMediaServerName().equals(serverName))
					handler.sendEmptyMessage(MSG_DEVICE_REMOVED);

			}else if(act.equals(ObserverContent.EXIT_APP)){
				finish();
			}
		}
		@Override
		public void onConfigurationChanged(Configuration newConfig) {
			// TODO Auto-generated method stub
			super.onConfigurationChanged(newConfig);
			orientation = getResources().getConfiguration().orientation;//LANDSCAPE or PORTRAIT
			initVariable();
			setContentView(R.layout.music);
			initLoading();

			audiolist = (GridView) findViewById(R.id.audio_list);
			topPath = (TextView) findViewById(R.id.music_path_top);
			initLayout();
			audiolist.setOnItemSelectedListener(itemSelectedListener);
			audiolist.setOnItemClickListener(itemClickListener);
			audiolist.setAdapter(simpleAdapter);
			simpleAdapter.notifyDataSetChanged();
			//initMenu();
		}
		
		private int statusHeight =0;
		private void getStatusHeight(){
			statusHeight = getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
		}
}
