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
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPFolder.ProtocolCommand;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.ListInfo;

import javax.mail.Folder;
import javax.mail.MessagingException;
import java.util.concurrent.BlockingQueue;

public class DeleteEmailViaImap implements Runnable {
	private static final String TRASH_FOLDER_ENGLISH_NAME = "[Gmail]/Trash", TRASH_FOLDER_NAME = "\\Trash";
	private static final String LOGTAG = DeleteEmailViaImap.class.getSimpleName();
	private final String accountName, oAuth2Token;
	private final BlockingQueue<Long> queue;
	
	public DeleteEmailViaImap(String accountName, String oAuth2Token, BlockingQueue<Long> queue) {
		this.queue = queue;
		this.accountName = accountName;
		this.oAuth2Token = oAuth2Token;
	}
	
	@Override
	public void run() {
		IMAPStore store = null;
		try {
			OAuth2Authenticator.initialize();
			store = OAuth2Authenticator.connectToImap("imap.gmail.com", 993, accountName, oAuth2Token, false);
			while (true)
				deleteEmail(store, queue.take());
		} catch (InterruptedException e) {
			// Nothing to do, we have to simply quit this thread
		} catch (Exception e) {
			Log.e(LOGTAG, "", e);
		} finally {
			try {
				if (store != null)
					store.close();
			} catch (MessagingException e) {
				Log.e(LOGTAG, "", e);
			}
		}
	}
	
	private String getTrashFolderLocalisedName(final IMAPFolder inbox) throws MessagingException {
		String trashFolderName = null;
		ListInfo[] li = null;
		li = (ListInfo[]) inbox.doCommand(new ProtocolCommand() {
		    public Object doCommand(IMAPProtocol p) throws ProtocolException {
			return p.list("", "*");
		    }
		});
		for (ListInfo info: li)
			if (info.attrs.length == 2 && TRASH_FOLDER_NAME.equals(info.attrs[1])) {
				trashFolderName = info.name;
				break;
			}
		return (trashFolderName == null ? TRASH_FOLDER_ENGLISH_NAME : trashFolderName);
	}
	
	private void deleteEmail(final IMAPStore store, long uid) throws Exception {
		IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
		inbox.open(Folder.READ_WRITE);
		javax.mail.Message message = inbox.getMessageByUID(uid);
		if (message != null) {
			Folder trash = store.getFolder(getTrashFolderLocalisedName(inbox));
			trash.open(Folder.READ_WRITE);
			inbox.copyMessages(new javax.mail.Message[] { message }, trash);
			trash.close(true);
		}
		inbox.close(true);
	}
}
