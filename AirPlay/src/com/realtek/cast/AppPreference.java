package com.realtek.cast;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

public class AppPreference {
	public static final String DEFAULT_AIRPLAY_SERVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
	public static final boolean DEFAULT_AIRPLAY_ENABLE = false;
	
	public static final String KEY_AIRPLAY_SWITCH = "airplay_switch";
	public static final String KEY_AIRPLAY_SERVICE_NAME = "airplay_service_name";

	public static AppPreference sInstance;
	
	public static AppPreference getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new AppPreference(context);
		}
		return sInstance;
	}
	
	private final Context mContext;
	private final SharedPreferences mPref;
	
	public AppPreference(Context context) {
		mContext = context.getApplicationContext();
		mPref = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

	public String getAirPlayServiceName() {
		return mPref.getString(KEY_AIRPLAY_SERVICE_NAME, DEFAULT_AIRPLAY_SERVICE_NAME);
    }

	public boolean isAirPlayEnabled() {
	    return mPref.getBoolean(KEY_AIRPLAY_SWITCH, DEFAULT_AIRPLAY_ENABLE);
    }
}
