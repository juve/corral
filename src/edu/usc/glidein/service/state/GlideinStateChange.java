package edu.usc.glidein.service.state;

import org.globus.wsrf.ResourceKey;

public class GlideinStateChange implements StateChange
{
	private ResourceKey key;
	
	public GlideinStateChange(ResourceKey key)
	{
		this.key = key;
	}

	public void run()
	{
		// TODO: Implement run()

	}
}
