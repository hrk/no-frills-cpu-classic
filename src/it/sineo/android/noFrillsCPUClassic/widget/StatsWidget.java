package it.sineo.android.noFrillsCPUClassic.widget;

import it.sineo.android.noFrillsCPUClassic.R;
import it.sineo.android.noFrillsCPUClassic.extra.Constants;
import it.sineo.android.noFrillsCPUClassic.extra.Frequency;
import it.sineo.android.noFrillsCPUClassic.extra.Stats;
import it.sineo.android.noFrillsCPUClassic.extra.Stats.SortMethod;
import it.sineo.android.noFrillsCPUClassic.extra.SysUtils;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class StatsWidget extends AppWidgetProvider {

	static List<Integer> deletedAppWidgetIds = new ArrayList<Integer>();

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(Constants.APP_TAG, "sizeOf appWidgetIds: " + appWidgetIds.length);
		for (int appWidgetId : appWidgetIds) {
			Log.d(Constants.APP_TAG, "onUpdate() for wdg " + appWidgetId);
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

			/* I wish I knew how to put this once and for all and not on each update */
			Intent in = new Intent(context, StatsWidget.class);
			in.setAction(Constants.APPWIDGET_UPDATE);
			in.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent pi = PendingIntent.getBroadcast(context, 0, in, 0);
			rv.setOnClickPendingIntent(R.id.wdg_container, pi);

			if (SysUtils.isRooted() && SysUtils.hasSysfs()) {
				Frequency curFreq = SysUtils.getCurFreq();
				String cur = curFreq != null ? curFreq.toString() : "?";
				String governor = SysUtils.getGovernor();
				if (governor == null) {
					governor = "?";
				}
				String text = cur + "\r\n" + governor;

				rv.setFloat(R.id.wdg_stats_governor, "setTextSize", 10.2f);
				rv.setTextViewText(R.id.wdg_stats_governor, text);

				Stats stats = SysUtils.getFrequencyStats(PreferenceManager.getDefaultSharedPreferences(
						context.getApplicationContext()).getBoolean(Constants.PREF_INCLUDE_DEEP_SLEEP,
						Constants.PREF_DEFAULT_INCLUDE_DEEP_SLEEP));
				if (stats != null) {
					stats.sort(SortMethod.Frequency);

					Paint p = new Paint();
					p.setAntiAlias(true);
					p.setStyle(Style.FILL);
					p.setStrokeWidth(2);

					Bitmap b = Bitmap.createBitmap(stats.getFrequencies().size() * 10, 100, Config.ARGB_8888);
					Canvas canvas = new Canvas(b);
					for (int i = 0; i < stats.getFrequencies().size(); i++) {
						Frequency f = stats.getFrequencies().get(i);
						Double perc = stats.getPercentage(f);
						float prop = (float) i / stats.getFrequencies().size();
						int color = Stats.colorFromPercentage(prop);
						int width = 10;// FISSO!
						int height = (int) (95 * perc);
						p.setColor(color);
						canvas.drawRect(new Rect((width * i), 95 - height, (width * i) + width, 95), p);
						canvas.drawRect(new Rect((width * i), 95, (width * i) + width, 100), p);
					}
					Bitmap bb = Bitmap.createScaledBitmap(b, 72, 72, false);
					rv.setImageViewBitmap(R.id.canvas, bb);
					b.recycle();
				}
			} else {
				Log.d(Constants.APP_TAG, "sys utils not available");
			}
			appWidgetManager.updateAppWidget(appWidgetId, rv);
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			Log.d(Constants.APP_TAG, "called super.onDeleted() for wdg " + appWidgetId);
			Integer k = Integer.valueOf(appWidgetId);
			deletedAppWidgetIds.add(k);
		}
		Log.d(Constants.APP_TAG, "# of deleted widgets: " + deletedAppWidgetIds.size());
		Log.d(Constants.APP_TAG, "deleted widgets: " + deletedAppWidgetIds);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] {
					appWidgetId
				});
			}
		} else if (Constants.APPWIDGET_UPDATE.equals(action)) {
			Toast.makeText(context, R.string.wdg_updating, Toast.LENGTH_SHORT).show();
			int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
			int[] appWidgetIds = {
				appWidgetId
			};
			onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
		} else {
			super.onReceive(context, intent);
		}
	}
}
