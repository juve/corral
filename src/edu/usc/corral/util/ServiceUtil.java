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
package edu.usc.corral.util;

import java.net.InetAddress;

import org.apache.log4j.Logger;

public class ServiceUtil {
	private static Logger logger = Logger.getLogger(ServiceUtil.class);
	
	public static String getServiceHost() {
		// Set the default condor host
		try {
			InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostName();
		}  catch (Exception e) {
			logger.warn("Unable to get service host",e);
			return null;
		}
	}
}
