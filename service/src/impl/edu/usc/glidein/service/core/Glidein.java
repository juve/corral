package edu.usc.glidein.service.core;

import java.io.File;

import edu.usc.glidein.service.types.GlideinDescription;

public class Glidein 
{
	private int id;
	private int count;
	private int hostCount;
	private File configuration;
	
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
		return count;
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
	
	public File getConfiguration()
	{
		return configuration;
	}
	
	public void setConfiguration(File configuration)
	{
		this.configuration = configuration;
	}
}
