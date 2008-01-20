package edu.usc.glidein.service.core;

import java.util.Calendar;


public class StatusChangeEvent
{
	private Status newStatus;
	private Status previousStatus;
	private Object source;
	private String message;
	private Throwable exception;
	private Calendar time;

	public Status getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(Status newStatus) {
		this.newStatus = newStatus;
	}

	public Status getPreviousStatus() {
		return previousStatus;
	}

	public void setPreviousStatus(Status previousStatus) {
		this.previousStatus = previousStatus;
	}

	public Calendar getTime() {
		return time;
	}

	public void setTime(Calendar time) {
		this.time = time;
	}

	public Object getSource() {
		return source;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}
}
