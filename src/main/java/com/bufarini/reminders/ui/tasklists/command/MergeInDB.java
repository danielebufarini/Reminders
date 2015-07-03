package com.bufarini.reminders.ui.tasklists.command;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.model.Item;

import android.database.sqlite.SQLiteDatabase;

public class MergeInDB extends DBCommand {
	public MergeInDB(RemindersDbHelper database, Item item) {
		super(database, item);
	}

	@Override
	public void doExecute(SQLiteDatabase db, Item item) {
		item.merge(db);
	}
}
