package edu.usc.glidein.service.impl;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;

import edu.usc.glidein.stubs.types.Site;

public class SiteResourceHome extends ResourceHomeImpl
{
	private Logger logger = Logger.getLogger(SiteResourceHome.class);
	
	public synchronized void initialize() throws Exception
	{
		logger.info("Initializing sites...");
		super.initialize();
		// TODO Recover site state
	}
	
	public ResourceKey create(Site site)
	throws ResourceException
	{
		SiteResource resource = new SiteResource(site);
		resource.create();
		ResourceKey key = createKey(site.getId());
		this.add(key, resource);
		return key;
	}
	
	public ResourceKey createKey(int id) {
		return new SimpleResourceKey(getKeyTypeName(), new Integer(id));
	}
}