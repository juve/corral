package edu.usc.glidein.service.ops;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.ResourceException;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.ServiceConfiguration;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.service.impl.SiteResource;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;
import edu.usc.glidein.util.IOUtil;

public class StageSiteOperation implements CondorEventListener
{
	private Logger logger = Logger.getLogger(StageSiteOperation.class);
	
	private SiteResource resource = null;
	private Site site = null;
	private GlobusCredential cred = null;
	
	public StageSiteOperation(SiteResource resource, GlobusCredential cred)
	{
		this.resource = resource;
		this.site = resource.getSite();
		this.cred = cred;
	}
	
	public void invoke() throws GlideinException
	{
		logger.debug("Submitting staging job for site '"+site.getName()+"'");
		
		ServiceConfiguration config = ServiceConfiguration.getInstance();
		
		// Create working directory
		File jobDirectory = new File(config.getTempDir(),"site-"+site.getId());
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory);
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT2);
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(stagingService.getServiceContact());
		job.setProject(stagingService.getProject());
		job.setQueue(stagingService.getQueue());
		
		// Set glidein_install executable
		String install = config.getInstall();
		job.setExecutable(install);
		job.setLocalExecutable(true);
		job.setMaxTime(300); // Not longer than 5 mins
		job.setCredential(cred);
		
		// Add environment
		EnvironmentVariable env[] = site.getEnvironment();
		if (env!=null) {
			for (EnvironmentVariable var : env)
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		if (site.getCondorPackage()==null) {
			job.addArgument("-condorVersion "+site.getCondorVersion());
		} else {
			job.addArgument("-condorPackage "+site.getCondorPackage());
		}
		String[] urls = config.getStagingURLs();
		for(String url : urls) job.addArgument("-url "+url);
		
		// Add a listener
		job.addListener(this);
		
		// Submit job
		try
		{
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		}
		catch(CondorException ce)
		{
			throw new GlideinException("Unable to submit staging job to Condor",ce);
		}
		
		logger.debug("Submitted staging job for site '"+site.getName()+"'");
	}
	
	private void cleanup(CondorJob job)
	{
		// Delete staging job directory
		File dir = job.getJobDirectory();
		File[] files = dir.listFiles();
		for(File file : files) file.delete();
		dir.delete();
	}
	
	private void failed(String message, Exception e)
	{
		// Log message
		if(e==null) logger.error(message);
		else logger.error(message, e);
		
		// Update site status to staging failed
		updateStatus(SiteStatus.FAILED, message);
	}
	
	private void failed(String message)
	{
		failed(message, null);
	}
	
	private void checkSuccess(CondorJob job)
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
					failed("Staging job failed:\n"+stderr);
				else
					success(job);
			}
			catch(IOException ioe)
			{
				failed("Unable to read error file",ioe);
			}
		}
		else
		{
			failed("Staging job produced no error file");
		}
	}
	
	private void success(CondorJob job)
	{
		// Log message
		logger.debug("Staging successful for site '"+site.getName()+"'");
		
		// Update site status to success
		updateStatus(SiteStatus.READY,"Staging successful");
		
		// Cleanup
		cleanup(job);
	}
	
	public void handleEvent(CondorEvent event) 
	{
		switch(event.getEventCode())
		{
			case GLOBUS_SUBMIT:
				// Change job status to submitted
				updateStatus(SiteStatus.STAGING,"Staging job submitted");
				break;
			case JOB_TERMINATED:
				// If job terminated normally, check the return code
				event.getGenerator().terminate();
				checkSuccess(event.getJob());
				break;
			case EXCEPTION:
				// If there is an exception then fail
				failed("Error parsing staging job log", 
						event.getException());
				break;
			case JOB_HELD:
				// Kill job if it is held, job will become aborted
				try {
					Condor condor = Condor.getInstance();
					condor.cancelJob(event.getJob().getCondorId());
				} catch(CondorException ce) {
					failed("Unable to cancel held staging job", ce);
				}
				break;
			case JOB_ABORTED:
				// Fail all aborted jobs
				event.getGenerator().terminate();
				failed("Staging job aborted");
				break;
		}
	}
	
	private void updateStatus(SiteStatus status, String statusMessage)
	{
		try {
			resource.updateStatus(status, statusMessage);
		} catch (ResourceException re) {
			logger.error("Unable to update site status: "+re.getMessage(),re);
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
			s.setCondorPackage(condorPackage);
			
			SiteResource resource = new SiteResource(s);
			StageSiteOperation h = new StageSiteOperation(resource,null);
			h.invoke();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
