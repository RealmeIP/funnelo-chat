/*
 * BEEM is a videoconference application on the Android Platform.
 * Copyright (C) 2009 by Frederic-Charles Barthelery,
 * Jean-Manuel Da Silva,
 * Nikita Kozlov,
 * Philippe Lago,
 * Jean Baptiste Vergely,
 * Vincent Veronis.
 * This file is part of BEEM.
 * BEEM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * BEEM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with BEEM. If not, see <http://www.gnu.org/licenses/>.
 * Please send bug reports with examples or suggestions to
 * contact@beem-project.com or http://dev.beem-project.com/
 * Epitech, hereby disclaims all copyright interest in the program "Beem"
 * written by Frederic-Charles Barthelery,
 * Jean-Manuel Da Silva,
 * Nikita Kozlov,
 * Philippe Lago,
 * Jean Baptiste Vergely,
 * Vincent Veronis.
 * Nicolas Sadirac, November 26, 2009
 * President of Epitech.
 * Flavien Astraud, November 26, 2009
 * Head of the EIP Laboratory.
 */
package com.beem.project.beem.ui;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.beem.project.beem.BeemApplication;
import com.beem.project.beem.BeemService;
import com.beem.project.beem.R;

/**
 * This class represents an activity which allows the user to create an account
 * on the XMPP server saved in settings.
 *
 * @author Jean-Manuel Da Silva <dasilvj at beem-project dot com>
 */
public class CreateAccount extends Activity {

	private static final boolean DEFAULT_BOOLEAN_VALUE = false;
	private static final String DEFAULT_STRING_VALUE = "";

	private static final int NOTIFICATION_DURATION = Toast.LENGTH_SHORT;

	private SharedPreferences mSettings;
	private Button mCreateAccountButton;

