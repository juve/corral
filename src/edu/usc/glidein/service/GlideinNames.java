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
	public static final String SERVICE_NS = "http://www.usc.edu/glidein/GlideinService";
	public static final QName RESOURCE_KEY = new QName(SERVICE_NS, "GlideinServiceKey");
	public static final QName RESOURCE_PROPERTIES = new QName(SERVICE_NS, "GlideinResourceProperties");
	
	public static final QName RP_GLIDEIN_ID = new QName(SERVICE_NS, "GlideinId");
	public static final String GLIDEIN_FACTORY_SERVICE = "glidein/GlideinFactoryService";
	public static final String GLIDEIN_SERVICE = "glidein/GlideinService";
}
