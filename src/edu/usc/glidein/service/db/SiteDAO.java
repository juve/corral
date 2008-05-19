package edu.usc.glidein.service.db;

import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;

public interface SiteDAO
{
	public int create(Site site) throws DatabaseException;
	public void store(Site site) throws DatabaseException;
	public Site load(int siteId) throws DatabaseException;
	public void delete(int siteId) throws DatabaseException;
	public SiteStatus getStatus(int siteId) throws DatabaseException;
	public void updateStatus(int siteId, SiteStatus status) throws DatabaseException;
}
