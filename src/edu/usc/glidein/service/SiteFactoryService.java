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

import java.net.URL;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.apache.axis.MessageContext;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.wsrf.ResourceContext;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.utils.AddressingUtils;

import edu.usc.glidein.stubs.types.Sites;
import edu.usc.glidein.stubs.types.Site;

public class SiteFactoryService
{
	public EndpointReferenceType createSite(Site site)
	throws RemoteException
	{	
		// Get resource home
		SiteResourceHome home = null;
		try {
			home = SiteResourceHome.getInstance();
		} catch (NamingException ne) {
			throw new RemoteException("Unable to get SiteResourceHome", ne);
		}
		
		// Create resource
		ResourceKey key = home.create(site);
		
		// Create an endpoint reference for the new resource
		EndpointReferenceType epr = null;
		try {
			URL baseURL = ServiceHost.getBaseURL();
			MessageContext mctx = MessageContext.getCurrentContext();
			String svc = (String) mctx.getService().getOption("instance");
			String instanceURI = baseURL.toString() + svc;
			epr = AddressingUtils.createEndpointReference(instanceURI, key);
		} catch (Exception e) {
			throw new RemoteException("Unable to create endpoint reference", e);
		}
		
		// Return the endpoint reference to the client
		return epr;
	}
	
	public Sites listSites(boolean longFormat)
	throws RemoteException
	{
		try {
			ResourceContext rctx = ResourceContext.getResourceContext();
			SiteResourceHome home = (SiteResourceHome) rctx.getResourceHome();
			Site[] sites = home.list(longFormat);
			return new Sites(sites);
		} catch (Exception e) {
			throw new RemoteException("Unable to list sites", e);
		}
	}
}