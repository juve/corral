package edu.usc.glidein.service.state;

import org.globus.wsrf.ResourceKey;

public class SiteStateChange implements StateChange
{
	private ResourceKey key;
	
	public SiteStateChange(ResourceKey key)
	{
		this.key = key;
	}

	public void run()
	{
		// TODO: Implement run()
		
	}

}