	/**
	 * Constructor.
	 */
	public CreateAccount() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_account);
		initCreateAccountButton();
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
	}

	/**
	 * Create an account on the XMPP server specified in settings.
	 *
	 * @param username
	 *            the username of the account.
	 * @param password
	 *            the password of the account.
	 * @return true if the account was created successfully.
	 */
	private boolean createAccount(String username, String password) {
		XMPPConnection xmppConnection = null;
		ConnectionConfiguration connectionConfiguration = null;
		ProxyInfo pi = getRegisteredProxy();
		String server = getXMPPServer();
		int port = getXMPPPort();
		if (pi != null) {
			connectionConfiguration = new ConnectionConfiguration(server, port, pi);
		} else {
			connectionConfiguration = new ConnectionConfiguration(server, port);
		}
		connectionConfiguration.setServiceName(BeemService.DEFAULT_XMPP_SERVICE);
		if (getRegisteredXMPPTLSUse())
			connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		xmppConnection = new XMPPConnection(connectionConfiguration);
		Log.i("createAccount", "Connecting to '" + server + ":" + port + "'...");
		try {
			xmppConnection.connect();
			for (int i = 0; i < 5 && !xmppConnection.isConnected(); i++)
				Thread.sleep(1000);
			AccountManager accountManager = new AccountManager(xmppConnection);
			Log.i("createAccount", "Creating new account: '" + username + "'...");
			accountManager.createAccount(username, password);
			Toast toast = Toast.makeText(getApplicationContext(), String.format(
					getString(R.string.create_account_successfull_after), username), NOTIFICATION_DURATION);
			toast.show();
		} catch (Exception e) {
			Log.w("createAccount", e.getMessage());
			createErrorDialog(e.getMessage());
			return false;
		}
		xmppConnection.disconnect();
		return true;
	}

	/**
	 * Create a dialog containing an error message.
	 *
	 * @param errMsg
	 *            the error message
	 */
	private void createErrorDialog(String errMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.create_account_err_dialog_title).setMessage(errMsg).setCancelable(false).setIcon(
				android.R.drawable.ic_dialog_alert);
		builder.setNeutralButton(R.string.create_account_close_dialog_button, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		AlertDialog settingsErrDialog = builder.create();
		settingsErrDialog.show();
	}

	/**
	 * Retrive proxy informations from the preferences.
	 *
	 * @return Registered proxy informations
	 */
	private ProxyInfo getRegisteredProxy() {
		if (getRegisteredProxyUse()) {
			ProxyInfo proxyInfo = new ProxyInfo(getRegisteredProxyType(), getRegisteredProxyServer(),
					getRegisteredProxyPort(), getRegisteredProxyUsername(), getRegisteredProxyPassword());
			return proxyInfo;
		}
		return null;
	}

	/**
	 * Retrieve proxy password from the preferences.
	 *
	 * @return Registered proxy password
	 */
	private String getRegisteredProxyPassword() {
		return mSettings.getString(BeemApplication.PROXY_PASSWORD_KEY, DEFAULT_STRING_VALUE);
	}

	/**
	 * Retrieve proxy port from the preferences.
	 *
	 * @return Registered proxy port
	 */
	private int getRegisteredProxyPort() {
		return Integer.parseInt(mSettings.getString(BeemApplication.PROXY_PORT_KEY, DEFAULT_STRING_VALUE));
	}

	/**
	 * Retrieve proxy server from the preferences.
	 *
	 * @return Registered proxy server
	 */
	private String getRegisteredProxyServer() {
		return mSettings.getString(BeemApplication.PROXY_SERVER_KEY, DEFAULT_STRING_VALUE);
	}

	/**
	 * Retrieve proxy type from the preferences.
	 *
	 * @return Registered proxy type
	 */
	private ProxyInfo.ProxyType getRegisteredProxyType() {
		ProxyInfo.ProxyType result = ProxyInfo.ProxyType.NONE;
		if (mSettings.getBoolean(BeemApplication.PROXY_USE_KEY, false)) {
			String type = mSettings.getString(BeemApplication.PROXY_TYPE_KEY, "none");
			if ("HTTP".equals(type))
				result = ProxyInfo.ProxyType.HTTP;
			else if ("SOCKS4".equals(type))
				result = ProxyInfo.ProxyType.SOCKS4;
			else if ("SOCKS5".equals(type))
				result = ProxyInfo.ProxyType.SOCKS5;
			else
				result = ProxyInfo.ProxyType.NONE;
		}
		return result;
	}

	/**
	 * Retrieve proxy use from the preferences.
	 *
	 * @return Registered proxy use
	 */
	private boolean getRegisteredProxyUse() {
		return mSettings.getBoolean(BeemApplication.PROXY_USE_KEY, DEFAULT_BOOLEAN_VALUE);
	}

	/**
	 * Retrieve proxy username from the preferences.
	 *
	 * @return Registered proxy username
	 */
	private String getRegisteredProxyUsername() {
		return mSettings.getString(BeemApplication.PROXY_USERNAME_KEY, DEFAULT_STRING_VALUE);
	}

	/**
	 * Retrieve xmpp port from the preferences.
	 *
	 * @return Registered xmpp port
	 */
	private int getXMPPPort() {
		return BeemService.DEFAULT_XMPP_PORT;
	}

	/**
	 * Retrieve xmpp server from the preferences.
	 *
	 * @return Registered xmpp server
	 */
	private String getXMPPServer() {
		return BeemService.DEFAULT_XMPP_SERVER;
	}

	/**
	 * Retrieve TLS use from the preferences.
	 *
	 * @return Registered TLS use
	 */
	private boolean getRegisteredXMPPTLSUse() {
		return mSettings.getBoolean("settings_key_xmpp_tls_use", DEFAULT_BOOLEAN_VALUE);
	}

	/**
	 * Check if the fields password and confirm password match.
	 *
	 * @return return true if password & confirm password fields match, else
	 *         false
	 */
	private boolean checkPasswords() {
		final String passwordFieldValue = ((EditText) findViewById(R.id.create_account_password)).getText().toString();
		final String passwordConfirmFielddValue = ((EditText) findViewById(R.id.create_account_confirm_password))
				.getText().toString();

		return passwordFieldValue.equals(passwordConfirmFielddValue) && !"".equals(passwordConfirmFielddValue);
	}

	/**
	 * Check the format of the email.
	 *
	 * @return true if the email is valid.
	 */
	private boolean checkEmail() {
		TextView tView = (TextView) findViewById(R.id.create_account_username);
		String email = tView.getText().toString();
		if (email.indexOf("@") < 0)
			return true;
		String service = StringUtils.parseServer(email);
		if (TextUtils.isEmpty(service))
			return true;
		if (service.equals(BeemService.DEFAULT_XMPP_SERVICE))
			return true;
		return false;
		// return
		// Pattern.matches("[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4}",
		// email);
	}

	/**
	 * Initialize the "Create this account" button which allows the user to
	 * create an account.
	 */
	private void initCreateAccountButton() {
		mCreateAccountButton = (Button) findViewById(R.id.create_account_button);
		mCreateAccountButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				String usernameFieldValue = ((EditText) findViewById(R.id.create_account_username)).getText()
						.toString();
				String passwordFieldValue = ((EditText) findViewById(R.id.create_account_password)).getText()
						.toString();
				if (!checkEmail())
					createErrorDialog(getString(R.string.create_account_err_username));
				else if (!checkPasswords())
					createErrorDialog(getString(R.string.create_account_err_passwords));
				else {
					if (usernameFieldValue.indexOf("@") < 0) // this will trick
						// empty
						// StringUtils.parseName() anomaly problem
						usernameFieldValue += "@";
					String username = StringUtils.parseName(usernameFieldValue);
					if (createAccount(username, passwordFieldValue))
						finish();
				}

			}
		});
		Button createAccountLoginButton = (Button) findViewById(R.id.create_account_login_button);
		createAccountLoginButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				String usernameFieldValue = ((EditText) findViewById(R.id.create_account_username)).getText()
						.toString();
				String passwordFieldValue = ((EditText) findViewById(R.id.create_account_password)).getText()
						.toString();
				if (!checkEmail())
					createErrorDialog(getString(R.string.create_account_err_username));
				else if (!checkPasswords())
					createErrorDialog(getString(R.string.create_account_err_passwords));
				else {
					if (usernameFieldValue.indexOf("@") < 0) // this will trick
						// empty
						// StringUtils.parseName() anomaly problem
						usernameFieldValue += "@";
					String username = StringUtils.parseName(usernameFieldValue);
					if (createAccount(username, passwordFieldValue)) {
						SharedPreferences.Editor settingsEditor = mSettings.edit();
						settingsEditor.putString(BeemApplication.ACCOUNT_USERNAME_KEY, username);
						settingsEditor.putString(BeemApplication.ACCOUNT_PASSWORD_KEY, passwordFieldValue);
						settingsEditor.putBoolean("settings_key_gmail", false);
						settingsEditor.commit();
						finish();
					}
				}
			}
		});
	}
}
