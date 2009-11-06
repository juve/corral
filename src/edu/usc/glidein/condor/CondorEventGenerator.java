/*
 *  Copyright 2007-2009 University Of Southern California
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
package edu.usc.glidein.condor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * This class generates Condor job events by tailing and parsing a Condor
 * user job log file. The file should be standard format (i.e. not XML).
 * 
 * @author Gideon Juve <juve@usc.edu>
 */
public class CondorEventGenerator extends Thread
{
	/** The interval (in milliseconds) to poll the log file for new data */
	private int pollInterval = 10000; // Default: 10 sec
	
	/** The job to generate events for */
	private CondorJob job;
	
	/** Is this event generator running? */
	private boolean running = false;
	
	/** The format of dates in the log file */
	private SimpleDateFormat eventDateFormat;
	
	/**
	 * Create a new event generator for the given job.
	 * @param job The job to generate events for.
	 */
	public CondorEventGenerator(CondorJob job)
	{
		this.job = job;
		this.eventDateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");
	}
	
	/**
	 * Parse the current event.
	 * @throws CondorException If there is an error parsing the event.
	 */
	private CondorEvent parseEvent(LinkedList<String> currentEvent)
	throws CondorException
	{
		// Process header
		String header = currentEvent.poll();
		String[] fields = header.split(" ",5);
		if(fields.length != 5)
			throw new CondorException("Error parsing event header: "+header);
		
		// New event
		CondorEvent event = new CondorEvent();
		event.setGenerator(this);
		event.setJob(job);
		
		// Event code
		try 
		{
			int intCode = Integer.parseInt(fields[0]);
			CondorEventCode code = CondorEventCode.fromEventCode(intCode);
			event.setEventCode(code);
		}
		catch(IllegalArgumentException e)
		{
			throw new CondorException("Unrecognized event code: "+fields[0],e);
		}
		
		// Job ID
		try
		{
			String id = fields[1].substring(1,fields[1].length()-2);
			String[] tokens = id.split("[.]");
			if(tokens.length != 3)
				throw new CondorException("Error parsing job id: "+fields[1]);
			String condorId = 
				Integer.parseInt(tokens[0])+"."+Integer.parseInt(tokens[1]);
			if(!condorId.equals(job.getJobId()))
			{
				throw new CondorException("Condor ID mismatch: got "+
						condorId + " expected " + job.getJobId());
			}
		}
		catch(CondorException ce)
		{
			throw ce; /* No need to wrap these */
		}
		catch(Exception e)
		{
			throw new CondorException("Error parsing job id: "+fields[1],e);
		}
		
		// Date / time
		String eventTime = fields[2] + " " + fields[3];
		try {
			Date date = eventDateFormat.parse(eventTime);
			event.setTime(date);
		} catch(ParseException pe) {
			throw new CondorException("Error parsing event time: "+eventTime);
		}
		
		// Message
		event.setMessage(fields[4].trim());
		
		// Event details
		StringBuffer details = new StringBuffer();
		details.append(header);
		for (String s : currentEvent) {
			details.append(s);
		}
		event.setDetails(details.toString());
		
		return event;
	}
	
	/**
	 * Deliver an event to all the job's event listeners
	 * @param event The event to deliver
	 */
	public void deliverEvent(CondorEvent event)
	{
		for(CondorEventListener listener : job.getListeners())
			listener.handleEvent(event);
	}
	
	/**
	 * Is this event generator running?
	 * @return true if running, false otherwise
	 */
	public boolean isRunning()
	{
		return running;
	}
	
	/**
	 * Tell the event generator to stop running.
	 */
	public void terminate()
	{
		this.running = false;
		this.interrupt();
	}
	
	/**
	 * 
	 * @return The log file polling interval
	 */
	public int getPollInterval()
	{
		return this.pollInterval;
	}
	
	/**
	 * Set the polling interval
	 * @param pollInterval The new poll interval
	 * @throws CondorException If the new poll interval <= 0
	 */
	public void setPollInterval(int pollInterval)
	throws CondorException
	{
		if(pollInterval<=0)
			throw new CondorException(
					"The poll interval must be greater than 0");
		this.pollInterval = pollInterval;
	}
	
	/**
	 * Start tailing the job log and generating events.
	 * @throws CondorException If there is a problem parsing the log file.
	 */
	private void tailJobLog() throws CondorException
	{
		long filePointer = 0;
	    File log = job.getLog();
	    LinkedList<String> currentEvent = new LinkedList<String>();
	    StringBuffer buffer = new StringBuffer();
	    
		while(running)
		{
			long fileLength = log.length();
			
			if(fileLength < filePointer) 
			{
				// The file was truncated
	            filePointer = 0;
			}

			if(fileLength > filePointer)
			{
				try 
				{
					// It is a bit ugly to constantly reopen
					// the file, but this is the only way it
					// will work on Mac OS X
					RandomAccessFile file = new RandomAccessFile(log, "r");
					
					// Seek to the last place we read from
					file.seek(filePointer);
					
					// Read {length} bytes
					int length = (int)(fileLength - filePointer);
					byte[] data = new byte[length];
					file.read(data);
					char[] chars = new String(data).toCharArray();
					for(char c : chars)
					{
						buffer.append(c);
						if(c=='\n')
						{
							String line = buffer.toString();
							
							// Add the current line to the current event
							currentEvent.add(line);
							
							// Look for the end-of-event marker
							if(line.startsWith("...")){
								CondorEvent event = parseEvent(currentEvent);
								deliverEvent(event);
								currentEvent = new LinkedList<String>();
							}
							
							buffer = new StringBuffer();
						}
					}
					
					// Get the new position
					filePointer = file.getFilePointer();
					
					file.close();
				}
				catch(IOException ioe)
				{
					throw new CondorException("Unable to read job log",ioe);
				}
			}

			// Sleep for the specified interval
			try { Thread.sleep(pollInterval); } 
			catch(InterruptedException ie){ /* Ignore */ }
		}
		
	}
	
	public void run()
	{
		try
		{
			running = true;
			tailJobLog();
			running = false;
		}
		catch(CondorException ce)
		{
			// Notify any listeners before exiting
			CondorEvent event = new CondorEvent();
			event.setEventCode(CondorEventCode.EXCEPTION);
			event.setJob(job);
			event.setException(ce);
			event.setMessage(ce.getMessage());
			event.setGenerator(this);
			deliverEvent(event);
		}
	}
	
	public static void main(String[] args)
	{
		CondorJob j = new CondorJob(new File("/Users/juve/Workspace/Condor"),System.getProperty("user.name"));
		j.setJobId("8.0");
		j.setLog(new File("/Users/juve/Workspace/Condor/terminated.log"));
		
		CondorEventGenerator gen = new CondorEventGenerator(j);
		
		j.addListener(new CondorEventListener()
		{
			public void handleEvent(CondorEvent event)
			{
				System.out.println(event.getEventCode()+" "+
								   event.getTime().getTime()+" "+
								   event.getMessage());
				
				switch(event.getEventCode())
				{
					case JOB_TERMINATED:
						event.getGenerator().terminate();
						break;
					case EXCEPTION:
						event.getException().printStackTrace();
						break;
				}
			}
		});
		
		gen.start();
		
		try {
			gen.join();
		} catch(InterruptedException ie){
			ie.printStackTrace();
		}
		System.out.println("done");
	}
}
