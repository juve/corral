package edu.usc.glidein.service.impl;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceContext;

import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Site;

public class SiteService 
{
	private Logger logger = Logger.getLogger(SiteService.class);
	
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
	
	public Site getSite(EmptyObject empty) throws RemoteException
	{
		return getResource().getSite();
	}
	
	public EmptyObject submit(EmptyObject empty) throws RemoteException
	{
		getResource().submit();
		return new EmptyObject();
	}
	
	public EmptyObject remove(EmptyObject empty) throws RemoteException
	{
		getResource().remove();
		return new EmptyObject();
	}
}