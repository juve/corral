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
package edu.usc.corral.types;

import java.io.PrintStream;
import java.util.Date;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class Site extends CreateSiteRequest implements Response {
	private @Attribute Integer id;
	private @Element SiteState state;
	private @Element String shortMessage;
	private @Element(required=false) String longMessage;
	private @Element Date created;
	private @Element Date lastUpdate;
	private @Element String subject;
	private @Element String localUsername;
	
	public Site() {
	}
	
	public Site(CreateSiteRequest r) {
		this.name = r.name;
		this.installPath = r.installPath;
		this.localPath = r.localPath;
		this.stagingService = r.stagingService;
		this.glideinService = r.glideinService;
		this.condorVersion = r.condorVersion;
		this.condorPackage = r.condorPackage;
		this.environment = r.environment;
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public SiteState getState() {
		return state;
	}
	public void setState(SiteState state) {
		this.state = state;
	}
	public String getShortMessage() {
		return shortMessage;
	}
	public void setShortMessage(String shortMessage) {
		this.shortMessage = shortMessage;
	}
	public String getLongMessage() {
		return longMessage;
	}
	public void setLongMessage(String longMessage) {
		this.longMessage = longMessage;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getLocalUsername() {
		return localUsername;
	}
	public void setLocalUsername(String localUsername) {
		this.localUsername = localUsername;
	}
	
	public void print() {
		print(System.out);
	}
	
	public void print(PrintStream out) {
		out.printf("id = %s\n", getId());
		out.printf("site-name = %s\n", getName());
		
		Date created = getCreated();
		if (created == null) {
			out.printf("created = null\n");
		} else {
			out.printf("created = %tc\n", created);
		}
		
		Date lastUpdate = getLastUpdate();
		if (lastUpdate == null) {
			out.printf("last-update = null\n");
		} else {
			out.printf("last-update = %tc\n", lastUpdate);
		}
		
		out.printf("state = %s\n", getState().toString());
		out.printf("short-message = %s\n", getShortMessage());
		String longMessage = getLongMessage();
		if (longMessage == null) {
			out.printf("long-message = null\n");
		} else {
			out.printf("long-message = <<END\n");
			out.printf(longMessage);
			out.printf("\nEND\n");
		}
		out.printf("install-path = %s\n", getInstallPath());
		out.printf("local-path = %s\n", getLocalPath());
		out.printf("condor-version = %s\n", getCondorVersion());
		out.printf("condor-package = %s\n", getCondorPackage());
		
		// Environment
		out.printf("environment =");
		if (getEnvironment() != null) {
			for (EnvironmentVariable var : getEnvironment()) {
				out.printf(" %s=%s", var.getVariable(), var.getValue());
			}
		}
		out.printf("\n");
		
		// Staging Service
		ExecutionService stagingService = getStagingService();
		if (stagingService == null) {
			out.printf("staging-service = null\n");
		} else {
			out.printf("staging-service = %s %s\n",
					stagingService.getServiceType(),
					stagingService.getServiceContact());
			out.printf("staging-service-project = %s\n", stagingService.getProject());
			out.printf("staging-service-queue = %s\n", stagingService.getQueue());
		}
		
		// Glidein Service
		ExecutionService glideinService = getGlideinService();
		if (glideinService == null) {
			out.printf("glidein-service = null\n");
		} else {
			out.printf("glidein-service = %s %s\n",
					glideinService.getServiceType(),
					glideinService.getServiceContact());
			out.printf("glidein-service-project = %s\n", glideinService.getProject());
			out.printf("glidein-service-queue = %s\n", glideinService.getQueue());
		}
		
		out.printf("subject = %s\n", getSubject());
		out.printf("local-username = %s\n", getLocalUsername());
	}
}
