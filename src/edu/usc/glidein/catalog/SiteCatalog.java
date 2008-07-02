package edu.usc.glidein.catalog;

import edu.usc.glidein.stubs.types.Site;

public interface SiteCatalog
{
	public Site getSite(String name) throws SiteCatalogException;
}
