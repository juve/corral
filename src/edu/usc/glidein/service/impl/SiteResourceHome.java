package edu.usc.glidein.service.impl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.SimpleResourceKey;

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.stubs.types.Site;

public class SiteResourceHome extends ResourceHomeImpl
{
	private Logger logger = Logger.getLogger(SiteResourceHome.class);
	
	public synchronized void initialize() throws Exception
	{
		logger.info("Initializing sites...");
		super.initialize();
		
		// TODO: Recover site state
	}
	
	public static SiteResourceHome getInstance() throws NamingException
	{
		String location = "java:comp/env/services/glidein/SiteService/home";
		Context initialContext = new InitialContext();
    	return (SiteResourceHome)initialContext.lookup(location);
	}
	
	public ResourceKey create(Site site)
	throws ResourceException
	{
		SiteResource resource = new SiteResource();
		resource.create(site);
		ResourceKey key = new SimpleResourceKey(
				getKeyTypeName(), new Integer(site.getId()));
		this.add(key, resource);
		return key;
	}

	public Site[] list(boolean longFormat) 
	throws ResourceException
	{
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			return dao.list(longFormat);
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to list sites",de);
		}
	}
}