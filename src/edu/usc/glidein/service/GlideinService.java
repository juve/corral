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
import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceContext;

import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;

public class GlideinService
{
	private Logger logger = Logger.getLogger(GlideinService.class);
	
	private GlideinResource getResource() throws RemoteException
	{
		try {
			Object resource = ResourceContext.getResourceContext().getResource();
			GlideinResource glideinResource = (GlideinResource) resource;
			return glideinResource;
		} catch (Exception e) {
			throw new RemoteException("Unable to find glidein resource", e);
		}
	}
	
	public Glidein getGlidein(EmptyObject empty) throws RemoteException
	{
		Glidein glidein = null;
		try {
			glidein = getResource().getGlidein();
		} catch (Throwable t) {
			logAndRethrow("Unable to get glidein", t);
		}
		return glidein;
	}
	
	public EmptyObject submit(EndpointReferenceType credential) throws RemoteException
	{
		try {
			getResource().submit(credential);
		} catch (Throwable t) {
			logAndRethrow("Unable to submit glidein",t);
		}
		return new EmptyObject();
	}
	
	public EmptyObject remove(boolean force) throws RemoteException
	{
		try {
			getResource().remove(force);
		} catch (Throwable t) {
			logAndRethrow("Unable to remove glidein",t);
		}
		return new EmptyObject();
	}
	
	private void logAndRethrow(String message, Throwable t) throws RemoteException
	{
		logger.error(message,t);
		throw new RemoteException(message,t);
	}
}
