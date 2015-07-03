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
