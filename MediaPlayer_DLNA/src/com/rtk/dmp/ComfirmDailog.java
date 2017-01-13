package com.rtk.dmp;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ComfirmDailog extends PopupWindow{
	private Activity context;
	private RelativeLayout rp = null;
	public TextView message = null;
	public Button confirm_yes = null;
	public Button confirm_no = null;
	
	LayoutInflater mInflater=null;
	
	ComfirmDailog (Activity mContext)
	{
		super(mContext);
		
		int height = 260;
		int width = 678;
		setHeight(height);
		setWidth(width);
		
		this.context=mContext;
		
		mInflater = LayoutInflater.from(context);
	    rp=(RelativeLayout) mInflater.inflate(R.layout.comfirm_dailog, null);
	    
	    message = (TextView)rp.findViewById(R.id.message);
	    confirm_yes = (Button)rp.findViewById(R.id.comfirm_yes);
	    confirm_no = (Button)rp.findViewById(R.id.comfirm_no);
	    setContentView(rp);	
	}
	
	ComfirmDailog (Activity mContext, int width,int height)
	{
		super(mContext);
		
		setHeight(height);
		setWidth(width);
		
		this.context=mContext;
		
		mInflater = LayoutInflater.from(context);
	    rp=(RelativeLayout) mInflater.inflate(R.layout.comfirm_dailog, null);
	    
	    message = (TextView)rp.findViewById(R.id.message);
	    confirm_yes = (Button)rp.findViewById(R.id.comfirm_yes);
	    confirm_no = (Button)rp.findViewById(R.id.comfirm_no);
	    setContentView(rp);	
	}
	
	public void setMessage(String s)
	{
		message.setText(s);
	}
	
	public void setMessageLeft()
	{
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) message.getLayoutParams();
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		message.setGravity(Gravity.LEFT);
		message.setLayoutParams(lp);
	}
	
	public void setMessageRight()
	{
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) message.getLayoutParams();
		lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		message.setGravity(Gravity.RIGHT);
		message.setLayoutParams(lp);
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
	
	public void show()//40,850
	{
		this.setFocusable(true);
		this.setOutsideTouchable(true);
		this.showAtLocation(rp, Gravity.CENTER, 0, 0);
	}
}
