package edu.usc.glidein.service.core;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.GlideinConfiguration;
import edu.usc.glidein.common.util.IOUtil;
import edu.usc.glidein.common.util.ProxyUtil;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.service.types.ExecutionService;
import edu.usc.glidein.service.types.GlideinDescription;
import edu.usc.glidein.service.types.PoolDescription;
import edu.usc.glidein.service.types.ServiceType;
import edu.usc.glidein.service.types.SiteDescription;

public class GlideinHandler implements Runnable, CondorEventListener
{
	private Logger logger = Logger.getLogger(GlideinHandler.class);
	
	private Pool pool;
	private Site site;
	private Glidein glidein;
	
	public GlideinHandler(Pool pool, Site site, Glidein glidein)
	{
		this.pool = pool;
		this.site = site;
		this.glidein = glidein;
	}
	
	public void run()
	{
		try 
		{
			if(logger.isDebugEnabled())
				logger.debug("Submitting glidein job for site '"+
							 site.getName()+"'");
			
			submitGlideinJob();
			
			if(logger.isDebugEnabled())
				logger.debug("Submitted glidein job for site '"+
							 site.getName()+"'");
		}
		catch(GlideinException ge)
		{
			glideinFailed("Unable to submit staging job",ge);
		}
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
		
		// TODO Update glidein status to success
		
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
		
		// TODO Update glidein status to failed
	}
	
	private void submitGlideinJob() throws GlideinException
	{
		GlideinConfiguration config = GlideinConfiguration.getInstance();
		
		// Create working directory
		File siteDirectory = site.getWorkingDirectory();
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
		job.setProxy(glideinService.getProxy());
		
		// Set glidein executable
		String run = config.getProperty("glidein.run");
		job.setExecutable(run);
		job.setLocalExecutable(true);
		
		// Set number of processes
		job.setHostCount(glidein.getHostCount());
		job.setCount(glidein.getCount());
		job.setMaxTime(glidein.getWallTime());
		
		// Add environment
		Map<String, String> env = site.getEnvironment();
		for (String key : env.keySet())
			job.addEnvironment(key, env.get(key));
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		job.addArgument("-localPath "+site.getLocalPath());
		job.addArgument("-condorHost "+pool.getCondorHost());
		job.addArgument("-wallTime "+glidein.getWallTime());
		if(glidein.getGcbBroker()!=null)
			job.addArgument("-gcbBroker "+glidein.getGcbBroker());
		if(glidein.getIdleTime()>0)
			job.addArgument("-idleTime "+glidein.getIdleTime());
		if(glidein.getDebug()!=null)
		{
			String[] debug = glidein.getDebug().split("[ ,]+");
			for(String level : debug)
				job.addArgument("-debug "+level);
		}
		if(glidein.getNumCpus()>0)
			job.addArgument("-numCpus "+glidein.getNumCpus());
		
		// If there is a special config file, use it
		String configFile = null;
		if (glidein.getConfiguration()==null)
		{
			configFile = config.getProperty("glidein.condor.config");
		}
		else
		{
			// Save config to a file in the submit directory
			configFile = "glidein_condor_config";
			try 
			{
				IOUtil.write(glidein.getConfiguration(), 
						new File(job.getJobDirectory(),configFile));
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
			glideinFailed("Unable to submit glidein job to Condor",ce);
		}
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
					condor.cancelJob(event.getJob());
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
			PoolDescription pd = new PoolDescription();
			pd.setCondorHost("array.usc.edu");
			pd.setCondorVersion("7.0.0");
			Pool p = PoolFactory.getInstance().createPool(pd);
			
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
			
			SiteDescription sd = new SiteDescription();
			sd.setName(name);
			sd.setInstallPath(installPath);
			sd.setLocalPath(localPath);
			sd.setStagingService(stagingService);
			sd.setGlideinService(glideinService);

			Site s = SiteFactory.getInstance().createSite(p.createSiteId(), sd);
			
			GlideinDescription gd = new GlideinDescription();
			gd.setWallTime(10); // 10 mins for test
			gd.setHostCount(1);
			gd.setNumCpus(2);
			gd.setGcbBroker(broker);
			//gd.setIdleTime(1); // 1 min for test
			//gd.setDebug("D_FULLDEBUG,D_DAEMONCORE");
			
			Glidein g = GlideinFactory.getInstance().createGlidein(
					s.createGlideinId(), gd);
			
			GlideinHandler h = new GlideinHandler(p,s,g);
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
