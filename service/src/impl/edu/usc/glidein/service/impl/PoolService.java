package edu.usc.glidein.service.impl;

import java.rmi.RemoteException;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.wsrf.ResourceContext;

import edu.usc.glidein.service.core.Glidein;
import edu.usc.glidein.service.core.GlideinFactory;
import edu.usc.glidein.service.core.Pool;
import edu.usc.glidein.service.core.Site;
import edu.usc.glidein.service.core.SiteFactory;
import edu.usc.glidein.service.stubs.CreateGlideinRequest;
import edu.usc.glidein.service.stubs.CreateGlideinResponse;
import edu.usc.glidein.service.stubs.CreateSiteRequest;
import edu.usc.glidein.service.stubs.CreateSiteResponse;
import edu.usc.glidein.service.stubs.DestroyGlideinRequest;
import edu.usc.glidein.service.stubs.DestroyGlideinResponse;
import edu.usc.glidein.service.stubs.DestroySiteRequest;
import edu.usc.glidein.service.stubs.DestroySiteResponse;
import edu.usc.glidein.service.stubs.QueryGlideinRequest;
import edu.usc.glidein.service.stubs.QueryGlideinResponse;
import edu.usc.glidein.service.stubs.QueryPoolRequest;
import edu.usc.glidein.service.stubs.QueryPoolResponse;
import edu.usc.glidein.service.stubs.QuerySiteRequest;
import edu.usc.glidein.service.stubs.QuerySiteResponse;
import edu.usc.glidein.service.types.GlideinDescription;
import edu.usc.glidein.service.types.PoolDescription;
import edu.usc.glidein.service.types.SiteDescription;

/**
 * The pool service manages the creation of sites and glideins for a single
 * grid site.
 * 
 * @author Gideon Juve <juve@usc.edu>
 */
public class PoolService 
{
	private Log logger = LogFactory.getLog(getClass());
	
	/**
	 * Look up the resource associated with the user's request.
	 * @return The user's pool resource
	 * @throws RemoteException if the resource is not found
	 */
	private PoolResource getResource() throws RemoteException
	{
		Object resource = null;
		try {
			resource = ResourceContext.getResourceContext().getResource();
		} catch (Exception e) {
			String message = "Unable to find pool resource";
			logger.error(message,e);
			throw new RemoteException(message, e);
		}
		PoolResource poolResource = (PoolResource) resource;
		return poolResource;
	}
	
	/**
	 * Create a new site for this pool.
	 * @param request Containing a description of the site
	 * @return A response indicating the site ID
	 * @throws RemoteException If the site cannot be created
	 */
	public CreateSiteResponse createSite(CreateSiteRequest request) 
	throws RemoteException
	{
		PoolResource resource = getResource();
		Pool pool = resource.getPool();
		SiteDescription siteDescription = request.getSiteDescription();
		
		// Create site
		int siteId = pool.createSiteId();
		SiteFactory factory = SiteFactory.getInstance();
		try {
			Site site = factory.createSite(siteId, siteDescription);
			pool.addSite(site);
		} catch(Exception e){
			String message = "Unable to create site: "+
							 siteDescription.getName();
			logger.error(message,e);
			throw new RemoteException(message,e);
		}
		
		// TODO Schedule site for staging
		
		CreateSiteResponse response = new CreateSiteResponse();
		response.setSiteId(siteId);
		return response;
	}
	
	/**
	 * Destroy a site
	 * @param request A request identifying the site to destroy
	 * @return An empty response
	 * @throws RemoteException if the site cannot be found
	 */
	public DestroySiteResponse destroySite(DestroySiteRequest request)
	throws RemoteException
	{
		PoolResource resource = getResource();
		Pool pool = resource.getPool();
		
		// Find site
		String siteName = request.getSiteName();
		Site site = pool.getSite(siteName);
		if(site == null){
			String message = "Unknown site: "+siteName+
			 				 " in pool "+pool.getId();
							 logger.error(message);
			throw new RemoteException(message);
		}
		
		// TODO Schedule site for destruction
		pool.removeSite(siteName);
		
		return new DestroySiteResponse();
	}
	
