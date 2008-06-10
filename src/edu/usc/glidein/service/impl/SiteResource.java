package edu.usc.glidein.service.impl;

import javax.naming.NamingException;

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
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.SimpleResourcePropertySet;

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.service.state.ReadyQueue;
import edu.usc.glidein.service.state.SiteStateChange;
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
		return getKey();
	}
	
	public ResourceKey getKey()
	{
		if (site==null) return null;
		return new SimpleResourceKey(
				SiteNames.RESOURCE_KEY,
				new Integer(site.getId()));
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
		remove(false);
	}
	
	public synchronized void remove(boolean force) throws ResourceException
	{
		logger.debug("Removing site resource "+getSite().getId());
		
		// If we are already removing, then just return
		if (SiteStatus.REMOVING.equals(site.getStatus())) {
			return;
		}
		
		// TODO: Start a remove operation
		try {
			GlobusCredential cred = ProxyUtil.getCallerCredential();
			ReadyQueue queue = ReadyQueue.getInstance();
			queue.add(new SiteStateChange(getKey()));
		} catch (NamingException ne) {
			throw new ResourceException("Unable to remove site: "+ne.getMessage(),ne);
		}
	}
	
	public synchronized void submit() throws ResourceException 
	{
		logger.debug("Submitting site "+getSite().getId());
		
		// If we are not new, then don't submit
		if (!SiteStatus.NEW.equals(site.getStatus())) {
			throw new ResourceException("Can only submit sites with NEW status");
		}
		
		// TODO: Submit the staging job
		try {
			GlobusCredential cred = ProxyUtil.getCallerCredential();
			ReadyQueue queue = ReadyQueue.getInstance();
			queue.add(new SiteStateChange(getKey()));
		} catch (NamingException ne) {
			throw new ResourceException("Unable to schedule site: "+ne.getMessage(),ne);
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
		
		// If status is already failed, don't change it
		if (status.equals(site.getStatus())) {
			logger.info("Status of site "+getSite().getId()+" is already FAILED");
			return;
		}
		
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
