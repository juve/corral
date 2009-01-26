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
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.container.ServiceHost;
import org.globus.wsrf.utils.AddressingUtils;

import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.SiteDAO;
import edu.usc.glidein.stubs.types.Identifiers;
import edu.usc.glidein.stubs.types.ListingRequest;
import edu.usc.glidein.stubs.types.SiteHistory;
import edu.usc.glidein.stubs.types.Sites;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.AuthenticationUtil;

public class SiteFactoryService
{
	private Logger logger = Logger.getLogger(SiteFactoryService.class);
	
	public EndpointReferenceType create(Site site)
	throws RemoteException
	{	
		EndpointReferenceType epr = null;
		
		try {
			
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
			logAndRethrow("Unable to create site", t);
		}
		
		return epr;
	}
	
	public Sites listSites(ListingRequest request)
	throws RemoteException
	{
		Sites sites = null;
		try {
			if(request.getUser()==null && !request.isAllUsers()) {
				request.setUser(AuthenticationUtil.getLocalUsername());
			}
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			Site[] _sites = dao.list(
					request.isLongFormat(),
					request.getUser(),
					request.isAllUsers());
			sites = new Sites(_sites);
		} catch (Throwable t) {
			logAndRethrow("Unable to list sites", t);
		}
		return sites;
	}
	
	public SiteHistory getHistory(Identifiers ids) throws RemoteException
	{
		SiteHistory history = new SiteHistory();
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			history.setEntries(dao.getHistory(ids.getIds()));
		} catch (Throwable t) {
			logAndRethrow("Unable to get site history", t);
		}
		return history;
	}
	
	private void logAndRethrow(String message, Throwable t) throws RemoteException
	{
		logger.error(message,t);
		throw new RemoteException(message,t);
	}
}