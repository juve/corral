package edu.usc.glidein.service.state;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.util.IOUtil;

public class GlideinListener implements CondorEventListener
{
	private Logger logger = Logger.getLogger(GlideinListener.class);
	
	public GlideinListener() { }
	
	private void checkGlideinSuccess(CondorJob job)
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
					glideinFailed("Glidein failed:\n"+stderr);
				else
					glideinSuccess(job);
			}
			catch(IOException ioe)
			{
				glideinFailed("Unable to read error file",ioe);
			}
		}
		else
		{
			glideinFailed("Glidein job produced no error file");
		}
	}
	
	private void cleanupGlideinJob(CondorJob job)
	{
		// Delete staging job directory
		File dir = job.getJobDirectory();
		File[] files = dir.listFiles();
		for(File file : files) file.delete();
		dir.delete();
	}
	
	private void glideinSuccess(CondorJob job)
	{
		// Log message
		logger.debug("Glidein exited successfully");
		
		// TODO: Update glidein status to success
		//glidein.setStatus(new GlideinStatus(GlideinStatusCode.TERMINATED,
		//									"Glidein exited successfully"));
		
		// Cleanup
		cleanupGlideinJob(job);
	}
	
	private void glideinFailed(String message)
	{
		glideinFailed(message,null);
	}
	
	private void glideinFailed(String message, Exception e)
	{
		// Log message
		if(e==null) logger.error(message);
		else logger.error(message, e);
		
		// TODO: Update glidein status to failed
		//glidein.setStatus(new GlideinStatus(GlideinStatusCode.FAILED,
		//									message));
	}
	
	public void handleEvent(CondorEvent event)
	{
		switch(event.getEventCode())
		{
			case JOB_TERMINATED:
				// If job terminated normally, check the return code
				event.getGenerator().terminate();
				checkGlideinSuccess(event.getJob());
				break;
			case EXCEPTION:
				// If there is an exception then fail
				glideinFailed("Error parsing glidein job log", 
						event.getException());
				break;
			case JOB_HELD:
				// Kill job if it is held, job will become
				// aborted
				try
				{
					Condor condor = Condor.getInstance();
					condor.cancelJob(event.getJob().getCondorId());
				} 
				catch(CondorException ce)
				{
					glideinFailed("Unable to cancel held glidein job", ce);
				}
				break;
			case JOB_ABORTED:
				// Fail all aborted jobs
				event.getGenerator().terminate();
				glideinFailed("Glidein job aborted");
				break;
		}
	}
}
