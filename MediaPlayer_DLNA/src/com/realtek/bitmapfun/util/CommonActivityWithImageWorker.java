package com.realtek.bitmapfun.util;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public abstract class CommonActivityWithImageWorker extends Activity {
    protected ImageWorker mImageWorker;

    protected abstract void initImageWorker();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initImageWorker();
    }

    
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
        if (mImageWorker != null && mImageWorker.getImageCache() != null)
        {
        	Log.d("CommonFrargmentWithImageWorker","destory mImageWorker_small:"+mImageWorker.toString());
        	mImageWorker.getImageCache().clearMemoryCache();
        	mImageWorker = null;
            System.gc();
        }
    }

    
    @Override
    public void onResume()
    {
    	if(mImageWorker != null)
    	{
    		mImageWorker.setExitTasksEarly(false);
    	}
    	else if(mImageWorker == null)
        {
        	initImageWorker();
        }	
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();

    	if(mImageWorker != null)
    	{
    		mImageWorker.setExitTasksEarly(false);
    	}

    }
}
