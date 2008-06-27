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

import java.rmi.RemoteException;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.wsrf.ResourceContext;

import edu.usc.glidein.stubs.RemoveRequest;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Site;

public class SiteService 
{
	private SiteResource getResource() throws RemoteException
	{
		try {
			return (SiteResource) ResourceContext.getResourceContext().getResource();
		} catch (Exception e) {
			throw new RemoteException("Unable to find site resource", e);
		}	
	}
	
	public Site getSite(EmptyObject empty) throws RemoteException
	{
		return getResource().getSite();
	}
	
	public EmptyObject submit(EndpointReferenceType credential) throws RemoteException
	{
		getResource().submit(credential);
		return new EmptyObject();
	}
	
	public EmptyObject remove(RemoveRequest request) throws RemoteException
	{
		boolean force = request.isForce();
		EndpointReferenceType credential = request.getCredential();
		getResource().remove(force,credential);
		return new EmptyObject();
	}
}