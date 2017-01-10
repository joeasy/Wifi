/* Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package android.app;

import java.io.IOException;

import android.app.tv.*;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.util.Slog;

// add by keven_yuan begin
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.format.Time;
// add by keven_yuan end




/**
 * This class provides access to the system TV services.  These allow you
 * to schedule your application to be run at some point in the future.  When
 * an alarm goes off, the {@link Intent} that had been registered for it
 * is broadcast by the system, automatically starting the target application
 * if it is not already running.  Registered alarms are retained while the
 * device is asleep (and can optionally wake the device up if they go off
 * during that time), but will be cleared if it is turned off and rebooted.
 *
 * <p>The Alarm Manager holds a CPU wake lock as long as the alarm receiver's
 * onReceive() method is executing. This guarantees that the phone will not sleep
 * until you have finished handling the broadcast. Once onReceive() returns, the
 * Alarm Manager releases this wake lock. This means that the phone will in some
 * cases sleep as soon as your onReceive() method completes.  If your alarm receiver
 * called {@link android.content.Context#startService Context.startService()}, it
 * is possible that the phone will sleep before the requested service is launched.
 * To prevent this, your BroadcastReceiver and Service will need to implement a
 * separate wake lock policy to ensure that the phone continues running until the
 * service becomes available.
 *
 * <p><b>Note: The Alarm Manager is intended for cases where you want to have
 * your application code run at a specific time, even if your application is
 * not currently running.  For normal timing operations (ticks, timeouts,
 * etc) it is easier and much more efficient to use
 * {@link android.os.Handler}.</b>
 *
 * <p>You do not
 * instantiate this class directly; instead, retrieve it through
 * {@link android.content.Context#getSystemService
 * Context.getSystemService(Context.TV_SERVICE)}.
 */
/** @hide */
public class TvManager
{
		//add by keven_yuan begin
		private static boolean flag = false;
		//add by keven_yuan end
    //TV_MEDIA_MSG
    public final static int TV_MEDIA_MSG_SLR_AUDIO_ONLY = 1310;
    public final static int TV_MEDIA_MSG_SCAN_FREQ_UPDATE = 1311;
    public final static int TV_MEDIA_MSG_SCAN_AUTO_COMPLETE = 1312;
    public final static int TV_MEDIA_MSG_SCAN_MANUAL_COMPLETE = 1313;
    public final static int TV_MEDIA_MSG_SCAN_SEEK_COMPLETE = 1314;
    public final static int TV_MEDIA_MSG_DTV_ENCRYPTED = 1331;
    public final static int TV_MEDIA_MSG_DTV_NO_CI_MODULE = 1332;

    // ENUM_TV_TYPE
    public final static int TV_TYPE_NONE = 0;
    public final static int TV_TYPE_ATV = 1;
    public final static int TV_TYPE_DTV = 2;
    public final static int TV_TYPE_IDTV = 3;

    // TV_SCAN_INFO
    public final static int SCAN_INFO_STATE = 0;
    public final static int SCAN_INFO_PROGRESS = 1;
    public final static int SCAN_INFO_CHANNEL_COUNT = 2;
    public final static int SCAN_INFO_CHANNEL_STRENGTH = 3;
    public final static int SCAN_INFO_CHANNEL_QUALITY = 4;
    public final static int SCAN_INFO_CHANNEL_FREQUENCY = 5;
    public final static int SCAN_INFO_CHANNEL_BANDWIDTH = 6;
    public final static int SCAN_INFO_CURRENT_CHANNEL_NUMBER = 7;
    public final static int SCAN_INFO_SET_SCANINFO_FREQUENCY = 8;

    // RT_ATV_SOUND_SYSTEM
    public final static int RT_ATV_SOUND_SYSTEM_UNKNOWN = 0;
    public final static int RT_ATV_SOUND_SYSTEM_DK = 1;
    public final static int RT_ATV_SOUND_SYSTEM_I = 2;
    public final static int RT_ATV_SOUND_SYSTEM_BG = 3;
    public final static int RT_ATV_SOUND_SYSTEM_MN = 4;
    public final static int RT_ATV_SOUND_SYSTEM_L = 5;
    public final static int RT_ATV_SOUND_SYSTEM_LA = 6;
    public final static int RT_ATV_SOUND_SYSTEM_AUTO = 7;

	// ATV_SOUND_MODE
	public final static int ATV_SOUND_MODE_MONO = 0;
	public final static int ATV_SOUND_MODE_STEREO = 1;
	public final static int ATV_SOUND_MODE_DUAL = 2;
	public final static int ATV_SOUND_MODE_SAP_MONO = 3;
	public final static int ATV_SOUND_MODE_SAP_STEREO = 4;
	public final static int ATV_SOUND_MODE_AUTO = 5;

	//MTS_PRIORITY
	public final static int MTS_PRIORITY_AUTO = 0;
	public final static int MTS_PRIORITY_MONO = 1;
	public final static int MTS_PRIORITY_STEREO = 2;
	public final static int MTS_PRIORITY_SAP = 3;

	//RT_ATV_SOUND_SEL
	public final static int RT_ATV_SOUND_SEL_UNKNOWN = 0;
	public final static int RT_ATV_SOUND_SEL_MONO = 1;
	public final static int RT_ATV_SOUND_SEL_BTSC_MONO = 2;
	public final static int RT_ATV_SOUND_SEL_BTSC_STEREO = 3;
	public final static int RT_ATV_SOUND_SEL_BTSC_SAP = 4;
	public final static int RT_ATV_SOUND_SEL_BTSC_L_MONO_R_SAP = 5;
	public final static int RT_ATV_SOUND_SEL_NICAM_ANA_MONO = 6;
	public final static int RT_ATV_SOUND_SEL_NICAM_DIG_MONO = 7;
	public final static int RT_ATV_SOUND_SEL_NICAM_DIG_STEREO = 8;
	public final static int RT_ATV_SOUND_SEL_NICAM_DIG_LANGA = 9;
	public final static int RT_ATV_SOUND_SEL_NICAM_DIG_LANGB = 10;
	public final static int RT_ATV_SOUND_SEL_NICAM_DIG_LANGAB = 11;
	public final static int RT_ATV_SOUND_SEL_A2_MONO = 12;
	public final static int RT_ATV_SOUND_SEL_A2_STEREO = 13;
	public final static int RT_ATV_SOUND_SEL_A2_LANGA = 14;
	public final static int RT_ATV_SOUND_SEL_A2_LANGB = 15;
	public final static int RT_ATV_SOUND_SEL_A2_LANGAB = 16;
	public final static int RT_ATV_SOUND_SEL_MONO1 = 17;
	public final static int RT_ATV_SOUND_SEL_MONO2 = 18;
	
	//MTS_DUAL
	public final static int MTS_DUAL1 = 0;
	public final static int MTS_DUAL2 = 1;
	
	//TSB Custom Sound Select type
	public final static int SOUND_SELECT_MONO = 0;
	public final static int SOUND_SELECT_MONO1 = 1;
	public final static int SOUND_SELECT_MONO2 = 2;
	public final static int SOUND_SELECT_DUAL1 = 3;
	public final static int SOUND_SELECT_DUAL2 = 4;
	public final static int SOUND_SELECT_SAP = 5;
	public final static int SOUND_SELECT_STEREO = 6;
	

    // ENUM_NET_IP_TYPE
    public final static int IP_TYPE_IP = 0;
    public final static int IP_TYPE_SUBMASK = 1;
    public final static int IP_TYPE_GATEWAY = 2;
    public final static int IP_TYPE_DNS = 3;

	//ENUM_NET_INTERFACE
    public final static int ETH0 = 0;
    public final static int WLAN0 = 1;
    public final static int WLAN1 = 2;
    public final static int PPP0 = 3;
    public final static int BR0 = 4;
    public final static int NET_INTERFACE_NUM = 5;

    // ENUM_WIRELESS_SECURITY
    public final static int WL_SECURITY_OPEN = 0;
    public final static int WL_SECURITY_WEP = 1;
    public final static int WL_SECURITY_WPA = 2;
    public final static int WL_SECURITY_WEP_SHAREKEY = 3;
    public final static int WL_SECURITY_UNKNOWN = 4;

    // ENUM_WIRELESS_MODE
    public final static int WL_MODE_INFRASTRUCTURE = 0;
    public final static int WL_MODE_P2P = 1;
    public final static int WL_MODE_WPS = 2;
    public final static int WL_MODE_WCN = 3;

    // ENUM_WPS_MODE
    public final static int WPS_MODE_NONE  = 0;
    public final static int WPS_MODE_PIN  = 1;
    public final static int WPS_MODE_PBC  = 2;

    // NET_WIRELESS_STATE
    public final static int NET_WL_NONE = 0;
    public final static int NET_WL_START = 1;
    public final static int NET_WL_WAIT = 2;
    public final static int NET_WL_PING = 3;
    public final static int NET_WL_OK = 4;
    public final static int NET_WL_ERROR = 5;

    //NET_WIRED_DHCP_STATE
    public final static int NET_DHCP_NONE = 0;
    public final static int NET_DHCP_START = 1;
    public final static int NET_DHCP_WAIT = 2;
    public final static int NET_DHCP_OK = 3;
    public final static int NET_DHCP_ERROR = 4;

	//DNR_MODE
	public final static int DNR_OFF = 0;
	public final static int DNR_LOW = 1;
	public final static int DNR_MEDIUM = 2;
	public final static int DNR_HIGH = 3;
	public final static int DNR_AUTO = 4;

	//PICTURE_MODE
	public final static int PICTURE_MODE_USER = 0;
	public final static int PICTURE_MODE_VIVID = 1;
	public final static int PICTURE_MODE_STD = 2;
	public final static int PICTURE_MODE_GENTLE = 3;
	public final static int PICTURE_MODE_MOVIE = 4;
	public final static int PICTURE_MODE_SPORT = 5;
	public final static int PICTURE_MODE_GAME = 6;
	public final static int PICTURE_MODE_MAX = 7;

	//ENUM_ASPECT_RATIO
	public final static int ASPECT_RATIO_UNKNOWN = 0;
	public final static int PS_4_3 = 1;
	public final static int LB_4_3 = 2;
	public final static int Wide_16_9 = 3;
	public final static int Wide_16_10 = 4;
	public final static int SCALER_RATIO_AUTO = 5;
	public final static int SCALER_RATIO_4_3 = 6;
	public final static int SCALER_RATIO_16_9 = 7;
	public final static int SCALER_RATIO_14_9 = 8;
	public final static int SCALER_RATIO_LETTERBOX = 9;
	public final static int SCALER_RATIO_PANORAMA = 10;
	public final static int SCALER_RATIO_FIT = 11;
	public final static int SCALER_RATIO_POINTTOPOINT = 12;
	public final static int SCALER_RATIO_BBY_AUTO = 13;
	public final static int SCALER_RATIO_BBY_NORMAL = 14;
	public final static int SCALER_RATIO_BBY_ZOOM = 15;
	public final static int SCALER_RATIO_BBY_WIDE_1 = 16;
	public final static int SCALER_RATIO_BBY_WIDE_2 = 17;
	public final static int SCALER_RATIO_BBY_CINEMA = 18;
	public final static int SCALER_RATIO_CUSTOM = 19;
	public final static int SCALER_RATIO_PERSON = 20;
	public final static int SCALER_RATIO_CAPTION = 21;
	public final static int SCALER_RATIO_MOVIE = 22;
	public final static int SCALER_RATIO_ZOOM = 23;
	public final static int SCALER_RATIO_100 = 24;
	public final static int SCALER_RATIO_SOURCE = 25;
	public final static int SCALER_RATIO_ZOOM_14_9 = 26;
	public final static int SCALER_RATIO_NATIVE = 27;
	public final static int SCALER_RATIO_DISABLE = 28;
	public final static int ASPECT_RATIO_MAX = 29;

	//SourceOption
	public final static int SOURCE_OSD = 0;
	public final static int SOURCE_ATV1 = 1;
	public final static int SOURCE_ATV2 = 2;
	public final static int SOURCE_DTV1 = 3;
	public final static int SOURCE_DTV2 = 4;
	public final static int SOURCE_AV1 = 5;
	public final static int SOURCE_AV2 = 6;
	public final static int SOURCE_AV3 = 7;
	public final static int SOURCE_SV1 = 8;
	public final static int SOURCE_SV2 = 9;
	public final static int SOURCE_YPP1 = 10;
	public final static int SOURCE_YPP2 = 11;
	public final static int SOURCE_YPP3 = 12;
	public final static int SOURCE_YPP4 = 13;
	public final static int SOURCE_VGA1 = 14;
	public final static int SOURCE_VGA2 = 15;
	public final static int SOURCE_HDMI1 = 16;
	public final static int SOURCE_HDMI2 = 17;
	public final static int SOURCE_HDMI3 = 18;
	public final static int SOURCE_HDMI4 = 19;
	public final static int SOURCE_HDMI5 = 20;
	public final static int SOURCE_HDMI6 = 21;
	public final static int SOURCE_SCART1 = 22;
	public final static int SOURCE_SCART2 = 23;
	public final static int SOURCE_PLAYBACK = 24;
	public final static int SOURCE_MIC = 25;
	public final static int SOURCE_AUTO = 26; //SOURCE_AUTO is not an actual source, it is only used for boot up source in setup menu
	public final static int SOURCE_IDTV1 = 27; // This is a conceptual source.
	public final static int SOURCE_BROWSER = 28; // This is a fake source.
	public final static int SOURCE_NULL = 29;
	public final static int SOURCE_MAX_NUM = 30;

	// RT_COLOR_STD definitin is consistent with  system.mac/branch_src_sharedMemory_integration/Include/Application/AppClass/MediaControl/Types/RtMediaTypes.h
	public final static int RT_COLOR_STD_UNKNOWN = 0;
	public final static int RT_COLOR_STD_AUTO = 1;
	public final static int RT_COLOR_STD_NTSC = 2;
	public final static int RT_COLOR_STD_NTSC_50 = 3;
	public final static int RT_COLOR_STD_NTSC_443 = 4;
	public final static int RT_COLOR_STD_PAL_I = 5;
	public final static int RT_COLOR_STD_PAL_M = 6;
	public final static int RT_COLOR_STD_PAL_N = 7;
	public final static int RT_COLOR_STD_PAL_60 = 8;
	public final static int RT_COLOR_STD_SECAM = 9;
	public final static int RT_COLOR_STD_SECAML = 10;
	public final static int RT_COLOR_STD_SECAMLA = 11;

	//The definition is consistent with system.mac/branch_src_sharedMemory_integration/Include/Platform_Lib/TVScalerControl_Darwin/scaler/scalerLib.h
	public final static int SLR_COLORTEMP_USER = 0;
	public final static int SLR_COLORTEMP_NORMAL = 1;   //std
	public final static int SLR_COLORTEMP_WARMER = 2;   //6500K
	public final static int SLR_COLORTEMP_WARM = 3;     //7300K
	public final static int SLR_COLORTEMP_COOL = 4;     //8200K
	public final static int SLR_COLORTEMP_COOLER = 5;   //9300K
	public final static int SLR_COLORTEMP_MAX_NUM = 6;

	//AUDIO_EQUALIZER_TYPE
	public final static int AUDIO_EQUALIZER_STANDARD = 0;
	public final static int AUDIO_EQUALIZER_MUSIC = 1;
	public final static int AUDIO_EQUALIZER_NEWS = 2;
	public final static int AUDIO_EQUALIZER_FILM = 3;
	public final static int AUDIO_EQUALIZER_USER = 4;
	public final static int AUDIO_EQUALIZER_NUM = 5;

	//ENUM_EQUALIZER_MODE
	public final static int ENUM_EQUALIZER_RESERVED = 0;
	public final static int ENUM_EQUALIZER_MODE_POP = 1;
	public final static int ENUM_EQUALIZER_MODE_LIVE = 2;
	public final static int ENUM_EQUALIZER_MODE_CLUB = 3;
	public final static int ENUM_EQUALIZER_MODE_ROCK = 4;
	public final static int ENUM_EQUALIZER_MODE_BASS = 5;
	public final static int ENUM_EQUALIZER_MODE_TREBLE = 6;
	public final static int ENUM_EQUALIZER_MODE_VOCAL = 7;
	public final static int ENUM_EQUALIZER_MODE_POWERFUL = 8;
	public final static int ENUM_EQUALIZER_MODE_DANCE = 9;
	public final static int ENUM_EQUALIZER_MODE_SOFT = 10;
	public final static int ENUM_EQUALIZER_MODE_PARTY = 11;
	public final static int ENUM_EQUALIZER_MODE_CLASSICAL = 12;

	//ENUM_MAGIC_PICTURE
	public final static int MAGIC_PICTURE_OFF = 0;
	public final static int MAGIC_PICTURE_STILLDEMO = 1;
	public final static int MAGIC_PICTURE_STILLDEMO_INVERSE = 2;
    public final static int MAGIC_PICTURE_DYNAMICDEMO = 3;
    public final static int MAGIC_PICTURE_MOVE = 4;
    public final static int MAGIC_PICTURE_MOVE_INVERSE = 5;
    public final static int MAGIC_PICTURE_ZOOM = 6;
    public final static int MAGIC_PICTURE_OPTIMIZE = 7;
    public final static int MAGIC_PICTURE_ENHANCE = 8;

    //ENUM_DREAM_PANEL
    public final static int DREAM_PANEL_OFF = 0;
    public final static int DREAM_PANEL_ENVIRONMENT = 1;
    public final static int DREAM_PANEL_IMAGE = 2;
    public final static int DREAM_PANEL_MULTIPLE = 3;
    public final static int DREAM_PANEL_DEMO = 4;
    public final static int DREAM_PANEL_MAX = 5;

	//ENUM_MENU_LANGUAGE  =>  Include/Application/AppClass/setupdef.h
	public final static int MENU_LANG_ENGLISH  = 0;
	public final static int MENU_LANG_CHINESE  = 1;
    public final static int MENU_LANG_JAPANESE = 2;
    public final static int MENU_LANG_SPANISH  = 3;
    public final static int MENU_LANG_FRENCH   = 4;
    public final static int MENU_LANG_GERMAN   = 5;
    public final static int MENU_LANG_ITALIAN  = 6;
    public final static int MENU_LANG_KOREAN   = 7;
    public final static int MENU_LANG_DUTCH    = 8;
    public final static int MENU_LANG_RUSSIAN  = 9;
    public final static int MENU_LANG_SCHINESE = 10;
    public final static int MENU_LANG_MAX_NUM  = 11;

