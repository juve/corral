package edu.usc.glidein.service.exec;

import java.io.File;
import java.util.HashSet;

/**
 * A class representing a condor job.
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class CondorJob
{
	/** 
	 * Cluster ID assigned by condor schedd. The format is "clusterId.jobId"
	 */
	private String condorId;
	
	/** 
	 * The description for this job
	 */
	private CondorJobDescription description;
	
	/**
	 * The event listeners for this job
	 */
	private HashSet<CondorEventListener> listeners;
	
	/**
	 * The working directory for this job
	 */
	private File jobDirectory;
	
	/**
	 * Submit script for this job
	 */
	private File submitScript;
	
	/**
	 * Log file for this job
	 */
	private File log;
	
	/**
	 * Standard error for this job
	 */
	private File error;
	
	/**
	 * Standard out for this job
	 */
	private File output;
	
	/**
	 * Create a job with a given job directory and set of parameters
	 * @param jobDirectory The directory where the files for this job 
	 * should be created
	 * @param description The parameters for this job
	 */
	CondorJob(File jobDirectory, CondorJobDescription description)
	{
		this.jobDirectory = jobDirectory;
		this.log = new File(jobDirectory,"log");
		this.error = new File(jobDirectory,"error");
		this.output = new File(jobDirectory,"output");
		this.submitScript = new File(jobDirectory,"submit");
		this.description = description;
		this.listeners = new HashSet<CondorEventListener>();
	}
	
	public void addListener(CondorEventListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(CondorEventListener listener)
	{
		listeners.remove(listener);
	}
	
	public CondorEventListener[] getListeners()
	{
		return (CondorEventListener[])
			listeners.toArray(new CondorEventListener[0]);
	}

	public CondorJobDescription getDescription()
	{
		return description;
	}

	public String getCondorId()
	{
		return condorId;
	}

	void setCondorId(String condorId)
	{
		this.condorId = condorId;
	}
	
	public File getJobDirectory()
	{
		return this.jobDirectory;
	}
	
	public File getSubmitScript()
	{
		return this.submitScript;
	}
	
	public File getLog()
	{
		return this.log;
	}
	
	public File getError()
	{
		return this.error;
	}
	
	public File getOutput()
	{
		return this.output;
	}

	public void setSubmitScript(File submitScript)
	{
		this.submitScript = submitScript;
	}

	public void setLog(File log)
	{
		this.log = log;
	}

	public void setError(File error)
	{
		this.error = error;
	}

	public void setOutput(File output)
	{
		this.output = output;
	}
}
