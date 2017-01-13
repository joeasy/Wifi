package com.realtek.bitmapfun.util;

import com.realtek.bitmapfun.util.*;
import com.rtk.dmp.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

/**
 * @author Eugene Popovich
 */
public abstract class CommonFrargmentWithImageWorker extends Fragment {
    protected ImageWorker mImageWorker;

    protected abstract void initImageWorker();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initImageWorker();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    
    
    @Override
    public void onDestroyView() {
    	
    	Log.d("CommonFrargmentWithImageWorker","onDestroyView");
    	super.onDestroyView();
        if (mImageWorker != null && mImageWorker.getImageCache() != null)
        {
        	Log.d("CommonFrargmentWithImageWorker","destory mImageWorker:"+mImageWorker.toString());
            mImageWorker.getImageCache().clearMemoryCache();
            mImageWorker = null;
            System.gc();
        }
    }
    
    @Override
    public void onAttach(Activity activity) {       
        super.onAttach(activity); 
    }
    
    @Override
    public void onResume()
    {
    	if (mImageWorker != null)
        {
            mImageWorker.setExitTasksEarly(false);
        }
        else
        {
        	initImageWorker();
        }	
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d("CommonFrargmentWithImageWorker","onPause: "+mImageWorker.toString());
        if (mImageWorker != null)
        {
            mImageWorker.setExitTasksEarly(true);
        }

    }
}
