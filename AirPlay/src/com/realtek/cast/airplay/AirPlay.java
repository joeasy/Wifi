
package com.realtek.cast.airplay;


public class AirPlay {
	public static final String TAG = "AirPlay";
	
	public static final String TYPE = "_airplay._tcp.local.";
	
	public static final int PORT = 7000;
	
	public static final String MODEL = 
			"AppleTV2,2";
//			"AppleTV3,2";
	
	public static final String SRCVERS =
//			"130.14";
			"150.33";
	
	public static final short FEATURES =
//			0x39f7;
//			0x77;
//			0x207D;
			AirPlay.FEATURE_VIDEO | 
			AirPlay.FEATURE_PHOTO |
//			AirPlay.FEATURE_VIDEO_FAIR_PLAY |
			AirPlay.FEATURE_VIDEO_VOLUME_CONTROL |
			AirPlay.FEATURE_VIDEO_HTTP_LIVE_STREAM |
			AirPlay.FEATURE_SLIDESHOW |
//			AirPlay.FEATURE_SCREEN |
			AirPlay.FEATURE_AUDIO |
//			AirPlay.FEATURE_FPSAPv2pt5_AES_GCM |
			AirPlay.FEATURE_PHOTO_CACHING;
	
	public static final short FEATURE_VIDEO = 0x1;
	public static final short FEATURE_PHOTO = 0x2;
	public static final short FEATURE_VIDEO_FAIR_PLAY = 0x4;
	public static final short FEATURE_VIDEO_VOLUME_CONTROL = 0x8;
	
	public static final short FEATURE_VIDEO_HTTP_LIVE_STREAM = 0x10;
	public static final short FEATURE_SLIDESHOW = 0x20;
	
	public static final short FEATURE_SCREEN = 0x80;
	
	public static final short FEATURE_SCREEN_ROTATE = 0x100;
	public static final short FEATURE_AUDIO = 0x200;
	
	public static final short FEATURE_AUDIO_REDUDANT = 0x800;
	
	public static final short FEATURE_FPSAPv2pt5_AES_GCM = 0x1000;
	public static final short FEATURE_PHOTO_CACHING = 0x2000;
	
}
