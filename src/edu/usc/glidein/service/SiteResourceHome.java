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

import org.apache.log4j.Logger;

import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.config.Initializable;
import edu.usc.corral.config.Registry;
import edu.usc.corral.types.ExecutionService;
import edu.usc.corral.types.Site;
import edu.usc.glidein.api.GlideinException;
import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.SiteDAO;
import edu.usc.glidein.nl.NetLogger;
import edu.usc.glidein.nl.NetLoggerEvent;
import edu.usc.glidein.nl.NetLoggerException;

public class SiteResourceHome extends ResourceHome implements Initializable {
	private final Logger logger = Logger.getLogger(SiteResourceHome.class);
	
	public synchronized void initialize() throws Exception {
		logger.info("Recovering sites...");
		
		// Recover site state
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			int[] ids = dao.listIds();
			for (int id : ids) {
				SiteResource resource = find(id);
				resource.recoverState();
			}
		} catch (DatabaseException de) {
			throw new Exception(
					"Unable to get sites from database",de);
		}
	}
	
	public static SiteResourceHome getInstance() throws ConfigurationException {
		return (SiteResourceHome)new Registry().lookup("corral/SiteResourceHome");
	}
	
	public int create(Site site) throws GlideinException {
		
		logger.info("Creating site...");
		
		// Save site in database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.create(site);
		} catch (DatabaseException de) {
			throw new GlideinException("Unable to create site", de);
		}
		
		ExecutionService stagingService = site.getStagingService();
		ExecutionService glideinService = site.getGlideinService();
		
		// Log it in the netlogger log
		try {
			NetLoggerEvent event = new NetLoggerEvent("site.new");
			event.setTimeStamp(site.getCreated());
			event.put("site.id", site.getId());
			event.put("name", site.getName());
			event.put("install_path", site.getInstallPath());
			event.put("local_path", site.getLocalPath());
			event.put("staging.type", stagingService.getServiceType());
			event.put("staging.contact", stagingService.getServiceContact());
			event.put("staging.queue", stagingService.getQueue());
			event.put("staging.project", stagingService.getProject());
			event.put("glidein.type", glideinService.getServiceType());
			event.put("glidein.contact", glideinService.getServiceContact());
			event.put("glidein.queue", glideinService.getQueue());
			event.put("glidein.project", glideinService.getProject());
			event.put("condor.version", site.getCondorVersion());
			event.put("condor.package", site.getCondorPackage());
			event.put("owner.subject", site.getSubject());
			event.put("owner.username", site.getLocalUsername());
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			logger.warn("Unable to log site event to NetLogger log",nle);
		}
		
		SiteResource resource = new SiteResource(site);
		add(site.getId(), resource);
		return site.getId();
	}
	
	public SiteResource find(int id) throws GlideinException {
		synchronized (this) {
			if (contains(id)) {
				return (SiteResource)get(id);
			} else {
				Site s = load(id);
				SiteResource r = new SiteResource(s);
				add(id, r);
				return r;
			}
		}
	}
	
	public Site load(int id) throws GlideinException {
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			return dao.load(id);
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to load site",de);
		}
	}
}