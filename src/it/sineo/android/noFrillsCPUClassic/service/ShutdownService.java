package it.sineo.android.noFrillsCPUClassic.service;

import it.sineo.android.noFrillsCPUClassic.extra.Constants;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class ShutdownService extends Service {

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
		Log.d(Constants.APP_TAG, "shutdown service started");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(Constants.CHECK_SHUTDOWN_OK, true);
		/*
		 * No need to touch LAST_UPDATE, since it'll be checked only if
		 * CHECK_SHUTDOWN_OK is false, but we remove the reset point for stats, if
		 * it exists.
		 */
		if (prefs.contains(Constants.STATS_ZERO_POINT)) {
			editor.remove(Constants.STATS_ZERO_POINT);
		}
		editor.commit();
		stopSelf();
		Log.d(Constants.APP_TAG, "shutdown service stopped");
	}
}
