package cz.jabbim.android.tools;

import cz.jabbim.android.Constants;
import cz.jabbim.android.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AimStatusArrayAdapter extends ArrayAdapter<String> {

	Activity context;
	String[] objects;

	// int resource;

	public AimStatusArrayAdapter(Activity context, int resource, String[] objects) {
		super(context, resource, objects);
		this.context = context;
		this.objects = objects;
		// this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		View row = inflater.inflate(R.layout.spinner_view, null, false);
		TextView label = (TextView) row.findViewById(R.id.spinner_item);

		label.setText(objects[position]);
		ImageView icon = (ImageView) row.findViewById(R.id.spinner_pic);

		if (position == Constants.STATUS_ONLINE) {
			icon.setImageResource(R.drawable.jabber_online);
		} else if (position == Constants.STATUS_OFFLINE) {
			icon.setImageResource(R.drawable.jabber_offline);
		} else if (position == Constants.STATUS_AWAY) {
			icon.setImageResource(R.drawable.jabber_away);
		} else if (position == Constants.STATUS_E_AWAY) {
			icon.setImageResource(R.drawable.jabber_xa);
		} else if (position == Constants.STATUS_DND) {
			icon.setImageResource(R.drawable.jabber_dnd);
		} else if (position == Constants.STATUS_FREE) {
			icon.setImageResource(R.drawable.jabber_chat);
		}

		return row;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		View row = convertView;

		if (row == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			row = inflater.inflate(R.layout.spinner_view_dropdown, null, false);
		}
		TextView label = (TextView) row.findViewById(R.id.spinner_item);

		label.setText(objects[position]);
		ImageView icon = (ImageView) row.findViewById(R.id.spinner_pic);

		if (position == Constants.STATUS_ONLINE) {
			icon.setImageResource(R.drawable.jabber_online);
		} else if (position == Constants.STATUS_OFFLINE) {
			icon.setImageResource(R.drawable.jabber_offline);
		} else if (position == Constants.STATUS_AWAY) {
			icon.setImageResource(R.drawable.jabber_away);
		} else if (position == Constants.STATUS_E_AWAY) {
			icon.setImageResource(R.drawable.jabber_xa);
		} else if (position == Constants.STATUS_DND) {
			icon.setImageResource(R.drawable.jabber_dnd);
		} else if (position == Constants.STATUS_FREE) {
			icon.setImageResource(R.drawable.jabber_chat);
		}

		return row;
	}

}
