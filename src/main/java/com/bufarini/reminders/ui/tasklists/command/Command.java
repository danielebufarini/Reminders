package com.bufarini.reminders.ui.tasklists.command;

public interface Command {
	public void execute(); // may throws GoogleIOException (runtime exception)
}
