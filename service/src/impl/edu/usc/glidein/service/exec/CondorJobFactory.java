package edu.usc.glidein.service.exec;

import java.io.File;
import java.util.UUID;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.ServiceConfiguration;

public class CondorJobFactory
{
	public CondorJob createJob(CondorJobDescription jobDescription)
	throws CondorException
	{
		// Create job directory if it doesn't exist
		try 
		{
			ServiceConfiguration config = ServiceConfiguration.getInstance();
			File workDirectory = new File(config.getProperty("work.dir"));
			UUID uuid = UUID.randomUUID();
			File jobDirectory = new File(workDirectory,"job-"+uuid.toString());
			if(jobDirectory.exists())
			{
				throw new CondorException("Job directory "+
						jobDirectory.getAbsolutePath()+" already exists");
			}
			else 
			{
				if(!jobDirectory.mkdirs())
				{
					throw new CondorException(
							"Unable to create job directory: "+jobDirectory);
				}
			}
			
			return new CondorJob(jobDirectory,jobDescription);
		}
		catch(GlideinException se)
		{
			throw new CondorException("Unable to create job directory",se);
		}
	}
	
	public CondorJob getJob(String uuid)
	throws CondorException
	{
		// TODO Create job
		return null;
	}
}
