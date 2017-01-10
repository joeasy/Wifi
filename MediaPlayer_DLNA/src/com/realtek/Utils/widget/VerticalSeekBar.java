package com.realtek.Utils.widget;
 
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;
 
public class VerticalSeekBar extends SeekBar {
 
	private int viewWidth  = 0;
	private int viewHeight = 0; 
   public VerticalSeekBar(Context context) {
       super(context);
    }
 
   public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
       super(context, attrs, defStyle);
    }
 
    public VerticalSeekBar(Context context,AttributeSet attrs) {
       super(context, attrs);
    }
    
 
   protected void onSizeChanged(int w, int h, int oldw, int oldh) {
       super.onSizeChanged(h, w, oldh, oldw);
    }
   public void setSize(int w,int h){
   		viewWidth = w;
   		viewHeight = h;
   }
 
   @Override
   protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
       super.onMeasure(heightMeasureSpec, widthMeasureSpec);
       setMeasuredDimension(viewWidth, viewHeight);
    }
 
   protected void onDraw(Canvas c) {
       c.rotate(-90);
       c.translate(-getHeight(),0);
 
       super.onDraw(c);
    }
 
   @Override
   public boolean onTouchEvent(MotionEvent event) {
       if (!isEnabled()) {
           return false;
       }
 
       switch (event.getAction()) {
           case MotionEvent.ACTION_DOWN:
           case MotionEvent.ACTION_MOVE:
           case MotionEvent.ACTION_UP:
                    int i=0;
                    i=getMax() - (int)(getMax() * event.getY() / getHeight());
                setProgress(i);
               Log.i("Progress",getProgress()+"");
                onSizeChanged(getWidth(),getHeight(), 0, 0);
                break;
 
           case MotionEvent.ACTION_CANCEL:
                break;
       }
       return true;
    }
   
}