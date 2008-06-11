package edu.usc.glidein.service.impl;

import java.rmi.RemoteException;

import org.globus.wsrf.ResourceContext;

import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;

public class GlideinService
{
	private GlideinResource getResource() throws RemoteException
	{
		try {
			Object resource = ResourceContext.getResourceContext().getResource();
			GlideinResource glideinResource = (GlideinResource) resource;
			return glideinResource;
		} catch (Exception e) {
			throw new RemoteException("Unable to find glidein resource", e);
		}
	}
	
	public Glidein getGlidein(EmptyObject empty) throws RemoteException
	{
		return getResource().getGlidein();
	}
	
	public EmptyObject submit(EmptyObject empty) throws RemoteException
	{
		getResource().submit();
		return new EmptyObject();
	}
	
	public EmptyObject remove(boolean force) throws RemoteException
	{
		getResource().remove(force);
		return new EmptyObject();
	}
}
