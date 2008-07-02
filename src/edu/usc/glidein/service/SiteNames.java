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
	// Service names
	public static final String SITE_FACTORY_SERVICE = "glidein/SiteFactoryService";
	public static final String SITE_SERVICE = "glidein/SiteService";
	
	// Resource properties
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/SiteService";
	public static final QName RESOURCE_KEY = new QName(SERVICE_NS, "SiteServiceKey");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS, "SiteResourceProperties");
	
	public static final QName RP_ID = new QName(SERVICE_NS, "Id");
	public static final QName RP_NAME = new QName(SERVICE_NS, "Name");
	public static final QName RP_INSTALL_PATH = new QName(SERVICE_NS, "InstallPath");
	public static final QName RP_LOCAL_PATH = new QName(SERVICE_NS, "LocalPath");
	public static final QName RP_CONDOR_PACKAGE = new QName(SERVICE_NS, "CondorPackage");
	public static final QName RP_CONDOR_VERSION = new QName(SERVICE_NS, "CondorVersion");
	public static final QName RP_STATE = new QName(SERVICE_NS, "State");
	public static final QName RP_SHORT_MESSAGE = new QName(SERVICE_NS, "ShortMessage");
	public static final QName RP_LONG_MESSAGE = new QName(SERVICE_NS, "LongMessage");
	public static final QName RP_CREATED = new QName(SERVICE_NS, "Created");
	public static final QName RP_LAST_UPDATE = new QName(SERVICE_NS, "LastUpdate");
	public static final QName RP_STAGING_SERVICE_CONTACT = new QName(SERVICE_NS, "StagingService/ServiceContact");
	public static final QName RP_STAGING_SERVICE_TYPE = new QName(SERVICE_NS, "StagingService/ServiceType");
	public static final QName RP_STAGING_PROJECT = new QName(SERVICE_NS, "StagingService/Project");
	public static final QName RP_STAGING_QUEUE = new QName(SERVICE_NS, "StagingService/Queue");
	public static final QName RP_GLIDEIN_SERVICE_CONTACT = new QName(SERVICE_NS, "GlideinService/ServiceContact");
	public static final QName RP_GLIDEIN_SERVICE_TYPE = new QName(SERVICE_NS, "GlideinService/ServiceType");
	public static final QName RP_GLIDEIN_PROJECT = new QName(SERVICE_NS, "GlideinService/Project");
	public static final QName RP_GLIDEIN_QUEUE = new QName(SERVICE_NS, "GlideinService/Queue");
}
