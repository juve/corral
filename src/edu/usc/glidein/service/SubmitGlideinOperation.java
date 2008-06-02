package edu.usc.glidein.service;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.usc.glidein.GlideinConfiguration;
import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.util.Base64;
import edu.usc.glidein.util.IOUtil;

public class SubmitGlideinOperation implements CondorEventListener
{
	private Logger logger = Logger.getLogger(SubmitGlideinOperation.class);
	
	private Site site;
	private Glidein glidein;
	
	public SubmitGlideinOperation(Site site, Glidein glidein)
	{
		this.site = site;
		this.glidein = glidein;
	}
	
	private void checkGlideinSuccess(CondorJob job)
	{
		// A job finished successfully if it didn't produce any
		// output on stderr. Its ugly, but GT2 is broken.
		File error = job.getError();
		if(error.exists())
		{
			try
			{
				String stderr = IOUtil.read(error);
				if(stderr.length()>0)
					glideinFailed("Glidein failed:\n"+stderr);
				else
					glideinSuccess(job);
			}
			catch(IOException ioe)
			{
				glideinFailed("Unable to read error file",ioe);
			}
		}
		else
		{
			glideinFailed("Glidein job produced no error file");
		}
	}
	
	private void cleanupGlideinJob(CondorJob job)
	{
		// Delete staging job directory
		File dir = job.getJobDirectory();
		File[] files = dir.listFiles();
		for(File file : files) file.delete();
		dir.delete();
	}
	
	private void glideinSuccess(CondorJob job)
	{
		// Log message
		logger.debug("Glidein exited successfully");
		
		// TODO: Update glidein status to success
		//glidein.setStatus(new GlideinStatus(GlideinStatusCode.TERMINATED,
		//									"Glidein exited successfully"));
		
		// Cleanup
		cleanupGlideinJob(job);
	}
	
	private void glideinFailed(String message)
	{
		glideinFailed(message,null);
	}
	
	private void glideinFailed(String message, Exception e)
	{
		// Log message
		if(e==null) logger.error(message);
		else logger.error(message, e);
		
		// TODO: Update glidein status to failed
		//glidein.setStatus(new GlideinStatus(GlideinStatusCode.FAILED,
		//									message));
	}
	
	private void invoke() throws GlideinException
	{
		logger.debug("Submitting glidein job for site '"+site.getName()+"'");
		
		GlideinConfiguration config = GlideinConfiguration.getInstance();
		
		// Create working directory
		File siteDirectory = new File(site.getSubmitPath());
		File jobDirectory = new File(siteDirectory,"glidein-"+glidein.getId());
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory);
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService glideinService = site.getGlideinService();
		if(ServiceType.GT2.equals(glideinService.getServiceType()))
			job.setGridType(CondorGridType.GT2);
		else
			job.setGridType(CondorGridType.GT4);
		job.setGridContact(glideinService.getServiceContact());
		job.setProject(glideinService.getProject());
		job.setQueue(glideinService.getQueue());
		
		// Set glidein executable
		String run = config.getProperty("glidein.run");
		job.setExecutable(run);
		job.setLocalExecutable(true);
		
		// Set number of processes
		job.setHostCount(glidein.getHostCount());
		job.setCount(glidein.getCount());
		job.setMaxTime(glidein.getWallTime());
		
		// Add environment
		EnvironmentVariable env[] = site.getEnvironment();
		if (env!=null) {
			for (EnvironmentVariable var : env)
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		job.addArgument("-localPath "+site.getLocalPath());
		job.addArgument("-condorHost "+glidein.getCondorHost());
		job.addArgument("-wallTime "+glidein.getWallTime());
		if(glidein.getGcbBroker()!=null)
			job.addArgument("-gcbBroker "+glidein.getGcbBroker());
		if(glidein.getIdleTime()>0)
			job.addArgument("-idleTime "+glidein.getIdleTime());
		if(glidein.getCondorDebug()!=null)
		{
			String[] debug = glidein.getCondorDebug().split("[ ,]+");
			for(String level : debug)
				job.addArgument("-debug "+level);
		}
		if(glidein.getNumCpus()>0)
			job.addArgument("-numCpus "+glidein.getNumCpus());
		
		// If there is a special config file, use it
		String configFile = null;
		if (glidein.getCondorConfigBase64()==null)
		{
			configFile = config.getProperty("glidein.condor.config");
		}
		else
		{
			// Save config to a file in the submit directory
			configFile = "glidein_condor_config";
			try 
			{
				String cfg = Base64.fromBase64(glidein.getCondorConfigBase64());
				IOUtil.write(cfg, new File(job.getJobDirectory(),configFile));
			} 
			catch(IOException ioe)
			{
				throw new GlideinException(
						"Unable to save configuration to file",ioe);
			}
		}
		job.addInputFile(configFile);
		
		// Add a listener
		job.addListener(this);
		
		// Submit job
		try
		{
			Condor condor = new Condor();
			condor.submitJob(job);
		}
		catch(CondorException ce)
		{
			throw new GlideinException("Unable to submit glidein job to Condor",ce);
		}
		
		logger.debug("Submitted glidein job for site '"+site.getName()+"'");
	}
	
