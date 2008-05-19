package edu.usc.glidein.service.impl;

import java.net.URL;
import java.rmi.RemoteException;

import org.apache.axis.MessageContext;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.utils.AddressingUtils;

import edu.usc.glidein.stubs.types.Glidein;

public class GlideinFactoryService
{
	private Logger logger = Logger.getLogger(GlideinFactoryService.class);
	
	public GlideinFactoryService() { }	
	
	public EndpointReferenceType createGlidein(Glidein glidein)
	throws RemoteException
	{
		// Create a new resource
		ResourceKey key = null;
		try {
			ResourceContext rctx = ResourceContext.getResourceContext();
			GlideinResourceHome home = (GlideinResourceHome) rctx.getResourceHome();
			key = home.create(glidein);
		} catch (Exception e) {
			String message = "Unable to create glidein resource";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
		
		// Create an endpoint reference for the new resource
		EndpointReferenceType epr = null;
		try {
			URL baseURL = ServiceHost.getBaseURL();
			MessageContext mctx = MessageContext.getCurrentContext();
			String svc = (String) mctx.getService().getOption("instance");
			String instanceURI = baseURL.toString() + svc;
			epr = AddressingUtils.createEndpointReference(instanceURI, key);
		} catch (Exception e) {
			String message = "Unable to create endpoint reference";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
		
		// Return the endpoint reference to the client
		return epr;
	}
}