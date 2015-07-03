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
