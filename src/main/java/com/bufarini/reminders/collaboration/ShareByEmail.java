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

package com.bufarini.reminders.collaboration;

import android.util.Log;
import com.bufarini.reminders.collaboration.OAuth2Authenticator.SMTPTransportInfo;
import com.bufarini.reminders.model.GTask;
import com.bufarini.reminders.model.GTaskList;
import com.bufarini.reminders.ui.tasklists.ListManager.TasksListInfo;

import javax.activation.DataHandler;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static com.bufarini.reminders.model.GTask.*;
import static com.bufarini.reminders.ui.tasklists.ListManager.TASKS_SEPARATOR_IN_EMAIL;

public class ShareByEmail implements Runnable {
	private static final String LOGTAG = ShareByEmail.class.getSimpleName();
	private final String accountName, oAuth2Token;
	private final BlockingQueue<TasksListInfo> queue;
	
	public ShareByEmail(String accountName, String oAuth2Token, BlockingQueue<TasksListInfo> queue) {
		this.queue = queue;
		this.accountName = accountName;
		this.oAuth2Token = oAuth2Token;
	}
	
	@Override
	public void run() {
		try {
			OAuth2Authenticator.initialize();
			SMTPTransportInfo smtpTransportInfo =
				OAuth2Authenticator.connectToSmtp("smtp.gmail.com", 587, accountName, oAuth2Token, false);
			while (true) {
				if (smtpTransportInfo.smtpTransport.isConnected())
					sendEmail(smtpTransportInfo, queue.take());
				else {
					Thread.sleep(1000L);
					smtpTransportInfo =
						OAuth2Authenticator.connectToSmtp("smtp.gmail.com", 587, accountName, oAuth2Token, false);
				}
			}
		} catch (InterruptedException e) {
			// Nothing to do, we have to simply quit this thread
		} catch (Exception e) {
			Log.e(LOGTAG, "", e);
		}
	}

	private byte[] getEmailBodyFromTasks(List<GTask> tasks) {
		StringBuilder body = new StringBuilder(2048);
		Date dueDate = new Date();
		for (GTask task: tasks) {
			body.append(TASKS_SEPARATOR_IN_EMAIL).append(' ').append(task.title);
			if (task.dueDate > 0) {
				dueDate.setTime(task.dueDate);
				body.append(DUE_TAG).append(DUE_DATE_FORMAT.format(dueDate));
			}
			if (task.reminderDate > 0) {
				dueDate.setTime(task.reminderDate);
				body.append(REMINDER_TAG).append(REMINDER_DATE_FORMAT.format(dueDate));
				if (task.reminderInterval > 0)
					body.append(INTERVAL_TAG).append(task.reminderInterval);
			}
			if (task.notes != null && !task.notes.equals(""))
				body.append(NOTE_TAG).append(task.notes);
			body.append("\r\n");
		}
		return body.toString().getBytes();
	}
	
	private void sendEmail(SMTPTransportInfo smtpTransportInfo, TasksListInfo info) throws Exception {
		List<GTaskList> lists = info.lists;
		for (GTaskList list : lists) {
			MimeMessage message = new MimeMessage(smtpTransportInfo.session);
	        message.setSender(new InternetAddress(accountName));   
	        message.setSubject(GMailImapReader.LIST_PREFIX_SHARED + list.title);   
	        DataHandler handler = new DataHandler(
	        	new ByteArrayDataSource(getEmailBodyFromTasks(list.tasks), "text/plain"));   
	        message.setDataHandler(handler);   
	        message.setRecipient(javax.mail.Message.RecipientType.TO,
	        	new InternetAddress(info.recipientEmailAddress));   
	        smtpTransportInfo.smtpTransport.sendMessage(message, message.getAllRecipients());
		}
        smtpTransportInfo.smtpTransport.close();
	}
}
