package edu.usc.glidein.service.impl;

import javax.xml.namespace.QName;

public class SiteQNames
{
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/SiteService";
	public static final QName RESOURCE_KEY = new QName(SERVICE_NS, "SiteServiceKey");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS, "SiteResourceProperties");
	
	public static final QName RP_SITE_ID = new QName(SERVICE_NS, "SiteId");
}
