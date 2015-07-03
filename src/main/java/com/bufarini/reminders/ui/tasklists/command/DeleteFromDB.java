package com.bufarini.reminders.ui.tasklists.command;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.model.Item;

import android.database.sqlite.SQLiteDatabase;

public class DeleteFromDB implements Command {
	private RemindersDbHelper dbHelper;
	private Item item;
	
	public DeleteFromDB(RemindersDbHelper dbHelper, Item item) {
		this.dbHelper = dbHelper;
		this.item = item;
	}

	@Override
	public void execute() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			item.delete(db);
		} finally {
			db.close();
		}
	}
}
