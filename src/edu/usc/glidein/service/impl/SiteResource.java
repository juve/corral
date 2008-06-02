package edu.usc.glidein.service.impl;

import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.PersistentResource;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.impl.ReflectionResourceProperty;
import org.globus.wsrf.impl.SimpleResourcePropertySet;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.StageSiteOperation;
import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;
import edu.usc.glidein.util.ProxyUtil;

public class SiteResource implements PersistentResource, ResourceProperties
{
	private Logger logger = Logger.getLogger(SiteResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Site site;
	
	/**
	 * Default constructor required
	 */
	public SiteResource() {
		
	}
	
	public SiteResource(Site site) throws ResourceException
	{
		this.site = site;
		setResourceProperties();
	}
	
	public Site getSite()
	{
		return site;
	}
	
	/** 
	 * @see org.globus.wsrf.ResourceIdentifier 
	 */
	public Object getID()
	{
		return new Integer(site.getId());
	}
	
	/**
	 * @see org.globus.wsrf.ResourceProperties
	 */
	public ResourcePropertySet getResourcePropertySet()
	{
		return resourceProperties;
	}
	
	private void setResourceProperties() throws ResourceException
	{
		try {
			resourceProperties = new SimpleResourcePropertySet(SiteNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_SITE_ID,"Id",site));
			// TODO: Set the rest of the resource properties, or don't, they seem kinda useless to me
		} catch(Exception e) {
			throw new ResourceException("Unable to set site resource properties",e);
		}
	}
	
	/**
	 * Create the resource
	 */
	public synchronized void create() throws ResourceException
	{
		logger.debug("Creating site resource "+getSite().getId());
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.create(site);
		} catch (DatabaseException de) {
			throw new ResourceException(de);
		}
	}

	/**
	 * Load the resource
	 * @see org.globus.wsrf.PersistenceCallback
	 */
	public synchronized void load(ResourceKey key) throws ResourceException
	{
		logger.debug("Loading site resource "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			site = dao.load(id);
			setResourceProperties();
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	/**
	 * Store the resource
	 * @see org.globus.wsrf.PersistenceCallback
	 */
	public synchronized void store() throws ResourceException
	{
		logger.debug("Storing site resource "+getSite().getId());
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.store(site);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}

	/**
	 * Remove the resource
	 * 
	 * NOTE: Don't get confused, this method is called when the ResourceHome
	 * decides to remove this resource from its cache, not when the client
	 * invokes the remove operation. The delete method is called when the
	 * client invokes the remove operation.
	 * 
	 * TODO: Need to work out the issues around remove. The home should not
	 * be able to remove anything that is running, or else the service will
	 * get confused because the service will not see the updates caused
	 * by the thread watching the job.
	 * 
	 * @see org.globus.wsrf.RemoveCallback
	 */
	public synchronized void remove() throws ResourceException
	{
		logger.debug("Removing site resource "+getSite().getId());
	}
	
	/**
	 * Delete the resource from the database
	 * @throws ResourceException
	 */
	public synchronized void delete() throws ResourceException
	{
		// TODO: Check status and cancel staging job if necessary
		
		// Remove the site from the database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
			site = null;
			resourceProperties = null;
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	/**
	 * Submit a staging job to the remote site
	 */
	public synchronized void submit() throws ResourceException 
	{
		logger.debug("Submitting site "+getSite().getId());
		try {
			// Get delegated credential
			GlobusCredential cred = ProxyUtil.getCallerCredential();
		
			// Submit the staging job
			StageSiteOperation operation = new StageSiteOperation(this, cred);
			operation.invoke();
		} catch (GlideinException ge) {
			throw new ResourceException("Unable to submit site: "+ge.getMessage(),ge);
		}
	}
	
	public synchronized void updateStatus(SiteStatus status, String statusMessage)
	throws ResourceException
	{
		logger.debug("Changing status of site "+getSite().getId()+" to "+status+": "+statusMessage);
		
		// Update object
		site.setStatus(status);
		site.setStatusMessage(statusMessage);
		
		// Update database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.updateStatus(site.getId(), status, statusMessage);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
}
