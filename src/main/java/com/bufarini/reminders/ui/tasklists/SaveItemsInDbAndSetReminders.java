package com.bufarini.reminders.ui.tasklists;

import android.content.Context;
import com.bufarini.reminders.NotificationUtils;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;

import java.util.List;

public class SaveItemsInDbAndSetReminders implements Runnable {
	private final SaveItems saveItems;
	private final List<GTaskList> lists;
	private final Context context;
	
	public SaveItemsInDbAndSetReminders(Context context, List<GTaskList> lists) {
		this.lists = lists;
		this.saveItems = new SaveItems(context, false, "", lists);
		this.context = context;
	}

	@Override
	public void run() {
		saveItems.run();
		for (GTaskList list: lists)
			for (GTask task: list.tasks)
				if (task.reminderDate > 0)
					NotificationUtils.setReminder(context, task);
	}

}
