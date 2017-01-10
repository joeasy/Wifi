
package com.realtek.cast;

import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.TextUtils;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
			        .add(android.R.id.content, new SettingsFragment()).commit();
		}
	}

	public static class SettingsFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			AppPreference pref = AppPreference.getInstance(getActivity());
			addPreferencesFromResource(R.xml.preference_airplay);

			// AirPlay Service
			SwitchPreference prefAirplay = (SwitchPreference) findPreference(AppPreference.KEY_AIRPLAY_SWITCH);
			prefAirplay.setChecked(pref.isAirPlayEnabled());
			prefAirplay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if ((Boolean) newValue) {
						AirService.startService(getActivity());
					} else {
						AirService.stopService(getActivity());
					}
					return true;
				}
			});
			
			// AirPlay Service Name
			String name = pref.getAirPlayServiceName();
			EditTextPreference prefAirplayName = (EditTextPreference) findPreference(AppPreference.KEY_AIRPLAY_SERVICE_NAME);
			prefAirplayName.setDefaultValue(AppPreference.DEFAULT_AIRPLAY_SERVICE_NAME);
			prefAirplayName.setText(name);
			prefAirplayName.setSummary(name);
			prefAirplayName.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String name = newValue.toString();
					if (TextUtils.isEmpty(name)) {
						// TODO: Toast error message
						return false;
					}
					preference.setSummary(name);
					return true;
				}
			});
			
		}

	}
}
