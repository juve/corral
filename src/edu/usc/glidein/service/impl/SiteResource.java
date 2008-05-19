package edu.usc.glidein.service.impl;

import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.PersistentResource;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.impl.ReflectionResourceProperty;
import org.globus.wsrf.impl.SimpleResourcePropertySet;

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;

public class SiteResource implements PersistentResource, ResourceProperties
{
	private SimpleResourcePropertySet resourceProperties;
	private Site site;
	private SiteStatus status;
	
	/**
	 * Default constructor required
	 */
	public SiteResource() { }
	
	public SiteResource(Site site, SiteStatus status) throws ResourceException
	{
		this.site = site;
		this.status = status;
		setResourceProperties();
	}
	
	public void setSite(Site site) {
		this.site = site;
	}
	
	public Site getSite() {
		return site;
	}
	
	public void setStatus(SiteStatus status) {
		this.status = status;
	}
	
	public SiteStatus getStatus() {
		return status;
	}
	
	private void setResourceProperties() throws ResourceException
	{
		try {
			resourceProperties = new SimpleResourcePropertySet(SiteQNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(
					SiteQNames.RP_SITE_ID,"Id",site));
			// TODO Set the rest of the resource properties
		} catch(Exception e) {
			throw new ResourceException("Unable to set site resource properties",e);
		}
	}
	
	public synchronized void create() throws ResourceException
	{
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
		System.out.println("Loading "+key.getValue()); //XXX
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			site = dao.load(id);
			status = dao.getStatus(id);
			setResourceProperties();
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void store() throws ResourceException
	{
		System.out.println("Storing "+getSite().getId()); //XXX
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.store(site);
			dao.updateStatus(site.getId(), status);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void delete() throws ResourceException
	{
		System.out.println("Deleting "+getSite().getId()); //XXX
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
			site = null;
			status = null;
			resourceProperties = null;
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public Object getID() {
		return new Integer(site.getId());
	}

	public void remove() throws ResourceException {
		
	}

	public ResourcePropertySet getResourcePropertySet() {
		return resourceProperties;
	}
}
