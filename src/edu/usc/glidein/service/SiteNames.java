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

public class SiteNames
{
	// Parameters
	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String CREATED = "created";
	public static final String LAST_UPDATE = "last-update";
	public static final String STATE = "state";
	public static final String SHORT_MESSAGE = "short-message";
	public static final String LONG_MESSAGE = "long-message";
	public static final String SUBJECT = "subject";
	public static final String LOCAL_USERNAME = "local-username";
	public static final String INSTALL_PATH = "install-path";
	public static final String LOCAL_PATH = "local-path";
	public static final String CONDOR_VERSION = "condor-version";
	public static final String CONDOR_PACKAGE = "condor-package";
	public static final String STAGING_SERVICE = "staging-service";
	public static final String STAGING_SERVICE_TYPE = "staging-service-type";
	public static final String STAGING_SERVICE_CONTACT = "staging-service-contact";
	public static final String STAGING_SERVICE_PROJECT = "staging-service-project";
	public static final String STAGING_SERVICE_QUEUE = "staging-service-queue";
	public static final String GLIDEIN_SERVICE = "glidein-service";
	public static final String GLIDEIN_SERVICE_TYPE = "glidein-service-type";
	public static final String GLIDEIN_SERVICE_CONTACT = "glidein-service-contact";
	public static final String GLIDEIN_SERVICE_PROJECT = "glidein-service-project";
	public static final String GLIDEIN_SERVICE_QUEUE = "glidein-service-queue";
	public static final String ENVIRONMENT = "environment";
	
	// Service names
	public static final String SITE_FACTORY_SERVICE = "glidein/SiteFactoryService";
	public static final String SITE_SERVICE = "glidein/SiteService";
	
	// Resource properties
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/SiteService";
	public static final QName RESOURCE_KEY = new QName(SERVICE_NS, "SiteServiceKey");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS, "SiteResourceProperties");
	
	public static final QName RP_ID = new QName(SERVICE_NS, ID);
	public static final QName RP_NAME = new QName(SERVICE_NS, NAME);
	public static final QName RP_INSTALL_PATH = new QName(SERVICE_NS, INSTALL_PATH);
	public static final QName RP_LOCAL_PATH = new QName(SERVICE_NS, LOCAL_PATH);
	public static final QName RP_CONDOR_PACKAGE = new QName(SERVICE_NS, CONDOR_PACKAGE);
	public static final QName RP_CONDOR_VERSION = new QName(SERVICE_NS, CONDOR_VERSION);
	public static final QName RP_STATE = new QName(SERVICE_NS, STATE);
	public static final QName RP_SHORT_MESSAGE = new QName(SERVICE_NS, SHORT_MESSAGE);
	public static final QName RP_LONG_MESSAGE = new QName(SERVICE_NS, LONG_MESSAGE);
	public static final QName RP_CREATED = new QName(SERVICE_NS, CREATED);
	public static final QName RP_LAST_UPDATE = new QName(SERVICE_NS, LAST_UPDATE);
	public static final QName RP_STAGING_SERVICE_CONTACT = new QName(SERVICE_NS, STAGING_SERVICE_CONTACT);
	public static final QName RP_STAGING_SERVICE_TYPE = new QName(SERVICE_NS, STAGING_SERVICE_TYPE);
	public static final QName RP_STAGING_PROJECT = new QName(SERVICE_NS, STAGING_SERVICE_PROJECT);
	public static final QName RP_STAGING_QUEUE = new QName(SERVICE_NS, STAGING_SERVICE_QUEUE);
	public static final QName RP_GLIDEIN_SERVICE_CONTACT = new QName(SERVICE_NS, GLIDEIN_SERVICE_CONTACT);
	public static final QName RP_GLIDEIN_SERVICE_TYPE = new QName(SERVICE_NS, GLIDEIN_SERVICE_TYPE);
	public static final QName RP_GLIDEIN_PROJECT = new QName(SERVICE_NS, GLIDEIN_SERVICE_PROJECT);
	public static final QName RP_GLIDEIN_QUEUE = new QName(SERVICE_NS, GLIDEIN_SERVICE_QUEUE);
	
	// State change topic
	public static final QName TOPIC_STATE_CHANGE = new QName(SERVICE_NS, "state-change");
}
