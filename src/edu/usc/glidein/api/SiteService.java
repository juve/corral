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
package edu.usc.glidein.api;

import java.util.HashSet;
import java.util.Set;

import edu.usc.corral.types.CreateSiteRequest;
import edu.usc.corral.types.CreateSiteResponse;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.ListSitesResponse;
import edu.usc.corral.types.RemoveRequest;
import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteStateChange;
import edu.usc.corral.types.SubmitRequest;
import edu.usc.corral.types.VoidResponse;

public class SiteService {
	private Client client;
	private Set<SiteListener> listeners = new HashSet<SiteListener>();
	
	public SiteService(String host, int port) {
		this.client = new Client(host, port);
	}
	
	public CreateSiteResponse create(CreateSiteRequest req) throws GlideinException {
		return client.doPost("/site/create", CreateSiteResponse.class, req);
	}
	
	public ListSitesResponse listSites(ListRequest req) throws GlideinException {
		return client.doPost("/site/list", ListSitesResponse.class, req);
	}
	
	public Site getSite(GetRequest req) throws GlideinException {
		return client.doPost("/site/get", Site.class, req);
	}
	
	public void submit(SubmitRequest req) throws GlideinException {
		client.doPost("/site/submit", VoidResponse.class, req);
	}
	
	public void remove(RemoveRequest req) throws GlideinException {
		client.doPost("/site/remove", VoidResponse.class, req);
	}
	
	public synchronized void addListener(SiteListener listener) throws GlideinException {
		if (listeners.size() == 0)
			subscribe();
		listeners.add(listener);
	}
	
	public synchronized void removeListener(SiteListener listener) throws GlideinException {
		listeners.remove(listener);
		if (listeners.size() == 0)
			unsubscribe();
	}
	
	private void subscribe() throws GlideinException {
		// TODO Implement subscribe
	}
	
	private void unsubscribe() throws GlideinException {
		// TODO Implement unsubscribe
	}
	
	public void deliver(SiteStateChange stateChange) {
		for (SiteListener listener : listeners) {
			listener.stateChanged(stateChange);
		}
	}
}
