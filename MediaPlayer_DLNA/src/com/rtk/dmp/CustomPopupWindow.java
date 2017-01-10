package com.rtk.dmp;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

public class CustomPopupWindow {
	protected final View anchor;
	protected final PopupWindow window;
	private View root;
	private Drawable background = null;
	protected final WindowManager windowManager;
	protected int windowWidth = 868;
	
	public CustomPopupWindow(View anchor, int width) {
        this.anchor = anchor;
        this.window = new PopupWindow(anchor.getContext());
        //window.setFocusable(true);
        //window.setBackgroundDrawable(new BitmapDrawable());
        windowManager = (WindowManager) anchor.getContext().getSystemService(Context.WINDOW_SERVICE);
        if(width != -1) {
        	windowWidth = width;
        }
	}
	
	protected void onShow() {
        
    }
	
	protected void preShow() {
        onShow();
        //window.setBackgroundDrawable(background);
        window.setWidth(windowWidth);
        window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        window.setTouchable(true);
        window.setFocusable(true);
        window.setOutsideTouchable(true);
        window.setContentView(root);
    }
	
	protected boolean isShowing() {
		return window.isShowing();
	}
	
	public void setBackgroundDrawable(Drawable background) {
        this.background = background;
    }
	
	public void setContentView(View root) {
        this.root = root;
        window.setContentView(root);
    }
	
	public void setContentView(int layoutResID) {
        LayoutInflater inflator = (LayoutInflater) anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(inflator.inflate(layoutResID, null));
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        window.setOnDismissListener(listener);
    }

    public void showDropDown() {
        showDropDown(0, 0);
    }

    public void showDropDown(int xOffset, int yOffset) {
        preShow();
        //window.setAnimationStyle(R.style.Animations_PopDownMenu);
        window.showAsDropDown(anchor, xOffset, yOffset);
    }

    public void dismiss() {
        window.dismiss();
    }
}
