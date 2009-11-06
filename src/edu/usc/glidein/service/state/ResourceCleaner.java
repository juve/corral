/*
 *  Copyright 2009 University Of Southern California
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
package edu.usc.glidein.service.state;

import java.util.Date;

import org.apache.log4j.Logger;

import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.config.Initializable;
import edu.usc.corral.config.Registry;
import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.GlideinDAO;

public class ResourceCleaner implements Initializable {
	
	private long interval = 30L; // 30 mins
	private boolean initialized = false;
	
	public void initialize() throws Exception {
		if (initialized)
			return;
			
		// Start thread to destroy finished glideins
		Thread thread = new Thread(new CleanupThread(), "CleanupThread");
		thread.setDaemon(false);
		thread.setPriority(Thread.NORM_PRIORITY);
		thread.start();
		
		initialized = true;
	}
	
	public void setInterval(long interval) {
		this.interval = interval;
	}
	
	public long getInterval() {
		return this.interval;
	}
	
	private class CleanupThread implements Runnable {
		
		private Logger logger = Logger.getLogger(CleanupThread.class);
		
		public void run() {
			
			logger.debug("CleanupThread starting...");
			
			// Wake up periodically and destroy any FINISHED and FAILED glideins
			while (true) {
				try {
					Thread.sleep(interval*60L*1000L);
				} catch(InterruptedException ie) {
					logger.warn("CleanupThread interrupted");
					continue;
				}
				
				logger.debug("Cleaning up glideins...");
				
				try {
					Database db = Database.getDatabase();
					GlideinDAO dao = db.getGlideinDAO();
					int[] ids = dao.listTerminated();
					for (int id : ids) {
						logger.debug("Cleaning glidein "+id);
						EventQueue queue = EventQueue.getInstance();
						GlideinEvent delete = new GlideinEvent(
								GlideinEventCode.DELETE, 
								new Date(), id);
						queue.add(delete);
					}
				} catch (Exception e) {
					logger.warn("Unable to clean up glideins", e);
				}
			}
		}
	}
	
	public static ResourceCleaner getInstance() throws ConfigurationException {
		return (ResourceCleaner)new Registry().lookup("corral/ResourceCleaner");
	}
}