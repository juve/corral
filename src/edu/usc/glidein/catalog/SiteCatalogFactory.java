package edu.usc.glidein.catalog;

import java.io.File;

public class SiteCatalogFactory
{
	public static SiteCatalog getSiteCatalog(File catalogFile) 
	throws SiteCatalogException
	{
		String name = catalogFile.getName();
		if (name.endsWith(".xml")) {
			return getSiteCatalog(catalogFile,SiteCatalogFormat.XML);
		} else if (name.endsWith(".ini")) {
			return getSiteCatalog(catalogFile,SiteCatalogFormat.INI);
		} else {
			throw new SiteCatalogException(
					"Unable to determine site catalog format from extension");
		}
	}
	
	public static SiteCatalog getSiteCatalog(File catalogFile, 
			SiteCatalogFormat format) throws SiteCatalogException
	{
		switch (format) {
			case XML:
				return new PegasusSiteCatalog(catalogFile);
			case INI:
				return new INISiteCatalog(catalogFile);
			default:
				throw new SiteCatalogException(
						"Unrecognized format: "+format);
		}
	}
}
