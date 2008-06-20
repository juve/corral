package edu.usc.glidein.service.exec;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;

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
	public void submitJob(CondorJob job) throws CondorException
	{
		// Prepare the job
		job.prepareForSubmit();
		
		// Run submit command
		CommandLine submit = new CommandLine();
		try {
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
		} catch(IOException ioe) {
			throw new CondorException("Unable to submit job",ioe);
		}
			
		// Check exit code and throw an exception if it failed
		int exit = submit.getExitCode();
		if(exit != 0) {
			throw new CondorException(
					"condor_submit failed with code "+exit+":\n\n"+
					"Standard out:\n"+submit.getOutput()+"\n"+
					"Standard error:\n"+submit.getError());
		}
		
		// Parse condor job ID from output
		Pattern p = Pattern.compile("[*]{2} Proc (([0-9]+).([0-9]+)):");
		Matcher m = p.matcher(submit.getOutput());
		if(m.find()) {
			String jobid = m.group(1);
			job.setJobId(jobid);
			job.saveJobId();
		} else {
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
	public void cancelJob(String condorId) throws CondorException
	{	
		//Run rm command
		CommandLine cancel = new CommandLine();
		try {
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
		} catch(IOException ioe) {
			throw new CondorException("Unable to cancel job",ioe);
		}
		
		// Check exit code and throw an exception if it failed
		int exit = cancel.getExitCode();
		if(exit != 0) {
			throw new CondorException(
					"condor_rm failed with code "+exit+":\n\n"+
					"Standard out:\n\n"+cancel.getOutput()+"\n\n"+
					"Standard error:\n\n"+cancel.getError());
		}
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
				System.out.println(job.getJobId()+": "+event.getMessage());
				switch(event.getEventCode()) {
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
		
		try {
			// Submit job
			Condor condor = new Condor();
			condor.submitJob(job);
			//try { Thread.sleep(5000); } catch(Exception e){}
			//condor.cancelJob(job);
		} catch(CondorException ce) {
			ce.printStackTrace();
			System.exit(1);
		}
	}
}
