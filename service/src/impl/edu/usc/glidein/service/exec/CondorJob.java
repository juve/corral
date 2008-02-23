package edu.usc.glidein.service.exec;

import java.io.CharArrayWriter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	 * Path to the executable
	 */
	private String executable;
	
	/**
	 * Is the executable on the submit machine or remote?
	 */
	private boolean localExecutable;
	
	/**
	 * Job arguments 
	 */
	private List<String> arguments;
	
	/**
	 * Environment variables for remote executable
	 */
	private Map<String,String> environment;
	
	/**
	 * Job universe
	 */
	private CondorUniverse universe = CondorUniverse.GRID;
	
	/**
	 * Condor requirements for job 
	 */
	private String requirements;
	
	/**
	 * The target grid type
	 */
	private CondorGridType gridType;
	
	/**
	 * Contact string for grid universe jobs
	 */
	private String gridContact;
	
	/**
	 * Project for accounting
	 */
	private String project;
	
	/**
	 * Target queue
	 */
	private String queue;
	
	/**
	 * Number of hosts
	 */
	private int hostCount;
	
	/**
	 * Number of processes per host
	 */
	private int processCount;
	
	/**
	 * Max runtime (walltime or cputime) of the application in minutes.
	 */
	private int maxTime;
	
	/**
	 * Files to transfer from the submit machine to the remote machine
	 */
	private List<String> inputFiles;
	
	/**
	 * Files to transfer from the remote machine back to the submit machine
	 */
	private List<String> outputFiles;
	
	/**
	 * Globus proxy credential (the actual credential, not the file)
	 */
	private String proxy;
	
	/**
	 * remote_initialdir
	 */
	private String remoteDirectory;
	
	/**
	 * Create a job with a given job directory and set of parameters
	 * @param jobDirectory The directory where the files for this job 
	 * should be created
	 */
	public CondorJob(File jobDirectory)
	{
		this.jobDirectory = jobDirectory;
		this.log = new File(jobDirectory,"log");
		this.error = new File(jobDirectory,"error");
		this.output = new File(jobDirectory,"output");
		this.submitScript = new File(jobDirectory,"submit");
		this.listeners = new HashSet<CondorEventListener>();
		this.arguments = new LinkedList<String>();
		this.environment = new HashMap<String,String>();
		this.inputFiles = new LinkedList<String>();
		this.outputFiles = new LinkedList<String>();
		this.localExecutable = false;
		this.hostCount = 1;
		this.processCount = 1;
		this.queue = null;
		this.project = null;
		this.maxTime = 1;
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
	
	public String getExecutable()
	{
		return executable;
	}

	public void setExecutable(String executable)
	{
		this.executable = executable;
	}

	public List<String> getArguments()
	{
		return arguments;
	}

	public void addArgument(String arg)
	{
		arguments.add(arg);
	}

	public Map<String, String> getEnvironment()
	{
		// TODO clone environment?
		return environment;
	}

	public void addEnvironment(String variable, String value)
	{
		environment.put(variable, value);
	}

	public CondorUniverse getUniverse()
	{
		return universe;
	}

	public void setUniverse(CondorUniverse universe)
	{
		this.universe = universe;
	}

	public String getRequirements()
	{
		return requirements;
	}

	public void setRequirements(String requirements)
	{
		this.requirements = requirements;
	}
	
	public void setGridType(CondorGridType gridType)
	{
		this.gridType = gridType;
	}
	
	public CondorGridType getGridType()
	{
		return this.gridType;
	}

	public String getGridContact()
	{
		return gridContact;
	}

	public void setGridContact(String gridContact)
	{
		this.gridContact = gridContact;
	}
	
	public void setGridResource(CondorGridType gridType, String gridContact)
	{
		setGridType(gridType);
		setGridContact(gridContact);
	}
	
	public List<String> getInputFiles()
	{
		return inputFiles;
	}
	
	public void addInputFile(String inputFile)
	{
		inputFiles.add(inputFile);
	}

	public List<String> getOutputFiles()
	{
		return outputFiles;
	}

	public void addOutputFile(String outputFile)
	{
		this.outputFiles.add(outputFile);
	}

	public String getProxy()
	{
		return proxy;
	}

	public void setProxy(String proxy)
	{
		this.proxy = proxy;
	}

	public String getRemoteDirectory()
	{
		return remoteDirectory;
	}

	public void setRemoteDirectory(String remoteDirectory)
	{
		this.remoteDirectory = remoteDirectory;
	}
	
	public boolean isLocalExecutable()
	{
		return localExecutable;
	}

	public void setLocalExecutable(boolean localExecutable)
	{
		this.localExecutable = localExecutable;
	}
	
	public String getProject()
	{
		return project;
	}

	public void setProject(String project)
	{
		this.project = project;
	}

	public String getQueue()
	{
		return queue;
	}

	public void setQueue(String queue)
	{
		this.queue = queue;
	}

	public int getHostCount()
	{
		return hostCount;
	}

	public void setHostCount(int hostCount)
	{
		this.hostCount = hostCount;
	}

	public int getProcessCount()
	{
		return processCount;
	}

	public void setProcessCount(int processCount)
	{
		this.processCount = processCount;
	}
	
	public void setMaxTime(int maxTime)
	{
		this.maxTime = maxTime;
	}
	
	public int getMaxTime()
	{
		return this.maxTime;
	}
	
	public static void main(String[] args)
	{	
		try 
		{
			CondorJob job = new CondorJob(new File("/tmp"));
			job.setUniverse(CondorUniverse.GRID);
			job.setGridType(CondorGridType.GT2);
			job.setGridContact("tg-login.sdsc.teragrid.org/jobmanager-pbs");
			job.setExecutable("/bin/ls");
			job.setHostCount(1);
			job.setProcessCount(1);
			job.setMaxTime(300);
			job.setProject(null);
			job.setQueue(null);
			job.setRequirements(null);
			job.setRemoteDirectory(null);
			job.setLocalExecutable(false);
			
			CharArrayWriter out = new CharArrayWriter();
			new Condor().writeSubmitScript(job,out);
			System.out.println(out.toString());
		}
		catch(Exception ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
