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

import org.apache.log4j.Logger;

import edu.usc.corral.api.GlideinException;
import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.config.Initializable;
import edu.usc.corral.config.Registry;
import edu.usc.corral.db.Database;
import edu.usc.corral.db.DatabaseException;
import edu.usc.corral.db.GlideinDAO;
import edu.usc.corral.nl.NetLogger;
import edu.usc.corral.nl.NetLoggerEvent;
import edu.usc.corral.nl.NetLoggerException;
import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteState;

public class GlideinResourceHome extends ResourceHome implements Initializable {
	private final Logger logger = Logger.getLogger(GlideinResourceHome.class);
	
	public void initialize() throws Exception {
		logger.info("Recovering glideins...");
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			int[] ids = dao.listIds();
			for (int id : ids) {
				GlideinResource resource = find(id);
				resource.recoverState();
			}
		} catch (DatabaseException de) {
			throw new Exception(
					"Unable to get glideins from database",de);
		}
	}
	
	public static GlideinResourceHome getInstance() throws ConfigurationException {
		return (GlideinResourceHome)new Registry().lookup("corral/GlideinResourceHome");
	}
	
	public int create(Glidein glidein) throws GlideinException {
		
		logger.info("Creating glidein for site "+glidein.getSiteId());
		
		// Get site or fail
		SiteResource siteResource = getSiteResource(glidein.getSiteId());
		Site site = siteResource.getSite();
		
		// Synchronize on the site resource to prevent state changes while
		// we are checking it and saving the glidein
		synchronized (siteResource) {
			
			// Check to make sure the site is in an 
			// appropriate state for creating a glidein
			SiteState siteState = site.getState();
			if (SiteState.FAILED.equals(siteState) || 
				SiteState.EXITING.equals(siteState) ||
				SiteState.REMOVING.equals(siteState)) {
				
				throw new GlideinException(
						"Site cannot be in "+siteState+
						" when creating a glidein");
			}
			
			// Set the name
			glidein.setSiteName(site.getName());
			
			// Save in the database
			try {
				Database db = Database.getDatabase();
				GlideinDAO dao = db.getGlideinDAO();
				dao.create(glidein);
			} catch (DatabaseException dbe) {
				throw new GlideinException("Unable to create glidein",dbe);
			}
		}
		
		// Log it in the netlogger log
		try {
			NetLoggerEvent event = new NetLoggerEvent("glidein.new");
			event.setTimeStamp(glidein.getCreated());
			event.put("glidein.id", glidein.getId());
			event.put("site.id", site.getId());
			event.put("condor.host", glidein.getCondorHost());
			event.put("condor.debug", glidein.getCondorDebug());
			event.put("count", glidein.getCount());
			event.put("host_count", glidein.getHostCount());
			event.put("wall_time", glidein.getWallTime());
			event.put("num_cpus", glidein.getNumCpus());
			event.put("gcb_broker", glidein.getGcbBroker());
			event.put("idle_time", glidein.getIdleTime());
			event.put("resubmit", glidein.getResubmit());
			event.put("until", glidein.getUntil());
			event.put("resubmits", glidein.getResubmits());
			event.put("rsl", glidein.getRsl());
			event.put("lowport", glidein.getLowport());
			event.put("highport", glidein.getHighport());
			event.put("ccb_address", glidein.getCcbAddress());
			event.put("owner.subject", glidein.getSubject());
			event.put("owner.username", glidein.getLocalUsername());
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			logger.warn("Unable to log glidein event to NetLogger log",nle);
		}
		
		GlideinResource resource = new GlideinResource(glidein);
		this.add(glidein.getId(), resource);
		return glidein.getId();
	}
	
	private SiteResource getSiteResource(int id) throws GlideinException {
		try {
			SiteResourceHome siteHome = SiteResourceHome.getInstance();
			return siteHome.find(id);
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get SiteResourceHome",ne);
		}
	}

	public GlideinResource find(int id) throws GlideinException {
		synchronized (this) {
			if (contains(id)) {
				return (GlideinResource)get(id);
			} else {
				Glidein g = load(id);
				GlideinResource r = new GlideinResource(g);
				add(id, r);
				return r;
			}
		}
	}
	
	public Glidein load(int id) throws GlideinException {
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			return dao.load(id);
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to load glidein",de);
		}
	}
}