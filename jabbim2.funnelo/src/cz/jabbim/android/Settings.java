package cz.jabbim.android;

import cz.jabbim.android.R;
import cz.jabbim.android.data.JabberoidDbConnector;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Settings extends PreferenceActivity {

	private static final String TAG = "JabbimSettings";

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.prefmenu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return doSelectedItem(item) || super.onOptionsItemSelected(item);
	}

	private boolean doSelectedItem(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.menuPrefReset):// OPEN PREFERENCES):
			final Context context = getBaseContext();
			final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			final SharedPreferences.Editor preferencesEditor = preferences.edit();
			preferencesEditor.clear();
			PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
			preferencesEditor.commit();
			(new JabberoidDbConnector(context)).dropAllTables();
			Log.i(TAG, "Reset complete!");
			getPreferenceScreen().removeAll();
			addPreferencesFromResource(R.xml.preferences);
			return true;

		}
		return false;
	}
}
