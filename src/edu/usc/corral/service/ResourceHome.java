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
package edu.usc.corral.service;

import java.util.HashMap;
import java.util.Map;

import edu.usc.corral.api.GlideinException;

// TODO Make sure this is synchronized properly
// TODO Clean up idle resources
public abstract class ResourceHome {
	
	private Map<Integer,Resource> resources;
	
	public ResourceHome() {
		resources = new HashMap<Integer,Resource>();
	}

	public synchronized <T extends Resource> void add(int id, T resource) {
		resources.put(id, resource);
	}
	
	public synchronized void remove(int id) {
		resources.remove(id);
	}
	
	public synchronized boolean contains(int id) {
		return resources.containsKey(id);
	}
	
	public synchronized Resource get(int id) {
		return resources.get(id);
	}
	
	abstract public Resource find(int id) throws GlideinException;
}
