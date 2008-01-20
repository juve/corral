package edu.usc.glidein.service.core;

import edu.usc.glidein.service.types.GlideinDescription;

public class GlideinFactory 
{
	private static GlideinFactory singleton;
	
	private GlideinFactory() { }
	
	public synchronized static GlideinFactory getInstance()
	{
		if(singleton == null){
			singleton = new GlideinFactory();
		}
		return singleton;
	}
	
	public Glidein createGlidein(int id, GlideinDescription description) 
	throws Exception
	{
		Glidein glidein = new Glidein(id);
		
		// Count
		int count = description.getCount();
		glidein.setCount(count);
		
		// HostCount
		int hostCount = description.getHostCount();
		glidein.setHostCount(hostCount);
		
		return glidein;
	}
}
