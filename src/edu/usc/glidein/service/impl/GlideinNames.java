package edu.usc.glidein.service.impl;

import javax.xml.namespace.QName;

public class GlideinNames
{
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/GlideinService";
	public static final QName RESOURCE_KEY = new QName(SERVICE_NS, "GlideinServiceKey");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS, "GlideinResourceProperties");
	
	public static final QName RP_GLIDEIN_ID = new QName(SERVICE_NS, "GlideinId");
	public static final String GLIDEIN_FACTORY_SERVICE = "glidein/GlideinFactoryService";
	public static final String GLIDEIN_SERVICE = "glidein/GlideinService";
}
