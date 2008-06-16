package edu.usc.glidein.service.impl;

import java.rmi.RemoteException;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.wsrf.ResourceContext;

import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Site;

public class SiteService 
{
	private SiteResource getResource() throws RemoteException
	{
		try {
			return (SiteResource) ResourceContext.getResourceContext().getResource();
		} catch (Exception e) {
			throw new RemoteException("Unable to find site resource", e);
		}	
	}
	
	public Site getSite(EmptyObject empty) throws RemoteException
	{
		return getResource().getSite();
	}
	
	public EmptyObject submit(EndpointReferenceType credential) throws RemoteException
	{
		getResource().submit(credential);
		return new EmptyObject();
	}
	
	public EmptyObject remove(boolean force) throws RemoteException
	{
		getResource().remove(force);
		return new EmptyObject();
	}
}