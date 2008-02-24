package edu.usc.glidein.service.core;

import edu.usc.glidein.service.types.GlideinDescription;

public class Glidein 
{
	private int id;
	
	/**
	 * Total number of processes. Defaults to one per host.
	 */
	private int count;
	
	/**
	 * Number of hosts to request
	 */
	private int hostCount;
	
	/**
	 * Number of CPUs to use for glidein
	 */
	private int numCpus;
	
	/**
	 * Requested runtime in minutes
	 */
	private int wallTime;
	
	/**
	 * Maximum glidein idle time in minutes
	 */
	private int idleTime;
	
	/**
	 * Glidein condor_config
	 */
	private String configuration;
	
	/**
	 * IP address of GCB broker
	 */
	private String gcbBroker;
	
	/**
	 * Comma-separated list of debug settings (e.g. D_FULLDEBUG,D_PID)
	 */
	private String debug;
	
	
	public Glidein(int id)
	{
		this.id = id;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public int getCount()
	{
		// The default is 1 process per host
		if (count==0) return hostCount;
		else return count;
	}

	public void setCount(int count)
	{
		this.count = count;
	}

	public int getHostCount()
	{
		return hostCount;
	}

	public void setHostCount(int hostCount)
	{
		this.hostCount = hostCount;
	}
	
	public GlideinDescription createDescription()
	{
		GlideinDescription description = new GlideinDescription();
		description.setId(getId());
		description.setCount(getCount());
		description.setHostCount(getHostCount());
		return description;
	}
	
	public String getConfiguration()
	{
		return configuration;
	}
	
	public void setConfiguration(String configuration)
	{
		this.configuration = configuration;
	}
	
	public int getWallTime() 
	{
		return wallTime;
	}
	
	public void setWallTime(int wallTime)
	{
		this.wallTime = wallTime;
	}
	
	public int getIdleTime()
	{
		return idleTime;
	}
	
	public void setIdleTime(int idleTime)
	{
		this.idleTime = idleTime;
	}
	
	public String getGcbBroker()
	{
		return gcbBroker;
	}
	
	public void setGcbBroker(String gcbBroker)
	{
		this.gcbBroker = gcbBroker;
	}
	
	public String getDebug()
	{
		return debug;
	}
	
	public void setDebug(String debug)
	{
		this.debug = debug;
	}
	
	public int getNumCpus()
	{
		return numCpus;
	}
	
	public void setNumCpus(int numCpus)
	{
		this.numCpus = numCpus;
	}
}
