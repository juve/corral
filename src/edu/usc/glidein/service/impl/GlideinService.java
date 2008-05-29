package edu.usc.glidein.service.impl;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;

public class GlideinService
{
	private Logger logger = Logger.getLogger(GlideinService.class);
	
	private GlideinResourceHome getResourceHome() throws RemoteException
	{
		try {
			Object resourceHome = ResourceContext.getResourceContext().getResourceHome();
			GlideinResourceHome glideinResourceHome = (GlideinResourceHome) resourceHome;
			return glideinResourceHome;
		} catch (Exception e) {
			String message = "Unable to find glidein resource home";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
	}
	
	private GlideinResource getResource() throws RemoteException
	{
		try {
			Object resource = ResourceContext.getResourceContext().getResource();
			GlideinResource glideinResource = (GlideinResource) resource;
			return glideinResource;
		} catch (Exception e) {
			String message = "Unable to find glidein resource";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
	}
	
	private ResourceKey getResourceKey() throws RemoteException 
	{
		try {
			return ResourceContext.getResourceContext().getResourceKey();
		} catch (Exception e) {
			String message = "Unable to find glidein resource key";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
	}
	
	public Glidein getGlidein(EmptyObject empty) throws RemoteException {
		return getResource().getGlidein();
	}
	
	public EmptyObject start(EmptyObject empty) throws RemoteException {
		// TODO submit glidein job
		return new EmptyObject();
	}
	
	public EmptyObject cancel(EmptyObject empty) throws RemoteException {
		// TODO cancel glidein job
		return new EmptyObject();
	}
	
	public EmptyObject delete(EmptyObject empty) throws RemoteException {
		getResource().delete();
		getResourceHome().remove(getResourceKey());
		return new EmptyObject();
	}
}
