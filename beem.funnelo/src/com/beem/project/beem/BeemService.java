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
package com.beem.project.beem;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyInfo.ProxyType;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.BytestreamsProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.beem.project.beem.service.XmppConnectionAdapter;
import com.beem.project.beem.service.XmppFacade;
import com.beem.project.beem.service.aidl.IXmppFacade;
import com.beem.project.beem.smack.avatar.AvatarMetadataProvider;
import com.beem.project.beem.smack.avatar.AvatarProvider;
import com.beem.project.beem.smack.ping.PingExtension;
import com.beem.project.beem.utils.BeemBroadcastReceiver;
import com.beem.project.beem.utils.BeemConnectivity;
import com.beem.project.beem.utils.Status;

import de.duenndns.ssl.MemorizingTrustManager;

/**
 * This class is for the Beem service. It must contains every global
 * informations needed to maintain the background service. The connection to the
 * xmpp server will be made asynchronously when the service will start.
 *
 * @author darisk
 */
public class BeemService extends Service {

	/** The id to use for status notification. */
	public static final int NOTIFICATION_STATUS_ID = 100;

	public static final String DEFAULT_XMPP_SERVER = "funnelo.co.cc"; // By Adit
	public static final String DEFAULT_XMPP_SERVICE = "chat.funnelo.com"; // By
	// Adit
	public static final int DEFAULT_XMPP_PORT = 5222;

	private static final String TAG = "BeemService";

	// private static final String COMMAND_NAMESPACE =
	// "http://jabber.org/protocol/commands";

	private NotificationManager mNotificationManager;
	private XmppConnectionAdapter mConnection;
	private SharedPreferences mSettings;
	private String mLogin;
	private String mPassword;
	private String mHost;
	private String mService;
	private int mPort;
	private ConnectionConfiguration mConnectionConfiguration;
	private ProxyInfo mProxyInfo;
	private boolean mUseProxy;
	private IXmppFacade.Stub mBind;

	private BeemBroadcastReceiver mReceiver = new BeemBroadcastReceiver();
	private BeemServiceBroadcastReceiver mOnOffReceiver = new BeemServiceBroadcastReceiver();
	private BeemServicePreferenceListener mPreferenceListener = new BeemServicePreferenceListener();

	private boolean mOnOffReceiverIsRegistered;

	/**
	 * Constructor.
	 */
	public BeemService() {
	}

