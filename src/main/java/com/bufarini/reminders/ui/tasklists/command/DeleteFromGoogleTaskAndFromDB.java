package com.bufarini.reminders.ui.tasklists.command;

import java.io.IOException;

import android.database.sqlite.SQLiteDatabase;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.model.Item;
import com.google.api.services.tasks.Tasks;

public class DeleteFromGoogleTaskAndFromDB implements Command {
	private RemindersDbHelper dbHelper;
	private Tasks googleService;
	private Item item;
	
	public DeleteFromGoogleTaskAndFromDB(RemindersDbHelper dbHelper, Tasks googleService, Item item) {
		this.dbHelper = dbHelper;
		this.googleService = googleService;
		this.item = item;
	}

	@Override
	public void execute() {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			if (item.googleId != null && item.googleId.length() > 0)
				item.delete(googleService);
			if (item.isStored)
				item.delete(db);
		} catch (IOException e) {
			if (item.isStored)
				item.merge(db);
		} finally {
			db.close();
		}
	}
}
