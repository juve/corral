package edu.usc.glidein.service.impl;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceKey;

import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;

public class SiteService 
{
	private Logger logger = Logger.getLogger(SiteService.class);
	
	private SiteResourceHome getResourceHome() throws RemoteException
	{
		try {
			return (SiteResourceHome)ResourceContext.getResourceContext().getResourceHome();
		} catch (Exception e) {
			String message = "Unable to find site resource home";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}	
	}
	
	private SiteResource getResource() throws RemoteException
	{
		try {
			return (SiteResource) ResourceContext.getResourceContext().getResource();
		} catch (Exception e) {
			String message = "Unable to find site resource";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}	
	}
	
	private ResourceKey getResourceKey() throws RemoteException
	{
		try {
			return (ResourceKey) ResourceContext.getResourceContext().getResourceKey();
		} catch (Exception e) {
			String message = "Unable to find site resource key";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}	
	}
	
	public SiteStatus getStatus(EmptyObject empty) throws RemoteException {
		return getResource().getStatus();
	}
	
	public Site getSite(EmptyObject empty) throws RemoteException {
		return getResource().getSite();
	}
	
	public EmptyObject delete(EmptyObject empty) throws RemoteException {
		getResource().delete();
		getResourceHome().remove(getResourceKey());
		return new EmptyObject();
	}
}