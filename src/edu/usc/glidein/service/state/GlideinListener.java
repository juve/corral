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
package edu.usc.glidein.service.state;

import java.io.File;
import java.io.IOException;

import javax.naming.NamingException;

import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.util.IOUtil;

public class GlideinListener extends BaseListener
{
	public GlideinListener(ResourceKey key) 
	{ 
		super(key);
	}
	
	public void terminated(CondorEvent event)
	{
		// A glidein job finished successfully if it didn't 
		// produce any output on stderr. Its ugly, but GT2 
		// is broken.
		CondorJob job = event.getJob();
		File error = job.getError();
		if(error.exists())
		{
			try
			{
				String stderr = IOUtil.read(error);
				if(stderr.length()>0)
					failure("Glidein failed: "+stderr);
				else
					success(job);
			}
			catch(IOException ioe)
			{
				failure("Unable to read error file",ioe);
			}
		}
		else
		{
			failure("Glidein job produced no error file");
		}
	}
	
	public void aborted(CondorEvent event)
	{
		// Treat aborted jobs like failed jobs
		enqueue(GlideinEventCode.JOB_ABORTED);
	}
	
	public void failed(CondorEvent event)
	{
		// Process failure
		failure(event.getMessage(),event.getException());
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
		// Delete staging job directory
		File dir = job.getJobDirectory();
		File[] files = dir.listFiles();
		for(File file : files) file.delete();
		dir.delete();
		
		// Generate success event
		enqueue(GlideinEventCode.JOB_SUCCESS);
	}
	
	private void failure(String message)
	{
		failure(message,null);
	}
	
	private void failure(String message, Exception exception)
	{
		// Generate failure event
		try {
			Event event = new GlideinEvent(GlideinEventCode.JOB_FAILURE,getKey());
			event.setProperty("message", message);
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
			Event event = new GlideinEvent(code,getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
}
