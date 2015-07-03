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
