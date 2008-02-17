package edu.usc.glidein.service.core;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.common.util.IOUtil;
import edu.usc.glidein.common.util.ProxyUtil;
import edu.usc.glidein.service.ServiceConfiguration;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorJobDescription;
import edu.usc.glidein.service.exec.CondorJobFactory;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.service.types.ExecutionService;
import edu.usc.glidein.service.types.PoolDescription;
import edu.usc.glidein.service.types.ServiceType;
import edu.usc.glidein.service.types.SiteDescription;

public class SiteHandler implements Runnable, CondorEventListener
{
	private Logger logger = Logger.getLogger(SiteHandler.class);
	
	private Site site = null;
	private Pool pool = null;
	
	public SiteHandler(Pool pool, Site site)
	{
		this.pool = pool;
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
		// Create a job description
		CondorJobDescription jd = new CondorJobDescription();
		jd.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType()))
			jd.setGridType(CondorGridType.GT2);
		else
			jd.setGridType(CondorGridType.GT4);
		jd.setGridContact(stagingService.getServiceContact());
		jd.setProject(stagingService.getProject());
		jd.setQueue(stagingService.getQueue());
		jd.setProxy(stagingService.getProxy());
		
		// Set glidein_install executable
		// TODO Fix executable path
		jd.setExecutable(new File("/Users/juve/Workspace/GlideinService/service/bin/glidein_install"));
		jd.setLocalExecutable(true);
		jd.setMaxTime(300); // Not longer than 5 mins
		
		// Add arguments
		jd.addArgument(site.getInstallPath());
		jd.addArgument(pool.getCondorVersion());
		ServiceConfiguration config = ServiceConfiguration.getInstance();
		jd.addArgument(config.getProperty("glidein.servers"));
		
		// Get return code file
		jd.addOutputFile(new File("rc"));
		
		// Submit job
		try
		{
			Condor condor = new Condor();
			CondorJob stagingJob = new CondorJobFactory().createJob(jd);
			stagingJob.addListener(this);
			condor.submitJob(stagingJob);
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
		
		// Update site status to staging failed
		SiteStatus status =
			new SiteStatus(SiteStatusCode.FAILED,message,e);
		site.setStatus(status);
	}
	
	private void stagingFailed(String message)
	{
		stagingFailed(message, null);
	}
	
	private void checkStagingSuccess(CondorJob job)
	{
		File dir = job.getJobDirectory();
		File rcFile = new File(dir,"rc");
		if(rcFile.exists())
		{
			try
			{
				String result = IOUtil.read(rcFile);
				String[] tmp = result.split("[ ]", 2);
				if(tmp.length!=2) 
					stagingFailed("Unable to parse staging job return code");
				int rc = Integer.parseInt(tmp[0]);
				if(rc==0)
				{
					stagingSuccess();
				} 
				else
				{
					stagingFailed("Staging job exited with " +
							"non-zero return code: "+result);
				}
			}
			catch(Exception ioe)
			{
				stagingFailed("Unable to read rc file");
			}
		}
		else
		{
			stagingFailed("Staging job produced no rc file");
		}
	}
	
	private void stagingSuccess()
	{
		// Log message
		logger.debug("Staging successful for site '"+site.getName()+"'");
		
		// Update site status to success
		SiteStatus status = 
			new SiteStatus(SiteStatusCode.READY,"Staging job successful");
		site.setStatus(status);
	}
	
	public void handleEvent(CondorEvent event) 
	{
		switch(event.getEventCode())
		{
			case JOB_TERMINATED:
				// If job terminated normally, check the return code
				event.getGenerator().terminate();
				checkStagingSuccess(event.getJob());
				cleanupStagingJob(event.getJob());
				break;
			case EXCEPTION:
				// If there is an exception then fail
				stagingFailed("Error parsing staging job log", 
						event.getException());
				cleanupStagingJob(event.getJob());
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
				cleanupStagingJob(event.getJob());
				break;
		}
	}
	
	
	
	public static void main(String[] args)
	{
		// Configure logging
		System.setProperty("log4j.defaultInitOverride", "true");
		BasicConfigurator.configure();
		
		try 
		{
			PoolDescription pd = new PoolDescription();
			pd.setCondorHost("juve.usc.edu");
			pd.setCondorPort(Pool.DEFAULT_CONDOR_PORT);
			pd.setCondorVersion("7.0.0");
			Pool p = PoolFactory.getInstance().createPool(pd);
			
			String proxy = ProxyUtil.readProxy();
			
			ExecutionService stagingService = new ExecutionService();
			stagingService.setServiceType(ServiceType.GT2);
			stagingService.setServiceContact("grid-abe.ncsa.teragrid.org:2119/jobmanager-fork");
			stagingService.setProxy(proxy);
			
			ExecutionService glideinService = new ExecutionService();
			glideinService.setServiceType(ServiceType.GT2);
			glideinService.setServiceContact("grid-abe.ncsa.teragrid.org:2119/jobmanager-pbs");
			glideinService.setProxy(proxy);
			glideinService.setQueue("normal");
			glideinService.setProject("nqi");
			
			SiteDescription sd = new SiteDescription();
			sd.setName("abe");
			sd.setInstallPath("/u/ac/juve/glidein");
			sd.setLocalPath("/cfs/scratch/users/juve/glidein");
			sd.setStagingService(stagingService);
			sd.setGlideinService(glideinService);
			Site s = SiteFactory.getInstance().createSite(p.createSiteId(), sd);
			
			SiteHandler h = new SiteHandler(p,s);
			s.setHandler(h);
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
