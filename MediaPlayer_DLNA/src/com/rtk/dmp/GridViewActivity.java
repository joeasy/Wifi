package com.rtk.dmp;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.DLNAFileInfoComparator;
import com.realtek.Utils.DensityUtil;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.Fragment;
import android.app.TvManager;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextPaint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupWindow.OnDismissListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;










import com.rtk.dmp.GridViewFragment.UiListener;
import com.rtk.dmp.MediaApplication;
public class GridViewActivity extends FragmentActivity implements UiListener,Observer {
	private MediaApplication mMediaApplicationMap = null;
	private Resources m_ResourceMgr = null;
	private XmlResourceParser m_MimeTypeXml;

	private GridViewFragment m_GridViewFragment = null;

	// Top mediaPathInfo
	private TextView m_tvTopMediaPathInfoTextView = null;
	private String m_sRootPath = null;
	private Path_Info m_PathInfo = null;
	private ArrayList<String> folderPath = new ArrayList<String>();
	private int dirLevel = -1;
	private ArrayList<String> parentPath = new ArrayList<String>();

	private static final String TAG = "GridViewActivity";

	private ArrayList<String> DMSName = new ArrayList<String>();
	private ConfirmMessage short_msg = null;
	public Activity mContext = GridViewActivity.this;
	private int ResultCodeRestartGridViewActivity = 10;
	Handler refreshHandler = null;
	private final int MSG_STARTLOADING_REFRESH = 200;
	private final int MSG_BACK_REFRESH = 15;
	private final int MSG_NORMAL_REFRESH = 16;
	private final int MSG_REFRESH = 17;
	private final int MSG_FIRST_REFRESH = 18;
	volatile boolean isKeyBack = false;

	private PopupWindow mPop = null;

	
	private ImageView btn_menu = null;
	private QuickMenu mQuickMenu = null;
	private QuickMenuPhotoAdapter mQuickmenuAdapter = null;
	private int mRepeatIndex = 0;
	private int mIntervalIndex = 1;
	private int mSleepTime = 0;
	private int mSleepTimeHour = 0, mSleepTimeMin = 0;

	private String[] mIntervalTimeStatus = new String[3];

	private String[] repeats = new String[2];

	private SharedPreferences mPerferences = null;

	private final int MSG_SET_REPEAT = 19;
	private final int MSG_SET_INTERVAL = 20;
	// loading icon
	static Handler loading_icon_system_handle = null;
	Animation mAnimLoading = null;
	ImageView mTopLoadingIcon = null;

	// banner
	private TextView banner_enter = null;

	private PopupMessage msg_hint = null;
	private PopupMessage msg_hint_noFile = null;
	
	private Handler mCheckTimerHandler = null;	

	private Message_not_avaible msg_notavaible = null;
	long mLastNotAvailableShowTime = 0;
	long mLastDeviceRemovedShowTime = 0;

	private int mActivityPauseFlag = 0;
	
	
	ArrayList<DLNAFileInfo> listItems = null;
	private Stack<ArrayList<DLNAFileInfo>> listStack;
	private List<String> mCharIDList = new ArrayList<String>();
	
	
	private View sortUp = null;
	private View sortDown = null;
	private ImageView sortUpBg = null;
	private ImageView sortDownBg = null;
	private int sortMode = 0;
	private int sortModeByDate = 0;
	
	private int [] vAddrForDTCP = null;
	private TvManager mTv  = null;
	
	private View mainView = null;
	private GestureDetector  gestureScanner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		
		m_ResourceMgr = this.getResources();

		mIntervalTimeStatus[0] = (String) m_ResourceMgr.getText(R.string.qm_interval_fast);
		mIntervalTimeStatus[1] = (String) m_ResourceMgr.getText(R.string.qm_interval_normal);
		mIntervalTimeStatus[2] = (String) m_ResourceMgr.getText(R.string.qm_interval_slow);

		repeats[0] = (String) m_ResourceMgr.getText(R.string.qm_repeat_off);
		repeats[1] = (String) m_ResourceMgr.getText(R.string.qm_repeat_on);

		mPerferences = PreferenceManager.getDefaultSharedPreferences(this);

		mMediaApplicationMap = (MediaApplication) getApplication();

		if (m_PathInfo != null) {
			m_PathInfo.cleanLevelInfo();
		} else {
			m_PathInfo = new Path_Info();
		}
		

		// GetMimeType
		m_MimeTypeXml = m_ResourceMgr.getXml(R.xml.mimetypes);

		Log.d("FileFilter", "mMimeTypeXml:" + m_MimeTypeXml.toString());


		if (true == RTKDMPConfig.getRight2Left(getApplicationContext()))
			setContentView(R.layout.activity_grid_view_a);
		else
			setContentView(R.layout.activity_grid_view);
		
		listStack = new Stack<ArrayList<DLNAFileInfo>>();

		m_GridViewFragment = (GridViewFragment) (getSupportFragmentManager()
				.findFragmentById(R.id.gridview_fragment));
		banner_enter = (TextView) findViewById(R.id.guide_enter);

		init();
		
		refreshHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_NORMAL_REFRESH: {
					normalShowInList();
					break;
				}

