package edu.usc.glidein.service.db;

import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;

public interface SiteDAO
{
	public int create(Site site) throws DatabaseException;
	public void store(Site site) throws DatabaseException;
	public Site load(int siteId) throws DatabaseException;
	public void delete(int siteId) throws DatabaseException;
	public void updateState(int siteId, SiteState state, String shortMessage, String longMessage) throws DatabaseException;
	public Site[] list(boolean full) throws DatabaseException;
}
