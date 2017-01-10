package com.rtk.dmp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class Menu extends PopupWindow {
	private int mHeight, mWidth;
	private LayoutInflater mInflater = null;
	private LinearLayout mlayout = null;
	private ListView mlist = null;

	Menu(Context context, ListAdapter listviewAdapter) {
		super(context);
		mHeight = listviewAdapter.getCount() * 59;
		mWidth = 468;

		setHeight(mHeight);
		setWidth(mWidth);

		mInflater = LayoutInflater.from(context);
		mlayout = (LinearLayout) mInflater.inflate(R.layout.menu, null);

		mlist = (ListView) mlayout.findViewById(R.id.list);
		mlist.setAdapter(listviewAdapter);

		setFocusable(true);
		setContentView(mlayout);
	}

	void showMenu(int x, int y) {
		setFocusable(true);
		setOutsideTouchable(true);
		showAtLocation(mlayout, Gravity.RIGHT | Gravity.TOP, x, y);
	}

	public void setOnItemClickListener(OnItemClickListener menuItemClickListener) {
		mlist.setOnItemClickListener(menuItemClickListener);
	}

	public void setOnItemSelectedListener(
			OnItemSelectedListener menuItemSelectedListener) {
		mlist.setOnItemSelectedListener(menuItemSelectedListener);
	}
}
