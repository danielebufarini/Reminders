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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GMailFeedReader implements Runnable {
	private static final String LOGTAG = GMailFeedReader.class.getSimpleName();
	private static final Pattern ENTRY_PATTERN = Pattern.compile("<entry>(.*?)</entry>");
	private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*)</title>");
	private static final Pattern SUMMARY_PATTERN = Pattern.compile("<summary>(.*)</summary>");
	private static final Pattern FROM_NAME_PATTERN = Pattern.compile("<name>(.*)</name>");
	private static final Pattern FROM_EMAIL_PATTERN = Pattern.compile("<email>(.*)</email>");
	private static final long FIVE_MINUTES = 60 * 5 * 1000L;
	private static final String LIST_PREFIX = "list";
	
	private final HttpGet method = new HttpGet("https://mail.google.com/mail/feed/atom/unread");
	private final HttpClient client = new DefaultHttpClient();
	private final Handler myHandler;
	private String token;

	public GMailFeedReader(Handler myHandler, String token) {
		this.myHandler = myHandler;
		this.token = token;
		method.setHeader("Authorization", "Bearer " + token);
	}
	
	private String getHttpResponse() {
		HttpResponse response = null;
		String contents = "";
		try {
			response = client.execute(method);
			try {
				BufferedReader reader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
				String line;
				while ((line = reader.readLine()) != null)
					contents += line;
			} catch (Exception e) {
				Log.e(LOGTAG, "getHttpResponse :: error reading http stream", e);
			}
		} catch (Exception e) {
			Log.e(LOGTAG, "getHttpResponse :: error executing http get", e);
		}
		return contents;
	}
	
	private ArrayList<MailEntry> parseGMailXmlFeed(String contents) {
		ArrayList<MailEntry> mailsArrayList = new ArrayList<MailEntry>(100);
		 
		Matcher entryMatcher = ENTRY_PATTERN.matcher(contents);
		while (entryMatcher.find()) {		 
		    String matchedEntry = entryMatcher.group(1);
		 
		    String subject = null;
		    Matcher titleMatcher = TITLE_PATTERN.matcher(matchedEntry);
		    while (titleMatcher.find())
		    	subject = titleMatcher.group(1);
		    if (subject!= null && subject.toLowerCase(Locale.US).contains(LIST_PREFIX)) {		    	
			    MailEntry currentMailEntry = new MailEntry();
			    currentMailEntry.setSubject(subject);
			    
			    Matcher summaryMatcher = SUMMARY_PATTERN.matcher(matchedEntry);
			    while (summaryMatcher.find())
			        currentMailEntry.setBody(summaryMatcher.group(1));
			 
			    Matcher fromNameMatcher = FROM_NAME_PATTERN.matcher(matchedEntry);
			    while (fromNameMatcher.find())
			        currentMailEntry.setFromName(fromNameMatcher.group(1));
			 
			    Matcher fromEmailMatcher = FROM_EMAIL_PATTERN.matcher(matchedEntry);
			    while (fromEmailMatcher.find())
			        currentMailEntry.setFromEmail(fromEmailMatcher.group(1));
			 
			    mailsArrayList.add(currentMailEntry);
		    }
		}
		
		return mailsArrayList;
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				String contents = getHttpResponse();
				ArrayList<MailEntry> entries = parseGMailXmlFeed(contents);
				Message message = myHandler.obtainMessage();
				Bundle bundle = new Bundle();
				bundle.putSerializable("emails", entries);
				message.setData(bundle);
				myHandler.sendMessage(message);
				Thread.sleep(FIVE_MINUTES);
			}
		} catch (InterruptedException e) {
			// Nothing to do - exits cleanly from thread
		}
	}
}