	/**
	 * Initialize the connection.
	 */
	private void initConnectionConfig() {
		// TODO add an option for this ?
		// SmackConfiguration.setPacketReplyTimeout(30000);
		mUseProxy = mSettings.getBoolean(BeemApplication.PROXY_USE_KEY, false);
		if (mUseProxy) {
			String stype = mSettings.getString(BeemApplication.PROXY_TYPE_KEY,
					"HTTP");
			String phost = mSettings.getString(
					BeemApplication.PROXY_SERVER_KEY, "");
			String puser = mSettings.getString(
					BeemApplication.PROXY_USERNAME_KEY, "");
			String ppass = mSettings.getString(
					BeemApplication.PROXY_PASSWORD_KEY, "");
			int pport = Integer.parseInt(mSettings.getString(
					BeemApplication.PROXY_PORT_KEY, "1080"));
			ProxyInfo.ProxyType type = ProxyType.valueOf(stype);
			mProxyInfo = new ProxyInfo(type, phost, pport, puser, ppass);
		} else {
			mProxyInfo = ProxyInfo.forNoProxy();
		}

		mConnectionConfiguration = new ConnectionConfiguration(mHost, mPort,
				mService, mProxyInfo);
		mConnectionConfiguration.setServiceName(DEFAULT_XMPP_SERVICE);

		if (mSettings.getBoolean("settings_key_xmpp_tls_use", false)
				|| mSettings.getBoolean("settings_key_gmail", false)) {
			mConnectionConfiguration.setSecurityMode(SecurityMode.required);
		}
		if (mSettings.getBoolean(BeemApplication.SMACK_DEBUG_KEY, false))
			mConnectionConfiguration.setDebuggerEnabled(true);
		mConnectionConfiguration.setSendPresence(true);
		// maybe not the universal path, but it works on most devices (Samsung
		// Galaxy, Google Nexus One)
		mConnectionConfiguration.setTruststoreType("BKS");
		mConnectionConfiguration
				.setTruststorePath("/system/etc/security/cacerts.bks");
		installMemorizingTrustManager(mConnectionConfiguration);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "ONBIND()");
		return mBind;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "ONUNBIND()");
		if (!mConnection.getAdaptee().isConnected()) {
			this.stopSelf();
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(mReceiver, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mSettings.registerOnSharedPreferenceChangeListener(mPreferenceListener);
		if (mSettings.getBoolean(BeemApplication.USE_AUTO_AWAY_KEY, false)) {
			mOnOffReceiverIsRegistered = true;
			registerReceiver(mOnOffReceiver, new IntentFilter(
					Intent.ACTION_SCREEN_OFF));
			registerReceiver(mOnOffReceiver, new IntentFilter(
					Intent.ACTION_SCREEN_ON));
		}
		mHost = DEFAULT_XMPP_SERVER;
		mService = DEFAULT_XMPP_SERVER;
		mLogin = mSettings.getString(BeemApplication.ACCOUNT_USERNAME_KEY, "")
				.trim();
		mPassword = mSettings.getString(BeemApplication.ACCOUNT_PASSWORD_KEY,
				"");
		mPort = DEFAULT_XMPP_PORT;

		if (mSettings.getBoolean(BeemApplication.FULL_JID_LOGIN_KEY, false)
				|| "gmail.com".equals(mService)
				|| "googlemail.com".equals(mService)) {
			mLogin += '@' + mService;
		}

		initConnectionConfig();
		configure(ProviderManager.getInstance());

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mConnection = new XmppConnectionAdapter(mConnectionConfiguration,
				mLogin, mPassword, this);

		Roster.setDefaultSubscriptionMode(SubscriptionMode.manual);
		mBind = new XmppFacade(mConnection);
		Log.d(TAG, "Create BeemService");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mNotificationManager.cancelAll();
		unregisterReceiver(mReceiver);
		mSettings
				.unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
		if (mOnOffReceiverIsRegistered)
			unregisterReceiver(mOnOffReceiver);
		if (mConnection.isAuthentificated()
				&& BeemConnectivity.isConnected(this))
			mConnection.disconnect();
		Log.i(TAG, "Stopping the service");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(TAG, "onStart");
		try {
			mConnection.connectAsync();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Show a notification using the preference of the user.
	 *
	 * @param id
	 *            the id of the notification.
	 * @param notif
	 *            the notification to show
	 */
	public void sendNotification(int id, Notification notif) {
		if (mSettings
				.getBoolean(BeemApplication.NOTIFICATION_VIBRATE_KEY, true))
			notif.defaults |= Notification.DEFAULT_VIBRATE;
		notif.ledARGB = 0xff0000ff; // Blue color
		notif.ledOnMS = 1000;
		notif.ledOffMS = 1000;
		notif.defaults |= Notification.DEFAULT_LIGHTS;
		String ringtoneStr = mSettings.getString(
				BeemApplication.NOTIFICATION_SOUND_KEY,
				Settings.System.DEFAULT_NOTIFICATION_URI.toString());
		notif.sound = Uri.parse(ringtoneStr);
		mNotificationManager.notify(id, notif);
	}

	/**
	 * Delete a notification.
	 *
	 * @param id
	 *            the id of the notification
	 */
	public void deleteNotification(int id) {
		mNotificationManager.cancel(id);
	}

	/**
	 * Reset the status to online after a disconnect.
	 */
	public void resetStatus() {
		Editor edit = mSettings.edit();
		edit.putInt(BeemApplication.STATUS_KEY, 1);
		edit.commit();
	}

	/**
	 * Initialize Jingle from an XmppConnectionAdapter.
	 *
	 * @param adaptee
	 *            XmppConnection used for jingle.
	 */
	public void initJingle(XMPPConnection adaptee) {
	}

	/**
	 * Return a bind to an XmppFacade instance.
	 *
	 * @return IXmppFacade a bind to an XmppFacade instance
	 */
	public IXmppFacade getBind() {
		return mBind;
	}

	public XmppConnectionAdapter getmConnection() {
		return mConnection;
	}

	/**
	 * Get the preference of the service.
	 *
	 * @return the preference
	 */
	public SharedPreferences getServicePreference() {
		return mSettings;
	}

	/**
	 * Get the notification manager system service.
	 *
	 * @return the notification manager service.
	 */
	public NotificationManager getNotificationManager() {
		return mNotificationManager;
	}

	/**
	 * Install the MemorizingTrustManager in the ConnectionConfiguration of
	 * Smack.
	 *
	 * @param config
	 *            the configuration to modify
	 */
	private void installMemorizingTrustManager(ConnectionConfiguration config) {
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, MemorizingTrustManager.getInstanceList(this),
					new java.security.SecureRandom());
			// config.setCustomSSLContext(sc);
		} catch (GeneralSecurityException e) {
			Log.w(TAG, "Unable to use MemorizingTrustManager", e);
		}
	}

	/**
	 * A sort of patch from this thread:
	 * http://www.igniterealtime.org/community/thread/31118. Avoid
	 * ClassCastException by bypassing the classloading shit of Smack.
	 *
	 * @param pm
	 *            The ProviderManager.
	 */
	private void configure(ProviderManager pm) {
		Log.d(TAG, "configure");

		// Time
		try {
			pm.addIQProvider("query", "jabber:iq:time", Class
					.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (ClassNotFoundException e) {
			Log.w(TAG,
					"Can't load class for org.jivesoftware.smackx.packet.Time");
		}

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new XHTMLExtensionProvider());

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster",
				new RosterExchangeProvider());

		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event",
				new MessageEventProvider());

		// Chat State
		pm.addExtensionProvider("active",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("composing",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("paused",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("inactive",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("gone",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());

		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference",
				new GroupChatInvitation.Provider());

		// Service Discovery
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());
		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());
		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());
		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());
		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay",
				new DelayInformationProvider());
		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version", Class
					.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			Log
					.w(TAG,
							"Can't load class for org.jivesoftware.smackx.packet.Version");
		}

		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());
		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());

		// Last Activity
		pm
				.addIQProvider("query", "jabber:iq:last",
						new LastActivity.Provider());

		// User Search
		pm
				.addIQProvider("query", "jabber:iq:search",
						new UserSearch.Provider());

		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup",
				"http://www.jivesoftware.org/protocol/sharedgroup",
				new SharedGroupsInfo.Provider());

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses",
				"http://jabber.org/protocol/address",
				new MultipleAddressesProvider());

		// PEP avatar
		pm.addExtensionProvider("metadata", "urn:xmpp:avatar:metadata",
				new AvatarMetadataProvider());
		pm.addExtensionProvider("data", "urn:xmpp:avatar:data",
				new AvatarProvider());

		// Ping
		pm.addIQProvider(PingExtension.ELEMENT, PingExtension.NAMESPACE,
				PingExtension.class);
	}

	/**
	 * Listen on preference changes.
	 */
	private class BeemServicePreferenceListener implements
			SharedPreferences.OnSharedPreferenceChangeListener {

		/**
		 * ctor.
		 */
		public BeemServicePreferenceListener() {
		}

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (BeemApplication.USE_AUTO_AWAY_KEY.equals(key)) {
				if (sharedPreferences.getBoolean(
						BeemApplication.USE_AUTO_AWAY_KEY, false)) {
					mOnOffReceiverIsRegistered = true;
					registerReceiver(mOnOffReceiver, new IntentFilter(
							Intent.ACTION_SCREEN_OFF));
					registerReceiver(mOnOffReceiver, new IntentFilter(
							Intent.ACTION_SCREEN_ON));
				} else {
					mOnOffReceiverIsRegistered = false;
					unregisterReceiver(mOnOffReceiver);
				}
			}
		}
	}

	/**
	 * Listen on some Intent broadcast, ScreenOn and ScreenOff.
	 */
	private class BeemServiceBroadcastReceiver extends BroadcastReceiver {

		private String mOldStatus;
		private int mOldMode;

		/**
		 * Constructor.
		 */
		public BeemServiceBroadcastReceiver() {
		}

		@Override
		public void onReceive(final Context context, final Intent intent) {
			String intentAction = intent.getAction();
			if (intentAction.equals(Intent.ACTION_SCREEN_OFF)) {
				mOldMode = mConnection.getPreviousMode();
				mOldStatus = mConnection.getPreviousStatus();
				if (mConnection.isAuthentificated())
					mConnection.changeStatus(Status.CONTACT_STATUS_AWAY,
							mSettings.getString(
									BeemApplication.AUTO_AWAY_MSG_KEY, "Away"));
			} else if (intentAction.equals(Intent.ACTION_SCREEN_ON)) {
				if (mConnection.isAuthentificated())
					mConnection.changeStatus(mOldMode, mOldStatus);
			}
		}
	}
}
