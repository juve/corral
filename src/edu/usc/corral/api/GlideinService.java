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
package edu.usc.corral.api;

import java.util.HashSet;
import java.util.Set;

import edu.usc.corral.types.CreateGlideinRequest;
import edu.usc.corral.types.CreateGlideinResponse;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.GlideinStateChange;
import edu.usc.corral.types.ListGlideinsResponse;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.RemoveRequest;
import edu.usc.corral.types.SubmitRequest;
import edu.usc.corral.types.VoidResponse;

public class GlideinService {	
	
	private Client client = null;
	private Set<GlideinListener> listeners = new HashSet<GlideinListener>();
	
	public GlideinService(String host, int port) {
		client = new Client(host, port);
	}
	
	public CreateGlideinResponse create(CreateGlideinRequest req) throws GlideinException {
		return client.doPost("/glidein/create", CreateGlideinResponse.class, req);
	}
	
	public ListGlideinsResponse listGlideins(ListRequest req) throws GlideinException {
		return client.doPost("/glidein/list", ListGlideinsResponse.class, req);
	}
	
	public Glidein getGlidein(GetRequest req) throws GlideinException {
		return client.doPost("/glidein/get", Glidein.class, req);
	}
	
	public void submit(SubmitRequest req) throws GlideinException {
		client.doPost("/glidein/submit", VoidResponse.class, req);
	}
	
	public void remove(RemoveRequest req) throws GlideinException {
		client.doPost("/glidein/remove", VoidResponse.class, req);
	}

	public synchronized void addListener(GlideinListener listener) throws GlideinException {
		if (listeners.size() == 0) {
			subscribe();
		}
		listeners.add(listener);
	}
	
	public synchronized void removeListener(GlideinListener listener) throws GlideinException {
		listeners.remove(listener);
		if (listeners.size() == 0) {
			unsubscribe();
		}
	}
	
	private void subscribe() throws GlideinException {
		// TODO Implement subscribe
	}
	
	private void unsubscribe() throws GlideinException {
		// TODO Implement unsubscribe
	}
	
	public void deliver(GlideinStateChange stateChange) {
		for (GlideinListener listener : listeners) {
			listener.stateChanged(stateChange);
		}
	}
}
