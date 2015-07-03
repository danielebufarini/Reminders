package com.bufarini.reminders.ui.tasklists.command;

import java.io.IOException;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.model.Item;
import com.google.api.services.tasks.Tasks;

public class InsertInGoogleTaskAndMergeInDB implements Command {
	private static final String TAG = InsertInGoogleTaskAndMergeInDB.class.getSimpleName();
	private RemindersDbHelper dbHelper;
	private Tasks googleService;
	private Item item;
	
	public InsertInGoogleTaskAndMergeInDB(RemindersDbHelper dbHelper, Tasks googleService, Item item) {
		this.dbHelper = dbHelper;
		this.googleService = googleService;
		this.item = item;
	}

	@Override
	public void execute() {
		try {
			item.isMerged = true;
			item.insert(googleService);
		} catch (IOException e) {
			item.isMerged = false;
			Log.w(TAG, "", e);
		} 
		SQLiteDatabase db = null;
		try {
			if (item.isStored && (item.isMerged == true || item.isModified)) {
				db = dbHelper.getWritableDatabase();
				item.merge(db);
			}
		} finally {
			if (db != null)
				db.close();
		}
	}
}
