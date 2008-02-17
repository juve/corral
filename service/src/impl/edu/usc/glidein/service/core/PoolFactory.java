package edu.usc.glidein.service.core;

import java.net.InetAddress;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.types.PoolDescription;

/**
 * This singleton factory class creates instances of the Pool class.
 * 
 * @author Gideon Juve <juve@usc.edu>
 */
public class PoolFactory 
{
	private static PoolFactory singleton;
	
	private int GUID = 0;
	
	private PoolFactory() { }
	
	public synchronized static PoolFactory getInstance()
	{
		if(singleton == null){
			singleton = new PoolFactory();
		}
		return singleton;
	}
	
	public Pool createPool(PoolDescription description) 
	throws GlideinException
	{
		Pool pool = new Pool(GUID++);
		
		// Host
		String host = description.getCondorHost();
		if(host==null || "".equals(host)){
			throw new GlideinException("Invalid Condor host: "+host);
		}
		else {
			try {
				InetAddress.getByName(host);
			} catch(Exception e){
				throw new GlideinException("Invalid Condor host: "+host);
			}
			pool.setCondorHost(host);
		}
		
		// Port
		int port = description.getCondorPort();
		if(port==0){ 
			port = Pool.DEFAULT_CONDOR_PORT;
		}
		pool.setCondorPort(port);
		
		// Version
		String version = description.getCondorVersion();
		if(version==null || "".equals(version)){
			throw new GlideinException("Invalid Condor version: "+version);
		}
		pool.setCondorVersion(version);
		
		return pool;
	}
}
