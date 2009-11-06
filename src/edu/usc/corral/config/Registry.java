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

import java.util.HashMap;
import java.util.Map;

public class Registry {
	private static Map<String,Object> REGISTRY = new HashMap<String,Object>();
	
	public void bind(String name, Object value) throws ConfigurationException {
		synchronized(REGISTRY) {
			if (REGISTRY.containsKey(name)) {
				throw new ConfigurationException("Name already bound: "+name);
			}
			REGISTRY.put(name, value);
		}
	}
	
	public Object lookup(String name) throws ConfigurationException {
		synchronized(REGISTRY) {
			if (!REGISTRY.containsKey(name)) {
				throw new ConfigurationException("Name not bound: "+name);
			}
			return REGISTRY.get(name);
		}
	}
	
	public void rebind(String name, Object value) throws ConfigurationException {
		synchronized(REGISTRY) {
			if (!REGISTRY.containsKey(name)) {
				throw new ConfigurationException("Name not bound: "+name);
			}
			REGISTRY.put(name, value);
		}
	}
	
	public void unbind(String name) throws ConfigurationException {
		synchronized(REGISTRY) {
			if (!REGISTRY.containsKey(name)) {
				throw new ConfigurationException("Name not bound: "+name);
			}
			REGISTRY.remove(name);
		}
	}
	
	public void initialize() throws ConfigurationException {
		synchronized(REGISTRY) {
			for (Object o : REGISTRY.values()) {
				Class<?> clazz = o.getClass();
				if (Initializable.class.isAssignableFrom(clazz)) {
					try {
						((Initializable)o).initialize();
					} catch (Exception e) {
						throw new ConfigurationException(
								"Unable to initialize resource "+
								((Resource)o).getName(),e);
					}
				}
			}
		}
	}
}
