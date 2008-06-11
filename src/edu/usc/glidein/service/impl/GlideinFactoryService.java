package edu.usc.glidein.service.impl;

import java.net.URL;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.apache.axis.MessageContext;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.utils.AddressingUtils;

import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.Glideins;
import edu.usc.glidein.util.AddressingUtil;

public class GlideinFactoryService
{
	public EndpointReferenceType createGlidein(Glidein glidein)
	throws RemoteException
	{
		// Get site or fail
		try {
			SiteResourceHome siteHome = SiteResourceHome.getInstance();
			siteHome.find(AddressingUtil.getSiteKey(glidein.getSiteId()));
		} catch(NamingException ne) {
			throw new ResourceException("Unable to locate site",ne);
		}
		
		// Get resource home
		GlideinResourceHome home = null;
		try {
			home = GlideinResourceHome.getInstance();
		} catch (NamingException e) {
			throw new ResourceException("Unable to get GlideinResourceHome", e);
		}
		
		// Create glidein
		ResourceKey key = home.create(glidein);
		
		// Create an endpoint reference for the new resource
		EndpointReferenceType epr = null;
		try {
			URL baseURL = ServiceHost.getBaseURL();
			MessageContext mctx = MessageContext.getCurrentContext();
			String svc = (String) mctx.getService().getOption("instance");
			String instanceURI = baseURL.toString() + svc;
			epr = AddressingUtils.createEndpointReference(instanceURI, key);
		} catch (Exception e) {
			throw new RemoteException("Unable to create endpoint reference", e);
		}
		
		// Return the endpoint reference to the client
		return epr;
	}
	
	public Glideins listGlideins(boolean longFormat)
	throws RemoteException
	{
		try {
			ResourceContext rctx = ResourceContext.getResourceContext();
			GlideinResourceHome home = (GlideinResourceHome) rctx.getResourceHome();
			Glidein[] glideins = home.list(longFormat);
			return new Glideins(glideins);
		} catch (Exception e) {
			throw new ResourceException("Unable to list sites", e);
		}
	}
}