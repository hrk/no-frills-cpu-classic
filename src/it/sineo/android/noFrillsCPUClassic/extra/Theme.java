package it.sineo.android.noFrillsCPUClassic.extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public enum Theme {
	/*
	 * Classic can't handle LIGHT+DARK (since there is no action bar!) but in
	 * order to avoid breakages, we put a fake third element in the enumeration.
	 */
	THEME_DARK(android.R.style.Theme_Black), THEME_LIGHT(android.R.style.Theme_Light), THEME_LIGHT_DARK(android.R.style.Theme_Light);
	int themeId;
	int descriptionId;

	private Theme(int themeId) {
		this.themeId = themeId;
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
