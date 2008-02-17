package edu.usc.glidein.service.core;

import java.util.HashMap;
import java.util.Map;

import edu.usc.glidein.service.types.PoolDescription;

public class Pool
{	
	public static final int DEFAULT_CONDOR_PORT = 9816;
	
	/** Resource properties */
	private int id;
	private String condorHost;
	private int condorPort;
	private String condorVersion;

	private int nextSiteId = 0;
	private Map<String,Site> sites;
	
	public Pool(int id)
	{
		this.id = id;
		this.sites = new HashMap<String,Site>();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCondorHost() {
		return condorHost;
	}
	
	public void setCondorHost(String condorHost) {
		this.condorHost = condorHost;
	}

	public int getCondorPort() {
		return condorPort;
	}

	public void setCondorPort(int condorPort) {
		this.condorPort = condorPort;
	}

	public String getCondorVersion() {
		return condorVersion;
	}

	public void setCondorVersion(String condorVersion) {
		this.condorVersion = condorVersion;
	}
	
	public void addSite(Site site) {
		this.sites.put(site.getName(), site);
	}
	
	public Site getSite(String name) {
		return this.sites.get(name);
	}
	
	public Site removeSite(String name) {
		return this.sites.remove(name);
	}
	
	public Site[] getSites() {
		return (Site[])this.sites.values().toArray(new Site[0]);
	}
	
	public synchronized int createSiteId()
	{
		int newId = nextSiteId;
		nextSiteId++;
		return newId;
	}
	
	public PoolDescription createDescription()
	{
		PoolDescription description = new PoolDescription();
		description.setCondorHost(getCondorHost());
		description.setCondorPort(getCondorPort());
		description.setCondorVersion(getCondorVersion());
		return description;
	}
}
