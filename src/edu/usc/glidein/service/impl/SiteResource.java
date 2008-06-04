package edu.usc.glidein.service.impl;

import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.PersistenceCallback;
import org.globus.wsrf.RemoveCallback;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceIdentifier;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.impl.ReflectionResourceProperty;
import org.globus.wsrf.impl.SimpleResourcePropertySet;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.RemoveSiteOperation;
import edu.usc.glidein.service.StageSiteOperation;
import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;
import edu.usc.glidein.util.ProxyUtil;

public class SiteResource implements Resource, ResourceIdentifier, PersistenceCallback, RemoveCallback, ResourceProperties
{
	private Logger logger = Logger.getLogger(SiteResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Site site;
	
	/**
	 * Default constructor required
	 */
	public SiteResource() { }
	
	public SiteResource(Site site) throws ResourceException
	{
		this.site = site;
		setResourceProperties();
	}
	
	public Site getSite()
	{
		return site;
	}
	
	public Object getID()
	{
		if (site==null) return null;
		return new Integer(site.getId());
	}
	
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
	
	public synchronized void store() throws ResourceException
	{
		logger.debug("Storing site resource "+getSite().getId());
		
		// If we are removing, then don't store
		if (SiteStatus.REMOVING.equals(site.getStatus())) {
			throw new ResourceException("Site is being removed");
		}
		
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.store(site);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void remove() throws ResourceException
	{
		logger.debug("Removing site resource "+getSite().getId());
		
		// If we are already removing, then just return
		if (SiteStatus.REMOVING.equals(site.getStatus())) {
			return;
		}
		
		// Update status to prevent others from updating
		updateStatus(SiteStatus.REMOVING, "Removing site");
		
		// Start a remove operation
		try {
			GlobusCredential cred = ProxyUtil.getCallerCredential();
			RemoveSiteOperation op = new RemoveSiteOperation(this,cred); 
			op.invoke();
		} catch (GlideinException ge) {
			throw new ResourceException("Unable to remove site: "+ge.getMessage(),ge);
		}
	}
	
	public synchronized void submit() throws ResourceException 
	{
		logger.debug("Submitting site "+getSite().getId());
		
		// If we are not new, then don't submit
		if (!SiteStatus.NEW.equals(site.getStatus())) {
			throw new ResourceException("Can only submit sites with NEW status");
		}
		
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
	
	public synchronized void delete() throws ResourceException
	{
		// Remove the site from the database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException(de);
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
