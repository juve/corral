package edu.usc.glidein.service.core;

import java.io.File;

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
	throws Exception
	{
		Site site = new Site(id,description.getName());
		
		// Install path
		String installPath = description.getInstallPath();
		if(installPath == null || "".equals(installPath)) 
			throw new Exception("Invalid install path: "+installPath);
		site.setInstallPath(description.getInstallPath());
		
		// Local path
		String localPath = description.getLocalPath();
		if(localPath == null || "".equals(localPath)) 
			localPath = description.getInstallPath();
		site.setLocalPath(localPath);
		
		// TODO Save configuration to a file
		String config = description.getConfiguration();
		File configuration = null;
		site.setConfiguration(configuration);
		
		// TODO Get proxy credential
		
		return site;
	}
}
