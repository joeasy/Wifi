package com.rtk.dmp;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.TvManager;
import android.content.Context;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnHoverListener;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.realtek.DataProvider.DLNADataProvider;
import com.realtek.DataProvider.FileFilterType;
import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.MarqueeTextView;
import com.realtek.bitmapfun.util.CommonFrargmentWithImageWorker;
import com.realtek.bitmapfun.util.ImageCache;
import com.realtek.bitmapfun.util.ImageFetcher;
import com.realtek.bitmapfun.util.ImageWorker;
import com.realtek.bitmapfun.util.LoadingControl;
import com.realtek.bitmapfun.util.ReturnSizes;

public class GridViewFragment  extends CommonFrargmentWithImageWorker 
{
    private ReturnSizes mReturnSizes;
    private GridView    mGridView=null;
    private String      TAG ="GridViewFragement"; 
    public GridViewAdapter mGridViewAdapter=null;
    
    public Context mC= null;
    
    public static final String IMAGE_CACHE_DIR = "images";       
    private int mFirstVisibleItem;
	private int mVisibleItemCount;
    public interface UiListener
    {  
        public ArrayList<DLNAFileInfo> getCurrentItemList();
        public void onItemSelected(int position);
        public void onItemClicked (int position);
        public int  getFocusIndex();
        public void onBackClicked();
        public boolean onKeyClicked(View view,int keyCode,KeyEvent event,int position,int iconNum,int firstVisibleItem,int lastVisibleItem);
        
        public void startLoadingIcon(int pos); //Added for GridViewFragment.GridViewLoadingControl.startLoading()
        public void stopLoadingIcon(int pos);  //Added for GridViewFragment.GridViewLoadingControl.stopLoading()
        
        public int[] getAddr();
        public void releaseAddr(int[] addr);
    }   
    TvManager mTv ;
    private boolean hasPliSharedMemory = false;
    int[] vAddrForDTCP = {-1,-1};
    
    private UiListener mUiCallback;
    public class GridViewLoadingControl implements LoadingControl
    {
    	private int i=0,j=0;
    	ImageView imageView = null;
    	TextView tv = null;
    	@Override
    	public void startLoading(int pos) {
    		// TODO Auto-generated method stub
    		/* //Use "mUiCallback.startLoadingIcon();" to replace these codes.
    		Message msg = new Message();
    		msg.what=0;
    		GridViewActivity.loading_icon_system_handle.sendMessage(msg);
    		*/
//    		mUiCallback.startLoadingIcon(pos);
    		
    		
    		String imageUrl=mUiCallback.getCurrentItemList().get(pos).getFilePath();

//	        hasAnimStarted=true; 
    		
  	        imageView = (ImageView)getGridView().findViewWithTag(imageUrl);
  	        imageView.setBackgroundResource(R.drawable.loadingicon);
//  	        if(imageView.getAnimation().hasStarted() == true)
//  	        	return;
  	        imageView.getAnimation().reset();
            imageView.getAnimation().startNow();
            i++;
    	}

    	@Override
    	public void stopLoading(int pos,boolean isfrom) {
    		// TODO Auto-generated method stub
    		/* //Use "mUiCallback.stopLoadingIcon();" to replace these codes.
    		Message msg = new Message();
    		msg.what=1;
    		GridViewActivity.loading_icon_system_handle.sendMessage(msg);
    		*/
    		//
//    		mUiCallback.stopLoadingIcon(pos);
    		String imageUrl = null;
    		if(pos<=mUiCallback.getCurrentItemList().size()-1)
    		{
    			imageUrl = mUiCallback.getCurrentItemList().get(pos).getFilePath();
    			
    		}
    		else 
    			return;


    		int time = 0;
    		while(true)
    		{
    			time++;
    			if (time > 10)
	  	        {
	  	        	Log.e(TAG, "Loading Icon time > 10??");
	  	        	return;
	  	        }
	  	        imageView = (ImageView)mGridView.findViewWithTag(imageUrl);
	  	        tv = (TextView)mGridView.findViewWithTag(imageUrl+"title");
	  	        if(imageView == null || imageView.getAnimation() == null)
	  	        {
	  	        	Log.e(TAG, "Loading Icon is null."+imageView+" "+time);
	  			}
	  	        else
	  	        	break;
    		}
    		imageView.getAnimation().reset();
  	        imageView.getAnimation().cancel();
  	        imageView.setBackgroundResource(0);
  	        if(isfrom == true)
  	        	tv.setText(null);
  	        else
  	        	tv.setText(mUiCallback.getCurrentItemList().get(pos).getFileName()+(String)getResources().getText(R.string.fullblank));
//  	        hasAnimStarted=false;           	

  	       // imageView.setImageResource(R.drawable.blank);
  	        j++;

    	}    	
    }
    public  GridViewLoadingControl gv_loadingcontrol=new GridViewLoadingControl();    
       
