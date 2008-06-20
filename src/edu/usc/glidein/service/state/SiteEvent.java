package edu.usc.glidein.service.state;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.impl.SiteResource;
import edu.usc.glidein.service.impl.SiteResourceHome;

public class SiteEvent extends Event
{
	private Logger logger;
	
	public SiteEvent(EventCode code, ResourceKey key)
	{
		super(code,key);
		this.logger = Logger.getLogger(SiteEvent.class);
	}

	public void run()
	{
		try {
			SiteResourceHome home = SiteResourceHome.getInstance();
			SiteResource resource = (SiteResource)home.find(getKey());
			resource.handleEvent(this);
		} catch (NamingException ne) {
			logger.error("Unable to get SiteResourceHome",ne);
		} catch (ResourceException re) {
			logger.error("Unable to find SiteResource: "+getKey(),re);
		} catch (Throwable t) {
			logger.error("Unable to process event "+getCode()+" for resource "+getKey(),t);
		}
	}
}
