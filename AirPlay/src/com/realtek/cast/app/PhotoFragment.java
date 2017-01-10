
package com.realtek.cast.app;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.realtek.cast.R;
import com.realtek.cast.control.PlaybackControl;
import com.realtek.cast.control.PlaybackControl.OnPhotoChangedListener;
import com.realtek.cast.control.PlaybackControl.PhotoInformation;
import com.realtek.cast.util.ImageLoader;
import com.realtek.cast.util.ImageLocalStore;

import java.io.File;

public class PhotoFragment extends Fragment implements OnPhotoChangedListener {
	
	// Swap two ImageViews to perform transition effects.
	private ImageView mImage1;
	private ImageView mImage2;
	
	private View mProgress;
	
	private String mPrevKey;
	
	private final PhotoInformation mInfo = new PhotoInformation();
	
	private ImageLoader.Callback mImageCallback;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo, container, false);
		mImage1 = (ImageView) v.findViewById(R.id.image1);
		mImage2 = (ImageView) v.findViewById(R.id.image2);
		mProgress = v.findViewById(R.id.progress_loading);
		mImageCallback = new FadeImageTransition(mImage1, mImage2);
		return v;
	}

	private void refresh() {
		PlaybackControl.getInstance().getPhotoInformation(mInfo);
		
		String key = mInfo.assetKey;

		// Show/Hide progress view
		mProgress.setVisibility((mPrevKey != null || key != null) ? View.GONE : View.VISIBLE);
		
		if (key != null && !key.equals(mPrevKey)) {
			mPrevKey = key;
			File f = ImageLocalStore.getImageFile(getActivity(), key);
			ImageLoader.getInstance().loadImage(mImage1, Uri.fromFile(f), mImageCallback);
		}
	}
	
	@Override
    public void onStart() {
	    super.onStart();
	    PlaybackControl.getInstance().registerPhotoCallback(this, true);
    }

	@Override
    public void onStop() {
	    super.onStop();
	    PlaybackControl.getInstance().unregisterPhotoCallback(this);
	    PlaybackControl.getInstance().stopPhoto();
	    PlaybackControl.getInstance().stopSlideShow();
    }

	@Override
    public void onPhotoStateChange(int stat) {
	    refresh();
    }

	@Override
    public void onSlideShowStateChange(int stat, int assetId, int lastAssetId) {
		refresh();
    }

}
