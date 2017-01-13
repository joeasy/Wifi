package com.rtk.dmp;

public class HandlerControlerVariable {
	
	public final static int PROGRESS_CHANGED = 1;
	public final static int CONTROLER_MOVE = 2;
	public final static int GET_DURATION = 3;
	public final static int HIDE_DIVX_INFO = 4;
	public final static int SET_DIVX_CHAPTER_INFO = 5;
	public final static int SET_DIVX_METADATA_LAWRATE_INFO = 6;
	public final static int SET_DIVX_METADATA_INFO = 7;
	public final static int SET_DIVX_METADATA_SUBTITLE_INFO = 8;
	public final static int SET_DIVX_METADATA_AUDIO_INFO = 9;
	public final static int SET_DIVX_METADATA_CHAPTER_INFO = 10;
	
	public final static int HIDE_MOVIE_BANNER = 11;
	public final static int HIDE_LAWRATE_METADATA = 12;
	public final static int HIDE_METADATA_NAME = 13;
	public final static int HIDE_LEFT_CORN_INFO = 14;
	
	public final static int DELAY_SHOW_BANNER = 15;
	public final static int QUICK_SHOW_BANNER = 100;
	public final static int DELAY_HIDE_BANNER = 101;
	public final static int QUICK_HIDE_BANNER = 102;
	
	
	public final static int MEDIAPLAY_INIT = 16;
	
	public final static int SET_CHAPTER_NUM = 17;
	
	public final static int HIDE_BUTTON_LIST = 18;
	
	public final static int SWITCH_TITLE_QUERY = 19;
	public final static int SWITCH_FILE_QUERY = 20;
	
	public final static int SET_DIVX_CHAPTER_NUMBER = 21;
	public final static int HIDE_PICTURE_SIZE_UI = 22;
	
	public final static int MEDIA_PLAYER_START = 23;
	public final static int MESSAGE_DISMISS = 24;
	public final static int HIDE_QUICK_MENU = 25;
	public final static int HIDE_DOLBY = 26;
	public final static int HIDE_POPUP_MESSAGE = 27;
	
	public final static int FROMMP_QUIT = 201;
	public final static int UPDATETIMEBARANDTIMENOW = 202;
	public final static int UPDATEENDTIME = 203;
	
	
	
	public final static int MSG_HINT_NOTGETAUDIOFOCUS = 301;
	
	
	
	public final static int MSG_SET_PLAYANDPAUSE_FOCUS = 350;
	public final static int MSG_SET_PLAYANDPAUSE_ICONTOPLAY  = 351;
	public final static int MSG_SET_PLAYANDPAUSE_ICONTOPAUSE  = 352;
	public final static int MSG_SET_FASTFORWARD_FOCUS  = 3511;
	public final static int MSG_SET_FASTBACKWARD_FOCUS = 3512;
	public final static int MSG_SET_SLOWBACKWARD_FOCUS = 3513;
	public final static int MSG_SET_SLOWFORWARD_FOCUS =3514;
	public final static int MSG_SET_SKIPNEXT_FOCUS = 3515;
	public final static int MSG_SET_SKIPPREVIOUS_FOCUS = 3516;
	
	public final static int MSG_SET_PLAYBACKSTATUS = 353;
	public final static int MSG_HINT_MEDIANOTAVAIL = 354;
	
	public final static int MSG_HIDE_MENU = 355;
	public final static int MSG_SET_REPEATMODE_ICON = 356;
	public final static int MSG_DMS_CLOSE = 357;
	public final static int MSG_RESET_SEEKFLAG = 358;
	public final static int MSG_STARTANIMATION = 359;
	public final static int MSG_DISSMISSANIMATION = 360;
	public final static int MSG_HINT_SHORTMESSAGE = 361;
}

class PlaybackStatus {
	public static final int STATUS_PLAY = 1;
	public static final int STATUS_PAUSE = 2;
	public static final int STATUS_SKIPNEXT = 3;
	public static final int STATUS_SKIPBEFORE = 4;
	
	public static final int STATUS_FF1 = 13;
	public static final int STATUS_FF2 = 5;
	public static final int STATUS_FF3 = 14;
	public static final int STATUS_FF4 = 15;
	public static final int STATUS_FF5 = 16;
	
	public static final int STATUS_FW1 = 17;
	public static final int STATUS_FW2 = 18;
	public static final int STATUS_FW3 = 19;
	public static final int STATUS_FW4 = 20;
	public static final int STATUS_FW5 = 21;
	
	public static final int STATUS_SF1 = 22;
	public static final int STATUS_SF2 = 23;
	public static final int STATUS_SF3 = 24;
	public static final int STATUS_SF4 = 25;
	public static final int STATUS_SF5 = 26;
	
	public static final int STATUS_SW1 = 27;
	public static final int STATUS_SW2 = 28;
	public static final int STATUS_SW3 = 29;
	public static final int STATUS_SW4 = 30;
	public static final int STATUS_SW5 = 31;
	
	public static final int STATUS_NOTREADY = 32;

	
	public static final int UNKNOWN = 11111;
}
