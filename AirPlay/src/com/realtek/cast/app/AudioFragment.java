
package com.realtek.cast.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.AudioInformation;
import com.realtek.cast.control.PlaybackControl.OnAudioChangedListener;

import com.realtek.cast.R;

public class AudioFragment extends Fragment implements OnAudioChangedListener {
	
	private final AudioInformation mInfo = new AudioInformation();
	
	private TextView mTextTrack;
	private TextView mTextArtist;
	private TextView mTextAlbum;

	private ImageView mImageView;
	private ProgressBar mProgress;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_audio, container, false);
		mImageView = (ImageView) v.findViewById(R.id.image);
		mProgress = (ProgressBar) v.findViewById(R.id.progress);
		mTextTrack = (TextView) v.findViewById(R.id.text_track);
		mTextArtist = (TextView) v.findViewById(R.id.text_artist);
		mTextAlbum = (TextView) v.findViewById(R.id.text_album);
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		PlaybackControl.getInstance().registerAudioCallback(this, true);
	}

	@Override
	public void onPause() {
		super.onPause();
		PlaybackControl.getInstance().unregisterAudioCallback(this);
	}

	@Override
    public void onAudioStart() {
	    
    }

	@Override
    public void onAudioStop() {
		
    }

	@Override
    public void onAudioProgressChanged() {
		PlaybackControl.getInstance().getAudioInformation(mInfo);
		updateProgress();
    }
	
	private void updateProgress() {
		int start = (int) (mInfo.start);
		int curr = (int) (mInfo.current);
		int end = (int) (mInfo.end);
		mProgress.setMax(end - start);
		mProgress.setProgress(curr - start);
	}

	@Override
    public void onAudioInfoChanged() {
		PlaybackControl.getInstance().getAudioInformation(mInfo);
		mImageView.setImageBitmap(PlaybackControl.getInstance().getAudioCoverArt());
		mTextTrack.setText(mInfo.mTrackName);
		mTextAlbum.setText(mInfo.mTrackAlbum);
		mTextArtist.setText(mInfo.mTrackArtist);

		updateProgress();
    }

}
