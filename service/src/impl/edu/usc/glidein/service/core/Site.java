package edu.usc.glidein.service.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.usc.glidein.service.core.Glidein;
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
	
	public Site(int id, String name)
	{
		this.id = id;
		this.name = name;
		status = SiteStatus.NEW;
		glideins = new HashMap<Integer,Glidein>();
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public synchronized void addGlidein(Glidein glidein) {
		this.glideins.put(glidein.getId(), glidein);
	}
	
	public synchronized Glidein getGlidein(int id) {
		return this.glideins.get(id);
	}
	
	public synchronized Glidein removeGlidein(int id) {
		return this.glideins.remove(id);
	}
	
	public synchronized int createGlideinId() {
		int newId = nextGlideinId;
		nextGlideinId++;
		return newId;
	}
	
	public SiteDescription createDescription() {
		SiteDescription description = new SiteDescription();
		description.setId(getId());
		description.setName(getName());
		return description;
	}

	public Glidein[] getGlideins() {
		return (Glidein[])glideins.values().toArray(new Glidein[0]);
	}

	public SiteStatus getStatus() {
		return status;
	}

	public void setStatus(SiteStatus status) {
		this.status = status;
	}

	public File getConfiguration() {
		return configuration;
	}

	public void setConfiguration(File configuration) {
		this.configuration = configuration;
	}

	public String getInstallPath() {
		return installPath;
	}

	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
}
