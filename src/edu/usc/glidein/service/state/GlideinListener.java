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
