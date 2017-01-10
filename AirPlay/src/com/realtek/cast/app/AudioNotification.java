package com.realtek.cast.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

import com.realtek.cast.CastApplication;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.AudioInformation;
import com.realtek.cast.control.PlaybackControl.OnAudioChangedListener;

public class AudioNotification implements OnAudioChangedListener, OnAudioFocusChangeListener {

	private final Context mContext;
	private final AudioInformation mInfo = new AudioInformation();
	private AudioManager mAudioManager;
	private NotificationManager mNotificationManager;
	
	public AudioNotification(Context context) {
		mContext = context;
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
    public void onAudioStart() {
	    mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

	@Override
    public void onAudioStop() {
		mAudioManager.abandonAudioFocus(this);
		mNotificationManager.cancel(0);
    }

	@Override
    public void onAudioProgressChanged() {
//		Intent intent = PlaybackActivity.createIntent(mContext);
//		PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
//    	int start = (int) (info.mAudioStart / 1000L);
//    	int curr = (int) (info.mAudioCurr / 1000L);
//    	int end = (int) (info.mAudioEnd / 1000L);
//	    Notification n = new Notification.Builder(mContext)
//	    .setSmallIcon(android.R.drawable.ic_media_play)
//    	.setContentTitle(info.mTrackName)
//    	.setContentText(info.mTrackAlbum)
//    	.setLargeIcon(info.getCoverArt())
//    	.setOngoing(true)
//    	.setProgress(end - start, curr - start, false)
//    	.setContentIntent(pi)
//    	.build();
//	    mNotificationManager.notify(CastApplication.NOTIFICATION_ID, n);
    }

	@Override
    public void onAudioInfoChanged() {
		PlaybackControl.getInstance().getAudioInformation(mInfo);
		
		Intent intent = PlaybackActivity.createIntent(mContext);
		PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 0);
		Notification n = new Notification.Builder(mContext)
		.setSmallIcon(android.R.drawable.ic_media_play)
		.setContentTitle(mInfo.mTrackName)
		.setContentText(mInfo.mTrackAlbum)
		.setContentIntent(pi)
		.setLargeIcon(PlaybackControl.getInstance().getAudioCoverArtForNotification())
		.setOngoing(true)
		.setAutoCancel(false)
		.build();
		mNotificationManager.notify(CastApplication.NOTIFICATION_ID, n);
    }

	@Override
    public void onAudioFocusChange(int focusChange) {
	    
    }
	
}
