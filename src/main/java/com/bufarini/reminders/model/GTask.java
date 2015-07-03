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

package com.bufarini.reminders.model;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.bufarini.reminders.db.Tables;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GTask extends Item implements Comparable<GTask>, Serializable {
    private static final long serialVersionUID = 987654321L;

	public static final String SEPARATOR = "::";
	public static final String NOTE_TAG = " " + SEPARATOR + " note ";
	public static final String DUE_TAG = " " + SEPARATOR + " due on ";
	public static final String REMINDER_TAG = " " + SEPARATOR + " reminder for ";
	public static final String INTERVAL_TAG = " " + SEPARATOR + " interval ";
	public static final SimpleDateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
	public static final SimpleDateFormat REMINDER_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

	private static final String TASK_COMPLETED = "completed", NEEDS_ACTION = "needsAction";
	private static final String LOGTAG = GTask.class.getSimpleName();
	
	public String category, notes;
	public long completed, dueDate, reminderDate, reminderInterval;
	public int priority, level;
	public GTaskList list;
	
	public GTask() {
		super();
	}

    public GTask(long id) {
        super(id);
    }

    public GTask(GTask that) {
        super(that);
		this.category = that.category;
		this.notes = that.notes;
		this.completed = that.completed;
		this.dueDate = that.dueDate;
		this.reminderDate = that.reminderDate;
		this.reminderInterval = that.reminderInterval;
		this.priority = that.priority;
		this.level = that.level;
		this.list = that.list;
	}

	@Override
	public String toString() {
		return "GTask[id = \"" + id + "\" :: googleId = \"" + googleId + "\""
			+ (title != null ? " :: title = \"" + title + "\"" : "") + "]";
	}
	
	@Override
	public int compareTo(GTask that) {
		return this.dueDate < that.dueDate ? -1 : (this.dueDate == that.dueDate ? 0 : 1);
	}

	private ContentValues getValues() {
		ContentValues values = new ContentValues();
		values.put(Tables.ID, id);
		values.put(Tables.TITLE, title);
		values.put(Tables.DELETED, isDeleted);
		values.put(Tables.GTASK_ID, googleId);
		values.put(Tables.CATEGORY, category);
		values.put(Tables.NOTES, notes);
		values.put(Tables.COMPLETED, completed);
		values.put(Tables.UPDATED, updated);
		values.put(Tables.PRIORITY, priority);
		values.put(Tables.LEVEL, level);
		values.put(Tables.DUE_DATE, dueDate);
		values.put(Tables.REMINDER_DATE, reminderDate);
		values.put(Tables.REMINDER_INTERVAL, reminderInterval);
		values.put(Tables.MERGED, isMerged);
		values.put(Tables.LIST_ID, list.id);
		values.put(Tables.ACCOUNT_NAME, accountName);
		return values;
	}
	
	@Override
	public void insert(SQLiteDatabase db) {
   		db.insert(Tables.TASK_TABLE, null, getValues());
   		Log.d(LOGTAG, "db :: inserted task " + this + " in list " + list);
	}

	@Override
	public void delete(SQLiteDatabase db) {
		db.delete(Tables.TASK_TABLE,
			Tables.ID + "=?", new String[] {Long.toString(id)});
		Log.d(LOGTAG, "db :: deleted task " + this + " in list " + list);
	}

	@Override
	public void merge(SQLiteDatabase db) {
		db.update(Tables.TASK_TABLE, getValues(), Tables.ID + "=?", new String[] {Long.toString(id)});
     	Log.d(LOGTAG, "db :: updated task " + this + " in list " + list);
	}

	private Task newTask() {
		Task task = new Task();
		task.setTitle(title);
		task.setNotes(notes);
		task.setDeleted(isDeleted);
		task.setUpdated(new DateTime(updated));
		task.setStatus(completed != 0 ? TASK_COMPLETED : NEEDS_ACTION);
		if (dueDate > 0)
			task.setDue(new DateTime(dueDate));		
		return task;
	}
	
	@Override
	public void insert(Tasks googleService) throws IOException {
		Task task = newTask();
		Task newTask = googleService.tasks().insert(list.googleId, task).execute();
		googleId = newTask.getId();
		Log.d(LOGTAG, "google :: inserted task " + this + " in list " + list);
	}
	
	@Override
	public void delete(Tasks googleService) throws IOException {
		googleService.tasks().delete(list.googleId, googleId).execute();
		Log.d(LOGTAG, "google :: deleted task " + this + " in list " + list);
	}
	
	@Override
	public void merge(Tasks googleService) throws IOException {
		Task task = newTask();
		task.setId(googleId);
		googleService.tasks().update(list.googleId, task.getId(), task).execute();
		Log.d(LOGTAG, "google :: updated task " + this + " in list " + list);
	}

	@Override
	public boolean hasChildren() {
		return false;
	}
	
	@Override
	public List<? extends Item> getChildren() {
		return EMPTY_LIST;
	}

	@Override
	public void setChildren(List<? extends Item> items) {
		// Do nothing
	}

	@Override
	public boolean hasReminder() {
		return true;
	}

	@Override
	public long getReminder() {
		return reminderDate;
	}

	@Override
	public void setReminder(long reminder) {
		this.reminderDate = reminder;
	}

	@Override
	public long getReminderInterval() {
		return reminderInterval;
	}

	@Override
	public void setReminderInterval(long interval) {
		this.reminderInterval = interval;
	}
	
	private String extractTagValue(final String source, final int i, final String tag) {
		String result = null;
		if (i > 0) {
			final int ii = source.indexOf(SEPARATOR, i + SEPARATOR.length());
			if (ii > 0)
				result = source.substring(i + tag.length(), ii);
			else
				result = source.substring(i + tag.length());
		}
		if (result != null)
			result = result.trim();
		return result;
	}
	
	public void parse(final String str) {
		final int i = str.indexOf(DUE_TAG);
		final int j = str.indexOf(REMINDER_TAG);
		final int k = str.indexOf(NOTE_TAG);
		final int l = str.indexOf(INTERVAL_TAG);
		if (i == -1 && j == -1 && k == -1 && l == -1) {
			title = str;
		} else {
			title = str.substring(0, str.indexOf(SEPARATOR));
			String tmp = extractTagValue(str, k, NOTE_TAG);
			if (tmp != null && !tmp.equals(""))
				notes = tmp;
			try {
				tmp = extractTagValue(str, i, DUE_TAG);
				if (tmp != null && !tmp.equals(""))
					dueDate = DUE_DATE_FORMAT.parse(tmp).getTime();
			} catch (ParseException e) {
				Log.d(LOGTAG, "error parsing due date", e);
			}
			try {
				tmp = extractTagValue(str, j, REMINDER_TAG);
				if (tmp != null && !tmp.equals(""))
					reminderDate = REMINDER_DATE_FORMAT.parse(tmp).getTime();
			} catch (ParseException e) {
				Log.d(LOGTAG, "error parsing reminder date", e);
			}
			try {
				tmp = extractTagValue(str, l, INTERVAL_TAG);
				if (tmp != null && !tmp.equals(""))
					reminderInterval = Long.parseLong(tmp);
			} catch (NumberFormatException e) {
				Log.d(LOGTAG, "error parsing reminder interval", e);
			}
		}
	}
}
