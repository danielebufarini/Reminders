package com.bufarini.reminders.model;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.bufarini.reminders.db.Tables;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GTaskList extends Item implements Serializable {
	private static final String LOGTAG = GTaskList.class.getSimpleName();
    private static final long serialVersionUID = 1234567890L;

	public transient List<GTask> tasks;
	public boolean isHideCompleted = false, isSortedByDueDate = true;
	
	public GTaskList() {
		super();
	}

    public GTaskList(long id) {
        super(id);
    }

	public GTaskList(GTaskList that) {
        super(that);
		this.tasks = new ArrayList<GTask>(that.tasks.size());
		for (GTask task: that.tasks)
			this.tasks.add(new GTask(task));
		this.isHideCompleted = that.isHideCompleted;
		this.isSortedByDueDate = that.isSortedByDueDate;
	}
	
	@Override
	public String toString() {
		return "GTaskList[id = \"" + id + "\" :: googleId = \"" + googleId + "\""
			+ (" :: title = \"" + title + "\"") + "]";
	}
	
	private ContentValues getValues() {
		ContentValues values = new ContentValues();
		values.put(Tables.ID, id);
		values.put(Tables.TITLE, title);
		values.put(Tables.DELETED, isDeleted);
		values.put(Tables.GTASK_ID, googleId);
		values.put(Tables.UPDATED, updated);
		values.put(Tables.MERGED, isMerged);
		values.put(Tables.ACCOUNT_NAME, accountName);
		values.put(Tables.HIDE_COMPLETED, isHideCompleted);
		values.put(Tables.SORT_BY_DUE_DATE, isSortedByDueDate);
		return values;
	}

	@Override
	public void insert(SQLiteDatabase db) {
		ContentValues values = getValues();
	   	db.insert(Tables.LIST_TABLE, null, values);
	   	Log.d(LOGTAG, "db :: inserted list " + this);
	}

	@Override
	public void delete(SQLiteDatabase db) {
		db.delete(Tables.LIST_TABLE,
			Tables.ID + "=?", new String[] {Long.toString(id)});
		db.delete(Tables.TASK_TABLE,
    		Tables.LIST_ID + "=?", new String[] {Long.toString(id)});
		Log.d(LOGTAG, "db :: deleted list " + this);
	}

	@Override
	public void merge(SQLiteDatabase db) {
		ContentValues values = getValues();
		db.update(Tables.LIST_TABLE, values, Tables.ID + "=?",
			new String[] {Long.toString(id)});
		Log.d(LOGTAG, "db :: updated list " + this);
	}

	private TaskList newTaskList() {
		TaskList taskList = new TaskList();
		taskList.setTitle(title);
		taskList.setUpdated(new DateTime(updated));
		return taskList;
	}
	
	@Override
	public void insert(Tasks googleService) throws IOException {
		TaskList taskList = googleService.tasklists().insert(newTaskList()).execute();
		googleId = taskList.getId();
		Log.d(LOGTAG, "google :: inserted list " + this);
	}
	
	@Override
	public void delete(Tasks googleService) throws IOException {
		googleService.tasklists().delete(googleId).execute();
		Log.d(LOGTAG, "google :: deleted list " + this);
	}
	
	@Override
	public void merge(Tasks googleService) throws IOException {
		TaskList taskList = newTaskList();
		taskList.setId(googleId);
		googleService.tasklists().update(googleId, taskList).execute();
		Log.d(LOGTAG, "google :: updated list " + this);
	}

	@Override
	public boolean hasChildren() {
		return true;
	}
	
	@Override
	public List<? extends Item> getChildren() {
		return tasks;
	}

	@Override
	public void setChildren(List<? extends Item> items) {
		tasks = (List<GTask>)items;
	}

	public boolean hasReminder() {
		return false;
	}

	public long getReminder() {
		return 0;
	}

	public void setReminder(long reminder) {
	}

	public long getReminderInterval() {
		return 0;
	}

	public void setReminderInterval(long interval) {
	}
}
