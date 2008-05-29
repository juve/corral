package edu.usc.glidein.service.impl;

import java.net.URL;
import java.rmi.RemoteException;

import org.apache.axis.MessageContext;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.utils.AddressingUtils;

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinStatus;
import edu.usc.glidein.stubs.types.Glideins;
import edu.usc.glidein.util.ProxyUtil;

public class GlideinFactoryService
{
	private Logger logger = Logger.getLogger(GlideinFactoryService.class);
	
	public GlideinFactoryService() { }	
	
	public EndpointReferenceType createGlidein(Glidein glidein)
	throws RemoteException
	{
		// TODO Init glidein
		glidein.setStatus(GlideinStatus.NEW);
		glidein.setStatusMessage("Created");
		
		// TODO Get site or fail
		
		/* TODO Move the following to instance service */
		// Setup cert
		GlobusCredential cred = ProxyUtil.getCallerCredential();
		
		// Check cert: This is just an initial check. Because we don't know
		// when the job will start we can not be certain that the cert won't
		// need to be renewed at some point.
		long timeLeft = cred.getTimeLeft()/60;
		if (timeLeft < glidein.getWallTime()) {
			throw new RemoteException("Not enough time left on proxy. " +
					"Need "+glidein.getWallTime()+", have "+timeLeft);
		}
		
		// TODO Save cert to file in site's temp dir
		
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
	
	public Glideins listGlideins(boolean longFormat)
	throws RemoteException
	{
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			Glidein[] glideins = dao.list(longFormat);
			return new Glideins(glideins);
		} catch(DatabaseException de) {
			throw new RemoteException("Unable to list sites",de);
		}
	}
}