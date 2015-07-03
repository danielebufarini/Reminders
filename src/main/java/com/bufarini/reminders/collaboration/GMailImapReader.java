package com.bufarini.reminders.collaboration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GMailImapReader implements Runnable {
	public static final String LIST_PREFIX_SHARED = "list:: ";
	
	private static final String LOGTAG = GMailImapReader.class.getSimpleName();
	private static final FlagTerm FLAG_TERM_SEEN = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
	private static final Pattern EMAIL_FROM = Pattern.compile("(.*)<(.*)>");
	private static final FetchProfile FETCH_PROFILE = new FetchProfile();
	private static final long TWO_MINUTES = 2 * 60 * 1000L;
	private static final String LIST_PREFIX = "list";
	
	private final String oAuth2Token, accountName;
	private final Handler myHandler;

	public GMailImapReader(long delayInMillis, String accountName, Handler myHandler, String oAuth2Token) {
		this.myHandler = myHandler;
		this.oAuth2Token = oAuth2Token;
		this.accountName = accountName;
		FETCH_PROFILE.add(FetchProfile.Item.ENVELOPE);
		FETCH_PROFILE.add(FetchProfile.Item.CONTENT_INFO);
		FETCH_PROFILE.add(FetchProfile.Item.FLAGS);
		FETCH_PROFILE.add("X-Mailer");
		if (delayInMillis > 0)
			try {
				Thread.sleep(delayInMillis);
			} catch (InterruptedException e) {
				// Nothing to do, we have to simply quit this thread
			}
	}
	
	private IMAPStore getStore(String accountName) throws Exception {
		OAuth2Authenticator.initialize();
		IMAPStore store = OAuth2Authenticator.connectToImap("imap.gmail.com", 993, accountName, oAuth2Token, false);
		return store;
	}
	
	private IMAPFolder getInbox(IMAPStore store) throws MessagingException {
		IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
		inbox.open(Folder.READ_WRITE);
		return inbox;
	}
	
	private ArrayList<MailEntry> getUnreadMessages(IMAPFolder inbox) throws MessagingException, IOException {
		javax.mail.Message[] messages = inbox.search(FLAG_TERM_SEEN);
		inbox.fetch(messages, FETCH_PROFILE);
		ArrayList<MailEntry> entries = new ArrayList<MailEntry>(messages.length);
		for (int i = 0; i < messages.length; ++i) {
			String subject = messages[i].getSubject();
			if (subject.toLowerCase(Locale.US).contains(LIST_PREFIX)) {
				if (subject.startsWith(LIST_PREFIX_SHARED))
					subject = subject.substring(LIST_PREFIX_SHARED.length());
				MailEntry mailEntry = new MailEntry();
				Address[] address = messages[i].getFrom();
				String from = "";
				if (address != null)
					from = address[0].toString();
				Matcher matcher = EMAIL_FROM.matcher(from);
				if (matcher.matches()) {
					mailEntry.setFromEmail(matcher.group(2));
					mailEntry.setFromName(matcher.group(1));
				} else
					mailEntry.setFromEmail(from);
				mailEntry.setSubject(subject);
				String body = getBody(messages[i]);
				mailEntry.setBody(body);
				mailEntry.setDate(messages[i].getReceivedDate());
				mailEntry.setUID(inbox.getUID(messages[i]));
				entries.add(mailEntry);
			}
		}
		return entries;
	}
	
	private String getBody(Part part) throws MessagingException, IOException {
		String result = "";
		if (part.isMimeType("text/plain")) {
			result = part.getContent().toString();
		} else if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			int count = mp.getCount();
			for (int i = 0; i < count; ++i)
				if (mp.getBodyPart(i).isMimeType("text/plain"))
					result = mp.getBodyPart(i).getContent().toString();
		}
		return result;
	}
	
	@Override
	public void run() {
		IMAPStore store = null;
		IMAPFolder inbox = null;
		try {
			store = getStore(accountName);
			inbox = getInbox(store);
			while (true) {
				ArrayList<MailEntry> messages = getUnreadMessages(inbox);
				Message message = myHandler.obtainMessage();
				Bundle bundle = new Bundle();
				bundle.putSerializable("emails", messages);
				message.setData(bundle);
				myHandler.sendMessage(message);
				Thread.sleep(TWO_MINUTES);
			}
		} catch (InterruptedException e) {
			// Nothing to do, we have to simply quit this thread
		} catch (Exception e) {
			Log.e(LOGTAG, "", e);
		} finally {
			try {
				if (inbox != null)
					inbox.close(false);
				if (store != null)
					store.close();
			} catch (MessagingException e) {
				Log.e(LOGTAG, "", e);
			}
		}
	}
}
