package edu.usc.glidein.service.state;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.util.IOUtil;

public class StageSiteListener implements CondorEventListener
{
	private Logger logger = Logger.getLogger(StageSiteListener.class);
	
	public ResourceKey key = null;
	
	public StageSiteListener(ResourceKey key)
	{
		this.key = key;
	}
	
	private void failed(String message, Exception e)
	{
		// Log message
		if(e==null) logger.error(message);
		else logger.error(message, e);
		
		// Update site status to staging failed
		// TODO: updateStatus(SiteStatus.FAILED, message);
	}
	
	private void failed(String message)
	{
		failed(message, null);
	}
	
	private void checkSuccess(CondorJob job)
	{
		// A job finished successfully if it didn't produce any
		// output on stderr. Its ugly, but GT2 is broken.
		File error = job.getError();
		if(error.exists())
		{
			try
			{
				String stderr = IOUtil.read(error);
				if(stderr.length()>0)
					failed("Staging job failed:\n"+stderr);
				else
					success(job);
			}
			catch(IOException ioe)
			{
				failed("Unable to read error file",ioe);
			}
		}
		else
		{
			failed("Staging job produced no error file");
		}
	}
	
	private void success(CondorJob job)
	{
		/* TODO: Staging Success
		// Log message
		Site site = resource.getSite();
		logger.debug("Staging successful for site '"+site.getName()+"'");
		
		// Update site status to success
		updateStatus(SiteStatus.READY,"Staging successful");
		
		// Cleanup job dir
		File dir = job.getJobDirectory();
		File[] files = dir.listFiles();
		for(File file : files) file.delete();
		dir.delete();
		*/
	}
	
	public void handleEvent(CondorEvent event) 
	{	
		switch(event.getEventCode())
		{
			case GLOBUS_SUBMIT:
				// Change job status to submitted
				// TODO: resource.updateStatus(SiteStatus.STAGING,"Staging job submitted");
				break;
			case JOB_TERMINATED:
				// If job terminated normally, check the return code
				event.getGenerator().terminate();
				checkSuccess(event.getJob());
				break;
			case EXCEPTION:
				// If there is an exception then fail
				failed("Error parsing staging job log", 
						event.getException());
				break;
			case GLOBUS_SUBMIT_FAILED:
			case GLOBUS_RESOURCE_DOWN:
			case GRID_RESOURCE_DOWN:
			case JOB_HELD:
				try {
					// Abort the job
					Condor condor = Condor.getInstance();
					condor.cancelJob(event.getJob().getCondorId());
					
					// Set status to failed and the reason
					failed(event.getEventCode().getDescription());
				} catch(CondorException ce) {
					failed("Unable to cancel held staging job", ce);
				}
				break;
			case JOB_ABORTED:
				// Fail all aborted jobs
				event.getGenerator().terminate();
				failed("Staging job aborted");
				break;
		}
	}
}
