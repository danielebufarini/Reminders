package com.bufarini.reminders.ui.tasklists;

import android.content.Context;
import android.util.Log;

import com.bufarini.reminders.BusProvider;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;
import com.bufarini.reminders.ui.Reminders;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class LoadItemsFromGoogle implements Runnable {
	public static final String LOGTAG = LoadItemsFromGoogle.class.getSimpleName();
	private static final Bus bus = BusProvider.getInstance();
	private final List<GTaskList> googleItems;
    private final CyclicBarrier barrier;
    private final Tasks googleService;
    private final String accountName;

    public LoadItemsFromGoogle(final Context context, final CyclicBarrier barrier, List<GTaskList> googleItems, String accountName) {
    	bus.register(this);
        this.barrier = barrier;
        this.googleItems = googleItems;
        this.accountName = accountName;
        this.googleService = Reminders.getGoogleTasksService(context, accountName);
    }
    
    private List<GTask> getTasks(GTaskList list) throws IOException {
    	List<GTask> gtasks = new ArrayList<GTask>(20);
    	com.google.api.services.tasks.model.Tasks tasks = googleService.tasks().list(list.googleId).execute();
    	if (tasks.getItems() != null)
    		for (com.google.api.services.tasks.model.Task task : tasks.getItems()) {
    			GTask gtask = new GTask();
    			gtask.googleId = task.getId();
    			gtask.title = task.getTitle();
    			gtask.updated = task.getUpdated().getValue();
    			gtask.notes = task.getNotes();
    			gtask.completed = task.getCompleted() != null ? task.getCompleted().getValue() : 0;
    			gtask.dueDate = task.getDue() != null ? task.getDue().getValue() : 0;
    			gtask.isDeleted = task.getDeleted() != null ? task.getDeleted() : false;
    			gtask.list = list;
    			gtask.accountName = accountName;
    			gtasks.add(gtask);
    			Log.d(LOGTAG, "google :: downloaded task " + gtask + " for list " + list);
    		}
		return gtasks;
    }

    @Override
    public void run() {
    	TaskLists taskLists;
		try {
			taskLists = googleService.tasklists().list().execute();
			for (TaskList taskList : taskLists.getItems())
				try {
					GTaskList list = new GTaskList();
					list.googleId = taskList.getId();
					list.title = taskList.getTitle();
					list.updated = taskList.getUpdated().getValue();
					list.accountName = accountName;
					list.tasks = getTasks(list);
					googleItems.add(list);
					Log.d(LOGTAG, "google :: downloaded list " + list);
				} catch (Exception e) {
					Log.e(LOGTAG, "google :: cannot retrive task from google servers for account +\""
							+ accountName + "\"", e);
				}
			try {
				barrier.await();
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "", e);
			} catch (BrokenBarrierException e) {
				Log.e(LOGTAG, "", e);
			}
		} catch (Exception e) {
			//TODO java.net.SocketTimeoutException: SSL handshake timed out
			//TODO java.net.ConnectException
			Log.e(LOGTAG, "google :: cannot retrive lists from google servers for account \""
				+ accountName + "\"", e);
		}
		Log.d(LOGTAG, "google :: loaded '" + googleItems.size() + "' items.");
    }
}
