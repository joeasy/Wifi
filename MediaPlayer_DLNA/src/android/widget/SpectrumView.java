package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class SpectrumView extends View  
{  
	private float max = 100;
	private int linesnums = 30;
    private long[] mBytes;  
    private float[] mPoints;  
    private Rect mRect = new Rect();  

    private Paint mForePaint = new Paint();  
    private int mSpectrumNum = 32;  

    public SpectrumView(Context context)  
    {  
        super(context);  
        init();  
    }
	public SpectrumView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	// 
	public SpectrumView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(); 
	}

    private void init()  
    {  
        mBytes = null;  

        mForePaint.setStrokeWidth(4f);  
        mForePaint.setAntiAlias(true);  
        mForePaint.setColor(Color.rgb(0, 128, 255));  
    }  

    public void updateSpectrum(long[] fft)  
    {    
          
          
        /*byte[] model = new byte[fft.length / 2 + 1];  

        model[0] = (byte) Math.abs(fft[0]);  
        for (int i = 2, j = 1; j < mSpectrumNum;)  
        {  
            model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);  
            i += 2;  
            j++;  
        }  */
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

        mRect.set(0, 0, getWidth(), getHeight());  

          
        //
        final int baseX = mRect.width()/mSpectrumNum;  
        final int baseY = mRect.height()/linesnums; 
        final int height = mRect.height();  

        //
       /* for (int i = 0; i < mSpectrumNum ; i++)  
        {                   
            final int xi = baseX*i + baseX/2;  
              
            mPoints[i * 4] = xi;  
            mPoints[i * 4 + 1] = height;  
              
            mPoints[i * 4 + 2] = xi;  
            mPoints[i * 4 + 3] = height - (mBytes[i] /max) * height;  
        }
        canvas.drawLines(mPoints,0,mSpectrumNum*4,mForePaint);*/
        
        
        //for tsb ui
        final int basex = mRect.width()/10; 
        int total =0;
        for (int i = 0; i < 10 ; i++)  
        {                   
            final int x1 = basex*i + 1;
            final int x2 = basex*(i+1)-3;
               
            int num = (int) ((mBytes[i] /max) * linesnums);
            for(int j = 0; j<num;j++)
            {
            	final int y1 = baseY*j + baseY/2;
                mPoints[total * 4] = x1;  
                mPoints[total * 4 + 1] = height - y1;  
                  
                mPoints[total * 4 + 2] = x2;  
                mPoints[total * 4 + 3] = height - y1; 
                total ++;
            }
        }
        canvas.drawLines(mPoints, 0,total*4,mForePaint);  
    }  
}  
