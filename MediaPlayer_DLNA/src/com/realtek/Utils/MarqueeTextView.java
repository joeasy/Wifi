package com.realtek.Utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;

public class MarqueeTextView extends TextView {

	boolean focused = false;

	public MarqueeTextView(Context context) {
		super(context);
	}

	public MarqueeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public boolean setFocused(boolean focused) {
		return this.focused = focused;
	}

	public boolean isFocused() {
		return focused;
	}

}
