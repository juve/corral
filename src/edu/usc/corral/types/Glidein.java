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
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

public class Glidein extends CreateGlideinRequest implements Response {
	private @Attribute Integer id;
	private @Element String siteName;
	private @Element GlideinState state;
	private @Element String shortMessage;
	private @Element(required=false) String longMessage;
	private @Element Date created;
	private @Element Date lastUpdate;
	private @Element Integer submits;
	private @Element String subject;
	private @Element String localUsername;
	
	public Glidein() { }
	
	public Glidein(CreateGlideinRequest r) {
		this.siteId = r.siteId;
		this.condorHost = r.condorHost;
		this.count = r.count;
		this.hostCount = r.hostCount;
		this.wallTime = r.wallTime;
		this.numCpus = r.numCpus;
		this.condorConfig = r.condorConfig;
		this.gcbBroker = r.gcbBroker;
		this.highport = r.highport;
		this.lowport = r.lowport;
		this.ccbAddress = r.ccbAddress;
		this.idleTime = r.idleTime;
		this.condorDebug = r.condorDebug;
		this.resubmit = r.resubmit;
		this.until = r.until;
		this.resubmits = r.resubmits;
		this.rsl = r.rsl;
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public GlideinState getState() {
		return state;
	}
	public void setState(GlideinState state) {
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
	public Integer getSubmits() {
		return submits;
	}
	public void setSubmits(Integer submits) {
		this.submits = submits;
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
	public String getSiteName() {
		return siteName;
	}
	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}
	
	public void print() {
		print(System.out);
	}
	
	public void print(PrintStream out) {
		out.printf("id = %d\n", getId());
		out.printf("site = %d\n", getSiteId());
		out.printf("site-name = %s\n", getSiteName());
		out.printf("condor-host = %s\n", getCondorHost());
		out.printf("count = %d\n", getCount());
		out.printf("host-count = %d\n", getHostCount());
		out.printf("num-cpus = %d\n", getNumCpus());
		out.printf("wall-time = %d\n", getWallTime());
		out.printf("idle-time = %d\n", getIdleTime());
		
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
		out.printf("condor-debug = %s\n", getCondorDebug());
		out.printf("gcb-broker = %s\n", getGcbBroker());
		out.printf("ccb-address = %s\n", getCcbAddress());
		out.printf("submits = %s\n", getSubmits());
		out.printf("resubmit = %s\n", getResubmit());
		out.printf("resubmits = %s\n", getResubmits());
		
		Date until = getUntil();
		if (until == null) {
			out.printf("until = null\n");
		} else {
			out.printf("until = %tc\n", until);
		}
		out.printf("rsl = %s\n", getRsl());
		out.printf("highport = %d\n", getHighport());
		out.printf("lowport = %d\n", getLowport());
		out.printf("subject = %s\n", getSubject());
		out.printf("local-username = %s\n", getLocalUsername());
	}
	
	public static void main(String[] args) throws Exception {
		Glidein req = new Glidein();
		req.setId(1000);
		req.setSiteName("foosite");
		req.setState(GlideinState.NEW);
		req.setShortMessage("short");
		req.setLongMessage("long");
		req.setCreated(new Date());
		req.setLastUpdate(new Date());
		req.setResubmits(0);
		req.setSubject("/gideon/juve");
		req.setLocalUsername("juve");
		
		req.setCount(1);
		req.setHostCount(1);
		req.setCondorHost("juve.isi.edu");
		req.setNumCpus(1);
		req.setSiteId(1);
		req.setWallTime(60);
		
		req.setHighport(100);
		req.setLowport(10);
		
		Style style = new HyphenStyle();
		Format format = new Format(style);
		Serializer serializer = new Persister(format);
		serializer.write(req, System.out);
	}
}
