package cz.jabbim.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class JabbimServiceStarter extends BroadcastReceiver {

	static final String TAG = "JabbimServiceStarter";

	@Override
	public void onReceive(Context context, Intent intent) {
		// Received intent only when the system boot is completed
		Log.d(TAG, "onReceiveIntent");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("prefAutoStartKey", true)) {
			Log.d(TAG, "Trying to auto start.");
			Intent sint = new Intent();
			sint.setAction("cz.jabbim.android.service.JabbimConnectionService");
			context.startService(sint);
			// Intent conServ = new Intent(context, JabbimConnectionService.class);
			// context.startService(conServ);
		}
	}
}
