package edu.usc.glidein.service.state;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;

public abstract class BaseListener implements CondorEventListener
{
	private static final Logger logger = Logger.getLogger(BaseListener.class);
	private ResourceKey key = null;
	private boolean aborted = false;
	
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
	
	public abstract void queued(CondorEvent event);
	public abstract void running(CondorEvent event);
	public abstract void terminated(CondorEvent event);
	public abstract void failed(CondorEvent event);
	public abstract void aborted(CondorEvent event);
	
	public void handleEvent(CondorEvent event) 
	{	
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
						condor.cancelJob(event.getJob().getJobId());
					} catch(CondorException ce) {
						// We are just going to log this because we may be 
						// recovering the state of a job that has already 
						// been aborted 
						logger.error("Unable to abort failed job: "+
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
