package com.rtk.dmp;
 
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.DensityUtil;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.realtek.bitmapfun.util.CommonActivityWithImageWorker;
import com.realtek.bitmapfun.util.DiskLruCache;
import com.realtek.bitmapfun.util.ImageCache;
import com.realtek.bitmapfun.util.ImageFetcher;
import com.realtek.bitmapfun.util.ImageResizer;
import com.realtek.bitmapfun.util.ImageWorker;
import com.realtek.bitmapfun.util.LoadingControl;
import com.realtek.bitmapfun.util.ReturnSizes;
import com.realtek.bitmapfun.util.Utils;
import com.rtk.dmp.GridViewFragment.GridViewLoadingControl;

import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.app.TvManager;
import android.widget.ProgressBar;
public class PhotoPlayerActivityPause extends CommonActivityWithImageWorker implements Observer
{
	private final static String TAG = "PhotoPlayerActivityPause";
	private int ScreenWidth = 0,ScreenHeight = 0; 
	private Resources m_ResourceMgr = null;
	private TvManager mTv = null;
	
	private MediaApplication mMediaApplicationMap = null;
	private ArrayList<DLNAFileInfo> mPhotoList;
	boolean mIsFromAnywhere=false;
	private BroadcastReceiver mTvawReceiver = null;
	IntentFilter mTvawFilter = null;
	private GestureDetector  gestureScanner;
	
	private int m_initPos = 0;
	private int m_totalCnt = 0;
	int dirsize = 0;
	private int m_currentPlayIndex = 0;
	private String[] m_filePathStrArray = null;
	private PictureSurfaceView sv = null;
	
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
	
	
	
	private float rate = 1;
	private float oldRate = 1;
	private boolean isFirst = true;
	
	
	private boolean canDrag = false;
	private boolean canRecord = true;
	float oldLineDistance = 0f;
	float oldDistanceX = 0f;
	float oldDistanceY = 0f;
	float moveX = 0f,moveY = 0f;
	float startPointX = 0f, startPointY = 0f;
	boolean disableMove = false;
	boolean hasTranslate = false;
	
	
	private String serverName = null;
	public ArrayList<String> DMSName = new ArrayList<String>();
	
	
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
	
	
	//save the Click order
	private int order = 0;
	
	
	//player zoom mode
	private int mZoomMode = 0;
	private final static int ZOOMMODE_NORMAL = 1;
	private final static int ZOOMMODE_INX2   = 2;
	private final static int ZOOMMODE_INX4   = 4;
	private final static int ZOOMMODE_INX8   = 8;
	private final static int ZOOMMODE_INX16  = 16;
	
	//player repeat mode
	private int mRepeatMode = REPEATMODE_OFF;
	private final static int REPEATMODE_ON  = 0;
	private final static int REPEATMODE_OFF = 1;
	
	//player rotate mode
	private int mRotateMode = ROTATEMODE_0D;
	private final static int ROTATEMODE_0D = 0;
	private final static int ROTATEMODE_90D = 90;
	private final static int ROTATEMODE_180D = 180;
	private final static int ROTATEMODE_270D = 270;
	
	
	//
	private final static int RESULT_PHOTOPLAY = 100;
	private final static int RESULT_VAL = 101;
	private int isReturnFromPlay_zoom   = -1;
	private int isReturnFromPlay_rotate = -1;
	
	private Handler UIHandler = null;
	private long mBannerShowTime = 6000; 
	private long mLastControlTime = 0l;
	
	
	
	//for menu icon(quickmenu)
	
	private QuickMenu mQuickMenu = null;
	private QuickMenuPhotoAdapter mQuickmenuAdapter = null;
	private String[] mIntervalTimeStatus = new String[3];
	private ImageView btn_menu = null;
	private int mIntervalIndex = 1;

	//Activity flag
	private int mActivityPauseFlag = 0;
	private int mActivityDestroyFlag = 0;
	private boolean  mIsFullScrean = false; 
	
	
	private SharedPreferences mPerferences = null;
	
	private boolean hasPliSharedMemory = false;
	private int[] vAddrForDTCP;
	
	private ProgressBar loading;
	private Handler loadingHandler;
	//for test
	private int test_decodethreadnum = 0;
	
