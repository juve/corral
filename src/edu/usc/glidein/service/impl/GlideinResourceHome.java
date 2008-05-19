package edu.usc.glidein.service.impl;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;

import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinStatus;
import edu.usc.glidein.stubs.types.GlideinStatusCode;

public class GlideinResourceHome extends ResourceHomeImpl
{
	private Logger logger = Logger.getLogger(GlideinResourceHome.class);
	
	public synchronized void initialize() throws Exception
	{
		logger.info("Initializing glideins...");
		super.initialize();
		// TODO Recover glidein state
	}
	
	public ResourceKey create(Glidein glidein)
	throws ResourceException
	{
		GlideinResource resource = new GlideinResource(glidein, 
				new GlideinStatus(GlideinStatusCode.NEW,GlideinStatusCode.NEW.toString()));
		resource.create();
		ResourceKey key = createKey(glidein.getId());
		this.add(key, resource);
		return key;
	}
	
	public ResourceKey createKey(int id) {
		return new SimpleResourceKey(getKeyTypeName(), new Integer(id));
	}
}