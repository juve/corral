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
	// Parameters
	public static final String ID = "id";
	public static final String SITE = "site";
	public static final String SITE_NAME = "site-name";
	public static final String CONDOR_HOST = "condor-host";
	public static final String COUNT = "count";
	public static final String HOST_COUNT = "host-count";
	public static final String WALL_TIME = "wall-time";
	public static final String NUM_CPUS = "num-cpus";
	public static final String CONDOR_CONFIG = "condor-config";
	public static final String GCB_BROKER = "gcb-broker";
	public static final String IDLE_TIME = "idle-time";
	public static final String CONDOR_DEBUG = "condor-debug";
	public static final String STATE = "state";
	public static final String SHORT_MESSAGE = "short-message";
	public static final String LONG_MESSAGE = "long-message";
	public static final String CREATED = "created";
	public static final String LAST_UPDATE = "last-update";
	public static final String RESUBMIT = "resubmit";
	public static final String RESUBMITS = "resubmits";
	public static final String UNTIL = "until";
	public static final String SUBMITS = "submits";
	public static final String RSL = "rsl";
	public static final String SUBJECT = "subject";
	public static final String LOCAL_USERNAME = "local-username";
	public static final String LOWPORT = "lowport";
	public static final String HIGHPORT = "highport";
	
	// Service names
	public static final String GLIDEIN_FACTORY_SERVICE = "glidein/GlideinFactoryService";
	public static final String GLIDEIN_SERVICE = "glidein/GlideinService";
	
	// Resource properties
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/GlideinService";
	public static final QName RESOURCE_KEY = new QName(SERVICE_NS, "GlideinServiceKey");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS, "GlideinResourceProperties");
	
	public static final QName RP_ID = new QName(SERVICE_NS, ID);
	public static final QName RP_SITE = new QName(SERVICE_NS, SITE);
	public static final QName RP_CONDOR_HOST = new QName(SERVICE_NS, CONDOR_HOST);
	public static final QName RP_COUNT = new QName(SERVICE_NS, COUNT);
	public static final QName RP_HOST_COUNT = new QName(SERVICE_NS, HOST_COUNT);
	public static final QName RP_WALL_TIME = new QName(SERVICE_NS, WALL_TIME);
	public static final QName RP_NUM_CPUS = new QName(SERVICE_NS, NUM_CPUS);
	public static final QName RP_CONDOR_CONFIG = new QName(SERVICE_NS, CONDOR_CONFIG);
	public static final QName RP_GCB_BROKER = new QName(SERVICE_NS, GCB_BROKER);
	public static final QName RP_IDLE_TIME = new QName(SERVICE_NS, IDLE_TIME);
	public static final QName RP_CONDOR_DEBUG = new QName(SERVICE_NS, CONDOR_DEBUG);
	public static final QName RP_STATE = new QName(SERVICE_NS, STATE);
	public static final QName RP_SHORT_MESSAGE = new QName(SERVICE_NS, SHORT_MESSAGE);
	public static final QName RP_LONG_MESSAGE = new QName(SERVICE_NS, LONG_MESSAGE);
	public static final QName RP_CREATED = new QName(SERVICE_NS, CREATED);
	public static final QName RP_LAST_UPDATE = new QName(SERVICE_NS, LAST_UPDATE);
	public static final QName RP_RESUBMIT = new QName(SERVICE_NS, RESUBMIT);
	public static final QName RP_RESUBMITS = new QName(SERVICE_NS, RESUBMITS);
	public static final QName RP_UNTIL = new QName(SERVICE_NS, UNTIL);
	public static final QName RP_SUBMITS = new QName(SERVICE_NS, SUBMITS);
	public static final QName RP_RSL = new QName(SERVICE_NS, RSL);
	public static final QName RP_LOWPORT = new QName(SERVICE_NS, LOWPORT);
	public static final QName RP_HIGHPORT = new QName(SERVICE_NS, HIGHPORT);
	
	// Topics
	public static final QName TOPIC_STATE_CHANGE = new QName(SERVICE_NS, "state-change");
}
