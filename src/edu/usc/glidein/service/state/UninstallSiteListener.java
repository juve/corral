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

import edu.usc.glidein.condor.CondorEvent;
import edu.usc.glidein.condor.CondorJob;
import edu.usc.glidein.util.IOUtil;

public class UninstallSiteListener extends BaseListener
{
	public UninstallSiteListener(ResourceKey key)
	{
		super(key);
	}
	
	public void aborted(CondorEvent event)
	{
		// Treat aborted jobs like failed jobs
		failed(event);
	}
	
	public void queued(CondorEvent event)
	{
		/* Ignore */
	}
	
	public void running(CondorEvent event)
	{
		/* Ignore */
	}
	
	public void failed(CondorEvent event)
	{
		failure(event.getMessage(),event.getException());
	}
	
	public void terminated(CondorEvent event)
	{
		// A job finished successfully if it didn't produce any
		// output on stderr. Its ugly, but GT2 is broken.
		CondorJob job = event.getJob();
		File error = job.getError();
		if(error.exists()) {
			try {
				String stderr = IOUtil.read(error);
				if(stderr.length()>0)
					failure("Uninstall job failed: "+stderr);
				else
					success(job);
			} catch(IOException ioe) {
				failure("Unable to read uninstall error file",ioe);
			}
		} else {
			failure("Install job produced no error file");
		}
	}
	
	private void failure(String message)
	{
		failure(message,null);
	}
	
	public void failure(String message, Exception exception)
	{
		// Generate failed event
		try {
			Event event = new SiteEvent(SiteEventCode.UNINSTALL_FAILED,getKey());
			event.setProperty("message", message);
			event.setProperty("exception", exception);
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
	
	private void success(CondorJob job)
	{
		// Cleanup job dir
		IOUtil.rmdirs(job.getJobDirectory());
		
		// Generate success event
		try {
			Event event = new SiteEvent(SiteEventCode.UNINSTALL_SUCCESS,getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
}
