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
package edu.usc.corral.service;

import java.util.Date;
import java.util.List;

import edu.usc.corral.api.GlideinException;
import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.db.Database;
import edu.usc.corral.db.DatabaseException;
import edu.usc.corral.db.SiteDAO;
import edu.usc.corral.types.CreateSiteRequest;
import edu.usc.corral.types.CreateSiteResponse;
import edu.usc.corral.types.ExecutionService;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.ListSitesResponse;
import edu.usc.corral.types.RemoveRequest;
import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteState;
import edu.usc.corral.types.SubmitRequest;
import edu.usc.corral.types.VoidResponse;

public class SiteService extends Service {

	public CreateSiteResponse create(CreateSiteRequest req) throws GlideinException {
		// Create new site
		Site site = new Site(req);
			
		// Set owner
		site.setSubject(getSubject());
		site.setLocalUsername(getUsername());
			
		// Set state
		site.setState(SiteState.NEW);
		site.setShortMessage("Created");
		Date time = new Date();
		site.setLastUpdate(time);
		site.setCreated(time);
			
		// Must have name
		if (site.getName() == null)
			throw new GlideinException("Site must have name");
			
		// Check staging service
		ExecutionService stagingService = site.getStagingService();
		if (stagingService == null)
			throw new GlideinException("Must provide staging service");
		if (stagingService.getServiceContact() == null || 
				stagingService.getServiceType() == null)
			throw new GlideinException("Invalid staging service: " +
					"must specify service contact and service type");
			
		// Check glidein service
		ExecutionService glideinService = site.getGlideinService();
		if (glideinService == null)
			throw new GlideinException("Must provide glidein service");
		if (glideinService.getServiceContact() == null || 
				glideinService.getServiceType() == null)
			throw new GlideinException("Invalid glidein service: " +
					"must specify service contact and service type");
			
		// Must specify condorPackage or condorVersion
		if (site.getCondorPackage() == null && site.getCondorVersion() == null)
			throw new GlideinException(
					"Must specify condor package OR condor version");
			
		// Check install path
		if (site.getInstallPath() == null)
			throw new GlideinException("Must specify install path");
			
		// Check local path
		if (site.getLocalPath() == null) 
			throw new GlideinException("Must specify local path");
			
		try {
			SiteResourceHome home = SiteResourceHome.getInstance();
			return new CreateSiteResponse(home.create(site));
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get SiteResourceHome", ne);
		}
	}
	
	public ListSitesResponse list(ListRequest req) throws GlideinException {
		try {
			if(req.getUser()==null && !req.isAllUsers()) {
				req.setUser(getUsername());
			}
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			List<Site> sites = dao.list(req.isLongFormat(),req.getUser(),req.isAllUsers());
			return new ListSitesResponse(sites);
		} catch (DatabaseException de) {
			throw new GlideinException("Unable to list sites", de);
		}
	}
	
	public Site get(GetRequest req) throws GlideinException {
		return getResource(req.getId()).getSite();
	}
	
	public VoidResponse submit(SubmitRequest req) throws GlideinException {
		SiteResource r = getResource(req.getId());
		if (!r.authorized(getSubject())) {
			throw new GlideinException("Not authorized");
		}
		r.submit(req.getGlobusCredential());
		return new VoidResponse();
	}
	
	public VoidResponse remove(RemoveRequest req) throws GlideinException {
		SiteResource r = getResource(req.getId());
		if (!r.authorized(getSubject())) {
			throw new GlideinException("Not authorized");
		}
		r.remove(req.isForce(), req.getGlobusCredential());
		return new VoidResponse();
	}
	
	private SiteResource getResource(int id) throws GlideinException {
		try {
			SiteResourceHome home = SiteResourceHome.getInstance();
			return home.find(id);
		} catch (ConfigurationException e) {
			throw new GlideinException("Unable to find site resource", e);
		}	
	}
}