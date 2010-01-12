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

import org.globus.gsi.GSIConstants;

public class ClientConfiguration extends GlobusConfiguration {
	private Integer delegation = null;
	private int lifetime = 0;
	
	public ClientConfiguration() {
	}
	
	public Integer getDelegation() {
		return delegation;
	}
	public void setDelegation(Integer delegation) {
		this.delegation = delegation;
	}
	public void setDelegation(String deleg) {
		if (deleg == null || "none".equalsIgnoreCase(deleg)) {
			delegation = null;
		} else if ("limited".equalsIgnoreCase(deleg)) {
			delegation = GSIConstants.DELEGATION_TYPE_LIMITED;
		} else if ("full".equalsIgnoreCase(deleg)) {
			delegation = GSIConstants.DELEGATION_TYPE_FULL;
		} else {
			throw new IllegalArgumentException(
					"Invalid delegation type: '"+deleg+"' Delegation type must be one of: 'none', 'full' or 'limited'");
		}
	}
	public int getLifetime() {
		return lifetime;
	}
	public void setLifetime(int lifetime) {
		this.lifetime = lifetime;
	}
	
	public static ClientConfiguration getDefault() {
		ClientConfiguration config = new ClientConfiguration();
		config.setAuthorization("host");
		return config;
	}
}
