package edu.usc.glidein.service.impl;

import org.apache.log4j.Logger;
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

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinStatus;

public class GlideinResource implements Resource, ResourceIdentifier, PersistenceCallback, RemoveCallback, ResourceProperties
{
	private Logger logger = Logger.getLogger(GlideinResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Glidein glidein = null;
	
	/**
	 * Default constructor required
	 */
	public GlideinResource() { }
	
	public GlideinResource(Glidein glidein) throws ResourceException
	{
		this.glidein = glidein;
		setResourceProperties();
	}
	
	public Glidein getGlidein()
	{
		return glidein;
	}
	
	public Object getID()
	{
		if (glidein == null) return null;
		return new Integer(glidein.getId());
	}
	
	public ResourcePropertySet getResourcePropertySet()
	{
		return resourceProperties;
	}
	
	private void setResourceProperties() throws ResourceException
	{
		try {
			resourceProperties = new SimpleResourcePropertySet(GlideinNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(GlideinNames.RP_GLIDEIN_ID,"Id",glidein));
			// TODO: Set the rest of the resource properties, or don't
		} catch(Exception e) {
			throw new ResourceException("Unable to set glidein resource properties",e);
		}
	}
	
	public synchronized void create() throws ResourceException
	{
		logger.debug("Creating glidein");
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.create(glidein);
			setResourceProperties();
		} catch (DatabaseException dbe) {
			throw new ResourceException(dbe);
		}
	}

	public synchronized void load(ResourceKey key) throws ResourceException
	{
		logger.debug("Loading "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			glidein = dao.load(id);
			setResourceProperties();
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void store() throws ResourceException 
	{
		logger.debug("Storing "+getGlidein().getId());
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.store(glidein);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void remove() throws ResourceException 
	{
		logger.debug("Removing "+getGlidein().getId());
		
		// TODO: Remove glideins correctly
		delete();
	}

	public synchronized void submit() throws ResourceException
	{
		logger.debug("Submitting "+getGlidein().getId());
		
		// TODO: Get delegated credential
		// TODO: Check to make sure site is ready before submitting
		// TODO: Submit glidein
	}
	
	public synchronized void delete() throws ResourceException
	{
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.delete(glidein.getId());
			glidein = null;
			resourceProperties = null;
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void updateStatus(GlideinStatus status, String statusMessage) 
	throws ResourceException
	{
		logger.debug("Changing status of glidein "+getGlidein().getId()+" to "+status+": "+statusMessage);
		
		// Update object
		glidein.setStatus(status);
		glidein.setStatusMessage(statusMessage);
		
		// Update database
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.updateStatus(glidein.getId(), status, statusMessage);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
}
