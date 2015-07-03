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

package com.bufarini.reminders.ui.tasklists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.bufarini.reminders.db.RemindersDbHelper;
import com.bufarini.reminders.db.Tables;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;

public class LoadItemsFromDB implements Runnable {
	public static final String LOGTAG = LoadItemsFromDB.class.getSimpleName();
	private List<GTaskList> dbItems;
	private RemindersDbHelper dbHelper;
    private CyclicBarrier barrier;
    private final String accountName;

    public LoadItemsFromDB(final Context context, final CyclicBarrier barrier, List<GTaskList> dbItems,
    	String accountName)
    {
        this.barrier = barrier;
        this.dbItems = dbItems;
        this.dbHelper = new RemindersDbHelper(context);
        this.accountName = accountName;
    }

    public List<GTask> getTasks(GTaskList list) {
    	List<GTask> gtasks = new ArrayList<GTask>(20);
    	SQLiteDatabase db = dbHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery(
    		String.format("select * from %s where %s=%s and %s=\"%s\"", Tables.TASK_TABLE,
    			Tables.LIST_ID, list.id, Tables.ACCOUNT_NAME, accountName), null);
    	try {
        	while (cursor.moveToNext()) {
    			GTask gtask = new GTask();
    			gtask.id = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.ID));
    			gtask.googleId = cursor.getString(cursor.getColumnIndexOrThrow(Tables.GTASK_ID));
    			gtask.title = cursor.getString(cursor.getColumnIndexOrThrow(Tables.TITLE));
    			gtask.updated = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.UPDATED));
    			gtask.notes = cursor.getString(cursor.getColumnIndexOrThrow(Tables.NOTES));
    			gtask.completed = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.COMPLETED));
    			gtask.dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.DUE_DATE));
    			gtask.reminderDate = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.REMINDER_DATE));
    			gtask.reminderInterval = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.REMINDER_INTERVAL));
    			gtask.isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.DELETED)) != 0;
    			gtask.isMerged = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.MERGED)) != 0;
    			gtask.accountName = cursor.getString(cursor.getColumnIndexOrThrow(Tables.ACCOUNT_NAME));
                gtask.priority = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.PRIORITY));
    			gtask.isStored = true;
    			gtask.list = list;
    			gtasks.add(gtask);
    			Log.d(LOGTAG, "db :: downloaded task " + gtask + " for list " + list);
    		}
    	} finally {
        	cursor.close();
        	db.close();
        }
		return gtasks;
    }
    
    @Override
    public void run() {
    	SQLiteDatabase db = dbHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery(
    		String.format("select * from %s where %s=\"%s\"", Tables.LIST_TABLE,
    			Tables.ACCOUNT_NAME, accountName),
    		null
    	);
    	while (cursor.moveToNext()) {
    		GTaskList list = new GTaskList();
    		list.id = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.ID));
    		list.title = cursor.getString(cursor.getColumnIndexOrThrow(Tables.TITLE));
    		list.updated = cursor.getLong(cursor.getColumnIndexOrThrow(Tables.UPDATED));
    		list.googleId = cursor.getString(cursor.getColumnIndexOrThrow(Tables.GTASK_ID));
    		list.isMerged = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.MERGED)) != 0;
    		list.isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.DELETED)) != 0;
    		list.accountName = cursor.getString(cursor.getColumnIndexOrThrow(Tables.ACCOUNT_NAME));
    		list.isHideCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.HIDE_COMPLETED)) != 0;
    		list.isSortedByDueDate = cursor.getInt(cursor.getColumnIndexOrThrow(Tables.SORT_BY_DUE_DATE)) != 0;
    		list.isStored = true;
    		list.tasks = getTasks(list);
    		dbItems.add(list);
    		Log.d(LOGTAG, "db :: loaded list " + list);
    	}
        try {
        	Log.d(LOGTAG, "db :: loaded '" + dbItems.size() + "' items.");
            barrier.await();
        } catch (Exception e) {
        	Log.e(LOGTAG, "", e);
        } finally {
        	cursor.close();
        	db.close();
        }
    }
}