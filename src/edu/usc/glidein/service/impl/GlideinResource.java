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
import edu.usc.glidein.stubs.types.GlideinStatus;

public class GlideinResource implements PersistentResource, ResourceProperties
{
	private SimpleResourcePropertySet resourceProperties;
	private Glidein glidein = null;
	private GlideinStatus status = null;
	
	/**
	 * Default constructor required
	 */
	public GlideinResource() { }
	
	public GlideinResource(Glidein glidein, GlideinStatus status) throws ResourceException
	{
		this.glidein = glidein;
		this.status = status;
		setResourceProperties();
	}
	
	public void setStatus(GlideinStatus status) {
		this.status = status;
	}
	
	public GlideinStatus getStatus() {
		return status;
	}
	
	public void setGlidein(Glidein glidein) {
		this.glidein = glidein;
	}
	
	public Glidein getGlidein() {
		return glidein;
	}
	
	private void setResourceProperties() throws ResourceException {
		try {
			resourceProperties = new SimpleResourcePropertySet(GlideinQNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(GlideinQNames.RP_GLIDEIN_ID,"Id",glidein));
			// TODO Set the rest of the resource properties
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
			status = dao.getStatus(id);
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
			dao.updateStatus(glidein.getId(), status);
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
			status = null;
			resourceProperties = null;
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}

	public ResourcePropertySet getResourcePropertySet() {
		return resourceProperties;
	}
}
