/*
    BEEM is a videoconference application on the Android Platform.

    Copyright (C) 2009 by Frederic-Charles Barthelery,
                          Jean-Manuel Da Silva,
                          Nikita Kozlov,
                          Philippe Lago,
                          Jean Baptiste Vergely,
                          Vincent Veronis.

    This file is part of BEEM.

    BEEM is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    BEEM is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BEEM.  If not, see <http://www.gnu.org/licenses/>.

    Please send bug reports with examples or suggestions to
    contact@beem-project.com or http://dev.beem-project.com/

    Epitech, hereby disclaims all copyright interest in the program "Beem"
    written by Frederic-Charles Barthelery,
               Jean-Manuel Da Silva,
               Nikita Kozlov,
               Philippe Lago,
               Jean Baptiste Vergely,
               Vincent Veronis.

    Nicolas Sadirac, November 26, 2009
    President of Epitech.

    Flavien Astraud, November 26, 2009
    Head of the EIP Laboratory.

 */
package com.beem.project.beem.ui.dialogs.builders;

import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.beem.project.beem.BeemApplication;
import com.beem.project.beem.BeemService;
import com.beem.project.beem.R;
import com.beem.project.beem.service.Contact;
import com.beem.project.beem.ui.Chat;

/**
 * Create dialog alias.
 */
public class JoinMUC extends AlertDialog.Builder {

	private static final String TAG = "Dialogs.Builders > JoinMUC";

	private Context context;
	private EditText mEditTextRoom;
	private EditText mEditTextPseudo;

	private SharedPreferences mSettings;

	/**
	 * Constructor.
	 *
	 * @param context
	 *            context activity.
	 * @param roster
	 *            Beem roster.
	 * @param contact
	 *            the contact to modify.
	 */
	public JoinMUC(final Context context) {
		super(context);
		this.context = context;
		LayoutInflater factory = LayoutInflater.from(context);
		final View textEntryView = factory
				.inflate(R.layout.joinmucdialog, null);
		setView(textEntryView);
		mEditTextRoom = (EditText) textEntryView
				.findViewById(R.id.CDRoomDialogName);
		mEditTextPseudo = (EditText) textEntryView
				.findViewById(R.id.CDNickDialogName);
		mSettings = PreferenceManager.getDefaultSharedPreferences(context);
		String title = mSettings.getString("JoinMUC", "Join Chat Room");
		setTitle(title);
		setPositiveButton(R.string.OkButton, new DialogClickListener());
		setNegativeButton(R.string.CancelButton, new DialogClickListener());
	}

	/**
	 * Event click listener.
	 */
	class DialogClickListener implements DialogInterface.OnClickListener {

		/**
		 * Constructor.
		 */
		public DialogClickListener() {
		}

		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				String room = mEditTextRoom.getText().toString();
				boolean isEmail = Pattern.matches("[a-zA-Z0-9._%+-]+", room);
				if (!isEmail) {
					Toast.makeText(context, "Invalid room name.",
							Toast.LENGTH_SHORT).show();
					return;
				}
				room += "@conference." + BeemService.DEFAULT_XMPP_SERVICE;
				String pseudo = mEditTextPseudo.getText().toString();
				String nick = mSettings.getString(
						BeemApplication.ACCOUNT_USERNAME_KEY, "").trim();
				if (pseudo == null)
					pseudo = nick;
				else if (pseudo.length() == 0)
					pseudo = nick;
				Contact c = new Contact(room, true);
				Intent i = new Intent(context, Chat.class);
				i.setData(c.toUri(pseudo));
				context.startActivity(i);
			}
		}
	}
}