	public void handleEvent(CondorEvent event)
	{
		switch(event.getEventCode())
		{
			case JOB_TERMINATED:
				// If job terminated normally, check the return code
				event.getGenerator().terminate();
				checkGlideinSuccess(event.getJob());
				break;
			case EXCEPTION:
				// If there is an exception then fail
				glideinFailed("Error parsing glidein job log", 
						event.getException());
				break;
			case JOB_HELD:
				// Kill job if it is held, job will become
				// aborted
				try
				{
					Condor condor = new Condor();
					condor.cancelJob(event.getJob().getCondorId());
				} 
				catch(CondorException ce)
				{
					glideinFailed("Unable to cancel held glidein job", ce);
				}
				break;
			case JOB_ABORTED:
				// Fail all aborted jobs
				event.getGenerator().terminate();
				glideinFailed("Glidein job aborted");
				break;
		}
	}
	
	public static void main(String[] args)
	{
		// Configure logging
		System.setProperty("log4j.defaultInitOverride", "true");
		BasicConfigurator.configure();
		
//		String name = "dynamic";
//		String installPath = "/home/geovault-00/juve/glidein";
//		String localPath = "/home/geovault-00/juve/glidein/local";
//		String fork = "dynamic.usc.edu:2119/jobmanager-fork";
//		String pbs = "dynamic.usc.edu:2119/jobmanager-pbs";
//		String queue = null;
//		String project = null;
//		String broker = "128.125.25.48";
		
		String name = "sdsc";
		String installPath = "/users/gideon/glidein";
		String localPath = "/gpfs/gideon/glidein/local";
		String fork = "tg-login.sdsc.teragrid.org/jobmanager-fork";
		String pbs = "tg-login.sdsc.teragrid.org/jobmanager-pbs";
		String queue = "dque";
		String project = "CSB246";
		String broker = null;
		
//		String name = "mercury";
//		String installPath = "/users/gideon/glidein";
//		String localPath = "/gpfs/gideon/glidein/local";
//		String fork = "grid-hg.ncsa.teragrid.org/jobmanager-fork";
//		String pbs = "grid-hg.ncsa.teragrid.org/jobmanager-pbs";
//		String queue = "normal";
//		String project = "nqi";
//		String broker = null;
		
		try 
		{
			ExecutionService stagingService = new ExecutionService();
			stagingService.setServiceType(ServiceType.GT2);
			stagingService.setServiceContact(fork);
			
			ExecutionService glideinService = new ExecutionService();
			glideinService.setServiceType(ServiceType.GT2);
			glideinService.setServiceContact(pbs);
			glideinService.setQueue(queue);
			glideinService.setProject(project);
			
			Site s = new Site();
			s.setCondorVersion("7.0.0");
			s.setName(name);
			s.setInstallPath(installPath);
			s.setLocalPath(localPath);
			s.setStagingService(stagingService);
			s.setGlideinService(glideinService);
			
			
			Glidein g = new Glidein();
			g.setCondorHost("array.usc.edu");
			g.setWallTime(10); // 10 mins for test
			g.setHostCount(1);
			g.setNumCpus(2);
			g.setGcbBroker(broker);
			//gd.setIdleTime(1); // 1 min for test
			//gd.setDebug("D_FULLDEBUG,D_DAEMONCORE");
			
			SubmitGlideinOperation h = new SubmitGlideinOperation(s,g);
			h.invoke();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