	/**
	 * Create a new glidein job for this pool and a specific site
	 * @param request A request specifying the site where the glidein 
	 * should be submitted and the characteristics of the glidein.
	 * @return A response indicating the glidein id
	 * @throws RemoteException if the site cannot be found or the glidein 
	 * cannot be created.
	 */
	public CreateGlideinResponse createGlidein(CreateGlideinRequest request) 
	throws RemoteException 
	{
		PoolResource resource = getResource();
		Pool pool = resource.getPool();
		GlideinDescription description = request.getGlideinDescription();
		
		// Find site
		String siteName = description.getSiteName();
		Site site = pool.getSite(siteName);
		if(site == null){
			String message = "Unknown site: "+siteName+
							 " in pool "+pool.getId();
			logger.error(message);
			throw new RemoteException(message);
		}
		
		// Create glidein
		int glideinId = site.createGlideinId();
		try {
			GlideinFactory factory = GlideinFactory.getInstance();
			Glidein glidein = factory.createGlidein(glideinId, description);
			site.addGlidein(glidein);
		} catch(Exception e){
			String message = "Unable to create glidein for site "+
							 site.getName() +" ("+site.getId()+")"+
							 " in pool "+pool.getId();
			logger.error(message,e);
			throw new RemoteException(message,e);
		}
		
		// TODO Schedule glidein job
		
		CreateGlideinResponse response = new CreateGlideinResponse();
		response.setGlideinId(glideinId);
		return response;
	}
	
	/**
	 * Destroy a glidein for this given pool / site.
	 * @param request A request specifying the glidein to destroy.
	 * @return An empty response.
	 * @throws RemoteException If the glidein cannot be located or destroyed
	 */
	public DestroyGlideinResponse destroyGlidein(DestroyGlideinRequest request)
	throws RemoteException
	{
		PoolResource resource = getResource();
		Pool pool = resource.getPool();
		
		// Find site
		String siteName = request.getSiteName();
		Site site = pool.getSite(siteName);
		if(site == null){
			String message = "Unknown site: "+siteName+
			 				 " in pool "+pool.getId();
			logger.error(message);
			throw new RemoteException(message);
		}
		
		// Find glidein
		int glideinId = request.getGlideinId();
		Glidein glidein = site.getGlidein(glideinId);
		if(glidein == null){
			String message = "Unknown glidein: "+glideinId+
							 " for site "+ siteName +
							 " in pool " + pool.getId();
			logger.error(message);
			throw new RemoteException(message);
		}
		
		// TODO Schedule the glidein for destruction
		site.removeGlidein(glideinId);
		
		return new DestroyGlideinResponse();
	}
	
	/**
	 * Get a description of the pool
	 * @param request An empty request object
	 * @return A response containing a description of the pool
	 * @throws RemoteException If the pool can't be located
	 */
	public QueryPoolResponse queryPool(QueryPoolRequest request) 
	throws RemoteException
	{
		PoolResource resource = getResource();
		Pool pool = resource.getPool();
		PoolDescription description = pool.createDescription();
		return new QueryPoolResponse(description);
	}
	
	/**
	 * Get a list of descriptors for all the sites in this pool
	 * @param request An empty request object
	 * @return A response containing all the site descriptions
	 * @throws RemoteException If the pool cannot be located
	 */
	public QuerySiteResponse querySite(QuerySiteRequest request) 
	throws RemoteException
	{
		PoolResource resource = getResource();
		Pool pool = resource.getPool();
		Site[] sites = pool.getSites();
		LinkedList<SiteDescription> list = 
			new LinkedList<SiteDescription>();
		for(Site site : sites){
			list.add(site.createDescription());
		}
		SiteDescription[] descriptions = (SiteDescription[])
			list.toArray(new SiteDescription[0]);
		return new QuerySiteResponse(descriptions);
	}
	
	/**
	 * Get a list of descriptors for all the glideins in this pool
	 * @param request An empty request object
	 * @return A response containing a list of all the glidein descriptions
	 * @throws RemoteException If the pool cannot be located
	 */
	public QueryGlideinResponse queryGlidein(QueryGlideinRequest request) 
	throws RemoteException
	{
		PoolResource resource = getResource();
		Pool pool = resource.getPool();
		Site[] sites = pool.getSites();
		LinkedList<GlideinDescription> list = 
			new LinkedList<GlideinDescription>();
		for(Site site : sites){
			Glidein[] glideins = site.getGlideins();
			for(Glidein glidein : glideins){
				GlideinDescription description = glidein.createDescription();
				description.setSiteName(site.getName());
				list.add(description);
			}
		}
		GlideinDescription[] descriptions = (GlideinDescription[])
			list.toArray(new GlideinDescription[0]);
		return new QueryGlideinResponse(descriptions);
	}
}