				case MSG_REFRESH: {
					showInList();
					break;
				}
				case MSG_FIRST_REFRESH: {
					firstsShowInList();
					break;
				}
				case MSG_BACK_REFRESH: {
					backShowInList();
					break;
				}
				case MSG_STARTLOADING_REFRESH:{
					onStartLoadingList();
					break;
				}
				default:
					break;

				}
				super.handleMessage(msg);
			}

		};
		
		if (listItems == null) {
			listItems = mMediaApplicationMap.getFileList();
			listItems.clear();
		}
		m_sRootPath = mMediaApplicationMap.getSubRootPath();
		if(!m_sRootPath.endsWith("/")){
			m_sRootPath += "/";
		}
		if(savedInstanceState!=null)
		{
			String[] pathInfo_paths = savedInstanceState.getStringArray("pathInfo_paths");
			String[] charIds = savedInstanceState.getStringArray("charIds");
			String[] folders = savedInstanceState.getStringArray("folders");
			
			for(int i = 0;i<pathInfo_paths.length;i++)
			{
				m_PathInfo.addLevelInfo(pathInfo_paths[i]);
			}

			for(int i = 0;i<charIds.length;i++)
			{
				mCharIDList.add(charIds[i]);
			}
			for(int i = 0;i<folders.length;i++)
			{
				folderPath.add(folders[i]);
			}
			firstShowInList();
			onStartLoadingList();
			isKeyBack = false;
			new Thread(new Runnable() {
				public void run() {
					String tmpPath = m_PathInfo.getLastLevelPath();
					getFileList(m_PathInfo.getLastLevelPath(), mCharIDList.get(mCharIDList.size()-1));
					if (!isKeyBack
							&& tmpPath.equals(m_PathInfo.getLastLevelPath())) {
						refreshHandler.sendEmptyMessage(MSG_REFRESH);
					}
				}
			}).start();
		}
		else
		{
			m_PathInfo.addLevelInfo(m_sRootPath);
		// setGridView
			new Thread(new Runnable() {
				public void run() {
					refreshHandler.sendEmptyMessage(MSG_STARTLOADING_REFRESH);
					String tmpPath = m_sRootPath;
					getFileList(m_sRootPath,null);
					if (!isKeyBack
							&& tmpPath.equals(m_PathInfo.getLastLevelPath())) {
						refreshHandler.sendEmptyMessage(MSG_FIRST_REFRESH);
					}
				}
			}).start();
		}
		
		short_msg = new ConfirmMessage(mContext, 678, 226);

		mCheckTimerHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					if (msg_notavaible.isShowing())
						msg_notavaible.dismiss();
					break;
				case 1:
					if (msg_hint.isShowing())
						msg_hint.dismiss();
					break;

				default:
					break;
				}
				super.handleMessage(msg);
			}
		};

		// createQuickMenu();
		mQuickmenuAdapter = new QuickMenuPhotoAdapter(this);
		mQuickMenu = new QuickMenu(this, mQuickmenuAdapter);
		mQuickMenu.setAnimationStyle(R.style.QuickAnimation);

		new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					if (m_PathInfo.getLastLevelPath() == m_sRootPath) {
						firstShowInList();
						onStartLoadingList();
						isKeyBack = false;
						new Thread(new Runnable() {
							public void run() {
								String tmpPath = m_sRootPath;
								getFileList(m_sRootPath,null);
								if (!isKeyBack
										&& tmpPath.equals(m_PathInfo
												.getLastLevelPath())) {
									refreshHandler
											.sendEmptyMessage(MSG_NORMAL_REFRESH);
								}
							}
						}).start();
					}
					break;
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
				mQuickMenu.markOperation();
				switch (position) {
					case 0: {
						mIntervalIndex++;
						mIntervalIndex %= 3;
	
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
	
						break;
					}
			
					case 1:
					{
						ComponentName componetName = new ComponentName("com.android.emanualreader",
								"com.android.emanualreader.MainActivity");
						mQuickMenu.dismiss();					
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
        
        OnKeyListener quickmenuKeyClickListener = new OnKeyListener(){

        	ListView quickMenuContent = mQuickMenu.getListView();
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				mQuickMenu.markOperation();
				if (event.getAction() == KeyEvent.ACTION_DOWN)
	        	{
					int position = quickMenuContent.getSelectedItemPosition();
		        	if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
		        	{
		        		// TODO: switch option
		        		switch(position)
		        		{
		        			case 0:
		        			{								
								mIntervalIndex++;
								mIntervalIndex %= 3;
								
								TextView OptionText = (TextView)(quickMenuContent.getChildAt(position).findViewById(R.id.menu_option));
								OptionText.setText(mIntervalTimeStatus[mIntervalIndex]);
								new Thread(new Runnable() {
									@Override
									public void run() {
										Editor editor = mPerferences.edit();
										editor.putInt("intervalIndex_photo", mIntervalIndex);
										editor.commit();
									}
								}).start();
								
								break;
		        			}
		        		}
		        		
		        		return true;
		        	}
		        	else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
		        	{
		        		// TODO: switch option
		        		switch(position)
		        		{
		        			case 0:
		        			{								
								mIntervalIndex = mIntervalIndex-1<0?2:mIntervalIndex-1;
								
								TextView OptionText = (TextView)(quickMenuContent.getChildAt(position).findViewById(R.id.menu_option));
								OptionText.setText(mIntervalTimeStatus[mIntervalIndex]);
								new Thread(new Runnable() {
									@Override
									public void run() {
										Editor editor = mPerferences.edit();
										editor.putInt("intervalIndex_photo", mIntervalIndex);
										editor.commit();
									}
								}).start();
								
								break;
		        			}
		        		}
		        		return true;
		        	}
		        	else if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
		        	{
		        		if(position == 0)
		        		{
		        			quickMenuContent.setSelection(quickMenuContent.getCount()-1);
		        		}
		        		return false;		
		        	}
		        	else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
		        	{
		        		if(position == quickMenuContent.getCount()-1)
		        		{
		        			quickMenuContent.setSelection(0);
		        		}
		        		return false;
		        	}
		        	
		        	
	        	}
				else if (event.getAction() == KeyEvent.ACTION_UP)
				{
					if (keyCode == KeyEvent.KEYCODE_Q || keyCode == 227 //227 presents KEYCODE_QUICK_MENU 
								 || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)

		        	{
		        		mQuickMenu.dismiss();
		        	}
					else if(keyCode ==  KeyEvent.KEYCODE_MENU || keyCode == 220)
					{
						mQuickMenu.dismiss();
						if(null == msg_notavaible)
						{
							msg_notavaible = new Message_not_avaible(GridViewActivity.this);
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
            	    				if(curtime - mLastNotAvailableShowTime > 3000)
            		    			{
            		    				Message msg = new Message();
            		    				msg.what = 0;
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
					
					return false;
				}
				return false;
			}
		};
		
		OnItemSelectedListener quickmenuItemSelectedListener = new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long arg3) 
			{
				// TODO Auto-generated method stub
				mQuickMenu.markOperation();
				if(mQuickmenuAdapter.LastSelectedItem_View == null)
					mQuickmenuAdapter.LastSelectedItem_View = view;
				ImageView left_arrow = (ImageView)mQuickmenuAdapter.LastSelectedItem_View.findViewById(R.id.left_arrow);
				ImageView right_arrow = (ImageView)mQuickmenuAdapter.LastSelectedItem_View.findViewById(R.id.right_arrow);
				left_arrow.setVisibility(View.INVISIBLE);
				right_arrow.setVisibility(View.INVISIBLE);
				mQuickmenuAdapter.LastSelectedItem_View = view;
				if(position == 0 )
				{
					left_arrow = (ImageView)mQuickmenuAdapter.LastSelectedItem_View.findViewById(R.id.left_arrow);
					right_arrow = (ImageView)mQuickmenuAdapter.LastSelectedItem_View.findViewById(R.id.right_arrow);
					left_arrow.setVisibility(View.VISIBLE);		
					right_arrow.setVisibility(View.VISIBLE);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) 
			{
				// TODO Auto-generated method stub
				Log.d(TAG, "Quick Menu ListView onNothingSelected");
			}
		};
		mQuickMenu.AddOnItemClickListener(quickmenuItemClickListener);
		mQuickMenu.AddOnItemSelectedListener(quickmenuItemSelectedListener);
		mQuickMenu.AddOnKeyClickListener(quickmenuKeyClickListener);
  
        msg_hint = new PopupMessage(this);
        msg_hint_noFile = new PopupMessage(this);
        //onItemSelected(0);
        if (m_GridViewFragment.getGridView().isInTouchMode()) {
			Log.d(TAG, "In touch mode");
		//	m_GridViewFragment.getGridView().exitTouchMode();	
		}
        
		sortUp     = (View)findViewById(R.id.lay_up);
		sortUpBg   = (ImageView)findViewById(R.id.sortup_bg);
		sortDown   = (View)findViewById(R.id.lay_down);
		sortDownBg = (ImageView)findViewById(R.id.sortdown_bg);
		sortUp.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				sortMode = 0;
				switch(sortModeByDate) {
					case 0:
						sortModeByDate = -1;
						break;
					case 1:
						sortModeByDate = -1;
						break;
					case -1:
						sortModeByDate = 1;
						break;
				}
				Collections.sort(listItems, new DLNAFileInfoComparatorByDate(sortModeByDate));
				m_GridViewFragment.RefreshGridView(0);
				sortUpBg.setImageResource(R.drawable.dnla_sorting_base_up);
				sortDownBg.setImageResource(R.drawable.dnla_sorting_base_down);
				
			}
			
		});
		sortDown.setOnClickListener(new OnClickListener(){	// by name

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				sortModeByDate = 0;
				switch(sortMode) {
					case 0:
						sortMode = -1;
						break;
					case 1:
						sortMode = -1;
						break;
					case -1:
						sortMode = 1;
						break;
				}
				Collections.sort(listItems, new DLNAFileInfoComparatorByName(sortMode));
				m_GridViewFragment.RefreshGridView(0);
				sortUpBg.setImageResource(R.drawable.dnla_sorting_base_down);
				sortDownBg.setImageResource(R.drawable.dnla_sorting_base_up);

			}
			
		});
		btn_menu = (ImageView) findViewById(R.id.btn_menu);
		btn_menu.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(mQuickMenu.isShowing() == true)
					mQuickMenu.dismiss();
				else
				{
					mQuickMenu.showAtRTop(DensityUtil.dip2px(getApplicationContext(), 14),
							DensityUtil.dip2px(getApplicationContext(), 72),120);
					mQuickMenu.setTimeout();
				}
			}
			
		});
    }
	//ellipsize="end"  "."-->"..."
	public static String dealDetailString(TextView v ,String content,float show_len){
		TextPaint tpaint =v.getPaint();
		//tpaint.setTextSize(21);
		String temp="";
		if(content!=null){
		temp=content.replaceAll("\n", " ").replaceAll("\b", " ");
		}
		String str_content=(content==null?"":temp);
		float len=0;
		int s_len=0;
		if(str_content!=null&&str_content!=""){
		 len = tpaint.measureText(str_content);
		 s_len=str_content.length();
		 }
		int i=0;
		for(;i<s_len&&len>show_len;i++){
		 str_content=str_content.substring(0, str_content.length()-1);
		 len = tpaint.measureText(str_content);
		}
		if(i>0){
		return str_content+"...";
		}else{
		   return content;
		}
	}
	
