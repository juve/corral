package edu.usc.glidein.service.state;

import java.io.File;
import java.io.IOException;

import javax.naming.NamingException;

import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.util.IOUtil;

public class InstallSiteListener extends BaseListener
{	
	public InstallSiteListener(ResourceKey key)
	{
		super(key);
	}
	
	public void aborted(CondorEvent event)
	{
		// Treat aborted jobs as failures
		failed(event);
	}
	
	public void failed(CondorEvent event)
	{
		failure(event.getMessage(),event.getException());
	}
	
	public void queued(CondorEvent event)
	{
		/* Ignore */
	}
	
	public void running(CondorEvent event)
	{
		/* Ignore */
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
					failure("Install job failed: "+stderr);
				else
					success(job);
			} catch(IOException ioe) {
				failure("Unable to read install error file",ioe);
			}
		} else {
			failure("Install job produced no error file");
		}
	}
	
	private void failure(String message)
	{
		failure(message,null);
	}
	
	private void failure(String message, Exception exception)
	{
		// Generate failed event
		try {
			Event event = new SiteEvent(SiteEventCode.INSTALL_FAILED,
					getKey());
			event.setProperty("message", message);
			event.setProperty("exception", exception);
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+
					ne.getMessage(),ne);
		}
	}
	
	private void success(CondorJob job)
	{
		// Cleanup job dir
		IOUtil.rmdirs(job.getJobDirectory());
		
		// Generate success event
		try {
			Event event = new SiteEvent(SiteEventCode.INSTALL_SUCCESS,
					getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+
					ne.getMessage(),ne);
		}
	}
}
