/*
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.usc.corral.condor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.globus.gsi.GlobusCredential;

import edu.usc.corral.util.CredentialUtil;
import edu.usc.corral.util.FilesystemUtil;

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
	 * globus_rsl
	 */
	private String globusRSL;
	
	/**
	 * globus_xml
	 */
	private String globusXML;
	
	/**
	 * The owner of the job
	 */
	private String owner;
	
	/**
	 * Create a job with a given job directory and set of parameters
	 * @param jobDirectory The directory where the files for this job 
	 * should be created
	 * @param owner The username of the owner of this job
	 */
	public CondorJob(File jobDirectory, String owner)
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
		this.globusRSL = null;
		this.globusXML = null;
		this.owner = owner;
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
	
	public void setGlobusRSL(String globusRSL)
	{
		this.globusRSL = globusRSL;
	}
	
	public String getGlobusRSL()
	{
		return globusRSL;
	}
	
	public void setGlobusXML(String globusXML)
	{
		this.globusXML = globusXML;
	}
	
	public String getGlobusXML()
	{
		return globusXML;
	}
	
	public void setOwner(String owner)
	{
		this.owner = owner;
	}
	
	public String getOwner()
	{
		return owner;
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
			
			// Set globus_rsl
			if (getGlobusRSL()!=null) {
				out.write("globus_rsl = ");
				out.write(getGlobusRSL());
				out.write("\n");
			}
			
			// Set globus_xml
			if (getGlobusXML()!=null) {
				out.write("globus_xml = ");
				out.write(getGlobusXML());
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
			out.write("environment = ");
			for(String name : env.keySet())
			{
				out.write(name);
				out.write("=");
				out.write(env.get(name));
				out.write(";");
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
		
		// Change the ownership
		if (owner != null && !System.getProperty("user.name").equals(owner)) {
			FilesystemUtil.chmod(jobidFile, 644);
			FilesystemUtil.chown(jobidFile, owner);
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
			FilesystemUtil.rm(log);
		}
		
		// Change the ownership
		if (owner != null && !System.getProperty("user.name").equals(owner)) {
			FilesystemUtil.chown(jobDirectory, owner);
		}
	}
}
