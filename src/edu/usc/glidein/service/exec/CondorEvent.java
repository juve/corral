/*
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.usc.glidein.service.exec;

import java.util.Calendar;

/**
 * An event generated by condor for a job
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class CondorEvent
{
	private CondorEventGenerator generator;
	private CondorEventCode eventCode;
	private CondorJob job;
	private String message;
	private Calendar time;
	private CondorException exception;
	
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

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Calendar getTime()
	{
		return time;
	}

	public void setTime(Calendar time)
	{
		this.time = time;
	}

	public void setEventCode(CondorEventCode eventCode)
	{
		this.eventCode = eventCode;
	}

	public void setJob(CondorJob job)
	{
		this.job = job;
	}

	public CondorEventGenerator getGenerator()
	{
		return generator;
	}

	public void setGenerator(CondorEventGenerator generator)
	{
		this.generator = generator;
	}
}
