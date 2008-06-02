package edu.usc.glidein.service.impl;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.UUID;

import org.apache.axis.MessageContext;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.utils.AddressingUtils;

import edu.usc.glidein.GlideinConfiguration;
import edu.usc.glidein.GlideinException;
import edu.usc.glidein.stubs.types.Sites;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;

public class SiteFactoryService
{
	private Logger logger = Logger.getLogger(SiteFactoryService.class);

	public SiteFactoryService() { }
	
	public EndpointReferenceType createSite(Site site)
	throws RemoteException
	{
		// Initialize site
		try {
			GlideinConfiguration config = GlideinConfiguration.getInstance();
			
			// Set status
			site.setStatus(SiteStatus.NEW);
			site.setStatusMessage("Created");
			
			// Set path
			File path = null;
			String var = config.getProperty("glidein.var","/tmp/glidein");
			do {
				UUID uid = UUID.randomUUID();
				String submitPath = var + File.separator + uid.toString();
				site.setSubmitPath(submitPath);
				path = new File(submitPath);
			} while (path.exists());
			
			// Check for Condor version and set reasonable default
			String ver = site.getCondorVersion();
			ver = "".equalsIgnoreCase(ver) ? null : ver;
			String pkg = site.getCondorPackage();
			pkg = "".equalsIgnoreCase(pkg) ? null : pkg;
			if (ver==null && pkg==null) {
				site.setCondorVersion("7.0.0");
			}
		} catch (GlideinException e) {
			String message = "Unable to initialize site";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
		
		// Create a new resource
		ResourceKey key = null;
		try {
			ResourceContext rctx = ResourceContext.getResourceContext();
			SiteResourceHome home = (SiteResourceHome) rctx.getResourceHome();
			key = home.create(site);
		} catch (Exception e) {
			String message = "Unable to create site resource";
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
	
	public Sites listSites(boolean longFormat)
	throws RemoteException
	{
		try {
			ResourceContext rctx = ResourceContext.getResourceContext();
			SiteResourceHome home = (SiteResourceHome) rctx.getResourceHome();
			Site[] sites = home.list(longFormat);
			return new Sites(sites);
		} catch (Exception e) {
			String message = "Unable to list sites";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
	}
}