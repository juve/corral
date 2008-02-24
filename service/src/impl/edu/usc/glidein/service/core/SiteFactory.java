package edu.usc.glidein.service.core;

import java.io.File;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.GlideinConfiguration;
import edu.usc.glidein.service.types.ExecutionService;
import edu.usc.glidein.service.types.EnvironmentVariable;
import edu.usc.glidein.service.types.SiteDescription;

public class SiteFactory 
{
	private static SiteFactory singleton;
	
	private SiteFactory() { }
	
	public synchronized static SiteFactory getInstance()
	{
		if(singleton == null){
			singleton = new SiteFactory();
		}
		return singleton;
	}
	
	public Site createSite(int id, SiteDescription description) 
	throws GlideinException
	{
		Site site = new Site(id,description.getName());
		
		// Set working directory
		GlideinConfiguration config = GlideinConfiguration.getInstance();
		File varDirectory = new File(config.getProperty("glidein.var"));
		File siteDirectory = new File(varDirectory,description.getName());
		site.setWorkingDirectory(siteDirectory);
		
		// Install path
		String installPath = description.getInstallPath();
		if(installPath == null || "".equals(installPath)) 
			throw new GlideinException("Invalid install path: "+installPath);
		site.setInstallPath(description.getInstallPath());
		
		// Local path
		String localPath = description.getLocalPath();
		if(localPath == null || "".equals(localPath)) 
			localPath = description.getInstallPath();
		site.setLocalPath(localPath);
		
		// Execution services
		ExecutionService stagingService = description.getStagingService();
		if(stagingService == null){
			throw new GlideinException("staging service was null");
		}
		site.setStagingService(stagingService);
		
		ExecutionService glideinService = description.getGlideinService();
		if(glideinService == null){
			throw new GlideinException("glidein service was null");
		}
		site.setGlideinService(glideinService);
		
		// Package
		site.setCondorPackage(description.getCondorPackage());
		
		// Environment
		EnvironmentVariable[] env = description.getEnvironment();
		if(env!=null)
		{
			for(EnvironmentVariable var : env)
				site.addEnvironment(var.getVariable(), var.getValue());
		}
		
		return site;
	}
}
