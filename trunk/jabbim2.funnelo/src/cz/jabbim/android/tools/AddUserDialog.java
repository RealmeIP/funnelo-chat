package cz.jabbim.android.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import cz.jabbim.android.Jabbim;

public class AddUserDialog extends Dialog implements OnClickListener, AdapterView.OnItemSelectedListener {

	Button confirm;
	Button cancel;
	EditText newUser;
	String newUserId;
	Spinner groupSwitcher;

	Jabbim context;

	OnConfirmListener confirmListener;
	OnAbortListener abortListener;

	private Vector<OnConfirmListener> confirmListeners = new Vector<OnConfirmListener>();
	private Vector<OnAbortListener> abortListeners = new Vector<OnAbortListener>();

	// private List<ContactGroup> groups;

	public AddUserDialog(Context context) {
		super(context);
	}

	public AddUserDialog(Activity context, int view, int button_confirm, int button_cancel, int editText_NewUser,
			int spinner, List<ContactGroup> groups) {
		super(context);

		setContentView(view);
		this.context = (Jabbim) context;
		confirm = (Button) findViewById(button_confirm);
		confirm.setOnClickListener(this);
		cancel = (Button) findViewById(button_cancel);
		cancel.setOnClickListener(this);
		newUser = (EditText) findViewById(editText_NewUser);
		groupSwitcher = (Spinner) findViewById(spinner);

		// this.groups = groups;
		ArrayAdapter<Object> aa = new ArrayAdapter<Object>(context, android.R.layout.simple_spinner_item, groups
				.toArray());

		groupSwitcher.setAdapter(aa);
		groupSwitcher.setOnItemSelectedListener(this);
	}

	public void onClick(View v) {
		if (v == cancel) {
			this.abort();
		} else {
			this.confirm();
		}
	}

	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {}

	public void setOnConfirmListener(OnConfirmListener l) {
		this.confirmListener = l;
		confirmListeners.add(confirmListener);

	}

	public void setOnAbortListener(OnAbortListener l) {
		this.abortListener = l;
		abortListeners.add(abortListener);

	}

	public String getJid() {
		return newUser.getText().toString();
	}

	public List<String> getGroups() {
		ArrayList<String> entryGroups = new ArrayList<String>();
		if (groupSwitcher.getSelectedItem() == null) return entryGroups;
		entryGroups.add(groupSwitcher.getSelectedItem().toString());
		return entryGroups;
	}

	public void confirm() {
		PressedEvent e = new PressedEvent("Confirm Pressed");
		for (OnConfirmListener ocl : confirmListeners) {
			ocl.onConfirm(e);
		}
		dismiss();
	}

	public void abort() {
		PressedEvent e = new PressedEvent("Abort Pressed");
		for (int i = 0; i < abortListeners.size(); i++) {
			OnAbortListener oal = abortListeners.elementAt(i);
			oal.onAbort(e);
		}
		dismiss();
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	// TODO Auto-generated method stub

	}

}