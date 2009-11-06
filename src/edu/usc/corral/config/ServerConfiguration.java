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
package edu.usc.corral.config;

import java.io.IOException;

import org.globus.security.gridmap.GridMap;

public class ServerConfiguration extends GlobusConfiguration implements Initializable {
	private String host = null;
	private int port = 8443;
	private int backlog = 5;
	private GridMap gridMap;
	private String gridMapfile = "/etc/grid-security/grid-mapfile";
	private String certificate = "/etc/grid-security/hostcert.pem";
	private String key = "/etc/grid-security/hostkey.pem";
	private String proxy = null;
	private boolean anonymousAllowed = false;
	private String cacertdir = "/etc/grid-security/certificates";
	
	
	public String getProxy() {
		return proxy;
	}
	public void setProxy(String proxy) {
		this.proxy = proxy;
	}
	public String getCertificate() {
		return certificate;
	}
	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getCacertdir() {
		return cacertdir;
	}
	public void setCacertdir(String cacertdir) {
		this.cacertdir = cacertdir;
	}
	public boolean isAnonymousAllowed() {
		return anonymousAllowed;
	}
	public void setAnonymousAllowed(boolean anonymousAllowed) {
		this.anonymousAllowed = anonymousAllowed;
	}
	public void setGridMapfile(String gridMapfile) {
		this.gridMapfile = gridMapfile;
	}
	public String getGridMapfile() {
		return gridMapfile;
	}
	public GridMap getGridMap() {
		return this.gridMap;
	}
	public void setGridMap(GridMap gridMap) {
		this.gridMap = gridMap;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getHost() {
		return host;
	}
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}
	public int getBacklog() {
		return backlog;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	
	public void initialize() throws Exception {
		try {
			gridMap = new GridMap();
			gridMap.load(getGridMapfile());
		} catch (IOException e) {
			throw new ConfigurationException("Unable to load gridmap file", e);
		}
	}
	
	public static ServerConfiguration getInstance() throws ConfigurationException {
		Registry registry = new Registry();
		return (ServerConfiguration)registry.lookup("corral/ServerConfiguration");
	}
}