    @Override
    public void onResume()
    {
        super.onResume();        
        Log.d(TAG,"onResume:");       
        mGridViewAdapter.loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
        
    }
    
    
    @Override
    public void onDestroyView()
    {    	 
    	super.onDestroyView();
    }   
    @Override
    public void onDestroy()
    {	
    	super.onDestroy();
    	if(hasPliSharedMemory == true)
		{
    		mUiCallback.releaseAddr(vAddrForDTCP);
    		hasPliSharedMemory = false;
		}
    }
    @Override
    public void onPause()
    {   	
    	 Log.d(TAG,"onPause");
    	 int childCount = mGridView.getChildCount();
         for(int i = 0; i < childCount; i++) 
         {
        	 //Log.d(TAG,"onPause cancel decode:"+i);
             View v = mGridView.getChildAt(i);
             cancelDecode(v);
         }
         
         super.onPause();
    }    
    @Override
    public void onAttach(Activity activity) {       
        super.onAttach(activity);      
        try{
            mUiCallback = (UiListener) activity; // check if the interface is implemented
        }catch(ClassCastException e){
            e.printStackTrace();
        }
        mTv  = (TvManager)activity.getSystemService("tv");
        Log.v(TAG,"onAttach:-"+mUiCallback);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) 
    {
        Log.d(TAG,"onViewCreated inflater");        

        return inflater.inflate(R.layout.fragment_gridview, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) 
    {
        super.onViewCreated(view, savedInstanceState);                
        Log.d(TAG,"onViewCreated view");       
        
        mGridView=(GridView)(view.findViewById(R.id.gridview));
        mC = getActivity().getApplicationContext();
        setGridView();     
    }
    @Override   
    public void onActivityCreated(Bundle savedInstanceState) {   
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG,"onActivityCreated");    
        mGridViewAdapter.notifyDataSetChanged();
    } 
    public GridView getGridView()
    {
    	return mGridView;
    }
    
    public void RefreshGridView(int initPos)
    {
        Log.d(TAG,"refreshGridView");      
        mGridViewAdapter.moveLine=0;
        mGridViewAdapter.movePage=0;
        int childCount = mGridView.getChildCount();
        for(int i = 0; i < childCount; i++) 
        {
            View v = mGridView.getChildAt(i);
            if(v !=null)
            {
            	cancelDecode(v);
            }            
        }
        
        mGridView.setSelected(true); 
        mGridView.setSelection(initPos);
   
        mGridViewAdapter.notifyDataSetChanged();
        mGridView.invalidateViews();
        
        mGridViewAdapter.enterFirstTime();
    }    
    
    @SuppressWarnings("unused")
	private class GridViewOnKeyListener implements OnKeyListener
    {
    	
        public boolean onKey(View view, int keyCode, KeyEvent event)
        {
        	if(event.getAction()==KeyEvent.ACTION_DOWN)
        	{
	        	int position = mGridView.getSelectedItemPosition();
	        	int iconNum=mGridView.getCount();
	        	int firstVisibleItem=mGridView.getFirstVisiblePosition();
	        	int lastVisibleItem=mGridView.getLastVisiblePosition();
	        	return mUiCallback.onKeyClicked(view,keyCode,event,position,iconNum,firstVisibleItem,lastVisibleItem);
        	}
			return false;       
        }
    }
    
