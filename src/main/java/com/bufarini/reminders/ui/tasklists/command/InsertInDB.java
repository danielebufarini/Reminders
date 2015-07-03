package com.bufarini.reminders.ui.tasklists.command;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.model.Item;

import android.database.sqlite.SQLiteDatabase;

public class InsertInDB extends DBCommand {
	public InsertInDB(RemindersDbHelper database, Item item) {
		super(database, item);
	}

	@Override
	public void doExecute(SQLiteDatabase db, Item item) {
        item.insert(db);
        item.isStored = true;
    }
}
