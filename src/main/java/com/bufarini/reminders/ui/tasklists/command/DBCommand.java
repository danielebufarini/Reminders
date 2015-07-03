package com.bufarini.reminders.ui.tasklists.command;

import android.database.sqlite.SQLiteDatabase;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.model.Item;

public abstract class DBCommand implements Command {
	private RemindersDbHelper dbHelper;
	private Item item;

	public DBCommand(RemindersDbHelper dbHelper, Item item) {
		this.dbHelper = dbHelper;
		this.item = item;
	}

	@Override
	final public void execute() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			doExecute(db, item);
		} finally {
			db.close();
		}
	}

	public abstract void doExecute(SQLiteDatabase db, Item item);
}
