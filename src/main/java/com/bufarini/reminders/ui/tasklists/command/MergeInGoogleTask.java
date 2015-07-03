package com.bufarini.reminders.ui.tasklists.command;

import java.io.IOException;

import com.bufarini.reminders.model.Item;
import com.google.api.services.tasks.Tasks;

public class MergeInGoogleTask extends GoogleTaskCommand {
	public MergeInGoogleTask(Tasks googleService, Item item) {
		super(googleService, item);
	}

	@Override
	public void doExecute(Tasks googleService, Item item) throws IOException {
		item.merge(googleService);
	}
}
