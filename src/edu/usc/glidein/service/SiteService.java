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

import org.globus.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceContext;

import edu.usc.glidein.stubs.RemoveRequest;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Site;

/* TODO Add --nostaging option for site creation
 * This will allow the site to enter the READY state without staging
 * executables and may be useful for sites that 1) don't have a shared
 * filesystem, or 2) have already been set up manually.
 */

public class SiteService 
{
	private Logger logger = Logger.getLogger(SiteService.class);
	
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
		Site site = null;
		try {
			site = getResource().getSite();
		} catch (Throwable t) {
			logAndRethrow("Unable to get site", t);
		}
		return site;
	}
	
	public EmptyObject submit(EndpointReferenceType credential) throws RemoteException
	{
		try {
			getResource().submit(credential);
		} catch (Throwable t) {
			logAndRethrow("Unable to submit site", t);
		}
		return new EmptyObject();
	}
	
	public EmptyObject remove(RemoveRequest request) throws RemoteException
	{
		try {
			boolean force = request.isForce();
			EndpointReferenceType credential = request.getCredential();
			getResource().remove(force,credential);
		} catch (Throwable t) {
			logAndRethrow("Unable to remove site", t);
		}
		return new EmptyObject();
	}
	
	private void logAndRethrow(String message, Throwable t) throws RemoteException
	{
		logger.error(message,t);
		throw new RemoteException(message,t);
	}
}