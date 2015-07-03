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
