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
package edu.usc.corral.nl;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Stores information about NetLogger events.
 */
public class NetLoggerEvent {
	private String event;
	private Date ts;
	private HashMap<String,Object> map;
	
	public NetLoggerEvent(String type) {
		this.ts = new Date();
		this.map = new HashMap<String,Object>();
		this.event = type;
	}
	
	public String getEvent() {
		return this.event;
	}
	
	public void setEvent(String event) {
		this.event = event;
	}
	
	public Date getTimeStamp() {
		return this.ts;
	}
	
	public void setTimeStamp(Date ts) {
		this.ts = ts;
	}
	
	public void put(String key, Object value) {
		map.put(key, value);
	}
	
	public Object get(String key) {
		return map.get(key);
	}
	
	public Set<String> keySet() {
		return map.keySet();
	}
}