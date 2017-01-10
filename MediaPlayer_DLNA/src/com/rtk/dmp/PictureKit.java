package com.rtk.dmp;

import java.io.File;
import java.util.Random;

import android.app.TvManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


public class PictureKit 
{
	//static Context mContext;
	public static TvManager mTV;
	private static final String TAG = "RTK_PictureKit"; 
	private static final boolean HW_DECODE = false;
	private int currentRotation = 0;
	private static int currentSwitchMode = -1;
	private static boolean zoom_move = false;
	private static String filepath;
	
	public PictureKit(Context mContext){
	      //this.mContext = mContext;
	      mTV = (TvManager) mContext.getSystemService("tv");
	      //mTV.setSource(TvManager.SOURCE_PLAYBACK);	      
		  //mTV.startDecodeImage();
		  //ForceBg(false);
	}
	
	private static void decodeHW(String filename,boolean bUpnpFile)
	{	
		int transitionType = 0;
		boolean check = true;
		//long current_source = 0;
		zoom_move = false;
		filepath = filename;
		/*current_source = mTV.getCurSourceType();
		if(current_source == 0) { //0 is SOURCE_OSD
			mTV.setSource(24);//24 is SOURCE_PLAYBACK
			Log.d(TAG,"PictureKit set source to PlayBack");
		}*/
		if(currentSwitchMode == 0) {
			transitionType = 8;//Normal : fade in/out
		}else {
			while(check == true) {
				Random dice = new Random();
				transitionType = 1+dice.nextInt(20);
				if(transitionType != 17) check = false; //Remove VIDEO_TRANSITION_BLUR(17)				
			}
		}
		
		Log.d(TAG,"decodeHW bUpnpFile :"+bUpnpFile);
		mTV.decodeImageEx(filename,transitionType,bUpnpFile);
		Log.v(TAG,"PictureKit decodeImage :"+filename);
		Log.v(TAG,"PictureKit transitionType :"+transitionType);
	}
	
	public long startPictureKit() { 
		long ret;
//		mTV.setVideoAreaOn(0, 0, 1366, 768);
	
		ret = mTV.startDecodeImage(true);
		Log.v(TAG,"Start PictureKit result :"+ret);
		return ret;
	}	
	
	public void stopPictureKit() { 	
		mTV.stopDecodeImage();
		Log.v(TAG,"Stop PictureKit");
	}
	
	public void ForceBg(boolean on) { 	
		mTV.scaler_ForceBg(on);
		Log.v(TAG,"ForceBg :"+on);
	}
	
	public long GetDecodeImageResult() {
		long ret;
		ret = mTV.getDecodeImageResult();
		Log.v(TAG,"GetDecodeImageResult :"+ret);
		return ret;
	}
	
	public static Bitmap loadPicture(String filename, DecoderInfo info) {
		//File f= new File(filename);
		//if(f.exists()){
		    Log.v(TAG,"Bitmap decodeFile :"+filename);
		    Log.v(TAG,"Bitmap decode mode :"+info.decodemode);
		    
		    Log.d(TAG,"mBrowserType:"+info.bUpnpFile);
		    
			if(info.decodemode == 0) {
				if(HW_DECODE) {
					decodeHW(filename,info.bUpnpFile);		
					return null;
				} else {
					Bitmap bitmap = BitmapFactory.decodeFile(filename);
					return bitmap;
				}
			} else if(info.decodemode == 7) {
				decodeHW(filename,info.bUpnpFile);
				return null;
			} else {
				Bitmap bm =BitmapFactory.decodeFile(filename);
				Bitmap bitmap = Bitmap.createScaledBitmap(bm, info.widthToDecoder, info.heightToDecoder, true);//Customer
				if(info.thumbprefered == 1) {
					Log.v(TAG,"Thumbnail "+info.widthToDecoder+"X"+info.heightToDecoder);
				} else {
					Log.v(TAG,"Customer size "+info.widthToDecoder+"X"+info.heightToDecoder);
				}
				return bitmap;
			} 
		//} else {
		//	Log.e(TAG,"file dosen't exist");
		//	return null;
		//}
	}  
	
	public void zoomIn() { 	
		zoom_move = true;
		mTV.zoomIn();
		Log.v(TAG,"zoom In");
	}
	
	public void zoomOut() { 
		zoom_move = true;
		mTV.zoomOut();
		Log.v(TAG,"zoom Out");
	}
	
	public void leftRotate() { 	
		mTV.leftRotate();
		Log.v(TAG,"Rotate left");
	}
	
	public void rightRotate() { 	
		mTV.rightRotate();
		Log.v(TAG,"Rotate right");
	}
	
	public void upRotate() { 	
		mTV.upRotate();
		Log.v(TAG,"Rotate up");
	}
	
	public void downRotate() { 	
		mTV.downRotate();
		Log.v(TAG,"Rotate down");
	}	
	
    public void Rotate(int rotation)
    {
    	//Log.d(TAG,"currentRotation:"+currentRotation);
    	//Log.d(TAG,"rotation:"+rotation);
    	if(zoom_move == true) {
    		zoom_move = false;
    		mTV.decodeImage(filepath,0);
    	}
    	if ((rotation - currentRotation)==270 ) {
    		//Log.v(TAG,"270 degree");
        	mTV.leftRotate();
        }
        else if ((rotation - currentRotation) > 180) {
        	//Log.v(TAG,"180 degree");
        	mTV.upRotate();
        }
        else if ((currentRotation - rotation) > 180) {            	
        	//Log.v(TAG,"Normal");
        	mTV.downRotate();
        }
        else if ((currentRotation - rotation) == 90) {            	
        	//Log.v(TAG,"revers 90 degree");
        	mTV.leftRotate();
        }
        else {            	
        	//Log.v(TAG,"90 degree");
        	mTV.rightRotate();
        }
    	currentRotation = rotation;
    }
    
    public void setSwitchMode(int mode)
    {
        currentSwitchMode = mode;
    }
    
    public int getSwitchMode()
    {
        return currentSwitchMode;
    }
    
    public long PictureKitGetSource() { 
		long type;
		type = mTV.getCurSourceType();
		Log.v(TAG,"SourceType : "+type);
		return type;
	}
}