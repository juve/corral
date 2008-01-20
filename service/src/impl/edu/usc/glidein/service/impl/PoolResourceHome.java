package edu.usc.glidein.service.impl;

import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;

import edu.usc.glidein.service.stubs.CreatePoolResourceRequest;

public class PoolResourceHome extends ResourceHomeImpl 
{
	public ResourceKey create(CreatePoolResourceRequest request) 
	throws Exception 
	{
		// Create a resource and initialize it
		PoolResource resource = (PoolResource) createNewInstance();
		resource.initialize(request);
		
		// Get key
		ResourceKey key = 
			new SimpleResourceKey(keyTypeName, resource.getID());
		
		// Add the resource to the list of resources in this home
		add(key, resource);
		
		return key;
	}
}
