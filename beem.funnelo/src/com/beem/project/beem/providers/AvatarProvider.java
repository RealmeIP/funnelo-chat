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
package com.beem.project.beem.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A simple content provider we expose the differents avatar downloaded.
 *
 */
public class AvatarProvider extends ContentProvider {

    /** The content uri of this provider. */
    public static final Uri CONTENT_URI =
	Uri.parse("content://com.beem.project.beem.providers.avatarprovider");

    private static final String TAG = AvatarProvider.class.getSimpleName();
    private static final String AUTHORITY = "com.beem.project.beem.providers.avatarprovider";

    /**
     * The differents columns available in the AvatarProvider.
     */
    public static interface Columns {

	/** The id of the avatar. */
	String ID = "_id";
    }

    private static String[] columnNames = new String[] {Columns.ID };

    private static final int AVATAR = 1;
    private static final int AVATAR_ID = 2;
    private static final UriMatcher URIMATCHER = new UriMatcher(AVATAR);

    static
    {
        URIMATCHER.addURI(AUTHORITY, "*", AVATAR_ID);
	// should not be needed if we pass AVATAR on the constructor but it does not work
        URIMATCHER.addURI(AUTHORITY, null, AVATAR);
    }

    private String mDataPath;

    /**
     * Create an AvatarProvider.
     */
    public AvatarProvider() {
    }

    /**
     * Translate the mode passed to {@link #openFile} into mode passed to {@link ParcelFileDescriptor#open}.
     *
     * @param uri the uri to open
     * @param mode the mode
     * @return the mode
     * @throws FileNotFoundException if the mode passed is illegal
     */
    public static int modeToMode(Uri uri, String mode) throws FileNotFoundException {
	int modeBits;
	if ("r".equals(mode)) {
	    modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
	} else if ("w".equals(mode) || "wt".equals(mode)) {
	    modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
		| ParcelFileDescriptor.MODE_CREATE
		| ParcelFileDescriptor.MODE_TRUNCATE;
	} else if ("wa".equals(mode)) {
	    modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
		| ParcelFileDescriptor.MODE_CREATE
		| ParcelFileDescriptor.MODE_APPEND;
	} else if ("rw".equals(mode)) {
	    modeBits = ParcelFileDescriptor.MODE_READ_WRITE
		| ParcelFileDescriptor.MODE_CREATE;
	} else if ("rwt".equals(mode)) {
	    modeBits = ParcelFileDescriptor.MODE_READ_WRITE
		| ParcelFileDescriptor.MODE_CREATE
		| ParcelFileDescriptor.MODE_TRUNCATE;
	} else {
	    throw new FileNotFoundException("Bad mode for " + uri + ": "
		    + mode);
	}
	return modeBits;
    }

    @Override
    public boolean onCreate() {
	File cacheDir = Environment.getExternalStorageDirectory();
	File dataPath = new File(cacheDir, "/Android/data/com.beem.project.beem/cache/avatar");
	dataPath.mkdirs();
	mDataPath = dataPath.getAbsolutePath();
	return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
	throws FileNotFoundException {
	String id = uri.getPath();
	File data = new File(mDataPath, id);
	int modeBits = AvatarProvider.modeToMode(uri, mode);
	return ParcelFileDescriptor.open(data, modeBits);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
	MatrixCursor c = new MatrixCursor(columnNames);
	int match = URIMATCHER.match(uri);
	switch (match) {
	    case AVATAR:
		File[] files = new File(mDataPath).listFiles();
		if (files != null) {
		    for (File f : files) {
			c.newRow().add(f.getName());
		    }
		}
		break;
	    case AVATAR_ID:
		String id = uri.getPathSegments().get(0);
		File f = new File(mDataPath, id);
		if (f.exists())
			c.newRow().add(f.getName());
		break;
	    default:
		Log.w(TAG, "Unsupported uri for query match = " + match);
	}
	return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
	return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
	int res = 0;
	String id = uri.getPath();
	File data = new File(mDataPath, id);
	if (data.exists() && data.delete()) {
	    res++;
	}
	return res;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
	return null;
    }

    @Override
    public String getType(Uri uri) {
	return null;
    }
}
