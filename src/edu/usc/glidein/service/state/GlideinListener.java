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
package edu.usc.glidein.service.state;

import java.io.File;
import java.io.IOException;

import javax.naming.NamingException;

import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.condor.CondorEvent;
import edu.usc.glidein.condor.CondorJob;
import edu.usc.glidein.util.IOUtil;

public class GlideinListener extends BaseListener
{
	public GlideinListener(ResourceKey key) 
	{ 
		super(key);
	}
	
	public void terminated(CondorEvent event)
	{
		// A job finished successfully if it didn't produce any
		// errors in the status file. Its ugly, but GT2 is broken.
		try {
			CondorJob job = event.getJob();
			File status = new File(job.getJobDirectory(),"status");
			String errors = IOUtil.read(status);
			if(errors.length() > 0) {
				// Read stderr and stdout of job
				File error = job.getError();
				String stderr = IOUtil.read(error);
				File output = job.getOutput();
				String stdout = IOUtil.read(output);
				Exception exception = new Exception(
						"ERRORS:\n"+errors+"\n\n" +
						"STDOUT:\n"+stdout+"\n\n" +
						"STDERR:\n"+stderr);
				
				failure("Glidein job failed",null,exception);
			} else {
				success(job);
			}
		} catch(IOException ioe) {
			failure("Unable to read glidein job output file(s)",null,ioe);
		}
	}
	
	public void aborted(CondorEvent event)
	{
		// Process an aborted job
		enqueue(GlideinEventCode.JOB_ABORTED);
	}
	
	public void failed(CondorEvent event)
	{
		// Process failure
		failure(event.getMessage(),event.getDetails(), event.getException());
	}
	
	public void queued(CondorEvent event)
	{
		// Generate queued event
		enqueue(GlideinEventCode.QUEUED);
	}
	
	public void running(CondorEvent event)
	{
		// Generate running event
		enqueue(GlideinEventCode.RUNNING);
	}
	
	private void success(CondorJob job)
	{		
		// Generate success event
		enqueue(GlideinEventCode.JOB_SUCCESS);
	}
	
	private void failure(String message, String longMessage, Exception exception)
	{
		// Generate failure event
		try {
			CondorEvent ce = getLastEvent();
			Event event = new GlideinEvent(GlideinEventCode.JOB_FAILURE,ce.getTime(),getKey());
			event.setProperty("message", message);
			event.setProperty("longMessage", longMessage);
			event.setProperty("exception", exception);
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
	
	private void enqueue(GlideinEventCode code)
	{
		try {
			CondorEvent ce = getLastEvent();
			Event event = new GlideinEvent(code,ce.getTime(),getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
}
