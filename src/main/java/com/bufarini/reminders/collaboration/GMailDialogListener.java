package com.bufarini.reminders.collaboration;

import java.util.List;

public interface GMailDialogListener {
	public void importEmailsAsTasks(boolean isToBeDeleted, List<MailEntry> mailEntries);
}
