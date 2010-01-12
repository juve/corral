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
package edu.usc.corral.service.state;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class Event implements Runnable {
	private EventCode code;
	private Map<String,Object> properties;
	private int id;
	private Date time;
	
	public Event(EventCode code, Date time, int id) {
		this.code = code;
		this.id = id;
		this.time = time;
		properties = new HashMap<String,Object>();
	}

	public EventCode getCode() {
		return code;
	}

	public void setCode(EventCode code) {
		this.code = code;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void setTime(Date time) {
		this.time = time;
	}
	
	public Date getTime() {
		return time;
	}
	
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}
	
	public Object getProperty(String key) {
		return properties.get(key);
	}
}
