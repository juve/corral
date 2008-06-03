package edu.usc.glidein.service.impl;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.globus.wsrf.InvalidResourceKeyException;
import org.globus.wsrf.NoSuchResourceException;
import org.globus.wsrf.RemoveNotSupportedException;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceHome;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.jndi.Initializable;

import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.stubs.types.Site;

public class SiteResourceHome implements ResourceHome, Initializable
{
	private Logger logger = Logger.getLogger(SiteResourceHome.class);
	
	public synchronized void initialize() throws Exception
	{
		logger.info("Initializing sites...");
		
		// TODO: Recover site state
	}
	
	public Class getKeyTypeClass()
	{
		return Integer.class;
	}
	
	public QName getKeyTypeName() 
	{
		return SiteNames.RESOURCE_KEY;
	}
	
	public ResourceKey create(Site site)
	throws ResourceException
	{
		SiteResource resource = new SiteResource(site);
		resource.create();
		ResourceKey key = new SimpleResourceKey(
				getKeyTypeName(), new Integer(site.getId()));
		return key;
	}

	public Resource find(ResourceKey key) 
	throws ResourceException, NoSuchResourceException, 
		   InvalidResourceKeyException
	{
		if (key == null) {
			throw new InvalidResourceKeyException();
		}
		SiteResource r = new SiteResource();
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
		SiteResource r = (SiteResource)find(key);
		r.remove();
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