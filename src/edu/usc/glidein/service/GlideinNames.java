/*
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.usc.glidein.service;

import javax.xml.namespace.QName;

public class GlideinNames
{
	// Service names
	public static final String GLIDEIN_FACTORY_SERVICE = "glidein/GlideinFactoryService";
	public static final String GLIDEIN_SERVICE = "glidein/GlideinService";
	
	// Resource properties
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/GlideinService";
	public static final QName RESOURCE_KEY = new QName(SERVICE_NS, "GlideinServiceKey");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS, "GlideinResourceProperties");
	
	public static final QName RP_ID = new QName(SERVICE_NS, "Id");
	public static final QName RP_SITE = new QName(SERVICE_NS, "SiteId");
	public static final QName RP_CONDOR_HOST = new QName(SERVICE_NS, "CondorHost");
	public static final QName RP_COUNT = new QName(SERVICE_NS, "Count");
	public static final QName RP_HOST_COUNT = new QName(SERVICE_NS, "HostCount");
	public static final QName RP_WALL_TIME = new QName(SERVICE_NS, "WallTime");
	public static final QName RP_NUM_CPUS = new QName(SERVICE_NS, "NumCpus");
	public static final QName RP_CONDOR_CONFIG = new QName(SERVICE_NS, "CondorConfig");
	public static final QName RP_GCB_BROKER = new QName(SERVICE_NS, "GcbBroker");
	public static final QName RP_IDLE_TIME = new QName(SERVICE_NS, "IdleTime");
	public static final QName RP_CONDOR_DEBUG = new QName(SERVICE_NS, "CondorDebug");
	public static final QName RP_STATE = new QName(SERVICE_NS, "State");
	public static final QName RP_SHORT_MESSAGE = new QName(SERVICE_NS, "ShortMessage");
	public static final QName RP_LONG_MESSAGE = new QName(SERVICE_NS, "LongMessage");
	public static final QName RP_CREATED = new QName(SERVICE_NS, "Created");
	public static final QName RP_LAST_UPDATE = new QName(SERVICE_NS, "LastUpdate");
	public static final QName RP_RESUBMIT = new QName(SERVICE_NS, "Resubmit");
	public static final QName RP_RESUBMITS = new QName(SERVICE_NS, "Resubmits");
	public static final QName RP_UNTIL = new QName(SERVICE_NS, "Until");
	public static final QName RP_SUBMITS = new QName(SERVICE_NS, "Submits");
}
