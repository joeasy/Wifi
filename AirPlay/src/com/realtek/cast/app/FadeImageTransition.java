
package com.realtek.cast.app;

import android.graphics.Bitmap;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

import com.realtek.cast.util.ImageLoader.Callback;

public class FadeImageTransition implements Callback {

	private ImageView mImage1;
	private ImageView mImage2;
	private ImageView mImageShown;

	public FadeImageTransition(ImageView image1, ImageView image2) {
		mImage1 = image1;
		mImage2 = image2;
	}

	@Override
	public void setImageViewBitmap(ImageView imageView, Bitmap bitmap, boolean immediate) {
		if (bitmap == null) {
			return;
		}

		ImageView last = mImageShown != null ? mImageShown : mImage2;
		ImageView next = last == mImage1 ? mImage2 : mImage1;

		next.setImageBitmap(bitmap);
		fadeOut(last);
		fadeIn(next);

		mImageShown = next;
	}

	public void fadeIn(ImageView view) {
		Animation a = fade(view, View.VISIBLE, 0.0f, 1.0f);
		view.startAnimation(a);
	}

	public void fadeOut(final ImageView view) {
		Animation a = fade(view, View.INVISIBLE, 1.0f, 0.0f);
		a.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				view.setImageBitmap(null);
			}
		});
		view.startAnimation(a);
	}

	public Animation fade(final View view, final int visibility, float startAlpha, float endAlpha) {
		AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
		anim.setDuration(300);
		view.setVisibility(visibility);
		return anim;
	}
}
