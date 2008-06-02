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
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.stubs.types.Glidein;

public class GlideinResource implements PersistentResource, ResourceProperties
{
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
	
	public void setGlidein(Glidein glidein) {
		this.glidein = glidein;
	}
	
	public Glidein getGlidein() {
		return glidein;
	}
	
	private void setResourceProperties() throws ResourceException {
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
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.create(glidein);
			setResourceProperties();
		} catch (DatabaseException dbe) {
			throw new ResourceException(dbe);
		}
	}

	public synchronized void load(ResourceKey key) throws ResourceException {
		System.out.println("Loading "+key.getValue());
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
	
	public synchronized void store() throws ResourceException {
		System.out.println("Storing "+getGlidein().getId());
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.store(glidein);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}

	public Object getID() {
		return new Integer(glidein.getId());
	}

	public void remove() throws ResourceException {
		
	}
	
	public synchronized void delete() throws ResourceException {
		System.out.println("Deleting "+getGlidein().getId());
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

	public synchronized void submit() throws ResourceException
	{
		// TODO: Get delegated credential
		// TODO: Check to make sure site is ready before submitting
		// TODO: Submit glidein
	}
	
	public ResourcePropertySet getResourcePropertySet() {
		return resourceProperties;
	}
}