     //SLR_3D_MODE;
    public final static int SLR_3DMODE_2D = 0;
    public final static int SLR_3DMODE_3D_AUTO = 1;
    public final static int SLR_3DMODE_3D_SBS = 2;
    public final static int SLR_3DMODE_3D_TB = 3;
    public final static int SLR_3DMODE_3D_FP = 4;

    // --- new 3D format ---
    public final static int SLR_3DMODE_3D_LBL = 5;
    public final static int SLR_3DMODE_3D_VSTRIP = 6;
    public final static int SLR_3DMODE_3D_CKB = 7;
    public final static int SLR_3DMODE_3D_REALID = 8;
    public final static int SLR_3DMODE_3D_SENSIO = 9;
	public final static int SLR_3DMODE_2D_CVT_3D = 10;
    // -------------------
    public final static int SLR_3DMODE_3D_AUTO_CVT_2D = 11;
    public final static int SLR_3DMODE_3D_SBS_CVT_2D = 12;
    public final static int SLR_3DMODE_3D_TB_CVT_2D = 13;
    public final static int SLR_3DMODE_3D_FP_CVT_2D = 14;
    // --- new 3D format ---
    public final static int SLR_3DMODE_3D_LBL_CVT_2D = 15;
    public final static int SLR_3DMODE_3D_VSTRIP_CVT_2D = 16;
    public final static int SLR_3DMODE_3D_CKB_CVT_2D = 17;
    public final static int SLR_3DMODE_3D_REALID_CVT_2D = 18;
    public final static int SLR_3DMODE_3D_SENSIO_CVT_2D = 19;
    // -------------------
    public final static int SLR_3DMODE_NUM = 20;
    // disable
    public final static int SLR_3DMODE_DISABLE = 0xff;

	//CHMGR_SORT_POLICY
	public final static int CHMGR_SORT_BY_CHNUM = 0;
	public final static int CHMGR_SORT_BY_FREQUENCY = 1;
	public final static int CHMGR_SORT_BY_USER = 2;

	//AUDIO_AO_CHANNEL_OUT_SWAP
	public final static int AUDIO_AO_CHANNEL_OUT_STEREO = 0x0;
	public final static int AUDIO_AO_CHANNEL_OUT_L_TO_R = 0x1;
	public final static int AUDIO_AO_CHANNEL_OUT_R_TO_L = 0x2;
	public final static int AUDIO_AO_CHANNEL_OUT_LR_SWAP = 0x3;
	public final static int AUDIO_AO_CHANNEL_OUT_LR_MIXED = 0x4;

	//VIDEO_TRANSITION_TYPE
	public final static int VIDEO_TRANSITION_COPY = 0;
	public final static int VIDEO_TRANSITION_CROSSFADE = 1;
	public final static int VIDEO_TRANSITION_LEFT_TO_RIGHT = 2;
	public final static int VIDEO_TRANSITION_TOP_TO_BOTTOM = 3;
	public final static int VIDEO_TRANSITION_WATERFALL = 4;
	public final static int VIDEO_TRANSITION_SNAKE = 5;
	public final static int VIDEO_TRANSITION_RANDOM_BOX = 6;
	public final static int VIDEO_TRANSITION_DIAGONAL = 7;
	public final static int VIDEO_TRANSITION_FADEIN_FADEOUT = 8;
	public final static int VIDEO_TRANSITION_MOVE = 9;
	public final static int VIDEO_TRANSITION_CROSSFADE_KENBURNS = 10;
	public final static int VIDEO_TRANSITION_WINDOW = 11;
	public final static int VIDEO_TRANSITION_EXTEND = 12;
	public final static int VIDEO_TRANSITION_EXPAND = 13;
	public final static int VIDEO_TRANSITION_STEP_ALPHA = 14;
	public final static int VIDEO_TRANSITION_FLYING_RECTANGLE = 15;
	public final static int VIDEO_TRANSITION_VENETIAN_BLINDS = 16;
	public final static int VIDEO_TRANSITION_BLUR = 17;
	public final static int VIDEO_TRANSITION_CIRCLE = 18;
	public final static int VIDEO_TRANSITION_RIGHT_TO_LEFT = 19;
	public final static int VIDEO_TRANSITION_BOTTOM_TO_TOP = 20;
	public final static int VIDEO_TRANSITION_UNKNOWN = 21;

	// SLR_RATIO_TYPE
	public final static int SLR_RATIO_AUTO = 0;
	public final static int SLR_RATIO_4_3 = 1;
	public final static int SLR_RATIO_16_9 = 2;
	public final static int SLR_RATIO_14_9 = 3;
	public final static int SLR_RATIO_LETTERBOX = 4;
	public final static int SLR_RATIO_PANORAMA = 5;
	public final static int SLR_RATIO_FIT = 6;
	public final static int SLR_RATIO_POINTTOPOINT = 7;
	public final static int SLR_RATIO_BBY_AUTO = 8;
	public final static int SLR_RATIO_BBY_NORMAL = 9;
	public final static int SLR_RATIO_BBY_ZOOM = 10;
	public final static int SLR_RATIO_BBY_WIDE_1 = 11;
	public final static int SLR_RATIO_BBY_WIDE_2 = 12;
	public final static int SLR_RATIO_BBY_CINEMA = 13;
	public final static int SLR_RATIO_CUSTOM = 14;
	public final static int SLR_ASPECT_RATIO_PERSON = 15;
	public final static int SLR_ASPECT_RATIO_CAPTION = 16;
	public final static int SLR_ASPECT_RATIO_MOVIE = 17;
	public final static int SLR_ASPECT_RATIO_ZOOM = 18;
	public final static int SLR_ASPECT_RATIO_100 = 19;
	public final static int SLR_ASPECT_RATIO_SOURCE = 20;
	public final static int SLR_RATIO_ZOOM_14_9 = 21;
	public final static int SLR_RATIO_DISABLE = 0xff;//disable

	//Get decode image result
    public final static long DEC_IMG_DECODING = 1;
    public final static long DEC_IMG_SUCCESS = 0;
    public final static long DEC_IMG_FAIL = -1;
	public final static long DEC_IMG_RESOURCE_LOCKED = -2;
	public final static long DEC_IMG_HANDLE_INVALID  = -3;
	public final static long DEC_IMG_PARAM_INVALID   = -4;
    public final static long DEC_IMG_NOT_WORKING = -5;

    // GLDC OSD SHOW mode
    public final static int GLDC_OSD_SHOW = 1;
    public final static int GLDC_OSD_HIDE = 0;

	//QUADFHD_SETTING_MODE_TYPE
	public final static int QUADFHD_NATIVE_MODE = 0;
	public final static int QUADFHD_PHOTO_MODE = 1;
	public final static int QUADFHD_VIDEO_MODE = 2;
	public final static int QUADFHD_FS_THREED_MODE = 3;
	public final static int QUADFHD_SBS_BYPASS_MODE = 4;
	public final static int QUADFHD_TAB_BYPASS_MODE = 5;
	public final static int QUADFHD_QUAD_OSD_MODE = 6;

///////////////////////////////////// add by hongzhi_yin ////////////////////////////////////////////
	// color wheel mode
	public final static int COLOR_WHEEL_OFF = 0;
	public final static int COLOR_WHEEL_ON = 1;
	public final static int COLOR_WHEEL_DEMO = 2;
///////////////////////////////////// end ////////////////////////////////////////////

	// qam const
	public final static int TV_QAM_CONST_4 = 0;
	public final static int TV_QAM_CONST_16 = 1;
	public final static int TV_QAM_CONST_32 = 2;
	public final static int TV_QAM_CONST_64 = 3;
	public final static int TV_QAM_CONST_128 = 4;
	public final static int TV_QAM_CONST_256 = 5;
	public final static int TV_QAM_CONST_512 = 6;
	public final static int TV_QAM_CONST_1024 = 7;

       private final ITvManager mService;


 /**
     * package private on purpose
     */
    TvManager(ITvManager service) {
        mService = service;
    }

	//SystemCommandExecutor
	public void suspendToRam(){
		try{
			mService.suspendToRam();
		} catch (RemoteException ex) {
		}
	}
    public void stopRpcServer() {
        try {
            mService.stopRpcServer();
        } catch (RemoteException ex) {
        }
    }

    public void dumpPliMemoryUsage() {
        try {
            mService.dumpPliMemoryUsage();
        } catch (RemoteException ex) {
        }
    }

    public void suspendRpcServer() {
        try {
            mService.suspendRpcServer();
        } catch (RemoteException ex) {
        }
    }

    public void resumeRpcServer() {
        try {
            mService.resumeRpcServer();
        } catch (RemoteException ex) {
        }
    }

    public void restoreSysDefSetting() {
        try {
            mService.restoreSysDefSetting();
        } catch (RemoteException ex) {
        }
    }

	public void setScreenSaverTiming(int itiming){
		try{
			mService.setScreenSaverTiming(itiming);
		}catch(RemoteException ex){
		}
	}

	public void setMenuLanguage(int iLanguage){
		try{
			mService.setMenuLanguage(iLanguage);
		}catch(RemoteException ex){
		}
	}

	public void setSerialCode(String strSerCode){
		try{
			mService.setSerialCode(strSerCode);
		}catch(RemoteException ex){
		}
	}

