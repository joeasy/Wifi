package com.realtek.Utils.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class VerticalBar extends ProgressBar{
    
private int viewWidth;
private int viewHeight;

    public VerticalBar(Context context) {
        super(context);
    }
    
    public VerticalBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public VerticalBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setSize(int w,int h){
    	viewWidth = w;
        viewHeight = h;
    }
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        this.setMeasuredDimension(viewWidth, viewHeight);
    }
    
@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldw, oldh);
    }
    
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        //
        canvas.rotate(90);
        canvas.translate(0, -this.getWidth());
        super.onDraw(canvas);
    }
}
