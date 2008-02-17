package edu.usc.glidein.service.exec;

import java.io.CharArrayWriter;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to describe all the parameters for a condor job.
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class CondorJobDescription
{
	/**
	 * Path to the executable
	 */
	private File executable;
	
	/**
	 * Is the executable on the submit machine or remote?
	 */
	private boolean localExecutable;
	/**
	 * 
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
	private List<File> inputFiles;
	
	/**
	 * Files to transfer from the remote machine back to the submit machine
	 */
	private List<File> outputFiles;
	
	/**
	 * Globus proxy credential (the actual credential, not the file)
	 */
	private String proxy;
	
	/**
	 * remote_initialdir
	 */
	private File remoteDirectory;
	
	public CondorJobDescription()
	{
		arguments = new LinkedList<String>();
		environment = new HashMap<String,String>();
		inputFiles = new LinkedList<File>();
		outputFiles = new LinkedList<File>();
		localExecutable = false;
		hostCount = 1;
		processCount = 1;
		queue = null;
		project = null;
		maxTime = 1;
	}

	public File getExecutable()
	{
		return executable;
	}

	public void setExecutable(File executable)
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
	
	public List<File> getInputFiles()
	{
		return inputFiles;
	}
	
	public void addInputFile(File inputFile)
	{
		inputFiles.add(inputFile);
	}

	public List<File> getOutputFiles()
	{
		return outputFiles;
	}

	public void addOutputFile(File outputFile)
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

	public File getRemoteDirectory()
	{
		return remoteDirectory;
	}

	public void setRemoteDirectory(File remoteDirectory)
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
		CondorJobDescription jd = new CondorJobDescription();
		jd.setUniverse(CondorUniverse.GRID);
		jd.setGridType(CondorGridType.GT2);
		jd.setGridContact("tg-login.sdsc.teragrid.org/jobmanager-pbs");
		jd.setExecutable(new File("/bin/ls"));
		jd.setHostCount(1);
		jd.setProcessCount(1);
		jd.setMaxTime(300);
		jd.setProject(null);
		jd.setQueue(null);
		jd.setRequirements(null);
		jd.setRemoteDirectory(null);
		jd.setLocalExecutable(false);
		
		try 
		{
			CondorJob job = new CondorJob(new File("/tmp"),jd);
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