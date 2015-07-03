/*
Copyright 2015 Daniele Bufarini

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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
