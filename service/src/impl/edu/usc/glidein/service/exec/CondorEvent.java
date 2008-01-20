package edu.usc.glidein.service.exec;

import java.util.Calendar;

public class CondorEvent
{
	private CondorEventGenerator generator;
	private CondorEventCode eventCode;
	private CondorJob job;
	private String message;
	private Calendar time;
	private CondorException exception;
	
	public CondorEvent(){ }
	
	public CondorException getException()
	{
		return exception;
	}
	
	public void setException(CondorException exception)
	{
		this.exception = exception;
	}
	
	public CondorEventCode getEventCode()
	{
		return this.eventCode;
	}
	
	public CondorJob getJob()
	{
		return this.job;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Calendar getTime() {
		return time;
	}

	public void setTime(Calendar time) {
		this.time = time;
	}

	public void setEventCode(CondorEventCode eventCode) {
		this.eventCode = eventCode;
	}

	public void setJob(CondorJob job) {
		this.job = job;
	}

	public CondorEventGenerator getGenerator() {
		return generator;
	}

	public void setGenerator(CondorEventGenerator generator) {
		this.generator = generator;
	}
}
