package com.rtk.dmp;
 
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.bitmapfun.util.CommonActivityWithImageWorker;
import com.realtek.bitmapfun.util.ImageCache;
import com.realtek.bitmapfun.util.ImageFetcher;
import com.realtek.bitmapfun.util.ImageResizer;
import com.realtek.bitmapfun.util.LoadingControl;
import com.realtek.bitmapfun.util.ReturnSizes;

import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.DisplayMetrics;
import android.util.Log;
public class DmrPhotoPlayerActivity extends CommonActivityWithImageWorker
{private int test = 0;
	private MediaApplication mMediaApplicationMap = null;
	private ArrayList<DLNAFileInfo> mPhotoList;
	boolean mIsFromAnywhere=false;
	private GestureDetector gestureScanner;
	
	private int m_initPos = 0;
	private int m_totalCnt = 0;
	private int m_currentPlayIndex = 0;
	private String[] m_filePathStrArray = null;
	private PictureSurfaceView sv = null;
	private HorizontalScrollView gallery; 
	private RelativeLayout controlbar_photoplayer = null;
	private LinearLayout item_gallery;
	private int hsv_width;  
	private int child_count;  
	private int child_width;  
	private int child_show_count;  
	private int child_start;  //first show item num
	private int last_item =0;
	
	
	private int screenW, screenH;
	
	
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
	
	
	private String serverName = null;
	public ArrayList<String> DMSName = new ArrayList<String>();
	
	
	private ReturnSizes mReturnSizes;
	
