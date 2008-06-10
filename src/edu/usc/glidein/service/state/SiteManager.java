package edu.usc.glidein.service.state;

import java.io.File;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.ServiceConfiguration;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.service.impl.SiteResource;
import edu.usc.glidein.service.impl.SiteResourceHome;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;
import edu.usc.glidein.util.AddressingUtil;

public class SiteManager
{
	private static Logger logger = Logger.getLogger(SiteManager.class);
	
	/** TODO: Fix throws */
	public static void stage(SiteResource resource) throws Exception
	{
		Site site = resource.getSite();
		
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
		// TODO: job.setCredential(cred);
		
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
		job.addListener(new StageSiteListener(resource.getKey()));
		
		// Submit job
		Condor condor = Condor.getInstance();
		condor.submitJob(job);
			
		logger.debug("Submitted staging job for site '"+site.getName()+"'");
	}
	
	/** TODO: Fix throws */
	public static void cleanup(SiteResource resource) throws Exception
	{
		// Update status to prevent others from updating
		resource.updateStatus(SiteStatus.REMOVING, "Removing site");
		
		// TODO: Cancel staging operations
		
		// TODO: Cancel running glideins
		
		// TODO: Submit uninstall job
		
		// Delete the site
		resource.delete();
		
		// Remove the site from the resource home
		ResourceKey key = AddressingUtil.getSiteKey(resource.getSite().getId());
		SiteResourceHome resourceHome = SiteResourceHome.getInstance();
		resourceHome.remove(key);
	}
}
