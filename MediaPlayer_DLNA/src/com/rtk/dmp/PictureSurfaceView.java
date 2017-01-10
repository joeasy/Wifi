package com.rtk.dmp; 


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PictureSurfaceView extends SurfaceView implements SurfaceHolder.Callback,Runnable{ 

	private static String TAG="PictureSurfaceView";
	private final static int UPDATETIME = 50;
	private static final int FRAME_TYPE_FADE_OUT = 2;
	private static final int FRAME_TYPE_NO_FADE = 0;
	private static final int FRAME_TYPE_FADE_IN = 1;
	
	private static final int VIEW_NORMAL = 0;
	private static final int VIEW_BEGIN = 1;
	private static final int VIEW_END =2;
	
	private int viewMode = 1;
 
	DisplayMetrics dm;
    
	boolean mbLoop = false; 
	Bitmap bmp_show = null;
	Bitmap bmp_new = null;
	int order = -10;
	boolean isCanvasFilled =false;


	SurfaceHolder mSurfaceHolder = null; 
	int miCount = 0; 
	int y =50; 
	private float rate = 1;
	private float preScale = 0f;

	private float moveX = 0f;
	private float moveY = 0f;
	private boolean isFirst = true;
	private boolean canDrag = false;
	
	private float XscaleCenter = 0f;
	private float YscaleCenter = 0f;
	private float XoldScaleCenter = 0f;
	private float YoldScaleCenter = 0f;
	private float XoriCenter = 0f;
	private float YoriCenter = 0f;
	private float XModification = 0f;
	private float YModification = 0f;
	private float XRevertModification = 0f;
	private float YRevertModification = 0f;
	private boolean hasModifyTranslate = false;
			
	
	//player rotate mode
	private int degrees = 0;
	private final static int ROTATEMODE_0D = 0;
	private final static int ROTATEMODE_90D = 90;
	private final static int ROTATEMODE_180D = 180;
	private final static int ROTATEMODE_270D = 270;
	
	private GestureDetector gestureScanner;
	
	//for effect 
	
	// effect last time
	private int duration;
	// update surfaceview time interval
	private int updateTime;
	// effect listener
	private EffectListener effectListener;
	
	
	///////////for Fade effect
	// Alpha extream value
		protected static final int ALPHA_MIN = 0;
		protected static final int ALPHA_MAX = 255;

		// effect confirm flag
		protected boolean isFadeRunning = false;

		// effect listener
		private FadeListener fadeListener;

		// target alpha
		private int alphaFrom;

		// origen alpha
		private int alphaTo;

		

		// change of alpha per time
		private int alphaStep;

		// current effect time
		private int fadeTime;

		// current alpha
		private int fadeAlpha;
		
		private int frameType;
		
		///////////for ROTATE_SCALE effect
		////////////////////////common
		protected boolean isRotateScaleRunning = false;
		private int effectTime_of_RotateScale;
		private int effectType_of_RotateScale;
		private int direct_of_RotateScale;
		///////////////////////rotate
		
		private float current_rotate_of_RotateScale;
		private int num_rotate;
		private float rotateStep_of_RotateScale;
		//////////////////////scale
		private float current_scale_of_RotateScale;
		private float scaleStep_of_RotateScale;
	
	public PictureSurfaceView(Context context) { 
		super(context); 
		

		mSurfaceHolder = this.getHolder(); 
		
		setLongClickable(true);
		mSurfaceHolder.addCallback(this); 
		this.setFocusable(true); 
		mbLoop = true; 
	} 
	

	
	public PictureSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        updateTime = UPDATETIME;
        mSurfaceHolder = this.getHolder(); 

		mSurfaceHolder.addCallback(this); 
		this.setFocusable(true); 
		mbLoop = true;

		setLongClickable(true);

		gestureScanner = new GestureDetector(context, new GestureDetector.OnGestureListener(){

			@Override
			public boolean onDown(MotionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("onDown");
				return false;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				// TODO Auto-generated method stub
				System.out.println("onFling");
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("onLongPress");
				
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				// TODO Auto-generated method stub
				System.out.println("onScroll");
				return true;
			}

			@Override
			public void onShowPress(MotionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("onShowPress");
				
			}

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("onSingleTapUp");
				return false;
			}
			
		});
		gestureScanner.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
			
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("double + onSingleTapConfirmed");
				return false;
			}
			
			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("double + nDoubleTapEvent");
				return false;
			}
			
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("double + onDoubleTap");
				return false;
			}
		});

    }
	
	public void setHasBitmap(Bitmap bmp,int viewMode,int order)
	{
		System.out.println(order);
		this.viewMode = viewMode;
		this.frameType = viewMode;
		this.effectType_of_RotateScale = viewMode;
		if(bmp != null)
			isCanvasFilled = true;
		else
			isCanvasFilled = false;
		
		if(this.order<order)
		{
			if(viewMode == 0)
			{
				this.bmp_show = bmp;
			}
			if(viewMode == 1)
			{
				this.bmp_show = bmp;
			}
			else if(viewMode == 2)
			{
				this.bmp_new = bmp;
			}
			this.order = order;
		}
		else
		{
			Log.v(TAG, "earlier Bitmap abandoned!");
		}
	}
	public boolean isCanvasFilled()
	{
		return isCanvasFilled;
	}
	public void setScale(float rate)
	{
		this.rate = rate;
	}
	public void resetCenter()
	{
		this.XoriCenter = 0;
		this.YoriCenter = 0;
		this.XscaleCenter = 0;
		this.YscaleCenter = 0;
		this.XoldScaleCenter = 0;
		this.YoldScaleCenter = 0;
		this.XModification = 0;
		this.YModification = 0;
	}
	public void setScaleCenter(float x,float y)
	{
		float XRotate = 0f;
	    float YRotate = 0f;
		switch(degrees)
		{
			case ROTATEMODE_0D:
			{
				XRotate = x;
				YRotate = y;
				break;
			}
			case ROTATEMODE_90D:
			{
				XRotate = y + getWidth()/2  - getHeight()/2;
				YRotate = -x + getHeight()/2 +getWidth()/2;
				break;
			}
			case ROTATEMODE_180D:
			{
				XRotate = getWidth() - x;
				YRotate = getHeight() - y;
				break;
			}
			case ROTATEMODE_270D:
			{
				XRotate = getWidth()/2 - y+getHeight()/2;
				YRotate = getHeight()/2 + x-getWidth()/2;
				break;
			}
		
		}
		this.XoriCenter = XRotate;
		this.YoriCenter = YRotate;
		this.XscaleCenter = XoldScaleCenter+(XoriCenter-XModification-XoldScaleCenter)/rate;
		this.YscaleCenter = YoldScaleCenter+(YoriCenter-YModification-YoldScaleCenter)/rate;
		this.XoldScaleCenter = XscaleCenter;
		this.YoldScaleCenter = YscaleCenter;
		this.XModification = XoriCenter-XscaleCenter;
		this.YModification = YoriCenter-YscaleCenter;
	}
	
	public void setMove(float x , float y)
	{
		this.moveX = x;
		this.moveY = y;
	}
	
	public void setRotate(int degrees)
	{
		this.degrees = degrees;
	}

	@Override 
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { 
		
		Log.v(TAG, "surfaceChanged");
	} 
	

	@Override 
	public void surfaceCreated(SurfaceHolder holder) { 

		Log.v(TAG, "surfaceCreated");
		frameType = FRAME_TYPE_FADE_OUT;
		mbLoop = true;
		new Thread(this).start(); 
	} 
	

	@Override 
	public void surfaceDestroyed(SurfaceHolder holder) { 

		Log.v(TAG, "surfaceDestroyed");
		mbLoop = false; 
		isCanvasFilled = false;
	} 
	
 
	@Override 
	public void run() { 
		while (mbLoop){ 
			long start = System.currentTimeMillis();
			synchronized( mSurfaceHolder ){ 
				Draw(); 
			} 
			long end = System.currentTimeMillis();
			try {
				if (end - start < updateTime) {
					Thread.sleep(updateTime - (end - start));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} 
	} 
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.v(TAG,"onTouchEvent");
		super.onTouchEvent(event);
		
		return gestureScanner.onTouchEvent(event);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
/*		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			 Animation mAlphaAnimation;
		
	        mAlphaAnimation = new AlphaAnimation(0.1f, 1.0f);  
	        mAlphaAnimation.setDuration(3000);  
	        this.startAnimation(mAlphaAnimation);  
		}
*/
		return super.onKeyDown(keyCode, event);
	}
	
	
	
	public void Draw(){ 
		
		Canvas canvas = mSurfaceHolder.lockCanvas(); 
		if (mSurfaceHolder==null || canvas == null) { 
			return; 
		} 
		if (miCount < 100) { 
			miCount++; 
		}else { 
			miCount = 0; 
		} 
		Paint mPaint = new Paint(); 
		mPaint.setAntiAlias(true); 
		mPaint.setColor(Color.BLACK); 
		//canvas.drawRect(0, 0, 1920, 1080, mPaint);
		//if screen is set to vertical mode,the canvas area is 1080*1920
		canvas.drawRect(0, 0,1920,1920,mPaint);
		if(bmp_show ==  null)
		{
			//Todo: loading icon function . 
			;
		}
		else
		{
			float wScale = (float)(getWidth())/(float)(bmp_show.getWidth());
			float hScale = (float)(getHeight())/(float)(bmp_show.getHeight());
			
			float translate_for_scaleX = 0f;
			float translate_for_scaleY = 0f;

			
			if(hScale < wScale)
			{
				//use hScale
				preScale = hScale;
				translate_for_scaleX = getWidth() / 2 - bmp_show.getWidth()*preScale / 2;
			}
			else
			{
				//use wScale
				preScale = wScale;
				translate_for_scaleY = getHeight() / 2 - bmp_show.getHeight()*preScale / 2;
			}
			
			Matrix matrix=new Matrix();
			
	        
	        matrix.preTranslate(translate_for_scaleX, translate_for_scaleY);
	        matrix.preScale(preScale, preScale);

	        matrix.postScale(
	        		rate,
	        		rate,
	        		XscaleCenter - moveX,
	        		YscaleCenter - moveY
	        		);
	     //   System.out.println(XscaleCenter+"xxxxxxxxxxxx");

	        matrix.postTranslate(XModification, YModification);
	        
	        matrix.postRotate(degrees,getWidth()/2 - moveX,getHeight()/2 - moveY);
    		
	        matrix.postTranslate(moveX, moveY);
	        
	        
		//	canvas.drawBitmap(bmp_show, matrix, mPaint);
			
		//	effect_Fade(canvas,mPaint);
			effect_RotateScale(canvas,matrix,mPaint);
		}
		mSurfaceHolder.unlockCanvasAndPost(canvas); 
	}
	
	private void effect_RotateScale(Canvas canvas, Matrix matrix, Paint paint) {
		// TODO Auto-generated method stub
		if(viewMode == VIEW_NORMAL)
		{
			canvas.drawBitmap(bmp_show, matrix, paint);//do nothing,just draw
		}
		else if(viewMode == VIEW_BEGIN)
		{
			if(!isRotateScaleRunning) {
				if(effectType_of_RotateScale == FRAME_TYPE_NO_FADE ||effectType_of_RotateScale == FRAME_TYPE_FADE_OUT)
				{
					;
				}
				else if(effectType_of_RotateScale == FRAME_TYPE_FADE_IN)
				{
					matrix.postScale(0,0,getWidth()/2,getHeight()/2);
					configRotateScaleAction(1500, 4, 1,ALPHA_MAX, ALPHA_MIN, updateTime);
					setEffectListener(new EffectListener(){

						@Override
						public void EffectOver() {
							isRotateScaleRunning = false;
							viewMode = VIEW_NORMAL;
							effectType_of_RotateScale = FRAME_TYPE_NO_FADE;
							setEffectListener(null);
						}
					});
					
				}
			}
			else
			{
				confirmRotateScaleAction(canvas,matrix, paint);
			}
			canvas.drawBitmap(bmp_show, matrix, paint);
		}
		else if(viewMode == VIEW_END)
		{
			if(!isRotateScaleRunning) {	
				if(effectType_of_RotateScale == FRAME_TYPE_FADE_OUT) 
				{
					configRotateScaleAction(1500, 4,-1,ALPHA_MIN, ALPHA_MAX, updateTime);
					setEffectListener(new EffectListener(){

						@Override
						public void EffectOver() {
							isRotateScaleRunning = false;
							setEffectListener(null);
							viewMode = VIEW_BEGIN;
							effectType_of_RotateScale = FRAME_TYPE_FADE_IN;
							bmp_show = bmp_new;
						}
						
					});
				} 
				else if(effectType_of_RotateScale == FRAME_TYPE_FADE_IN || effectType_of_RotateScale == FRAME_TYPE_NO_FADE) 
				{
					;
				}
			} 
			else
			{
				confirmRotateScaleAction(canvas,matrix, paint);
			}
			canvas.drawBitmap(bmp_show, matrix, paint);
		}
		
	}

	private void effect_Fade(Canvas canvas, Paint paint) {
		// TODO Auto-generated method stub
		if(viewMode == VIEW_NORMAL)
		{
			;//do nothing
		}
		else if(viewMode == VIEW_BEGIN)
		{
			if(!isFadeRunning) {
				if(frameType == FRAME_TYPE_NO_FADE ||frameType == FRAME_TYPE_FADE_OUT)
				{
					;
				}
				else if(frameType == FRAME_TYPE_FADE_IN)
				{
					paint.setAlpha(ALPHA_MAX);
					configFadeAction(1500, ALPHA_MAX, ALPHA_MIN, updateTime);
					setFadeListener(new FadeListener(){

						@Override
						public void fadeOver() {
							isFadeRunning = false;
							viewMode = VIEW_NORMAL;
							frameType = FRAME_TYPE_NO_FADE;
							setFadeListener(null);
						}
						
					});
					
				}
			}
			else
			{
				confirmFadeAction(canvas, paint);
			}
			canvas.drawPaint(paint);
		}
		else if(viewMode == VIEW_END)
		{
			if(!isFadeRunning) {	
				if(frameType == FRAME_TYPE_FADE_OUT) 
				{
					paint.setAlpha(ALPHA_MIN);
					configFadeAction(1500, ALPHA_MIN, ALPHA_MAX, updateTime);
					setFadeListener(new FadeListener(){

						@Override
						public void fadeOver() {
							isFadeRunning = false;
							setFadeListener(null);
							viewMode = VIEW_BEGIN;
							frameType = FRAME_TYPE_FADE_IN;
							bmp_show = bmp_new;
						}
						
					});
				} 
				else if(frameType == FRAME_TYPE_FADE_IN || frameType == FRAME_TYPE_NO_FADE) 
				{
					;
				}
			} 
			else
			{
				confirmFadeAction(canvas, paint);
			}
			canvas.drawPaint(paint);
		}
	} 
	/**
	 * config animation
	 */
	public void configFadeAction(int _duration, int _alphaFrom, int _alphaTo, int _updateTime) {
		duration = _duration;
		alphaFrom = _alphaFrom;
		alphaTo = _alphaTo;
		updateTime = _updateTime;

		fadeTime = 0;
		fadeAlpha = alphaFrom;
		alphaStep = (alphaTo - alphaFrom) / (duration / updateTime);

		isFadeRunning = true;
	}

	/**
	 * confirm animation
	 * @param canvas 
	 * @param paint
	 */
	public void confirmFadeAction(Canvas canvas, Paint paint) {
		try {
			fadeAlpha += alphaStep;

			paint.setAlpha(fadeAlpha);
//			canvas.drawPaint(paint);

			fadeTime += updateTime;
		} catch(Exception e) {
			Log.e(e.getClass().getName(), e.getMessage());
			fadeListener.fadeOver();
		} finally {
			if(fadeTime > duration) {
				fadeListener.fadeOver();
			}
		}
	}

	/**
	 * set fadListener
	 * @param fadeListener
	 */
	public void setFadeListener(FadeListener fadeListener) {
		this.fadeListener = fadeListener;
	}
	/**
	 * set effectListener
	 * @param fadeListener
	 */
	public void setEffectListener(EffectListener effectListener) {
		this.effectListener = effectListener;
	}
	
	/**
	 * config animation
	 */
	public void configRotateScaleAction(int _duration, int _num_rotate,int _direct_rotate,int scale_From ,int scale_To, int _updateTime) {
		duration = _duration;
		direct_of_RotateScale = _direct_rotate;
		updateTime = _updateTime;
		
		num_rotate = _num_rotate;
		rotateStep_of_RotateScale = num_rotate*360*direct_of_RotateScale / (duration / updateTime);
		
		current_scale_of_RotateScale =(direct_of_RotateScale ==1)?0f:1f;
		scaleStep_of_RotateScale = (float)direct_of_RotateScale/(float)(duration / updateTime);
		
		effectTime_of_RotateScale = 0;
		isRotateScaleRunning = true;
	}

	/**
	 * confirm animation
	 * @param canvas 
	 * @param matrix 
	 * @param paint
	 */
	public void confirmRotateScaleAction(Canvas canvas, Matrix matrix, Paint paint) {
		try {
			current_rotate_of_RotateScale += rotateStep_of_RotateScale;
			current_scale_of_RotateScale += scaleStep_of_RotateScale;
			if(current_scale_of_RotateScale<0)
				current_scale_of_RotateScale = 0;
			else if(current_scale_of_RotateScale >1)
				current_scale_of_RotateScale =1;
			matrix.postRotate(current_rotate_of_RotateScale, getWidth()/2,getHeight()/2);
			matrix.postScale(current_scale_of_RotateScale, current_scale_of_RotateScale, getWidth()/2, getHeight()/2);
			effectTime_of_RotateScale += updateTime;
		} catch(Exception e) {
			Log.e(e.getClass().getName(), e.getMessage());
			effectListener.EffectOver();
		} finally {
			if(effectTime_of_RotateScale > duration) {
				effectListener.EffectOver();
			}
		}
	}
	private class CenterXY
	{
		private float x;
		private float y;
		CenterXY(float x ,float y) {
			this.x=x;
			this.y=y;
		}
		public void setCenter(float x,float y)
		{
			this.x=x;
			this.y=y;
		}
		public float getX()
		{
			return this.x;
		}
		public float getY()
		{
			return this.y;
		}
		public void calcCenter(float hScale,float wScale)
		{
			if(hScale < wScale)
			{

				x = bmp_show.getWidth()*hScale / 2;
			}
			else
			{
				//use wScale
				y = bmp_show.getHeight()*wScale / 2;
			}
			
		}
	}
} 
