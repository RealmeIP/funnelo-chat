package cz.jabbim.android.tools;

import java.util.List;

import cz.jabbim.android.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GroupListAdapter extends BaseAdapter {

	private Activity context;
	private List<ContactGroup> groups;

	public GroupListAdapter(Context context, List<ContactGroup> groups) {
		this.context = (Activity) context;
		this.groups = groups;
	}

	public void update(List<ContactGroup> groups) {
		this.groups = groups;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return groups.size();
	}

	@Override
	public ContactGroup getItem(int position) {
		return groups.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			v = inflater.inflate(R.layout.grouplist_item, null);
		}

		String label = groups.get(position).getName();

		if (groups.get(position).isVirtual()) {
			label = "@".concat(label);
		}

		((TextView) v).setText(label);

		return v;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

}
