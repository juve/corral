package edu.usc.glidein.service.core;

import edu.usc.glidein.common.util.Base64;
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
		glidein.setCount(description.getCount());
		glidein.setHostCount(description.getHostCount());
		glidein.setNumCpus(description.getNumCpus());
		glidein.setWallTime(description.getWallTime());
		glidein.setIdleTime(description.getIdleTime());
		glidein.setGcbBroker(description.getGcbBroker());
		glidein.setConfiguration(
				Base64.fromBase64(description.getConfigBase64()));
		glidein.setDebug(description.getDebug());
		
		return glidein;
	}
}
