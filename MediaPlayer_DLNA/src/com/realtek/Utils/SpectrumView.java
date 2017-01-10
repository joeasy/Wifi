package com.realtek.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class SpectrumView extends View  
{  
	private float max = 90;
	private int linesnums = 30;
	private int ratenums = 15;
    private long[] mBytes;  
    private float[] mPoints;  
    private float[] mPoints1;  
    private float[] mPoints2;  
    private Rect mRect = new Rect();  
    private Paint mForePaint = new Paint(); 
    private Paint mForePaint1 = new Paint(); 
    private Paint mForePaint2 = new Paint(); 

    public SpectrumView(Context context)  
    {  
        super(context);   
        init();  
    }
	public SpectrumView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public SpectrumView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(); 
	}
    private void init()  
    {  
        mBytes = null;  

        mForePaint.setStrokeWidth(4f);  
        mForePaint.setAntiAlias(true);  
        mForePaint.setColor(Color.rgb(130,172,223)); 
        
        mForePaint1.setStrokeWidth(4f);  
        mForePaint1.setAntiAlias(true);  
        mForePaint1.setColor(Color.rgb(87,155,230)); 
        
        mForePaint2.setStrokeWidth(4f);  
        mForePaint2.setAntiAlias(true);  
        mForePaint2.setColor(Color.rgb(44,129,222)); 
    }  

    public void updateSpectrum(long[] fft)  
    {    
        mBytes = fft;  
        invalidate();  
    }  

    @Override  
    protected void onDraw(Canvas canvas)  
    {  
        super.onDraw(canvas);  

        if (mBytes == null)  
        {  
            return;  
        }  

        if (mPoints == null || mPoints.length < mBytes.length * 4)  
        {  
            mPoints = new float[mBytes.length * 40];  
        }  
        if (mPoints1 == null || mPoints1.length < mBytes.length * 4)  
        {  
            mPoints1 = new float[mBytes.length * 40];  
        } 
        if (mPoints2 == null || mPoints2.length < mBytes.length * 4)  
        {  
            mPoints2 = new float[mBytes.length * 40];  
        } 
        mRect.set(0, 0, getWidth(), getHeight());  
        final int baseY = mRect.height()/linesnums; 
        final int height = mRect.height();  
        
        //for tsb ui
        final int basex = mRect.width()/ratenums; 
        int total =0;
        int total1 =0;
        int total2 =0;
        for (int i = 0; i < ratenums ; i++)  
        {     
        	int index = i+1;
            final int x1 = basex*i + 1;
            final int x2 = basex*(i+1)-3;
            if(mBytes[index] >0 || mBytes[index] <-90)
            {
            	mBytes[index] = 0;
            }
            else
            	mBytes[index] += 90;
            int num = (int) ((mBytes[index] /max) * linesnums);
            for(int j = 0; j<num;j++)
            {
            	final int y1 = baseY*j + baseY/2;
            	if(j<10)
            	{
                mPoints[total * 4] = x1;  
                mPoints[total * 4 + 1] = height - y1;  
                  
                mPoints[total * 4 + 2] = x2;  
                mPoints[total * 4 + 3] = height - y1; 
                total ++;
            	}else if(j<20)
            	{
                    mPoints1[total1 * 4] = x1;  
                    mPoints1[total1 * 4 + 1] = height - y1;  
                      
                    mPoints1[total1 * 4 + 2] = x2;  
                    mPoints1[total1 * 4 + 3] = height - y1; 
                    total1 ++;
            	}else
            	{
                    mPoints2[total2 * 4] = x1;  
                    mPoints2[total2 * 4 + 1] = height - y1;  
                      
                    mPoints2[total2 * 4 + 2] = x2;  
                    mPoints2[total2 * 4 + 3] = height - y1; 
                    total2 ++;
            	}
            }
        }
        canvas.drawLines(mPoints, 0,total*4,mForePaint);  
        canvas.drawLines(mPoints1, 0,total1*4,mForePaint1);
        canvas.drawLines(mPoints2, 0,total2*4,mForePaint2);
    }  
}  
