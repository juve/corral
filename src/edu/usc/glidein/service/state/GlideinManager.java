package edu.usc.glidein.service.state;

import java.io.File;

import org.apache.log4j.Logger;

import edu.usc.glidein.service.ServiceConfiguration;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.service.impl.GlideinResource;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.Base64;
import edu.usc.glidein.util.IOUtil;

public class GlideinManager
{
	private static Logger logger = Logger.getLogger(GlideinManager.class);
	
	/** TODO: Fix throws */
	public static void submit(GlideinResource resource) throws Exception
	{
		Site site = null; // TODO: Get site from SiteResourceHome
		Glidein glidein = resource.getGlidein();
		ServiceConfiguration config = ServiceConfiguration.getInstance();

		logger.debug("Submitting glidein job for site '"+site.getName()+"'");
		
		// Create working directory
		File jobDirectory = new File(config.getTempDir(),"glidein-"+glidein.getId());
		
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
		String run = config.getRun();
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
			configFile = config.getGlideinCondorConfig();
		}
		else
		{
			// Save config to a file in the submit directory
			configFile = "glidein_condor_config";
			String cfg = Base64.fromBase64(glidein.getCondorConfigBase64());
			IOUtil.write(cfg, new File(job.getJobDirectory(),configFile));
		}
		job.addInputFile(configFile);
		
		// Add a listener
		job.addListener(new GlideinListener());
		
		// Submit job
		Condor condor = Condor.getInstance();
		condor.submitJob(job);
		
		logger.debug("Submitted glidein job for site '"+site.getName()+"'");
	}
	
	public static void userCancelled(GlideinResource resource)
	{
		
	}
}
