package edu.usc.glidein.service.exec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.globus.gsi.GlobusCredential;

import edu.usc.glidein.util.CredentialUtil;

/**
 * A class representing a condor job.
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class CondorJob implements Serializable
{
	private static final long serialVersionUID = -3773144546382523651L;

	/** 
	 * Cluster ID assigned by condor schedd. The format is "clusterId.jobId"
	 */
	private String jobId;
	
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
	 * File where credential will be saved
	 */
	private File credentialFile;
	
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
	 * Total number of processes
	 */
	private int count;
	
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
	 * Globus proxy credential
	 */
	private GlobusCredential credential;
	
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
		this.credentialFile = new File(jobDirectory,"credential");
		this.listeners = new HashSet<CondorEventListener>();
		this.arguments = new LinkedList<String>();
		this.environment = new HashMap<String,String>();
		this.inputFiles = new LinkedList<String>();
		this.outputFiles = new LinkedList<String>();
		this.localExecutable = false;
		this.hostCount = 1;
		this.count = 1;
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

	public String getJobId()
	{
		return jobId;
	}

	public void setJobId(String jobId)
	{
		this.jobId = jobId;
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

	public GlobusCredential getCredential()
	{
		return credential;
	}

	public void setCredential(GlobusCredential credential)
	{
		this.credential = credential;
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

	public int getCount()
	{
		return count;
	}

	public void setCount(int count)
	{
		this.count = count;
	}
	
	public void setMaxTime(int maxTime)
	{
		this.maxTime = maxTime;
	}
	
	public int getMaxTime()
	{
		return this.maxTime;
	}
	
	public boolean hasCredential()
	{
		return credential != null;
	}
	
	public File getCredentialFile()
	{
		return credentialFile;
	}
	
	public void setCredentialFile(File credentialFile)
	{
		this.credentialFile = credentialFile;
	}
	
	public void saveCredential() throws CondorException
	{
		if (hasCredential()) {
			try {
				CredentialUtil.store(credential,getCredentialFile());
			} catch (Exception e) {
				throw new CondorException("Unable to save credential",e);
			}
		}
	}
	
	public void writeSubmitScript() throws CondorException
	{
		try {
			FileWriter writer = new FileWriter(submitScript);
			writeSubmitScript(writer);
			writer.close();
		} catch(IOException ioe) {
			throw new CondorException("Unable to write submit script",ioe);
		}
	}
	
	public void writeSubmitScript(Writer out) throws CondorException
	{
		try {
			_writeSubmitScript(out);
		} catch (IOException ioe) {
			throw new CondorException("Unable to write submit script",ioe);
		}
	}
	
	private void _writeSubmitScript(Writer out) throws IOException
	{	
		// UNIVERSE
		out.write("universe = "+getUniverse().getTypeString()+"\n");
		
		// Grid stuff
		if(getUniverse() == CondorUniverse.GRID)
		{
			// Set the grid resource string
			out.write("grid_resource = ");
			out.write(getGridType().getTypeString());
			out.write(" ");
			out.write(getGridContact());
			out.write("\n");
			
			// Don't stream in/out/err (default is True)
			out.write("stream_input = False\n");
			out.write("stream_output = False\n");
			out.write("stream_error = False\n");
			
			// Transfer all in/out/err (default is True)
			out.write("transfer_input = True\n");
			out.write("transfer_output = True\n");
			out.write("transfer_error = True\n");
			
			if(getGridType() == CondorGridType.GT2)
			{
				// Set globus_rsl
				out.write("globus_rsl = ");
				if(getProject() != null)
					out.write("(project="+getProject()+")");
				if(getQueue() != null)
					out.write("(queue="+getQueue()+")");
				out.write("(hostCount="+getHostCount()+")");
				out.write("(count="+getCount()+")");
				out.write("(jobType=multiple)");
				out.write("(maxTime="+getMaxTime()+")");
				out.write("\n");
			}
			else if(getGridType() == CondorGridType.GT4)
			{
				// Set globus_xml
				out.write("globus_xml = ");
				out.write("<count>"+getCount()+"</count>");
				out.write("<hostCount>"+getHostCount()+"</hostCount>");
				if(getProject() != null)
					out.write("<project>"+getProject()+"</project>");
				if(getQueue() != null)
					out.write("<queue>"+getQueue()+"</queue>");
				out.write("<maxTime>"+getMaxTime()+"</maxTime>");
				out.write("<jobType>multiple</jobType>");
				out.write("\n");
			}
			
			// X509 User Proxy
			if(hasCredential())
			{
				File credentialFile = getCredentialFile();
				out.write("x509userproxy = ");
				out.write(credentialFile.getAbsolutePath());
				out.write("\n");
			}
		}
		
		// EXECUTABLE
		out.write("executable = "+getExecutable()+"\n");
		if(!isLocalExecutable())
			out.write("transfer_executable = false\n");
		
		// Arguments
		List<String> args = getArguments();
		if(args.size()>0)
		{
			out.write("arguments = \"");
			for(String arg : args)
			{
				out.write(" ");
				out.write(arg);
			}
			out.write("\"\n");
		}
		
		
		// Environment
		Map<String,String> env = getEnvironment();
		if(env.size()>0)
		{
			out.write("environment =");
			for(String name : env.keySet())
			{
				out.write(" ");
				out.write(name);
				out.write("=");
				out.write(env.get(name));
			}
			out.write("\n");
		}
		
		// Log, output, error
		out.write("log = "+getLog().getAbsolutePath()+"\n");
		out.write("output = "+getOutput().getAbsolutePath()+"\n");
		out.write("error = "+getError().getAbsolutePath()+"\n");

		// Never notify user
		out.write("notification = Never\n");
		
		// Requirements
		if(getRequirements() != null)
			out.write("requirements = "+getRequirements()+"\n");
		
		// Directories
		if(getRemoteDirectory()!=null)
			out.write("remote_initialdir = "+
					getRemoteDirectory()+"\n");
		
		out.write("initialdir = "+
				getJobDirectory().getAbsolutePath()+"\n");
		
		
		// Input files
		List<String> infiles = getInputFiles();
		if(infiles.size() > 0)
		{
			out.write("transfer_input_files = ");
			int i = 0;
			for(String infile : infiles)
			{
				if(i++ > 0) out.write(",");
				out.write(infile);
			}
			out.write("\n");
		}
		
		
		// Output files
		List<String> outfiles = getOutputFiles();
		if(outfiles.size() > 0)
		{
			out.write("transfer_output_files = ");
			int i = 0;
			for(String outfile : outfiles)
			{
				if(i++ > 0) out.write(",");
				out.write(outfile);
			}
			out.write("\n");
		}
		
		
		// Not sure why this is needed for input files, but it is
		if(infiles.size() > 0 || outfiles.size() > 0)
		{
			out.write("when_to_transfer_output = ON_EXIT\n");
		}
		
		
		// Queue 1 job
		out.write("queue\n");
	}
	
	public void saveJobId() throws CondorException
	{
		File jobidFile = new File(jobDirectory,"jobid");
		try {
			boolean append = false;
			Writer writer = new FileWriter(jobidFile,append);
			writer.write(this.jobId);
			writer.write("\n");
			writer.close();
		} catch (IOException ioe) {
			throw new CondorException(
					"Unable to save Condor job id to file",ioe);
		}
	}
	
	public void prepareForSubmit() throws CondorException
	{
		// Create job directory if it doesn't exist
		File jobDirectory = getJobDirectory();
		if(!jobDirectory.exists())
		{
			if(!jobDirectory.mkdirs())
			{
				throw new CondorException(
						"Unable to create job directory: "+jobDirectory);
			}
		}
		
		// Save the credential if it exists
		saveCredential();
		
		// Generate submit script
		writeSubmitScript();
		
		// Delete the log file if it exists
		if (log.exists()) {
			log.delete();
		}
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
			job.setCount(1);
			job.setMaxTime(300);
			job.setProject(null);
			job.setQueue(null);
			job.setRequirements(null);
			job.setRemoteDirectory(null);
			job.setLocalExecutable(false);
			job.writeSubmitScript(new PrintWriter(System.out));
		}
		catch(Exception ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
