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

import android.util.Log;

import com.bufarini.reminders.model.Item;
import com.google.api.services.tasks.Tasks;

abstract class GoogleTaskCommand implements Command {
	private static final String LOGTAG = GoogleTaskCommand.class.getSimpleName();
	private Tasks googleService;
	private Item item;

	public GoogleTaskCommand(Tasks googleService, Item item) {
		this.googleService = googleService;
		this.item = item;
	}

	@Override
	final public void execute() {
		try {
			doExecute(googleService, item);
		} catch (IOException e) {
			item.updated = System.currentTimeMillis(); // force update for the next sync
			Log.e(LOGTAG, "error executing command for " + item, e);
		}
	}

	abstract public void doExecute(Tasks googleService, Item item) throws IOException;
}