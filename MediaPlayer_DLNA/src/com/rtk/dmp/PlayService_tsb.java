
package com.rtk.dmp;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.realtek.DataProvider.FileFilterType;
import com.rtk.dmp.MusicActivity.ForwardStatus;
import com.rtk.dmp.MusicActivity.RepeatStatus;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class PlayService_tsb extends Service {
	/*public static MediaPlayer mMediaPlayer;
	public static int status;
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
	private String TAG = "PlayService";

	public static boolean isBackground = false;
	public static ForwardStatus forwardStatus = ForwardStatus.NONE;
	public static int[] forwardSpeed = { 2, 8, 16, 32, 16, 8 };
	public static int forwardIndex = 0;
	public static RepeatStatus repeatStatus = RepeatStatus.OFF;
	private Thread worker = null;
	private Object obj = null;
	private boolean canForward = false;
	private int total = 0;
	public static int hacktick = 0;
*/
	//
	public void onCreate() {
		Log.i("Service", "onCreate");
		super.onCreate();
/*		initWorker();
		obj = new Object();
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				if (MusicActivity.musicList == null
						|| MusicActivity.musicList.size() == 0)
				{
					goBack();
					return;
				}
				if (repeatStatus != RepeatStatus.ONE) {
					MusicActivity.index++;
				}
				if (MusicActivity.index == MusicActivity.musicList.size())
				{
					if(repeatStatus == RepeatStatus.OFF)
					{
						MusicActivity.index --;
						goBack();
						return;
					}
					else
					{
						MusicActivity.index = MusicActivity.firstindex;
					}
				}
				if (!isBackground)
					MusicActivity.handler
							.sendEmptyMessage(MusicActivity.MSG_RESET);
				play();
			}
		});*/
	}

	//
	public IBinder onBind(Intent arg0) {
		Log.i("Service", "onBind");
		return null;
	}

/*	//
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("Service", "onStartCommand");

		if(intent == null)
			return 0;
			
		String action = intent.getAction();
		if (action.equals(PLAY_ACTION)) {
			play();
		} else if (action.equals(PAUSE_ACTION)) {
			pause();
		} else if (action.equals(PAUSEONE_ACTION)) {
			pauseOne();
		} else if (action.equals(NEXT_ACTION)) {
			next();
		} else if (action.equals(PREVIOUS_ACTION)) {
			previous();
		} else if (action.equals(STOP_ACTION)) {
			stop();
		} else if (action.equals(SET_REPEAT)) {
			String repeat = intent.getStringExtra("repeat");
			repeatStatus = RepeatStatus.valueOf(repeat);
		} else if (action.equals(SET_FORWARD)) {
			if (status == MusicActivity.sta_play) {
				try {
					mMediaPlayer.pause();

					status = MusicActivity.sta_pause;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			hacktick = mMediaPlayer.getCurrentPosition();
			synchronized (obj) {
				canForward = true;
			}

		} else if (action.equals(STOP_FORWARD)) {
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
			if (!isBackground)
			{
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_HACK_TIME);
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_RESET_FWD);
			}
			if (status == MusicActivity.sta_pause) {
				try {
					mMediaPlayer.seekTo(hacktick);
					mMediaPlayer.start();
					status = MusicActivity.sta_play;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (action.equals(GO_START)) {
			goStart();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	//
	public void onDestroy() {
		Log.i("Service", "onDestroy");

		mMediaPlayer.release();

		super.onDestroy();
	}

	public void init() {
		synchronized (obj) {
			canForward = false;
		}
		if (MusicActivity.musicList == null
				|| MusicActivity.musicList.isEmpty())
			return;
		mMediaPlayer.reset();
		// String dataSource = getDateByPosition(mCursor, mPlayPosition);
		// String info = getInfoByPosition(mCursor, mPlayPosition);
		//
		// Toast.makeText(getApplicationContext(), info,
		// Toast.LENGTH_SHORT).show();
		try {

			String playPath = MusicActivity.musicList.get(MusicActivity.index);

			// This is Very important Audio only to TvServer

			Log.d(TAG, "set path audio only:" + playPath);
			Map<String, String> config;
			config = new HashMap<String, String>();
			config.put("FLOWTYPE", "PLAYBACK_TYPE_AUDIO_ONLY");
			Method method = mMediaPlayer.getClass().getMethod("setDataSource",
					new Class[] { String.class, Map.class });
			method.invoke(mMediaPlayer, new Object[] { playPath, config });
			Log.e(TAG, "method.invoke done");
			// This is Very important Audio only to TvServer

			mMediaPlayer.prepare();
			mMediaPlayer.start();
			status = MusicActivity.sta_play;
			if (!isBackground)
				MusicActivity.handler
						.sendEmptyMessage(MusicActivity.MSG_TOTAL_TIME);
			total = mMediaPlayer.getDuration();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) // add by kelly catch exception 20120820
		{
			Log.e(TAG, "kellykelly exception error: " + e);
			e.printStackTrace();

		}
	}

	// play the music
	public void play() {
		init();
	}

	//
	public void pause() {
		if (status == MusicActivity.sta_play) {
			mMediaPlayer.pause();
			status = MusicActivity.sta_pause;
			if (!isBackground)
				MusicActivity.handler
						.sendEmptyMessage(MusicActivity.MSG_PAUSE);
		} else {
			mMediaPlayer.start();
			status = MusicActivity.sta_play;
			if (!isBackground)
				MusicActivity.handler
						.sendEmptyMessage(MusicActivity.MSG_PLAY);
		}
	}

	public void pauseOne() {
		if (status == MusicActivity.sta_play) {
			mMediaPlayer.pause();
			status = MusicActivity.sta_pause;
		}
	}

	//
	public void stop() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		stopSelf();
	}

	//
	public void previous() {
		int pretag = status;
		if (MusicActivity.index == MusicActivity.firstindex) {
			MusicActivity.index = MusicActivity.musicList.size() - 1;
		} else {
			MusicActivity.index--;
		}
		if (!isBackground)
			MusicActivity.handler.sendEmptyMessage(MusicActivity.MSG_RESET);
		init();
		if(forwardStatus != ForwardStatus.NONE)
		{
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
			if (!isBackground)
			{
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_RESET_FWD);
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_PLAY);
			}
		}else
		{
			if(pretag == MusicActivity.sta_pause)
			{
				mMediaPlayer.pause();
				status = MusicActivity.sta_pause;
			}
		}
	}

	//
	public void next() {
		int pretag = status;
		if (MusicActivity.index == MusicActivity.musicList.size() - 1) {
			MusicActivity.index = MusicActivity.firstindex;
		} else {
			MusicActivity.index++;
		}
		if (!isBackground)
			MusicActivity.handler.sendEmptyMessage(MusicActivity.MSG_RESET);
		init();
		if(forwardStatus != ForwardStatus.NONE)
		{
			synchronized (obj) {
				canForward = false;
			}
			forwardStatus = ForwardStatus.NONE;
			if (!isBackground)
			{
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_RESET_FWD);
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_PLAY);
			}
		}else
		{
			if(pretag == MusicActivity.sta_pause)
			{
				mMediaPlayer.pause();
				status = MusicActivity.sta_pause;
			}
		}
	}

	public void goStart() {
		if (status == MusicActivity.sta_play) {
			mMediaPlayer.seekTo(0);
		}
	}

	public void addSeek(int tick) {
		if (status == MusicActivity.sta_play) {
			try {
				mMediaPlayer.pause();
				int position = mMediaPlayer.getCurrentPosition() + tick;
				if (position < 0)
					position = 0;
				if (position > mMediaPlayer.getDuration())
					position = mMediaPlayer.getDuration() - 100;
				Log.e("ggggg", "position = " + position);
				mMediaPlayer.seekTo(position);
				mMediaPlayer.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addFakeSeek(int tick) {

		hacktick +=  tick;
		if (hacktick < 0)
		{
			hacktick = 0;
			synchronized (obj) {
				canForward = false;
			}
			try {
				mMediaPlayer.seekTo(hacktick);
				mMediaPlayer.start();
				status = MusicActivity.sta_play;
			} catch (Exception e) {
				e.printStackTrace();
			}
			forwardStatus = ForwardStatus.NONE;
			if (!isBackground)
			{
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_RESET_FWD);
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_PLAY);
			}
		}
		if (hacktick >= mMediaPlayer.getDuration()) {
			hacktick = 0;
			synchronized (obj) {
				canForward = false;
			}
			if (repeatStatus == RepeatStatus.ONE) {
				try {
					mMediaPlayer.seekTo(hacktick);
					mMediaPlayer.start();
					status = MusicActivity.sta_play;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else {
				MusicActivity.index++;
				if (MusicActivity.index == MusicActivity.musicList.size())
				{
					if(repeatStatus == RepeatStatus.OFF)
					{
						MusicActivity.index --;
						try {
							mMediaPlayer.seekTo(mMediaPlayer.getDuration() -10);
							mMediaPlayer.start();
							status = MusicActivity.sta_play;
						} catch (Exception e) {
							e.printStackTrace();
						}
						return;
					}
					MusicActivity.index = MusicActivity.firstindex;
				}
				if (!isBackground)
					MusicActivity.handler
							.sendEmptyMessage(MusicActivity.MSG_RESET);
				play();
			}
			forwardStatus = ForwardStatus.NONE;
			if (!isBackground)
			{
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_RESET_FWD);
				MusicActivity.handler
				.sendEmptyMessage(MusicActivity.MSG_PLAY);
			}
		}
		if (!isBackground)
		{
			MusicActivity.handler
					.sendEmptyMessage(MusicActivity.MSG_HACK_TIME);
		}
		Log.e("ggggg", "position = " + hacktick);
	}



	private void initWorker() {
		worker = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (canForward) {
						int sign = 1;
						if (forwardStatus == ForwardStatus.REWIND)
							sign = -1;
						int position = forwardSpeed[forwardIndex] * 1000 * sign;
						Log.e("ggggg", "seek :" + position);
						addFakeSeek(position);
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		worker.start();
	}
	
	private void goBack()
	{
		AudioBrowser.index = MusicActivity.index;
		AudioBrowser.changeIndex = true;
		Intent intent = new Intent();
		intent.putExtra("action", "BACK");
		intent.setAction("com.rtk.dmp.PlayService");
		sendBroadcast(intent);
	}*/
}
