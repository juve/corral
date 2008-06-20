package edu.usc.glidein.service.state;

import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorJob;

public abstract class BaseListener implements CondorEventListener
{
	private ResourceKey key = null;
	private String abortMessage = null;
	
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
	
	public void failed(String message)
	{
		failed(message, null);
	}
	
	public abstract void terminated(CondorJob job);
	public abstract void failed(String message, Exception e);
	
	public void handleEvent(CondorEvent event) 
	{	
		switch(event.getEventCode())
		{
			case JOB_TERMINATED:
				// Job terminated normally
				event.getGenerator().terminate();
				terminated(event.getJob());
			break;
			case EXCEPTION:
				// If there is an exception then fail
				failed("Error parsing job log", event.getException());
			break;
			case GLOBUS_SUBMIT_FAILED:
			case GLOBUS_RESOURCE_DOWN:
			case GRID_RESOURCE_DOWN:
			case JOB_HELD:
				try {
					// Abort the job
					Condor condor = Condor.getInstance();
					condor.cancelJob(event.getJob().getJobId());
					
					// Set message
					abortMessage = event.getMessage();
				} catch(CondorException ce) {
					failed("Unable to cancel held job", ce);
				}
			break;
			case JOB_ABORTED:
				// Fail all aborted jobs
				event.getGenerator().terminate();
				if (abortMessage == null) {
					failed(event.getMessage());
				} else {
					failed(abortMessage);
				}
			break;
		}
	}
}
