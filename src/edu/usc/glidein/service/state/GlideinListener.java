package edu.usc.glidein.service.state;

import java.io.File;
import java.io.IOException;

import javax.naming.NamingException;

import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.util.IOUtil;

public class GlideinListener extends BaseListener
{
	public GlideinListener(ResourceKey key) 
	{ 
		super(key);
	}
	
	public void terminated(CondorJob job)
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
					failed("Glidein failed: "+stderr);
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
			failed("Glidein job produced no error file");
		}
	}
	
	private void success(CondorJob job)
	{
		// Delete staging job directory
		File dir = job.getJobDirectory();
		File[] files = dir.listFiles();
		for(File file : files) file.delete();
		dir.delete();
		
		// Generate success event
		try {
			Event event = new GlideinEvent(GlideinEventCode.JOB_SUCCESS,getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
	
	public void failed(String message, Exception e)
	{
		// Generate failure event
		try {
			Event event = new GlideinEvent(GlideinEventCode.JOB_FAILURE,getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new RuntimeException("Unable to get event queue: "+ne.getMessage(),ne);
		}
	}
}
