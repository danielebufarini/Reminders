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

package com.bufarini.reminders.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Tables {
	public static final String TASK_TABLE = "tasks";
	public static final String ID = "_id";
	public static final String TITLE = "title";
	public static final String UPDATED = "updated_date";
	public static final String DELETED = "deleted";
	public static final String GTASK_ID = "gtask_id";
	public static final String CATEGORY = "category";
	public static final String NOTES = "notes";
	public static final String COMPLETED = "completed";
	public static final String PRIORITY = "priority";
	public static final String LEVEL = "level";
	public static final String DUE_DATE = "due_date";
	public static final String REMINDER_DATE = "reminder_date";
	public static final String REMINDER_INTERVAL = "reminder_interval";
	public static final String MERGED = "merged";
	public static final String LIST_ID = "list_id";
	public static final String ACCOUNT_NAME = "account_name";
	public static final String HIDE_COMPLETED = "hide_completed";
	public static final String SORT_BY_DUE_DATE = "order_by_date";
	
	private static final String ITEM_COLUMNS =
			ID + " integer primary key autoincrement,"
			+ TITLE + " text not null,"
			+ UPDATED + " integer not null,"
			+ GTASK_ID + " text not null,"
			+ MERGED + " integer not null,"
			+ DELETED + " tinyint not null,"
			+ ACCOUNT_NAME + " text";
	
	private static final String CREATE_TASK_TABLE =
			"create table " + TASK_TABLE
			+ "("
			+ ITEM_COLUMNS + ","
			+ CATEGORY + " text null,"
			+ NOTES + " text null,"
			+ COMPLETED + " integer not null,"
			+ PRIORITY + " tinyint not null,"
			+ LEVEL + " integer not null,"
			+ DUE_DATE + " integer not null,"
			+ REMINDER_DATE + " integer,"
			+ REMINDER_INTERVAL + " integer,"
			+ LIST_ID + " integer not null"
			+ ");";
	
	public static final String LIST_TABLE = "task_lists";
	private static final String CREATE_LIST_TABLE =
			"create table " + LIST_TABLE
			+ "("
			+ ITEM_COLUMNS + ","
			+ HIDE_COMPLETED + " integer null,"
			+ SORT_BY_DUE_DATE + " integer null"
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_TASK_TABLE);
		database.execSQL(CREATE_LIST_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Log.w(Tables.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE);
		onCreate(database);
	}

	private void upgradeDB() {
		// TODO add upgrade logic
	}
}
