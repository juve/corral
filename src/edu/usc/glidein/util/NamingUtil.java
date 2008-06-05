package edu.usc.glidein.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import edu.usc.glidein.service.impl.GlideinFactoryService;
import edu.usc.glidein.service.impl.GlideinNames;
import edu.usc.glidein.service.impl.GlideinResourceHome;
import edu.usc.glidein.service.impl.GlideinService;
import edu.usc.glidein.service.impl.SiteFactoryService;
import edu.usc.glidein.service.impl.SiteNames;
import edu.usc.glidein.service.impl.SiteResourceHome;
import edu.usc.glidein.service.impl.SiteService;

public class NamingUtil 
{
	public static String getHomeLocation(String service)
	{
		return "java:comp/env/services/" + service + "/home";
	}
	
	public static SiteResourceHome getSiteResourceHome()
	throws NamingException
	{
		String location = getHomeLocation(SiteNames.SITE_SERVICE);
		Context initialContext = new InitialContext();
    	return (SiteResourceHome)initialContext.lookup(location);
	}
	
	public static GlideinResourceHome getGlideinResourceHome()
	throws NamingException
	{
		String location = getHomeLocation(GlideinNames.GLIDEIN_SERVICE);
		Context initialContext = new InitialContext();
    	return (GlideinResourceHome)initialContext.lookup(location);
	}
	
	public static String getServiceLocation(String service)
	{
		return "java:comp/env//services/" + service;
	}
	
	public static SiteService getSiteService()
	throws NamingException
	{
		String location = getServiceLocation(SiteNames.SITE_SERVICE);
		Context initialContext = new InitialContext();
    	return (SiteService)initialContext.lookup(location);
	}
	
	public static SiteFactoryService getSiteFactoryService()
	throws NamingException
	{
		String location = getServiceLocation(SiteNames.SITE_FACTORY_SERVICE);
		Context initialContext = new InitialContext();
    	return (SiteFactoryService)initialContext.lookup(location);
	}
	
	public static GlideinService getGlideinService()
	throws NamingException
	{
		String location = getServiceLocation(GlideinNames.GLIDEIN_SERVICE);
		Context initialContext = new InitialContext();
    	return (GlideinService)initialContext.lookup(location);
	}
	
	public static GlideinFactoryService getGlideinFactoryService() 
	throws NamingException
	{
		String location = getServiceLocation(GlideinNames.GLIDEIN_FACTORY_SERVICE);
		Context initialContext = new InitialContext();
    	return (GlideinFactoryService)initialContext.lookup(location);
	}
}
