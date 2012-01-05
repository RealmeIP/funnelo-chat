package cz.jabbim.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import cz.jabbim.android.service.ConnectionServiceCall;
import cz.jabbim.android.service.JabbimConnectionService;
import cz.jabbim.android.tools.AddUserDialog;
import cz.jabbim.android.tools.AimStatusArrayAdapter;
import cz.jabbim.android.tools.ContactEntry;
import cz.jabbim.android.tools.ContactGroup;
import cz.jabbim.android.tools.ContactResource;
import cz.jabbim.android.tools.GroupListAdapter;
import cz.jabbim.android.tools.OnAbortListener;
import cz.jabbim.android.tools.OnConfirmListener;
import cz.jabbim.android.tools.Presence;
import cz.jabbim.android.tools.PressedEvent;
import cz.jabbim.android.tools.RosterAdapter;
import cz.jabbim.android.tools.SetStatusDialog;

public class Jabbim extends Activity implements AdapterView.OnItemSelectedListener, ListView.OnItemClickListener {

	/**
	 * The request ID, if the Settings Activity is called. If the setting dialog is finished, this ID will be returned.
	 * The actual number does not have a meaning.
	 */
	private static final int REQUEST_SETTINGS = 2008;
	private static final int RESULT_CHATLIST = 4003;

	// private final static int OPEN_PREFERENCES = Menu.FIRST+1;
	// private final static int ADD_CONTACT = Menu.FIRST+2;
	// private final static int SET_STATUS = Menu.FIRST+3;

	private static final String TAG = "Jabbim.class";

	private SharedPreferences prefs;

	private BroadcastReceiver connectionClosedReceiver;
	private BroadcastReceiver connectionFailedReceiver;
	private BroadcastReceiver progressReceiver;
	private BroadcastReceiver presenceBcr;
	private BroadcastReceiver rosterEntryReceiver;
	private BroadcastReceiver rosterGroupReceiver;
	private BroadcastReceiver rosterProgressStartReceiver;
	private BroadcastReceiver rosterProgressStopReceiver;

	private ProgressDialog rosterProgress;
	private ProgressDialog pd = null;

	private boolean rosterDataLoaded = false;

	private Gallery groupList;
	private ArrayList<ContactGroup> groups;
	private GroupListAdapter groupListAdapter;
	private ContactGroup selectedGroup;

	private ListView roster;
	private List<Presence> presenceQueue;
	private List<ContactEntry> rosterEntries;
	private RosterAdapter rosterAdapter;
	private String[] status;

	private ContactEntry contextMenuItem;

	/**
	 * btnSettings invokes a new intent/activity (Settings.class) where all adjustments can be made
	 */

	private Spinner spin;

	private SetStatusDialog d;

	private final Activity aim = this;

