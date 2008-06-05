package edu.usc.glidein.service;

import javax.naming.Context;
import javax.naming.InitialContext;

import edu.usc.glidein.GlideinException;

public class ServiceConfiguration
{
	private String install;
	private String uninstall;
	private String run;
	private String[] stagingURLs;
	private String glideinCondorConfig;
	private String tempDir;
	
	public ServiceConfiguration() { }
	
	public static ServiceConfiguration getInstance()
	throws GlideinException
	{
		String location = "java:comp/env/glidein/ServiceConfiguration";
		try {
			Context initialContext = new InitialContext();
	    	return (ServiceConfiguration)initialContext.lookup(location);
		} catch (Exception e) {
			throw new GlideinException("Unable to load configuration: "+location,e);
		}
	}

	public String getInstall()
	{
		return install;
	}

	public void setInstall(String install)
	{
		this.install = install;
	}

	public String getUninstall()
	{
		return uninstall;
	}

	public void setUninstall(String uninstall)
	{
		this.uninstall = uninstall;
	}

	public String getRun()
	{
		return run;
	}

	public void setRun(String run)
	{
		this.run = run;
	}

	public String[] getStagingURLs()
	{
		return stagingURLs;
	}

	public void setStagingURLs(String stagingURLs)
	{
		this.stagingURLs = stagingURLs.split("[ ,;\t\n]+");
	}

	public String getGlideinCondorConfig()
	{
		return glideinCondorConfig;
	}

	public void setGlideinCondorConfig(String glideinCondorConfig)
	{
		this.glideinCondorConfig = glideinCondorConfig;
	}

	public String getTempDir()
	{
		return tempDir;
	}

	public void setTempDir(String tempDir)
	{
		this.tempDir = tempDir;
	}
}
