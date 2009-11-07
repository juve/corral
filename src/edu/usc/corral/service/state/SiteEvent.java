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
package edu.usc.corral.service.state;

import java.util.Date;

import org.apache.log4j.Logger;

import edu.usc.corral.api.GlideinException;
import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.service.SiteResource;
import edu.usc.corral.service.SiteResourceHome;

public class SiteEvent extends Event {
	private Logger logger;
	
	public SiteEvent(EventCode code, Date time, int id) {
		super(code,time,id);
		this.logger = Logger.getLogger(SiteEvent.class);
	}

	public void run() {
		try {
			SiteResourceHome home = SiteResourceHome.getInstance();
			SiteResource resource = (SiteResource)home.find(getId());
			resource.handleEvent(this);
		} catch (ConfigurationException ne) {
			logger.error("Unable to get SiteResourceHome",ne);
		} catch (GlideinException re) {
			logger.error("Unable to find SiteResource: "+getId(),re);
		} catch (Throwable t) {
			logger.error("Unable to process event "+getCode()+" for resource "+getId(),t);
		}
	}
}
