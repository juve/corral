package edu.usc.glidein.service.exec;

import java.io.File;

import edu.usc.glidein.service.ServiceConfiguration;
import edu.usc.glidein.service.ServiceException;

public class Condor
{
	private ServiceConfiguration serviceConfiguration;
	private File condorHome;
	private File condorBin;
	private File condorConfiguration;
	
	public Condor() throws ServiceException
	{
		serviceConfiguration = ServiceConfiguration.getInstance();
		setCondorHome(new File(
				serviceConfiguration.getProperty("condor.home")));
		setCondorConfiguration(new File(
				serviceConfiguration.getProperty("condor.config")));
		setCondorBin(new File(
				serviceConfiguration.getProperty("condor.bin")));
	}
	
	public void submitJob(CondorJob job)
	{
		// Create job directory if it doesn't exist
		
		// Create submit script
		
		// Get working directory
		
		// Find condor_submit executable
		
		// Set environment
		//CONDOR_HOME
		//CONDOR_CONFIG
		
		// Run condor_submit
		// condor_submit -verbose SUBMIT_SCRIPT
		
		// Check exit code
		
		// Parse job ID from output
		
		// Attach event generator to log
	}
	
	public void cancelJob(CondorJob job)
	{
		// Get ID
		
		// Run condor_rm
	}

	public File getCondorHome()
	{
		return condorHome.getAbsoluteFile();
	}

	public void setCondorHome(File condorHome) 
	throws ServiceException
	{
		if(condorHome == null)
			throw new ServiceException("null");
		if(!condorHome.exists())
			throw new ServiceException(
				condorHome.getAbsolutePath()+" does not exist");
		if(!condorHome.isDirectory())
			throw new ServiceException(
				condorHome.getAbsolutePath()+" is not a directory");
		this.condorHome = condorHome;
	}
	
	public File getCondorConfiguration()
	{
		return condorConfiguration.getAbsoluteFile();
	}

	public void setCondorConfiguration(File condorConfiguration)
	throws ServiceException
	{
		if(condorConfiguration == null)
			throw new ServiceException("null");
		if(!condorConfiguration.exists())
			throw new ServiceException(
				condorConfiguration.getAbsolutePath()+" does not exist");
		if(!condorConfiguration.isFile())
			throw new ServiceException(
				condorConfiguration.getAbsolutePath()+" is not a file");
		this.condorConfiguration = condorConfiguration;
	}
	
	public File getCondorBin()
	{
		return condorBin.getAbsoluteFile();
	}
	
	public void setCondorBin(File condorBin)
	throws ServiceException
	{
		if(condorBin == null)
			throw new ServiceException("null");
		if(!condorBin.exists())
			throw new ServiceException(
				condorBin.getAbsolutePath()+" does not exist");
		if(!condorBin.isDirectory())
			throw new ServiceException(
				condorBin.getAbsolutePath()+" is not a directory");
		this.condorBin = condorBin;
	}
}
