package edu.usc.glidein.service.impl;

import javax.xml.namespace.QName;

public class PoolQNames
{
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/PoolService";
	public static final QName RP_CONDOR_HOST = new QName(SERVICE_NS, "CondorHost");
	public static final QName RP_CONDOR_PORT = new QName(SERVICE_NS, "CondorPort");
	public static final QName RP_CONDOR_VERSION = new QName(SERVICE_NS, "CondorVersion");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS,"PoolResourceProperties");
}
