package it.sineo.android.noFrillsCPUClassic.activity;

import it.sineo.android.noFrillsCPUClassic.R;
import it.sineo.android.noFrillsCPUClassic.extra.Constants;
import it.sineo.android.noFrillsCPUClassic.extra.Frequency;
import it.sineo.android.noFrillsCPUClassic.extra.Stats;
import it.sineo.android.noFrillsCPUClassic.extra.Theme;
import it.sineo.android.noFrillsCPUClassic.extra.Stats.SortMethod;
import it.sineo.android.noFrillsCPUClassic.extra.SysUtils;

import java.text.NumberFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatsActivity extends ListActivity {

	protected Theme theme;

	protected NumberFormat nf;
	protected ProgressDialog progressDialog;
	protected StatsAdapter adapter = new StatsAdapter();
	protected SharedPreferences preferences;
	//
	protected ImageView refreshButton;
	protected TextView tvCurrentFrequency;
	//
	protected TextView tvHeaderFrequency;
	protected TextView tvHeaderPerc;
	protected TextView tvHeaderPerc5;
	protected SortMethod sortMethod = SortMethod.Frequency;
	/**
	 * Set through interface, kept as a reference for totals.
	 */
	protected Stats zero;

	protected ScheduledExecutorService scheduleTaskExecutor;

	protected View.OnTouchListener sortListener = new View.OnTouchListener() {
		private long lastEvent = 0L;
		private final long MIN_DELAY = 500;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (System.currentTimeMillis() > lastEvent + MIN_DELAY) {
				lastEvent = System.currentTimeMillis();
				if (v == tvHeaderFrequency) {
					/* Default for changing sort method is ascending order */
					if (sortMethod != SortMethod.Frequency) {
						sortMethod = SortMethod.Frequency;
					} else {
						sortMethod = SortMethod.FrequencyDesc;
					}
				} else if (v == tvHeaderPerc) {
					if (sortMethod != SortMethod.Percentage) {
						sortMethod = SortMethod.Percentage;
					} else {
						sortMethod = SortMethod.PercentageDesc;
					}
				} else if (v == tvHeaderPerc5) {
					if (sortMethod != SortMethod.PartialPercentage) {
						sortMethod = SortMethod.PartialPercentage;
					} else {
						sortMethod = SortMethod.PartialPercentageDesc;
					}
				}
				updateSortingHeader();

				Editor editor = preferences.edit();
				editor.putString(Constants.PREF_SORT_METHOD, sortMethod.name());
				editor.commit();

				if (adapter.stats != null) {
					adapter.stats.sort(sortMethod);
				}
				adapter.notifyDataSetChanged();
				return true;
			}
			return false;
		}
	};

	protected Runnable statsRunnable = new Runnable() {
		public void run() {
			if (adapter.stats != null) {
				adapter.stats.clear();
			}
			boolean withDeepSleep = preferences.getBoolean(Constants.PREF_INCLUDE_DEEP_SLEEP,
					Constants.PREF_DEFAULT_INCLUDE_DEEP_SLEEP);
			Stats previousStats = SysUtils.getFrequencyStats(withDeepSleep);
			// No need to sort these.
			try {
				Thread.sleep(5000);
			} catch (InterruptedException iex) {
				iex.printStackTrace();
			}
			adapter.stats = SysUtils.getFrequencyStats(withDeepSleep);
			if (adapter.stats != null) {
				if (zero != null) {
					adapter.stats.setZero(zero);
				}
				adapter.stats.setPreviousStats(previousStats);
				adapter.stats.sort(sortMethod);
			}

			Frequency curFreq = SysUtils.getCurFreq();
			String s = curFreq != null ? curFreq.toString() : getString(R.string.stats_current_unavailable);
			final String text = getString(R.string.stats_current, s);

			runOnUiThread(new Runnable() {
				public void run() {
					if (progressDialog != null) {
						adapter.notifyDataSetChanged();
						progressDialog.dismiss();
						//
						tvCurrentFrequency.setText(text);
					}
				}
			});
		}
	};

	protected Runnable updateCurrentFrequencyRunnable = new Runnable() {
		@Override
		public void run() {
			Frequency curFreq = SysUtils.getCurFreq();
			String s = curFreq != null ? curFreq.toString() : getString(R.string.stats_current_unavailable);
			final String text = getString(R.string.stats_current, s);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tvCurrentFrequency.setText(text);
				}
			});
		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		theme = Theme.applyTo(this);
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
		setListAdapter(adapter);

		tvCurrentFrequency = (TextView) findViewById(R.id.tv_stats_current);
		Frequency curFreq = SysUtils.getCurFreq();
		String s = curFreq != null ? curFreq.toString() : getString(R.string.stats_current_unavailable);
		tvCurrentFrequency.setText(getString(R.string.stats_current, s));

		refreshButton = (ImageView) findViewById(R.id.btn_refresh_stats);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			/* Since there's the action bar, hide the now redundant refresh button. */
			refreshButton.setVisibility(View.GONE);
		} else {
			if (theme.equals(Theme.THEME_LIGHT)) {
				refreshButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_refresh_inverted));
			}
			refreshButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					progressDialog.show();
					(new Thread(statsRunnable)).start();
				}
			});
		}

		if (preferences.getBoolean(Constants.PREF_UPDATE_CURFREQ, Constants.PREF_DEFAULT_UPDATE_CURFREQ)) {
			/* Use a single thread to update every N seconds the current frequency */
			scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
			scheduleTaskExecutor.scheduleAtFixedRate(updateCurrentFrequencyRunnable, 2, 2, TimeUnit.SECONDS);
		}

		/* (Sort-) Header */
		LinearLayout grpHeader = (LinearLayout) findViewById(R.id.grp_stats_header);
		tvHeaderFrequency = (TextView) grpHeader.findViewById(R.id.tv_stats_row_frequency);
		tvHeaderPerc = (TextView) grpHeader.findViewById(R.id.tv_stats_row_perc);
		tvHeaderPerc5 = (TextView) grpHeader.findViewById(R.id.tv_stats_row_perc5);
		tvHeaderFrequency.setOnTouchListener(sortListener);
		tvHeaderPerc.setOnTouchListener(sortListener);
		tvHeaderPerc5.setOnTouchListener(sortListener);
		/* Defult sorting method */
		sortMethod = SortMethod.valueOf(preferences.getString(Constants.PREF_SORT_METHOD,
				Constants.PREF_DEFAULT_SORT_METHOD));
		updateSortingHeader();

		nf = NumberFormat.getPercentInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);

		progressDialog = ProgressDialog.show(this, null, getString(R.string.spinner_stats_text), true);

		(new Thread(statsRunnable)).start();
	};

	@Override
	protected void onResume() {
		super.onResume();
		if (preferences.contains(Constants.STATS_ZERO_POINT)) {
			zero = Stats.fromPersistedString(preferences.getString(Constants.STATS_ZERO_POINT, null));
		}
	}

	private void updateSortingHeader() {
		/* Clear compound drawables for every view */
		tvHeaderFrequency.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		tvHeaderPerc.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		tvHeaderPerc5.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		switch (sortMethod) {
			case Frequency:
				tvHeaderFrequency.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sort_asc, 0, 0, 0);
				break;
			case FrequencyDesc:
				tvHeaderFrequency.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sort_desc, 0, 0, 0);
				break;
			case Percentage:
				tvHeaderPerc.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sort_asc, 0);
				break;
			case PercentageDesc:
				tvHeaderPerc.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sort_desc, 0);
				break;
			case PartialPercentage:
				tvHeaderPerc5.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sort_asc, 0);
				break;
			case PartialPercentageDesc:
				tvHeaderPerc5.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_sort_desc, 0);
				break;
		}
	}

	// private Handler handler = new Handler() {
	// public void handleMessage(android.os.Message msg) {
	// if (progressDialog != null) {
	// adapter.notifyDataSetChanged();
	// progressDialog.dismiss();
	// //
	// Frequency curFreq = SysUtils.getCurFreq();
	// String s = curFreq != null ? curFreq.toString() :
	// getString(R.string.stats_current_unavailable);
	// String s2 = getString(R.string.stats_current, s);
	// tvCurrentFrequency.setText(s2);
	// }
	// };
	// };

	private class StatsAdapter extends BaseAdapter {
		LayoutInflater li = null;
		Paint p = null;

		private final class ViewHolder {
			TextView tvFrequency;
			TextView tvPerc;
			TextView tvPerc5;
		}

		protected Stats stats;

		public int getCount() {
			if (stats != null && stats.getFrequencies() != null) {
				return stats.getFrequencies().size();
			}
			return 0;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				if (li == null) {
					li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				}
				if (p == null) {
					p = new Paint();
					p.setAntiAlias(true);
					p.setStyle(Style.FILL);
					p.setStrokeWidth(2);
				}
				convertView = li.inflate(R.layout.stats_row, null);
			}
			ViewHolder holder = (ViewHolder) convertView.getTag();
			if (holder == null) {
				holder = new ViewHolder();
				holder.tvFrequency = (TextView) convertView.findViewById(R.id.tv_stats_row_frequency);
				holder.tvPerc = (TextView) convertView.findViewById(R.id.tv_stats_row_perc);
				holder.tvPerc5 = (TextView) convertView.findViewById(R.id.tv_stats_row_perc5);
				convertView.setTag(holder);
			}

			if (stats != null && stats.getFrequencies() != null) {
				Frequency freq = stats.getFrequencies().get(position);
				if (freq != null) {
					if ("-1".equals(freq.getValue())) {
						holder.tvFrequency.setText(getString(R.string.stats_deep_sleep));
					} else {
						holder.tvFrequency.setText(freq.toString());
					}
					holder.tvPerc.setText(nf.format(stats.getPercentage(freq)));
					holder.tvPerc5.setText(nf.format(stats.getPartialPercentage(freq)));
					/* Chart */
					p.setColor(Stats.colorFromPercentage((float) (stats.getFrequencyIndex(freq)) / stats.getFrequencies().size()));

					Bitmap bmp = Bitmap.createBitmap(101, 5, Config.ARGB_8888);
					Canvas c = new Canvas(bmp);
					c.drawRect((int) (100 * (1 - stats.getPercentage(freq))), 4, 101, 5, p);
					__wraper_setBackground(holder.tvPerc, new BitmapDrawable(getResources(), bmp));

					bmp = Bitmap.createBitmap(101, 5, Config.ARGB_8888);
					c = new Canvas(bmp);
					c.drawRect((int) (100 * (1 - stats.getPartialPercentage(freq))), 4, 101, 5, p);
					__wraper_setBackground(holder.tvPerc5, new BitmapDrawable(getResources(), bmp));
				}
			}
			return convertView;
		}
	}

	@SuppressWarnings("deprecation")
	@TargetApi(16)
	private void __wraper_setBackground(View v, Drawable d) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			v.setBackground(d);
		} else {
			v.setBackgroundDrawable(d);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (scheduleTaskExecutor != null) {
			scheduleTaskExecutor.shutdown();
			scheduleTaskExecutor = null;
		}

		preferences = null;
		/*
		 * Proper handling of progressDialog
		 */
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
		refreshButton = null;
		tvCurrentFrequency = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.stats, menu);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			menu.findItem(R.id.menu_refresh).setIcon(R.drawable.ic_menu_refresh);
			menu.findItem(R.id.menu_reset).setIcon(R.drawable.ic_menu_reset);
		} else if (theme.equals(Theme.THEME_LIGHT)) {
			menu.findItem(R.id.menu_refresh).setIcon(R.drawable.ic_action_refresh_inverted);
			menu.findItem(R.id.menu_reset).setIcon(R.drawable.ic_action_reset_inverted);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh: {
				progressDialog.show();
				(new Thread(statsRunnable)).start();
				return true;
			}
			case R.id.menu_reset: {
				progressDialog.show();
				boolean withDeepSleep = preferences.getBoolean(Constants.PREF_INCLUDE_DEEP_SLEEP,
						Constants.PREF_DEFAULT_INCLUDE_DEEP_SLEEP);
				zero = SysUtils.getFrequencyStats(withDeepSleep);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(Constants.STATS_ZERO_POINT, zero.toPersistableString());
				editor.commit();
				(new Thread(statsRunnable)).start();
				return true;
			}
			default: {
				return super.onOptionsItemSelected(item);
			}
		}
	}
}
