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

package com.bufarini.reminders.ui;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageButton;

import com.bufarini.reminders.ui.tasklists.command.Command;

public class AsyncAuthorisation extends AsyncTask<Command, Void, Void> {
	private final ImageButton syncButton;
	
	public AsyncAuthorisation(ImageButton syncButton) {
		this.syncButton = syncButton;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	
	@Override
	protected Void doInBackground(Command... commands) {
		for (Command command: commands)
			command.execute();
		return null;
	}

	@Override
	protected void onPostExecute(Void noparams) {
		super.onPostExecute(noparams);
		syncButton.setVisibility(View.VISIBLE);
	}
}