	private boolean isFirstEnter = true;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.photoplayer_gallary);	
		mPerferences = PreferenceManager.getDefaultSharedPreferences(this);
		m_ResourceMgr = this.getResources();
		mTv = (TvManager)this.getSystemService("tv");
		play_btn   = (ImageButton)findViewById(R.id.play_btn);
		repeat_btn = (ImageButton)findViewById(R.id.repeat_btn);
		zoom_btn   = (ImageButton)findViewById(R.id.info_zoom);
		rotate_btn = (ImageButton)findViewById(R.id.rotate_btn);
		
		mCache_small = ImageCache.createCache(this,"images");
		Intent intent = getIntent();
		m_initPos = intent.getIntExtra("initPos", 0);
		m_totalCnt = intent.getIntExtra("totalCnt", 0);
		serverName = intent.getStringExtra("serverName");
		mIsFromAnywhere = intent.getBooleanExtra("isanywhere", false);
		mMediaApplicationMap = (MediaApplication) getApplication();

		if (mIsFromAnywhere == false)
		{
			if (mPhotoList == null) 
			{
				mPhotoList = mMediaApplicationMap.getFileList();
			}
			int photoListSize = mPhotoList.size();
			m_filePathStrArray = new String[photoListSize];
			{
				int tmpj = 0;
				if (m_totalCnt > 0) 
				{
					// get filePathArrayStr
					for (int i = 0; i < photoListSize; i++) {
						if (mPhotoList.get(i).getFileType() == FileFilterType.DEVICE_FILE_PHOTO) {
							m_filePathStrArray[tmpj] = mPhotoList.get(i).getFilePath();
							tmpj++;
						}
						else
						{
							dirsize++;
						}
					}
				}
			}
		}
		else 
		{
			m_filePathStrArray = intent.getStringArrayExtra("filelist");
			m_totalCnt = m_filePathStrArray.length;
			registerReceiver(mTvawReceiver, mTvawFilter);
		}
		// mRepeatIndex = intent.getIntExtra("repeatIndex", 0);
		// mRepeatMode = mRepeatIndex;

		//check if dtcp photo.
		for (int i = 0; i < m_filePathStrArray.length-dirsize; i++) {
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
			
			
		if (m_initPos < 0 && m_initPos > m_totalCnt-dirsize - 1) {
			m_initPos = 0;
		}

		m_currentPlayIndex = m_initPos;
		mRepeatMode = REPEATMODE_OFF;
		repeat_btn_statu = mRepeatMode == REPEATMODE_OFF?REPEAT_OFF_BTN_NOTFOCUS:REPEAT_ON_BTN_NOTFOCUS;
	//	picture_full = (ImageView)findViewById(R.id.picture_focused);
		controlbar_photoplayer = (RelativeLayout)findViewById(R.id.controlbar_photoplayer);
		controlbar_photoplayer.getBackground().setAlpha(50);
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		ScreenWidth = dm.widthPixels;
		ScreenHeight = dm.heightPixels;
		Log.v(TAG,"ScreenWidth is "+ScreenWidth+" ,ScreenHeight is "+ScreenHeight);

		loading = (ProgressBar)findViewById(R.id.loading);
		loading.setVisibility(View.INVISIBLE);
		loadingHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					loading.setVisibility(View.VISIBLE);
					break;
				case 1:
					loading.setVisibility(View.INVISIBLE);
					break;
				default:
					break;
				}
				super.handleMessage(msg);
			}
		};
		

		Init_QuickMenu();
	}
	
    public class GridViewLoadingControl implements LoadingControl
    {
    	@Override
    	public void startLoading(int pos) {
    		// TODO Auto-generated method stub

    		ImageView content_bg = new ImageView(PhotoPlayerActivityPause.this);   
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
	  	    		PhotoPlayerActivityPause.this, R.drawable.anim);
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

	

	private void Init_QuickMenu() {
		// TODO Auto-generated method stub
		
		mIntervalTimeStatus[0] = (String) m_ResourceMgr.getText(R.string.qm_interval_fast);
		mIntervalTimeStatus[1] = (String) m_ResourceMgr.getText(R.string.qm_interval_normal);
		mIntervalTimeStatus[2] = (String) m_ResourceMgr.getText(R.string.qm_interval_slow);
		
		mQuickmenuAdapter = new QuickMenuPhotoAdapter(this);
		mQuickMenu = new QuickMenu(this, mQuickmenuAdapter);
		mQuickMenu.setAnimationStyle(R.style.QuickAnimation);
		
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
						break;
					}
					case 2:
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
				/*
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
				*/
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
		
	}



	@Override
	public void onNewIntent(Intent intent) {
		
		super.onNewIntent(intent);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
		if(event.getPointerCount() == 1)
        {
			if(!disableMove)
			{
				if(mZoomMode == ZOOMMODE_NORMAL)
				{
				
					if(canRecord)
		        	{
		        		startPointX = rotate_translateX(event.getX(),event.getY());
		        		startPointY = rotate_translateY(event.getX(),event.getY());
		        		canRecord = false;
		        	}
					else
		        	{
		        		float distanceX = rotate_translateX(event.getX(),event.getY()) - startPointX;
		        		float distanceY = rotate_translateY(event.getX(),event.getY()) - startPointY;
		        		
		        		float rotate_tmp = -1;
		        		if(mRotateMode ==ROTATEMODE_0D)
		        		{
		        			rotate_tmp = distanceX;
		        		}
		        		else if(mRotateMode ==ROTATEMODE_90D)
		        		{
		        			rotate_tmp = -distanceY;
		        		}
		        		else if(mRotateMode ==ROTATEMODE_180D)
		        		{
		        			rotate_tmp = -distanceX;
		        		}
		        		else if(mRotateMode ==ROTATEMODE_270D)
		        		{
		        			rotate_tmp = distanceY;
		        		}
		        		
		        		if(rotate_tmp > 100 && hasTranslate == false)
		        		//show left pictures(start from left,end to right)
		        		{
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
		        		else if(rotate_tmp < -100 && hasTranslate == false)
		        		//show right pictures(start from right,end to left)
		        		{
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
		        			hasTranslate = true;
		        		}
		        	}
					
				}
				else if(mZoomMode > 0) 
				{
					if(canRecord)
		        	{
		        		startPointX = rotate_translateX(event.getX(),event.getY());
		        		startPointY = rotate_translateY(event.getX(),event.getY());
		        		canRecord = false;
		        	}
		        	else
		        	{
		        		float distanceX = rotate_translateX(event.getX(),event.getY()) - startPointX;
		        		float distanceY = rotate_translateY(event.getX(),event.getY()) - startPointY;
		        		moveX = distanceX+oldDistanceX;
		        		moveY = distanceY+oldDistanceY;

		        		if(moveX > ScreenWidth*(oldRate -1)/2 )
		        			moveX = ScreenWidth*(oldRate -1)/2 ;
		        		else if(moveX < -ScreenWidth*(oldRate -1)/2)
		        			moveX =  -ScreenWidth*(oldRate -1)/2;

		        		if(moveY > ScreenHeight*(oldRate -1)/2)
		        			moveY = ScreenHeight*(oldRate -1)/2;
		        		else if(moveY < -ScreenHeight*(oldRate -1)/2)
		        			moveY =  -ScreenHeight*(oldRate -1)/2;
		        		
		        		
		        		sv.setMove(moveX,moveY);
		        		//if sv.setMove(moveX,moveY), it shows another acceptable effect
		        		//when surface is scaled, X and Y is scaled too.
		        		//So before use the values, we need to return it to original condition  
		        	}
				}	
			}
        }
		
		if (event.getPointerCount() > 1) {
			disableMove = true;
			if (event.getPointerCount() == 2) {
				float rotate_translateX0 = rotate_translateX(event.getX(0),event.getY(0));
				float rotate_translateY0 = rotate_translateY(event.getX(0),event.getY(0));
				float rotate_translateX1 = rotate_translateX(event.getX(1),event.getY(1));
				float rotate_translateY1 = rotate_translateY(event.getX(1),event.getY(1));
				
				float centerX =(event.getX(0)+event.getX(1))/2;
				float centerY =(event.getY(0)+event.getY(1))/2;
				if (isFirst) {

					oldLineDistance = (float) Math.sqrt(Math.pow(rotate_translateX1 - rotate_translateX0, 2)
							+ Math.pow(rotate_translateY1 - rotate_translateY0, 2));
					isFirst = false;
					
					
					sv.setScaleCenter(centerX, centerY);
					System.out.println("center x and y is :"+centerX+" "+centerY);
				} else {

					float newLineDistance = (float) Math.sqrt(Math.pow(rotate_translateX1 - rotate_translateX0, 2)
							+ Math.pow(rotate_translateY1 - rotate_translateY0, 2));

					rate = oldRate * newLineDistance / oldLineDistance;
					if(rate >1)
					{
						mZoomMode = ZOOMMODE_INX2;
						if(rate>ZOOMMODE_INX16)
							rate =ZOOMMODE_INX16;
						checkZoomMode(rate);
						sv.setScale(rate);
					}
					else
					{
						;//if rate < 1 ,do nothing here and do resetsurfaceview in action_up,
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
				
				if(rate<=1)
				{
					resetSurfaceView();
				}
					
				break;
	        }
        }

        return gestureScanner.onTouchEvent(event); 
	}
	private float rotate_translateX(float x,float y)
	{
		switch(mRotateMode)
		{
			case ROTATEMODE_0D:
			{
				return x;
			}
			case ROTATEMODE_90D:
			{
				return y;
			}
			case ROTATEMODE_180D:
			{
				return -x;
			}
			case ROTATEMODE_270D:
			{
				return -y;
			}
		
		}
		return -1f;//error
	}
	private float rotate_translateY(float x,float y)
	{
		switch(mRotateMode)
		{
			case ROTATEMODE_0D:
			{
				return y;
			}
			case ROTATEMODE_90D:
			{
				return -x;
			}
			case ROTATEMODE_180D:
			{
				return -y;
			}
			case ROTATEMODE_270D:
			{
				return x;
			}
		}
		return -1f;//error
	}
	float rotate_translateCenterX(float x1,float x2)
	{
		return 0f;
	}
	float rotate_translateCenterY(float y1,float y2)
	{
		return 0f;
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



	private void NextImage() {
		// TODO Auto-generated method stub
		if(mRepeatMode == REPEATMODE_ON)
		{
			if(m_currentPlayIndex == m_totalCnt-dirsize - 1)
			{
				m_currentPlayIndex = 0;
				playPicture(m_currentPlayIndex);
			}
			else 
			{
				playPicture(m_currentPlayIndex+1);
			}
		}
		else if(mRepeatMode == REPEATMODE_OFF)
		{
		
			if(m_currentPlayIndex == m_totalCnt-dirsize - 1)
			{
				playPicture(m_currentPlayIndex);
			}
			else 
			{
				playPicture(m_currentPlayIndex+1);
			}
			
		}
	}
	private void PreImage() {
		// TODO Auto-generated method stub
		if(mRepeatMode == REPEATMODE_ON)
		{
			if(m_currentPlayIndex == 0)
			{
				m_currentPlayIndex = m_totalCnt-dirsize - 1;
				playPicture(m_currentPlayIndex);
			}
			else
			{
				playPicture(m_currentPlayIndex-1);
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
				playPicture(m_currentPlayIndex-1);
			}
		}
	}



	@Override
	public void onResume() {
		super.onResume();
		if(sv == null)
		{
			sv = (PictureSurfaceView)findViewById(R.id.picture_focused);
		
			sv.setOnTouchListener(new View.OnTouchListener() {  
	  
	            @Override  
	            public boolean onTouch(View v, MotionEvent event) {  
	                // TODO Auto-generated method stub  
	                return false; 
	            }  
	        });
		}
		
		UIHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					resetSurfaceView();
					if(isReturnFromPlay_zoom != -1)
					{
						mZoomMode = isReturnFromPlay_zoom;
						checkZoomMode(-1);
						isReturnFromPlay_zoom = -1;
					}
					break;
				case 1:
					setbanner();
					break;
				case 2:
					 mQuickmenuAdapter.notifyDataSetChanged();
					 break;
				default:
					break;
				}

				super.handleMessage(msg);
			}
			
		};
	
		playPicture(m_currentPlayIndex);
		
		play_btn.setBackgroundResource(R.drawable.dnla_play_icon_n);
		play_btn.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				// TODO Auto-generated method stub

				play_btn.setBackgroundResource(R.drawable.dnla_play_icon_f);

				switch (event.getAction()) {   
	                case MotionEvent.ACTION_UP: 

	    				Bundle bundle = new Bundle();
	    				bundle.putString("serverName", serverName);

	    				bundle.putInt("initPos", m_currentPlayIndex);
	    				bundle.putInt("totalCnt", m_totalCnt);
	    				bundle.putInt("repeat", mRepeatMode);
	    				bundle.putInt("rotate", mRotateMode);
	    				
	    				Intent intent = new Intent();
	    				intent.putExtras(bundle);
	    				ComponentName componetName = new ComponentName(
	    						"com.rtk.dmp",
	    						"com.rtk.dmp.PhotoPlayerActivityPlay");
	    				intent.setComponent(componetName);
	    				startActivityForResult(intent, RESULT_PHOTOPLAY);

	                break;
				 }
				 return true;
			}
		});
		
		if(mRepeatMode == REPEATMODE_ON)
		{
			repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_on_icon_n);
		}
		else if(mRepeatMode == REPEATMODE_OFF)
		{
			repeat_btn.setBackgroundResource(R.drawable.dnla_repeat_off_icon_n);
		}

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
				mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
				int divide = arg0.getWidth()/5;
				if(event.getX()>0 && event.getX()<divide)
				{
					zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_1_f);
					mZoomMode = ZOOMMODE_NORMAL;
					resetSurfaceView();
				}
				else if(event.getX()>divide && event.getX()<divide*2)
				{
					resetSurfaceView();
					mZoomMode = ZOOMMODE_INX2;
					sv.setScaleCenter(sv.getWidth()/2, sv.getHeight()/2);
					checkZoomMode(-1);
				}
				else if(event.getX()>divide*2 && event.getX()<divide*3)
				{
					resetSurfaceView();
					mZoomMode = ZOOMMODE_INX4;
					sv.setScaleCenter(sv.getWidth()/2, sv.getHeight()/2);
					checkZoomMode(-1);
				}
				else if(event.getX()>divide*3 && event.getX()<divide*4)
				{
					resetSurfaceView();
					mZoomMode = ZOOMMODE_INX8;
					sv.setScaleCenter(sv.getWidth()/2, sv.getHeight()/2);
					checkZoomMode(-1);
				}
				else if(event.getX()>divide*4 && event.getX()<divide*5)
				{
					resetSurfaceView();
					mZoomMode = ZOOMMODE_INX16;
					sv.setScaleCenter(sv.getWidth()/2, sv.getHeight()/2);
					checkZoomMode(-1);
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
	                	resetSurfaceView();
	                	
	                	sv.setRotate(mRotateMode);
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
				if(mQuickMenu.isShowing() == true)
					mQuickMenu.dismiss();
				else
				{
					mQuickMenu.showAtRTop(DensityUtil.dip2px(getApplicationContext(), 14),
							DensityUtil.dip2px(getApplicationContext(), 72),318);
					mQuickMenu.setTimeout();
				}
			}
			
		});
		
		controlbar_photoplayer.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
				return true;
			}
			
		});
		
		gestureScanner = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
			@Override
		    public boolean onDoubleTap(MotionEvent event) {
				System.out.println("onDoubleTap ");
		        //TODO
				mZoomMode = mZoomMode * 2 > ZOOMMODE_INX16 ? ZOOMMODE_NORMAL : mZoomMode * 2;
				if(mZoomMode == ZOOMMODE_NORMAL)
					resetSurfaceView();
				else
				{
					float centerX = event.getX();
					float centerY = event.getY();

					sv.setScaleCenter(centerX, centerY);
					checkZoomMode(-1);
				}
				
		        return false;
		    }
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {   
				setbanner();
		        return false;   
		      } 
			
		});
		
		mLastControlTime = (new Date(System.currentTimeMillis())).getTime();
		new Thread(new Runnable() {
			public void run() {
				long curtime = 0;
				while (true) {
					if (mActivityDestroyFlag == 1)
						break;
					curtime = (new Date(System.currentTimeMillis())).getTime();
					if (!mIsFullScrean) {
						if (curtime - mLastControlTime > mBannerShowTime) {
							Message msg = new Message();
							msg.what = 1;
							UIHandler.sendMessage(msg);
						}
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
		
		checkZoomMode(-1);
		
		getStoredVals();

	};
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
	private void getStoredVals() {
		// TODO Auto-generated method stub
		new Thread(new Runnable() {
			@Override
			public void run() {
				Editor editor = mPerferences.edit();
				int tmp_index_interval = mPerferences.getInt("intervalIndex_photo", -1);
				
				if(tmp_index_interval == -1)
				{
					editor.putInt("intervalIndex_photo", mIntervalIndex);
					editor.commit();
				}else{
					mIntervalIndex = tmp_index_interval;
				}
			}
		}).start();	
	}

	private void checkZoomMode(float fRate)
	{
		if(fRate == -1)
		{
			rate = mZoomMode;
			if(mZoomMode == ZOOMMODE_INX2)
			{
				zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_2_f);
			}
			else if(mZoomMode == ZOOMMODE_INX4)
			{
				zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_4_f);
			}
			else if(mZoomMode == ZOOMMODE_INX8)
			{
				zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_8_f);
			}
			else if(mZoomMode == ZOOMMODE_INX16)
			{
				zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_16_f);
			}
			else if(mZoomMode == ZOOMMODE_NORMAL)
			{
				zoom_btn.setBackgroundResource(R.drawable.dnla_zoom_1_f);
			}
			
		}
		else
		{
			rate = fRate;
		}
		sv.setScale(rate);
		
	}
	
	public static String getFileCreateDate(File _file,String formatString) {
        File file = _file;
        Date last_date = new Date(file.lastModified());
        SimpleDateFormat df_des = new SimpleDateFormat(formatString);
        String last_modify_date=df_des.format(last_date);
 		return last_modify_date;
    }
	public static String dateFormate(String date,String formatString) {
		// TODO Auto-generated method stub				
		SimpleDateFormat df_ori_exif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
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
	private void init_gsv() {
		// TODO Auto-generated method stub
		gallery= (HorizontalScrollView)findViewById(R.id.gallery);
		item_gallery=(RelativeLayout) findViewById(R.id.item_gallery);
        child_count = m_totalCnt-dirsize;  
        child_show_count = 7;   
        child_start = m_currentPlayIndex+1;  
        if(child_count<=7)
        	noScrollGallery = true;
	}
	
	
	@Override  
    public void onWindowFocusChanged(boolean hasFocus) {  
        // TODO Auto-generated method stub  
        super.onWindowFocusChanged(hasFocus);
        Log.v(TAG, "onWindowFocusChanged "+hasFocus);
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
	
	private void initHsvStart() {
		// TODO Auto-generated method stub
		Log.v(TAG,"initHsvStart");
		final ViewTreeObserver observer = gallery.getViewTreeObserver();  
		
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {  
  
            @Override  
            public boolean onPreDraw() {  
                // TODO Auto-generated method stub
            	
            	boolean isAlive =observer.isAlive();
            	if(isAlive)
            	{
            		observer.removeOnPreDrawListener(this);
            	}
            	else
            	{
            		Log.v(TAG, "Activity is not Alive yet!");
            	}
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
    		
    		int tag = i-child_count*2>0?i-child_count*2:(i-1);
    		ImageView iv = (ImageView)(gallery.findViewWithTag(tag));

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
	                    	index_from = current_item- 3;
	                    	index_to = current_item+6;
	                    	//it should be +3."+6" is used to pre-load 3 pictures in hsv-gallery
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
                    
                    System.out.println("current_item % child_count"+ current_item + " " + child_count+ 
                            " " + (current_item % child_count)+" "+child_width / 2+" "+hsv_width / 2);
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
	
		
	}

	private void initHsvData() {
		// TODO Auto-generated method stub
		if(noScrollGallery)
		{
			for (int i = 0; i < child_count; i++) 
			{  
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
						
						changeFocusToItem(tmpi);
						m_currentPlayIndex =(Integer) img_content.getTag();
						playPicture(m_currentPlayIndex);
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
						changeFocusToItem(tmpi);
						m_currentPlayIndex =(Integer) img_content.getTag();
						playPicture(m_currentPlayIndex);	
					}
					
				}
        	});	
        }
		for (int i = 0; i < child_count; i++)
		{
            final ImageView img_content = new ImageView(this);  
        	colorMatrix.set(array_grey);
        	img_content.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            img_content.setLayoutParams(new ViewGroup.LayoutParams(child_width,  
            		ViewGroup.LayoutParams.MATCH_PARENT));  
            img_content.setPadding(5, 3, 5, 3);
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
			if(hasJump)
			{
				for(int i=itemTag-2;i <= itemTag+3;i++)// =itemTag- 4;i<=itemTag-num+1;i++)
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
				
			}
		}
		else
		{
			num = (tmpDestScorllX - tmpScrollX)/child_width;
			for(int i =itemTag + 4 - num;i<=itemTag+3;i++)
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
			if(hasJump)
			{
				;//fix me
			}
		}
	}
	private void playPicture(final int index)
	{
		order++;
		m_currentPlayIndex = index;
		final int tmporder = order;
		final Message msg = new Message();
		final DLNAFileInfo dif = mPhotoList.get(index);
		test_decodethreadnum++;
		Thread getBitmapThread = new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				File file =null;
				Bitmap b = null;
				
				Message msgLoadingStart = new Message();
				msgLoadingStart.what = 0;
				loadingHandler.sendMessage(msgLoadingStart);
				
    			file = mImageWorker.getFileFromcache(m_filePathStrArray[m_currentPlayIndex],true,vAddrForDTCP);
    			if(file == null )
    			{
    				Message msgLoadingEnd = new Message();
    				msgLoadingEnd.what = 1;
    				loadingHandler.sendMessage(msgLoadingEnd);
    				return;
    			}

				
				ExifInterface exif_http = null;
				int ori    = -1;
				int digree = -1; 
				try {
					exif_http = new ExifInterface(file.getPath());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if(exif_http == null)
					return;
				
				ori = exif_http.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED);
				switch (ori) 
				{ 
					case ExifInterface.ORIENTATION_ROTATE_90: 
						digree = 90; 
						break; 
					case ExifInterface.ORIENTATION_ROTATE_180: 
						digree = 180; 
						break; 
					case ExifInterface.ORIENTATION_ROTATE_270: 
						digree = 270; 
						break; 
					default: 
						digree = 0; 
						break; 
				} 
				dif.setOrientionExif(digree);
				
				
				String date = exif_http.getAttribute(ExifInterface.TAG_DATETIME);
		        if(date == null)
		        {
			//		File _file = new File(mDataProvider.GetDataAt(tmppos));
			//		date = getFileCreateDate(_file,DATAFORMAT);
					date = DLNADataProvider.queryDataByFile(
							dif.getFileName(), DLNADataProvider.UPNP_DMP_RES_DATE, null);
		        }
		        else
		        {
		        	String 	language= getApplicationContext()
		        			.getResources().getConfiguration().locale.getLanguage();
		        	if(language.equals("ja"))
		        	{
		        		date = dateFormate(date,
		        				"yyyy"+(String) m_ResourceMgr.getText(R.string.year_menuicon)
		        			   +" MMM dd"+(String) m_ResourceMgr.getText(R.string.day_menuicon)
		        			   +" EEEEEE HH:mm");
		        	}
		        	else
		        	{
		        		date = dateFormate(date,"HH:mm EEE,dd MMM yyyy");
		        	}
		        }
		        dif.setDateExif(date);
		        
		        Message msg = new Message();
		        msg.what = 2;
			//	UIHandler.sendMessage(msg);
				
				b=ImageResizer.decodeSampledBitmapFromFile(file.toString(), 1080, 1920); 
				if (mPhotoList.get(m_currentPlayIndex).getOrientionExif() > 0) { 
	    			Matrix m = new Matrix(); 
	    			m.postRotate(mPhotoList.get(m_currentPlayIndex).getOrientionExif()); 
	    			b = Bitmap.createBitmap(b, 0, 0, b.getWidth(),b.getHeight(), m, true); 
	    		} 

				Message msgLoadingEnd = new Message();
				msgLoadingEnd.what = 1;
				loadingHandler.sendMessage(msgLoadingEnd);
				
				////bitmap is available
				if(sv.isCanvasFilled()==false)
				{
					sv.setHasBitmap(b,0,tmporder);
				}
				else
				{
					sv.setHasBitmap(b,0,tmporder);
				}
				
				
				msg.what = 0;
				UIHandler.sendMessage(msg);
				
				sv.setRotate(-mRotateMode);
				mRotateMode = ROTATEMODE_0D;
				
				if(isReturnFromPlay_rotate != -1)
				{
					sv.setRotate(isReturnFromPlay_rotate);
					mRotateMode = isReturnFromPlay_rotate;
					isReturnFromPlay_rotate = -1;
				}

			}

			
		});
		getBitmapThread.start();

		test_decodethreadnum--;
		
	}

    private void isChecked(int item, boolean isChecked) {  
        if (isChecked) {  
        } else {  
        }  
    }  
	



	@Override
    protected void initImageWorker()
    {
		if(mImageWorker==null)
		{
	        mReturnSizes =  new ReturnSizes(200, 100);
	        mImageWorker = new ImageFetcher(this, null, mReturnSizes.getWidth(),
	                mReturnSizes.getHeight());
	        mImageWorker.setImageCache(mCache_small);
	        mImageWorker.setImageFadeIn(false);
		}
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
       	 //Log.d(TAG,"onPause cancel decode:"+i);
        	ImageView iv = (ImageView)gallery.findViewWithTag(i);
            cancelDecode(iv);
        }
        
		if (mQuickMenu.isShowing()) {
			mQuickMenu.setIsActivityPause(mActivityPauseFlag);
		}

	}
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
		mActivityDestroyFlag = 1;
		mMediaApplicationMap.deleteObserver(this);
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) { 

    		case KeyEvent.KEYCODE_DPAD_UP: 
    			sv.y-=3; 
    			break; 

    		case KeyEvent.KEYCODE_DPAD_DOWN: 
    			sv.y+=3; 
    			break; 
    		case KeyEvent.KEYCODE_G:
    			gallery.setVisibility(View.INVISIBLE);
    		case KeyEvent.KEYCODE_BACK:
    			if (mZoomMode == ZOOMMODE_NORMAL) {
    				finish();
    				return true;
    			} else {
    				resetSurfaceView();
    			}
    			break;
    	} 
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		
		switch(keyCode)
		{
			case KeyEvent.KEYCODE_Q:
			{
                gallery.smoothScrollTo(child_width  
                        * last_item - child_width / 2 - hsv_width / 2,  
                        gallery.getScrollY());  
                break;
			}
			case KeyEvent.KEYCODE_W:
			{
				resetSurfaceView();
				break;
			}
			case KeyEvent.KEYCODE_BACK:
				return true;
			
		}
		
		return super.onKeyDown(keyCode, event);
	}


	private void resetSurfaceView() {
		// TODO Auto-generated method stub
		moveX = 0;
		moveY = 0;
		sv.setMove(moveX, moveY);
		mZoomMode = ZOOMMODE_NORMAL;
		checkZoomMode(-1);
		oldRate = 1;
		oldDistanceX = 0;
		oldDistanceY = 0;
		
		sv.resetCenter();
//		disableMove = false;
		
	}
	
	class QuickMenuPhotoAdapter extends BaseAdapter {
		public View LastSelectedItem_View = null;
		int[] menu_name = new int[] { 
				R.string.quick_menu_photo_intervalTime,
				R.string.quick_menu_detail, 
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
			
			if(position == 1){
        		DLNAFileInfo info = mPhotoList.get(m_currentPlayIndex);
        		convertView = mInflater.inflate(R.layout.photo_detail_row, null);
        		TextView filename = (TextView)convertView.findViewById(R.id.filename_info);
        		TextView date = (TextView)convertView.findViewById(R.id.date_exif_info);
        		TextView size = (TextView)convertView.findViewById(R.id.size_exif_info);

        		filename.setText(info.getFileName());
        		date.setText( mPhotoList.get(m_currentPlayIndex).getDateExif());
        		size.setText(DLNADataProvider.queryDataByFile(
        				info.getFileName(), DLNADataProvider.UPNP_DMP_RES_RESOLUTION, null));

        		return convertView;
        	}

			if (convertView == null) {

				if (true == RTKDMPConfig.getRight2Left(getApplicationContext()))
					convertView = mInflater.inflate(R.layout.quick_list_row_a,null);
				else
					convertView = mInflater.inflate(R.layout.quick_list_row,null);

				holder = new ViewHolder();
				holder.menu_name = (TextView) convertView.findViewById(R.id.menu_name);
				holder.menu_option = (TextView) convertView.findViewById(R.id.menu_option);
				holder.left = (ImageView) convertView.findViewById(R.id.left_arrow);
				holder.right = (ImageView) convertView.findViewById(R.id.right_arrow);
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



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent it) {
		Log.v(TAG,"OnActivityResult: "+requestCode+ " "+ resultCode);
		order++;
		if(sv == null)
		{
			sv = (PictureSurfaceView)findViewById(R.id.picture_focused);
			sv.setOnTouchListener(new View.OnTouchListener() {  
				  
	            @Override  
	            public boolean onTouch(View v, MotionEvent event) {  
	                // TODO Auto-generated method stub  
	                return false; 
	            }  
	        });
		}
		sv.setHasBitmap(null,0,order);
		switch(requestCode)
		{
			case RESULT_PHOTOPLAY:
			{
				switch (resultCode) 
				{ 
					case RESULT_VAL:
					{
						Bundle b=it.getExtras();  
						boolean playtoend = b.getBoolean("playtoend",false);
						if(playtoend == true)
						{
							this.finish();
		    				return;
						}
						
						m_currentPlayIndex = b.getInt("playindex",m_currentPlayIndex);
						mRepeatMode = b.getInt("repeat",mRepeatMode);
						mZoomMode   = b.getInt("zoommode",-1);
						mRotateMode = b.getInt("rotate",mRotateMode);
						isReturnFromPlay_zoom = mZoomMode;
						isReturnFromPlay_rotate = mRotateMode;
						boolean needShowMenu = b.getBoolean("menu",false);
						if(needShowMenu == true)
						{
							mQuickMenu.showAtRTop(DensityUtil.dip2px(getApplicationContext(), 14),
									DensityUtil.dip2px(getApplicationContext(), 72),318);
							mQuickMenu.setTimeout();
						}
						//rate = b.getFloat("scalerate",rate);
						//sv.setScale(rate);
						
						child_start = m_currentPlayIndex + 1;
						isFirstEnter=true;
						break;
					}
			
					default:
					          break;
				}
				
				
				break;
			
			}
			
			default :
				break;
				
		}
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
}
