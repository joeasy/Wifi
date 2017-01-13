package com.realtek.Utils.widget; 

import android.content.Context; 
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder; 
import android.widget.AbsListView;
import android.widget.HorizontalScrollView;

public class HSVtoGallery extends HorizontalScrollView { 


	DisplayMetrics dm;
    
	boolean mbLoop = false; 
	Bitmap bmp_show = null;


	SurfaceHolder mSurfaceHolder = null; 
	int miCount = 0; 
	int y =50; 

	public HSVtoGallery(Context context) { 
		super(context); 
	} 

	
	public HSVtoGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

	@Override
	protected void onScrollChanged (int l, int t, int oldl, int oldt) {
		// TODO Auto-generated method stub
		super.onScrollChanged(l, t, oldl, oldt);
	}   
} 