    public GridViewAdapter getGridViewAdapter()
    {
    	return mGridViewAdapter;
    }
        
    private class GridViewtemSelectedListener implements OnItemSelectedListener
    {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position , long id)
        {   
            Log.d(TAG,"GridViewAdapter focuseIndex:"+mGridView.getSelectedItemPosition());

        }
        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {
            Log.d("kellykelly","onNothingSelected:");
        }           
    }
       
    private class GridViewItemClickListener implements OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) 
        {   
 
       //	int reLocatedPosition=ReLocatePosition(position);
            mUiCallback.onItemClicked(position);
        }
    }    
      
    @Override
    protected void initImageWorker()
    {
        //Log.d("kelly","initImageWorker");
        mReturnSizes =  new ReturnSizes(156, 137);
        mImageWorker = new ImageFetcher(getActivity(), null, mReturnSizes.getWidth(),
                mReturnSizes.getHeight());
  
        mImageWorker.setImageCache(ImageCache.findOrCreateCache(getActivity(),
                IMAGE_CACHE_DIR));
        /*mImageWorker.setImageCache(ImageCache.findOrCreateCache((FragmentActivity) mContext,
                "images"));*/
        mImageWorker.setImageFadeIn(false);
    }
 //   public PicSize getSizeCache(String path)
 //   {
 //   	return null;mImageWorker.getImageCache().getSizeFromMemCache(path);
 //   }
 //   public void setSizeCache(String path,PicSize ps)
 //   {
 //   	mImageWorker.getImageCache().addPicSizeToCache(path,ps);
 //   	//mImageWorker.getImageCache().showsizecache();//for debug
 //   }
    public int ReLocatePosition(int position) {
		// TODO Auto-generated method stub
    	mGridViewAdapter.RefreshData();
    	Log.v(TAG,"move line is "+mGridViewAdapter.moveLine+", move page is "+mGridViewAdapter.movePage);
        position=position+mGridViewAdapter.moveLine*4+mGridViewAdapter.movePage*12;
        return position;
	}


	public void cancelDecode(View view)
    {
        if(view !=null)
        {    
            ImageView imageView = (ImageView)view.findViewById(R.id.grid_img);
            ImageWorker.cancelWork(imageView);
        }
    }    
    
    private void setGridView()
    {
        Log.d(TAG,"setGridView");
       
        mGridView.setOnItemSelectedListener(new GridViewtemSelectedListener());
        mGridView.setOnItemClickListener(new GridViewItemClickListener());
//      mGridView.setOnKeyListener(new GridViewOnKeyListener());
//        mGridViewAdapter = new GridViewAdapter();
        mGridViewAdapter = new GridViewAdapter( mGridView);
        mGridView.setFocusable(true);
        mGridView.requestFocus();
        mGridView.setSelection(0);
        mGridView.setSelected(true);
        mGridView.setClickable(true);

        mGridView.setAdapter(mGridViewAdapter);
        mGridView.setFocusable(true);
        mGridView.requestFocus();
        mGridView.setSelection(0);
        mGridView.setSelected(true);
        mGridView.setClickable(true);
       
    }
   
    class GridViewAdapter  extends BaseAdapter implements OnScrollListener 
    {        
    	private ArrayList<DLNAFileInfo> itemlist = null;
    	public int moveLine=0;
    	public int movePage=0;
    	public int positionCurrent=0;
    	public int pageCurrent = 0;		
    	public int countTotal = 0;
    	public int pageTotal = 0;
    	public int count = 0;
    	
    	
    	private GridView mPhotoWall;
    	
    	private boolean isFirstEnter = true;
    	
    	
    	public void enterFirstTime()
    	{
    		isFirstEnter = true;
    	}
    	GridViewAdapter(GridView photoWall)
    	{
    		
    		 mPhotoWall = photoWall;
    //		 mPhotoWall.setOnScrollListener(this);
    		 
    		 itemlist = mUiCallback.getCurrentItemList();
    		 if(itemlist != null)
    		 {
    			 countTotal = itemlist.size();
    		 }
    		 mPhotoWall.setOnScrollListener(this);
    	}
    	
    	GridViewAdapter()
    	{
    		itemlist = mUiCallback.getCurrentItemList();
    		if(itemlist != null)
    		{
    			countTotal = itemlist.size();
    			pageTotal = countTotal/12+1;
    			
    			pageCurrent=0;
    		}
    		
    		if((countTotal-(pageCurrent+1)*12) >= 0)        
            	count = 12;
            	//	mUiCallback.getCurrentItemList().GetSize();
            else
            	count = countTotal-pageCurrent*12;
    	}
    	public void RefreshData()
    	{
    		itemlist = mUiCallback.getCurrentItemList();
    		if(itemlist != null)
    		{
    			countTotal = itemlist.size();
    			pageTotal = countTotal/12+1;
    		}
    	}
        
        @Override
        public int getCount() {
        	if(mUiCallback ==null)
        	{
        		return 0;
        	}
        	else if(mUiCallback.getCurrentItemList() == null)
        	{
        		return 0;
        	}
        	else
        		return mUiCallback.getCurrentItemList().size();
        }
        
        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        class Holder  
        {  
        	public ImageView imageView;
            public MarqueeTextView tv;  
      
        } 
        private void updateView(int itemIndex,boolean isScroll)
        { 
	        int visiblePosition = mGridView.getFirstVisiblePosition(); 
	        View view = mGridView.getChildAt(itemIndex - visiblePosition);
	        if(view==null)
	        	return;
	        Holder holderOne=new Holder();
	        holderOne.tv = (MarqueeTextView) view.findViewById(R.id.grid_text);
	        
	        if(isScroll == true)
	        {
		        holderOne.tv.setFocused(true);
		        holderOne.tv.setEllipsize( android.text.TextUtils.TruncateAt.MARQUEE);
		        holderOne.tv.setMarqueeRepeatLimit(-1);
	        }
	        else
	        {
	        	holderOne.tv.setFocused(false);
		        holderOne.tv.setEllipsize( android.text.TextUtils.TruncateAt.END);
	        }
        } 
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) 
        {
            // MarqueeTextView 

            View itemView = null;
            final Holder  holder;  
            if (convertView == null)
            {   
            	holder=new Holder();
                Log.d(TAG,"getview:convertView==null :"+position); 
                // if it's not recycled, initialize some attributes
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                itemView = inflater.inflate(R.layout.item_of_gridview, null);
                holder.imageView = (ImageView)(itemView.findViewById(R.id.grid_img));
                holder.imageView.setBackgroundResource(R.drawable.loadingicon);
                holder.tv = (MarqueeTextView)(itemView.findViewById(R.id.grid_text));  
                convertView = itemView;
                convertView.setTag(holder);  
            } 
            else 
            {
                Log.d(TAG,"getview:convertView!=null :"+position); 
                itemView = convertView;
                holder=(Holder)convertView.getTag();  
                holder.imageView = (ImageView)(itemView.findViewById(R.id.grid_img));
                holder.imageView.setImageBitmap(null);
                holder.imageView.setBackgroundResource(R.drawable.loadingicon);
                holder.tv = (MarqueeTextView)(itemView.findViewById(R.id.grid_text));
                holder.tv.setText(null);
            }

            if(holder.imageView != null)
            {   
            	holder.imageView.setTag(mUiCallback.getCurrentItemList().get(position).getFilePath());
            	holder.tv.setTag(mUiCallback.getCurrentItemList().get(position).getFilePath()+"title");
                String path = null;
              //   position = position + movePage*12+moveLine*4;
                 int type = mUiCallback.getCurrentItemList().get(position).getFileType();
                 switch (type)
                 {
                     case FileFilterType.DEVICE_FILE_DIR:
                     {
                    	 holder.imageView.setBackgroundResource(R.drawable.dnla_folder_icon_b);
                          path = "android.resource://" + R.drawable.photo_list_item_folder;
                          holder.imageView.setImageResource(R.drawable.blank);
                          holder.tv.setText(mUiCallback.getCurrentItemList().get(position).getFileName()+(String)getResources().getText(R.string.fullblank));
                     }
                     break;
                     
                     case  FileFilterType.DEVICE_FILE_PHOTO:
                     {
                         Log.d(TAG,"photo :"+position); 
                    	// holder.imageView.setBackgroundResource(R.drawable.loadingicon);
                         path = mUiCallback.getCurrentItemList().get(position).getFilePath();
                     }  
                     break;
                     
                     default:
                    	 break;
                 } 

                 itemView.setOnHoverListener(new OnHoverListener(){

                	@Override
					public boolean onHover(View v, MotionEvent event) {
						// TODO Auto-generated method stub
						switch(event.getAction()){  
		   					case MotionEvent.ACTION_HOVER_ENTER:    
		                        updateView(position,true);
		                        break;  
		                    case MotionEvent.ACTION_HOVER_EXIT:   
		                        updateView(position,false);
		                        break;  
							}
							return false;
						}
                   	 
                 });
                 holder.imageView.setImageResource(R.drawable.blank);
                 Animation mAnimLoading = AnimationUtils.loadAnimation(mC, R.drawable.anim);
                 holder.imageView.setAnimation(mAnimLoading);
                 holder.imageView.getAnimation().cancel();
            }
            return itemView;        
        }
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub
			Log.v(TAG+" firstVisibleItem & visibleItemCount",firstVisibleItem+" & "+visibleItemCount);
			mFirstVisibleItem = firstVisibleItem;
			mVisibleItemCount = visibleItemCount;
			//first enter
			if (isFirstEnter && visibleItemCount > 0) {
			   loadBitmaps(firstVisibleItem, visibleItemCount);
			   isFirstEnter = false;
			}
		}
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
			if (scrollState == SCROLL_STATE_IDLE) {
				loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
			} else 
			{
				cancelAllTasks();
			}
		}   
		private void cancelAllTasks()
		{
			ImageView imageView = null;
			for (int i = mFirstVisibleItem; i < mFirstVisibleItem + mVisibleItemCount; i++) {
				String imageUrl=mUiCallback.getCurrentItemList().get(i).getFilePath();
				int type = itemlist.get(i).getFileType();
				if(type != FileFilterType.DEVICE_FILE_PHOTO)
					continue;
			
				imageView = (ImageView)mPhotoWall.findViewWithTag(imageUrl);
			//    	        gv_loadingcontrol.startLoading(i);
			
				ImageWorker.cancelPotentialWork(imageUrl, imageView);
			//		mImageWorker.loadImage(imageUrl, imageView, gv_loadingcontrol,i,false);

			}		
			
		}
		private void loadBitmaps(int firstVisibleItem, int visibleItemCount) {		
			itemlist=mUiCallback.getCurrentItemList();
			ImageView imageView = null;
			for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++)
			{

				String  filePath = mUiCallback.getCurrentItemList().get(i).getFilePath();
				if(hasPliSharedMemory ==false)
				{
					String tailStr = null;
		    		if(filePath.contains(" "))
		    		{
		    			tailStr = filePath.substring(filePath.indexOf(" "));
		    		}
		    				
		    			
	    			if(tailStr != null&&tailStr.contains("protocolinfo"))
	    			{
	    				//f = downloadBitmapDTCPIP(data);
	    				
						vAddrForDTCP = mUiCallback.getAddr();
	    				hasPliSharedMemory = true;

	    			}
					
				}
				
				int type = itemlist.get(i).getFileType();
				if(type != FileFilterType.DEVICE_FILE_PHOTO)
					continue;
				
				imageView = (ImageView)mPhotoWall.findViewWithTag(filePath);
				if(mImageWorker != null)      
				{
					mImageWorker.loadImage(filePath, imageView, gv_loadingcontrol,i,false,vAddrForDTCP);
				}
			}		
    }
	
}}
