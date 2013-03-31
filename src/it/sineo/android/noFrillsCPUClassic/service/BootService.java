package it.sineo.android.noFrillsCPUClassic.service;

import it.sineo.android.noFrillsCPUClassic.R;
import it.sineo.android.noFrillsCPUClassic.extra.Constants;
import it.sineo.android.noFrillsCPUClassic.extra.SysUtils;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		/*
		 * Not working with IPC, we can return null.
		 */
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onCreate();
		Log.d(Constants.APP_TAG, "boot service started");
		if (SysUtils.isRooted() && SysUtils.hasSysfs()) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

			/*
			 * Read user setting: are settings to be applied?
			 */
			if (prefs.getBoolean(Constants.PREF_APPLY_ON_BOOT, false)) {
				String sound = prefs.getString(Constants.PREF_RINGTONE, Constants.PREF_DEFAULT_RINGTONE);
				String pattern = null;
				if (prefs.getBoolean(Constants.PREF_VIBRATE, Constants.PREF_DEFAULT_VIBRATE)) {
					pattern = prefs.getString(Constants.PREF_VIBRATE_PATTERN, "");
				}
				/*
				 * Safety-valves to skip applying on boot.
				 */
				if (!prefs.getBoolean(Constants.PREF_DISABLE_SAFETY_VALVE, Constants.PREF_DEFAULT_DISABLE_SAFETY_VALVE)
						&& !prefs.getBoolean(Constants.CHECK_SHUTDOWN_OK, false)) {
					/*
					 * Phone didn't shut down properly.
					 */
					Log.i(Constants.APP_TAG, "detected bad shutdown");
					/*
					 * Check for uptime, sometimes it's just the UI layer which was killed
					 * by an OOM or by a (stupid) killall.
					 */
					long uptime = SystemClock.uptimeMillis();
					if (uptime > prefs.getLong(Constants.LAST_UPTIME, uptime)) {
						Log.i(Constants.APP_TAG, "detected a soft-reboot of the UI layer, skipping bad reboot notification");
					} else {
						/* Not a soft-reboot */
						SysUtils.showNotification(this, getString(R.string.notify_title_safety),
								getString(R.string.notify_safety_shutdown), sound, pattern);
					}
				} else if ((new File(Constants.NLJ_SAFETY_CHECK_DATA)).exists()) {
					/*
					 * NameLessJedi check on /data
					 */
					Log.i(Constants.APP_TAG, "detected NLJ's " + Constants.NLJ_SAFETY_CHECK_DATA);
					SysUtils.showNotification(this, getString(R.string.notify_title_safety),
							getString(R.string.notify_nlj_safety_found, Constants.NLJ_SAFETY_CHECK_DATA), sound, pattern);
				} else if ((new File(Constants.NLJ_SAFETY_CHECK_EXT)).exists()) {
					/*
					 * NameLessJedi check on /sd-ext
					 */
					Log.i(Constants.APP_TAG, "detected NLJ's " + Constants.NLJ_SAFETY_CHECK_EXT);
					SysUtils.showNotification(this, getString(R.string.notify_title_safety),
							getString(R.string.notify_nlj_safety_found, Constants.NLJ_SAFETY_CHECK_EXT), sound, pattern);
				} else {
					/*
					 * SG-1 you have a go!
					 */
					String min_freq = prefs.getString(Constants.PREF_MIN_FREQ, null);
					String max_freq = prefs.getString(Constants.PREF_MAX_FREQ, null);
					String governor = prefs.getString(Constants.PREF_GOVERNOR, null);
					String ioscheduler = prefs.getString(Constants.PREF_IOSCHEDULER, null);
					if (min_freq != null || max_freq != null || governor != null || ioscheduler != null) {
						/*
						 * Set dirty flag before altering frequencies:
						 */
						SharedPreferences.Editor editor = prefs.edit();
						editor.putBoolean(Constants.CHECK_SHUTDOWN_OK, false);
						editor.putLong(Constants.LAST_UPTIME, SystemClock.uptimeMillis());
						editor.commit();

						boolean changePermissions = prefs.getBoolean(Constants.PREF_CHANGE_PERMISSIONS,
								Constants.PREF_DEFAULT_CHANGE_PERMISSIONS);

						boolean updated = SysUtils.setFrequenciesAndGovernor(min_freq, max_freq, governor, ioscheduler,
								changePermissions, this, R.string.ok_updated_freqs, R.string.err_update_failed);
						if (prefs.getBoolean(Constants.PREF_NOTIFY_ON_BOOT, Constants.PREF_DEFAULT_NOTIFY_ON_BOOT)) {
							if (updated) {
								SysUtils.showNotification(
										this,
										getString(R.string.notify_title_success),
										getString(R.string.notify_success, SysUtils.getMinFreq(), SysUtils.getMaxFreq(),
												SysUtils.getGovernor(), SysUtils.getIOScheduler()), sound, pattern);
							} else {
								SysUtils.showNotification(this, getString(R.string.notify_title_safety),
										getString(R.string.err_update_failed), sound, pattern);
							}
						}
					}
				}
			}
			/* Reset previous zero points, if found. */
			if (prefs.contains(Constants.STATS_ZERO_POINT)) {
				Editor editor = prefs.edit();
				editor.remove(Constants.STATS_ZERO_POINT);
				editor.commit();
			}
		}
		stopSelf();
		Log.d(Constants.APP_TAG, "boot service stopped");
	}
}
