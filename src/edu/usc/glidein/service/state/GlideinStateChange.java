package edu.usc.glidein.service.state;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.impl.GlideinResource;
import edu.usc.glidein.service.impl.GlideinResourceHome;
import edu.usc.glidein.stubs.types.GlideinState;

public class GlideinStateChange implements StateChange
{
	private Logger logger;
	private ResourceKey key;
	private GlideinState newState;
	
	public GlideinStateChange(ResourceKey key, GlideinState newState)
	{
		this.logger = Logger.getLogger(GlideinStateChange.class);
		this.key = key;
		this.newState = newState;
	}

	public void run()
	{
		try {
			GlideinResourceHome home = GlideinResourceHome.getInstance();
			GlideinResource resource = (GlideinResource) home.find(key);
			resource.processStateChange(newState);
		} catch (NamingException ne) {
			logger.error("Unable to get GlideinResourceHome",ne);
		} catch (ResourceException re) {
			logger.error("Unable to find resource: "+key,re);
		}
	}
}
