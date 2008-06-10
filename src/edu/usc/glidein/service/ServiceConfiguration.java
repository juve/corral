package edu.usc.glidein.service;

import java.io.File;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ServiceConfiguration
{
	private File install;
	private File uninstall;
	private File run;
	private String[] stagingURLs;
	private File glideinCondorConfig;
	private File tempDir;
	
	public ServiceConfiguration()
	{
		this.tempDir = new File("/tmp");
	}
	
	public static ServiceConfiguration getInstance() throws NamingException
	{
		String location = "java:comp/env/glidein/ServiceConfiguration";
		Context initialContext = new InitialContext();
	    return (ServiceConfiguration)initialContext.lookup(location);
	}

	public String getInstall()
	{
		return install.getAbsolutePath();
	}

	public void setInstall(String install)
	{
		this.install = new File(install);
	}

	public String getUninstall()
	{
		return uninstall.getAbsolutePath();
	}

	public void setUninstall(String uninstall)
	{
		this.uninstall = new File(uninstall);
	}

	public String getRun()
	{
		return run.getAbsolutePath();
	}

	public void setRun(String run)
	{
		this.run = new File(run);
	}

	public String[] getStagingURLs()
	{
		return stagingURLs;
	}
	
	public void setStagingURLs(String[] stagingURLs)
	{
		this.stagingURLs = stagingURLs;
	}
	
	public String getGlideinServerURLs()
	{
		if (stagingURLs==null) {
			return null;
		}
		StringBuffer buff = new StringBuffer();
		for (int i=0; i<stagingURLs.length; i++) {
			buff.append(stagingURLs[i]);
			if (i<stagingURLs.length-1) buff.append(",");
		}
		return buff.toString();
	}

	public void setGlideinServerURLs(String glideinServerURLs)
	{
		this.stagingURLs = glideinServerURLs.split("[ ,;\t\n]+");
	}

	public String getGlideinCondorConfig()
	{
		return glideinCondorConfig.getAbsolutePath();
	}

	public void setGlideinCondorConfig(String glideinCondorConfig)
	{
		this.glideinCondorConfig = new File(glideinCondorConfig);
	}

	public String getTempDir()
	{
		return tempDir.getAbsolutePath();
	}

	public void setTempDir(String tempDir)
	{
		this.tempDir = new File(tempDir);
	}
}
