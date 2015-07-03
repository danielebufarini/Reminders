package com.bufarini.reminders.collaboration;

import java.io.Serializable;
import java.util.Date;

public class MailEntry implements Serializable {	 
	private static final long serialVersionUID = 3932751425401866271L;
	private String subject, body, fromName, fromEmail;
    private long id;
    private Date date;
    private boolean toBeImported;

    public long getUID() {
		return id;
	}

	public void setUID(long id) {
		this.id = id;
	}

	public String getFromEmail() {
        return fromEmail;
    }
 
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }
 
    public String getFromName() {
        return fromName;
    }
 
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
 
    public String getSubject() {
        return subject;
    }
 
    public void setSubject(String subject) {
        this.subject = subject;
    }
 
    public String getBody() {
        return body;
    }
 
    public void setBody(String body) {
        this.body = body;
    }
 
    public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public boolean isToBeImported() {
		return toBeImported;
	}

	public void setToBeImported(boolean toBeImported) {
		this.toBeImported = toBeImported;
	}

	@Override
    public String toString() {
        return "From: " + fromName + "\nMail ID: " + id + "\nFrom email: " + fromEmail
        			+ "\nSubject: " + subject + "\body: " + body;
    }
}
