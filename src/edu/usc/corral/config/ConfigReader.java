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

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class ConfigReader {
	
	public static Configuration parseConfig(File config) throws ConfigurationException {
		try {
			Serializer serializer = new Persister();
			return serializer.read(Configuration.class, config);
		} catch(Exception e) {
			throw new ConfigurationException("Unable to parse configuration",e);
		}
	}
	
	public static void loadConfig(File config) throws ConfigurationException {
		Registry registry = new Registry();
		Configuration conf = parseConfig(config);
		for (Resource resource : conf.getResources()) {
			Object obj = createResource(resource);
			registry.bind(resource.getName(), obj);
		}
		registry.initialize();
	}
	
	private static Object createResource(Resource r) throws ConfigurationException {
		try {
			Class<?> clazz = Class.forName(r.getType());
			Object obj = clazz.newInstance();
			
			HashMap<String,Method> methods = new HashMap<String,Method>();
			for (Method m : clazz.getMethods()) {
				methods.put(m.getName().toLowerCase(), m);
			}
			
			for (Parameter p : r.getProperties()) {
				
				Method m = methods.get("set"+p.getName().toLowerCase());
				if (m == null) {
					throw new ConfigurationException(
							"No method to set parameter "+p.getName()+" of resource "+r.getName());
				}
				
				Class<?>[] params = m.getParameterTypes();
				if (params.length != 1) {
					throw new ConfigurationException(
							"Method "+m.getName()+" of object "+r.getName()+" must take one parameter");
				}
				
				try {
					Class<?> arg = params[0];
					if (String.class.isAssignableFrom(arg)) {
						m.invoke(obj, p.getValue());
					} else if (Double.class.isAssignableFrom(arg) || 
							   double.class.isAssignableFrom(arg)) {
						m.invoke(obj, Double.parseDouble(p.getValue()));
					} else if (Integer.class.isAssignableFrom(arg) || 
							   int.class.isAssignableFrom(arg)) {
						m.invoke(obj, Integer.parseInt(p.getValue()));
					} else if (Long.class.isAssignableFrom(arg) || 
							   long.class.isAssignableFrom(arg)) {
						m.invoke(obj, Long.parseLong(p.getValue()));
					} else if (Boolean.class.isAssignableFrom(arg) || 
							   boolean.class.isAssignableFrom(arg)) {
						m.invoke(obj, Boolean.parseBoolean(p.getValue()));
					} else {
						throw new ConfigurationException(
								"Unrecognized parameter type: "+arg.getName()+" for resource "+r.getName());
					}
				} catch (Exception e) {
					throw new ConfigurationException(
							"Unable to set parameter "+p.getName(), e);
				}
				
				
			}
			
			return obj;
		} catch(ClassNotFoundException e) {
			throw new ConfigurationException(
					"Class not found for resource "+r.getName(),e);
		} catch(IllegalAccessException e) {
			throw new ConfigurationException(
					"Unable to access resource "+r.getName(),e);
		} catch(InstantiationException e) {
			throw new ConfigurationException(
					"Unable to instantiate resource "+r.getName(),e);
		}
	}
}
