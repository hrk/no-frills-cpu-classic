package it.sineo.android.noFrillsCPUClassic.activity;

import it.sineo.android.changelog.ChangelogFactory;
import it.sineo.android.noFrillsCPUClassic.R;
import it.sineo.android.noFrillsCPUClassic.extra.Constants;
import it.sineo.android.noFrillsCPUClassic.extra.Frequency;
import it.sineo.android.noFrillsCPUClassic.extra.SysUtils;
import it.sineo.android.noFrillsCPUClassic.extra.Theme;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button mBtnApply;
	private Button mBtnExit;
	private Spinner mSpnMinFreq;
	private Spinner mSpnMaxFreq;
	private Spinner mSpnGovernor;
	private Spinner mSpnIOScheduler;
	private TextView mTvCurMinFreq;
	private TextView mTvCurMaxFreq;
	private TextView mTvCurGovernor;
	private TextView mTvCurIOScheduler;
	private CheckBox mCkbApplyOnBoot;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Theme.applyTo(this);
		super.onCreate(savedInstanceState);

		if (!SysUtils.isRooted()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dlg_noroot_title);
			builder.setIcon(R.drawable.ic_launcher);
			builder.setMessage(R.string.dlg_noroot_body);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			builder.create().show();
		} else if (!SysUtils.hasSysfs()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dlg_nofs_title);
			builder.setIcon(R.drawable.ic_launcher);
			builder.setMessage(R.string.dlg_nofs_body);
			builder.setCancelable(false);
			builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			builder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					prepareAndSendEmail();
					finish();
				}
			});
			builder.create().show();
		} else {
			setContentView(R.layout.main);
			/*
			 * Get a reference to GUI elements.
			 */
			mSpnMinFreq = (Spinner) findViewById(R.id.sp_min_freq);
			mSpnMaxFreq = (Spinner) findViewById(R.id.sp_max_freq);
			mSpnGovernor = (Spinner) findViewById(R.id.sp_governor);
			mSpnIOScheduler = (Spinner) findViewById(R.id.sp_ioscheduler);
			mTvCurMinFreq = (TextView) findViewById(R.id.tv_cur_min_freq);
			mTvCurMaxFreq = (TextView) findViewById(R.id.tv_cur_max_freq);
			mTvCurGovernor = (TextView) findViewById(R.id.tv_cur_governor);
			mTvCurIOScheduler = (TextView) findViewById(R.id.tv_cur_ioscheduler);
			mCkbApplyOnBoot = (CheckBox) findViewById(R.id.ckb_apply_on_boot);
			mBtnApply = (Button) findViewById(R.id.btn_apply);
			mBtnExit = (Button) findViewById(R.id.btn_exit);

			/*
			 * Set event handlers
			 */
			mBtnExit.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});

			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			mCkbApplyOnBoot.setChecked(prefs.getBoolean(Constants.PREF_APPLY_ON_BOOT, false));

			mCkbApplyOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean(Constants.PREF_APPLY_ON_BOOT, isChecked);
					editor.commit();
				}
			});

			/*
			 * Read currest situation and populate GUI.
			 */
			String[] frequencies = SysUtils.getAvailableFrequencies();
			Frequency min_frequency = SysUtils.getMinFreq();
			Frequency max_frequency = SysUtils.getMaxFreq();
			if (frequencies != null && min_frequency != null && max_frequency != null) {
				mTvCurMinFreq.setText(min_frequency.toString());
				mTvCurMaxFreq.setText(max_frequency.toString());

				Frequency[] freqs = new Frequency[frequencies.length];
				for (int i = 0; i < frequencies.length; i++) {
					freqs[i] = new Frequency(frequencies[i]);
				}

				Frequency requested_min_freq = new Frequency(prefs.getString(Constants.PREF_MIN_FREQ, min_frequency.getValue()));
				Frequency requested_max_freq = new Frequency(prefs.getString(Constants.PREF_MAX_FREQ, max_frequency.getValue()));

				ArrayAdapter<Frequency> minFreqAdapter = new ArrayAdapter<Frequency>(this,
						android.R.layout.simple_spinner_item, freqs);
				minFreqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mSpnMinFreq.setAdapter(minFreqAdapter);
				mSpnMinFreq.setSelection(minFreqAdapter.getPosition(requested_min_freq));

				ArrayAdapter<Frequency> maxFreqAdapter = new ArrayAdapter<Frequency>(this,
						android.R.layout.simple_spinner_item, freqs);
				maxFreqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mSpnMaxFreq.setAdapter(maxFreqAdapter);
				mSpnMaxFreq.setSelection(maxFreqAdapter.getPosition(requested_max_freq));
			} else {
				/*
				 * No available frequencies.
				 */
				mTvCurMinFreq.setText(R.string.unavailable);
				mTvCurMaxFreq.setText(R.string.unavailable);

				mSpnMinFreq.setEnabled(false);
				mSpnMaxFreq.setEnabled(false);
			}

			/*
			 * Governor
			 */
			String governor = SysUtils.getGovernor();
			String[] governors = SysUtils.getAvailableGovernors();
			if (governor != null && governors != null) {
				mTvCurGovernor.setText(governor);

				String requested_governor = prefs.getString(Constants.PREF_GOVERNOR, governor);

				ArrayAdapter<String> governorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
						governors);
				governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mSpnGovernor.setAdapter(governorAdapter);
				mSpnGovernor.setSelection(governorAdapter.getPosition(requested_governor));
			} else {
				mTvCurGovernor.setText(R.string.unavailable);
				mSpnGovernor.setEnabled(false);
			}

			/*
			 * I/O Scheduler
			 */
			String ioscheduler = SysUtils.getIOScheduler();
			String[] ioschedulers = SysUtils.getAvailableIOSchedulers();
			if (ioscheduler != null && ioschedulers != null) {
				mTvCurIOScheduler.setText(ioscheduler);

				String requested_ioscheduler = prefs.getString(Constants.PREF_IOSCHEDULER, ioscheduler);

				ArrayAdapter<String> ioschedulerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
						ioschedulers);
				ioschedulerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mSpnIOScheduler.setAdapter(ioschedulerAdapter);
				mSpnIOScheduler.setSelection(ioschedulerAdapter.getPosition(requested_ioscheduler));
			} else {
				mTvCurIOScheduler.setText(R.string.unavailable);
				mSpnIOScheduler.setEnabled(false);
			}

			mBtnApply.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					v.post(new Runnable() {
						public void run() {
							String min_freq = null;
							String max_freq = null;
							String governor = null;
							String ioscheduler = null;

							if (mSpnMinFreq.isEnabled()) {
								Frequency aux = (Frequency) mSpnMinFreq.getSelectedItem();
								if (aux != null) {
									min_freq = aux.getValue();
								}
							}

							if (mSpnMaxFreq.isEnabled()) {
								Frequency aux = (Frequency) mSpnMaxFreq.getSelectedItem();
								if (aux != null) {
									max_freq = aux.getValue();
								}
							}

							if (mSpnGovernor.isEnabled()) {
								Object aux = mSpnGovernor.getSelectedItem();
								if (aux != null) {
									governor = aux.toString();
								}
							}

							if (mSpnIOScheduler.isEnabled()) {
								ioscheduler = mSpnIOScheduler.getSelectedItem().toString();
							}

							/*
							 * First thing to do: set dirty flag.
							 */
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
							SharedPreferences.Editor editor = prefs.edit();
							editor.putBoolean(Constants.CHECK_SHUTDOWN_OK, false);
							editor.putLong(Constants.LAST_UPTIME, SystemClock.uptimeMillis());
							editor.commit();

							boolean changePermissions = prefs.getBoolean(Constants.PREF_CHANGE_PERMISSIONS,
									Constants.PREF_DEFAULT_CHANGE_PERMISSIONS);

							SysUtils.setFrequenciesAndGovernor(min_freq, max_freq, governor, ioscheduler, changePermissions,
									MainActivity.this, R.string.ok_updated_freqs, R.string.err_update_failed);
							if (Constants.DEBUG) {
								if (prefs.getBoolean(Constants.PREF_NOTIFY_ON_BOOT, Constants.PREF_DEFAULT_NOTIFY_ON_BOOT)) {
									String sound = prefs.getString(Constants.PREF_RINGTONE, Constants.PREF_DEFAULT_RINGTONE);
									String pattern = null;
									if (prefs.getBoolean(Constants.PREF_VIBRATE, Constants.PREF_DEFAULT_VIBRATE)) {
										pattern = prefs.getString(Constants.PREF_VIBRATE_PATTERN, "");
									}
									Log.d(Constants.APP_TAG, "sound: " + sound + ", pattern: " + pattern);
									SysUtils.showNotification(
											MainActivity.this,
											getString(R.string.notify_title_success),
											getString(R.string.notify_success, SysUtils.getMinFreq(), SysUtils.getMaxFreq(),
													SysUtils.getGovernor(), SysUtils.getIOScheduler()), sound, pattern);
								} else {
									Log.d(Constants.APP_TAG, "notify_on_boot is FALSE");
								}
							}
							/*
							 * Save requested values:
							 */
							editor.putString(Constants.PREF_MIN_FREQ, min_freq);
							editor.putString(Constants.PREF_MAX_FREQ, max_freq);
							editor.putString(Constants.PREF_GOVERNOR, governor);
							editor.putString(Constants.PREF_IOSCHEDULER, ioscheduler);
							editor.commit();

							finish();
						}
					});
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (SysUtils.hasSysfs() && SysUtils.isRooted()) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.main, menu);
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_send_info: {
				prepareAndSendEmail();
				return true;
			}
			case R.id.menu_view_stats: {
				Intent statsActivity = new Intent(this, StatsActivity.class);
				startActivity(statsActivity);
				return true;
			}
			case R.id.menu_details: {
				displayAboutDialog();
				return true;
			}
			case R.id.menu_prefs: {
				Intent prefsActivity = new Intent(this, PreferencesActivity.class);
				startActivity(prefsActivity);
				return true;
			}
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void prepareAndSendEmail() {
		String[] email = new String[] {
			Constants.DEVELOPER_MAIL
		};
		String body = SysUtils.discoverCPUData();
		String version = "";
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = info.versionName;
		} catch (PackageManager.NameNotFoundException pmnnfex) {
			pmnnfex.printStackTrace();
		}
		/*
		 * Hard-coded because it's sent to me.
		 */
		String subject = "No-frills CPU Data [" + version + "]";

		Intent share = new Intent(android.content.Intent.ACTION_SEND);
		share.setType(/* "text/plain" */"message/rfc822");
		share.putExtra(Intent.EXTRA_EMAIL, email);
		share.putExtra(Intent.EXTRA_SUBJECT, subject);
		share.putExtra(Intent.EXTRA_TEXT, body);
		// Toast.makeText(this, body, Toast.LENGTH_LONG).show();
		try {
			startActivity(Intent.createChooser(share, getString(R.string.dlg_send_title)));
		} catch (ActivityNotFoundException anfex) {
			/*
			 * No activity is able to handle message/rfc822 type.
			 */
			Toast.makeText(this, R.string.err_no_email_client, Toast.LENGTH_LONG).show();
		}
	}

	protected void displayChangelogIfNeeded() {
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			int versionCode = packageInfo.versionCode;

			SharedPreferences settings = getSharedPreferences(getPackageName(), MODE_PRIVATE);
			int viewedChangelogVersion = settings.getInt(Constants.LAST_CHANGELOG_VERSION_VIEWED, 0);

			if (viewedChangelogVersion < versionCode) {
				Editor editor = settings.edit();
				editor.putInt(Constants.LAST_CHANGELOG_VERSION_VIEWED, versionCode);
				editor.commit();
				displayChangeLog();
			}
		} catch (NameNotFoundException e) {
			Log.w(Constants.APP_TAG, "Unable to get version code. Will not show changelog", e);
		}
	}

	protected void displayChangeLog() {
		ChangelogFactory.buildAlertDialog(this, R.xml.changelog, R.drawable.ic_launcher).show();
	}

	protected void displayAboutDialog() {
		startActivity(new Intent(this, AboutActivity.class));
	}
}
