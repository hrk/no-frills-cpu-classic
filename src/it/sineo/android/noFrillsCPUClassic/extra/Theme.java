package it.sineo.android.noFrillsCPUClassic.extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

public enum Theme {
	/*
	 * Light + Dark action bar har no meaning when on SDK<=10, since there is no
	 * action bar at all.
	 */
	THEME_DARK(android.R.style.Theme_Black, android.R.style.Theme_Holo), THEME_LIGHT(android.R.style.Theme_Light,
			android.R.style.Theme_Holo_Light), THEME_LIGHT_DARK(android.R.style.Theme_Light,
			android.R.style.Theme_Holo_Light_DarkActionBar);
	int themeId;
	int descriptionId;

	private Theme(int themeId, int v11ThemeId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.themeId = v11ThemeId;
		} else {
			this.themeId = themeId;
		}
	}

	public static Theme applyTo(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Theme theme = Theme.valueOf(prefs.getString(Constants.PREF_THEME, Constants.PREF_DEFAULT_THEME));
		final int themeId = theme.themeId;
		context.setTheme(themeId);
		return theme;
	}

	public int getThemeId() {
		return this.themeId;
	}
}
