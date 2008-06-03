package edu.usc.glidein.service.impl;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.globus.wsrf.InvalidResourceKeyException;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.RemoveNotSupportedException;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceHome;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.jndi.Initializable;

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.stubs.types.Glidein;

public class GlideinResourceHome implements ResourceHome, Initializable
{
	private Logger logger = Logger.getLogger(GlideinResourceHome.class);
	
	public synchronized void initialize() throws Exception
	{
		logger.info("Initializing glideins...");
		
		// TODO: Recover glidein state
	}
	
	public Class getKeyTypeClass()
	{
		return Integer.class;
	}
	
	public QName getKeyTypeName()
	{
		return GlideinNames.RESOURCE_KEY;
	}
	
	public ResourceKey create(Glidein glidein)
	throws ResourceException
	{
		GlideinResource resource = new GlideinResource(glidein);
		resource.create();
		ResourceKey key = new SimpleResourceKey(
				getKeyTypeName(), new Integer(glidein.getId()));
		return key;
	}
	
	public Resource find(ResourceKey key)
	throws ResourceException, NoSuchResourceException, 
		   InvalidResourceKeyException
	{
		if (key == null) {
			throw new InvalidResourceKeyException();
		}
		GlideinResource r = new GlideinResource();
		r.load(key);
		return r;
	}
	
	public void remove(ResourceKey key) 
	throws ResourceException, NoSuchResourceException,
		   InvalidResourceKeyException, RemoveNotSupportedException
	{
		if (key == null) {
			throw new InvalidResourceKeyException();
		}
		GlideinResource r = (GlideinResource)find(key);
		r.remove();
	}

	public Glidein[] list(boolean longFormat)
	throws ResourceException
	{
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			return dao.list(longFormat);
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to list sites",de);
		}
	}
}