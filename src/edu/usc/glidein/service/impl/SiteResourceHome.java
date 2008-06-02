package edu.usc.glidein.service.impl;

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
	
	/**
	 * Create a new site and add it
	 */
	public ResourceKey create(Site site)
	throws ResourceException
	{
		SiteResource resource = new SiteResource(site);
		resource.create();
		ResourceKey key = new SimpleResourceKey(
				getKeyTypeName(), new Integer(site.getId()));
		this.add(key, resource);
		return key;
	}

	/**
	 * List all the sites in the database
	 */
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