/*	private int getSleepTimeValue()
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
	*/
	private void PopupMessageShow(String msg, int resid, int height, int width, int gravity, int x, int y, final int dismiss_time)
	{
		if(msg_hint.isShowing() == true)
		{
			msg_hint.dismiss();
		}
		msg_hint.setMessage(msg);
		msg_hint.show(resid, height, width, gravity, x, y);
		
		mLastDeviceRemovedShowTime = (new Date(System.currentTimeMillis())).getTime();
		new Thread(new Runnable() {
    		public void run() {
    			long curtime = 0;
    			while(true)
    			{
    				if(msg_hint.isShowing() == false)
    					break;
    				curtime = (new Date(System.currentTimeMillis())).getTime();
    				if(curtime - mLastDeviceRemovedShowTime > dismiss_time)
	    			{
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.activity_grid_view, menu);
		return true;
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		mActivityPauseFlag = 1;
		if (mQuickMenu.isShowing()) {
			mQuickMenu.setIsActivityPause(mActivityPauseFlag);
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		if (mPop != null && mPop.isShowing()) {
			mPop.dismiss();
		}

		super.onDestroy();
		mMediaApplicationMap.deleteObserver(this);
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		mActivityPauseFlag = 0;
		if (mQuickMenu.isShowing()) {
			mQuickMenu.setIsActivityPause(mActivityPauseFlag);
			mQuickMenu.setTimeout();
		}
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
				
				if(tmp_index_interval == -1)
				{
					editor.putInt("intervalIndex_photo", mIntervalIndex);
					editor.commit();
				}else{
					mIntervalIndex = tmp_index_interval;
				}
				
				int mins = 0;//getSleepTimeValue();
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
					int mins = 0;//getSleepTimeValue();
					mSleepTimeHour = mins / 60;
					mSleepTimeMin = mins % 60;
					
					if(mQuickMenu.isShowing())
					{
			//			quickUIhandler.sendEmptyMessage(GridViewActivity.MSG_REFRESH_TIMER);
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
        super.onResume();
    
    }
 
    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
    	if(msg_hint.isShowing())
    		msg_hint.dismiss();
    	if(event.getAction() == KeyEvent.ACTION_UP)
    	{
    		Log.d(TAG, event.toString()+"Action up.");
    		switch(event.getKeyCode())
	    	{
		    	case KeyEvent.KEYCODE_Q:
				case 227:    //227 present KEYCODE_QUICK_MENU
				{
					if(mQuickMenu.isShowing() == true)
						mQuickMenu.dismiss();
					else
					{
						mQuickMenu.showQuickMenu(14,14);
						mQuickMenu.setTimeout();
					}
					super.dispatchKeyEvent(event);
					return true;
				}
				case 82://MENU
				case 220://STEREO/DUAL for L4300
				{
					if(null == msg_notavaible)
					{
						msg_notavaible = new Message_not_avaible(GridViewActivity.this);
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
        		    				msg.what = 0;
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
					super.dispatchKeyEvent(event);
					return true;
				}
				
				default:
				    	break;
	    	}
    		return super.dispatchKeyEvent(event);
    	}
    	else if(event.getAction() == KeyEvent.ACTION_DOWN)
    	{
    		Log.d(TAG, event.toString()+"Action down.");
	    	switch(event.getKeyCode())
	    	{
				case KeyEvent.KEYCODE_E:
				case KeyEvent.KEYCODE_ESCAPE:
				{
					try {
						ComponentName componetName = new ComponentName(
								"com.rtk.mediabrowser",// another apk name
								"com.rtk.mediabrowser.MediaBrowser" // another apk
																	// activity name
						);
						Intent intent = new Intent();
						Bundle bundle = new Bundle();
						intent.putExtras(bundle);
						intent.setComponent(componetName);
						startActivity(intent);
					} catch (Exception e) {
					}
					finish();
					return true;
				}
				case KeyEvent.KEYCODE_BACK:
				{
					onBackClicked();
					return false;
				}			
	    	}
    	}
    	
    	return super.dispatchKeyEvent(event);
    }
    
    
     
    private void init()
    {
        m_tvTopMediaPathInfoTextView = (TextView)findViewById(R.id.topMeidiaPathInfo);

        

    	mTopLoadingIcon=(ImageView)findViewById(R.id.topLoadingIcon);
        
    	mTopLoadingIcon.setVisibility(View.INVISIBLE);
        mAnimLoading = AnimationUtils.loadAnimation(this, R.drawable.anim);
        mTopLoadingIcon.setAnimation(mAnimLoading);
        mTopLoadingIcon.getAnimation().cancel();
        mTopLoadingIcon.setImageResource(R.drawable.blank);
         
        loading_icon_system_handle = new Handler()
        {
        	private int loading_num = 0;
        	boolean hasAnimStarted=false;
        	private synchronized void setLoadingThreadNum(int flag)
        	{
        		if(flag == 0)
        		{
	          		  loading_num++;   
        		}
        		else if(flag == 1)
        		{
	          		  loading_num--;     
        		}
        	}
        	private synchronized int getLoadingThreadNum()
        	{
        		return loading_num;
        	}
        	
        	@Override  
            public void handleMessage(Message msg)  
            {        		
            	switch (msg.what)  
                {  
                  case 0:
                  {
                	  setLoadingThreadNum(msg.what);
                	  break;  
                  }
                  case 1:
                  {
                	  setLoadingThreadNum(msg.what);
                	  break;
                  }
                 default:
                	 break;
                }  
            	if(getLoadingThreadNum() > 0)
            	{           	
            		if(hasAnimStarted == false && loading_num == 1)
            		{           	
            			mTopLoadingIcon.getAnimation().reset();
            			mTopLoadingIcon.getAnimation().startNow();
            			mTopLoadingIcon.setVisibility(View.VISIBLE);
            			hasAnimStarted=true;
            		}            		
            		mTopLoadingIcon.setImageResource(R.drawable.others_icons_loading);
            	}
            	else if(getLoadingThreadNum() == 0)
            	{	
            		if(mTopLoadingIcon == null || mTopLoadingIcon.getAnimation() == null)
            		{
            			Log.e(TAG, "Loading Icon is null.");
            			return;
            		}
            		mTopLoadingIcon.getAnimation().cancel();
            		hasAnimStarted=false;           	
            		mTopLoadingIcon.setVisibility(View.INVISIBLE);
            		mTopLoadingIcon.setImageResource(R.drawable.blank);
            	}
            	else
            		Log.e(TAG,"loading icon error");
              super.handleMessage(msg);  
            }         	
        };      
    }

	private class Message_not_avaible extends PopupWindow
	{
		private Activity context;
		private RelativeLayout rp = null;
		public TextView message = null;
		
		LayoutInflater mInflater=null;
		
		Message_not_avaible(Activity mContext)
		{
			super(mContext);					
			
			this.context=mContext;
			
			mInflater = LayoutInflater.from(context);
		    rp=(RelativeLayout) mInflater.inflate(R.layout.message_not_available, null);
		    message = (TextView)rp.findViewById(R.id.not_available);    
		    setContentView(rp);	
		}
		
		public void show_msg_notavailable()
		{
			TextPaint paint = message.getPaint(); 
			int len = (int) paint.measureText((String) m_ResourceMgr.getText(R.string.toast_not_available))+102;
			message.setText(m_ResourceMgr.getText(R.string.toast_not_available));
			setHeight(72);
			setWidth(len);
			message.setTextColor(Color.BLACK);
			this.setFocusable(true);
			this.setOutsideTouchable(true);
			this.showAtLocation(rp, Gravity.LEFT| Gravity.BOTTOM, 18, 18);
			
		}	
	}

 private void setTopMediaPathInfoTextView()
    {
    	int dirLevel = m_PathInfo.getLastLevel();
    	System.out.println("dirLevel :"+dirLevel);
    	System.out.println("folderPath :"+folderPath.size());
    	
    	if(dirLevel <= 0)
    	{
        	m_tvTopMediaPathInfoTextView.setVisibility(View.INVISIBLE);
        	return;
    	}
    	else
    	{
    		m_tvTopMediaPathInfoTextView.setVisibility(View.VISIBLE);
    	}
    	
        if(m_tvTopMediaPathInfoTextView != null && m_PathInfo.getLastLevelPath() != null)
        {
        	
        	String pathTag ="";
			for(int i = 1;i<=dirLevel-1;i++)
				pathTag +="/";
        	
			if(false == RTKDMPConfig.getRight2Left(getApplicationContext()))
			{
				m_tvTopMediaPathInfoTextView.setText("/"+pathTag+folderPath.get(dirLevel-1));
			}
			else
			{
				m_tvTopMediaPathInfoTextView.setText(folderPath.get(dirLevel-1)+pathTag+"/");
			}          
        }           
    }
      

	public ArrayList<DLNAFileInfo> getCurrentItemList()
	{
		if(listItems == null)
		{
			System.out.println("public ArrayList<DLNAFileInfo> getCurrentItemList() listItems == null?????");
		}
		return listItems;
	}
	public void onItemSelected(int position) {
		/*if (position != -1) {
			String tmpPath = listItems.get(position).getFilePath();
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
							DMSName.clear();
							if (m_PathInfo != null) {
								m_PathInfo.cleanLevelInfo();
							}
							m_PathInfo.addLevelInfo(m_sRootPath);
							firstShowInList();
							onStartLoadingList();
							isKeyBack = false;
							new Thread(new Runnable() {
								public void run() {
									String tmpPath = m_sRootPath;
									getFileList(m_sRootPath,null);
									if (!isKeyBack
											&& tmpPath.equals(m_PathInfo
													.getLastLevelPath())) {
										refreshHandler
												.sendEmptyMessage(MSG_NORMAL_REFRESH);
									}
								}
							}).start();
						}
					});

					short_msg.show();
					return;
				}
			}
		}

		m_PathInfo.setLastLevelFocus(position);*/

	}

	private void setBottomBanner(int pos) {
		if (listItems.get(pos).getFileType() == FileFilterType.DEVICE_FILE_PHOTO) {
			banner_enter.setText((String) m_ResourceMgr
					.getText(R.string.guide_select));
		}
		else if((listItems.get(pos).getFileType()  == FileFilterType.DEVICE_FILE_DIR))
		{			banner_enter.setText((String) m_ResourceMgr
					.getText(R.string.guide_enter));
		}
    	
    }
    


	public static String getFileCreateDate(File _file) {
		File file = _file;
		Date last_date = new Date(file.lastModified());
		SimpleDateFormat df_des = new SimpleDateFormat(
				"HH:mm EEEE,dd MMMM yyyy");
		String last_modify_date = df_des.format(last_date);
		return last_modify_date;
	}

	public static String dateFormate(String date) {
		// TODO Auto-generated method stub
		SimpleDateFormat df_ori_exif = new SimpleDateFormat(
				"yyyy:MM:dd HH:mm:ss");
		SimpleDateFormat df_des = new SimpleDateFormat(
				"HH:mm EEEE,dd MMMM yyyy");
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

	public void onItemClicked(int position) {

		System.out.println("onItemClicked:+"+position);
		m_PathInfo.setLastLevelFocus(position);
		if (m_PathInfo.getLastLevelFocus() >= 0
				&& m_PathInfo.getLastLevelFocus() < listItems.size()) {
			Log.v(TAG, "focus: " + m_PathInfo.getLastLevelFocus());
			String tmpPath = listItems.get(position).getFilePath();
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
							DMSName.clear();
							if (m_PathInfo != null) {
								m_PathInfo.cleanLevelInfo();
							}
							m_PathInfo.addLevelInfo(m_sRootPath);
							folderPath.clear();
							firstShowInList();
							onStartLoadingList();
							isKeyBack = false;
							new Thread(new Runnable() {
								public void run() {
									String tmpPath = m_sRootPath;
									getFileList(m_sRootPath,null);
									if (!isKeyBack
											&& tmpPath.equals(m_PathInfo
													.getLastLevelPath())) {
										refreshHandler
												.sendEmptyMessage(MSG_NORMAL_REFRESH);
									}
								}
							}).start();
						}
					});

					short_msg.show();
					return;
				}
			}
			if (listItems.get(position).getFileType() == FileFilterType.DEVICE_FILE_PHOTO) {
				ComponentName componetName = new ComponentName(
						"com.rtk.dmp",
						"com.rtk.dmp.PhotoPlayerActivityPause");
				Intent intent = new Intent();
				Bundle bundle = new Bundle();
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				
				int curPos = 0;
				int totalCnt = 0;
				int tmpsize = listItems.size();
				for (int i = 0; i < tmpsize; i++)
				{
					if (listItems.get(position).getFileType() == FileFilterType.DEVICE_FILE_PHOTO)
					{
						if (listItems.get(position).getFileName().compareTo(
								listItems.get(i).getFileName()) == 0)
							curPos = totalCnt;
						totalCnt++;
					}
				}
				int dirsize = DLNADataProvider.getDirectorySize();
				Log.d(TAG, "onItemClicked curPos:" + curPos);
				Log.d(TAG, "onItemClicked totalCnt:" + totalCnt);
				Log.d(TAG, "listItems.size():"+listItems.size()+" dirsize:"+dirsize);

				String serverName = null;
				
				serverName = mMediaApplicationMap.getMediaServerName();
				
				System.out.println("serverName and m_sRootPath and  is "+serverName+"!!"+m_sRootPath);
				bundle.putString("serverName", serverName);
				bundle.putInt("mSleepTime", mSleepTime);

				bundle.putInt("initPos", position - dirsize);
				bundle.putInt("totalCnt", totalCnt);

				intent.putExtras(bundle);
				intent.setComponent(componetName);
				startActivityForResult(intent, 0);
			} 
			else if (listItems.get(position).getFileType() == FileFilterType.DEVICE_FILE_DIR) 
			{
				mCharIDList.add(listItems.get(position).getUniqueCharID());
				m_GridViewFragment.getGridViewAdapter().enterFirstTime();
				String pathTitle = listItems.get(position).getFileName();
				String header = m_PathInfo.getLastLevelPath();
				folderPath.add(pathTitle);

				m_PathInfo.addLevelInfo(header + pathTitle + "/");
				firstShowInList();
				onStartLoadingList();
				isKeyBack = false;
				new Thread(new Runnable() {
					public void run() {
						String tmpPath = m_PathInfo.getLastLevelPath();
						getFileList(m_PathInfo.getLastLevelPath(), mCharIDList.get(mCharIDList.size()-1));
						if (!isKeyBack
								&& tmpPath.equals(m_PathInfo.getLastLevelPath())) {
							refreshHandler.sendEmptyMessage(MSG_REFRESH);
						}
					}
				}).start();
			}
		} else {
			Log.v(TAG, "onItemClick error!!!");
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == 0)
		{
			if(resultCode == -10)//exit key
			{
				finish();
				return ;
			}
			else if(resultCode == -1)//
			{
				setResult(1);
				finish();
				return ;
			}
			else if(resultCode == -2)//plug out usb when photo full screen;
			{
				/*
				m_GridViewFragment.directionControl.BackToMultiView(resultCode,
						m_GridViewFragment.directionControl.getPositionCurrent_allitems(),
						m_GridViewFragment.directionControl.getPositionCurrent_pageitem());
						*/
				finish();
				return;
			} else if (resultCode == ResultCodeRestartGridViewActivity) {
				m_PathInfo.cleanLevelInfo();
				m_PathInfo.addLevelInfo(m_sRootPath);
				folderPath.clear();
				firstShowInList();
				onStartLoadingList();
				isKeyBack = false;
				new Thread(new Runnable() {
					public void run() {
						String tmpPath = m_sRootPath;
						getFileList(m_sRootPath,null);
						if (!isKeyBack
								&& tmpPath.equals(m_PathInfo.getLastLevelPath())) {
							refreshHandler.sendEmptyMessage(MSG_FIRST_REFRESH);
						}
					}
				}).start();
			} else {
				//back to MultiView
				System.out.println("back to MultiView!!back to MultiView");
			}
		}
	}

	public void onBackClicked() {
		System.out.println("onBackClicked start");
		isKeyBack = true;
		DLNA_DMP_1p5.DLNA_DMP_1p5_stopWatingResponse();

		if (m_PathInfo.getLastLevel() > 0) {
			m_PathInfo.backToLastLevel();
			if(mCharIDList.size() > 0)
			{
				mCharIDList.remove(mCharIDList.size()-1);
			}
			firstShowInList();
			onStartLoadingList();
			isKeyBack = false;
			new Thread(new Runnable() {
				public void run() {
					if(mCharIDList.size() > 0) {
						String tmpPath = m_PathInfo.getLastLevelPath();
						getFileList(m_PathInfo.getLastLevelPath(),mCharIDList.get(mCharIDList.size()-1));
						if (!isKeyBack
								&& tmpPath.equals(m_PathInfo.getLastLevelPath())) {
							refreshHandler.sendEmptyMessage(MSG_BACK_REFRESH);
						}
					}
				}
			}).start();
		} else {
			dismissLoading();
			this.finish();
		}
		// getSDInfo();

	}

	public boolean onKeyClicked(View view, int keyCode, KeyEvent event,
			int position, int iconNum, int firstVisibleItem, int lastVisibleItem) {
		return false;
	}

	public int getFocusIndex() {
		return m_PathInfo.getLastLevelFocus();
	}

	class QuickMenuPhotoAdapter extends BaseAdapter {
		public View LastSelectedItem_View = null;
		int[] menu_name = new int[] { 
				R.string.quick_menu_photo_intervalTime,
				R.string.quick_menu_help 
				};

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
				holder.menu_option.setText(mIntervalTimeStatus[mIntervalIndex]);
				break;
			}
			case 1: {
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

	// Added for GridViewFragment.GridViewLoadingControl.startLoading()
	public void startLoadingIcon(int pos) {
		Message msg = new Message();
		msg.what = 0;
		loading_icon_system_handle.sendMessage(msg);
	}

	// Added for GridViewFragment.GridViewLoadingControl.stopLoading()
	public void stopLoadingIcon(int pos) {
		Message msg = new Message();
		msg.what = 1;
		loading_icon_system_handle.sendMessage(msg);
	}

	private void firstShowInList() {
		listItems.clear();
		setTopMediaPathInfoTextView();

		m_GridViewFragment.RefreshGridView(0);
	}

	public void onStartLoadingList() {
		if (mTopLoadingIcon != null && mTopLoadingIcon.getAnimation() != null) {
			mTopLoadingIcon.getAnimation().reset();
			mTopLoadingIcon.getAnimation().startNow();
			mTopLoadingIcon.setVisibility(View.VISIBLE);
			mTopLoadingIcon.setImageResource(R.drawable.others_icons_loading);
		}
	}

	private void dismissLoading() {
		if (mTopLoadingIcon != null && mTopLoadingIcon.getAnimation() != null) {
			mTopLoadingIcon.getAnimation().cancel();
			mTopLoadingIcon.setVisibility(View.INVISIBLE);
			mTopLoadingIcon.setImageResource(R.drawable.blank);
		}
	}

	private void normalShowInList() {
		dismissLoading();
		m_tvTopMediaPathInfoTextView.setText(m_sRootPath);

		m_PathInfo.setLastLevelFocus(0);
		m_GridViewFragment.RefreshGridView(m_PathInfo.getLastLevelFocus());


		if (listItems.size() == 0) {
			msg_hint_noFile.setMessage(m_ResourceMgr
					.getString(R.string.msg_noFile));

			msg_hint_noFile.show();
			msg_hint_noFile.setFocusable(true);
			msg_hint_noFile.update();

			msg_hint_noFile.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss() {
					// TODO Auto-generated method stub
	//				onBackClicked();
				}

			});
			return;
		}
	}

	private void showInList() {
		dismissLoading();
		if (listItems.size() == 0) {
			msg_hint_noFile.setMessage(m_ResourceMgr
					.getString(R.string.msg_noFile));

			msg_hint_noFile.show();
			msg_hint_noFile.setFocusable(true);
			msg_hint_noFile.update();

			msg_hint_noFile.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss() {
					// TODO Auto-generated method stub
	//				onBackClicked();
				}

			});

			return;
		}
		if (listItems.size() >= MediaApplication.MAXFILENUM) {
			PopupMessageShow(
					(String) m_ResourceMgr.getText(R.string.maxfilenum),
					R.drawable.message_box_bg, 260, 678, Gravity.CENTER, 0, 0,
					5000);
		}

		m_PathInfo.setLastLevelFocus(0);
		m_GridViewFragment.RefreshGridView(m_PathInfo.getLastLevelFocus());
	}

	private void firstsShowInList() {
		dismissLoading();
		m_GridViewFragment.RefreshGridView(m_PathInfo.getLastLevelFocus());

		if (listItems.size() == 0) {
			msg_hint_noFile.setMessage(m_ResourceMgr
					.getString(R.string.msg_noFile));

			msg_hint_noFile.show();
			msg_hint_noFile.setFocusable(true);
			msg_hint_noFile.update();

			msg_hint_noFile.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss() {
					// TODO Auto-generated method stub
	//				onBackClicked();
				}

			});
			return;
		}
	}

	private void backShowInList() {
		System.out.println("backShowInList");
		dismissLoading();
		System.out.println("m_PathInfo.getLastLevel()"
				+ m_PathInfo.getLastLevel());
		folderPath.remove(m_PathInfo.getLastLevel());
		m_GridViewFragment.RefreshGridView(m_PathInfo.getLastLevelFocus());

		if (listItems.size() == 0) {
			msg_hint_noFile.setMessage(m_ResourceMgr
					.getString(R.string.msg_noFile));
			if(mActivityPauseFlag == 1)
			{
				return;
			}
			msg_hint_noFile.show();
			msg_hint_noFile.setFocusable(true);
			msg_hint_noFile.update();

			msg_hint_noFile.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss() {
					// TODO Auto-generated method stub
	//				onBackClicked();
				}

			});
			return;
		}
	}
	
	
	public void getFileList(String path, String uniqueCharID) {
		boolean upnpBrowserSuccess = false;
		int iDirSize = 0, iItemSize = 0;
		if(path == null || path.length() == 0)
			return;
		if (path.equals(m_sRootPath)) {
			upnpBrowserSuccess = DLNADataProvider.browseServer(mMediaApplicationMap
					.getMediaServerUUID());
			mCharIDList.add(mMediaApplicationMap.getMediaServerUUID());
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
			return;
		}
		CreateTotalFileList(iDirSize, iItemSize);
	}

	public void CreateTotalFileList(int iDirSize, int iItemSize) {
		if (listItems == null) {
			listItems = mMediaApplicationMap.getFileList();
		}
		listItems.clear();

		if (iDirSize > 0) {
			for (int i = 0; i < iDirSize; i++) {
				DLNAFileInfo finfo = new DLNAFileInfo(i,
						DLNADataProvider.getDirectoryTitle(i),
						DLNADataProvider.getDirectoryCharID(i),
						FileFilterType.DEVICE_FILE_DIR,
						m_PathInfo.getLastLevelPath()+DLNADataProvider.getDirectoryTitle(i) + "/",
						DLNADataProvider.queryDataByFile(
								DLNADataProvider.getDirectoryTitle(i),
								DLNADataProvider.UPNP_DMP_RES_DATE, null)
						, null
						, null
						);
				listItems.add(finfo);
			}
		}

		if (iItemSize > 0) {
			for (int i = 0; i < iItemSize; i++) {
				String FileName = DLNADataProvider.getItemTitle(i);
				if (FileName ==null||FileName.length() == 0
						|| DLNADataProvider.GetMediaType(FileName) == 0) {
					return;
				}
				
				if(DLNADataProvider.GetMediaType(FileName) == FileFilterType.DEVICE_FILE_PHOTO) {
					String itemCharID = DLNADataProvider.getItemCharID(i);
					
					boolean bDTCP = false;
					 String tmpProtolinfo = "";
					 String tmpFilePath = null;
					 tmpFilePath = DLNADataProvider.getItemUrl(itemCharID);
					 
					 tmpProtolinfo = DLNADataProvider.queryDataByID(itemCharID,DLNADataProvider.UPNP_DMP_RES_PROTOCOLINFO);
				//	 Log.v(TAG, "originalpath isss= "+tmpFilePath);
				//	 Log.v(TAG, "tmpProtolinfooooo= "+tmpProtolinfo);
					 if(tmpProtolinfo != null){
						 bDTCP = tmpProtolinfo.contains("DTCP1HOST");
						 System.out.println("tmpProtolinfotmpProtolinfo"+tmpProtolinfo+"-"+bDTCP);
						 if(!bDTCP){
							 
							 if(tmpFilePath != null && tmpFilePath.contains("?")){
								 String tmpPath = tmpFilePath.substring(tmpFilePath.indexOf("?"));
								 if(tmpPath.contains("CONTENTPROTECTIONTYPE=DTCP1")){
									 bDTCP = true;
								 }
							 }
						 }
						 if(bDTCP){
							 tmpProtolinfo = " protocolinfo=" + tmpProtolinfo.substring(0, tmpProtolinfo.lastIndexOf(":"     ) + 1) + "*";
						 } else {
							 tmpProtolinfo = "";
						 }
					 } else {
						 tmpProtolinfo = "";
					 }
					String url = tmpFilePath +tmpProtolinfo;
					
					
					
					DLNAFileInfo finfo = new DLNAFileInfo(i, FileName, itemCharID,
							FileFilterType.DEVICE_FILE_PHOTO,
							url,
							DLNADataProvider.queryDataByFile(FileName,DLNADataProvider.UPNP_DMP_RES_DATE, null),
							0,0,null);
					listItems.add(finfo);
				}
			}
		}
	}
	
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
	    // TODO Auto-generated method stub  
	    String []paths = new String[m_PathInfo.curPathArr.size()];
	    String []charIds = new String[mCharIDList.size()];
	    String []folders = new String[folderPath.size()];
	    for(int i=0;i<m_PathInfo.curPathArr.size();i++)
	    {
	    	paths[i]=m_PathInfo.curPathArr.get(i).path;
	    }
	    for(int i=0;i<mCharIDList.size();i++)
	    {
	    	charIds[i]=mCharIDList.get(i);
	    }
	    for(int i=0;i<folderPath.size();i++)
	    {
	    	folders[i]=folderPath.get(i);
	    }

	    outState.putStringArray("pathInfo_paths", paths);
	    outState.putStringArray("charIds", charIds);
	    outState.putStringArray("folders", folders);
	    
	    Log.v(TAG, "onSaveInstanceState");  
	    super.onSaveInstanceState(outState);
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
	@Override
	public int[] getAddr() {
		// TODO Auto-generated method stub
		if(mTv ==null)
		{
			mTv  = (TvManager)this.getSystemService("tv");
		}
		if(vAddrForDTCP==null)
		{
			vAddrForDTCP = mMediaApplicationMap.getAddrForDTCP();
		}
			
		/*if(vAddrForDTCP[0]==-1||vAddrForDTCP[1]==-1)
		{	
			Log.v(TAG, "dtcp memory alloc.");
			vAddrForDTCP = mTv.startDecodeDtcpImageFile(64*1024);
			mMediaApplicationMap.setAddr(vAddrForDTCP);
		}*/
		return vAddrForDTCP;
	}
	@Override
	public void releaseAddr(int[] addr) {
		// TODO Auto-generated method stub
		if(mTv ==null||vAddrForDTCP[0] == -1||vAddrForDTCP[1] == -1)
		{
			return;
		}
		//mTv.stopDecodeDtcpImageFile(vAddrForDTCP);
		Log.v(TAG, "dtcp memory release.");
		vAddrForDTCP[0] = -1;
		vAddrForDTCP[1] = -1;
		mMediaApplicationMap.setAddr(vAddrForDTCP);	
	}
}
