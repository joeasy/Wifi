package com.rtk.dmp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.AdapterView.OnItemClickListener;

public class M_QuickMenu extends CustomPopupWindow {
	private final View root;
	private final Context context;
	private final LayoutInflater inflater;
	private ListView mListView = null;
	String TAG = "DEBUG" ;
	
	public M_QuickMenu(View anchor, ListAdapter listviewAdapter, int width) {
		super(anchor, width);
		// TODO Auto-generated constructor stub
		context = anchor.getContext();
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		root = inflater.inflate(R.layout.video_browser_quickmenu, null);
		mListView = (ListView) root.findViewById(R.id.quick_list);
		mListView.setAdapter(listviewAdapter);
		setContentView(root);
	}
	
	public void show() {
		preShow();
		int xPos, yPos;
		int[] location = new int[2];
		anchor.getLocationOnScreen(location);
		Rect anchorRect = new Rect(location[0], location[1], location[0]+ anchor.getWidth(), location[1] + anchor.getHeight());
		//root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		root.setLayoutParams(new LayoutParams(windowWidth,LayoutParams.WRAP_CONTENT));
		root.measure(windowWidth, LayoutParams.WRAP_CONTENT);
		int rootHeight = root.getMeasuredHeight();
		int rootWidth = root.getMeasuredWidth();
		int screenWidth = windowManager.getDefaultDisplay().getWidth();
		int screenHeight = windowManager.getDefaultDisplay().getHeight();
		Log.v(TAG, "rootHeight  := " + rootHeight);
		Log.v(TAG, "rootWidth := " + rootWidth);
		if ((anchorRect.left + rootWidth) > screenWidth) {
            xPos = anchorRect.left - (rootWidth - anchor.getWidth());
            Log.v(TAG, "anchorRect.left := " + anchorRect.left + "||anchor.getWidth() := " + anchor.getWidth()
            		+ "xPos := " + xPos);
        } else {
            if (anchor.getWidth() > rootWidth) {
                xPos = anchorRect.centerX() - (rootWidth / 2);
            } else {
                xPos = anchorRect.left;
            }
        }
		
		int dyTop = anchorRect.top;
        int dyBottom = screenHeight - anchorRect.bottom;

        boolean onTop = (dyTop > dyBottom) ? true : false;

        if (onTop) {
            if (rootHeight > dyTop) {
            	Log.e("NotAble SHOW", "NotAble SHOW");
                return ;
            } else {
                yPos = anchorRect.top - rootHeight;
            }
        } else {
            yPos = anchorRect.bottom;

            if (rootHeight > dyBottom) {
            	Log.e("NotAble SHOW", "NotAble SHOW");
                return ;
            }
            
            yPos += 10;
        }
        
        window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
		
	}
	
	public void AddOnItemClickListener(OnItemClickListener quickmenuItemClickListener)
	{
		mListView.setOnItemClickListener(quickmenuItemClickListener);
	}
}
