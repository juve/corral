package edu.usc.glidein.service.state;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.impl.SiteResource;
import edu.usc.glidein.service.impl.SiteResourceHome;
import edu.usc.glidein.stubs.types.SiteState;

public class SiteStateChange implements StateChange
{
	private Logger logger;
	private ResourceKey key;
	private SiteState newState;
	
	public SiteStateChange(ResourceKey key, SiteState newState)
	{
		this.logger = Logger.getLogger(SiteStateChange.class);
		this.key = key;
		this.newState = newState;
	}

	public void run()
	{
		try {
			SiteResourceHome home = SiteResourceHome.getInstance();
			SiteResource resource = (SiteResource)home.find(key);
			resource.processStateChange(newState);
		} catch (NamingException ne) {
			logger.error("Unable to get SiteResourceHome",ne);
		} catch (ResourceException re) {
			logger.error("Unable to find SiteResource: "+key,re);
		}
	}

}
