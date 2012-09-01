package it.sineo.android.noFrillsCPUClassic.activity;

import it.sineo.android.changelog.ChangelogFactory;
import it.sineo.android.noFrillsCPUClassic.R;
import it.sineo.android.noFrillsCPUClassic.extra.Constants;
import it.sineo.android.noFrillsCPUClassic.extra.Theme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class AboutActivity extends Activity implements TabContentFactory {

	private final static String TAG_INFO = "info";
	private final static String TAG_CHANGELOG = "changelog";
	private final static String TAG_FAQ = "faq";
	private final static String TAG_LICENSE = "license";

	private final static String SELECTED_TAB_INDEX = "selected_tab_index";

	private TabHost mTabHost;

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mTabHost.setCurrentTab(savedInstanceState.getInt(SELECTED_TAB_INDEX, 0));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_TAB_INDEX, mTabHost.getCurrentTab());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Theme.applyTo(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		TabSpec tab = mTabHost.newTabSpec(TAG_INFO);
		tab.setIndicator(getString(R.string.tab_info));
		tab.setContent(this);
		mTabHost.addTab(tab);

		tab = mTabHost.newTabSpec(TAG_CHANGELOG);
		tab.setIndicator(getString(R.string.tab_changelog));
		tab.setContent(this);
		mTabHost.addTab(tab);

		tab = mTabHost.newTabSpec(TAG_FAQ);
		tab.setIndicator(getString(R.string.tab_faq));
		tab.setContent(this);
		mTabHost.addTab(tab);

		tab = mTabHost.newTabSpec(TAG_LICENSE);
		tab.setIndicator(getString(R.string.tab_license));
		tab.setContent(this);
		mTabHost.addTab(tab);
	}

	private View tabInfo, tabChangelog, tabFaq, tabLicense;

	public View createTabContent(String tag) {
		View v = null;
		long t0 = System.currentTimeMillis();
		if (TAG_INFO.equals(tag)) {
			if (tabInfo == null) {
				LayoutInflater li = LayoutInflater.from(this);
				tabInfo = li.inflate(R.layout.about_info, null);

				String version = "?";
				try {
					PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
					version = info.versionName;
				} catch (PackageManager.NameNotFoundException pmnnfex) {
					pmnnfex.printStackTrace();
				}
				/* Parametric views */
				final Resources r = getResources();
				final TextView tvVersion = (TextView) tabInfo.findViewById(R.id.about_info_version);
				tvVersion.setText(r.getString(R.string.about_info_version, version));

				final TextView tvAuthor = (TextView) tabInfo.findViewById(R.id.about_info_author);
				tvAuthor.setText(r.getString(R.string.about_info_author, r.getString(R.string.author_name)));

				final TextView tvHomepage = (TextView) tabInfo.findViewById(R.id.about_info_homepage);
				tvHomepage.setText(r.getString(R.string.about_info_homepage, r.getString(R.string.homepage_url)));

				final TextView tvQuestions = (TextView) tabInfo.findViewById(R.id.about_info_questions);
				tvQuestions.setText(r.getString(R.string.about_info_questions, r.getString(R.string.author_email)));

				/* Donate button */
				ImageView ivDonate = (ImageView) tabInfo.findViewById(R.id.donate_button);
				ivDonate.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.paypal_donate_url)));
						startActivity(i);
					}
				});
				return tabInfo;
			}
			v = tabInfo;
		} else if (TAG_CHANGELOG.equals(tag)) {
			if (tabChangelog == null) {
				tabChangelog = ChangelogFactory.inflate(this, R.xml.changelog);
				;
			}
			v = tabChangelog;
		} else if (TAG_FAQ.equals(tag)) {
			if (tabFaq == null) {
				LayoutInflater li = LayoutInflater.from(this);
				tabFaq = li.inflate(R.layout.about_faq, null);
			}
			v = tabFaq;
		} else if (TAG_LICENSE.equals(tag)) {
			if (tabLicense == null) {
				String text = "";
				try {
					InputStream is = getResources().openRawResource(R.raw.license);
					BufferedReader br = new BufferedReader(new InputStreamReader(is), 4096);
					String line = null;
					while ((line = br.readLine()) != null) {
						text += line + "\r\n";
					}
					br.close();
					is.close();
				} catch (IOException ioex) {
					text = ioex.getLocalizedMessage();
				}
				LayoutInflater li = LayoutInflater.from(this);
				tabLicense = li.inflate(R.layout.about_license, null);
				((TextView) tabLicense.findViewById(R.id.about_license)).setText(text);
			}
			v = tabLicense;
		} else {
			Log.e(getClass().getName(), "unknown tag: " + tag);
		}
		long t1 = System.currentTimeMillis();
		Log.d(Constants.APP_TAG, "delta: " + (t1 - t0) + " ms");
		return v;
	}

}
