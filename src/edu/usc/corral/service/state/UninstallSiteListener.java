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
package edu.usc.corral.service.state;

import java.io.File;
import java.io.IOException;

import edu.usc.corral.condor.CondorEvent;
import edu.usc.corral.condor.CondorJob;
import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.util.FilesystemUtil;
import edu.usc.corral.util.IOUtil;

public class UninstallSiteListener extends BaseListener {
	private int siteId;
	
	public UninstallSiteListener(int siteId) {
		this.siteId = siteId;
	}
	
	public void aborted(CondorEvent event) {
		// Treat aborted jobs like failed jobs
		failed(event);
	}
	
	public void queued(CondorEvent event) {
		/* Ignore */
	}
	
	public void running(CondorEvent event) {
		/* Ignore */
	}
	
	public void failed(CondorEvent event) {
		failure(event.getMessage(),event.getException());
	}
	
	public void terminated(CondorEvent event) {
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
				
				failure("Uninstall job failed",exception);
			} else {
				success(job);
			}
		} catch(IOException ioe) {
			failure("Unable to read uninstall job output file(s)",ioe);
		}
	}
	
	public void failure(String message, Exception exception) {
		// Generate failed event
		try {
			CondorEvent ce = getLastEvent();
			Event event = new SiteEvent(
				SiteEventCode.UNINSTALL_FAILED,ce.getTime(),this.siteId);
			event.setProperty("message", message);
			event.setProperty("exception", exception);
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (ConfigurationException ne) {
			throw new RuntimeException(
				"Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
	
	private void success(CondorJob job) {
		// Cleanup job dir
		try {
			FilesystemUtil.rm(job.getJobDirectory());
		} catch (IOException ioe) {
			throw new RuntimeException(
				"Unable to remove uninstall job directory", ioe);
		}
		
		// Generate success event
		try {
			CondorEvent ce = getLastEvent();
			Event event = new SiteEvent(
				SiteEventCode.UNINSTALL_SUCCESS,ce.getTime(),this.siteId);
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (ConfigurationException ne) {
			throw new RuntimeException(
				"Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
}
