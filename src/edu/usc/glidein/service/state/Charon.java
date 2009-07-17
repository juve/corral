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

import java.util.Calendar;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.jndi.Initializable;

import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.GlideinDAO;
import edu.usc.glidein.service.GlideinNames;
import edu.usc.glidein.service.GlideinResource;
import edu.usc.glidein.service.GlideinResourceHome;

public class Charon implements Initializable {
	
	private long interval = 30L; // 30 mins
	private boolean initialized = false;
	
	public void initialize() throws Exception {
		if (initialized)
			return;
			
		// Start thread to destroy finished glideins
		Thread thread = new Thread(new CharonThread(), "Charon");
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
	
	private class CharonThread implements Runnable {
		
		private Logger logger = Logger.getLogger(CharonThread.class);
		
		public void run() {
			
			logger.debug("Charon starting...");
			
			// Wake up periodically and destroy any FINISHED and FAILED glideins
			while (true) {
				try {
					Thread.sleep(interval*60L*1000L);
				} catch(InterruptedException ie) {
					logger.warn("Who dares interrupt Charon!");
					continue;
				}
				
				logger.debug("Charon making a trip...");
				
				try {
					GlideinResourceHome home = GlideinResourceHome.getInstance();
					Database db = Database.getDatabase();
					GlideinDAO dao = db.getGlideinDAO();
					int[] ids = dao.listTerminated();
					for (int id : ids) {
					
						// Get resource object
						ResourceKey key = new SimpleResourceKey(
								GlideinNames.RESOURCE_KEY, new Integer(id));
						GlideinResource resource = (GlideinResource)home.find(key);
						
						logger.debug("Charon transporting glidein "+id);
						
						// Queue up a delete event
						EventQueue queue = EventQueue.getInstance();
						GlideinEvent delete = new GlideinEvent(
								GlideinEventCode.DELETE, 
								Calendar.getInstance(), resource.getKey());
						queue.add(delete);
					}
				} catch (Exception e) {
					logger.warn("Unable to clean up glideins", e);
				}
			}
		}
	}
	
	public static Charon startCharon() throws NamingException {
		String location = "java:comp/env/glidein/Charon";
		Context initialContext = new InitialContext();
		return (Charon)initialContext.lookup(location);
	}
}