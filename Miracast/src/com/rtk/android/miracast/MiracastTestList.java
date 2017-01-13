package com.rtk.android.miracast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;
//import android.app.TvManager;
import android.os.CountDownTimer;

public class MiracastTestList extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener,
		Preference.OnPreferenceChangeListener {

	public static final String KEY_AUTO_GO_MODE = "autogomode";
	public static final String KEY_GO_INTENT = "gointent";
	public static final String KEY_LISTEN_CHANNEL = "listenchannel";
	public static final String KEY_OPERATING_CHANNEL = "operatingchannel";
	public static final String KEY_USER_COMMAND = "selfcmd";
	public static final String TAG = "MiracastTest";
	private SwitchPreference mAutoGoPref;
	private int[] mFreqs;
	private ListPreference mGoIntentPref;
	private ListPreference mListenChannelPref;
	private ListPreference mOperatingChannelPref;
	private SharedPreferences mSharedPreferences;
	private EditTextPreference mUserCommandPref;
	private PreferenceScreen mSSIDPref;
	private PreferenceScreen mPSKPref;
	
	CountDownTimer timer = null;
	
	//public TvManager mTV = null;

	public boolean RootCmd(String paramString) {
		return false;
	}

	protected void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		
	//	mTV = (TvManager) this.getSystemService("tv");
		
		addPreferencesFromResource(R.xml.testlist);

		this.mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

		this.mListenChannelPref = ((ListPreference) findPreference("listenchannel"));
		this.mListenChannelPref
				.setSummary("channel "
						+ this.mSharedPreferences.getString("listenchannel",
								"not set"));

		this.mOperatingChannelPref = ((ListPreference) findPreference("operatingchannel"));
		this.mOperatingChannelPref.setSummary("channel "
				+ this.mSharedPreferences.getString("operatingchannel",
						"not set"));

		this.mGoIntentPref = ((ListPreference) findPreference("gointent"));
		this.mGoIntentPref.setSummary(this.mSharedPreferences.getString(
				"gointent", "not set"));

		this.mAutoGoPref = ((SwitchPreference) findPreference("autogomode"));
		this.mAutoGoPref.setChecked(this.mSharedPreferences.getBoolean(
				"autogomode", false));
		this.mAutoGoPref.setOnPreferenceChangeListener(this);

		this.mUserCommandPref = ((EditTextPreference) findPreference("selfcmd"));
		this.mUserCommandPref.setOnPreferenceChangeListener(this);
		
		/*
		this.mUserCommandPref
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(
							Preference paramAnonymousPreference,
							Object paramAnonymousObject) {
						
						 * if
						 * (paramAnonymousPreference.getKey().equals("selfcmd"))
						 * { String str = (String)paramAnonymousObject;
						 * Log.e("MiracastTest", "set user cmd:" + str);
						 * MiracastTestList.this.RootCmd(str); }
						 
						return true;
					}
				});
		*/
		
		this.mSSIDPref = ((PreferenceScreen) findPreference("ssid"));
		this.mSSIDPref.setTitle("SSID");
		
		this.mPSKPref = ((PreferenceScreen) findPreference("psk"));
		this.mPSKPref.setTitle("PSK");
		
		this.mFreqs = getResources().getIntArray(R.array.freqs);
		
		timer = new CountDownTimer(1000, 500) {  
		    public void onTick(long millisUntilFinished) {  
		    }  
		    public void onFinish() { 
		    	readSSID();
		    	readPSK();
		    }  
		};
	}

	public boolean onCreateOptionsMenu(Menu paramMenu) {
		getMenuInflater().inflate(R.menu.miracast_test_list, paramMenu);
		return true;
	}

	public void onSharedPreferenceChanged(
			SharedPreferences paramSharedPreferences, String paramString) {
		if (paramString.equals("listenchannel")) {
			String str3 = this.mListenChannelPref.getValue();
			Log.d("MiracastTest", "set Listen channel:" + str3);
			setListenChannel(str3);
			this.mListenChannelPref.setSummary("channel " + str3);
		} else if (paramString.equals("operatingchannel")) {
			String str2 = this.mOperatingChannelPref.getValue();
			Log.d("MiracastTest", "set operating channel:" + str2);
			this.mOperatingChannelPref.setSummary("channel " + str2);
		} else if (paramString.equals("gointent")) {
			String str1 = this.mGoIntentPref.getValue();
			Log.d("MiracastTest", "set GO intent value:" + str1);
			setGoIntentValue(str1);
			this.mGoIntentPref.setSummary("value " + str1);
		}
	}

	public boolean onPreferenceChange(Preference paramPreference,
			Object paramObject) {
		if (paramPreference.getKey().equals("autogomode"))
	    {
			if (!((Boolean)paramObject).booleanValue())
			{
				Log.d("Miracast", "Turn Off AutoGO");
				return true;
			}
		  
			Log.d("Miracast", "Turn On AutoGO");
			String opChStr = this.mSharedPreferences.getString("operatingchannel", "not set");
			if (opChStr.equals("not set")) {
				Toast.makeText(this, "Please set operating channel!", 1).show();
			} else {
				int i = Integer.valueOf(opChStr).intValue();
				Log.d("Miracast", "AudtoGO: " + (i-1));
				setAutoGoMode(this.mFreqs[i-1]);
			}
	    } else if (paramPreference.getKey().equals("selfcmd")) {
	    	String shellCmd = (String)paramObject;
	    	Log.d("Miracast", "exec cmd: " + shellCmd);
			//mTV.execRootCmd(shellCmd);
		}
		
		return true;
	}

	public void setAutoGoMode(int paramInt) {
		String cmd = "wpa_cli -i p2p0 -p /data/misc/wifi/sockets/ p2p_group_add persistent freq=";
		StringBuilder listenCmd = new StringBuilder();
		listenCmd.append(cmd);
		listenCmd.append(paramInt);
		
		Log.d("Miracast", "exec cmd: " + listenCmd);
		//mTV.execRootCmd(listenCmd.toString());
		
		timer.cancel();
		timer.start();
	}

	public void setGoIntentValue(String paramString) {
		String cmd = "wpa_cli -i p2p0 -p /data/misc/wifi/sockets/ set p2p_go_intent ";
		StringBuilder listenCmd = new StringBuilder();
		listenCmd.append(cmd);
		listenCmd.append(paramString);
		
		Log.d("Miracast", "exec cmd: " + listenCmd);
		//mTV.execRootCmd(listenCmd.toString());
	}

	public void setListenChannel(String paramString) {
		String cmd = "wpa_cli -i p2p0 -p /data/misc/wifi/sockets/ p2p_set listen_channel ";
		StringBuilder listenCmd = new StringBuilder();
		listenCmd.append(cmd);
		listenCmd.append(paramString);
		
		Log.d("Miracast", "exec cmd: " + listenCmd);
		//mTV.execRootCmd(listenCmd.toString());
	}
	
	public void readSSID() {
		try {
			File f = new File("/tmp/RealtekTmp/p2p_ssid");
			FileInputStream fis = new FileInputStream(f);
			byte[] buf = new byte[1024];
			int hasRead = fis.read(buf);
			if (hasRead > 0)
			{	
				String name = new String(buf);
				this.mSSIDPref.setTitle("SSID - " + name);
			}
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void readPSK() {
		try {
			File f = new File("/tmp/RealtekTmp/p2p_psk");
			FileInputStream fis = new FileInputStream(f);
			byte[] buf = new byte[1024];
			int hasRead = fis.read(buf);
			if (hasRead > 0)
			{	
				String name = new String(buf);
				this.mPSKPref.setTitle("PSK - " + name);
			}
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
