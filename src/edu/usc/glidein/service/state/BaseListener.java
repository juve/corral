package edu.usc.glidein.service.state;

import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventCode;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;

public abstract class BaseListener implements CondorEventListener
{
	private ResourceKey key = null;
	private CondorEvent abortEvent = null;
	private boolean aborting = false;
	
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
				if (!aborting) {
					try {
						// Now we are aborting and don't want to
						// abort twice.
						aborting = true;
						
						// Some errors cause the job to be held
						// For those errors cancel the job first.
						// Abort the job
						Condor condor = Condor.getInstance();
						condor.cancelJob(event.getJob().getJobId());
						
						// Set abort event
						abortEvent = event;
					} catch(CondorException ce) {
						CondorEvent e = new CondorEvent();
						e.setEventCode(CondorEventCode.EXCEPTION);
						e.setJob(event.getJob());
						e.setException(ce);
						e.setMessage("Unable to cancel held job");
						failed(e);
					}
				}
			} break;
			
			case JOB_ABORTED: {
				// Fail all aborted jobs
				event.getGenerator().terminate();
				if (abortEvent == null) {
					failed(event);
				} else {
					failed(abortEvent);
				}
			} break;
		}
	}
}