	private Intent aimConServ;
	// private final Intent aimConServ = new Intent(this,service.AimConnectionService);
	private ConnectionServiceCall service;
	private ServiceConnection callConnectService = null;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.main);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		groupList = (Gallery) findViewById(R.id.grouplist);
		groupList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
				selectedGroup = groupListAdapter.getItem(position);
				updateList();
			}
		});
		groups = new ArrayList<ContactGroup>();
		groupListAdapter = new GroupListAdapter(this, groups);
		groupList.setAdapter(groupListAdapter);

		roster = (ListView) findViewById(R.id.contactList);
		roster.setOnItemClickListener(this);
		presenceQueue = new ArrayList<Presence>();
		rosterEntries = new ArrayList<ContactEntry>();
		rosterAdapter = new RosterAdapter(this);
		roster.setAdapter(rosterAdapter);

		final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (velocityX <= -400.0f) {
					for (int i = 0; i < groupListAdapter.getCount() - 1; ++i) {
						ContactGroup cg = groupListAdapter.getItem(i);
						if (cg == selectedGroup
								|| (cg.isVirtual() && selectedGroup.isVirtual() && cg.getName().equals(
										selectedGroup.getName()))) {
							selectedGroup = groupListAdapter.getItem(i + 1);
							groupList.setSelection(i + 1);
							updateList();
							roster.cancelLongPress();
							break;
						}
					}

					return true;
				} else if (velocityX >= 400.0f) {

					for (int i = 1; i < groupListAdapter.getCount(); ++i) {
						ContactGroup cg = groupListAdapter.getItem(i);
						if (cg == selectedGroup
								|| (cg.isVirtual() && selectedGroup.isVirtual() && cg.getName().equals(
										selectedGroup.getName()))) {
							selectedGroup = groupListAdapter.getItem(i - 1);
							groupList.setSelection(i - 1);
							updateList();
							roster.cancelLongPress();
							break;
						}
					}

					return true;
				}

				return true;
			}
		});

		roster.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});

		roster.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
				ContactEntry entry = (ContactEntry) rosterAdapter.getItem(info.position);
				menu.setHeaderTitle(entry.getJid());
				if (entry.countResources() > 1) {
					SubMenu resourcesMenu = menu.addSubMenu(Menu.CATEGORY_ALTERNATIVE, 2, Menu.FIRST + 1,
							R.string.RosterContextOpenChatResource);
					ArrayList<ContactResource> resources = entry.getResources();
					for (ContactResource res : resources) {
						resourcesMenu.add(Menu.NONE, 3, Menu.NONE, res.getResourceName());
					}
				}
				menu.add(Menu.NONE, 1, Menu.FIRST, getString(R.string.RosterContextOpenChat));
			}
		});

		status = getResources().getStringArray(R.array.statusSpinner);
		spin = (Spinner) findViewById(R.id.status);
		spin.setOnItemSelectedListener(this);
		AimStatusArrayAdapter aa = new AimStatusArrayAdapter(this, R.layout.spinner_view_dropdown, status); // (this,R.layout.spinner_view,R.array.statusSpinner);
		spin.setAdapter(aa);
		spin.setPromptId(R.string.setYourStatus);

		/*
		 * Start the connection service, which then actually build the connection to the xmpp server and logs the user
		 * in.
		 */

		aimConServ = new Intent(this, JabbimConnectionService.class);
		startService(aimConServ);

		Log.i(getClass().getSimpleName(), "AIM Started");
	}

	@Override
	public void onResume() {
		super.onResume();

		bindToService();

		/*
		 * SQLiteDatabase db = new JabberoidDbConnector(this).getReadableDatabase();
		 * String[] columns = { Constants.TABLE_SETTINGS_FIELD_VALUE };
		 * String selection = Constants.TABLE_SETTINGS_FIELD_KEY + " = '" + Constants.SETTINGS_SELECTED_GROUP + "'";
		 * try {
		 * Cursor result = db.query(Constants.TABLE_SETTINGS, columns, selection, null, null, null, null);
		 * result.moveToFirst();
		 * String selectedGroupName = result.getString(result.getColumnIndex(Constants.TABLE_SETTINGS_FIELD_VALUE));
		 * for(ContactGroup group : groups) {
		 * if(group.getName().equals(selectedGroupName)) {
		 * selectedGroup = group;
		 * }
		 * }
		 * }
		 * catch(Exception e) {
		 * }
		 * db.close();
		 */

		// updateList();
	}

	@Override
	public void onPause() {
		super.onPause();

		/*
		 * try {
		 * SQLiteDatabase db = new JabberoidDbConnector(this).getWritableDatabase();
		 * ContentValues cv = new ContentValues();
		 * cv.put(Constants.TABLE_SETTINGS_FIELD_KEY, Constants.SETTINGS_SELECTED_GROUP);
		 * cv.put(Constants.TABLE_SETTINGS_FIELD_VALUE, selectedGroup.getName());
		 * String whereClause = Constants.TABLE_SETTINGS_FIELD_KEY + " = '" + Constants.SETTINGS_SELECTED_GROUP + "'";
		 * if(db.update(Constants.TABLE_SETTINGS, cv, whereClause, null) == 0) {
		 * db.insert(Constants.TABLE_SETTINGS, null, cv);
		 * }
		 * }
		 * catch(Exception e) {
		 * }
		 */

		unbindFromService();
		Log.i(TAG, "AIM Paused");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG, "AIM Stopped");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		rosterEntries.clear();
		updateList();
		Log.i(TAG, "AIM Destroyed");
		// Debug.stopMethodTracing();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		int position;
		ContactEntry entry;
		if (contextMenuItem != null) {
			entry = contextMenuItem;
		} else {
			try {
				info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				position = info.position;
				entry = (ContactEntry) rosterAdapter.getItem(position);
			} catch (Exception e) {
				return false;
			}
		}

		switch (item.getItemId()) {
		case 1:
			startChat(entry.getJid());
			break;
		case 2:
			contextMenuItem = entry;
			break;
		case 3:
			startChat(entry.getJid() + "/" + item.getTitle());
			contextMenuItem = null;
			break;
		default:
			super.onContextItemSelected(item);
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return doSelectedItem(item) || super.onOptionsItemSelected(item);
	}

	private boolean doSelectedItem(MenuItem item) {

		switch (item.getItemId()) {
		case (R.id.menuMainPref):// OPEN PREFERENCES):
			menuaction("Pref");
			return true;
		case (R.id.menuMainAddContact):// ADD_CONTACT):
			menuaction("AddContact");
			return true;
		case (R.id.menuMainSetStatus):// SET_STATUS):
			menuaction("SetStatus");
			return true;
		case (R.id.menuMainShowChats):// SET_STATUS):
			menuaction("ShowChats");
			return true;
		case (R.id.menuMainExit):
			try {
				service.logOff();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			onDestroy();
			finish();
			int pid = android.os.Process.myPid();
			android.os.Process.killProcess(pid);

			return true;
		}

		return false;
	}

	private boolean bindToService() {
		if (callConnectService == null) {
			callConnectService = new ServiceConnection() {

				public void onServiceConnected(ComponentName name, IBinder binder) {
					service = ConnectionServiceCall.Stub.asInterface(binder);

					try {
						if (service != null && !service.isLoggedIn()) {
							spin.setSelection(Constants.STATUS_OFFLINE);
							if (prefs.getString("prefJabberIdKey", null) != null
									&& !prefs.getString("prefJabberIdKey", null).equals("")
									&& prefs.getString("prefPasswordKey", null) != null
									&& !prefs.getString("prefPasswordKey", null).equals("")) {
								spin.setSelection(Constants.STATUS_ONLINE);
							} else {
								Intent i = new Intent(aim, Settings.class);
								startActivityForResult(i, REQUEST_SETTINGS);
							}

						} else if (service != null && service.isLoggedIn()) {
							service.getRoster();
							spin.setSelection(prefs.getInt("currentSelection", Constants.STATUS_OFFLINE));
						} else {
							spin.setSelection(Constants.STATUS_OFFLINE);
						}
					} catch (RemoteException e) {
						Log.e(TAG, "Unable to communicate with service");
						e.printStackTrace();
					}

				}

				public void onServiceDisconnected(ComponentName name) {
					service = null;
				}
			};
		}

		boolean bound = bindService(aimConServ, callConnectService, BIND_AUTO_CREATE);

		connectionClosedReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				setAllContactsOffline();
				updateList();
			}
		};

		registerReceiver(connectionClosedReceiver, new IntentFilter("cz.jabbim.android.androidim.CONNECTION_CLOSED"));

		connectionFailedReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (pd.isShowing()) pd.dismiss();
				spin.setSelection(Constants.STATUS_OFFLINE);
				new AlertDialog.Builder(aim).setTitle(R.string.ConnectionFailedTitle).setMessage(
						R.string.ConnectionFailedText).setCancelable(false).setPositiveButton("Ok", null).show();
				setAllContactsOffline();
				updateList();
			}
		};
		registerReceiver(connectionFailedReceiver, new IntentFilter("cz.jabbim.android.androidim.CONNECTION_FAILED"));

		progressReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (pd.isShowing()) {
					pd.dismiss();
				}
			}
		};
		registerReceiver(progressReceiver, new IntentFilter("cz.jabbim.android.androidim.LOGGED_IN"));

		presenceBcr = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG, "Presence received, updating roster data");
				String jid = intent.getStringExtra("jid");
				String resourceName = intent.getStringExtra("resourceName");
				int resourcePriority = intent.getIntExtra("resourcePriority", 0);
				int presenceType = intent.getIntExtra("presenceType", Constants.PRESENCETYPE_NULL);
				int presenceMode = intent.getIntExtra("presenceMode", Constants.PRESENCEMODE_NULL);
				String presenceMessage = intent.getStringExtra("presenceMessage");
				String avatarHash = intent.getStringExtra("avatarHash");

				rosterPresenceUpdate(jid, resourceName, resourcePriority, presenceType, presenceMode, presenceMessage,
						avatarHash);
				updateList();
			}
		};
		registerReceiver(presenceBcr, new IntentFilter("cz.jabbim.android.androidim.PRESENCE_CHANGED"));

		rosterEntryReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String jid = intent.getStringExtra("jid");
				String resourceName = intent.getStringExtra("resourceName");
				int resourcePriority = intent.getIntExtra("resourcePriority", 0);
				String name = intent.getStringExtra("name");
				String status = intent.getStringExtra("status");
				int presenceType = intent.getIntExtra("presenceType", Constants.PRESENCETYPE_NULL);
				int presenceMode = intent.getIntExtra("presenceMode", Constants.PRESENCEMODE_NULL);
				String presenceMessage = intent.getStringExtra("msg");
				ArrayList<String> entryGroups = intent.getStringArrayListExtra("groups");
				if (entryGroups == null) {
					entryGroups = new ArrayList<String>();
				} else {
					for (String grp : entryGroups) {
						for (ContactGroup group : groups) {
							if (group.getName().equals(grp)) {
								if (presenceType == Constants.PRESENCETYPE_AVAILABLE) {
									group.addOnlineEntry(jid);
								} else {
									group.removeOnlineEntry(jid);
								}

								group.addEntry(new ContactEntry(jid, name, status, entryGroups));
							}
						}
					}
				}

				for (int i = 0; i < rosterEntries.size(); i++) {
					if (rosterEntries.get(i).getJid().equals(jid)) {
						// entry already in roster, only update fileds
						rosterEntries.get(i).setName(name);
						rosterEntries.get(i).setStatus(status);
						rosterEntries.get(i).setGroups(entryGroups);

						rosterPresenceUpdate(jid, resourceName, resourcePriority, presenceType, presenceMode,
								presenceMessage, null);
						rosterProgress.incrementProgressBy(1);
						return;
					}
				}

				ContactEntry contact = new ContactEntry(jid, name, status, entryGroups);
				rosterEntries.add(contact);

				try {
					for (int i = 0; i < presenceQueue.size(); i++) {
						if (presenceQueue.get(i).getJid().equals(jid)) {
							Presence presence = presenceQueue.get(i);
							rosterPresenceUpdate(presence.getJid(), presence.getResourceName(), presence
									.getResourcePriority(), presence.getPresenceType(), presence.getPresenceMode(),
									presence.getPresenceMessage(), presence.getAvatarHash());
							presenceQueue.remove(i);
						}
					}
				} catch (NullPointerException e) {
					// null value in presenceQueue
				}

				rosterPresenceUpdate(jid, resourceName, resourcePriority, presenceType, presenceMode, presenceMessage,
						null);
				rosterProgress.incrementProgressBy(1);
			}
		};
		registerReceiver(rosterEntryReceiver, new IntentFilter("cz.jabbim.android.androidim.PRESENCE_ROSTER_ENTRY"));

		rosterGroupReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String groupName = intent.getStringExtra("groupName");

				for (ContactGroup group : groups) {
					if (group.getName().equals(groupName)) { return; }
				}

				groups.add(new ContactGroup(groupName, false));
				rosterProgress.incrementProgressBy(1);
			}
		};
		registerReceiver(rosterGroupReceiver, new IntentFilter("cz.jabbim.android.androidim.PRESENCE_ROSTER_GROUP"));

		rosterProgressStartReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				int groups = intent.getIntExtra("groupsCount", 0);
				int entries = intent.getIntExtra("entriesCount", 0);

				rosterProgress = new ProgressDialog(aim);
				rosterProgress.setTitle(getString(R.string.DialogRosterProgress));
				rosterProgress.setMax(groups + entries);
				rosterProgress.setProgress(0);
				rosterProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				rosterProgress.show();
			}
		};
		registerReceiver(rosterProgressStartReceiver, new IntentFilter(
				"cz.jabbim.android.androidim.ROSTER_PRESENCE_START"));

		rosterProgressStopReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				rosterDataLoaded = true;

				if (rosterProgress.isShowing()) {
					rosterProgress.dismiss();
				}

				updateList();
			}
		};
		registerReceiver(rosterProgressStopReceiver, new IntentFilter(
				"cz.jabbim.android.androidim.ROSTER_PRESENCE_STOP"));

		return bound;
	}

	private void unbindFromService() {
		if (callConnectService != null) {
			unregisterReceiver(rosterProgressStartReceiver);
			unregisterReceiver(rosterProgressStopReceiver);
			unregisterReceiver(rosterGroupReceiver);
			unregisterReceiver(rosterEntryReceiver);
			unregisterReceiver(presenceBcr);
			unregisterReceiver(progressReceiver);
			unregisterReceiver(connectionClosedReceiver);
			unregisterReceiver(connectionFailedReceiver);
			unbindService(callConnectService);
		}
	}

	private void progressD() {
		pd = ProgressDialog.show(this, getString(R.string.DialogConnecting1), getString(R.string.DialogConnecting2)
				+ " " + prefs.getString("prefJabberIdKey", "NOT SET") + ". " + getString(R.string.DialogConnecting3),
				true, true, new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface arg0) {
						spin.setSelection(Constants.STATUS_OFFLINE);
						stopService(aimConServ);
						setAllContactsOffline();
						updateList();
					}
				});
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

		setPreference("currentSelection", position);

		String mode = null;

		switch (position) {
		case Constants.STATUS_ONLINE:
			mode = "available";
			break;
		case Constants.STATUS_AWAY:
			mode = "away";
			break;
		case Constants.STATUS_E_AWAY:
			mode = "xa";
			break;
		case Constants.STATUS_DND:
			mode = "dnd";
			break;
		case Constants.STATUS_FREE:
			mode = "chat";
			break;
		}

		try {
			if (mode != null && !service.isLoggedIn()) {
				progressD();
				service.connect(null, "available", mode);
			} else if (mode != null) {
				service.setStatus(null, "available", mode);
			} else {
				service.disconnect();
			}
		} catch (DeadObjectException e) {
			callConnectService.onServiceDisconnected(null);
		} catch (RemoteException e) {
			callConnectService.onServiceDisconnected(null);
		}

	}

	public void onNothingSelected(AdapterView<?> parent) {
	// tv.setText("");
	}

	public void menuaction(String action) {
		if (action == "Pref") {
			Intent i = new Intent(this, Settings.class);
			startActivityForResult(i, REQUEST_SETTINGS);
		} else if (action == "AddContact") {
			final AddUserDialog addUserDialog;
			addUserDialog = new AddUserDialog(this, R.layout.add_user_dialog, R.id.addUserDialog_confirm,
					R.id.addUserDialog_cancel, R.id.addUserDialog_entry, R.id.addUserDialog_groupSwitcher, groups);
			addUserDialog.setCancelable(true);
			addUserDialog.setTitle(R.string.MainAddUser);
			addUserDialog.setOnConfirmListener(new OnConfirmListener() {

				public void onConfirm(PressedEvent e) {
					List<String> listGroup = addUserDialog.getGroups();
					if (listGroup.isEmpty()) return;
					addEntry(addUserDialog.getJid(), "", listGroup);
				}
			});
			addUserDialog.show();

		} else if (action == "ShowChats") {
			Intent i = new Intent(this, ConversationList.class);
			// this.getApplication().startActivity(i);
			startActivity(i);

		} else if (action == "SetStatus") {
			d = new SetStatusDialog(this, R.layout.status_message_dialog, R.id.statusMessageDialog_confirm,
					R.id.statusMessageDialog_cancel, R.id.statusMessageDialog_msg,
					R.id.statusMessageDialog_msgSwitcher, getLastStatusMessages());
			d.setCancelable(true);
			d.setTitle(R.string.MainSetStatus);
			d.setOnAbortListener(new OnAbortListener() {

				public void onAbort(PressedEvent e) {
					Toast.makeText(aim, R.string.MainStatusAbort, Toast.LENGTH_SHORT).show();
				}
			});
			d.setOnConfirmListener(new OnConfirmListener() {

				public void onConfirm(PressedEvent e) {
					Toast.makeText(aim, d.getNewUserId(), Toast.LENGTH_SHORT).show();
				}
			});
			d.show();
		}
	}

	private List<ContactEntry> getContacts() {
		List<ContactEntry> resultList = new ArrayList<ContactEntry>();

		for (ContactEntry entry : rosterEntries) {
			if (!prefs.getBoolean("prefShowOfflineKey", false)
					&& entry.getPresenceType() == Constants.PRESENCETYPE_AVAILABLE) {
				resultList.add(entry);
			} else if (prefs.getBoolean("prefShowOfflineKey", false)) {
				resultList.add(entry);
			}
		}

		return resultList;
	}

	private List<ContactEntry> getContacts(ContactGroup group) {
		ArrayList<ContactEntry> resultList = new ArrayList<ContactEntry>();
		List<ContactEntry> groupEntries = group.getEntries();

		for (ContactEntry entry : groupEntries) {
			if (!prefs.getBoolean("prefShowOfflineKey", false)
					&& entry.getPresenceType() == Constants.PRESENCETYPE_AVAILABLE) {
				resultList.add(entry);
			} else if (prefs.getBoolean("prefShowOfflineKey", false)) {
				resultList.add(entry);
			}
		}

		return resultList;
	}

	private List<ContactEntry> getContactsUngrouped() {
		List<ContactEntry> resultList = new ArrayList<ContactEntry>();

		for (ContactEntry entry : rosterEntries) {
			if (entry.getPresenceType() == Constants.PRESENCETYPE_AVAILABLE) {
				if (entry.getGroups().size() == 0) {
					if (!prefs.getBoolean("prefShowOfflineKey", false)
							&& entry.getPresenceType() == Constants.PRESENCETYPE_AVAILABLE) {
						resultList.add(entry);
					} else if (prefs.getBoolean("prefShowOfflineKey", false)) {
						resultList.add(entry);
					}
				}
			}
		}

		return resultList;
	}

	private ArrayList<ContactGroup> getGroups() {
		ArrayList<ContactGroup> returnList = new ArrayList<ContactGroup>();

		if (prefs.getBoolean("prefShowGroupAllContactsKey", false)) {
			returnList.add(new ContactGroup(getString(R.string.virtualGroupAllContacts), true));
		}

		for (ContactGroup group : groups) {
			int online = group.getEntriesOnline();

			if (!prefs.getBoolean("prefShowOfflineKey", false) && online > 0) {
				returnList.add(group);
			} else if (prefs.getBoolean("prefShowOfflineKey", false)) {
				returnList.add(group);
			}
		}

		if (prefs.getBoolean("prefShowGroupUngroupedKey", true)) {
			returnList.add(new ContactGroup(getString(R.string.virtualGroupUngrouped), true));
		}

		return returnList;
	}

	private void rosterPresenceUpdate(String jid, String resourceName, int resourcePriority, int presenceType,
			int presenceMode, String presenceMessage, String avatarHash) {
		// boolean found = false;
		for (int i = 0; i < rosterEntries.size(); i++) {
			ContactEntry entry = rosterEntries.get(i);
			if (entry.getJid().equals(jid)) {
				rosterEntries.get(i).updatePresence(resourceName, resourcePriority, presenceType, presenceMode,
						presenceMessage, avatarHash);

				for (ContactGroup group : groups) {
					try {
						group.getEntry(jid).updatePresence(resourceName, resourcePriority, presenceType, presenceMode,
								presenceMessage, avatarHash);
					} catch (NullPointerException e) {
						// contact is not in group;
					}
				}

				/*
				 * if(rosterEntries.get(i).getAvatarChanged()) {
				 * try {
				 * service.getAvatar(jid);
				 * }
				 * catch(Exception e) {
				 * Log.e(TAG, e.getMessage());
				 * }
				 * }
				 */
				return;
			}
		}

		presenceQueue.add(new Presence(jid, resourceName, resourcePriority, presenceType, presenceMode,
				presenceMessage, avatarHash));
	}

	private void updateGroupList() {
		Collections.sort(groups);
		groupListAdapter.update(getGroups());

		try {
			if (selectedGroup == null) {
				groupList.setSelection(0, true);
				selectedGroup = (ContactGroup) groupList.getItemAtPosition(0);
			}
		} catch (Exception e) {
			Log.e(TAG, "Selected group null");
		}
	}

	private void updateList() {
		if (!rosterDataLoaded) { return; }

		updateGroupList();

		if (selectedGroup == null) {
			rosterAdapter.update(getContacts());
		} else {
			if (selectedGroup.isVirtual()) {
				if (selectedGroup.getName().equals(getString(R.string.virtualGroupAllContacts))) {
					rosterAdapter.update(getContacts());
				} else if (selectedGroup.getName().equals(getString(R.string.virtualGroupUngrouped))) {
					rosterAdapter.update(getContactsUngrouped());
				}
			} else {
				rosterAdapter.update(getContacts(selectedGroup));
			}
		}
	}

	public void setPreference(String name, Object value) {
		SharedPreferences.Editor editor = prefs.edit();

		if (value instanceof String) {
			editor.putString(name, String.valueOf(value));
		} else if (value instanceof Integer) {
			editor.putInt(name, Integer.parseInt(String.valueOf(value)));
		}

		editor.commit();
	}

	private void addEntry(String user, String name, List<String> groups) {
		try {
			service.addEntry(user, name, groups);
		} catch (RemoteException e) {
			callConnectService.onServiceDisconnected(null);
		}
	}

	private void startChat(String jid) {
		finishActivity(RESULT_CHATLIST);
		Intent i = new Intent(this, ConversationList.class);
		i.putExtra("startChat", true);
		i.putExtra("jid", jid);
		startActivityForResult(i, RESULT_CHATLIST);
	}

	private void setAllContactsOffline() {
		for (ContactGroup group : groups) {
			group.clearEntries();
		}
		rosterEntries.clear();
	}

	public void setStatusMessage(String message) {
		try {
			service.insertAndUseMessage(message);
		} catch (DeadObjectException e) {
			callConnectService.onServiceDisconnected(null);
		} catch (RemoteException e) {
			callConnectService.onServiceDisconnected(null);
		}
	}

	public List<String> getLastStatusMessages() {
		List<String> list = null;
		try {
			list = service.getLastStatusMessages();
		} catch (DeadObjectException e) {
			callConnectService.onServiceDisconnected(null);
		} catch (RemoteException e) {
			callConnectService.onServiceDisconnected(null);
		}

		if (list == null) {
			list = new ArrayList<String>();
			list.add("");
		}

		return list;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		ContactEntry contact = (ContactEntry) rosterAdapter.getItem(arg2);
		startChat(contact.getJid());
	}

}