package edu.usc.glidein.service.state;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.service.impl.GlideinResource;
import edu.usc.glidein.service.impl.GlideinResourceHome;

public class GlideinEvent extends Event
{
	private Logger logger;
	
	public GlideinEvent(GlideinEventCode code, ResourceKey key)
	{
		super(code,key);
		this.logger = Logger.getLogger(GlideinEvent.class);
	}

	public void run()
	{
		try {
			GlideinResourceHome home = GlideinResourceHome.getInstance();
			GlideinResource resource = (GlideinResource) home.find(getKey());
			resource.handleEvent(this);
		} catch (NamingException ne) {
			logger.error("Unable to get GlideinResourceHome",ne);
		} catch (ResourceException re) {
			logger.error("Unable to find resource: "+getKey(),re);
		} catch (Throwable t) {
			logger.error("Unable to process event "+getCode()+" for glidein "+getKey().getValue(),t);
		}
	}
}
