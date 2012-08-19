package it.sineo.android.noFrillsCPUClassic.activity;

import it.sineo.android.noFrillsCPUClassic.R;
import it.sineo.android.noFrillsCPUClassic.extra.Constants;
import it.sineo.android.noFrillsCPUClassic.extra.PatternReplacerInputFilter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.Log;

public class PreferencesActivity extends PreferenceActivity implements OnPreferenceChangeListener {

	private EditTextPreference prefPattern;
	private CheckBoxPreference prefSafetyValve;
	private CheckBoxPreference prefChangePermissions;
	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		prefPattern = (EditTextPreference) findPreference(Constants.PREF_VIBRATE_PATTERN);
		prefPattern.setOnPreferenceChangeListener(this);
		prefPattern.setSummary(prefs.getString(Constants.PREF_VIBRATE_PATTERN, ""));
		prefPattern.getEditText().setFilters(new InputFilter[] {
			new PatternReplacerInputFilter("[^0-9,]")
		});

		prefChangePermissions = (CheckBoxPreference) findPreference(Constants.PREF_CHANGE_PERMISSIONS);
		prefChangePermissions.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(final Preference preference, final Object newValue) {
		if (preference == prefPattern) {
			preference.setSummary((String) newValue);
			return true;
		} else if (preference == prefSafetyValve || preference == prefChangePermissions) {
			Log.d(Constants.APP_TAG, preference.getKey() + " -> " + newValue.toString());
			if (Boolean.valueOf(newValue.toString())) {
				/*
				 * Prompt an alert dialog in style of CyanogenMod's "Dragons ahead!".
				 * Return false and handle dialog.
				 */
				int title = -1;
				int message = -1;
				if (preference == prefSafetyValve) {
					title = R.string.prefs_disable_safety_valve;
					message = R.string.prefs_disable_safety_valve_dialog_message;
				} else if (preference == prefChangePermissions) {
					title = R.string.prefs_change_permissions;
					message = R.string.prefs_change_permissions_dialog_message;
				}
				AlertDialog.Builder bldr = new AlertDialog.Builder(PreferencesActivity.this);
				bldr.setIcon(android.R.drawable.ic_dialog_alert);
				bldr.setTitle(title);
				bldr.setMessage(message);
				bldr.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						prefs.edit().putBoolean(preference.getKey(), true).commit();
						((CheckBoxPreference) preference).setChecked(true);
					}
				});
				bldr.setNegativeButton(android.R.string.cancel, null);
				bldr.create().show();
				return false;
			} else {
				/* Setting to false: ok */
				return true;
			}
		}
		return false;
	}
}
