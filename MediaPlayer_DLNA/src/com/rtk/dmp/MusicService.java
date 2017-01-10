package com.rtk.dmp;


import java.util.ArrayList;

import com.realtek.Utils.DLNAFileInfo;
import com.realtek.Utils.FileUtils;
import com.realtek.Utils.MusicMsg;
import com.realtek.Utils.observer.Observable;
import com.realtek.Utils.observer.Observer;
import com.realtek.Utils.observer.ObserverContent;
import com.rtk.dmp.MusicActivity.ForwardStatus;
import com.rtk.dmp.MusicActivity.RepeatStatus;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TvManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.app.NotificationManager;
import com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5;
import com.realtek.DataProvider.DLNADataProvider;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener,Observer{
	private MediaPlayer mp =null;
	private MediaApplication map = null;
	private boolean isBackground = false;
	private boolean quitPlay = false;
	private ForwardStatus forwardStatus = ForwardStatus.NONE;
	private int[] forwardSpeed = { 2, 8, 16, 32, 16, 8 };
	private int forwardIndex = 0;
	private int forwardIconIndex = -1;
	private int repeatIndex =0;
	private RepeatStatus repeatStatus = RepeatStatus.OFF;
	private Object obj = null;
	private boolean canForward = false;
	private int hacktick = 0;
	private int status = -1;
	private int sta_play = 0;
	private int sta_pause = 1;
	private int sta_stop = 3;
	private int index;
	private int prePosition = 0;
	private int m_duration = 0;
	private boolean seeking = false;// since seek action is async and not accurat
	private ArrayList<DLNAFileInfo> musicList;
	public static final String PLAY_ACTION = "com.realtek.rtkmusicplayer.PLAY_ACTION";
	public static final String PAUSE_ACTION = "com.realtek.rtkmusicplayer.PAUSE_ACTION";
	public static final String PAUSEONE_ACTION = "com.realtek.rtkmusicplayer.PAUSEONE_ACTION";
	public static final String NEXT_ACTION = "com.realtek.rtkmusicplayer.NEXT_ACTION";
	public static final String PREVIOUS_ACTION = "com.realtek.rtkmusicplayer.PREVIOUS_ACTION";
	public static final String STOP_ACTION = "com.realtek.rtkmusicplayer.STOP_ACTION";
	public static final String SET_REPEAT = "com.realtek.rtkmusicplayer.SET_REPEAT";
	public static final String SET_FORWARD = "com.realtek.rtkmusicplayer.SET_FORWARD";
	public static final String STOP_FORWARD = "com.realtek.rtkmusicplayer.STOP_FORWARD";
	public static final String GO_START = "com.realtek.rtkmusicplayer.GO_START";
	private static final String PLAY_ONLY_ACTION = "com.realtek.rtkmusicplayer.PLAY_ONLY_ACTION";
	private static final String CANCEL_FORWARD = "com.realtek.rtkmusicplayer.CANCEL_FORWARD";
	private static final String INIT_ACTION = "com.realtek.rtkmusicplayer.INIT_ACTION";
	private static final String SET_FORWARD_INDEX = "com.realtek.rtkmusicplayer.SET_FORWARD_INDEX";
	private static final String SET_SEEK_OUT = "com.realtek.rtkmusicplayer.SET_SEEK_OUT";
	private static final String SET_BK_PLAY = "com.realtek.rtkmusicplayer.SET_BK_PLAY";
	private static final String SEEK_ACTION = "com.realtek.rtkmusicplayer.SEEK_ACTION";
	private static final String UPDATE_ACTION = "com.realtek.rtkmusicplayer.UPDATE_ACTION";
	
	
	private static final String CMD_MSG_GO_BACK = "CMD_MSG_GO_BACK";	
	private final String CMD_MSG_TOTAL_TIME = "CMD_MSG_TOTAL_TIME";
	private final String CMD_MSG_PROCESS_TIME = "CMD_MSG_PROCESS_TIME";
	private final String CMD_MSG_RESET = "CMD_MSG_RESET";
	private final String CMD_MSG_REFRESH = "CMD_MSG_REFRESH";
	private final String CMD_MSG_RESET_FWD = "CMD_MSG_RESET_FWD";
	private final String CMD_MSG_PLAY = "CMD_MSG_PLAY";
	private final String CMD_MSG_PAUSE = "CMD_MSG_PAUSE";
	private final String CMD_MSG_LAST = "CMD_MSG_LAST";
	private final String CMD_MSG_CLOSE = "CMD_MSG_CLOSE";
	private final String CMD_MSG_SHOWMESSAGEHINT_PROTOCAL = "CMD_MSG_SHOWMESSAGEHINT_PROTOCAL";
	
	private Handler handler = null;
	private final int MSG_CURRENT_TIME = 1;
	private final int MSG_SEEK_OUT = 2;
	private final int MSG_HACK_TIME = 3;
	private final int MSG_HACK_TIKE = 4;
	private final int MSG_SERVER_DIE = 5;
	private int playStaSrc = R.drawable.dnla_music_bar_icon_play;
	private AudioManager am;
	private MyBroadcastReciver myBroadcastReciver;
	private boolean isReset = false;
	private String servername = "";
	private boolean isSupportPause = true;
	private TvManager mTv = null;
	private SharedPreferences mPerferences = null;
	@Override
	public void onCreate() {
		super.onCreate();
		map = (MediaApplication) getApplication();
		mp = map.getMediaPlayer();
		map.addObserver(this);
		mp.setOnCompletionListener(this);
		mp.setOnErrorListener(mp_error);
		obj = new Object();	
		index = -1;
		mPerferences = PreferenceManager.getDefaultSharedPreferences(this);
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 

		if (mTv == null)
			mTv = (TvManager) getSystemService("tv");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.rtk.dmp.play.broadcast");
        intentFilter.addAction("com.rtk.dmp.next.broadcast");
        intentFilter.addAction("com.rtk.dmp.last.broadcast");
        intentFilter.addAction("com.rtk.dmp.rw.broadcast");
        intentFilter.addAction("com.rtk.dmp.fw.broadcast");
        intentFilter.addAction("com.rtk.dmp.clear.broadcast");
        intentFilter.addAction("com.rtk.dmp.updatelist.broadcast");
        intentFilter.addAction("com.rtk.dmp.seek.broadcast");
        intentFilter.addAction("com.rtk.dmp.repeat.broadcast");
        myBroadcastReciver = new MyBroadcastReciver();
        this.registerReceiver(myBroadcastReciver, intentFilter);
		initHandler();
	}
	private int currentTime;
	private void initHandler(){
		final Intent intent = new Intent();
		if (handler==null){
			handler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					switch (msg.what)
					{
					case MSG_CURRENT_TIME:
					{
						intent.setAction(CMD_MSG_PROCESS_TIME);
						if(status == sta_play){
							int timePosition = mp.getCurrentPosition();
							if (hacktick > 0
									&& (timePosition <= hacktick
											|| timePosition == prePosition || timePosition
											- hacktick > 1000)) {
								timePosition = hacktick;
							} else {
								hacktick = 0;
								handler.removeMessages(MSG_SEEK_OUT);
							}
							if (hacktick == 0) {
								if (seeking && timePosition > 1000)
									break;
								else {
									seeking = false;
									handler.removeMessages(MSG_SEEK_OUT);
								}
							}
							
							intent.putExtra("currentTime", timePosition);
							intent.putExtra("duration", m_duration>0?m_duration:1);
							intent.putExtra("status", status);
							intent.putExtra("forwardDrawableIndex", forwardIconIndex);
							currentTime = timePosition;
							//updateNotification();
							sendBroadcast(intent);
							progressNotification();
							}
						handler.sendEmptyMessageDelayed(MSG_CURRENT_TIME, 1000);
					}
						break;
					case MSG_SEEK_OUT :
					{
						hacktick = 0;
						seeking = false;
					}
						break;
					case MSG_HACK_TIME :
					{
						if(hacktick >= m_duration)
							hacktick = (int)m_duration;
						intent.setAction(CMD_MSG_PROCESS_TIME);
						intent.putExtra("currentTime", hacktick);
						intent.putExtra("forwardDrawableIndex", forwardIconIndex);
						sendBroadcast(intent);
						progressNotification();
					}
						break;
					case MSG_HACK_TIKE :
					{
						tick();
						handler.sendEmptyMessageDelayed(MSG_HACK_TIKE, 1000);
					}
						break;
					case MSG_SERVER_DIE :
					{
						stopPlay();
					}
						break;
					default :
						break;
					}
				}
			};
		}
	}

	private void doRequestAF(){
        int result = am.requestAudioFocus(audioFocusChangeListener,  
                AudioManager.STREAM_MUSIC, // Request permanent focus.  
                AudioManager.AUDIOFOCUS_GAIN);  
        		if(result == AudioManager.AUDIOFOCUS_REQUEST_FAILED){
        			Log.e("MusciService", "AudioManager.AUDIOFOCUS_REQUEST_FAILED");
        		}else
        		{
        			lostFocus = true;
        		}
	}
	
	private void doAbandonAF(){
		am.abandonAudioFocus(audioFocusChangeListener);
	}
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent == null)
			return 0;
			
		String action = intent.getAction();

		if (action.equals(PLAY_ACTION)) {
			Intent intent2 = new Intent();
			intent2.setAction(CMD_MSG_RESET);
			intent2.putExtra("index", index);
			sendBroadcast(intent2);
			resetNotification();
			play();
		} else if(action.equals(INIT_ACTION)){
			servername = intent.getStringExtra("serverName");
			synchronized(MusicService.this){
				index  = intent.getIntExtra("index", 0);
			}
			musicList = map.getPlayList();
			String filePath = musicList.get(index).getFilePath();
			if(!playPath.equals(filePath)){
				isReset = true;
			}
			quitPlay = false;
			status = -1;
		}else if (action.equals(PAUSE_ACTION)) {
			pause();
		} else if (action.equals(PLAY_ONLY_ACTION)) {
			playOnly();
		} else if (action.equals(PAUSEONE_ACTION)) {
			pauseOne();
		} else if (action.equals(NEXT_ACTION)) {
			isReset = true;
			next();
		} else if (action.equals(PREVIOUS_ACTION)) {
			seeking = intent.getBooleanExtra("seeking", false);
			isReset = true;
			previous();
		} else if (action.equals(STOP_ACTION)) {
			stop();
		} else if (action.equals(SET_REPEAT)) {
			String repeat = intent.getStringExtra("repeat");
			repeatStatus = RepeatStatus.valueOf(repeat);
			switch(repeatStatus){
			case OFF:
				repeatIndex =0;
				break;
			case ALL:
				repeatIndex = 1;
				break;
			case ONE:
				repeatIndex =2;
				break;
			}
		} else if (action.equals(SET_FORWARD)) {
			if (status == sta_play) {
				try {
					mp.pause();
					status = sta_pause;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			handler.removeMessages(MSG_SEEK_OUT);
			if (hacktick <= 0) {
				hacktick = mp.getCurrentPosition();
				prePosition = hacktick;
			}

			synchronized (obj) {
				canForward = true;
			}
			handler.removeMessages(MSG_HACK_TIKE);
			handler.sendEmptyMessageDelayed(MSG_HACK_TIKE, 0);
		} else if (action.equals(STOP_FORWARD)) {
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
			forwardIconIndex = -1;
			if (status == sta_pause) {
				try {
					mp.seekTo(hacktick);
					doRequestAF();
					mp.start();
					// hacktick = 0;
					status = sta_play;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			handler.removeMessages(MSG_HACK_TIKE);
		} else if (action.equals(GO_START)) {
			goStart();
		} else if (action.equals(CANCEL_FORWARD)) {
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
			forwardIconIndex = -1;
			if (status == sta_pause) {
				try {
					handler.removeMessages(MSG_SEEK_OUT);
					doRequestAF();
					mp.start();
					hacktick = 0;
					status = sta_play;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			handler.removeMessages(MSG_HACK_TIKE);
		}else if(action.equals(SET_FORWARD_INDEX)){
			forwardIndex = intent.getIntExtra("forwardIndex", 0);
			forwardStatus = ForwardStatus.valueOf(intent.getStringExtra("forwardStatus"));
		}
		else if(action.equals(SET_SEEK_OUT)){
			seeking = intent.getBooleanExtra("seeking", false);
			handler.removeMessages(MSG_SEEK_OUT);
			Message msg = handler.obtainMessage(MSG_SEEK_OUT);
			handler.sendMessageDelayed(msg, 3000);
		}else if(action.equals(SET_BK_PLAY)){
			bkplay = intent.getBooleanExtra("bkplay", false);
			if (!quitPlay) {
				if (!bkplay){
					clearNofification();
					quitPlay = false;
				}
				handler.removeMessages(1);
				handler.sendEmptyMessage(1);
			}
			updateNotification();
		}else if(action.equals(SEEK_ACTION)){
			seekAction(intent);
		}else if(action.equals(UPDATE_ACTION)){
			if(lostFocus){
				playOnly();
			}
			handler.removeMessages(1);
			handler.sendEmptyMessage(1);
			Intent intent2 = new Intent();
			intent2.setAction(CMD_MSG_REFRESH);
			intent2.putExtra("index", index);
			sendBroadcast(intent2);
			update();
		}

		return super.onStartCommand(intent, flags, startId);
	}
	private void update(){
		Intent intent = new Intent();
		intent.setAction(CMD_MSG_PROCESS_TIME);
		if(status == sta_play){
			int timePosition = mp.getCurrentPosition();
			if (hacktick > 0
					&& (timePosition <= hacktick
							|| timePosition == prePosition || timePosition
							- hacktick > 1000)) {
				timePosition = hacktick;
			} else {
				hacktick = 0;
				handler.removeMessages(MSG_SEEK_OUT);
			}
			if (hacktick == 0) {
				if (seeking && timePosition > 1000)
					return;
				else {
					seeking = false;
					handler.removeMessages(MSG_SEEK_OUT);
				}
			}
			
			intent.putExtra("currentTime", timePosition);

			}
		intent.putExtra("status", status);
		intent.putExtra("duration", m_duration);
		intent.putExtra("forwardDrawableIndex", forwardIconIndex);
		sendBroadcast(intent);
		progressNotification();
	}
	private boolean bkplay = false;
	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("Service destroy!");
		if (mp!=null){
			mp.stop();
			mp = null;
		}
		doAbandonAF();
		unregisterReceiver(myBroadcastReciver);
		map.deleteObserver(this);
	}
	

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		if(quitPlay)
			return;
		if (musicList == null || musicList.size() == 0) {
			goBack();
			return;
		}
		synchronized(MusicService.this){
		if (repeatStatus != RepeatStatus.ONE) {
			index++;
		}
		if (index == musicList.size()) {
			if (repeatStatus == RepeatStatus.OFF) {
				index--;
				goBack();
				return;
			} else {
				index = 0;
			}
		}
	}
		Intent intent2 = new Intent();
		intent2.setAction(CMD_MSG_RESET);
		intent2.putExtra("index", index);
		sendBroadcast(intent2);
		resetNotification();
		status = sta_stop;
		isReset = true;
		play();
	}
	

	private void goBack()
	{
		doReset();
		Intent intent = new Intent();
		intent.setAction(CMD_MSG_GO_BACK);
		intent.putExtra("index", index);
		sendBroadcast(intent);
		doAbandonAF();
	}
	
	private void stopPlay()
	{
		bkplay = false;
		handler.removeMessages(1);
		clearNofification();
		doReset();
		Intent intent = new Intent();
		intent.setAction(CMD_MSG_GO_BACK);
		intent.putExtra("index", -1);
		sendBroadcast(intent);
		doAbandonAF();
	}
	
	private void doReset(){
		try{
			mp.reset();
		}catch(IllegalStateException e){
			map.releaseMediaPlayer();
			mp = map.getMediaPlayer();
			mp.setOnCompletionListener(this);
			mp.setOnErrorListener(mp_error);
		}
	}
	// play the music
	public void play() {
		if(musicList==null || musicList.size()<=0){
			return;
		}
		startInit();
	}
	private String title;
	private String artist;
	private void startInit(){
		init();	
	title = FileUtils.removeExtension(musicList.get(
			index).getFileName());
	artist = musicList.get(
			index).getPerformer();
	if(artist == null || artist.length()<1 || artist.equalsIgnoreCase("unknown"))
		artist = "";
		handler.removeMessages(1);
	}
	String playPath = "";
	private synchronized void init() {
		synchronized (obj) {
			canForward = false;
		}
		doReset();
		try {
				if (musicList.get(index).getCanPlay() == -1) {
					//handler.sendEmptyMessage(MSG_PLAY_NEXT);
					return;
				}
				playPath = musicList.get(index).getFilePath();
				//playPath = "/mnt/udisk/sda1/bug music/1.ogg";
				boolean bDTCP = false;
				String tmpProtolinfo = "";
				//Log.v(tag, playPath);
				
				String tmpContentLength = null;
    			tmpContentLength = DLNADataProvider.queryDataByID(musicList.get(index).getUniqueCharID(), DLNADataProvider.UPNP_DMP_RES_SIZE);
    			if(tmpContentLength != null) {
    				playPath = playPath + " contentlength=" + tmpContentLength;
    			}
    			String tmpDuration = null;
    			tmpDuration = DLNADataProvider.queryDataByID(musicList.get(index).getUniqueCharID(), DLNADataProvider.UPNP_DMP_RES_DURATION);
    			if(tmpDuration != null) {
    				playPath = playPath + " duration=" + tmpDuration;
    			}
				tmpProtolinfo = DLNADataProvider.queryDataByID(musicList.get(index).getUniqueCharID(), DLNADataProvider.UPNP_DMP_RES_PROTOCOLINFO);
				// Filter those not able play
				boolean ablePlay = false;
				ablePlay = checkProtolInfo(tmpProtolinfo);
				if(!ablePlay) {
					Intent it = new Intent();
	    			it.setAction(CMD_MSG_SHOWMESSAGEHINT_PROTOCAL);
	    			sendBroadcast(it);
					return ;
				}
				if(tmpProtolinfo != null){
					/*if(tmpProtolinfo.contains("DLNA.ORG_FLAGS")){	
						String flags = null;
						int len = "DLNA.ORG_FLAGS=".length();
						flags = tmpProtolinfo.substring(tmpProtolinfo.indexOf("DLNA.ORG_FLAGS"));
						flags = flags.substring(len,len + 4);
						String binaryFlags = hexString2binaryString(flags);
						char bit21 = binaryFlags.charAt(10);
						if(bit21 == '1'){
							isSupportPause = true;
						}else{
							isSupportPause = false;
						}
					}*/
						
					if(tmpProtolinfo.contains("DLNA.ORG_OP")){	
						String operation = null;
						int len = "DLNA.ORG_OP=".length();
						operation = tmpProtolinfo.substring(tmpProtolinfo.indexOf("DLNA.ORG_OP"));
						operation = operation.substring(len);
						if(operation.charAt(0) == '1' || operation.charAt(1) == '1'){
							isSupportPause = true;
						}else {
							isSupportPause = false;
						}
					}else {
						isSupportPause = false;
					}
					if(tmpProtolinfo.contains("DLNA.ORG_FLAGS")){	
						String flags = null;
						int len = "DLNA.ORG_FLAGS=".length();
						flags = tmpProtolinfo.substring(tmpProtolinfo.indexOf("DLNA.ORG_FLAGS"));
						flags = flags.substring(len,len + 6);
						String binaryFlags = hexString2binaryString(flags);
						char bit16 = binaryFlags.charAt(15);
						char bit15 = binaryFlags.charAt(16);
						char bit14 = binaryFlags.charAt(17);
						char bit23 = binaryFlags.charAt(8);
						if (bit16 == '1') {
		                     // bit 15: cleartextbyteseek-full flag
		                     if ((bit15 == '1') || (bit14 == '1' && bit23 == '0'))
		                         isSupportPause = true;
		                }
					}
					bDTCP = tmpProtolinfo.contains("DTCP1HOST");
					if(!bDTCP){
						if(playPath != null && playPath.contains("?")){
							String tmpPath = playPath.substring(playPath.indexOf("?"));
							if(tmpPath.contains("CONTENTPROTECTIONTYPE=DTCP1")){
								bDTCP = true;
							}
						}
				 	}
					if(bDTCP){
						tmpProtolinfo = " protocolinfo=" + tmpProtolinfo.substring(0, tmpProtolinfo.lastIndexOf(":") + 1) + "*"     ;
				 	}else{
							tmpProtolinfo = "";
					}
				}
				mp.setPlayerType(6);//using rtkplayer
				mp.setDataSource(playPath + tmpProtolinfo);
				int fileType = DLNADataProvider.getFileType(musicList.get(index).getFileName());
				if(fileType == 14 || fileType == 36)
					mp.setParameter(1900, fileType);
				else
					mp.setParameter(1900, 0);
				mp.setOnPreparedListener(audioPreparedListener);
				mp.prepareAsync();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
			musicList.get(index).setCanPlay(-1);
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
			musicList.get(index).setCanPlay(-1);
		}
		 catch (Exception e) 
		{
			e.printStackTrace();
			musicList.get(index).setCanPlay(-1);
		}
	}
	
	private OnPreparedListener audioPreparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			doRequestAF();
			//m_duration = mp.getDuration();
			if(!isReset && currentTime >0 && currentTime <m_duration){
				mp.seekTo(currentTime);
				hacktick = currentTime;
			}
			isReset = false;
			int fileType = DLNADataProvider.getFileType(musicList.get(index).getFileName());
			if (fileType == 14) { // LPCM
				byte[] inputArray = DLNADataProvider.getMediaInfo(musicList.get(index).getFileName());
				mp.execSetGetNavProperty(17, inputArray); //NAVPROP_INPUT_SET_LPCM_INFO = 17
			}
			m_duration = mp.getDuration();

			boolean bDTCP = false;
			String tmpProtolinfo = "";
			tmpProtolinfo = DLNADataProvider.queryDataByID(musicList.get(index).getUniqueCharID(), DLNADataProvider.UPNP_DMP_RES_PROTOCOLINFO);
			Log.d("MusicService", "protocalinfo= " + tmpProtolinfo);
			if(tmpProtolinfo != null){
				bDTCP = tmpProtolinfo.contains("DTCP1HOST");

				/*if (bDTCP || (SystemProperties.getBoolean("rtk.source.sendkey", false) == true) )
					mTv.setProtectionKey("DTCP");
				else
					mTv.setProtectionKey("NULL");*/
			}

			mp.start();
			status = sta_play;
			Intent intent = new Intent();
			intent.setAction(CMD_MSG_TOTAL_TIME);
			intent.putExtra("totaltime",m_duration );
			intent.putExtra("currentTime",currentTime );
			sendBroadcast(intent);
			handler.sendEmptyMessageDelayed(1,0);
			updateNotification();
		}
	};
	
	public void pause() {
		if (status == sta_play) {
			if(isSupportPause){
				handler.removeMessages(MSG_CURRENT_TIME);
				mp.pause();
				status = sta_pause;
				Intent intent = new Intent();
				intent.setAction(CMD_MSG_PAUSE);
				sendBroadcast(intent);
				pauseNotification();
			}else{
				Toast.makeText(MusicService.this, "Server is not support pause", 1).show();
			}
		} else {
			//play();
			doRequestAF();
			mp.start();
			status = sta_play;
			Intent intent = new Intent();
			intent.setAction(CMD_MSG_PLAY);
			sendBroadcast(intent);
			playNotification();
			handler.sendEmptyMessageDelayed(1,0);
		}
	}

	public void pauseOne() {
		if (status == sta_play) {
			if(isSupportPause){
				handler.removeMessages(MSG_CURRENT_TIME);
				mp.pause();
				status = sta_pause;
				Intent intent = new Intent();
				intent.setAction(CMD_MSG_PAUSE);
				sendBroadcast(intent);
				pauseNotification();
			}else{
				Toast.makeText(MusicService.this, "Server is not support pause", 1).show();
			}
		}
	}

	public void playOnly() {
		//play();
		//rtk can not support more than one player
		if (status == sta_pause) {
			doRequestAF();
			mp.start();
			status = sta_play;
			Intent intent = new Intent();
			intent.setAction(CMD_MSG_PLAY);
			sendBroadcast(intent);
			playNotification();
			handler.sendEmptyMessageDelayed(1,0);
		}
	}

	public void stop() {
		if (mp != null) {
			doReset();
			mp = null;
		}
	}

	public void previous() {
		currentTime = 0;
		Intent intent = new Intent();
		intent.setAction(CMD_MSG_LAST);
		sendBroadcast(intent);
		lastNotification();
		int pretag = status;
		synchronized(MusicService.this){
		if (index == 0) {
			index = musicList.size() - 1;
		} else {
			index--;
		}
		}
		Intent intent2 = new Intent();
		intent2.setAction(CMD_MSG_RESET);
		intent2.putExtra("index", index);
		sendBroadcast(intent2);
		resetNotification();
		startInit();
		if (forwardStatus != ForwardStatus.NONE) {
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
			forwardIconIndex = -1;
				Intent intent3 = new Intent();
				intent3.setAction(CMD_MSG_RESET_FWD);
				sendBroadcast(intent3);
				
				Intent intent4 = new Intent();
				intent4.setAction(CMD_MSG_PLAY);
				sendBroadcast(intent4);
				playNotification();
		} else {
			if (pretag == sta_pause) {
				mp.pause();
				status = sta_pause;
			}
		}
	}

	//
	public void next() {
		currentTime =0;
		int pretag = status;
		synchronized(MusicService.this){
			if (index == musicList.size() - 1) {
				index = 0;
			} else {
				index++;
			}
		}
			Intent intent2 = new Intent();
			intent2.setAction(CMD_MSG_RESET);
			intent2.putExtra("index", index);
			sendBroadcast(intent2);
			resetNotification();
		startInit();
		if (forwardStatus != ForwardStatus.NONE) {
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
			forwardIconIndex = -1;
			Intent intent3 = new Intent();
			intent3.setAction(CMD_MSG_RESET_FWD);
			sendBroadcast(intent3);
			
			Intent intent4 = new Intent();
			intent4.setAction(CMD_MSG_PLAY);
			sendBroadcast(intent4);
			playNotification();
		} else {
			if (pretag == sta_pause) {
				mp.pause();
				status = sta_pause;
			}
		}
	}

	public void goStart() {
		if (status == sta_play) {
			mp.seekTo(0);
		}
	}

	
	private void tick() {
		if (canForward) {
			int sign = 1;
			if (forwardStatus == ForwardStatus.REWIND)
				sign = -1;
			int position = forwardSpeed[forwardIndex] * 1000 * sign;
			if (MediaApplication.DEBUG)
				Log.e("ggggg", "seek :" + position);
			addFakeSeek(position);
		}
	}
	public void addFakeSeek(int tick) {

		hacktick += tick;
		if (hacktick < 0) {
			hacktick = 0;
			synchronized (obj) {
				canForward = false;
			}
			try {
				mp.seekTo(hacktick);
				doRequestAF();
				mp.start();
				status = sta_play;
			} catch (Exception e) {
				e.printStackTrace();
			}
			forwardStatus = ForwardStatus.NONE;
			forwardIconIndex = -1;
			Intent intent3 = new Intent();
			intent3.setAction(CMD_MSG_RESET_FWD);
			sendBroadcast(intent3);
			
			Intent intent4 = new Intent();
			intent4.setAction(CMD_MSG_PLAY);
			intent4.putExtra("forwardDrawableIndex", forwardIconIndex);
			sendBroadcast(intent4);
			playNotification();
		}
		if (hacktick >= m_duration) {
			hacktick = 0;
			synchronized (obj) {
				canForward = false;
			}
			if (repeatStatus == RepeatStatus.ONE) {
				try {
					mp.seekTo(hacktick);
					doRequestAF();
					mp.start();
					status = sta_play;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				synchronized(MusicService.this){
				index++;
					int total = musicList.size();
				if (index == total) {
					if (repeatStatus == RepeatStatus.OFF) {
						index--;
						if (!isBackground) {
							handler.sendEmptyMessage(MSG_HACK_TIME);
						}
						goBack();
						return;
					}
					index = 0;
				}
				}
				Intent intent2 = new Intent();
				intent2.setAction(CMD_MSG_RESET);
				intent2.putExtra("index", index);
				sendBroadcast(intent2);
				resetNotification();
				hacktick = 0;
				play();
			}
			forwardStatus = ForwardStatus.NONE;
			forwardIconIndex = -1;
			Intent intent3 = new Intent();
			intent3.setAction(CMD_MSG_RESET_FWD);
			sendBroadcast(intent3);
			
			Intent intent4 = new Intent();
			intent4.setAction(CMD_MSG_PLAY);
			intent4.putExtra("forwardDrawableIndex", forwardIconIndex);
			sendBroadcast(intent4);
			playNotification();
		}
		if (!isBackground) {
			handler.sendEmptyMessage(MSG_HACK_TIME);
		}
		if (MediaApplication.DEBUG)
			Log.e("ggggg", "position = " + hacktick);
	}

	
	private OnErrorListener mp_error = new OnErrorListener() {
		public boolean onError(MediaPlayer mp, int what, int extra) {
			System.out.println("what XXXXXXXXXXXXXXXXX  " + what);
			musicList.get(index).setCanPlay(-1);
			isReset = true;
			if(!quitPlay)
				next();
			return false;
		}
	};
	
	
	
    private void updateNotification() {
    	if(!bkplay)
    		return;
		MusicMsg musicMsg = new MusicMsg();
		musicMsg.setAction("COM.RTK.MUSIC.UPDATE");
		musicMsg.setTrackname(title);
		musicMsg.setArtistalbum(artist);
		musicMsg.setTimeNow(Util.toTime(currentTime));
		musicMsg.setTimeTotal(Util.toTime(m_duration));
		musicMsg.setCurrentTime(currentTime);
		musicMsg.setDuration(m_duration);
		musicMsg.setRepeatStatus(repeatStatus.name());
		sentNotification(MusicMsg.valString(musicMsg));
    }
    
    public static final int PLAYBACKSERVICE_STATUS = 1;
	private int[] forwardDarwable = { R.drawable.dnla_music_bar_icon_ff2,
			R.drawable.dnla_music_bar_icon_ff8,
			R.drawable.dnla_music_bar_icon_ff16,
			R.drawable.dnla_music_bar_icon_ff32,
			R.drawable.dnla_music_bar_icon_ff16,
			R.drawable.dnla_music_bar_icon_ff8 };
	private int[] backDarwable = { R.drawable.dnla_music_bar_icon_rew2,
			R.drawable.dnla_music_bar_icon_rew8,
			R.drawable.dnla_music_bar_icon_rew16,
			R.drawable.dnla_music_bar_icon_rew32,
			R.drawable.dnla_music_bar_icon_rew16,
			R.drawable.dnla_music_bar_icon_rew8  };
    private class MyBroadcastReciver extends BroadcastReceiver { 
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if(action.equals("com.rtk.dmp.play.broadcast")) { 
				if (forwardStatus != ForwardStatus.NONE) {
					stopForward();
					forwardStatus = ForwardStatus.NONE;
					forwardIconIndex = -1;
    				playStaSrc =R.drawable.dnla_music_bar_icon_play;
				} else {

    			if (status == sta_play) {
    				mp.pause();
    				status = sta_pause;
    				Intent it = new Intent();
    				it.setAction(CMD_MSG_PAUSE);
    				sendBroadcast(it);
    				pauseNotification();
    				playStaSrc = R.drawable.dnla_music_bar_icon_stop;
    			} else {
    				play();
    				//rtk can not support more than one player
    				/*doRequestAF();
    				mp.start();
    				status = sta_play;
    				Intent it = new Intent();
    				it.setAction(CMD_MSG_PLAY);
    				sendBroadcast(it);*/
    				playStaSrc =  R.drawable.dnla_music_bar_icon_play;
    				
    			}}
    			updateNotification();
    		}
    		else if(action.equals("com.rtk.dmp.last.broadcast")) { 
				if (forwardStatus != ForwardStatus.NONE) {
					cancelForward();
				}
				previous();
    			updateNotification();
    		}
    		else if(action.equals("com.rtk.dmp.next.broadcast")) { 
				if (forwardStatus != ForwardStatus.NONE) {
					cancelForward();
				}
				next();
    			updateNotification();
    		}
    		else if(action.equals("com.rtk.dmp.fw.broadcast")) { 
    			if (forwardStatus != ForwardStatus.FORWARD) {
    				forwardIndex = 0;
    				forwardStatus = ForwardStatus.FORWARD;
    			} else {
    				forwardIndex++;
    				forwardIndex %= 6;
    			}
    			playStaSrc = forwardDarwable[forwardIndex];	
					Intent it  = new Intent();
					it.setAction(CMD_MSG_PROCESS_TIME);		
					it.putExtra("currentTime", currentTime);
					it.putExtra("forwardDrawableIndex", forwardIndex);
					forwardIconIndex = forwardIndex;
					sendBroadcast(it);
					progressNotification();
    			setForward();
    			updateNotification();
    		}
    		else if(action.equals("com.rtk.dmp.rw.broadcast")) { 
    			if (forwardStatus != ForwardStatus.REWIND) {
    				forwardIndex = 0;
    				forwardStatus = ForwardStatus.REWIND;
    			} else {
    				forwardIndex++;
    				forwardIndex %= 6;
    			}
    			playStaSrc = backDarwable[forwardIndex];
				Intent it  = new Intent();
				it.setAction(CMD_MSG_PROCESS_TIME);		
				it.putExtra("currentTime", currentTime);
				it.putExtra("forwardDrawableIndex", forwardIndex +6);
				forwardIconIndex = forwardIndex+6;
				sendBroadcast(it);
				progressNotification();
    			setForward();
    			updateNotification();
    		}else if(action.equals("com.rtk.dmp.clear.broadcast")) { 
    			bkplay = false;
    			doReset();
    			map.exitApp();
    			clearNofification();
    			Intent it = new Intent();
    			it.setAction(CMD_MSG_CLOSE);
    			sendBroadcast(it);
    		}else if(action.equals("com.rtk.dmp.updatelist.broadcast")) { 
    			updatePlaylist();
    		}else if(action.equals("com.rtk.dmp.seek.broadcast")) { 
    			seekAction(intent);
    		}
    		else if(action.equals("com.rtk.dmp.repeat.broadcast")) { ;
				repeatIndex++;
				repeatIndex %= 3;
				if(repeatIndex == 0)
					repeatStatus = RepeatStatus.OFF;
				else if(repeatIndex == 1)
					repeatStatus = RepeatStatus.ALL;
				else 
					repeatStatus = RepeatStatus.ONE;
				new Thread(new Runnable() {
					@Override
					public void run() {
						Editor editor = mPerferences.edit();//
						editor.putInt("repeatIndex_audio", repeatIndex);
						editor.commit();
					}
				}).start();
				updateNotification();
    		}
    	}
    }
    
    private void seekAction(Intent intent){
		hacktick = intent.getIntExtra("progress", 0);
		synchronized (obj) {
			canForward = false;
		}
		if(forwardStatus != ForwardStatus.NONE){
		forwardStatus = ForwardStatus.NONE;
		forwardIconIndex = -1;
		}
		if (status == sta_pause) {
			try {
				mp.seekTo(hacktick);
				doRequestAF();
				mp.start();
				status = sta_play;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			mp.seekTo(hacktick);
			handler.removeMessages(MSG_SEEK_OUT);
			Message msg = handler.obtainMessage(MSG_SEEK_OUT);
			handler.sendMessageDelayed(msg, 1000);
		}
		handler.removeMessages(MSG_HACK_TIKE);
    }
    private void updatePlaylist(){
    	ArrayList<DLNAFileInfo> listItems = map.getFileList();
		ArrayList<DLNAFileInfo> playlistItems = new  ArrayList<DLNAFileInfo>(listItems);
		musicList = playlistItems;
		map.setPlayList(playlistItems);
    }
	private void cancelForward() {
		synchronized (obj) {
			canForward = false;
		}
		forwardStatus = ForwardStatus.NONE;
		forwardIconIndex = -1;
		if (status == sta_pause) {
			try {
				handler.removeMessages(MSG_SEEK_OUT);
				doRequestAF();
				mp.start();
				hacktick = 0;
				status = sta_play;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		handler.removeMessages(MSG_HACK_TIKE);
	}
	
	private void setForward(){
		if (status == sta_play) {
			try {
				mp.pause();
				status = sta_pause;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		handler.removeMessages(MSG_SEEK_OUT);
		if (hacktick <= 0) {
			hacktick = mp.getCurrentPosition();
			prePosition = hacktick;
		}

		synchronized (obj) {
			canForward = true;
		}
		handler.removeMessages(MSG_HACK_TIKE);
		handler.sendEmptyMessageDelayed(MSG_HACK_TIKE, 0);
	}
	
	private void stopForward(){
		synchronized (obj) {
			canForward = false;
		}
		forwardStatus = ForwardStatus.NONE;
		forwardIconIndex = -1;
		if (status == sta_pause) {
			try {
				mp.seekTo(hacktick);
				doRequestAF();
				mp.start();
				// hacktick = 0;
				status = sta_play;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		handler.removeMessages(MSG_HACK_TIKE);
	}
	
	OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {  
        public void onAudioFocusChange(int focusChange) {  
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {  
            	//doReset();
            	mp.pause();
            	lostFocus = true;
    			playStaSrc = R.drawable.dnla_music_bar_icon_stop;
    			status = sta_pause;  
    			updateNotification();
    			
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {  
            	lostFocus = true;
            	playOnly(); 
                // Resume playback  
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {  
            	//doReset();
            	mp.pause();
            	lostFocus = true;
    			playStaSrc = R.drawable.dnla_music_bar_icon_stop;
    			status = sta_pause; 
    			updateNotification();
                am.abandonAudioFocus(audioFocusChangeListener);  
                // Stop playback  
            }   
        }  
    };
    
	@Override
    public synchronized void update(Observable o, Object arg) {
		ObserverContent content = (ObserverContent)arg;
		String serverName = content.getMsg();
		String act = content.getAction();
		if(act.equals(ObserverContent.REMOVE_DEVICE)) {
			if(servername.equals(serverName))
				handler.sendEmptyMessage(MSG_SERVER_DIE);
		}
	}
	
	private void clearNofification(){
		quitPlay = true;
		handler.removeMessages(1);
		MusicMsg musicMsg = new MusicMsg();
		musicMsg.setAction("COM.RTK.MUSIC.CLOSE");
		Notification mNotification = new Notification();
		mNotification.icon = R.drawable.ic_launcher;
		mNotification.tickerText = MusicMsg.valString(musicMsg);
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotification.setLatestEventInfo(this, "", "", null);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		mNotificationManager.notify(0, mNotification);
	}
	
	public static String hexString2binaryString(String hexString){  
		if (hexString == null || hexString.length() % 2 != 0)  
			return null;  
		String bString = "", tmp;  
		for (int i = 0; i < hexString.length(); i++){  
			tmp = "0000" + Integer.toBinaryString(Integer.parseInt(hexString.substring(i, i + 1), 16)); 
			bString += tmp.substring(tmp.length() - 4);  
		}  
		return bString;  
	}
	
	public boolean checkProtolInfo(String protolInfo) {
		if(protolInfo != null) {
			if(protolInfo.contains("DLNA.ORG_PN")) {
				return checkNext("DLNA.ORG_PN", protolInfo);
			}
			return false;
		}
		return false;
	}
	
	boolean checkNext(String prefix, String protolInfo) {
		int len = prefix.length();
		String fromPrefix = protolInfo.substring(protolInfo.indexOf(prefix));
		String noPrefix = fromPrefix.substring(len, fromPrefix.length());
		Log.v("MusciService", "fromPrefix=" + fromPrefix);
		Log.v("MusciService", "noPrefix=" + noPrefix);
		if(prefix.equals("DLNA.ORG_PN")) {
			if(noPrefix.contains("LPCM")
					|| noPrefix.contains("MP3") || noPrefix.contains("AAC_ADTS") 
					|| noPrefix.contains("AAC_ISO_320") ||noPrefix.contains("WMABASE")
					|| noPrefix.contains("WMAFULL")) {
				return true;
			}
		}
		return false;
	}
	
	private boolean lostFocus = false;
	private void sentNotification(String msg) {
		if (!bkplay)
			return;
		Notification mNotification = new Notification();
		mNotification.icon = R.drawable.ic_launcher;
		mNotification.tickerText = msg;
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotification.setLatestEventInfo(this, "", "", null);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		mNotificationManager.notify(0, mNotification);
	}
	
	private void progressNotification() {
    	if(!bkplay)
    		return;
		MusicMsg musicMsg = new MusicMsg();
		musicMsg.setAction("CMD_MSG_PROCESS_TIME");
		musicMsg.setCurrentTime((currentTime));
		musicMsg.setDuration((m_duration));
		musicMsg.setStatus(status);
		musicMsg.setForwardDrawableIndex(forwardIconIndex);
		sentNotification(MusicMsg.valString(musicMsg));
	}

	private void resetNotification() {
    	if(!bkplay)
    		return;
		MusicMsg musicMsg = new MusicMsg();
		musicMsg.setAction(CMD_MSG_RESET);
		sentNotification(MusicMsg.valString(musicMsg));
	}

	private void playNotification() {
    	if(!bkplay)
    		return;
		MusicMsg musicMsg = new MusicMsg();
		musicMsg.setAction(CMD_MSG_PLAY);
		musicMsg.setForwardDrawableIndex(forwardIconIndex);
		sentNotification(MusicMsg.valString(musicMsg));
	}

	private void pauseNotification() {
    	if(!bkplay)
    		return;
		MusicMsg musicMsg = new MusicMsg();
		musicMsg.setAction(CMD_MSG_PAUSE);
		sentNotification(MusicMsg.valString(musicMsg));
	}
	
	private void lastNotification() {
    	if(!bkplay)
    		return;
		MusicMsg musicMsg = new MusicMsg();
		musicMsg.setAction(CMD_MSG_LAST);
		sentNotification(MusicMsg.valString(musicMsg));
	}
}
