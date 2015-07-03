package com.bufarini.reminders.contentprovider;

import java.util.Arrays;
import java.util.HashSet;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.db.Tables;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class RemindersContentProvider extends ContentProvider {

	// database
	private RemindersDbHelper database;

	// Used for the UriMacher
	private static final int TODOS = 10;
	private static final int TODO_ID = 20;

	public static final String AUTHORITY = RemindersContentProvider.class.getPackage().getName();

	public static final String BASE_PATH = "todos";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/todos";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/todo";

	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BASE_PATH, TODOS);
		sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", TODO_ID);
	}

	@Override
	public boolean onCreate() {
		database = new RemindersDbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// Check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(Tables.TASK_TABLE);

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TODOS:
			break;
		case TODO_ID:
			// Adding the ID to the original query
			queryBuilder.appendWhere(Tables.ID + "=" + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		// Make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long id = 0;
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
			case TODOS:
				SQLiteDatabase sqlDB = database.getWritableDatabase();
				id = sqlDB.insert(Tables.TASK_TABLE, null, values);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return ContentUris.withAppendedId(CONTENT_URI, id);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case TODOS:
			rowsDeleted = sqlDB.delete(Tables.TASK_TABLE, selection, selectionArgs);
			break;
		case TODO_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(Tables.TASK_TABLE, Tables.ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(Tables.TASK_TABLE, Tables.ID + "=" + id + " and "
						+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
			case TODOS:
				rowsUpdated = sqlDB.update(Tables.TASK_TABLE, values, selection, selectionArgs);
				break;
			case TODO_ID:
				String id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(Tables.TASK_TABLE, values, Tables.ID + "=" + id, null);
				} else {
					rowsUpdated = sqlDB.update(Tables.TASK_TABLE, values, Tables.ID + "=" + id
							+ " and " + selection, selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

	private void checkColumns(String[] projection) {
		String[] available = { Tables.CATEGORY, Tables.TITLE,
				Tables.NOTES, Tables.ID };
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			// Check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}

}
