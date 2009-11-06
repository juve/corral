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

import java.util.Date;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class CreateGlideinRequest implements Request {
	protected @Element Integer siteId;
	protected @Element String condorHost;
	protected @Element Integer count;
	protected @Element Integer hostCount;
	protected @Element Integer wallTime;
	protected @Element Integer numCpus;
	protected @Element(required=false,data=true) String condorConfig;
	protected @Element(required=false) String gcbBroker;
	protected @Element(required=false) Integer highport;
	protected @Element(required=false) Integer lowport;
	protected @Element(required=false) String ccbAddress;
	protected @Element(required=false) Integer idleTime;
	protected @Element(required=false) String condorDebug;
	protected @Element(required=false) Boolean resubmit;
	protected @Element(required=false) Date until;
	protected @Element(required=false) Integer resubmits;
	
	private @Element(required=false) String rsl;
	
	public Integer getSiteId() {
		return siteId;
	}
	public void setSiteId(Integer siteId) {
		this.siteId = siteId;
	}
	public String getCondorHost() {
		return condorHost;
	}
	public void setCondorHost(String condorHost) {
		this.condorHost = condorHost;
	}
	public Integer getCount() {
		return count;
	}
	public void setCount(Integer count) {
		this.count = count;
	}
	public Integer getHostCount() {
		return hostCount;
	}
	public void setHostCount(Integer hostCount) {
		this.hostCount = hostCount;
	}
	public Integer getWallTime() {
		return wallTime;
	}
	public void setWallTime(Integer wallTime) {
		this.wallTime = wallTime;
	}
	public Integer getNumCpus() {
		return numCpus;
	}
	public void setNumCpus(Integer numCpus) {
		this.numCpus = numCpus;
	}
	public String getCondorConfig() {
		return condorConfig;
	}
	public void setCondorConfig(String condorConfig) {
		this.condorConfig = condorConfig;
	}
	public String getGcbBroker() {
		return gcbBroker;
	}
	public void setGcbBroker(String gcbBroker) {
		this.gcbBroker = gcbBroker;
	}
	public Integer getHighport() {
		return highport;
	}
	public void setHighport(Integer highport) {
		this.highport = highport;
	}
	public Integer getLowport() {
		return lowport;
	}
	public void setLowport(Integer lowport) {
		this.lowport = lowport;
	}
	public String getCcbAddress() {
		return ccbAddress;
	}
	public void setCcbAddress(String ccbAddress) {
		this.ccbAddress = ccbAddress;
	}
	public Integer getIdleTime() {
		return idleTime;
	}
	public void setIdleTime(Integer idleTime) {
		this.idleTime = idleTime;
	}
	public String getCondorDebug() {
		return condorDebug;
	}
	public void setCondorDebug(String condorDebug) {
		this.condorDebug = condorDebug;
	}
	public Date getUntil() {
		return until;
	}
	public void setUntil(Date until) {
		this.until = until;
	}
	public Integer getResubmits() {
		return resubmits;
	}
	public void setResubmits(Integer resubmits) {
		this.resubmits = resubmits;
	}
	public String getRsl() {
		return rsl;
	}
	public void setRsl(String rsl) {
		this.rsl = rsl;
	}
	public Boolean getResubmit() {
		return resubmit;
	}
	public void setResubmit(Boolean resubmit) {
		this.resubmit = resubmit;
	}
}
