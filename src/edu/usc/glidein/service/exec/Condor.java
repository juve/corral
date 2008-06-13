package edu.usc.glidein.service.exec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.globus.gsi.GlobusCredential;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.util.CommandLine;

/**
 * This class is an interface for managing condor jobs.
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class Condor
{
	private File condorHome;
	private File condorConfig;
	
	public Condor()
	{
		condorHome = null;
		condorConfig = null;
	}
	
	public Condor(String condorHome, String condorConfig)
	{
		setCondorConfig(condorConfig);
		setCondorHome(condorHome);
	}
	
	public static Condor getInstance() throws CondorException
	{
		String location = "java:comp/env/glidein/Condor";
		try {
			Context initialContext = new InitialContext();
	    	return (Condor)initialContext.lookup(location);
		} catch (Exception e) {
			throw new CondorException("Unable to load condor: "+location,e);
		}
	}
	
	public void setCondorConfig(String condorConfig)
	{
		this.condorConfig = new File(condorConfig);
	}
	
	public String getCondorConfig()
	{
		return condorConfig.getAbsolutePath();
	}
	
	public String getCondorHome()
	{
		return condorHome.getAbsolutePath();
	}
	
	public void setCondorHome(String condorHome)
	{
		this.condorHome = new File(condorHome);
	}
	
	/**
	 * Submit a job to the condor scheduler
	 * 
	 * @param job The job to submit
	 * @throws CondorException If the submission fails
	 */
	public void submitJob(CondorJob job)
	throws CondorException
	{
		// Create job directory if it doesn't exist
		File jobDirectory = job.getJobDirectory();
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
		
		// Generate submit script
		generateSubmitScript(job);
		
		// Run submit command
		CommandLine submit = new CommandLine();
		try 
		{
			// Find condor_submit executable
			File condorBin = new File(condorHome,"bin");
			File condorSubmit = new File(condorBin,"condor_submit");
			
			submit.setExecutable(condorSubmit);
			submit.setWorkingDirectory(job.getJobDirectory());
			
			// Arguments
			submit.addArgument("-verbose");
			submit.addArgument(job.getSubmitScript().getAbsolutePath());
			
			// Set environment
			submit.addEnvironmentVariable(
					"CONDOR_HOME",getCondorHome());
			submit.addEnvironmentVariable(
					"CONDOR_CONFIG",getCondorConfig());
			
			// Run condor_submit
			submit.execute();
		}
		catch(GlideinException se)
		{
			throw new CondorException("Unable to submit job",se);
		}
			
		// Check exit code and throw an exception if it failed
		int exit = submit.getExitCode();
		if(exit != 0)
		{
			throw new CondorException(
					"condor_submit failed with code "+exit+":\n\n"+
					"Standard out:\n"+submit.getOutput()+"\n"+
					"Standard error:\n"+submit.getError());
		}
		
		// Parse ID from output and update job object
		Pattern p = Pattern.compile("[*]{2} Proc (([0-9]+).([0-9]+)):");
		Matcher m = p.matcher(submit.getOutput());
		if(m.find())
		{
			job.setCondorId(m.group(1));
		}
		else
		{
			throw new CondorException("Unable to parse cluster and job id\n\n"+
					"Standard out:\n"+submit.getOutput()+"\n"+
					"Standard error:\n"+submit.getError());
		}
		
		// Attach event generator to log
		CondorEventGenerator gen = new CondorEventGenerator(job);
		gen.start();
	}
	
	/**
	 * Cancel a condor job
	 * 
	 * @param job The job to cancel
	 * @throws CondorException If there is an error cancelling the job
	 */
	public void cancelJob(String condorId)
	throws CondorException
	{	
		//Run rm command
		CommandLine cancel = new CommandLine();
		try 
		{
			// Find condor_rm executable
			File condorBin = new File(condorHome,"bin");
			File condorRm = new File(condorBin,"condor_rm");
			
			cancel.setExecutable(condorRm);
			
			// Arguments
			cancel.addArgument(condorId);
			
			// Set environment
			cancel.addEnvironmentVariable("CONDOR_HOME",
					getCondorHome());
			cancel.addEnvironmentVariable("CONDOR_CONFIG",
					getCondorConfig());
			
			// Run condor_rm
			cancel.execute();
		}
		catch(GlideinException se)
		{
			throw new CondorException("Unable to cancel job",se);
		}
		
		// Check exit code and throw an exception if it failed
		int exit = cancel.getExitCode();
		if(exit != 0)
		{
			throw new CondorException(
					"condor_rm failed with code "+exit+":\n\n"+
					"Standard out:\n\n"+cancel.getOutput()+"\n\n"+
					"Standard error:\n\n"+cancel.getError());
		}
	}
	
	/**
	 * Generate a condor job submit script
	 * @param job The job to generate the submit script for
	 * @throws CondorException If there is an error writing to the script file
	 */
	public void generateSubmitScript(CondorJob job)
	throws CondorException
	{
		File submitScript = job.getSubmitScript();
		try
		{
			FileWriter writer = new FileWriter(submitScript);
			try
			{
				writeSubmitScript(job,writer);
			}
			finally
			{
				writer.close();
			}
		}
		catch(IOException ioe)
		{
			throw new CondorException("Unable to generate submit script "+
					submitScript, ioe);
		}
	}
	
	/**
	 * Write a submit script for a job
	 * @param job The job to write the script for
	 * @param out The writer to write the script to
	 * @throws IOException If there is a problem accessing the writer
	 */
	public void writeSubmitScript(CondorJob job, Writer out)
	throws IOException, CondorException
	{	
		// UNIVERSE
		out.write("universe = "+job.getUniverse().getTypeString()+"\n");
		
		// Grid stuff
		if(job.getUniverse() == CondorUniverse.GRID)
		{
			// Set the grid resource string
			out.write("grid_resource = ");
			out.write(job.getGridType().getTypeString());
			out.write(" ");
			out.write(job.getGridContact());
			out.write("\n");
			
			// Don't stream in/out/err (default is True)
			out.write("stream_input = False\n");
			out.write("stream_output = False\n");
			out.write("stream_error = False\n");
			
			// Transfer all in/out/err (default is True)
			out.write("transfer_input = True\n");
			out.write("transfer_output = True\n");
			out.write("transfer_error = True\n");
			
			if(job.getGridType() == CondorGridType.GT2)
			{
				// Set globus_rsl
				out.write("globus_rsl = ");
				if(job.getProject() != null)
					out.write("(project="+job.getProject()+")");
				if(job.getQueue() != null)
					out.write("(queue="+job.getQueue()+")");
				out.write("(hostCount="+job.getHostCount()+")");
				out.write("(count="+job.getCount()+")");
				out.write("(jobType=multiple)");
				out.write("(maxTime="+job.getMaxTime()+")");
				out.write("\n");
			}
			else if(job.getGridType() == CondorGridType.GT4)
			{
				// Set globus_xml
				out.write("globus_xml = ");
				out.write("<count>"+job.getCount()+"</count>");
				out.write("<hostCount>"+job.getHostCount()+"</hostCount>");
				if(job.getProject() != null)
					out.write("<project>"+job.getProject()+"</project>");
				if(job.getQueue() != null)
					out.write("<queue>"+job.getQueue()+"</queue>");
				out.write("<maxTime>"+job.getMaxTime()+"</maxTime>");
				out.write("<jobType>multiple</jobType>");
				out.write("\n");
			}
			
			// X509 User Proxy
			GlobusCredential cred = job.getCredential();
			if(cred!=null)
			{
				File proxyFile = new File(job.getJobDirectory(),"proxy");
				try {
					// Save the credential to a file
					FileOutputStream pstream = new FileOutputStream(proxyFile);
					cred.save(pstream);
					pstream.close();
					
					// Change file permissions
					CommandLine chmod = new CommandLine();
					chmod.setExecutable(new File("/bin/chmod"));
					chmod.addArgument("600");
					chmod.addArgument(proxyFile.getAbsolutePath());
					chmod.execute();
					int exitCode = chmod.getExitCode();
					if(exitCode != 0){
						throw new CondorException(
								"Unable to change proxy permissions\n\n"+
								"Stdout:\n"+chmod.getOutput()+
								"Stderr:\n"+chmod.getError());
					}
				} catch(Exception e) {
					throw new CondorException("Unable to write proxy file",e);
				}
				out.write("x509userproxy = ");
				out.write(proxyFile.getAbsolutePath());
				out.write("\n");
			}
		}
		
		
		// EXECUTABLE
		out.write("executable = "+job.getExecutable()+"\n");
		if(!job.isLocalExecutable())
			out.write("transfer_executable = false\n");
		
		// Arguments
		List<String> args = job.getArguments();
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
		Map<String,String> env = job.getEnvironment();
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
		out.write("log = "+job.getLog().getAbsolutePath()+"\n");
		out.write("output = "+job.getOutput().getAbsolutePath()+"\n");
		out.write("error = "+job.getError().getAbsolutePath()+"\n");

		// Never notify user
		out.write("notification = Never\n");
		
		// Requirements
		if(job.getRequirements() != null)
			out.write("requirements = "+job.getRequirements()+"\n");
		
		// Directories
		if(job.getRemoteDirectory()!=null)
			out.write("remote_initialdir = "+
					job.getRemoteDirectory()+"\n");
		
		out.write("initialdir = "+
				job.getJobDirectory().getAbsolutePath()+"\n");
		
		
		// Input files
		List<String> infiles = job.getInputFiles();
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
		List<String> outfiles = job.getOutputFiles();
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
	
	public static void main(String[] args)
	{
		CondorJob job = new CondorJob(new File("/tmp/glidein_service/job-123"));
		job.setUniverse(CondorUniverse.GRID);
		//job.setGridType(CondorGridType.GT2);
		//job.setGridContact("dynamic.usc.edu/jobmanager-fork");
		job.setGridType(CondorGridType.GT4);
		job.setGridContact("https://grid-hg.ncsa.teragrid.org:8443/wsrf/services/ManagedJobFactoryService Fork");
		job.setExecutable("/bin/uname");
		//job.setLocalExecutable(true);
		job.addArgument("-f");
		//job.setHostCount(2);
		//job.setProcessCount(2);
		//job.setMaxTime(300);
		//job.setProject(null);
		//job.setQueue(null);
		//job.setRequirements(null);
		//job.setRemoteDirectory(new File("/etc"));
		//job.addInputFile(new File("/tmp/glidein_service/hello"));
		//job.addOutputFile(new File("hello"));

		// Create new job
		job.addListener(new CondorEventListener()
		{
			public void handleEvent(CondorEvent event) 
			{
				CondorJob job = event.getJob();
				System.out.println(job.getCondorId()+": "+event.getMessage());
				switch(event.getEventCode())
				{
					case JOB_TERMINATED:
						event.getGenerator().terminate();
						break;
					case EXCEPTION:
						event.getException().printStackTrace();
						break;
					case JOB_ABORTED:
						event.getGenerator().terminate();
						break;
				}
			}
		});
		
		try 
		{
			// Submit job
			Condor condor = new Condor();
			condor.submitJob(job);
			//try { Thread.sleep(5000); } catch(Exception e){}
			//condor.cancelJob(job);
		}
		catch(CondorException ce)
		{
			ce.printStackTrace();
			System.exit(1);
		}
	}
}
