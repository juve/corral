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
package edu.usc.glidein.api;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.axis.util.Util;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;

public class BaseService
{
	private EndpointReferenceType epr;
	private ClientSecurityDescriptor descriptor;
	
	public BaseService(EndpointReferenceType epr)
	{
		this.epr = epr;
	}
	
	public ClientSecurityDescriptor getDescriptor()
	{
		return descriptor;
	}
	
	public void setDescriptor(ClientSecurityDescriptor descriptor)
	{
		this.descriptor = descriptor;
		if (descriptor != null) {
			// I got this from org.globus.delegation.client.BaseClient
			// Its required, but completely undocumented. Thanks Globus.
			if (descriptor.getGSITransport() != null) {
				Util.registerTransport();
			}
		}
	}
	
	public EndpointReferenceType getEPR()
	{
		return epr;
	}
	
	public void setEPR(EndpointReferenceType epr)
	{
		this.epr = epr;
	}
}