	ImageCache mCache_small;
	private ImageView play_btn = null;
	
	
	DMRPhotoBroadcastReceiver bc_receiver = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.photoplayer_gallary_dmr);	
		
		play_btn = (ImageView)findViewById(R.id.play_btn);
		mCache_small = ImageCache.createCache(this,"images");
		Intent intent = getIntent();
		m_initPos = intent.getIntExtra("initPos", 0);
		m_totalCnt = intent.getIntExtra("len", 0);
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
					}
				}
			}
		}
		else 
		{
			m_initPos = intent.getIntExtra("initPos", 0);	    	
			m_filePathStrArray = intent.getStringArrayExtra("filelist");
			m_totalCnt = m_filePathStrArray.length;
		}
		// mRepeatIndex = intent.getIntExtra("repeatIndex", 0);
		// mRepeatMode = mRepeatIndex;

		if (m_initPos < 0 && m_initPos > m_totalCnt - 1) {
			m_initPos = 0;
		}
		
		m_currentPlayIndex = m_initPos;
		
	//	picture_full = (ImageView)findViewById(R.id.picture_focused);
		controlbar_photoplayer = (RelativeLayout)findViewById(R.id.controlbar_photoplayer);
		controlbar_photoplayer.getBackground().setAlpha(50);
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenW = dm.widthPixels;
		screenH = (dm.heightPixels) / 2;
		
		
		init_gsv();
		
	}

	@Override
	public void onNewIntent(Intent intent) {
		
		super.onNewIntent(intent);
		m_totalCnt = intent.getIntExtra("len", 0);
		m_currentPlayIndex = intent.getIntExtra("initPos", 0);
		m_filePathStrArray = intent.getStringArrayExtra("filelist");
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
	
		System.out.println("surfaceView touched!!!!!!!!!!!!");
		
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
	        		float distanceY = event.getY() - startPointY;
	        		moveX = distanceX+oldDistanceX;
	        		moveY = distanceY+oldDistanceY;
	        		sv.setMove(moveX,moveY);
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
					sv.setScale(rate);	
				}
				if(canSetScaleCenter)
				{
					canSetScaleCenter = false;
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
				break;
	        }
        }
        
    
        
        return false; 
	}
	@Override
	public void onResume() {
		super.onResume();
		doRegisterReceiver();
		sv = (PictureSurfaceView)findViewById(R.id.picture_focused);
		sv.setOnTouchListener(new View.OnTouchListener() {  
  
            @Override  
            public boolean onTouch(View v, MotionEvent event) {  
                // TODO Auto-generated method stub  
                return false; 
            }  
        });
		
		final Bitmap[] bmp = {null};
		synchronized(this)
   	 	{
			Thread getBitmapThread = new Thread(new Runnable(){
	
				@Override
				public void run() {
					// TODO Auto-generated method stub
					URL url = null;
					URLConnection  conn = null;
					InputStream is =null;
					try {
						url = new URL(m_filePathStrArray[m_currentPlayIndex]);
					
					conn = url.openConnection();
			        conn.connect();
			        is = conn.getInputStream();
			        
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			        
			        Bitmap b = BitmapFactory.decodeStream(is);

					//bmp[0]  = BitmapmImageWorker.getImage(m_filePathStrArray[m_currentPlayIndex], null,0,false,1);
					if(sv.isCanvasFilled()==false)
					{
						sv.setHasBitmap(b,1,0);
					}
					else
					{
						sv.setHasBitmap(b,2,0);
					}
				}			
			});
			getBitmapThread.start();
   	 	}
		

		play_btn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				DLNADataProvider.getDirectorySize();
				int totalCnt = DLNADataProvider.getItemSize();

				Bundle bundle = new Bundle();
				bundle.putString("serverName", serverName);

				bundle.putInt("initPos", m_currentPlayIndex);
				bundle.putInt("totalCnt", totalCnt);
				Intent intent = new Intent();
				intent.putExtras(bundle);
				ComponentName componetName = new ComponentName(
						"com.rtk.dmp",
						"com.rtk.dmp.PhotoPlayerActivity_Play");
				intent.setComponent(componetName);
				startActivityForResult(intent, 0);
			}
			
		});
		 
		
		System.out.println("bmpbmpbmp"+"   "+m_filePathStrArray[m_currentPlayIndex]);

   }
	private void init_gsv() {
		// TODO Auto-generated method stub
		gallery= (HorizontalScrollView)findViewById(R.id.gallery);
		item_gallery=(LinearLayout) findViewById(R.id.item_gallery);
        child_count = m_totalCnt;  
        child_show_count = 7;  
        child_start = 4;  
	}
	
	@Override  
    public void onWindowFocusChanged(boolean hasFocus) {  
        // TODO Auto-generated method stub  
        super.onWindowFocusChanged(hasFocus);  
        hsv_width = gallery.getWidth();  
        int child_width_temp = hsv_width / child_show_count;  
        if (child_width_temp % 2 != 0) {  
            child_width_temp++;  
        }  
        child_width = child_width_temp;  
        initHsvData();  
        initHsvTouch();  
        initHsvStart();  
    }  
	
	private void initHsvStart() {
		// TODO Auto-generated method stub
		final ViewTreeObserver observer = gallery.getViewTreeObserver();  
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {  
  
            @Override  
            public boolean onPreDraw() {  
                // TODO Auto-generated method stub  
                observer.removeOnPreDrawListener(this);  
                int child_start_item = child_start;  
                if ((child_start * child_width - child_width / 2 - hsv_width / 2) <= child_width) {  
                    child_start_item += child_count;  
                }  
                gallery.scrollTo(child_width * child_start_item  
                        - child_width / 2 - hsv_width / 2,  
                        gallery.getScrollY());  
                isChecked(child_start_item, true);  
                return true;  
            }  
        });  
	}


	private boolean flag_last_item = false;
	private void initHsvTouch() {
		// TODO Auto-generated method stub
		gallery.setOnTouchListener(new View.OnTouchListener() {  
			  
            private int pre_item;  
  
            @Override  
            public boolean onTouch(View v, MotionEvent event) {  
                // TODO Auto-generated method stub  
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
                	System.out.println("ACTION_MOVE X="+gallery.getScrollX()+"w="+item_gallery.getWidth());

                    flag = false;  
                    if (x <= child_width) {  
                    	gallery.scrollBy(child_width * child_count, 0);  
                        current_item += child_count;  
                        System.out.println("current_item1111111 "+current_item+" "+child_count);
                    } 
                    
                    else if (x >= (child_width * child_count * 2 - hsv_width - child_width)) 
                    {  
                    	gallery.scrollBy(-child_width * child_count, 0);  
                        current_item -= child_count;  
                        System.out.println("current_item2222222 "+current_item+" "+child_count);
                    }  
                    for(int i =current_item- 4;i<=current_item+4;i++)
                    {
                    	System.out.println("i is ::"+i);
                    	ImageView iv = (ImageView)(gallery.findViewWithTag(i-(child_count-1)*2>0?i-(child_count-1)*2:i));
                    	synchronized(this)
                   	 	{
	                   		 mImageWorker.setImageSize(200, 100);
	                   		 mImageWorker.setImageCache(mCache_small);
	                   		 mImageWorker.loadImage
	                   		 (
	                   				 m_filePathStrArray[i>(child_count-1)?(i-(child_count-1)*2>0?i-(child_count-1)*2:i-(child_count-1)):i]
	                   						 ,iv
	                   						 ,null
	                   						 ,i
	                   						 ,false
	                   						 ,null
	                   		 );
                   	 	}	
                    }
                    
                    break;  
                case MotionEvent.ACTION_UP:  
                	flag_last_item = false;
                    flag = true;  
                    gallery.smoothScrollTo(child_width  
                            * current_item - child_width / 2 - hsv_width / 2,  
                            gallery.getScrollY());  
                 //   System.out.println("tmpx==tmpx_old "+tmpx+" "+tmpx_old);
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
		for (int i = 0; i < child_count; i++) {  
            ImageView img_content = new ImageView(this);  
            img_content.setLayoutParams(new ViewGroup.LayoutParams(child_width,  
                    ViewGroup.LayoutParams.MATCH_PARENT));  

            	img_content.setImageDrawable(this.getResources().getDrawable(R.drawable.au_pause));
            	img_content.setTag(i);
            	final int tmpi= i;
            	img_content.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						
						setMiddleItem(tmpi+1);
						
						
						final Bitmap[] bmp = {null};
						synchronized(this)
				   	 	{
							Thread getBitmapThread = new Thread(new Runnable(){
					
								@Override
								public void run() {
									// TODO Auto-generated method stub
									
									URL url = null;
									URLConnection  conn = null;
									InputStream is =null;
									try {
										url = new URL(m_filePathStrArray[m_currentPlayIndex]);
									
									conn = url.openConnection();
							        conn.connect();
							        is = conn.getInputStream();
							        
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
							        
							        Bitmap b = BitmapFactory.decodeStream(is);
							        
									if(sv.isCanvasFilled()==false)
									{
										sv.setHasBitmap(b,1,0);
									}
									else
									{
										sv.setHasBitmap(b,2,0);
									}
								}			
							});
							getBitmapThread.start();
				   	 	}
						
					}
            		
            	});

            item_gallery.addView(img_content);  
        }  
		
		for (int i = 0; i < child_count; i++) {  
            ImageView img_content = new ImageView(this);  
            img_content.setLayoutParams(new ViewGroup.LayoutParams(child_width,  
                    ViewGroup.LayoutParams.MATCH_PARENT));  
            img_content.setPadding(10, 10, 10, 10);
            img_content.setBackgroundResource(R.drawable.loadingicon);
            
            if(i<=child_show_count)
            {
            	 synchronized(this)
            	 {
            		 mImageWorker.setImageSize(200, 100);
            		 mImageWorker.setImageCache(mCache_small);
            		 mImageWorker.loadImage(m_filePathStrArray[i], img_content, null,i,false,null);
            	 }
            }
            else
            {
            	img_content.setImageDrawable(this.getResources().getDrawable(R.drawable.au_pause));
            }
            img_content.setTag(i+child_count-1);
            
            final int tmpi= i;
        	img_content.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					setMiddleItem(tmpi+2+child_count-1);
					final Bitmap[] bmp = {null};
					synchronized(this)
			   	 	{
						Thread getBitmapThread = new Thread(new Runnable(){
				
							@Override
							public void run() {
								// TODO Auto-generated method stub
								
								URL url = null;
								URLConnection  conn = null;
								InputStream is =null;
								try {
									url = new URL(m_filePathStrArray[m_currentPlayIndex]);
								
								conn = url.openConnection();
						        conn.connect();
						        is = conn.getInputStream();
						        
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						        
						        Bitmap b = BitmapFactory.decodeStream(is);
						        
								if(sv.isCanvasFilled()==false)
								{
									sv.setHasBitmap(b,1,0);
								}
								else
								{
									sv.setHasBitmap(b,2,0);
								}
							}			
						});
						getBitmapThread.start();
			   	 	}
					
				}
        		
        	});
            item_gallery.addView(img_content);  
        }  
        
		
	}
	private void setMiddleItem(int itemTag)
	{
		System.out.println(itemTag+" System.out.println(itemTag);"+" "+child_width);
		System.out.println("complicate "+" "+gallery.getScrollX()+" "+(child_width* itemTag - child_width / 2 - hsv_width / 2)+" "+gallery.getScrollY());
		int tmpScrollX = gallery.getScrollX();
		int tmpDestScorllX = child_width * itemTag - child_width / 2 - hsv_width / 2;
		gallery.smoothScrollTo(tmpDestScorllX,gallery.getScrollY());  
		int num =  0;
		if(tmpScrollX - tmpDestScorllX>0)
		{
			System.out.println("+++++++++++++++++++++");
			num = (tmpScrollX - tmpDestScorllX)/child_width;
			for(int j=0,i=itemTag-3-num;j<num&&i<itemTag-3;i++,j++)// =itemTag- 4;i<=itemTag-num+1;i++)
	        {
				System.out.println("iiii+item"+i+" "+itemTag+" "+num);
	        	ImageView iv = (ImageView)(gallery.findViewWithTag(i-(child_count-1)*2>0?i-(child_count-1)*2:i));
	        	synchronized(this)
	       	 	{
	           		 mImageWorker.setImageSize(200, 100);
	           		 mImageWorker.setImageCache(mCache_small);
	           		 mImageWorker.loadImage
	           		 (
	           				 m_filePathStrArray[i>(child_count-1)?(i-(child_count-1)*2>0?i-(child_count-1)*2:i-(child_count-1)):i]
	           						 ,iv
	           						 ,null
	           						 ,i
	           						 ,false
	           						 ,null
	           		 );
	       	 	}	
	        }
		}
		else
		{
			System.out.println("---------------------");
			for(int i =itemTag+ num;i<=itemTag+4;i++)
	        {
				System.out.println("iiii+item"+i+" "+itemTag+" "+num);
	        	ImageView iv = (ImageView)(gallery.findViewWithTag(i-(child_count-1)*2>0?i-(child_count-1)*2:i));
	        	synchronized(this)
	       	 	{
	           		 mImageWorker.setImageSize(200, 100);
	           		 mImageWorker.setImageCache(mCache_small);
	           		 mImageWorker.loadImage
	           		 (
	           				 m_filePathStrArray[i>(child_count-1)?(i-(child_count-1)*2>0?i-(child_count-1)*2:i-(child_count-1)):i]
	           						 ,iv
	           						 ,null
	           						 ,i
	           						 ,false
	           						 ,null
	           		 );
	       	 	}	
	        }
		}
	}
	/** 
     *  
     * @param item 
     * @param isChecked 
     */  
    private void isChecked(int item, boolean isChecked) {  
        ImageView imageview = (ImageView) item_gallery.getChildAt(item - 1);  
        if (isChecked) {  
    //    	imageview.setBackground(this.getResources().getDrawable(R.drawable.bar));  
        } else {  
    //    	imageview.setBackground(this.getResources().getDrawable(R.drawable.av_play)); 
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
	public class GridViewLoadingControl implements LoadingControl
    {
    	@Override
    	public void startLoading(int pos) {

    	}
 
    	@Override
    	public void stopLoading(int pos,boolean isFromonCancel) {

    	}    	
    }
    public  GridViewLoadingControl full_loadingcontrol=new GridViewLoadingControl(); 


	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		doUnRegisterReceiver();
		
	}
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
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
			}
		}
		
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	private class DMRPhotoBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			if(intent.getAction().equals("com.android.DMRService.toplay"))
			{
				Bundle bundle = intent.getExtras();
				String cmd = bundle.getString("cmd");
				if(cmd.equals("Photo")) {
					//do nothing, just like I'm start
				} else {
					//stop onNewIntent
					DmrPhotoPlayerActivity.this.finish();
				}
			}
		}
		
	} 
	public void doRegisterReceiver() {
		bc_receiver = new DMRPhotoBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.android.DMRService.toplay");
		
		registerReceiver(bc_receiver, intentFilter);
	}
	public void doUnRegisterReceiver(){
		unregisterReceiver(bc_receiver);
	}

}
