package com.rtk.dmp;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ConfirmMessage extends PopupWindow{
	
	private Activity context;
	private RelativeLayout rp = null;
	public TextView message = null;
	public ImageButton confirm_bt = null;
	public ImageView left = null;
	public ImageView right = null;
	public TextView confirm_text = null;
	public TextView confirm_title = null;
	
	LayoutInflater mInflater=null;
	
	OnKeyListener keyListener = new OnKeyListener() {
		@Override
		public boolean onKey(View arg0,  int keyCode, KeyEvent event) {	
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				switch(keyCode)
				{
					case KeyEvent.KEYCODE_DPAD_LEFT:
						if(confirm_text.getText().toString().compareTo(context.getResources().getString(R.string.msg_yes))==0)
							confirm_text.setText(context.getResources().getString(R.string.msg_no));
						else if(confirm_text.getText().toString().compareTo(context.getResources().getString(R.string.msg_no))==0)
							confirm_text.setText(context.getResources().getString(R.string.msg_yes));
						break;
						
					case KeyEvent.KEYCODE_DPAD_RIGHT:
						if(confirm_text.getText().toString().compareTo(context.getResources().getString(R.string.msg_yes))==0)
							confirm_text.setText(context.getResources().getString(R.string.msg_no));
						else if(confirm_text.getText().toString().compareTo(context.getResources().getString(R.string.msg_no))==0)
							confirm_text.setText(context.getResources().getString(R.string.msg_yes));
						break;
	
					default:
						break;
				}
        	}
	
			return false;
		}	
	};
	
	/*OnDismissListener dismissListener = new OnDismissListener(){

		@Override
		public void onDismiss() {
			// TODO Auto-generated method stub
			context.finish();
		}
		
	};*/
	
	ConfirmMessage(Activity mContext)
	{
		super(mContext);
		
		int height = 260;
		int width = 678;
		setHeight(height);
		setWidth(width);
		
		this.context=mContext;
		
		mInflater = LayoutInflater.from(context);
	    rp=(RelativeLayout) mInflater.inflate(R.layout.confirm_msg, null);
	    
	    message = (TextView)rp.findViewById(R.id.message);
	    confirm_bt =(ImageButton)rp.findViewById(R.id.comfirm_bt_bg);
	    confirm_text = (TextView)rp.findViewById(R.id.confirm_text);
	    confirm_title = (TextView)rp.findViewById(R.id.confirm_title);
	    left=(ImageView)rp.findViewById(R.id.confirm_left);
	    right=(ImageView)rp.findViewById(R.id.confirm_right);

		setContentView(rp);	
	}
	
	ConfirmMessage(Activity mContext,int width,int height)
	{
		super(mContext);
	
		setHeight(height);
		setWidth(width);
		
		this.context=mContext;
		
		mInflater = LayoutInflater.from(context);
	    rp=(RelativeLayout) mInflater.inflate(R.layout.confirm_msg, null);
	    
	    message = (TextView)rp.findViewById(R.id.message);
	    confirm_bt =(ImageButton)rp.findViewById(R.id.comfirm_bt_bg);
	    confirm_text = (TextView)rp.findViewById(R.id.confirm_text);
	    confirm_title = (TextView)rp.findViewById(R.id.confirm_title);
	    left=(ImageView)rp.findViewById(R.id.confirm_left);
	    right=(ImageView)rp.findViewById(R.id.confirm_right);

		setContentView(rp);	
	}
	
	public void setTitle(String s)
	{
		confirm_title.setText(s);
	}

	public void setMessage(String s)
	{
		message.setText(s);
	}
	
	public void setButtonText(String s)
	{
		confirm_text.setVisibility(View.VISIBLE);
		confirm_text.setText(s);
	}
	
	public void setKeyListener(boolean isExist)
	{
		if (isExist)
			confirm_bt.setOnKeyListener(keyListener);
		else
			confirm_bt.setOnKeyListener(null);
	}
	
	public void setMessageLeft()
	{
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) message.getLayoutParams();
		//lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		//message.setGravity(Gravity.LEFT);
		message.setLayoutParams(lp);
		message.setX(20);
	}
	
	public void setMessageCenterHorizotal()
	{
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) message.getLayoutParams();
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		message.setGravity(Gravity.CENTER);
		message.setLayoutParams(lp);
	}
	
	/*public void setDismissListener(ConfirmMessage msg, boolean isExist)
	{
		if (isExist)
			msg.setOnDismissListener(dismissListener);
		else
			msg.setOnDismissListener(null);
	}*/
	
	public void show()//40,850
	{
		this.setFocusable(true);
		this.setOutsideTouchable(true);
		this.showAtLocation(rp, Gravity.CENTER, 0, 0);
	}
}
	
