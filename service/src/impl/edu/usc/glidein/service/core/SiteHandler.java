package edu.usc.glidein.service.core;

import java.io.File;
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
		GlideinConfiguration config = GlideinConfiguration.getInstance();
		
		// Create working directory
		File siteDirectory = site.getWorkingDirectory();
		File jobDirectory = new File(siteDirectory,"stage");
		
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
		Map<String, String> env = site.getEnvironment();
		for (String key : env.keySet())
			job.addEnvironment(key, env.get(key));
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		if (site.getCondorPackage()==null)
			job.addArgument("-version "+pool.getCondorVersion());
		else
			job.addArgument("-package "+site.getCondorPackage());
		String urlstr = config.getProperty("glidein.staging.urls");
		String[] urls = urlstr.split("[ ,;\t]+");
		for(String url : urls) job.addArgument("-url "+url);
		
		// Get return code file
		job.addOutputFile("rc");
		
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
					stagingSuccess(job);
				else 
					stagingFailed("Staging job exited with non-zero " +
							"return code: "+result);
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
	
	private void stagingSuccess(CondorJob job)
	{
		// Log message
		logger.debug("Staging successful for site '"+site.getName()+"'");
		
		// Update site status to success
		SiteStatus status = 
			new SiteStatus(SiteStatusCode.READY,"Staging job successful");
		site.setStatus(status);
		
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
		
		try 
		{
			PoolDescription pd = new PoolDescription();
			pd.setCondorHost("juve.usc.edu");
			pd.setCondorVersion("7.0.0");
			Pool p = PoolFactory.getInstance().createPool(pd);
			
			String proxy = ProxyUtil.readProxy();
			
			ExecutionService stagingService = new ExecutionService();
			stagingService.setServiceType(ServiceType.GT2);
			stagingService.setServiceContact("dynamic.usc.edu:2119/jobmanager-fork");
			stagingService.setProxy(proxy);
			
			ExecutionService glideinService = new ExecutionService();
			glideinService.setServiceType(ServiceType.GT2);
			glideinService.setServiceContact("dynamic.usc.edu:2119/jobmanager-pbs");
			glideinService.setProxy(proxy);
			glideinService.setQueue("normal");
			glideinService.setProject("nqi");
			
			SiteDescription sd = new SiteDescription();
			sd.setName("dynamic");
			//sd.setInstallPath("/u/ac/juve/glidein");
			sd.setInstallPath("/home/geovault-00/juve/glidein");
			//sd.setLocalPath("/cfs/scratch/users/juve/glidein");
			sd.setLocalPath("/home/geovault-00/juve/glidein/local");
			sd.setStagingService(stagingService);
			sd.setGlideinService(glideinService);
			sd.setCondorPackage("7.0.0-x86-Linux-2.6-glibc2.3.tar.gz");
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
