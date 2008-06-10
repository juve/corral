package edu.usc.glidein.service.state;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEvent;
import edu.usc.glidein.service.exec.CondorEventListener;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.util.IOUtil;

public class GlideinListener implements CondorEventListener
{
	private Logger logger = Logger.getLogger(GlideinListener.class);
	
	public GlideinListener() { }
	
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
					Condor condor = Condor.getInstance();
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
			
			// TODO: Figure out how to test
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
