package edu.usc.glidein.service.impl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.security.authorization.exceptions.InitializeException;

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.stubs.types.Glidein;

public class GlideinResourceHome extends ResourceHomeImpl
{
	private final Logger logger = Logger.getLogger(GlideinResourceHome.class);
	private boolean initialized = false;
	
	public synchronized void initialize() throws Exception
	{
		if (initialized)
			return;
		
		logger.info("Initializing glideins...");
		
		super.initialize();
		
		// Recover glidein state
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			Glidein[] glideins = dao.list(true);
			for (Glidein glidein : glideins) {
			
				// Create a resource object
				GlideinResource resource = new GlideinResource();
				resource.setGlidein(glidein);
				
				// Recover the resource state
				resource.recoverState();
				
				// Add the resource object
				ResourceKey key = resource.getKey();
				this.add(key, resource);
			}
		} catch (DatabaseException de) {
			throw new InitializeException(
					"Unable to get glideins from database",de);
		}
		
		initialized = true;
	}
	
	public static GlideinResourceHome getInstance() throws NamingException
	{
		String location = "java:comp/env/services/glidein/GlideinService/home";
		Context initialContext = new InitialContext();
    	return (GlideinResourceHome)initialContext.lookup(location);
	}
	
	public ResourceKey create(Glidein glidein)
	throws ResourceException
	{
		GlideinResource resource = new GlideinResource();
		resource.create(glidein);
		ResourceKey key = new SimpleResourceKey(
				getKeyTypeName(), new Integer(glidein.getId()));
		this.add(key, resource);
		return key;
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