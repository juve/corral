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
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;
import edu.usc.glidein.stubs.types.SiteStatusCode;
import edu.usc.glidein.util.IOUtil;
import edu.usc.glidein.util.ProxyUtil;

public class SiteHandler implements Runnable, CondorEventListener
{
	private Logger logger = Logger.getLogger(SiteHandler.class);
	
	private Site site = null;
	
	public SiteHandler(Site site)
	{
		this.site = site;
	}
	
	public void run()
	{
		try 
		{
			if(logger.isDebugEnabled())
				logger.debug("Submitting staging job for site '"+
							 site.getName()+"'");
			
			submitStagingJob();
			
			if(logger.isDebugEnabled())
				logger.debug("Submitted staging job for site '"+
							 site.getName()+"'");
		}
		catch(GlideinException ge)
		{
			stagingFailed("Unable to submit staging job",ge);
		}
	}
	
	private void submitStagingJob() throws GlideinException
	{
		GlideinConfiguration config = GlideinConfiguration.getInstance();
		
		// Create working directory
		File varDirectory = new File(config.getProperty("glidein.var"));
		File jobDirectory = new File(varDirectory,"site-"+site.getId());
		
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory);
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType()))
			job.setGridType(CondorGridType.GT2);
		else
			job.setGridType(CondorGridType.GT4);
		job.setGridContact(stagingService.getServiceContact());
		job.setProject(stagingService.getProject());
		job.setQueue(stagingService.getQueue());
		job.setProxy(stagingService.getProxy());
		
		// Set glidein_install executable
		String install = config.getProperty("glidein.install");
		job.setExecutable(install);
		job.setLocalExecutable(true);
		job.setMaxTime(300); // Not longer than 5 mins
		
		// Add environment
		EnvironmentVariable env[] = site.getEnvironment();
		if (env!=null) {
			for (EnvironmentVariable var : env)
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		if (site.getCondorPackage()==null)
			job.addArgument("-condorVersion "+site.getCondorVersion());
		else
			job.addArgument("-condorPackage "+site.getCondorPackage());
		String urlstr = config.getProperty("glidein.staging.urls");
		String[] urls = urlstr.split("[ ,;\t]+");
		for(String url : urls) job.addArgument("-url "+url);
		
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
			stagingFailed("Unable to submit staging job to Condor",ce);
		}
	}
	
	private void cleanupStagingJob(CondorJob job)
	{
		// Delete staging job directory
		File dir = job.getJobDirectory();
		File[] files = dir.listFiles();
		for(File file : files) file.delete();
		dir.delete();
	}
	
	private void stagingFailed(String message, Exception e)
	{
		// Log message
		if(e==null) logger.error(message);
		else logger.error(message, e);
		
		// TODO Update site status to staging failed
		//site.setStatus(new SiteStatus(SiteStatusCode.FAILED, message));
	}
	
	private void stagingFailed(String message)
	{
		stagingFailed(message, null);
	}
	
	private void checkStagingSuccess(CondorJob job)
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
					stagingFailed("Staging job failed:\n"+stderr);
				else
					stagingSuccess(job);
			}
			catch(IOException ioe)
			{
				stagingFailed("Unable to read error file",ioe);
			}
		}
		else
		{
			stagingFailed("Staging job produced no error file");
		}
	}
	
	private void stagingSuccess(CondorJob job)
	{
		// Log message
		logger.debug("Staging successful for site '"+site.getName()+"'");
		
		// TODO Update site status to success
		//site.setStatus(new SiteStatus(SiteStatusCode.READY,"Staging successful"));
		
		// Cleanup
		cleanupStagingJob(job);
	}
	
	public void handleEvent(CondorEvent event) 
	{
		switch(event.getEventCode())
		{
			case JOB_TERMINATED:
				// If job terminated normally, check the return code
				event.getGenerator().terminate();
				checkStagingSuccess(event.getJob());
				break;
			case EXCEPTION:
				// If there is an exception then fail
				stagingFailed("Error parsing staging job log", 
						event.getException());
				break;
			case JOB_HELD:
				// Kill job if it is held, job will become
				// aborted
				try {
					Condor condor = new Condor();
					condor.cancelJob(event.getJob());
				} catch(CondorException ce) {
					stagingFailed("Unable to cancel held staging job", ce);
				}
				break;
			case JOB_ABORTED:
				// Fail all aborted jobs
				event.getGenerator().terminate();
				stagingFailed("Staging job aborted");
				break;
		}
	}
	
	public static void main(String[] args)
	{
		// Configure logging
		System.setProperty("log4j.defaultInitOverride", "true");
		BasicConfigurator.configure();
		
		String name = "dynamic";
		String installPath = "/home/geovault-00/juve/glidein";
		String localPath = "/home/geovault-00/juve/glidein/local";
		String fork = "dynamic.usc.edu:2119/jobmanager-fork";
		String pbs = "dynamic.usc.edu:2119/jobmanager-pbs";
		String queue = null;
		String project = null;
		String condorPackage = "7.0.0-x86-Linux-2.6-glibc2.3.tar.gz";
		
//		String name = "sdsc";
//		String installPath = "/users/gideon/glidein";
//		String localPath = "/gpfs/gideon/glidein/local";
//		String fork = "tg-login.sdsc.teragrid.org/jobmanager-fork";
//		String pbs = "tg-login.sdsc.teragrid.org/jobmanager-pbs";
//		String queue = "dque";
//		String project = "CSB246";
//		String condorPackage = null;
		
//		String name = "mercury";
//		String installPath = "/users/gideon/glidein";
//		String localPath = "/gpfs/gideon/glidein/local";
//		String fork = "grid-hg.ncsa.teragrid.org/jobmanager-fork";
//		String pbs = "grid-hg.ncsa.teragrid.org/jobmanager-pbs";
//		String queue = "normal";
//		String project = "nqi";
//		String condorPackage = null;
		
		try 
		{
			String proxy = ProxyUtil.readProxy();
			
			ExecutionService stagingService = new ExecutionService();
			stagingService.setServiceType(ServiceType.GT2);
			stagingService.setServiceContact(fork);
			stagingService.setProxy(proxy);
			
			ExecutionService glideinService = new ExecutionService();
			glideinService.setServiceType(ServiceType.GT2);
			glideinService.setServiceContact(pbs);
			glideinService.setProxy(proxy);
			glideinService.setQueue(queue);
			glideinService.setProject(project);
			
			Site s = new Site();
			s.setCondorVersion("7.0.0");
			s.setName(name);
			s.setInstallPath(installPath);
			s.setLocalPath(localPath);
			s.setStagingService(stagingService);
			s.setGlideinService(glideinService);
			s.setCondorPackage(condorPackage);
			
			SiteHandler h = new SiteHandler(s);
			Thread t = new Thread(h);
			t.start();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
