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

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.condor.Condor;
import edu.usc.glidein.condor.CondorEvent;
import edu.usc.glidein.condor.CondorEventListener;
import edu.usc.glidein.condor.CondorException;

public abstract class BaseListener implements CondorEventListener
{
	private static final Logger logger = Logger.getLogger(BaseListener.class);
	private ResourceKey key = null;
	private boolean aborted = false;
	private CondorEvent lastEvent = null;
	
	public BaseListener(ResourceKey key)
	{
		setKey(key);
	}
	
	public ResourceKey getKey()
	{
		return key;
	}
	
	public void setKey(ResourceKey key)
	{
		if (key==null) throw new NullPointerException();
		this.key = key;
	}
	
	public CondorEvent getLastEvent()
	{
		return lastEvent;
	}
	
	public void setLastEvent(CondorEvent lastEvent)
	{
		this.lastEvent = lastEvent;
	}
	
	public abstract void queued(CondorEvent event);
	public abstract void running(CondorEvent event);
	public abstract void terminated(CondorEvent event);
	public abstract void failed(CondorEvent event);
	public abstract void aborted(CondorEvent event);
	
	public void handleEvent(CondorEvent event) 
	{	
		setLastEvent(event);
		
		switch(event.getEventCode())
		{
			case GRID_SUBMIT:
			case GLOBUS_SUBMIT: {
				queued(event);
			} break;
			
			case EXECUTE: {
				running(event);
			} break;
			
			case JOB_TERMINATED: {
				// Job terminated normally
				event.getGenerator().terminate();
				terminated(event);
			} break;
			
			case EXCEPTION:
			case SHADOW_EXCEPTION:
			case REMOTE_ERROR: {
				// Other errors
				failed(event);
			} break;
			
			case GLOBUS_SUBMIT_FAILED:
			case GLOBUS_RESOURCE_DOWN:
			case GRID_RESOURCE_DOWN:
			case JOB_HELD: {
				// Some errors cause the job to be held. For those errors
				// we need to abort the job.
				if (!aborted) { // Only try to abort once
					try {
						aborted = true; // Only try to abort once
						Condor condor = Condor.getInstance();
						condor.cancelJob(event.getJob());
					} catch(CondorException ce) {
						// We are just going to log this because we may be 
						// recovering the state of a job that has already 
						// been aborted 
						logger.warn("Unable to abort failed job: "+
								event.getJob().getJobId(),ce);
					}
				}
				failed(event);
			} break;
			
			case JOB_ABORTED: {
				// Fail all aborted jobs
				event.getGenerator().terminate();
				aborted(event);
			} break;
		}
	}
}
