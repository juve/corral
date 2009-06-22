/*
 *  Copyright 2007-2009 University Of Southern California
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
import org.globus.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.utils.AddressingUtils;

import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.GlideinDAO;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinHistory;
import edu.usc.glidein.stubs.types.Glideins;
import edu.usc.glidein.stubs.types.Identifiers;
import edu.usc.glidein.stubs.types.ListingRequest;
import edu.usc.glidein.util.AuthenticationUtil;

public class GlideinFactoryService
{
	private Logger logger = Logger.getLogger(GlideinFactoryService.class);
	
	public EndpointReferenceType create(Glidein glidein)
	throws RemoteException
	{
		EndpointReferenceType epr = null;
		
		try {	
			
			// Get resource home
			GlideinResourceHome home = null;
			try {
				home = GlideinResourceHome.getInstance();
			} catch (NamingException e) {
				throw new ResourceException("Unable to get GlideinResourceHome", e);
			}
			
			// Create glidein
			ResourceKey key = home.create(glidein);
			
			// Create an endpoint reference for the new resource
			try {
				URL baseURL = ServiceHost.getBaseURL();
				MessageContext mctx = MessageContext.getCurrentContext();
				String svc = (String) mctx.getService().getOption("instance");
				String instanceURI = baseURL.toString() + svc;
				epr = AddressingUtils.createEndpointReference(instanceURI, key);
			} catch (Exception e) {
				throw new RemoteException("Unable to create endpoint reference", e);
			}
		} catch (Throwable t) {
			logAndRethrow("Unable to create glidein", t);
		}
		
		return epr;
	}
	
	public Glideins listGlideins(ListingRequest request)
	throws RemoteException
	{
		Glideins glideins = null;
		try {
			if(request.getUser()==null && !request.isAllUsers()) {
				request.setUser(AuthenticationUtil.getLocalUsername());
			}
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			Glidein[] _glideins = dao.list(
					request.isLongFormat(),
					request.getUser(),
					request.isAllUsers());
			glideins = new Glideins(_glideins);
		} catch (Throwable t) {
			logAndRethrow("Unable to list glideins", t);
		}
		return glideins;
	}
	
	public GlideinHistory getHistory(Identifiers ids) throws RemoteException
	{
		GlideinHistory history = new GlideinHistory();
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			history.setEntries(dao.getHistory(ids.getIds()));
		} catch (Throwable t) {
			logAndRethrow("Unable to get glidein history", t);
		}
		return history;
	}

	private void logAndRethrow(String message, Throwable t) throws RemoteException
	{
		logger.error(message,t);
		throw new RemoteException(message,t);
	}
}