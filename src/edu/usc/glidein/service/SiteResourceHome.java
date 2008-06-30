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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.ResourceHomeImpl;
import org.globus.wsrf.impl.security.authorization.exceptions.InitializeException;

import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.SiteDAO;
import edu.usc.glidein.stubs.types.Site;

public class SiteResourceHome extends ResourceHomeImpl
{
	private final Logger logger = Logger.getLogger(SiteResourceHome.class);
	private boolean initialized = false;
	
	public synchronized void initialize() throws Exception
	{
		if (initialized)
			return;
		
		logger.info("Initializing sites...");
		
		super.initialize();
		
		// Recover site state
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			Site[] sites = dao.list(true);
			for (Site site : sites) {
			
				// Create a resource object
				SiteResource resource = new SiteResource();
				resource.setSite(site);
				
				// Recover the resource state
				resource.recoverState();
				
				// Add the resource object
				ResourceKey key = resource.getKey();
				this.add(key, resource);
			}
		} catch (DatabaseException de) {
			throw new InitializeException(
					"Unable to get sites from database",de);
		}
		
		initialized = true;
	}
	
	public static SiteResourceHome getInstance() throws NamingException
	{
		String location = "java:comp/env/services/glidein/SiteService/home";
		Context initialContext = new InitialContext();
    	return (SiteResourceHome)initialContext.lookup(location);
	}
	
	public ResourceKey create(Site site)
	throws ResourceException
	{
		SiteResource resource = new SiteResource();
		resource.create(site);
		ResourceKey key = resource.getKey();
		this.add(key, resource);
		return key;
	}
}