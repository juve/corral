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
package edu.usc.corral.server;

import java.io.File;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import edu.usc.corral.config.ConfigReader;
import edu.usc.corral.config.ServerConfiguration;

public class Server {
	
	public static void main(String[] args) throws Exception {
		String homeEnv = System.getenv("CORRAL_HOME");
		if (homeEnv == null) {
			System.out.println("Please set CORRAL_HOME");
			System.exit(1);
		}
		
		File homeDir = new File(homeEnv);
		if (!homeDir.isDirectory()) {
			System.out.println("CORRAL_HOME is not a directory: "+homeDir.getPath());
			System.exit(1);
		}
		
		File configFile = new File(homeDir,"etc/config.xml");
		if (!configFile.isFile()) {
			System.out.println("Config file is missing: "+configFile.getPath());
			System.exit(1);
		}
		
		// Configure logging
		if (System.getProperty("log4j.configuration") == null)
			System.setProperty("log4j.configuration", "file://"+homeEnv+"/etc/server-log4j.properties");
		System.setProperty("org.mortbay.log.class", Log4jLog.class.getName());
		
		ConfigReader.loadConfig(configFile);
		
		ServerConfiguration config = ServerConfiguration.getInstance();
		Connector connector = new GSISocketConnector(config);
		
		connector.setHost(null);
		
		org.mortbay.jetty.Server server = new org.mortbay.jetty.Server();
		server.setConnectors(new Connector[] { connector });
		server.setSendServerVersion(false);
		server.setSendDateHeader(false);
		
		
		Context root = new Context(server,"/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new CorralServlet()),"/*");
		
		server.start();
		server.join();
	}

}
