package cz.jabbim.android;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import cz.jabbim.android.data.JabberoidDbConnector;
import cz.jabbim.android.service.JabbimConnectionService;

import cz.jabbim.android.R;
import cz.jabbim.android.service.ConnectionServiceCall;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout; // import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public class Conversations extends Activity implements View.OnClickListener, OnKeyListener {

	// private final static int CLOSE_CONVERSATION = Menu.FIRST+1;
	// private final static int CLOSE_GO_CONTACTLIST = Menu.FIRST+2;
	// private final static int RETURN_TO_CONTACTLIST = Menu.FIRST+3;

	// private final static String TAG = "Conversation.class";

	private SharedPreferences prefs;

	Spinner contactChoser;
	TextView conversationWindow;
	EditText messageInput;
	Button sendButton;
	ScrollView scroller;
	LinearLayout ll1;
	LinearLayout ll2;

	// SQLiteDatabase db;

	private static String jid;

	public BroadcastReceiver csr;
	private IntentFilter f;

	private Intent aimConServ;
	// private final Intent aimConServ = new Intent(this,service.AimConnectionService);
	private ConnectionServiceCall service;
	private ServiceConnection callConnectService = null;

	// private Date date;
	// private Calendar cal;
	private DateFormat df;
	private Calendar lastStamp;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.conversation);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		ll1 = (LinearLayout) findViewById(R.id.conversationLinear1);
		ll1.setVisibility(View.GONE);
		ll2 = (LinearLayout) findViewById(R.id.conversationLinear2);

		scroller = (ScrollView) findViewById(R.id.conversationScroller);
		conversationWindow = (TextView) findViewById(R.id.conversationWindow);
		messageInput = (EditText) findViewById(R.id.messageInput);
		sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setOnClickListener(this);
		conversationWindow.setSingleLine(false);

		messageInput.setOnKeyListener(this);

		registerForContextMenu(ll2);
		// registerForContextMenu(scroller);
		// registerForContextMenu(conversationWindow);

		aimConServ = new Intent(this, JabbimConnectionService.class);
		df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
	}

	@Override
	public void onResume() {
		super.onResume();
		SQLiteDatabase db = new JabberoidDbConnector(this).getWritableDatabase();
		jid = getIntent().getStringExtra("jid");
		setTitle(getString(R.string.conversation_name) + ": " + jid);
		bindToService();

		printMessages(getMessages(db, false));
		db.close();
		setUnread();
	}

	@Override
	public void onPause() {
		super.onPause();
		// db.close();
		jid = null;
		unbindFromService();
		setResult(RESULT_OK, getIntent().setAction(jid));
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.conversation, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
		new MenuInflater(getApplication()).inflate(R.menu.conversation, menu);
		menu.setHeaderTitle("Chat with " + jid);
		super.onCreateContextMenu(menu, v, info);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return doSelectedItem(item) || super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return doSelectedItem(item) || super.onContextItemSelected(item);
	}

	public void onClick(View v) {
		if (v == sendButton) {
			sendMessage();
		}
	}

	public boolean onKey(View view, int keyCode, KeyEvent event) {
		if (view == messageInput && event.getAction() == KeyEvent.ACTION_DOWN && event.isShiftPressed()
				&& keyCode == KeyEvent.KEYCODE_ENTER) {
			sendMessage();
			return true;
		}
		return false;
	}

	private boolean doSelectedItem(MenuItem item) {

		switch (item.getItemId()) {
		case (R.id.menuConvCloseChat):// CLOSE_CONVERSATION):
			closeConversation();
			return true;
		case (R.id.menuConvReturn):// RETURN_TO_CONTACTLIST):
			setResult(RESULT_OK, getIntent().setAction(jid));
			finish();
			return true;
		}

		return false;
	}

	private void bindToService() {
		if (callConnectService == null) {
			callConnectService = new ServiceConnection() {

				public void onServiceConnected(ComponentName name, IBinder binder) {
					service = ConnectionServiceCall.Stub.asInterface(binder);
					try {
						if (!service.isLoggedIn()) {
							sendButton.setEnabled(false);
							conversationWindow.append(getString(R.string.ChatYouAreOffline));
							conversationWindow.setEnabled(false);

						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				public void onServiceDisconnected(ComponentName name) {
					service = null;
				}
			};
		}

		csr = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				SQLiteDatabase db = new JabberoidDbConnector(context).getWritableDatabase();
				printMessages(getMessages(db, true));
				db.close();
				String inJid = intent.getStringExtra("jid");
				// String username = intent.getStringExtra("username");;
				if (jid.equalsIgnoreCase(inJid)) {
					csr.abortBroadcast();
				}
				setUnread();
			}
		};
		f = new IntentFilter();
		f.setPriority(10);
		f.addAction("cz.jabbim.android.androidim.NEW_MESSAGE");

		bindService(aimConServ, callConnectService, BIND_AUTO_CREATE);
		registerReceiver(csr, f);
	}

	private void unbindFromService() {
		if (callConnectService != null) {
			unregisterReceiver(csr);
			unbindService(callConnectService);
		}
	}

	private Cursor getMessages(SQLiteDatabase db, boolean justNewMessages) {

		final String table = Constants.TABLE_CONVERSATION;
		// index
		final String[] columns = { Constants.TABLE_CONVERSATION_FIELD_ID, // 0
				Constants.TABLE_CONVERSATION_FIELD_DATE, // 1
				Constants.TABLE_CONVERSATION_FIELD_FROM, // 2
				Constants.TABLE_CONVERSATION_FIELD_TO, // 3
				Constants.TABLE_CONVERSATION_FIELD_MSG // 4
		};

		String selection;

		if (justNewMessages) {
			selection = "(" + Constants.TABLE_CONVERSATION_FIELD_FROM + " = '" + jid + "' or "
					+ Constants.TABLE_CONVERSATION_FIELD_TO + " = '" + jid + "') and "
					+ Constants.TABLE_CONVERSATION_FIELD_NEW + " = '1'";
		} else {
			selection = "(" + Constants.TABLE_CONVERSATION_FIELD_FROM + " = '" + jid + "' or "
					+ Constants.TABLE_CONVERSATION_FIELD_TO + " = '" + jid + "')";
		}

		final String[] selectionArgs = null;
		final String groupBy = null;
		final String having = null;
		final String orderBy = Constants.TABLE_CONVERSATION_FIELD_DATE;

		return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);

	}

	private boolean sendMessage() {
		if (messageInput.getText().length() > 0) {
			try {
				service.sendMessage(jid, messageInput.getText().toString());
				printMessages(null);
				messageInput.setText("");

			} catch (RemoteException e) {
				return false;
			}
		} else return false;

		return true;
	}

	private void printMessages(final Cursor c) {
		runOnUiThread(new Runnable() {

			public void run() {

				// TODO Improve the date comparison of the method
				Date currentDate = new Date();
				Calendar today = Calendar.getInstance();
				today.clear();
				today.set(currentDate.getYear(), currentDate.getMonth(), currentDate.getDate());

				Calendar compareCal = Calendar.getInstance();

				if (c != null) {
					c.moveToFirst();

					String date;
					String from;
					String message;

					while (!c.isAfterLast()) {

						Date temp = new Date(c.getLong(c.getColumnIndex(Constants.TABLE_CONVERSATION_FIELD_DATE)));
						compareCal.clear();
						compareCal.set(temp.getYear(), temp.getMonth(), temp.getDate());

						from = c.getString(2);
						message = c.getString(4);
						if (from.equals("me")) {
							from = prefs.getString("prefJabberIdKey", "me");
						}

						if (lastStamp == null || today.compareTo(lastStamp) != 0) {
							date = temp.toLocaleString();
						} else {
							date = df.format(temp.getTime());
						}
						message = parseEmo(message);
						conversationWindow.append(Html.fromHtml("<b>(" + date + ") " + from + ":</b><br/>" + message
								+ "<br/>", new SmajlGetter(getResources()), null));

						lastStamp = compareCal;

						c.moveToNext();

					}

				} else {

					String message = messageInput.getText().toString();
					String date;

					if (lastStamp == null || today.compareTo(lastStamp) != 0) {
						date = currentDate.toLocaleString();
					} else {
						date = df.format(currentDate);
					}
					String myusername = prefs.getString("prefJabberIdKey", "me");
					message = parseEmo(message);
					conversationWindow.append(Html.fromHtml("<b>(" + date + ") " + myusername + ":</b><br/>" + message
							+ "<br/>", new SmajlGetter(getResources()), null));

					lastStamp = today;

				}
				// c.close();
				scrollDown();
			}
		});

	}

	private String parseEmo(String message) {
		// HTML musime zlikvidovat, nevime, co prislo...
		// message = message.replaceAll("&","&amp;");
		// message = message.replaceAll("\"", "&quot;");
		// message = message.replaceAll("'", "&apos;");
		message = message.replaceAll(">", "&gt;");
		message = message.replaceAll("<", "&lt;");
		// konce radek nahradime za <br/>
		message = message.replaceAll("\n", "<br/>");
		// clickable urls
		message = message.replaceAll("(?:ht|f)tp://\\S+(?<![!.?])", "<a href=\"$0\">$0</a>");
		// smajl test
		message = message.replaceAll("&gt;:-\\)", "<img src=\"smile_devil\">");
		message = message.replaceAll("&gt;:\\)", "<img src=\"smile_devil\">");
		message = message.replaceAll(":-\\)", "<img src=\"smile_smile\">");
		message = message.replaceAll(":\\)", "<img src=\"smile_smile\">");
		message = message.replaceAll(";-\\)", "<img src=\"smile_wink\">");
		message = message.replaceAll(";\\)", "<img src=\"smile_wink\">");
		message = message.replaceAll(":-\\(", "<img src=\"smile_unhappy\">");
		message = message.replaceAll(":\\(", "<img src=\"smile_unhappy\">");
		message = message.replaceAll("\\]:->", "<img src=\"smile_devil\">");
		message = message.replaceAll("B-\\)", "<img src=\"smile_coolglasses\">");
		message = message.replaceAll("8-\\)", "<img src=\"smile_coolglasses\">");
		message = message.replaceAll(":-D", "<img src=\"smile_biggrin\">");
		message = message.replaceAll(":-&gt;", "<img src=\"smile_biggrin\">");
		message = message.replaceAll(":D", "<img src=\"smile_biggrin\">");
		message = message.replaceAll(":&gt;", "<img src=\"smile_biggrin\">");
		message = message.replaceAll("xD", "<img src=\"smile_biggrin\">");
		message = message.replaceAll(":'-\\(", "<img src=\"smile_cry\">");
		message = message.replaceAll(":'\\(", "<img src=\"smile_cry\">");
		message = message.replaceAll(";-\\(", "<img src=\"smile_cry\">");
		message = message.replaceAll(";\\(", "<img src=\"smile_cry\">");
		message = message.replaceAll(":-/", "<img src=\"smile_frowning\">");
		// message=message.replaceAll(":/","<img src=\"smile_frowning\">");
		message = message.replaceAll(":-S", "<img src=\"smile_frowning\">");
		message = message.replaceAll(":S", "<img src=\"smile_frowning\">");
		message = message.replaceAll(":-\\$", "<img src=\"smile_blush\">");
		message = message.replaceAll(":\\$", "<img src=\"smile_blush\">");
		message = message.replaceAll(":-@", "<img src=\"smile_angry\">");
		message = message.replaceAll(":@", "<img src=\"smile_angry\">");
		message = message.replaceAll(":-\\[", "<img src=\"smile_bat\">");
		message = message.replaceAll(":\\[", "<img src=\"smile_bat\">");
		message = message.replaceAll(":-\\*", "<img src=\"smile_kiss\">");
		message = message.replaceAll(":\\*", "<img src=\"smile_kiss\">");
		message = message.replaceAll(":-P", "<img src=\"smile_tongue\">");
		message = message.replaceAll(":P", "<img src=\"smile_tongue\">");
		message = message.replaceAll(":-p", "<img src=\"smile_tongue\">");
		message = message.replaceAll(":p", "<img src=\"smile_tongue\">");
		message = message.replaceAll(":-O", "<img src=\"smile_oh\">");
		message = message.replaceAll(":O", "<img src=\"smile_oh\">");
		message = message.replaceAll("&lt;3", "<img src=\"smile_heart\">");
		// MSN smajliky
		message = message.replaceAll("\\(@\\)", "<img src=\"smile_pussy\">");
		message = message.replaceAll("\\(%\\)", "<img src=\"smile_cuffs\">");
		message = message.replaceAll("\\(S\\)", "<img src=\"smile_moon\">");
		message = message.replaceAll("\\(I\\)", "<img src=\"smile_lamp\">");
		message = message.replaceAll("\\(8\\)", "<img src=\"smile_music\">");
		message = message.replaceAll("\\(B\\)", "<img src=\"smile_beer\">");
		message = message.replaceAll("\\(L\\)", "<img src=\"smile_heart\">");
		message = message.replaceAll("\\(6\\)", "<img src=\"smile_devil\">");
		message = message.replaceAll("\\(W\\)", "<img src=\"smile_brflower\">");
		message = message.replaceAll("\\(Z\\)", "<img src=\"smile_boy\">");
		message = message.replaceAll("\\(X\\)", "<img src=\"smile_girl\">");
		message = message.replaceAll("\\(E\\)", "<img src=\"smile_mail\">");
		message = message.replaceAll("\\(N\\)", "<img src=\"smile_thumbdown\">");
		message = message.replaceAll("\\(P\\)", "<img src=\"smile_photo\">");
		message = message.replaceAll("\\(K\\)", "<img src=\"smile_kiss\">");
		message = message.replaceAll("\\(Y\\)", "<img src=\"smile_thumbup\">");
		message = message.replaceAll("\\(\\}\\)", "<img src=\"smile_hugleft\">");
		message = message.replaceAll("\\(U\\)", "<img src=\"smile_brheart\">");
		message = message.replaceAll("\\(F\\)", "<img src=\"smile_flower\">");
		message = message.replaceAll("\\(H\\)", "<img src=\"smile_coolglasses\">");
		message = message.replaceAll("\\(D\\)", "<img src=\"smile_drink\">");
		message = message.replaceAll("\\(T\\)", "<img src=\"smile_phone\">");
		message = message.replaceAll("\\(C\\)", "<img src=\"smile_coffee\">");
		message = message.replaceAll("\\(\\{\\)", "<img src=\"smile_hugright\">");
		message = message.replaceAll("\\(\\*\\)", "<img src=\"smile_star\">");
		message = message.replaceAll("\\(R\\)", "<img src=\"smile_rainbow\">");

		// vratime sracku :-D
		return message;
	}

	private void setUnread() {
		ContentValues cv = new ContentValues();
		cv.put(Constants.TABLE_CONVERSATION_FIELD_NEW, 0);
		SQLiteDatabase db = new JabberoidDbConnector(this).getWritableDatabase();
		db.update(Constants.TABLE_CONVERSATION, cv, Constants.TABLE_CONVERSATION_FIELD_FROM + "='" + jid + "' or "
				+ Constants.TABLE_CONVERSATION_FIELD_TO + "='" + jid + "'", null);
		db.close();
	}

	private void closeConversation() {
		SQLiteDatabase db = new JabberoidDbConnector(this).getWritableDatabase();
		db.delete(Constants.TABLE_CONVERSATION, Constants.TABLE_CONVERSATION_FIELD_FROM + " = '" + jid + "' or "
				+ Constants.TABLE_CONVERSATION_FIELD_TO + " = '" + jid + "'", null);

		db.close();
		setResult(RESULT_OK, getIntent().setAction(jid));
		finish();
	}

	private void scrollDown() {
		scroller.smoothScrollTo(0, conversationWindow.getBottom() + 2000); // WORKAROU
	}

}

class SmajlGetter implements ImageGetter {

	private Resources r;

	public SmajlGetter(Resources res) {
		r = res;
	}

	@Override
	public Drawable getDrawable(String arg0) {
		// R.drawable.
		// Drawable drawable = r.getDrawable(r.getIdentifier(arg0, null, null));
		Drawable d = r.getDrawable(r.getIdentifier(arg0, "drawable", "cz.jabbim.android"));
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		// BitmapDrawable b = new BitmapDrawable(r);
		// b.createFromPath(arg0);
		return d;
		// return new BitmapDrawable(arg0);
	}

}