	public int getScreenSaverTiming(){
		try{
			return	mService.getScreenSaverTiming();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getMenuLanguage(){
		try{
			return	mService.getMenuLanguage();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getSerialCode(){
		try{
			return mService.getSerialCode();
		}catch(RemoteException ex){
		}
		return null;
	}

    public void startPerformanceLog(){
        try {
            mService.startPerformanceLog();
        } catch (RemoteException ex) {
        }
    }

	public void stopPerformanceLog(String strFilePath){
		try{
			mService.stopPerformanceLog(strFilePath);
		}catch(RemoteException ex){
		}

	}

    public void clearEeprom(){
        try {
            mService.clearEeprom();
        } catch (RemoteException ex) {
        }
    }

	public boolean readEepToFile(){
		boolean result = false;

		try{
			result = mService.readEepToFile();
		} catch(RemoteException ex){
		}

		return result;
	}

	public boolean writeFileToEep(){
		boolean result = false;

		try{
			result = mService.writeFileToEep();
		} catch(RemoteException ex){
		}

		return result;
	}

    public void upgradeSystem(){
        try {
            mService.upgradeSystem();
        } catch (RemoteException ex) {
        }
    }

	public String getCurAPK(){
		try{
			return mService.getCurAPK();
		} catch (RemoteException ex) {
		}
		return null;
	}
	/*public Context tsbGetCurApkContext(){
		try{
			return mService.TSBGetCurApkContext();
		} catch (RemoteException ex) {
		}
		return null;
	}*/
	public int getHDMIAvailableNum(){
		try{
			return mService.getHDMIAvailableNum();
		} catch (RemoteException ex) {
		}
		return 0;
	}

	public void write_SerialNumStr2xml(String SerialNum){

		try{
			mService.write_SerialNumStr2xml(SerialNum);
		}catch(RemoteException ex){
		}

	}
	
	public void EraseDataLocalTmpFile(String deleteFileName){

        try{
            mService.EraseDataLocalTmpFile(deleteFileName);
        }catch(RemoteException ex){
        }

    }

    //TvChannelApiExecutor
	public boolean tvAutoScanStart(boolean updateScan) {
        try {
            return mService.tvAutoScanStart(updateScan);
        } catch (RemoteException ex) {
			return false;
        }
    }


	public boolean idtvAutoScanStart(boolean updateScan,int tvtype,int scanmode,int tvsubtype) {
        try {
            return mService.idtvAutoScanStart(updateScan,tvtype,scanmode,tvsubtype);
        } catch (RemoteException ex) {
			return false;
        }
    }


    public void tvAutoScanStop() {
        try {
            mService.tvAutoScanStop();
        } catch (RemoteException ex) {
        }
    }

    public void tvAutoScanComplete() {
        try {
            mService.tvAutoScanComplete();
        } catch (RemoteException ex) {
        }
    }

	public boolean tvSeekScanStart(boolean bSeekForward ){
		boolean result = false;

		try{
			result = mService.tvSeekScanStart(bSeekForward);
		} catch(RemoteException ex){
		}

		return result;
	}

	public void tvSeekScanStop(){
		try{
			mService.tvSeekScanStop();
		} catch(RemoteException ex){
		}
	}

	public boolean tvScanManualStart(int iFreq, int iBandWidth, int iPhyChNum){
		boolean result = false;

		try{
			result = mService.tvScanManualStart(iFreq, iBandWidth, iPhyChNum);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean idtvScanManualStart(int iFreq, int iBandWidth, int iPhyChNum,int tvtype,int tvsubtype){
		boolean result = false;

		try{
			result = mService.idtvScanManualStart(iFreq, iBandWidth, iPhyChNum, tvtype, tvsubtype);
		}catch(RemoteException ex){
		}

		return result;
	}

	public void tvScanManualStop(){
		try{
			mService.tvScanManualStop();
		}catch(RemoteException ex){
		}
	}

	public void tvScanManualComplete(){
		try{
			mService.tvScanManualComplete();
		}catch(RemoteException ex){
		}
	}

	public int tvScanInfo(int infoId){
		try{
			return	mService.tvScanInfo(infoId);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean isTvScanning(){
		try{
			return	mService.isTvScanning();
		}catch(RemoteException ex){
		}
		return false;
	}

	public int getAtvSeqScanStartFreq(){
		try{
			return	mService.getAtvSeqScanStartFreq();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAtvSeqScanEndFreq(){
		try{
			return	mService.getAtvSeqScanEndFreq();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void saveChannel(){
		try{
			mService.saveChannel();
		}catch(RemoteException ex){
		}
	}

	public void playNextChannel(){
		try{
			mService.playNextChannel();
		}catch(RemoteException ex){
		}
	}

	public void playPrevChannel(){
		try{
			mService.playPrevChannel();
		}catch(RemoteException ex){
		}
	}

	public void playFirstChannel(){
		try{
			mService.playFirstChannel();
		}catch(RemoteException ex){
		}
	}

	public void playHistoryChannel(){
		try{
			mService.playHistoryChannel();
		}catch(RemoteException ex){
		}
	}

	public void updateTvChannelList(ChannelFilter filter) {
		try {
			mService.updateTvChannelList(filter);
		} catch (RemoteException ex) {
		}
	}

	public ChannelInfo getCurChannel() {
		try {
			return mService.getCurChannel();
		} catch (RemoteException ex) {
			return null;
		}
	}

	public ChannelInfo getChannelInfoByIndex(int iIndex) {
		try {
			return mService.getChannelInfoByIndex(iIndex);
		} catch (RemoteException ex) {
			return null;
		}
	}

	public ChannelInfoBySize getChannelInfoBySize(int iSize) {
		try {
			return mService.getChannelInfoBySize(iSize);
		} catch (RemoteException ex) {
			return null;
		}
	}    

	public int getChannelCount() {
		try {
			return mService.getChannelCount();
		} catch (RemoteException ex) {
			return 0;
		}
	}

	public int getChannelCountByServiceType(int serviceType) {
		try {
			return mService.getChannelCountByServiceType(serviceType);
		} catch (RemoteException ex) {
			return 0;
		}
	}

	public boolean sortChannel(int policy){
		//CHMGR_SORT_POLICY
		try{
			return mService.sortChannel(policy);
		}catch(RemoteException ex){
		}
		return false;
	}

	public void playChannelByIndex(int iIndex){
		try{
			mService.playChannelByIndex(iIndex);
		}catch(RemoteException ex){
		}
	}

	public void playChannelByNum(int iNum){
		try{
			mService.playChannelByNum(iNum);
		}catch(RemoteException ex){
		}
	}

	public boolean playChannel(int iIndex){
		boolean result = false;

		try{
			result = mService.playChannel(iIndex);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean playChannelByLCN(int iLcnNum){
		boolean result = false;

		try{
			result = mService.playChannelByLCN(iLcnNum);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean playFirstChannelInFreq(int iFreq){
		boolean result = false;

		try{
			result = mService.playFirstChannelInFreq(iFreq);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean playChannelByChnumFreq(int iSysChNum, int iFreq, String tvSystem){
		boolean result = false;

		try{
			result = mService.playChannelByChnumFreq(iSysChNum, iFreq, tvSystem);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean playNumberChannel(int majorNum, int minorNum, boolean isAudioFocus){
		boolean result = false;

		try{
			result = mService.playNumberChannel(majorNum, minorNum, isAudioFocus);
		}catch(RemoteException ex){
		}

		return result;
	}

	public void swapChannelByIdxEx(int iChIdx, boolean bSwapChNum, boolean bPlayAfterSwap){
		try{
			mService.swapChannelByIdxEx(iChIdx, bSwapChNum, bPlayAfterSwap);
		}catch(RemoteException ex){
		}
	}

    public void swapChannelByIdx(int iSrcChIdx, int TgtChIdx, boolean bSwapChNum){
		try{
			mService.swapChannelByIdx(iSrcChIdx, TgtChIdx, bSwapChNum);
		}catch(RemoteException ex){
		}
	}

	public void swapChannelByNumEx(int iChNum, boolean bSwapChNum, boolean bPlayAfterSwap){
		try{
			mService.swapChannelByNumEx(iChNum, bSwapChNum, bPlayAfterSwap);
		}catch(RemoteException ex){
		}
	}


	public void ReloadLastPlayedSource(){
		try{
			mService.reloadLastPlayedSource();
		}catch(RemoteException ex){
		}
	}

	public void setCurChannelSkipped(boolean bSkip){
		try{
			mService.setCurChannelSkipped(bSkip);
		}catch(RemoteException ex){
		}
	}

	public void setCurAtvSoundStd(int soundSystemId){
		try{
			mService.setCurAtvSoundStd(soundSystemId);
		}catch(RemoteException ex){
		}
	}

	public void fineTuneCurFrequency(int iOffset, boolean bPerminant){
		try{
			mService.fineTuneCurFrequency(iOffset, bPerminant);
		}catch(RemoteException ex){
		}
	}

	public void setCurChAudioCompensation(int iValue, boolean bApply){
		try{
			mService.setCurChAudioCompensation(iValue, bApply);
		}catch(RemoteException ex){
		}
	}

	public boolean setSource(int iIndex){
		try{
			return mService.setSource(iIndex);
		}catch(RemoteException ex){
		}

		return false;
	}

	public void setBootSource(int sourceOpt){
		try{
			mService.setBootSource(sourceOpt);
		}catch(RemoteException ex){
		}
	}

	public boolean setSourceAndDisplayWindow(int src, int x, int y, int width, int height){
		boolean result = false;

		try{
			result = mService.setSourceAndDisplayWindow(src, x, y, width, height);
		}catch(RemoteException ex){
		}
		return result;
	}

	public boolean getCurChannelSkipped(){
		boolean result = false;

		try{
			result = mService.getCurChannelSkipped();
		} catch(RemoteException ex){
		}
		return result;
	}

	public int getCurAtvSoundStd(){
		//RT_ATV_SOUND_SYSTEM
		try{
			return	mService.getCurAtvSoundStd();
		}catch(RemoteException ex){
		}
		return 0;
	}

    public int getCurAtvSoundMode(){
        //ATV_SOUND_MODE
		try{
			return  mService.getCurAtvSoundMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getMtsPriority()
	{
		//MTS_PRIORITY
		try
		{
			return mService.getMtsPriority();
		}catch(RemoteException ex)
		{
		}
		return 0;
	}
	
	public boolean setMtsPriority(int MtsPriority){
		try{
			return mService.setMtsPriority(MtsPriority);
		}catch(RemoteException ex){
		}
		return false;
	}

	public int getMtsDualPriority()
	{
		//MTS_DUAL
		try
		{
			return mService.getMtsDualPriority();
		}catch(RemoteException ex)
		{
		}
		return 0;
	}
	
	public boolean setMtsDualPriority(int dual){
		try{
			return mService.setMtsDualPriority(dual);
		}catch(RemoteException ex){
		}
		return false;
	}
	
	public int TSB_GetAtvSoundSelect()
	{
		try
		{
			return mService.TSB_GetAtvSoundSelect();
		}catch(RemoteException ex)
		{
		}
		return 0;
	}
	
	public int TSB_SetAtvSoundSelect(boolean bApply)
	{
		try
		{
			return mService.TSB_SetAtvSoundSelect(bApply);
		}catch(RemoteException ex)
		{
		}
		return 0;
	}

	public int getCurChAudioCompensation(){
		try{
			return	mService.getCurChAudioCompensation();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getSourceList(){
		try{
			return	mService.getSourceList();
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getSourceListCnt(boolean bWithoutPlayback){
		try{
			return	mService.getSourceListCnt(bWithoutPlayback);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean getIsYppAvailable(){
		try{
			return	mService.getIsYppAvailable();
		}catch(RemoteException ex){
		}
		return false;
	}

	public long getCurSourceType(){
		try{
			return	mService.getCurSourceType();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getBootSource(){
		try{
			return	mService.getBootSource();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getCurTvType(){
		try{
			return	mService.getCurTvType();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getCurLiveSource(){
		try{
			return mService.getCurLiveSource();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean replaceChannel(int srcIdx, int destIdx){
		try{
			return mService.replaceChannel(srcIdx, destIdx);
		}catch(RemoteException ex){
		}
		return false;
	}

		public boolean removeChannel(int Idx){
		try{
			return mService.removeChannel(Idx);
		}catch(RemoteException ex){
		}
		return false;
	}

	public int getChannelbooster(int channelIdx){
		try{
			return mService.getChannelbooster(channelIdx);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean setChannelbooster(int channelIdx, boolean bIsEnable){
	        try{
	                return mService.setChannelbooster(channelIdx, bIsEnable);
	        }catch(RemoteException ex){
	        }
	        return false;
	}

	public String getChannelName(int channelIdx){
	        try{
	                return mService.getChannelName(channelIdx);
	        }catch(RemoteException ex){
	        }
	        return null;
	}

	public boolean setChannelName(int channelIdx,  String pChannelName){
		try{
			return mService.setChannelName(channelIdx, pChannelName);
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setRecoverManualSetting(int freq,  boolean recover){
		try{
			mService.setRecoverManualSetting(freq, recover);
		}catch(RemoteException ex){
		}
	}

	public int getTvCurChFreqOffset(){
		try{
			return mService.getTvCurChFreqOffset();
		}catch(RemoteException ex){
		}
		return -1;
	}

	public int getScanTableIdxByFreq(int freq){
		try{
			return mService.getScanTableIdxByFreq(freq);
		}catch(RemoteException ex){
		}
		return -1;
	}

	public String getChNameByScanTableIndex(int iIdx){
		try{
			return mService.getChNameByScanTableIndex(iIdx);
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getCurFrequency(){
		try{
			return mService.getCurFrequency();
		}catch(RemoteException ex){
		}
		return -1;
	}

	public boolean setCurFrequency(int freq){
		try{
			return mService.setCurFrequency(freq);
		}catch(RemoteException ex){
		}
		return false;
	}

	public String getChannelNameByTableIndex(int iIdx){
		try{
			return mService.getChannelNameByTableIndex(iIdx);
		}catch(RemoteException ex){
		}
		return null;
	}
        public int getTableIndexByChannelName(String ch_name){
		try{
			return mService.getTableIndexByChannelName(ch_name);
		}catch(RemoteException ex){
		}
		return -1;
        }
	public String getChannelBandwidthByTableIndex(int iIdx){
		try{
			return mService.getChannelBandwidthByTableIndex(iIdx);
		}catch(RemoteException ex){
		}
		return null;
	}

	public void setTvSeekScanFreq(int freq){
		try{
			mService.setTvSeekScanFreq(freq);
		}catch(RemoteException ex){
		}
	}

	public void loadChannelTableBySource(){
		try{
			mService.loadChannelTableBySource();
		}catch(RemoteException ex){
		}
	}

	public boolean enterTeletext(){
		try{
			return mService.enterTeletext();
		}catch(RemoteException ex){
		}
		return false;
	}

	public int getTeletextDispType(){
		try{
			return mService.getTeletextDispType();
		}catch(RemoteException ex){
		}
		return -1;
	}	

	public boolean setTeletextDispType(int displaytype){
		try{
			return mService.setTeletextDispType(displaytype);
		}catch(RemoteException ex){
		}
		return false;
	}
	
	public void startTTX830(){
		try{
			mService.startTTX830();
		}catch(RemoteException ex){
		}
	}
	
	public String getTTX_830(){
	try{
		return mService.getTTX_830();
	}catch(RemoteException ex){
	}
	return null;
	}

	public void exitTeletext(){
		try{
			mService.exitTeletext();
		}catch(RemoteException ex){
		}
	}
	
	public boolean getTeletextStatus(){
		try{
			return mService.getTeletextStatus();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void ttx_Cmd(int iValue){
		try{
			mService.ttx_Cmd(iValue);
		}catch(RemoteException ex){
		}
	}

        public boolean getNoSupportFmtStatus(){
		try{
			return mService.getNoSupportFmtStatus();
		}catch(RemoteException ex){
		}
		return false;
	}

	public boolean doYPP_PC_ADadjust(){
		try{
			return mService.doYPP_PC_ADadjust();
		}catch(RemoteException ex){
		}
		return false;
	}


    //TvDisplaySetupApiExecutor
	public void setBrightness(int iValue){
		try{
			mService.setBrightness(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setContrast(int iValue){
		try{
			mService.setContrast(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setSaturation(int iValue){
		try{
			mService.setSaturation(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setHue(int iValue){
		try{
			mService.setHue(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setSharpness(boolean iApply, int iValue){
		try{
			mService.setSharpness(iApply, iValue);
		}catch(RemoteException ex){
		}
	}

	public void setColorTempMode(int level){
		try{
			mService.setColorTempMode(level);
		}catch(RemoteException ex){
		}
	}
///////////////////////////////////////////////wangzhh//////////////////////////////////////////////////////////////

	public void setSkinAdjust(int mode){
		try{
			mService.setSkinAdjust(mode);
		}catch(RemoteException ex){
		}
	}

	public int getSkinAdjust(){
		try{//DNR_MODE
			return	mService.getSkinAdjust();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean setDetailEnhance(boolean bEnable){
		try{
			return mService.setDetailEnhance(bEnable);
		}catch(RemoteException ex){
		}
		return false;
	}

	public boolean getDetailEnhance(){
		boolean result = false;

		try{
			result = mService.getDetailEnhance();
		} catch(RemoteException ex){
		}
		return result;
	}


	public void setDNR(int mode){
		try{
			mService.setDNR(mode);
		}catch(RemoteException ex){
		}
	}

	public boolean setDynamicbeautiful(boolean bEnable){
		try{
			return mService.setDynamicbeautiful(bEnable);
		}catch(RemoteException ex){
		}
		return false;
	}

	public boolean getDynamicbeautiful(){
		boolean result = false;

		try{
			result = mService.getDynamicbeautiful();
		} catch(RemoteException ex){
		}
		return result;
	}

	public void  setEnergySaving(int mode){
		try{
			mService.setEnergySaving(mode);
		}catch(RemoteException ex){
		}
	}

	public int getEnergySaving(){
		try{
			return	mService.getEnergySaving();
		}catch(RemoteException ex){
		}
		return 0;
	}

	// 0-COLOR_WHEEL_OFF  1-COLOR_WHEEL_ON  2-COLOR_WHEEL_DEMO
	public void  setColorWheel(int mode){
		try{
			mService.setColorWheel(mode);
		}catch(RemoteException ex){
		}
	}

	public int getColorWheel(){
		try{
			return	mService.getColorWheel();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean setScreenSaver(boolean bEnable){
		try{
			return mService.setScreenSaver(bEnable);
		}catch(RemoteException ex){
		}
		return false;
	}

	public boolean getScreenSaver(){
		boolean result = false;

		try{
			result = mService.getScreenSaver();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean setNosignalPoweroff(boolean bEnable){
		try{
			return mService.setNosignalPoweroff(bEnable);
		}catch(RemoteException ex){
		}
		return false;
	}

	public boolean getNosignalPoweroff(){
		boolean result = false;

		try{
			result = mService.getNosignalPoweroff();
		} catch(RemoteException ex){
		}
		return result;
	}


	public void setNoactionPoweroff(int minute){
		try{
			mService.setNoactionPoweroff(minute);
		}catch(RemoteException ex){
		}
	}

	public int getNoactionPoweroff(){
		try{
			return	mService.getNoactionPoweroff();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setPanelLock()
	{
		try
		{
			mService.setPanelLock();
		}catch(RemoteException ex)
		{}
		
	}

	public void cleanPanelLock()
	{
		try
		{
			mService.cleanPanelLock();
		}catch(RemoteException ex)
		{}
		
	}

	public int getPanelLock()
	{
		try
		{
			return mService.getPanelLock();
		}catch(RemoteException ex)
		{}
		return 0;
	}


	public void setPoweroffMode(int mode){
		try{
			mService.setPoweroffMode(mode);
		}catch(RemoteException ex){
		}
	}

	public int getPoweroffMode(){
		try{
			return	mService.getPoweroffMode();
		}catch(RemoteException ex){
		}
		return 0;
	}


      public void setPowerOnMusicMode(int bOnMode) {
	    try {
	        mService.setPowerOnMusicMode(bOnMode);
	    } catch (RemoteException ex) {
	    }
	}

	public int getPowerOnMusicMode()
	{
		try{
			return mService.getPowerOnMusicMode();
		}
		catch(RemoteException ex){
		}

		return 0;
	}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void bypassSetAspectRatio(boolean value){
		try{
			mService.bypassSetAspectRatio(value);
		}catch(RemoteException ex){
		}
	}

	public void setAspectRatio(int ratio){
		try{
			mService.setAspectRatio(ratio);
		}catch(RemoteException ex){
		}
	}

	public void saveAspectRatio(int ratio){
		try{
			mService.saveAspectRatio(ratio);
		}catch(RemoteException ex){
		}
	}

	public void setPictureMode(int mode){
		try{
			mService.setPictureMode(mode);
		}catch(RemoteException ex){
		}
	}

	public void setCurAtvColorStd(int colorStd){
		try{
			mService.setCurAtvColorStd(colorStd);
		}catch(RemoteException ex){
		}
	}

	public void setAvColorStd(int colorStd){
		try{
			mService.setAvColorStd(colorStd);
		}catch(RemoteException ex){
		}
	}

	public boolean setVgaAutoAdjust(){
		boolean result = false;

		try{
			result = mService.setVgaAutoAdjust();
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean setVgaHPosition(char ucPosition){
		boolean result = false;

		try{
			result = mService.setVgaHPosition(ucPosition);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean setVgaVPosition(char ucPosition){
		boolean result = false;

		try{
			result = mService.setVgaVPosition(ucPosition);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean setVgaPhase(char ucValue){
		boolean result = false;

		try{
			result = mService.setVgaPhase(ucValue);
		}catch(RemoteException ex){
		}

		return result;
	}

	public boolean setVgaClock(char ucValue){
		boolean result = false;

		try{
			result = mService.setVgaClock(ucValue);
		}catch(RemoteException ex){
		}

		return result;
	}

	public void setMagicPicture(int magicPic){
		try{
			mService.setMagicPicture(magicPic);
		}catch(RemoteException ex){
		}
	}

	public void setDCR(int iDCR){
		try{
			mService.setDCR(iDCR);
		}catch(RemoteException ex){
		}
	}

	public void setDCC(boolean iDccOn, boolean iIsApply){
		try{
			mService.setDCC(iDccOn, iIsApply);
		}catch(RemoteException ex){
		}
	}

	public void setBacklight(int iValue){
		try{
			mService.setBacklight(iValue);
		}catch(RemoteException ex){
		}
	}

	public void set3dMode(int imode){
		try{
			mService.set3dMode(imode);
		}catch(RemoteException ex){
		}
	}

	public void set3dDeep(int imode){
		try{
			mService.set3dDeep(imode);
		}catch(RemoteException ex){
		}
	}

    public void set3dLRSwap(boolean bOn) {
        try {
            mService.set3dLRSwap(bOn);
        } catch (RemoteException ex) {
        }
    }

    public boolean set3dCvrt2D(boolean bOn, int iFrameFlag) {
		boolean result = false;

		try {
            result = mService.set3dCvrt2D(bOn, iFrameFlag);
        } catch (RemoteException ex) {
        }

		return result;
    }

	public void set3dStrength(int iStrength){
		try{
			mService.set3dStrength(iStrength);
		}catch(RemoteException ex){
		}
	}

	public boolean set3dModeAndChangeRatio(int iMode, boolean bMute, int iType){
		boolean result = false;

		try{
			result = mService.set3dModeAndChangeRatio(iMode, bMute, iType);
		}catch(RemoteException ex){
		}
		return result;
	}

	public boolean getIs3d(){
		boolean result = false;

		try{
			result = mService.getIs3d();
		} catch(RemoteException ex){
		}
		return result;
	}

	public void setColorTempRGain(int iValue){
		try{
			mService.setColorTempRGain(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setColorTempGGain(int iValue){
		try{
			mService.setColorTempGGain(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setColorTempBGain(int iValue){
		try{
			mService.setColorTempBGain(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setColorTempROffset(int iValue){
		try{
			mService.setColorTempROffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setColorTempGOffset(int iValue){
		try{
			mService.setColorTempGOffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setColorTempBOffset(int iValue){
		try{
			mService.setColorTempBOffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public boolean getIsFirstRunTVAPK(){
		boolean result = false;
                try{
                	result = mService.getIsFirstRunTVAPK();
		}catch(RemoteException ex){
		}
		return result;
	}

	public void setIsFirstRunTVAPK(boolean value){
		try{
			mService.setIsFirstRunTVAPK(value);
		}catch(RemoteException ex){
		}
	}

	public void setDisplayWindow(int iX, int iY, int iWidth, int iHeight){
		try{
			mService.setDisplayWindow(iX, iY, iWidth, iHeight);
		}catch(RemoteException ex){
		}
	}

        public void setDisplayWindowPositionChange(int iX, int iY, int iWidth, int iHeight){
		try{
			mService.setDisplayWindowPositionChange(iX, iY, iWidth, iHeight);
		}catch(RemoteException ex){
		}
	}

	public void Scaler_Rotate_Set(int rotate_mode,int rotate_enable) {
		try{
			mService.Scaler_Rotate_Set(rotate_mode, rotate_enable);
		}catch(RemoteException ex){
		}
	}

	public void setPanelOn(boolean bOn){
		try{
			mService.setPanelOn(bOn);
		}catch(RemoteException ex){
		}
	}

	public void scaler_ForceBg(boolean bOn){
		try{
			mService.scaler_ForceBg(bOn);
		}catch(RemoteException ex){
		}
	}

	public void scaler_4k2kOSD_ForceBg(boolean bOn){
		try{
			mService.scaler_4k2kOSD_ForceBg(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setOverScan(int h_start, int h_width, int v_start, int v_length){
		try{
			mService.setOverScan(h_start, h_width, v_start, v_length);
		}catch(RemoteException ex){
		}
	}

	public int getBrightness(){
		try{
			return	mService.getBrightness();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getContrast(){
		try{
			return	mService.getContrast();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getSaturation(){
		try{
			return	mService.getSaturation();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getHue(){
		try{
			return	mService.getHue();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getSharpness(){
		try{
			return	mService.getSharpness();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getColorTempLevel(){
		try{
			return	mService.getColorTempLevel();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getDNR(){
		try{//DNR_MODE
			return	mService.getDNR();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getPictureMode(){
		try{//PICTURE_MODE
			return	mService.getPictureMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getCurAtvColorStd(){
		try{//RT_COLOR_STD
			return	mService.getCurAtvColorStd();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getCurAtvColorStdNoAuto(){
		try{//RT_COLOR_STD
			return	mService.getCurAtvColorStdNoAuto();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAvColorStd(){
		try{//RT_COLOR_STD
			return	mService.getAvColorStd();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAvColorStdNoAuto(){
		try{//RT_COLOR_STD
			return	mService.getAvColorStdNoAuto();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public char getVgaHPosition(){
		try{
			return	mService.getVgaHPosition();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public char getVgaVPosition(){
		try{
			return	mService.getVgaVPosition();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public char getVgaPhase(){
		try{
			return	mService.getVgaPhase();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public char getVgaClock(){
		try{
			return	mService.getVgaClock();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAspectRatio(int iSourceOption){
		try{//ENUM_ASPECT_RATIO --(int iSourceOption)
			return	mService.getAspectRatio(iSourceOption);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getMagicPicture(){
		try{//ENUM_MAGIC_PICTURE --()
			return mService.getMagicPicture();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getDCR(){
		try{//ENUM_DREAM_PANEL --()
			return mService.getDCR();
		}catch(RemoteException ex){
		}
		return 0;
	}
	public String getDCRDemoDate(){
		try{
			return	mService.getDCRDemoDate();
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getBacklight(){
		try{
			return mService.getBacklight();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int get3dMode(){
		try{
			return mService.get3dMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int get3dDeep(){
		try{
			return mService.get3dDeep();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean get3dLRSwap(){
		boolean result = false;

		try{
			result = mService.get3dLRSwap();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean get3dCvrt2D(){
		boolean result = false;

		try{
			result = mService.get3dCvrt2D();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getPanelOn(){
		boolean result = false;

		try{
			result = mService.getPanelOn();
		} catch(RemoteException ex){
		}
		return result;
	}

	public String getResolutionInfo(){
		try{
			return	mService.getResolutionInfo();
		}catch(RemoteException ex){
		}
		return null;
	}

	public boolean checkDviMode(){
		boolean result = false;

		try{
			result = mService.checkDviMode();
		} catch(RemoteException ex){
		}
		return result;
	}

	public String getOverScan(){
		try{
			return	mService.getOverScan();
		}catch(RemoteException ex){
		}
		return null;
	}

	public void setNoiseReduction_impulse(boolean enable){
		try{
			mService.setNoiseReduction_impulse(enable);
		}catch(RemoteException ex){
		}
	}

	//TvFactoryApiExecutor
	public void setScalerAutoColorRGain(int iValue){
		try{
			mService.setScalerAutoColorRGain(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setScalerAutoColorGGain(int iValue){
		try{
			mService.setScalerAutoColorGGain(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setScalerAutoColorBGain(int iValue){
		try{
			mService.setScalerAutoColorBGain(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setScalerAutoColorROffset(int iValue){
		try{
			mService.setScalerAutoColorROffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setScalerAutoColorGOffset(int iValue){
		try{
			mService.setScalerAutoColorGOffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setScalerAutoColorBOffset(int iValue){
		try{
			mService.setScalerAutoColorBOffset(iValue);
		}catch(RemoteException ex){
		}
	}

	//TvSoundSetupApiExecutor
	public void setVolume(int iValue){
		try{
			mService.setVolume(iValue);
		}catch(RemoteException ex){
		}
	}

    public void setMute(boolean mute) {
        try {
            mService.setMute(mute);
        } catch (RemoteException ex) {
        }
    }

    public void setApMute(boolean mute) {
        try {
            mService.setApMute(mute);
        } catch (RemoteException ex) {
        }
    }

	public void setNoSigMute(boolean mute) {
        try {
            mService.setNoSigMute(mute);
        } catch (RemoteException ex) {
        }
    }

	public void setHWClock() {
        try {
            mService.setHWClock();
        } catch (RemoteException ex) {
        }
    }

	public void setAndroidMode(int mode){
		try{
			mService.setAndroidMode(mode);
		}catch(RemoteException ex){
		}
	}

	public int getAndroidMode(){
		try{
			return	mService.getAndroidMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setBalance(int iValue){
		try{
			mService.setBalance(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setBass(int iValue){
		try{
			mService.setBass(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setTreble(int iValue){
		try{
			mService.setTreble(iValue);
		}catch(RemoteException ex){
		}
	}

    public void setTrueSurround(boolean bEnable) {
        try {
            mService.setTrueSurround(bEnable);
        } catch (RemoteException ex) {
        }
    }

    public void setClarity(boolean bEnable) {
        try {
            mService.setClarity(bEnable);
        } catch (RemoteException ex) {
        }
    }

    public void setTrueBass(boolean bEnable) {
        try {
            mService.setTrueBass(bEnable);
        } catch (RemoteException ex) {
        }
    }

    public void setSubWoof(boolean bEnable) {
        try {
            mService.setSubWoof(bEnable);
        } catch (RemoteException ex) {
        }
    }

	public void setSubWoofVolume(int iValue){
		try{
			mService.setSubWoofVolume(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setEqualizerMode(int iMode){
		try{
			mService.setEqualizerMode(iMode);
		}catch(RemoteException ex){
		}
	}

	public void setEqualizer(int iFreq, int iValue){
		try{
			mService.setEqualizer(iFreq, iValue);
		}catch(RemoteException ex){
		}
	}

	public void setGraphicEqualizer(int iFreq, int iValue){
		try{
			mService.setGraphicEqualizer(iFreq, iValue);
		}catch(RemoteException ex){
		}
	}

	public int getGraphicEqualizer(int iFreq){
		try{
			return	mService.getGraphicEqualizer(iFreq);
		}catch(RemoteException ex){
		}
		return 0;
	}

    public void setOnOffMusic(boolean bOn) {
        try {
            mService.setOnOffMusic(bOn);
        } catch (RemoteException ex) {
        }
    }

	public void setAudioHdmiOutput(int mode){
		try{
			mService.setAudioHdmiOutput(mode);
		}catch(RemoteException ex){
		}
	}

	public void setSurroundSound(int mode){
		try{
			mService.setSurroundSound(mode);
		}catch(RemoteException ex){
		}
	}

	public void setAudioSpdifOutput(int mode){
		try{
			mService.setAudioSpdifOutput(mode);
		}catch(RemoteException ex){
		}
	}

	public void setWallEffect(boolean bEnable) {
        try {
            mService.setWallEffect(bEnable);
        } catch (RemoteException ex) {
        }
    }

	public void setAudioMode(int mode){
		try{
			mService.setAudioMode(mode);
		}catch(RemoteException ex){
		}
	}

	public void setAudioEffect(int audioEffect, int param){
		try{
			mService.setAudioEffect(audioEffect, param);
		}catch(RemoteException ex){
		}
	}

	public void setKeyToneVolume(int iVol){
		try{
			mService.setKeyToneVolume(iVol);
		}catch(RemoteException ex){
		}
	}

	/**
	 * TvServer's SetSource() will restore audio swapped channel to default value.
	 * MediaPlayServer will restore audio swapped channel to default value when user stops to play media.
	 */
	public void setAudioChannelSwap(int sel){
		//AUDIO_AO_CHANNEL_OUT_SWAP
		try{
			mService.setAudioChannelSwap(sel);
		}catch(RemoteException ex){
		}
	}

	public boolean setAutoVolume(boolean bEnable){
		try{
			return mService.setAutoVolume(bEnable);
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setASFEnable(boolean bEnable){
		try{
			mService.setASFEnable(bEnable);
		}catch(RemoteException ex){
		}
	}

	public void setASFBalance(int iValue){
		try{
			mService.setASFBalance(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setASFVolume(int iValue){
		try{
			mService.setASFVolume(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setVolume2TvServerDirectly(int iValue){
		try{
			mService.setVolume2TvServerDirectly(iValue);
		}catch(RemoteException ex){
		}
	}

	public void AMP_SetBalance(int iValue){
		try{
			mService.AMP_SetBalance(iValue);
		}catch(RemoteException ex){
		}
	}

	public void AMP_SetGraphicEqualiser(int Frequency, int eqVal){
		try{
			mService.AMP_SetGraphicEqualiser(Frequency, eqVal);
		}catch(RemoteException ex){
		}
	}
	
	public void AMP_SetSurround(boolean bIsEnable){
		try{
			mService.AMP_SetSurround(bIsEnable);
		}catch(RemoteException ex){
		}
	}
	
	public void AMP_SetPowerBassBooster( int PowerBassBoosterMode ){
		try{
			mService.AMP_SetPowerBassBooster(PowerBassBoosterMode);
		}catch(RemoteException ex){
		}
	}
	

	public boolean getAutoVolume(){
		boolean result = false;

		try{
			result = mService.getAutoVolume();
		} catch(RemoteException ex){
		}
		return result;
	}

	public int getVolume(){
		try{
			return mService.getVolume();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean getMute(){
		boolean result = false;

		try{
			result = mService.getMute();
		} catch(RemoteException ex){
		}
		return result;
	}

	public int getBalance(){
		try{
			return	mService.getBalance();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getBass(){
		try{
			return	mService.getBass();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getTreble(){
		try{
			return	mService.getTreble();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getEqualizerMode(){
		try{
			return	mService.getEqualizerMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getEqualizer(int iFreq){
		try{
			return	mService.getEqualizer(iFreq);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean getTrueSurround(){
		boolean result = false;

		try{
			result = mService.getTrueSurround();
		} catch(RemoteException ex){
		}

		return result;
	}

	public boolean getClarity(){
		boolean result = false;

		try{
			result = mService.getClarity();
		} catch(RemoteException ex){
		}

		return result;
	}

	public boolean getTrueBass(){
		boolean result = false;

		try{
			result = mService.getTrueBass();
		} catch(RemoteException ex){
		}

		return result;
	}

	public boolean getSubWoof(){
		boolean result = false;

		try{
			result = mService.getSubWoof();
		} catch(RemoteException ex){
		}

		return result;
	}

	public int getSubWoofVolume(){
		try{
			return	mService.getSubWoofVolume();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean getOnOffMusic(){
		boolean result = false;

		try{
			result = mService.getOnOffMusic();
		} catch(RemoteException ex){
		}

		return result;
	}

	public boolean getWallEffect(){
		boolean result = false;

		try{
			result = mService.getWallEffect();
		} catch(RemoteException ex){
		}

		return result;
	}

	public int getAudioMode(){
		try{
			return	mService.getAudioMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean getAudioVolume(){
		boolean result = false;

		try{
			result = mService.getAudioVolume();
		} catch(RemoteException ex){
		}

		return result;
	}

	public int getKeyToneVolume(){
		try{
			return	mService.getKeyToneVolume();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAudioChannelSwap(){
		try{
			return	mService.getAudioChannelSwap();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean getASFEnable(){
		boolean result = false; // The default value is false.

		try{
			result = mService.getASFEnable();
		}catch(RemoteException ex){
		}

		return result;
	}

	public int getASFBalance(){
		int result = -3; // The default value is -3.

		try{
			result = mService.getASFBalance();
		}catch(RemoteException ex){
		}

		return result;
	}

	public int getASFVolume(){
		int result = 30; // The default value is 30.

		try{
			result = mService.getASFVolume();
		}catch(RemoteException ex){
		}

		return result;
	}
	public int AMP_GetBalance(){
		int result = 0; // The default value is 0.

		try{
			result = mService.AMP_GetBalance();
		}catch(RemoteException ex){
		}

		return result;
	}
	public int AMP_GetGraphicEqualiser(int Frequency){
		int result = 0; // The default value is 0.

		try{
			result = mService.AMP_GetGraphicEqualiser( Frequency );
		}catch(RemoteException ex){
		}

		return result;
	}
	public boolean AMP_GetSurround(){
		boolean result = false; // The default value is 0.

		try{
			result = mService.AMP_GetSurround();
		}catch(RemoteException ex){
		}

		return result;
	}
	public int AMP_GetPowerBassBooster(){
		int result = 0; // The default value is 0.

		try{
			result = mService.AMP_GetPowerBassBooster();
		}catch(RemoteException ex){
		}

		return result;
	}
	
	//Invoke WLanSetupApiExecutor functions
	void setWLanTmpProfileName(String pStrName){
		try{
			mService.setWLanTmpProfileName(pStrName);
		}catch(RemoteException ex){
		}
	}

	void setWLanTmpProfileSSID(String pStrSSID){
		try{
			mService.setWLanTmpProfileSSID(pStrSSID);
		}catch(RemoteException ex){
		}
	}

	public void setWLanTmpProfileWifiMode(int mode){
		//ENUM_WIRELESS_MODE
		try{
			mService.setWLanTmpProfileWifiMode(mode);
		}catch(RemoteException ex){
		}
	}

	public void setWLanTmpProfileWifiSecurity(int security, String pStrKey){
		try{//ENUM_WIRELESS_SECURITY
			mService.setWLanTmpProfileWifiSecurity(security, pStrKey);
		}catch(RemoteException ex){
		}
	}

	public void setWLanTmpProfileDhcpHostIp(char ip1, char ip2, char ip3, char ip4){
		try{
			mService.setWLanTmpProfileDhcpHostIp(ip1, ip2, ip3, ip4);
		}catch(RemoteException ex){
		}
	}

	public void setWLanTmpProfileDhcpStartIp(char ip1, char ip2, char ip3, char ip4){
		try{
			mService.setWLanTmpProfileDhcpStartIp(ip1, ip2, ip3, ip4);
		}catch(RemoteException ex){
		}
	}

	public void setWLanTmpProfileDhcpEndIp(char ip1, char ip2, char ip3, char ip4){
		try{
			mService.setWLanTmpProfileDhcpEndIp(ip1, ip2, ip3, ip4);
		}catch(RemoteException ex){
		}
	}

	public void setWLanTmpProfileWepIndex(int iIndex){
		try{
			mService.setWLanTmpProfileWepIndex(iIndex);
		}catch(RemoteException ex){
		}
	}

	public void setWLanProfileCopyToTmp(int iProfileIndex){
		try{
			mService.setWLanProfileCopyToTmp(iProfileIndex);
		}catch(RemoteException ex){
		}
	}

	public void setWLanProfileCopyFromTmp(int iProfileIndex){
		try{
			mService.setWLanProfileCopyFromTmp(iProfileIndex);
		}catch(RemoteException ex){
		}
	}

	public void setWLanIpAddr(int netType, char ip1, char ip2, char ip3, char ip4){
		try{//ENUM_NET_IP_TYPE
			mService.setWLanIpAddr(netType, ip1, ip2, ip3, ip4);
		}catch(RemoteException ex){
		}
	}

	public void setWLanProfileActiveIndex(int iProfileIndex){
		try{
			mService.setWLanProfileActiveIndex(iProfileIndex);
		}catch(RemoteException ex){
		}
	}

	public String getWLanProfileName(int iProfileIndex){
		try{
			return	mService.getWLanProfileName(iProfileIndex);
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getWLanProfileSSID(int iProfileIndex){
		try{
			return	mService.getWLanProfileSSID(iProfileIndex);
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getWLanProfileWifiMode(int iProfileIndex){
		try{//ENUM_WIRELESS_MODE --(int iProfileIndex)
			return	mService.getWLanProfileWifiMode(iProfileIndex);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getWLanProfileWifiSecurity(int iProfileIndex){
		try{//ENUM_WIRELESS_SECURITY --(int iProfileIndex)
			return	mService.getWLanProfileWifiSecurity(iProfileIndex);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getWLanProfileDhcpHostIp(int iProfileIndex){
		try{
			return	mService.getWLanProfileDhcpHostIp(iProfileIndex);
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getWLanProfileDhcpStartIp(int iProfileIndex){
		try{
			return	mService.getWLanProfileDhcpStartIp(iProfileIndex);
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getWLanProfileDhcpEndIp(int iProfileIndex){
		try{
			return	mService.getWLanProfileDhcpEndIp(iProfileIndex);
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getWLanProfileWepIndex(int iProfileIndex){
		try{
			return	mService.getWLanProfileWepIndex(iProfileIndex);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getWLanProfileActiveIndex(){
		try{
			return	mService.getWLanProfileActiveIndex();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getWLanProfileTotalNumber(){
		try{
			return	mService.getWLanProfileTotalNumber();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getWLanIpAddr(int netType){
		try{//ENUM_NET_IP_TYPE
			return	mService.getWLanIpAddr(netType);
		}catch(RemoteException ex){
		}
		return null;
	}

	public boolean getWLanDHCPState(){
		try{
			return	mService.getWLanDHCPState();
		}catch(RemoteException ex){
		}
		return false;
	}

	public int getWLanApListSize(){
		try{
			return	mService.getWLanApListSize();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getWLanApName(int iApIndex){
		try{
			return	mService.getWLanApName(iApIndex);
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getWLanApSecurity(int iApIndex){
		try{//ENUM_WIRELESS_SECURITY
			return	mService.getWLanApSecurity(iApIndex);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getWLanApStrength(int iApIndex){
		try{
			return	mService.getWLanApStrength(iApIndex);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setWLanWpsMode(int mode){
		try{//ENUM_WPS_MODE
			mService.setWLanWpsMode(mode);
		}catch(RemoteException ex){
		}
	}

	public int getWLanWpsMode(){
		try{//ENUM_WPS_MODE --()
			return	mService.getWLanWpsMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getWLanPinCode(){
		try{
			return	mService.getWLanPinCode();
		}catch(RemoteException ex){
		}
		return null;
	}

	public void wLanConnectStart(int iProfileIndex){
		try{
			mService.wLanConnectStart(iProfileIndex);
		}catch(RemoteException ex){
		}
	}

	public void wLanConnectStop(boolean bForce){
		try{
			mService.wLanConnectStop(bForce);
		}catch(RemoteException ex){
		}
	}

	public int wLanConnectQueryState(){
		try{//NET_WIRELESS_STATE --()
			return	mService.wLanConnectQueryState();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int wLan0ActivateState(){
		try{//NET_WIRELESS_STATE --()
			return	mService.wLan0ActivateState();
		}catch(RemoteException ex){
		}
		return 0;
	}

	//LanSetupApiExecutor
	public boolean wiredLanDHCPStart(){
		try{
			return	mService.wiredLanDHCPStart();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void wiredLanDhcpStop(boolean bForceStop){
		try{
			mService.wiredLanDhcpStop(bForceStop);
		}catch(RemoteException ex){
		}
	}

	public int wiredLanDhcpQueryState(){
		try{//NET_WIRED_DHCP_STATE --()
			return	mService.wiredLanDhcpQueryState();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean getWiredLanDhcpEnable(){
		try{
			return	mService.getWiredLanDhcpEnable();
		}catch(RemoteException ex){
		}
		return false;
	}

	public String getWiredLanIpAddr(int netType, boolean bFromDatabase){
		//String --(ENUM_NET_IP_TYPE, boolean)
		try{
			return	mService.getWiredLanIpAddr(netType, bFromDatabase);
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getMacAddressInfo(int iNetInterface){
		//String --(ENUM_NET_INTERFACE)
		try{
			return	mService.getMacAddressInfo(iNetInterface);
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getMacAddress(){
		try{
			return	mService.getMacAddress();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getSystemVersion(){
		try{
			return	mService.getSystemVersion();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getBootcodeVersion(){
		try{
			return	mService.getBootcodeVersion();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getEepVersion(){
		try{
			return	mService.getEepVersion();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getCpuVersion(){
		try{
			return	mService.getCpuVersion();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getReleaseDate(){
		try{
			return	mService.getReleaseDate();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getPanelName(){
		try{
			return	mService.getPanelName();
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getUartMode(){
		try{
			return	mService.getUartMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getEepPage(){
		try{
			return	mService.getEepPage();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getEepOffset(){
		try{
			return	mService.getEepOffset();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getEepData(){
		try{
			return	mService.getEepData();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setUartMode(int iValue){
		try{
			mService.setUartMode(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setEepPage(int iValue){
		try{
			mService.setEepPage(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setEepOffset(int iValue){
		try{
			mService.setEepOffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setEepData(int iValue){
		try{
			mService.setEepData(iValue);
		}catch(RemoteException ex){
		}
	}

	public void startWifi(){
		try{
			mService.startWifi();
		}catch(RemoteException ex){
		}
	}

	public void stopWifi(){
		try{
			mService.stopWifi();
		}catch(RemoteException ex){
		}
	}

	public boolean getWifiState(){
		try{
			return	mService.getWifiState();
		}catch(RemoteException ex){
		}
		return false;
	}

	public int getNsta(int type){
		try{
			return	mService.getNsta(type);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setNsta(int type,int iValue){
		try{
			mService.setNsta(type,iValue);
		}catch(RemoteException ex){
		}
	}

	public int getPattern(){
		try{
			return	mService.getPattern();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setPattern(int iValue){
		try{
			mService.setPattern(iValue);
		}catch(RemoteException ex){
		}
	}

	public void rebootSystem(){
		try{
			mService.rebootSystem();
		}catch(RemoteException ex){
		}
	}

	public void setFacSingleKey(boolean mode){
		try{
			mService.setFacSingleKey(mode);
		}catch(RemoteException ex){
		}
	}

	public boolean getFacSingleKey(){
		try{
			return	mService.getFacSingleKey();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setWarmMode(boolean mode){
		try{
			mService.setWarmMode(mode);
		}catch(RemoteException ex){
		}
	}

	public void exitSkyworthFactorySet(){
		try{
			mService.exitSkyworthFactorySet();
		}catch(RemoteException ex){
		}
	}

	public void setBusoffMode(boolean mode){
		try{
			mService.setBusoffMode(mode);
		}catch(RemoteException ex){
		}
	}

	public boolean getFacAutoScanGuide(){
		try{
			return	mService.getFacAutoScanGuide();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setFacAutoScanGuide(boolean mode){
		try{
			mService.setFacAutoScanGuide(mode);
		}catch(RemoteException ex){
		}
	}

	public boolean getFacWarmMode(){
		try{
			return	mService.getFacWarmMode();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setFacWarmMode(boolean mode){
		try{
			mService.setFacWarmMode(mode);
		}catch(RemoteException ex){
		}
	}

	public boolean getDDREnable(){
		try{
			return	mService.getDDREnable();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setDDREnable(boolean mode){
		try{
			mService.setDDREnable(mode);
		}catch(RemoteException ex){
		}
	}

	public boolean getDDRPhaseShift(){
		try{
			return	mService.getDDRPhaseShift();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setDDRPhaseShift(boolean mode){
		try{
			mService.setDDRPhaseShift(mode);
		}catch(RemoteException ex){
		}
	}

	public int getDDRStep(){
		try{
			return	mService.getDDRStep();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setDDRStep(int iValue){
		try{
			mService.setDDRStep(iValue);
		}catch(RemoteException ex){
		}
	}

	public int getDDRPeriod(){
		try{
			return	mService.getDDRPeriod();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setDDRPeriod(int iValue){
		try{
			mService.setDDRPeriod(iValue);
		}catch(RemoteException ex){
		}
	}

	public int getDDROffset(){
		try{
			return	mService.getDDROffset();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setDDROffset(int iValue){
		try{
			mService.setDDROffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public boolean getLVDSEnable(){
		try{
			return	mService.getLVDSEnable();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setLVDSEnable(boolean mode){
		try{
			mService.setLVDSEnable(mode);
		}catch(RemoteException ex){
		}
	}

	public int getLVDSDclkRange(){
		try{
			return	mService.getLVDSDclkRange();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setLVDSDclkRange(int iValue){
		try{
			mService.setLVDSDclkRange(iValue);
		}catch(RemoteException ex){
		}
	}

	public int getLVDSDclkFMDIV(){
		try{
			return	mService.getLVDSDclkFMDIV();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setLVDSDclkFMDIV(int iValue){
		try{
			mService.setLVDSDclkFMDIV(iValue);
		}catch(RemoteException ex){
		}
	}

	public boolean getLVDSNewMode(){
		try{
			return	mService.getLVDSNewMode();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setLVDSNewMode(boolean mode){
		try{
			mService.setLVDSNewMode(mode);
		}catch(RemoteException ex){
		}
	}

	public int getLVDSPLLOffset(){
		try{
			return	mService.getLVDSPLLOffset();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setLVDSPLLOffset(int iValue){
		try{
			mService.setLVDSPLLOffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public boolean getLVDSOnlyEvenOdd(){
		try{
			return	mService.getLVDSOnlyEvenOdd();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setLVDSOnlyEvenOdd(boolean mode){
		try{
			mService.setLVDSOnlyEvenOdd(mode);
		}catch(RemoteException ex){
		}
	}

	public boolean getLVDSEvenOrOdd(){
		try{
			return	mService.getLVDSEvenOrOdd();
		}catch(RemoteException ex){
		}
		return false;
	}

	public void setLVDSEvenOrOdd(boolean mode){
		try{
			mService.setLVDSEvenOrOdd(mode);
		}catch(RemoteException ex){
		}
	}

	public int getLVDSDrivingCurrent(){
		try{
			return	mService.getLVDSDrivingCurrent();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setLVDSDrivingCurrent(int iValue){
		try{
			mService.setLVDSDrivingCurrent(iValue);
		}catch(RemoteException ex){
		}
	}

	public void set_LED_Green(){
		try{
			mService.set_LED_Green();
		}catch(RemoteException ex){
		}
	}

	public void set_LED_Red(){
		try{
			mService.set_LED_Red();
		}catch(RemoteException ex){
		}
	}

	public void set_LED_Orange(){
		try{
			mService.set_LED_Orange();
		}catch(RemoteException ex){
		}
	}

	public String getBARCODE1(){
		try{
			return	mService.getBARCODE1();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getBARCODE2(){
		try{
			return	mService.getBARCODE2();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getBARCODE3(){
		try{
			return	mService.getBARCODE3();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getBARCODE4(){
		try{
			return	mService.getBARCODE4();
		}catch(RemoteException ex){
		}
		return null;
	}

	public void setBARCODE1(char bar1, char bar2, char bar3, char bar4, char bar5, char bar6, char bar7, char bar8){
		try{
			mService.setBARCODE1(bar1, bar2, bar3, bar4, bar5, bar6, bar7, bar8);
		}catch(RemoteException ex){
		}
	}

	public void setBARCODE2(char bar1, char bar2, char bar3, char bar4, char bar5, char bar6, char bar7, char bar8){
		try{
			mService.setBARCODE2(bar1, bar2, bar3, bar4, bar5, bar6, bar7, bar8);
		}catch(RemoteException ex){
		}
	}

	public void setBARCODE3(char bar1, char bar2, char bar3, char bar4, char bar5, char bar6, char bar7, char bar8){
		try{
			mService.setBARCODE3(bar1, bar2, bar3, bar4, bar5, bar6, bar7, bar8);
		}catch(RemoteException ex){
		}
	}

	public void setBARCODE4(char bar1, char bar2, char bar3, char bar4, char bar5, char bar6, char bar7, char bar8){
		try{
			mService.setBARCODE4(bar1, bar2, bar3, bar4, bar5, bar6, bar7, bar8);
		}catch(RemoteException ex){
		}
	}

	public int getColorTempData(int index){
		try{
			return	mService.getColorTempData(index);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setColorTempData(int index,int iValue){
		try{
			mService.setColorTempData(index,iValue);
		}catch(RemoteException ex){
		}
	}

	public boolean setScalerAutoColor(){
		boolean result = false;
		try{
			result = mService.setScalerAutoColor();
		}catch(RemoteException ex){
		}
		return result;
	}

	public void resetColorTemp(){
		try{
			mService.resetColorTemp();
		}catch(RemoteException ex){
		}
	}


	public void setWiredLanManualInit(){
		try{
			mService.setWiredLanManualInit();
		}catch(RemoteException ex){
		}
	}

	public void setWiredLanManualIp(){
		try{
			mService.setWiredLanManualIp();
		}catch(RemoteException ex){
		}
	}

	public void setWiredLanIpAddr(int netType, char ip1, char ip2, char ip3, char ip4){
		try{//ENUM_NET_IP_TYPE
			mService.setWiredLanIpAddr(netType, ip1, ip2, ip3, ip4);
		}catch(RemoteException ex){
		}
	}

	public void setMacAddress(char mac1, char mac2, char mac3, char mac4, char mac5, char mac6){
		try{//ENUM_NET_IP_TYPE
			mService.setMacAddress(mac1, mac2, mac3, mac4, mac5, mac6);
		}catch(RemoteException ex){
		}
	}


	//ImageDecoderApiExecutor
	public long startDecodeImage(boolean bBackupHttpFileSource){
		try{
			return mService.startDecodeImage(bBackupHttpFileSource);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void decodeImage(String pFilePath, int transitionType){
		//VIDEO_TRANSITION_TYPE
		try{
			mService.decodeImage(pFilePath, transitionType);
		}catch(RemoteException ex){
		}
	}

	public void decodeImageEx(String pFilePath, int transitionType, boolean bUpnpFile){
                try{
                        mService.decodeImageEx(pFilePath, transitionType, bUpnpFile);
                }catch(RemoteException ex){
                }
        }

	public long getDecodeImageResult(){
		try{
			return	mService.getDecodeImageResult();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void stopDecodeImage(){
		try{
			mService.stopDecodeImage();
		}catch(RemoteException ex){
		}
	}

	public void zoomIn(){
		try{
			mService.zoomIn();
		}catch(RemoteException ex){
		}
	}

	public void zoomOut(){
		try{
			mService.zoomOut();
		}catch(RemoteException ex){
		}
	}

	public void leftRotate(){
		try{
			mService.leftRotate();
		}catch(RemoteException ex){
		}
	}

	public void rightRotate(){
		try{
			mService.rightRotate();
		}catch(RemoteException ex){
		}
	}

	public void upRotate(){
		try{
			mService.upRotate();
		}catch(RemoteException ex){
		}
	}

	public void downRotate(){
		try{
			mService.downRotate();
		}catch(RemoteException ex){
		}
	}


	public void enableQFHD(){
		try{
			mService.enableQFHD();
		}catch(RemoteException ex){
		}
	}

	public void disableQFHD(){
		try{
			mService.disableQFHD();
		}catch(RemoteException ex){
		}

	}

    	public void setSuperResolutionMode(boolean enable){
            try{
                mService.setSuperResolutionMode(enable);
            }catch(RemoteException ex){
            }
	}

	public int getCurQuadFHDMode(){
		try{
			return  mService.getCurQuadFHDMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean setCurQuadFHDMode(int mode){
		//QUADFHD_SETTING_MODE_TYPE
	    boolean result = false;

        try {
            result = mService.setCurQuadFHDMode(mode);
        } catch (RemoteException ex) {
        }
        return result;
	}

	//NoSignal DisplayReady
	public boolean getNoSignalDisplayReady(){
		try{
			/**
			 * NoSignal = true
			 * DisplayReady = false
			 */
			return mService.getNoSignalDisplayReady();
		}catch( RemoteException ex ){
		}
		return true;
	}

	public void setEnableBroadcast(boolean enable){
		try{
			mService.setEnableBroadcast(enable);
		}catch(RemoteException ex){
		}
	}

	public void setVideoAreaOn(int x, int y, int w, int h, int plane){
		try{
			mService.setVideoAreaOn(x, y, w, h, plane);
		}catch(RemoteException ex){
		}

	}

	public void setVideoAreaOff(int plane){
		try{
			mService.setVideoAreaOff(plane);
		}catch(RemoteException ex){
		}

	}

    public boolean gdmaShow(int idx, int enable) {
        boolean result = false;

        try {
            result = mService.gdmaShow(idx, enable);
        } catch (RemoteException ex) {
        }

        return result;
    }

    public boolean gldcOsdShow(int mode) {
        boolean result = false;

        try {
            result = mService.gldcOsdShow(mode);
        } catch (RemoteException ex) {
        }

        return result;
    }

	public boolean isKeyDown(int key) {
        boolean result = false;

        try {
            result = mService.isKeyDown(key);
        } catch (RemoteException ex) {
        }

        return result;
    }

	public void setInitialFlag(boolean bInitial) {
		try {
			mService.setInitialFlag(bInitial);
		} catch (RemoteException ex) {
		}
	}

	public boolean getInitialFlag() {
		boolean result = false;

		try {
			result = mService.getInitialFlag();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public int getDtvTime(){
		try{
			return  mService.getDtvTime();
		}catch(RemoteException ex){
		}
		return 0;
	}

    public boolean setRTKIRMouse(boolean setting) {
        boolean result = false;

        try {
            result = mService.setRTKIRMouse(setting);
        } catch (RemoteException ex) {
        }

        return result;
    }

	public String getChannelNameList(int iStartIdx, int iContentLen, boolean bFilter) {
		try{
			return	mService.getChannelNameList(iStartIdx, iContentLen, bFilter);
		}catch(RemoteException ex){
		}

		return null;
	}

	public String getCurrentProgramInfo() {
		try{
			return	mService.getCurrentProgramInfo();
		}catch(RemoteException ex){
		}

		return null;
	}

	public String getCurrentProgramDescription() {
		try{
			return	mService.getCurrentProgramDescription();
		}catch(RemoteException ex){
		}

		return null;
	}

	public String getCurrentProgramRating() {
		try{
			return	mService.getCurrentProgramRating();
		}catch(RemoteException ex){
		}

		return null;
	}

	public boolean hasCurrentProgramWithSubtitle() {
		boolean result = false;

		try {
			result = mService.hasCurrentProgramWithSubtitle();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public String getCurAtvSoundSelect(){
		try{
			return	mService.getCurAtvSoundSelect();
		}catch(RemoteException ex){
		}
		return null;
	}

	public String getCurrentAudioLang() {
		try{
			return	mService.getCurrentAudioLang();
		}catch(RemoteException ex){
		}

		return null;
	}

	public String getCurInputInfo() {
		try{
			return	mService.getCurInputInfo();
		}catch(RemoteException ex){
		}

		return null;
	}

	public String getCurrentSetting_tv(String tvStr) {
		try{
			return	mService.getCurrentSetting_tv(tvStr);
		}catch(RemoteException ex){
		}

		return null;
	}

   	public void setFunctionParser(int paramcounter, String param){
   		Log.w("TVMANAGER~~~~~~~~~~~~~~~~~~~~~~~", "setFunctionParser~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		try{
			mService.setFunctionParser(paramcounter, param);
		}catch(RemoteException ex){
		}
	}

   	public String getFunctionParser(int paramcounter, String param){
   		Log.w("TVMANAGER~~~~~~~~~~~~~~~~~~~~~~~", "getFunctionParser~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		try{
			return mService.getFunctionParser(paramcounter, param);
		}catch(RemoteException ex){
		}
		return null;
	}

	public int getChannelFreqCount(){
		try{
			return	mService.getChannelFreqCount();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getChannelFreqByTableIndex(int index){
		try{
			return	mService.getChannelFreqByTableIndex(index);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getChannelchannelNumByTableIndex(int index){
		try{
			return	mService.getChannelchannelNumByTableIndex(index);
		}catch(RemoteException ex){
		}

		return null;
	}

	public int getChannelCountByFreq(int freq){
		try{
			return	mService.getChannelCountByFreq(freq);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getChannelCountByFreqAndServiceType(int freq, int serviceType){
		try{
			return	mService.getChannelCountByFreqAndServiceType(freq, serviceType);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getCurChannelIndex(){
		try{
			return	mService.getCurChannelIndex();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getChannelListChannelCount(){
		try{
			return	mService.getChannelListChannelCount();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getChannelDataList(int iStartIdx, int iContentLen){
		try{
			return	mService.getChannelDataList(iStartIdx, iContentLen);
		}catch(RemoteException ex){
		}

		return null;
	}

	public int getChannelListChannelCountByServiceType(int serviceType){
		try{
			return	mService.getChannelListChannelCountByServiceType(serviceType);
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getChannelNameListByServiceType(int iStartIdx, int iContentLen, int serviceType){
		try{
			return	mService.getChannelNameListByServiceType(iStartIdx, iContentLen, serviceType);
		}catch(RemoteException ex){
		}

		return null;
	}

	public String getChannelDataListByServiceType(int iStartIdx, int iContentLen, int serviceType){
		try{
			return	mService.getChannelDataListByServiceType(iStartIdx, iContentLen, serviceType);
		}catch(RemoteException ex){
		}

		return null;
	}

	public void addScheduleProgram(int startTime, int endTime, int eventId, int progNo, String eventName, String progName){
		try {
			mService.addScheduleProgram(startTime, endTime, eventId, progNo, eventName, progName);
		} catch (RemoteException ex) {
		}
	}

	public boolean delScheduleProgramByIndex(int iCurListIdx){
		boolean result = false;

		try {
			result = mService.delScheduleProgramByIndex(iCurListIdx);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void delAllScheduleProgram(){
		try {
			mService.delAllScheduleProgram();
		} catch (RemoteException ex) {
		}
	}

	public void setScheduleNotifyTime(int second){
		try {
			mService.setScheduleNotifyTime(second);
		} catch (RemoteException ex) {
		}
	}

	public int getScheduleNotifyTime(){
		try {
			return	mService.getScheduleNotifyTime();
		} catch (RemoteException ex) {
		}
		return 0;
	}

	public int getScheduleProgramListCount(){
		try{
			return	mService.getScheduleProgramListCount();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public String getScheduleProgramList(int iCurListIdx){
		try{
			return	mService.getScheduleProgramList(iCurListIdx);
		}catch(RemoteException ex){
		}

		return null;
	}

	public boolean recoverVideoSize() {
		boolean result = false;

		try {
			result = mService.recoverVideoSize();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public String getVideoSize() {
		try{
			return	mService.getVideoSize();
		}catch(RemoteException ex){
		}

		return null;
	}

	public void setVideoSize(int iX, int iY, int iWidth, int iHeight) {
		try {
			mService.setVideoSize(iX, iY, iWidth, iHeight);
		} catch (RemoteException ex) {
		}
	}

	public String getCurDtvSoundSelectList() {
		try{
			return	mService.getCurDtvSoundSelectList();
		}catch(RemoteException ex){
		}

		return null;
	}

	public int getCurDtvSoundSelectCount(){
		try{
			return	mService.getCurDtvSoundSelectCount();
		}catch(RemoteException ex){
		}
		return 0;
	}

  public boolean setCurDtvSoundSelectByIndex(int iAudioIndex){
    boolean result = false;

		try{
			result = mService.setCurDtvSoundSelectByIndex(iAudioIndex);
		}catch(RemoteException ex){
		}
		return result;
	}

	public String getCurAtvSoundSelectList() {
		try{
			return	mService.getCurAtvSoundSelectList();
		}catch(RemoteException ex){
		}

		return null;
	}

	public int getCurAtvSoundSelectCount(){
		try{
			return	mService.getCurAtvSoundSelectCount();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public boolean setDisplayFreeze(boolean enable) {
		boolean result = false;

		try {
			result = mService.setDisplayFreeze(enable);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public boolean getDisplayFreeze() {
		boolean result = false;

		try {
			result = mService.getDisplayFreeze();
		} catch (RemoteException ex) {
		}
		return result;
	}
	

	public void setCaptionMode(int mode) {
		try {
			mService.setCaptionMode(mode);
		} catch (RemoteException ex) {
		}
	}

	public void setAnalogCaption(int type) {
		try {
			mService.setAnalogCaption(type);
		} catch (RemoteException ex) {
		}
	}

	public void setDigitalCaption(int type) {
		try {
			mService.setDigitalCaption(type);
		} catch (RemoteException ex) {
		}
	}

	public boolean getDCC() {
		boolean result = false;

		try {
			result = mService.getDCC();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void setChannelLockEnable(boolean enable) {
		try {
			mService.setChannelLockEnable(enable);
		} catch (RemoteException ex) {
		}
	}

	public void setChannelFav(int index, boolean enable) {
		try {
			mService.setChannelFav(index, enable);
		} catch (RemoteException ex) {
		}
	}

	public void setChannelSkip(int index, boolean enable) {
		try {
			mService.setChannelSkip(index, enable);
		} catch (RemoteException ex) {
		}
	}

	public void setChannelBlock(int index, boolean enable) {
		try {
			mService.setChannelBlock(index, enable);
		} catch (RemoteException ex) {
		}
	}

	public void setChannelDel(int index, boolean enable) {
		try {
			mService.setChannelDel(index, enable);
		} catch (RemoteException ex) {
		}
	}

	public boolean getChannelFav(int index) {
		boolean result = false;

		try {
			result = mService.getChannelFav(index);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public boolean getChannelSkip(int index) {
		boolean result = false;

		try {
			result = mService.getChannelSkip(index);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public boolean getChannelBlock(int index) {
		boolean result = false;

		try {
			result = mService.getChannelBlock(index);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public boolean queryTvStatus(int type) {
		boolean result = false;

		try {
			result = mService.queryTvStatus(type);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void setHdmiAudioSource(int index) {
		try {
			mService.setHdmiAudioSource(index);
		} catch (RemoteException ex) {
		}
	}

	public boolean getIsContentLocked() {
		boolean result = false;

		try {
			result = mService.getIsContentLocked();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void setSourceLockEnable(boolean enable) {
		try {
			mService.setSourceLockEnable(enable);
		} catch (RemoteException ex) {
		}
	}

	public boolean getSourceLockStatus(int source) {
		boolean result = false;

		try {
			result = mService.getSourceLockStatus(source);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void setSourceLockStatus(int source, boolean lock) {
		try {
			mService.setSourceLockStatus(source, lock);
		} catch (RemoteException ex) {
		}
	}

	public boolean getSourceLockStatusByIndex(int srcIndex) {
		boolean result = false;

		try {
			result = mService.getSourceLockStatusByIndex(srcIndex);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void setSourceLockStatusByIndex(int srcIndex, boolean lock) {
		try {
			mService.setSourceLockStatusByIndex(srcIndex, lock);
		} catch (RemoteException ex) {
		}
	}

    public void setOverScanAndAdjustment(int h_ratio, int v_ratio, int h_start, int h_width, int v_start, int v_length, boolean applyalltiming, int customer){
		try{
			mService.setOverScanAndAdjustment(h_ratio, v_ratio, h_start, h_width, v_start, v_length, applyalltiming, customer);
		}catch(RemoteException ex){
		}
	}

	public String getOverScanAndAdjustment(int customer){
		try{
			return mService.getOverScanAndAdjustment(customer);
		}catch(RemoteException ex){
		}

		return null;
	}

	public int startRecordTs(String filePath, boolean bWithPreview) {
		try {
			return mService.startRecordTs(filePath, bWithPreview);
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public boolean stopRecordTs() {
		boolean result = false;

		try {
			result = mService.stopRecordTs();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void getEpgData(int iDayOffset, int iDayCount) {
		try {
			mService.getEpgData(iDayOffset, iDayCount);
		} catch (RemoteException ex) {
		}
	}

	public void getEpgDataByLCN(int u16Lcn, int iDayOffset, int iDayCount) {
		try {
			mService.getEpgDataByLCN(u16Lcn, iDayOffset, iDayCount);
		} catch (RemoteException ex) {
		}
	}

	public int getEpgListEpgCount() {
		try {
			return mService.getEpgListEpgCount();
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public String getEpgDataList(int iStartIdx, int iContentLen){
		try{
			return mService.getEpgDataList(iStartIdx, iContentLen);
		}catch(RemoteException ex){
		}

		return null;
	}
	public void setTvTimeZone(float timezone)
	{
		try {
			mService.setTvTimeZone(timezone);
		} catch (RemoteException ex) {
		}
	}
	public void setParentalLockEnable(boolean enable) {
		try {
			mService.setParentalLockEnable(enable);
		} catch (RemoteException ex) {
		}
	}

	public boolean getParentalLockEnable() {
		boolean result = false;

		try {
			result = mService.getParentalLockEnable();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void setParentalLockPasswd(int passwd) {
		try {
			mService.setParentalLockPasswd(passwd);
		} catch (RemoteException ex) {
		}
	}

	public int getParentalLockPasswd() {
		try {
			return mService.getParentalLockPasswd();
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public void setParentalLockRegion(int region) {
		try {
			mService.setParentalLockRegion(region);
		} catch (RemoteException ex) {
		}
	}

	public int getParentalLockRegion() {
		try {
			return mService.getParentalLockRegion();
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public void setParentalRatingDvb(int rating) {
		try {
			mService.setParentalRatingDvb(rating);
		} catch (RemoteException ex) {
		}
	}

	public int getParentalRatingDvb() {
		try {
			return mService.getParentalRatingDvb();
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public void setPasswordReverify(boolean isVerified) {
		try {
			mService.setPasswordReverify(isVerified);
		} catch (RemoteException ex) {
		}
	}

	public boolean getPasswordReverify() {
		boolean result = false;

		try {
			result = mService.getPasswordReverify();
		} catch (RemoteException ex) {
		}
		return result;
	}

	public void setPasswordVerified(int lockType) {
		try {
			mService.setPasswordVerified(lockType);
		} catch (RemoteException ex) {
		}
	}

	public boolean getPasswordVerified(int lockType) {
		boolean result = false;

		try {
			result = mService.getPasswordVerified(lockType);
		} catch (RemoteException ex) {
		}
		return result;
	}

	public void setPipEnable(boolean enable, boolean bKeepSubSourceAlive) {
		try {
			mService.setPipEnable(enable, bKeepSubSourceAlive);
		} catch (RemoteException ex) {
		}
	}

	public boolean getPipEnable() {
		boolean result = false;

		try {
			result = mService.getPipEnable();
		} catch (RemoteException ex) {
		}
		return result;
	}

	public void setPipSubSource(int subSource, boolean apply) {
		try {
			mService.setPipSubSource(subSource, apply);
		} catch (RemoteException ex) {
		}
	}

	public int getPipSource(int vout) {
		try {
			return mService.getPipSource(vout);
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public void clearPipSourceIndicator(int source) {
		try {
			mService.clearPipSourceIndicator(source);
		} catch (RemoteException ex) {
		}
	}

	public void setPipSubMode(int subMode) {
		try {
			mService.setPipSubMode(subMode);
		} catch (RemoteException ex) {
		}
	}

	public int getPipSubMode() {
		try {
			return mService.getPipSubMode();
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public void setPipSubPosition(int subPos) {
		try {
			mService.setPipSubPosition(subPos);
		} catch (RemoteException ex) {
		}
	}

	public int getPipSubPosition() {
		try {
			return mService.getPipSubPosition();
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public void setPipSubDisplayWindow(int x, int y, int width, int height) {
		try {
			mService.setPipSubDisplayWindow(x, y, width, height);
		} catch (RemoteException ex) {
		}
	}

	/*
	 * For recovery mode
	 */
	public String getRecoveryCmd(){
		try{
			return	mService.getRecoveryCmd();
		}catch(RemoteException ex){
		}
		return null;
	}

	public void setRecoveryCmd(String strCmd){
		try{
			mService.setRecoveryCmd(strCmd);
		}catch(RemoteException ex){
		}
	}

	/*
	 * Transcode Control - for TVAnywhere
	 */
	public void transcodeControlStart(){
		try{
			mService.transcodeControlStart();
		}
		catch(RemoteException ex){
		}
	}

	public void transcodeControlStop(){
		try{
			mService.transcodeControlStop();
		}
		catch(RemoteException ex){
		}
	}

	public void transcodeControlPause(){
		try{
			mService.transcodeControlPause();
		}
		catch(RemoteException ex){
		}
	}

	public void transcodeControlResume(){
		try{
			mService.transcodeControlResume();
		}
		catch(RemoteException ex){
		}
	}

	public void transcodeControlStartHttp(){
		try{
			mService.transcodeControlStartHttp();
		}
		catch(RemoteException ex){
		}
	}

	public void transcodeControlStopHttp(){
		try{
			mService.transcodeControlStopHttp();
		}
		catch(RemoteException ex){
		}
	}


    public String registerDivX(){
	    try{
	        return mService.registerDivX();
	    }
	    catch(RemoteException ex){
	    }
		return null;
	}

	public boolean isDeviceActivated(){
		boolean result=false;
		try{
			result = mService.isDeviceActivated();
		}
		catch(RemoteException ex){
		}
		return result;
	}

	public String deregisterDivX(){
		try{
			return mService.deregisterDivX();
		}
		catch(RemoteException ex){
		}
		return null;
	}

	public void enableSpectrumData(){
		try {
			mService.enableSpectrumData();
		}
		catch(RemoteException ex) {
		}
	}

	public void disableSpectrumData(){
		try {
			mService.disableSpectrumData();
		}
		catch(RemoteException ex) {
		}
	}

	public SpectrumDataInfo getSpectrumInfo() {
        try {
            return mService.getSpectrumInfo();
        } catch (RemoteException ex) {
            return null;
        }
    }

    public char getSignalStrength() {
        try {
            return mService.getSignalStrength();
        } catch (RemoteException ex) {
        }
        return 0;
    }

    public char getSignalQuality() {
        try {
            return mService.getSignalQuality();
        } catch (RemoteException ex) {
        }
        return 0;
    }

    public FreqTableInfo getFrequencyTable() {
        try {
            return mService.getFrequencyTable();
        } catch (RemoteException ex) {
            return null;
        }
    }

	//ravi_li add start
	public String getBseColorParam() {
        try {
            return mService.getBseColorParam();
        } catch (RemoteException ex) {
            return null;
        }
    }
	public void setBseColorParam(String strCmd) {
        try {
             mService.setBseColorParam( strCmd );
        } catch (RemoteException ex) {
            return;
        }
    }

	public void SetPowerSoundEqualiser( boolean enable){
   		Log.d("TvManager", "SetPowerSoundEqualiser");
		try{
			mService.SetPowerSoundEqualiser(enable);
		}catch(RemoteException ex){
		}
	}

	public void SetOnOffFunctionValue(String functionPath, boolean enable){
   		Log.d("TvManager", "SetOnOffFunctionValue");
		try{
			mService.SetOnOffFunctionValue(functionPath,enable);
		}catch(RemoteException ex){
		}
	}

	public void setCinemaMode(boolean bOn){
		try{
			mService.setCinemaMode(bOn);
		}catch(RemoteException ex){
		}
	}

	public void set3dColourManagement(boolean bOn){
		try{
			mService.set3dColourManagement(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setAudioDistortionControl(boolean bOn){
		try{
			mService.setAudioDistortionControl(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setNoSignalBlueScreen(boolean bOn){
		try{
			mService.setNoSignalBlueScreen(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setAutoNR(boolean bOn){
		try{
			mService.setAutoNR(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setAutoSignalBooster(boolean bOn){
		try{
			mService.setAutoSignalBooster(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setAutoPowerDown(boolean bOn){
		try{
			mService.setAutoPowerDown(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setEnableOnTImer(boolean bOn){
		try{
			mService.setEnableOnTImer(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setDisplayAutoFormat(boolean bOn){
		try{
			mService.setDisplayAutoFormat(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setDisplay43StretchFormat(boolean bOn){
		try{
			mService.setDisplay43StretchFormat(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setDynamicRangeControl(boolean bOn){
		try{
			mService.setDynamicRangeControl(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setResetTV(boolean bOn){
		try{
			mService.setResetTV(bOn);
		}catch(RemoteException ex){
		}
	}

	public void resetCurPictureMode(boolean bOn){
		try{
			mService.resetCurPictureMode(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setDCRTsb(boolean bOn){
		try{
			mService.setDCRTsb(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setStickDemoAutoEnable(boolean bOn){
		try{
			mService.setStickDemoAutoEnable(bOn);
		}catch(RemoteException ex){
		}
	}

	public void setRotateVedio(boolean bOn, int angle){
		try{
			mService.setRotateVedio(bOn,angle);
		}catch(RemoteException ex){
		}
	}

	public int getScalerInputSrcGetMainChType(){
		try{
			return	mService.getScalerInputSrcGetMainChType();
		}catch(RemoteException ex){
		}
		return 0;
	}
	
	public int getDrvifHDMIIsVideoTiming(){
		try{
			return	mService.getDrvifHDMIIsVideoTiming();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getScalerGetHDMIVideoModeFlag(){
		try{
			return	mService.getScalerGetHDMIVideoModeFlag();
		}catch(RemoteException ex){
		}
		return 0;
	}


	public boolean GetPowerSoundEqualiser(){
		boolean result = false;
   		Log.d("TvManager", "GetPowerSoundEqualiser");
		try{
			result = mService.GetPowerSoundEqualiser( );
		}catch(RemoteException ex){
		}
		return result;
	}


	public boolean GetOnOffFunctionValue(String funcName){
		boolean result = false;
   		Log.d("TvManager", "GetOnOffFunctionValue");
		try{
			result = mService.GetOnOffFunctionValue(funcName);
		}catch(RemoteException ex){
		}
		return result;
	}



	public boolean getCinemaMode(){
		boolean result = false;

		try{
			result = mService.getCinemaMode();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean get3dColourManagement(){
		boolean result = false;

		try{
			result = mService.get3dColourManagement();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getAudioDistortionControl(){
		boolean result = false;

		try{
			result = mService.getAudioDistortionControl();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getNoSignalBlueScreen(){
		boolean result = false;

		try{
			result = mService.getNoSignalBlueScreen();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getAutoNR(){
		boolean result = false;

		try{
			result = mService.getAutoNR();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getAutoSignalBooster(){
		boolean result = false;

		try{
			result = mService.getAutoSignalBooster();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getAutoPowerDown(){
		boolean result = false;

		try{
			result = mService.getAutoPowerDown();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getEnableOnTImer(){
		boolean result = false;

		try{
			result = mService.getEnableOnTImer();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getDisplayAutoFormat(){
		boolean result = false;

		try{
			result = mService.getDisplayAutoFormat();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getDisplay43StretchFormat(){
		boolean result = false;

		try{
			result = mService.getDisplay43StretchFormat();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getDynamicRangeControl(){
		boolean result = false;

		try{
			result = mService.getDynamicRangeControl();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getDCRTsb(){
		boolean result = false;

		try{
			result = mService.getDCRTsb();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getIsOnTimerWakeUp(){
		boolean result = false;

		try{
			result = mService.getIsOnTimerWakeUp();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean setTimeUntilPowerOnTime(){
		boolean result = false;

		try{
			result = mService.setTimeUntilPowerOnTime();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean killTimeUntilPowerOnTimer(){
		boolean result = false;

		try{
			result = mService.killTimeUntilPowerOnTimer();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean onTimerHandler(){
		boolean result = false;

		try{
			result = mService.onTimerHandler();
		} catch(RemoteException ex){
		}
		return result;
	}

	public boolean getStickDemoAutoEnable(){
		boolean result = false;

		try{
			result = mService.getStickDemoAutoEnable();
		}catch(RemoteException ex){
		}
		return result;
	}

	public boolean getIsDolbyDigitalContent(){
		boolean result = false;

		try{
			result = mService.getIsDolbyDigitalContent();
		}catch(RemoteException ex){
		}
		return result;
	}



	public void setOptionFuncValue(String funcName, int mode){
   		Log.d("TvManager", "setOptionFuncValue");
		try{
			mService.setOptionFuncValue(funcName,mode);
		}catch(RemoteException ex){
		}
	}

	public void setMpegNR(int iValue){
		try{
			mService.setMpegNR(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setCurAtvSoundSelect(int iValue){
		try{
			mService.setCurAtvSoundSelect(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setCurAtvSoundSelect_dual(int iValue){
		try{
			mService.setCurAtvSoundSelect_dual(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setAudioModeBassBoost(int iValue){
		try{
			mService.setAudioModeBassBoost(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setAudioModeResetFlag(int iValue){
		try{
			mService.setAudioModeResetFlag(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setOnTimerInput(int iValue){
		try{
			mService.setOnTimerInput(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setOnTimerPosition(int iValue){
		try{
			mService.setOnTimerPosition(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setOnTimerVolume(int iValue){
		try{
			mService.setOnTimerVolume(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setTimeUntilPowerOn(int iValue){
		try{
			mService.setTimeUntilPowerOn(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setTVLocation(int iValue){
		try{
			mService.setTVLocation(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setPcHdmiAudioPath(int iValue){
		try{
			mService.setPcHdmiAudioPath(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setAudioVolumeOffset(int iValue){
		try{
			mService.setAudioVolumeOffset(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setBrightnessTsb(int iValue){
		try{
			mService.setBrightnessTsb(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setNoSigBacklightTsb(){
		try{
			mService.setNoSigBacklightTsb();
		}catch(RemoteException ex){
		}
	}

	public void setColorTempModeTsb(int iValue){
		try{
			mService.setColorTempModeTsb(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setAudioModeTsb(int iValue){
		try{
			mService.setAudioModeTsb(iValue);
		}catch(RemoteException ex){
		}
	}

	public void setAdvancedSoundReset(int iValue){
		try{
			mService.setAdvancedSoundReset(iValue);
		}catch(RemoteException ex){
		}
	}


	public int getOptionFuncValue(String funcName){
		try {
			return mService.getOptionFuncValue( funcName );
		} catch (RemoteException ex) {
		}

		return 0;
	}

	public int getMpegNR(){
		try{
			return	mService.getMpegNR();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getCurAtvSoundSelect_dual(){
		try{
			return	mService.getCurAtvSoundSelect_dual();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAudioModeBassBoost(){
		try{
			return	mService.getAudioModeBassBoost();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAudioModeResetFlag(){
		try{
			return	mService.getAudioModeResetFlag();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getOnTimerInput(){
		try{
			return	mService.getOnTimerInput();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getOnTimerInput_UI(){
		try{
			return	mService.getOnTimerInput_UI();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getOnTimerPosition(){
		try{
			return	mService.getOnTimerPosition();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getOnTimerVolume(){
		try{
			return	mService.getOnTimerVolume();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getTimeUntilPowerOn(){
		try{
			return	mService.getTimeUntilPowerOn();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getTVLocation(){
		try{
			return	mService.getTVLocation();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getPcHdmiAudioPath(){
		try{
			return	mService.getPcHdmiAudioPath();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getHdmiAudioSource(){
		try{
			return	mService.getHdmiAudioSource();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAudioVolumeOffset(){
		try{
			return	mService.getAudioVolumeOffset();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getColorTempLevelTsb(){
		try{
			return	mService.getColorTempLevelTsb();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getOnTimerRemainTime(){
		try{
			return	mService.getOnTimerRemainTime();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public int getAudioModeTsb(){
		try{
			return	mService.getAudioModeTsb();
		}catch(RemoteException ex){
		}
		return 0;
	}


	public void setDCCTSB(int iDccValue, boolean iIsApply){
		try{
			mService.setDCCTSB(iDccValue, iIsApply);
		}catch(RemoteException ex){
		}
	}

	public void setSourceLabel(int source, String label){
		try{
			mService.setSourceLabel(source, label);
		}catch(RemoteException ex){
		}
	}

	public String getSourceLabel(int source ) {
		try {
			return  mService.getSourceLabel(source);
		} catch (RemoteException ex) {
		}

		return null;
	}

	public int getDCCTSB() {
		int result = 0;

		try {
			result = mService.getDCCTSB();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void setTeleTextMode(int Value){
		try{
			mService.setTeleTextMode(Value);
		}catch(RemoteException ex){
		}
	}

      public int getTeleTextMode() {
		int result = 0;

		try {
			result = mService.getTeleTextMode();
		} catch (RemoteException ex) {
		}

		return result;
	}

	 public void setTeleTextLanguage(int Value){
		try{
			mService.setTeleTextLanguage(Value);
		}catch(RemoteException ex){
		}
	}

      public int getTeleTextLanguage() {
		int result = 0;

		try {
			result = mService.getTeleTextLanguage();
		} catch (RemoteException ex) {
		}

		return result;
	}

        public void setTempTSBRGBGain(int rgb, int Value){
		try{
			mService.setTempTSBRGBGain(rgb,Value);
		}catch(RemoteException ex){
		}
	}

      public int getTempTSBRGBGain(int rgb){
		int result = 0;

		try {
			result = mService.getTempTSBRGBGain(rgb);
		} catch (RemoteException ex) {
		}

		return result;
	}

	public void onZoomMoveUp(){
		try {
			mService.onZoomMoveUp();
		}
		catch(RemoteException ex) {
		}
	}

	public void onZoomMoveDown(){
		try {
			mService.onZoomMoveDown();
		}
		catch(RemoteException ex) {
		}
	}

	public void onZoomMoveLeft(){
		try {
			mService.onZoomMoveLeft();
		}
		catch(RemoteException ex) {
		}
	}

	public void onZoomMoveRight(){
		try {
			mService.onZoomMoveRight();
		}
		catch(RemoteException ex) {
		}
	}

	// add by liufei Samba 20130507 begin
	public boolean pingHost(String host){
		boolean bIsOK = false;
		Log.d("TvManager", "pingHost");
		try{
			bIsOK = mService.pingHost(host);
			}
		catch(RemoteException ex){
			}
		return bIsOK;
		}
	// add by liufei Samba 20130507 end

	// add by liufei burn mac/sn/hdcpkey 20130507 begin
	public boolean writeMac(String mac){
		Log.d("TvManager", "writeMac");
		boolean result = false;
		try{
			Log.d("TvManager","writeMac try");
			result = mService.writeMac(mac);
		}catch(RemoteException ex){
			Log.d("TvManager","writeMac Exception");
		}
		return result;
	}
	public String getMac(){
		Log.d("TvManager", "getMac");
		try{
			Log.d("TvManager","getMac try");
			return mService.getMac();
		}catch(RemoteException ex){
			Log.d("TvManager","getMac Exception");
			}
		return null;
	}
	public String getMacFromUSB(){
		Log.d("TvManager","getMacFromUSB");
		try{
			Log.d("TvManager","getMacFromUSB try");
			return mService.getMacFromUSB();
		}catch(RemoteException ex){
			Log.d("TvManager","getMacFromUSB Exception");
			}
		return null;
	}
	public boolean writeSerialNumber(String sn){
		Log.d("TvManager", "writeSerialNumber");
		boolean result = false;
		try{
			Log.d("TvManager", "writeSerialNumber try");
			result = mService.writeSerialNumber(sn);
		}catch(RemoteException ex){
			Log.d("TvManager", "writeSerialNumber Exception");
		}
		return result;
	}
	public String getSerialNumber(){
		Log.d("TvManager", "getSerialNumber");
		try{
			Log.d("TvManager", "getSerialNumber try");
			return mService.getSerialNumber();
		}catch(RemoteException ex){
			Log.d("TvManager", "getSerialNumber Exception");
			}
		return null;
	}
	public String getSerialNumberFromUSB(){
		Log.d("TvManager", "getSerialNumberFromUSB");
		try{
			Log.d("TvManager", "getSerialNumberFromUSB try");
			return mService.getSerialNumberFromUSB();
		}catch(RemoteException ex){
			Log.d("TvManager", "getSerialNumberFromUSB Exception");
			}
		return null;
	}
	public boolean writeHDCPKey(String hdcpkey){
		Log.d("TvManager", "writeHDCPKey");
		boolean result = false;
		try{
			Log.d("TvManager","writeHDCPKey try");
			result = mService.writeHDCPKey(hdcpkey);
		}catch(RemoteException ex){
			Log.d("TvManager","writeHDCPKey Exception");
		}
		return result;
	}
	public String getHDCPKey(){
		Log.d("TvManager", "getHDCPKey");
		try{
			Log.d("TvManager","getHDCPKey try");
			return mService.getHDCPKey();
		}catch(RemoteException ex){
			Log.d("TvManager","getHDCPKey Exception");
			}
		return null;
	}
	public String getHDCPKeyFromUSB(){
		Log.d("TvManager", "getHDCPKeyFromUSB");
		try{
			Log.d("TvManager", "getHDCPKeyFromUSB try");
			return mService.getHDCPKeyFromUSB();
		}catch(RemoteException ex){
			Log.d("TvManager", "getHDCPKeyFromUSB Exception");
			}
		return null;
	}
	// add by liufei burn mac/sn/hdcpkey 20130507 end
	public void setAspectRatio_equ(int rtkMode){
		Log.d("TvManager", "getHDCPKeyFromUSB");
		try {
			mService.setAspectRatio_equ(rtkMode);
		}
		catch(RemoteException ex) {
		}
	}
	public void setSurfaceTexture(SurfaceTexture surfaceTexture){
		setJniSurfaceTexture(surfaceTexture);
	}
	private native void setJniSurfaceTexture(SurfaceTexture surfaceTexture);

	/**
	 * CEC Control.
	 */
	public void initCec()
	{
		try{
			mService.initCec();
		}
		catch(RemoteException ex){
		}
	}

	public void uninitCec()
	{
		try{
			mService.uninitCec();
		}
		catch(RemoteException ex){
		}
	}

	public void initArc()
	{
		try{
			mService.initArc();
		}
		catch(RemoteException ex){
		}
	}

	public void handleRCPassThroughEvent(char logicalAddr, char event)
	{
		try{
			mService.handleRCPassThroughEvent(logicalAddr, event);
		}
		catch(RemoteException ex){
		}
	}

	public void cecSetDeviceMenuLanguage(long language)
	{
		try{
			mService.cecSetDeviceMenuLanguage(language);
		}
		catch(RemoteException ex){
		}
	}

	public void enableCEC(boolean bOn)
	{
		try{
			mService.enableCEC(bOn);
		}
		catch(RemoteException ex){
		}
	}

	public void enableARC(boolean bOn)
	{
		try{
			mService.enableARC(bOn);
		}
		catch(RemoteException ex){
		}
	}

	public void setCecDeviceStandby(char logicalAddr)
	{
		try{
			mService.setCecDeviceStandby(logicalAddr);
		}
		catch(RemoteException ex){
		}
	}

	public void setPowerOnFromCec()
	{
		try{
			mService.setPowerOnFromCec();
		}
		catch(RemoteException ex){
		}
	}

	public String getDeviceOsdName(char logicalAddr, int channel)
	{
		try{
			return mService.getDeviceOsdName(logicalAddr, channel);
		}
		catch(RemoteException ex){
		}

		return null;
	}

	public String getCecDeviceList(int channel)
	{
		try{
			return mService.getCecDeviceList(channel);
		}
		catch(RemoteException ex){
		}

		return null;
	}

	public char getActiveDeviceLogicalAddr()
	{
		try{
			return mService.getActiveDeviceLogicalAddr();
		}
		catch(RemoteException ex){
		}

		return 0;
	}

	public boolean activateDeviceByLogicalAddr(char logicalAddr)
	{
		try{
			return mService.activateDeviceByLogicalAddr(logicalAddr);
		}
		catch(RemoteException ex){
		}

		return false;
	}

	public void setSurfaceTexture(int dispChannel, SurfaceTexture surfaceTexture){
		setJniSurfaceTexture(dispChannel, surfaceTexture);

	}


	// add by keven_yuan begin
	public boolean setTimingOffEnable(boolean timingoffenable)
	{
		try{
			return mService.setTimingOffEnable(timingoffenable);
		}
		catch(RemoteException ex){
		}
		return false;
	}
	
	public boolean getTimingOffEnable()
	{
		try{
			return mService.getTimingOffEnable();
		}
		catch(RemoteException ex){
		}
		return false;
	}
	
	public boolean setOffTime(final Context context, int offhour, int offminute)
	{
		final Time time = new Time();
		final Handler handler=new Handler();
		final Intent intent = new Intent(
				"konka.action.TIMING_OFF_WARNING");
				
		if((offhour<0)||(offhour>23)||(offminute<0)||(offminute>59))
		{
			return false;
		}
		try{
			if(true != mService.setOffTime(offhour, offminute)){
				return false;
			}
		}
		catch(RemoteException ex){
		}
		if(flag == false){
			flag = true;
			Runnable runnable=new Runnable(){
				@Override
				public void run() {
					// TODO Auto-generated method stub
					time.setToNow();
					if((time.hour == getOffHour())&&(time.minute == getOffMinute())){
						context.sendBroadcast(intent);
						Slog.v("setOffTime","######### timer_off take effect,sendbroadcast!!#########");
						handler.removeCallbacks(this);
						flag = false;
						return;
					}
					handler.postDelayed(this, 60*1000);
				}
			};
			handler.postDelayed(runnable, 60*1000);
		}
		return true;
	}
	
	public int getOffHour()
	{
		try{
			return mService.getOffHour();
		}
		catch(RemoteException ex){
		}
		return 0;
	}
	
	public int getOffMinute()
	{
		try{
			return mService.getOffMinute();
		}
		catch(RemoteException ex){
		}
		return 0;
	}
	
	public boolean setTimingOnEnable(boolean timingonenable)
	{
		try{
			return mService.setTimingOnEnable(timingonenable);
		}
		catch(RemoteException ex){
		}
		return true;
	}

	public boolean getTimingOnEnable()
	{
		try{
			return mService.getTimingOnEnable();
		}
		catch(RemoteException ex){
		}
		return false;
	}
	
	public boolean setOnTime(int onhour, int onminute)
	{
		if((onhour<0)||(onhour>23)||(onminute<0)||(onminute>59))
		{
			return false;
		}

		try{
			return mService.setOnTime(onhour, onminute);
		}
		catch(RemoteException ex){
		}
		return true;
	}

	public int getOnHour()
	{
		try{
			return mService.getOnHour();
		}
		catch(RemoteException ex){
		}
		return 0;
	}
	
	public int getOnMinute()
	{
		try{
			return mService.getOnMinute();
		}
		catch(RemoteException ex){
		}
		return 0;
	}
	
	public boolean setInputSrc(int inputsrc)
	{
		try{
			return mService.setInputSrc(inputsrc);
		}
		catch(RemoteException ex){
		}
		return true;
	}

	public int getInputSrc()
	{
		try{
			return mService.getInputSrc();
		}
		catch(RemoteException ex){
		}
		return 0;
	}

	public boolean standBy()
	{
		try{
			return mService.standBy();
		}
		catch(RemoteException ex){
		}
		return true;
	}

	public void setUpgrade(boolean enble)
	{
		try{
			mService.setUpgrade(enble);
		}
		catch(RemoteException ex){
		}
	}
	// add by keven_yuan end

	private native void setJniSurfaceTexture(int dispChannel, SurfaceTexture surfaceTexture);

	/*
	@param dispChannel
					1: main
					2: sub
			@param surface

			@return
	*/
	public native void setSurface(int dispChannel, Surface surface);
	private int mNativeSurfaceTexture;	// accessed by native

	public boolean getPanelLockStatus(){
		boolean ret = false;
		try{
			ret = mService.getPanelLockStatus();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setPanelLockStatus(boolean status){
		try{
			mService.setPanelLockStatus(status);
		}
		catch(RemoteException ex){}
	}

	public void enableGetMediaTotalTime()
	{
		try{
			mService.enableGetMediaTotalTime();
		}
		catch(RemoteException ex){
		}
	}

	public void disableGetMediaTotalTime()
	{
		try{
			mService.disableGetMediaTotalTime();
		}
		catch(RemoteException ex){
		}
	}

	public long getMediaTotalTime(String url)
	{
		try{
			return mService.getMediaTotalTime(url);
		}
		catch(RemoteException ex){
		}

		return 0;
	}
///////////////////////////////////// add by hongzhi_yin ////////////////////////////////////////////
	public int GetPanelWidth()
	{
			try{
				return mService.GetPanelWidth();
			}
			catch(RemoteException ex){
			}

			return 0;
	}

	public int GetPanelHeight()
	{
			try{
				return mService.GetPanelHeight();
			}
			catch(RemoteException ex){
			}

			return 0;
	}

	public int getAudioSpdifOutput()
	{
		try{
			return mService.getAudioSpdifOutput();
		}
		catch(RemoteException ex){
		}

		return 1;
	}
	public int getHdmi_DVI()
	{
		try{
			return mService.getHdmi_DVI();
		}
		catch(RemoteException ex){
		}

		return 0;
	}
	public boolean getIsNoSignal(){
		boolean result=false;
		try{
			result = mService.getIsNoSignal();
		}
		catch(RemoteException ex){
		}
		return result;
	}
	public String getVideoPixelInfo(int x, int y){
		String  result = "";
		try {
			result = mService.getVideoPixelInfo (x, y);
			} catch (RemoteException ex) {
		}
		return result;
	}
	public void set3dAutoModeCheck(int imode){
		try{
			mService.set3dAutoModeCheck(imode);
		}catch(RemoteException ex){
		}
	}
	public int get3dAutoModeCheck(){
		try{
			return mService.get3dAutoModeCheck();
		}catch(RemoteException ex){
		}
		return 0;
	}
	public void set3dViewOffset(int imode){
		try{
			mService.set3dViewOffset(imode);
		}catch(RemoteException ex){
		}
	}
	public int get3dViewOffset(){
		try{
			return mService.get3dViewOffset();
		}catch(RemoteException ex){
		}
		return 0;
	}
	public boolean set3dTo2D(boolean bOn, int iFrameFlag) {
		boolean result = false;

		try {
            result = mService.set3dTo2D(bOn, iFrameFlag);
        } catch (RemoteException ex) {
        }
		return result;
    }

	//set 3d effect
	public void set3DEffectMode(int imode){
		try{
			mService.set3DEffectMode(imode);
		}catch(RemoteException ex){
		}
	}
	public int get3DEffectMode(){
		try{
			return mService.get3DEffectMode();
		}catch(RemoteException ex){
		}
		return 0;
	}

	//improve the picture quality of 3d or not.
	public void setPQImproveEnable(boolean onoff) {
		try{
			mService.setPQImproveEnable(onoff);
		}catch(RemoteException ex){

		}
	}
	public boolean getPQImproveEnable() {
		try{
			return mService.getPQImproveEnable();
		}
		catch(RemoteException ex){

		}
		return false;
	}

	//enable or disable smart definition control.(3d)
	public void setSmartDefinitionControlEnable(boolean onoff) {
		try{
			mService.setSmartDefinitionControlEnable(onoff);
		}catch(RemoteException ex){

		}
	}
	public boolean getSmartDefinitionControlEnable() {
		try{
			return mService.getSmartDefinitionControlEnable();
		}
		catch(RemoteException ex){

		}
		return false;
	}

	public void setDynamicBacklight(boolean onoff){
		try{
			mService.setDynamicBacklight(onoff);
		}catch(RemoteException ex){
		}
	}
	public boolean getDynamicBacklight(){
		try{
			return mService.getDynamicBacklight();
		}catch(RemoteException ex){
		}
		return false;
	}
	
    //enable or disable dynamic lumenance control.
	public void setDLCEnable(boolean onoff){
		try{
			mService.setDLCEnable(onoff);
		}catch(RemoteException ex){
		}
	}

	//get average luminance of DLC.
	public int getDLCAverageLuminance(){
		try{
			return mService.getDLCAverageLuminance();
		}catch(RemoteException ex){
		}
		return 0;
	}

	public void setVideoArea(int iX, int iY, int iWidth, int iHeight) {
		try {
			mService.setVideoArea(iX, iY, iWidth, iHeight);
		} catch (RemoteException ex) {
		}
	}

	public void setAdbMode(boolean onoff) {
		try{
			mService.setAdbMode(onoff);
		}catch(RemoteException ex){

		}
	}

	public boolean getAdbMode() {
		try{
			return mService.getAdbMode();
		}
		catch(RemoteException ex){

		}
		return false;
	}

	public void setAudioCurve(int mode,int value) {
		try{
			mService.setAudioCurve(mode,value);
		}catch(RemoteException ex){

		}
	}

	public int getAudioCurve(int mode) {
		try{
			return mService.getAudioCurve(mode);
		}
		catch(RemoteException ex){

		}
		return 0;
	}

	public void setUpgradeEnable(int enable,boolean apply) {
		try{
			mService.setUpgradeEnable(enable,apply);
		}catch(RemoteException ex){

		}
	}
	
	public void setBassBack(int mode,int value) {
		try{
			mService.setBassBack(mode,value);
		}catch(RemoteException ex){
		
		}
	}
	
	public int getBassBack(int mode) {
		try{
			return mService.getBassBack(mode);
		}catch(RemoteException ex){
		
		}
		return 0;
	}
	
	public void setAudioBoost(int mode,long value) {
		try{
			mService.setAudioBoost(mode,value);
		}catch(RemoteException ex){
		
		}
	}
	
	public long getAudioBoost(int mode) {
		try{
			return mService.getAudioBoost(mode);
		}catch(RemoteException ex){
		
		}
		return 0;
	}

	public void setScanFrequency(int startFreq, int endFreq) {
		try {
			mService.setScanFrequency(startFreq, endFreq);
		} catch (RemoteException ex) {
		}
	}

	public void setForceBgColor(int iRGB) {
		try {
			mService.setForceBgColor(iRGB);
		} catch (RemoteException ex) {
		}
	}

	public String get_HDCP_Reciever_ID(){
		String  result = "";
		try {
			result = mService.get_HDCP_Reciever_ID();
			} catch (RemoteException ex) {
		}
		return result;
	}
	public String get_HDCP_SerialNum(){
		String  result = "";
		try {
			result = mService.get_HDCP_SerialNum();
			} catch (RemoteException ex) {
		}
		return result;
	}
	public String get_Widevine_Device_ID(){
		String  result = "";
		try {
			result = mService.get_Widevine_Device_ID();
			} catch (RemoteException ex) {
		}
		return result;
	}
	public int get_wakeup_source(){
		int  result = 0;
		try {
			result = mService.get_wakeup_source();
			} catch (RemoteException ex) {
		}
		return result;
	}
	
	public boolean getSecureBoot() {
		boolean ret = false;
		try{
			ret = mService.getSecureBoot();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	
	public void white_window_on(){
		try {
			mService.white_window_on();
		} catch (RemoteException ex) {
		}
	}
	public void white_window_off(){
		try {
			mService.white_window_off();
		} catch (RemoteException ex) {
		}
	}
	public void write_RDRV_to_factory(int RDRV){
		try {
			mService.write_RDRV_to_factory(RDRV);
		} catch (RemoteException ex) {
		}
	}
	public void write_BDRV_to_factory(int BDRV){
		try {
			mService.write_BDRV_to_factory(BDRV);
		} catch (RemoteException ex) {
		}
	}
	
	public boolean isAutoColorRunning() {
		boolean ret = false;
		try{
			ret = mService.isAutoColorRunning();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int getAutoColor_info() {
		int ret = 0;
		try{
			ret = mService.getAutoColor_info();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	
	public int get_R_Drive_RDRV() {
		int ret = 0;
		try{
			ret = mService.get_R_Drive_RDRV();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int get_G_Drive_GDRV() {
		int ret = 0;
		try{
			ret = mService.get_G_Drive_GDRV();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int get_B_Drive_BDRV() {
		int ret = 0;
		try{
			ret = mService.get_B_Drive_BDRV();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void set_R_Drive_RDRV(int RValue) {
		try {
			mService.set_R_Drive_RDRV(RValue);
		} catch (RemoteException ex) {
		}
	}

	public void set_G_Drive_GDRV(int GValue) {
		try {
			mService.set_G_Drive_GDRV(GValue);
		} catch (RemoteException ex) {
		}
	}

	public void set_B_Drive_BDRV(int BValue) {
		try {
			mService.set_B_Drive_BDRV(BValue);
		} catch (RemoteException ex) {
		}
	}

	public int ctrlGet_R_CUTOFF_RCUT() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_R_CUTOFF_RCUT();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_G_CUTOFF_GCUT() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_G_CUTOFF_GCUT();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_B_CUTOFF_BCUT() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_B_CUTOFF_BCUT();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void ctrlSet_R_CUTOFF_RCUT(int RValue){
		try {
			mService.ctrlSet_R_CUTOFF_RCUT(RValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_G_CUTOFF_GCUT(int GValue){
		try {
			mService.ctrlSet_G_CUTOFF_GCUT(GValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_B_CUTOFF_BCUT(int BValue){
		try {
			mService.ctrlSet_B_CUTOFF_BCUT(BValue);
		} catch (RemoteException ex) {
		}
	}

	public int ctrlGet_R_Drive_Offset_Natural_RDON() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_R_Drive_Offset_Natural_RDON();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_G_Drive_Offset_Natural_GDON() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_G_Drive_Offset_Natural_GDON();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_B_Drive_Offset_Natural_BDON() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_B_Drive_Offset_Natural_BDON();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void ctrlSet_R_Drive_Offset_Natural_RDON(int RValue){
		try {
			mService.ctrlSet_R_Drive_Offset_Natural_RDON(RValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_G_Drive_Offset_Natural_GDON(int GValue){
		try {
			mService.ctrlSet_G_Drive_Offset_Natural_GDON(GValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_B_Drive_Offset_Natural_BDON(int BValue){
		try {
			mService.ctrlSet_B_Drive_Offset_Natural_BDON(BValue);
		} catch (RemoteException ex) {
		}
	}

	public int ctrlGet_R_CUTOFF_Offset_Natural_RCON() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_R_CUTOFF_Offset_Natural_RCON();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_G_CUTOFF_Offset_Natural_GCON() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_G_CUTOFF_Offset_Natural_GCON();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_B_CUTOFF_Offset_Natural_BCON() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_B_CUTOFF_Offset_Natural_BCON();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void ctrlSet_R_CUTOFF_Offset_Natural_RCON(int RValue){
		try {
			mService.ctrlSet_R_CUTOFF_Offset_Natural_RCON(RValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_G_CUTOFF_Offset_Natural_GCON(int GValue){
		try {
			mService.ctrlSet_G_CUTOFF_Offset_Natural_GCON(GValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_B_CUTOFF_Offset_Natural_BCON(int BValue){
		try {
			mService.ctrlSet_B_CUTOFF_Offset_Natural_BCON(BValue);
		} catch (RemoteException ex) {
		}
	}

	public int ctrlGet_R_Drive_Offset_Warm_RDOW() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_R_Drive_Offset_Warm_RDOW();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_G_Drive_Offset_Warm_GDOW() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_G_Drive_Offset_Warm_GDOW();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_B_Drive_Offset_Warm_BDOW() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_B_Drive_Offset_Warm_BDOW();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void ctrlSet_R_Drive_Offset_Warm_RDOW(int RValue){
		try {
			mService.ctrlSet_R_Drive_Offset_Warm_RDOW(RValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_G_Drive_Offset_Warm_GDOW(int GValue){
		try {
			mService.ctrlSet_G_Drive_Offset_Warm_GDOW(GValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_B_Drive_Offset_Warm_BDOW(int BValue){
		try {
			mService.ctrlSet_B_Drive_Offset_Warm_BDOW(BValue);
		} catch (RemoteException ex) {
		}
	}

	public int ctrlGet_R_CUTOFF_Offset_Warm_RCOW() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_R_CUTOFF_Offset_Warm_RCOW();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_G_CUTOFF_Offset_Warm_GCOW() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_G_CUTOFF_Offset_Warm_GCOW();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int ctrlGet_B_CUTOFF_Offset_Warm_BCOW() {
		int ret = 0;
		try{
			ret = mService.ctrlGet_B_CUTOFF_Offset_Warm_BCOW();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void ctrlSet_R_CUTOFF_Offset_Warm_RCOW(int RValue){
		try {
			mService.ctrlSet_R_CUTOFF_Offset_Warm_RCOW(RValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_G_CUTOFF_Offset_Warm_GCOW(int GValue){
		try {
			mService.ctrlSet_G_CUTOFF_Offset_Warm_GCOW(GValue);
		} catch (RemoteException ex) {
		}
	}

	public void ctrlSet_B_CUTOFF_Offset_Warm_BCOW(int BValue){
		try {
			mService.ctrlSet_B_CUTOFF_Offset_Warm_BCOW(BValue);
		} catch (RemoteException ex) {
		}
	}

	public int getLightSensorValue() {
		int ret = 0;
		try{
			ret = mService.getLightSensorValue();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void setQamConst(int qamconst) {
		try {
			mService.setQamConst(qamconst);
		} catch (RemoteException ex) {
		}
	}

	public void setSymbolRateValue(int rate) {
		try {
			mService.setSymbolRateValue(rate);
		} catch (RemoteException ex) {
		}
	}

	public boolean getIsChannelScramble() {
		boolean ret = false;
		try{
			ret = mService.getIsChannelScramble();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public void TSBTJPChannel() {
		try{
			mService.TSBTJPChannel();
		}
		catch(RemoteException ex){
		}
	}

//TSB Service Mode
	public int getVerticalLine() {
		int ret = 0;
		try{
			ret = mService.getVerticalLine();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setVerticalLine(int Value) {
		try {
			mService.setVerticalLine(Value);
		} catch (RemoteException ex) {
		}
	}
	public int getVLock() {
		int ret = 0;
		try{
			ret = mService.getVLock();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setVLock(int Value) {
		try {
			mService.setVLock(Value);
		} catch (RemoteException ex) {
		}
	}
	public int getColorFlick() {
		int ret = 0;
		try{
			ret = mService.getColorFlick();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setColorFlick(int Value) {
		try {
			mService.setColorFlick(Value);
		} catch (RemoteException ex) {
		}
	}
	public int getWhitePeak() {
		int ret = 0;
		try{
			ret = mService.getWhitePeak();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setWhitePeak(int Value) {
		try {
			mService.setWhitePeak(Value);
		} catch (RemoteException ex) {
		}
	}


	public int getH_Jitter() {
		int ret = 0;
		try{
			ret = mService.getH_Jitter();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setH_Jitter(int Value) {
		try {
			mService.setH_Jitter(Value);
		} catch (RemoteException ex) {
		}
	}
	public int getVsync_Threshold() {
		int ret = 0;
		try{
			ret = mService.getVsync_Threshold();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setVsync_Threshold(int Value) {
		try {
			mService.setVsync_Threshold(Value);
		} catch (RemoteException ex) {
		}
	}
	public int getAgcSpeed() {
		int ret = 0;
		try{
			ret = mService.getAgcSpeed();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setAgcSpeed(int Value) {
		try {
			mService.setAgcSpeed(Value);
		} catch (RemoteException ex) {
		}
	}
	public int getSignalDet_HSync() {
		int ret = 0;
		try{
			ret = mService.getSignalDet_HSync();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setSignalDet_HSync(int Value) {
		try {
			mService.setSignalDet_HSync(Value);
		} catch (RemoteException ex) {
		}
	}



	public int getCH_WIDTH_VIDEO_SYS() {
		int ret = 0;
		try{
			ret = mService.getCH_WIDTH_VIDEO_SYS();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setCH_WIDTH_VIDEO_SYS(int Value) {
		try {
			mService.setCH_WIDTH_VIDEO_SYS(Value);
		} catch (RemoteException ex) {
		}
	}
	public int getAGC_DNSample() {
		int ret = 0;
		try{
			ret = mService.getAGC_DNSample();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}
	public void setAGC_DNSample(int Value) {
		try {
			mService.setAGC_DNSample(Value);
		} catch (RemoteException ex) {
		}
	}
	

	
//TSB Service Mode

	public int getVOSignalState() {
		int ret = 0;
		try{
			ret = mService.getVOSignalState();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

///////////////////////////////////// add by lynn_sheng ////////////////////////////////////////////
	public boolean aVCaptureInit() {
		boolean ret = false;
		try{
			ret = mService.aVCaptureInit();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public boolean aVCapturePicStart(int mode, int w, int h) {
		boolean ret = false;
		try{
			ret = mService.aVCapturePicStart(mode, w, h);
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public boolean aVCapturePicStop() {
		boolean ret = false;
		try{
			ret = mService.aVCapturePicStop();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public boolean aVCapturePic(int picType, int x, int y, int w, int h, String path) {
		boolean ret = false;
		try{
			ret = mService.aVCapturePic(picType, x, y, w, h, path);
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int aVCapturePicGetEvent() {
		int result = 0;

		try {
			result = mService.aVCapturePicGetEvent();
		} catch (RemoteException ex) {
		}

		return result;
	}

	public boolean aVCaptureExit() {
		boolean ret = false;
		try{
			ret = mService.aVCaptureExit();
		}
		catch(RemoteException ex){}
		finally{
			return ret;
		}
	}

	public int getDtvChPlayFilterType() {
		try {
			return mService.getDtvChPlayFilterType();
		} catch (RemoteException ex) {
		}
		return 0;
	}

	public void setDtvChPlayFilterType(int type) {
		try {
			mService.setDtvChPlayFilterType(type);
		} catch (RemoteException ex) {
		}
	}

	public int getDtvChinaRegion() {
		try {
			return mService.getDtvChinaRegion();
		} catch (RemoteException ex) {
		}
		return 0;
	}

	public void setDtvChinaRegion(int region) {
		try {
			mService.setDtvChinaRegion(region);
		} catch (RemoteException ex) {
		}
	}

	public void setCountryCode(int  countryCode) {
		try {
			 mService.setCountryCode( countryCode );
		} catch (RemoteException ex) {
			return;
		}
	}


	public int getCountryCode() {
		try {
			return mService.getCountryCode();
		} catch (RemoteException ex) {
			return 0;
		}
	}

	
	public void SetTvMountSetting(int  mode) {
		try {
			 mService.SetTvMountSetting( mode );
		} catch (RemoteException ex) {
			return;
		}
	}

	public void setLEDControl(int R_G, int values) {
		try {
			mService.setLEDControl(R_G, values);
		} catch (RemoteException ex) {    }
	}

	public void setOnOffBackLight(boolean enable) {
        try{
	            mService.setOnOffBackLight(enable);
	    } catch (RemoteException ex){
		}
    }

	public int GetTvMountSetting() {
		try {
			return mService.GetTvMountSetting();
		} catch (RemoteException ex) {
			return 0;
		}
	}

	public boolean serviceControl(boolean ctrl, String servicename) {
        	boolean result = false;

        	try {
            	result = mService.serviceControl(ctrl,servicename);
	        } catch (RemoteException ex) {
        	}
	        return result;
    	}
	
	public boolean stopVideoPlay()
	{
		try{
			return mService.stopVideoPlay();
		}
		catch(RemoteException ex){
		}
		return false;
	}
	
	public void start_Widi_Service()
	{
			Log.i("TvManager Log", "execute_Rtk_Widi");
			SystemProperties.set("ctl.start", "sample_widi");
	}
	
    public void setRtkConsole(String value) {
        try {
            mService.setRtkConsole(value);
        } catch (RemoteException ex) {
        }
    }

	public void execRootCmd(String cmd) {
		try {
            mService.execRootCmd(cmd);
        } catch (RemoteException ex) {
        }
	}
	
	public String getMacAddrByIP(String ipAddr)
	{
		try{
			return mService.getMacAddrByIP(ipAddr);
		}
		catch(RemoteException ex){
		}

		return "00:00:00:00:00:00";
	}

	public int Mheg5_isStarted()
	{
	    Log.e("TvManager Log", "Mheg5_isStarted");
	    try{
			return mService.Mheg5_isStarted();
		}
		catch(RemoteException ex){
		}
		return -1;
	}

	public int Mheg5_Init()
	{
	    Log.e("TvManager Log", "Mheg5_Init");
	    try{
			return mService.Mheg5_Init();
		}
		catch(RemoteException ex){
		}
		return -1;
	}

	public int Mheg5_keyPress(int keyenum)
	{
	    int key = keyenum;
	    try{
			return mService.Mheg5_keyPress(key);
		}
		catch(RemoteException ex){
		}
		return -1;
	}
	
	public void Mheg5_isStop()
	{
	    try{
			mService.Mheg5_isStop();
		}
		catch(RemoteException ex){
		}
	}

}
