package edu.usc.glidein.service.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.usc.glidein.service.core.Glidein;
import edu.usc.glidein.service.types.ExecutionService;
import edu.usc.glidein.service.types.SiteDescription;

public class Site
{
	/** Site ID */
	private int id;
	
	/** The name of the site */
	private String name;
	
	/** The current status of the site */
	private SiteStatus status;
	
	/** The glidein configuration file to use */
	private File configuration;
	
	/** The path where executables should be installed on the remote site */
	private String installPath;
	
	/** The path where log files should be written on the remote site */
	private String localPath;

	/** The next glidein id for this site */
	private int nextGlideinId = 0;
	
	/** A map of glidein jobs */
	private Map<Integer,Glidein> glideins;
	
	/** Handler for the site */
	private SiteHandler handler = null;
	
	/** Service for staging executables */
	private ExecutionService stagingService;
	
	/** Service for running glideins */
	private ExecutionService glideinService;
	
	/** Condor package name: eg. 7.0.0-x86-MacOSX-10.4.tar.gz */
	private String condorPackage;
	
	/** Environment */
	private Map<String,String> environment;
	
	/** Site working directory */
	private File workingDirectory;
	
	public Site(int id, String name)
	{
		this.id = id;
		this.name = name;
		this.status = new SiteStatus(SiteStatusCode.NEW,"New");
		this.glideins = new HashMap<Integer,Glidein>();
		this.environment = new HashMap<String,String>();
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
	
	public synchronized void addGlidein(Glidein glidein)
	{
		this.glideins.put(glidein.getId(), glidein);
	}
	
	public synchronized Glidein getGlidein(int id)
	{
		return this.glideins.get(id);
	}
	
	public synchronized Glidein removeGlidein(int id)
	{
		return this.glideins.remove(id);
	}
	
	public synchronized int createGlideinId()
	{
		int newId = nextGlideinId;
		nextGlideinId++;
		return newId;
	}
	
	public SiteDescription createDescription()
	{
		SiteDescription description = new SiteDescription();
		description.setId(getId());
		description.setName(getName());
		description.setInstallPath(getInstallPath());
		description.setLocalPath(getLocalPath());
		description.setGlideinService(getGlideinService());
		description.setStagingService(getStagingService());
		// TODO get status
		return description;
	}

	public Glidein[] getGlideins()
	{
		return (Glidein[])glideins.values().toArray(new Glidein[0]);
	}

	public SiteStatus getStatus()
	{
		return status;
	}

	public void setStatus(SiteStatus status) 
	{
		this.status = status;
	}

	public File getConfiguration()
	{
		return configuration;
	}

	public void setConfiguration(File configuration)
	{
		this.configuration = configuration;
	}

	public String getInstallPath()
	{
		return installPath;
	}

	public void setInstallPath(String installPath)
	{
		this.installPath = installPath;
	}

	public String getLocalPath()
	{
		return localPath;
	}

	public void setLocalPath(String localPath)
	{
		this.localPath = localPath;
	}
	
	public SiteHandler getHandler()
	{
		return handler;
	}
	
	public void setHandler(SiteHandler handler)
	{
		this.handler = handler;
	}
	
	public void setGlideinService(ExecutionService glideinService)
	{
		this.glideinService = glideinService;
	}
	
	public ExecutionService getGlideinService()
	{
		return glideinService;
	}
	
	public void setStagingService(ExecutionService stagingService)
	{
		this.stagingService = stagingService;
	}
	
	public ExecutionService getStagingService()
	{
		return stagingService;
	}

	public String getCondorPackage()
	{
		return condorPackage;
	}

	public void setCondorPackage(String condorPackage)
	{
		this.condorPackage = condorPackage;
	}
	
	public Map<String,String> getEnvironment()
	{
		return this.environment;
	}
	
	public void addEnvironment(String key, String value)
	{
		this.environment.put(key, value);
	}
	
	public File getWorkingDirectory()
	{
		return workingDirectory;
	}
	
	public void setWorkingDirectory(File workingDirectory)
	{
		this.workingDirectory = workingDirectory;
	}
}
