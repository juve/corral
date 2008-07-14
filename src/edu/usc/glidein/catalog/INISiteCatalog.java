package edu.usc.glidein.catalog;

import static edu.usc.glidein.service.SiteNames.*;

import java.io.File;
import java.util.Properties;

import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.INI;
import edu.usc.glidein.util.SiteUtil;

public class INISiteCatalog implements SiteCatalog
{
	private INI ini;
	
	public INISiteCatalog(File iniFile) throws SiteCatalogException
	{
		read(iniFile);
	}
	
	private void read(File iniFile) throws SiteCatalogException
	{
		try {
			ini = new INI();
			ini.read(iniFile);
		} catch (Exception e) {
			throw new SiteCatalogException(
					"Error reading INI catalog file.",e);
		}
	}
	
	public Site getSite(String name) throws SiteCatalogException
	{
		if (ini.hasSection(name)) {
			return extractINISite(ini,name);
		} else {
			throw new SiteCatalogException(
					"Site '"+name+"' not found in site catalog");
		}
	}
	
	private Site extractINISite(INI ini, String name) throws SiteCatalogException
	{
		try {
			Properties p = new Properties();
			p.setProperty(NAME, name);
			p.setProperty(INSTALL_PATH, getINIValue(ini,name,INSTALL_PATH));
			p.setProperty(LOCAL_PATH, getINIValue(ini,name,LOCAL_PATH));
			p.setProperty(CONDOR_PACKAGE, getINIValue(ini,name,CONDOR_PACKAGE));
			p.setProperty(CONDOR_VERSION, getINIValue(ini,name,CONDOR_VERSION));
			p.setProperty(STAGING_SERVICE, getINIValue(ini,name,STAGING_SERVICE));
			p.setProperty(STAGING_SERVICE_PROJECT, getINIValue(ini,name,STAGING_SERVICE_PROJECT));
			p.setProperty(STAGING_SERVICE_QUEUE, getINIValue(ini,name,STAGING_SERVICE_QUEUE));
			p.setProperty(GLIDEIN_SERVICE, getINIValue(ini,name,GLIDEIN_SERVICE));
			p.setProperty(GLIDEIN_SERVICE_PROJECT, getINIValue(ini,name,GLIDEIN_SERVICE_PROJECT));
			p.setProperty(GLIDEIN_SERVICE_QUEUE, getINIValue(ini,name,GLIDEIN_SERVICE_QUEUE));
			p.setProperty(ENVIRONMENT, getINIValue(ini,name,ENVIRONMENT));
			return SiteUtil.createSite(p);
		} catch (Exception e) {
			throw new SiteCatalogException(e);
		}
	}

	private String getINIValue(INI ini, String site, String key)
	{
		String value = ini.getString(site, key, null);
		if (value == null) {
			value = ini.getString(key, null);
		}
		if (value != null) {
			value = value.trim();
		}
		return value;
	}
}
