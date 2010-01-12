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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.usc.corral.api.GlideinException;
import edu.usc.corral.types.Request;
import edu.usc.corral.types.Response;

public class Service {
	private String username;
	private String subject;
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public Response invoke(String operation, Request request) throws GlideinException {
		try {
			Method m = getClass().getMethod(operation, request.getClass());
			return (Response)m.invoke(this, request);
		} catch (NoSuchMethodException e) {
			throw new GlideinException("No such operation: "+operation, e);
		} catch (InvocationTargetException ite) {
			Throwable t = ite.getCause();
			if (t instanceof GlideinException) {
				throw (GlideinException)t;
			} else {
				throw new GlideinException("Unable to invoke operation",t);
			}
		} catch (IllegalAccessException iae) {
			throw new GlideinException("Unable to access method "+operation, iae);
		}
	}
}
