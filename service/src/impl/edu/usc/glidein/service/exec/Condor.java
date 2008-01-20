package edu.usc.glidein.service.exec;

import java.io.File;

public class Condor
{
	public static final int DEFAULT_COLLECTOR_PORT = 9618;
	
	private File condorHome;
	private File condorBin;
	private File condorConfiguration;
	
	public Condor(File condorHome, File condorConfiguration)
	{
		setCondorHome(condorHome);
		setCondorConfiguration(condorConfiguration);
	}
	
	public void submitJob(CondorJob job)
	{
		// Create submit script
		
		// Find condor_submit executable
		
		// Run condor_submit
		// condor_submit -pool juve.usc.edu:9618 -name juve@juve.usc.edu -verbose job.sub
	}
	
	public void cancelJob(CondorJob job)
	{
		// Get ID
	}

	public File getCondorHome()
	{
		return condorHome;
	}

	public void setCondorHome(File condorHome) 
	throws NullPointerException, IllegalArgumentException
	{
		if(condorHome == null)
			throw new NullPointerException("CONDOR_HOME cannot be null");
		if(!condorHome.isDirectory())
			throw new IllegalArgumentException("CONDOR_HOME must be a directory");
		this.condorHome = condorHome;
		this.condorBin = new File(condorHome,"bin");
		if(!this.condorBin.isDirectory())
			throw new IllegalArgumentException("CONDOR_HOME/bin does not exist");
	}
	
	public File getCondorConfiguration()
	{
		return this.condorConfiguration;
	}

	public void setCondorConfiguration(File condorConfiguration)
	{
		if(condorConfiguration == null)
			throw new NullPointerException("CONDOR_CONFIG cannot be null");
		if(!condorConfiguration.isFile())
			throw new IllegalArgumentException("CONDOR_CONFIG must be a file");
		this.condorConfiguration = condorConfiguration;
	}
}
