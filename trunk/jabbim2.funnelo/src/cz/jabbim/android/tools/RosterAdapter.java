package cz.jabbim.android.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.jabbim.android.Constants;
import cz.jabbim.android.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RosterAdapter extends BaseAdapter {

	private Activity context;
	private List<ContactEntry> contacts;

	// private List<Presence> presenceQueue;

	public RosterAdapter(Context context) {
		this.context = (Activity) context;
		this.contacts = new ArrayList<ContactEntry>();
	}

	public void update(List<ContactEntry> contacts) {
		this.contacts = contacts;
		Collections.sort(this.contacts);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return contacts.size();
	}

	@Override
	public Object getItem(int position) {
		return contacts.get(position);
	}

	@Override
	public long getItemId(int position) {
		return contacts.get(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View contactRow = convertView;
		ContactEntry contact = contacts.get(position);

		if (contactRow == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			contactRow = inflater.inflate(R.layout.contactlist_item, null);
		}

		TextView labelView = (TextView) contactRow.findViewById(R.id.contactlist_name);
		TextView statusView = (TextView) contactRow.findViewById(R.id.contactlist_status);
		ImageView iconView = (ImageView) contactRow.findViewById(R.id.contactlist_pic);
		// ImageView avatarView = (ImageView)contactRow.findViewById(R.id.contactlist_avatar);

		labelView.setText(contact.getName());

		try {
			if (contact.getPresenceMessage().length() > 90) {
				statusView.setText(contact.getPresenceMessage().substring(0, 90).concat("..."));
			} else {
				statusView.setText(contact.getPresenceMessage());
			}
		} catch (Exception e) {
			statusView.setText("");
		}

		/*
		 * try {
		 * Bitmap avatar = BitmapFactory.decodeByteArray(contact.getAvatar(), 0, contact.getAvatar().length);
		 * avatarView.setImageBitmap(avatar);
		 * }
		 * catch(Exception e) {
		 * // probably no avatar: Nullpointer exception
		 * }
		 */

		int presenceType = contact.getPresenceType();
		int presenceMode = contact.getPresenceMode();

		if (presenceType == Constants.PRESENCETYPE_AVAILABLE) {
			switch (presenceMode) {
			case Constants.PRESENCEMODE_AWAY:
				iconView.setImageResource(R.drawable.jabber_away);
				break;
			case Constants.PRESENCEMODE_XA:
				iconView.setImageResource(R.drawable.jabber_xa);
				break;
			case Constants.PRESENCEMODE_DND:
				iconView.setImageResource(R.drawable.jabber_dnd);
				break;
			case Constants.PRESENCEMODE_CHAT:
				iconView.setImageResource(R.drawable.jabber_chat);
				break;
			default:
				iconView.setImageResource(R.drawable.jabber_online);
			}
		} else {
			iconView.setImageResource(R.drawable.jabber_offline);
		}

		return contactRow;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

}
