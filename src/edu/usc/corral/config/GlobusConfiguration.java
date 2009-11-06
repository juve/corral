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
import org.globus.gsi.gssapi.auth.Authorization;
import org.globus.gsi.gssapi.auth.HostAuthorization;
import org.globus.gsi.gssapi.auth.IdentityAuthorization;
import org.globus.gsi.gssapi.auth.NoAuthorization;
import org.globus.gsi.gssapi.auth.SelfAuthorization;
import org.globus.gsi.gssapi.net.GssSocket;

public class GlobusConfiguration {
	private boolean requestEncryption = true;
	private boolean requestAnonymous = false;
	private Integer gssMode = GSIConstants.MODE_SSL;
	private int wrapMode = GssSocket.SSL_MODE;
	private Authorization authz = NoAuthorization.getInstance();
	private boolean rejectLimitedProxy = false;
	
	public Authorization getAuthorization() {
		return authz;
	}
	public void setAuthorization(Authorization authz) {
		this.authz = authz;
	}
	public void setAuthorization(String authz) {
		if (authz==null || "host".equalsIgnoreCase(authz)) {
			this.authz = HostAuthorization.getInstance();
		} else if ("self".equalsIgnoreCase(authz)) {
			this.authz = SelfAuthorization.getInstance();
		} else if ("none".equalsIgnoreCase(authz)){
			this.authz = NoAuthorization.getInstance();
		} else {
			this.authz = new IdentityAuthorization(authz);
		}
	}
	public int getWrapMode() {
		return wrapMode;
	}
	public void setWrapMode(int wrapMode) {
		this.wrapMode = wrapMode;
	}
	public void setWrapMode(String wrapMode) {
		if (wrapMode == null || "ssl".equalsIgnoreCase(wrapMode)) {
			this.wrapMode = GssSocket.SSL_MODE;
		} else if ("gsi".equalsIgnoreCase(wrapMode)) {
			this.wrapMode = GssSocket.GSI_MODE;
		} else {
			throw new IllegalArgumentException(
					"Invalid wrap mode: '"+wrapMode+"'. Wrap mode must be one of 'ssl' or 'gsi'.");
		}
	}
	public Integer getGssMode() {
		return gssMode;
	}
	public void setGssMode(Integer gssMode) {
		this.gssMode = gssMode;
	}
	public void setGssMode(String gssMode) {
		if (gssMode == null || "ssl".equalsIgnoreCase(gssMode)) {
			this.gssMode = GSIConstants.MODE_SSL;
		} else if ("gsi".equalsIgnoreCase(gssMode)) {
			this.gssMode = GSIConstants.MODE_GSI;
		} else {
			throw new IllegalArgumentException(
					"Invalid gss mode: '"+gssMode+"'. GSS mode must be one of 'ssl' or 'gsi'.");
		}
	}
	
	public boolean isRequestEncryption() {
		return requestEncryption;
	}
	public void setRequestEncryption(boolean requestEncryption) {
		this.requestEncryption = requestEncryption;
	}
	public boolean isRequestAnonymous() {
		return requestAnonymous;
	}
	public void setRequestAnonymous(boolean requestAnonymous) {
		this.requestAnonymous = requestAnonymous;
	}
	public boolean isRejectLimitedProxy() {
		return rejectLimitedProxy;
	}
	public void setRejectLimitedProxy(boolean rejectLimitedProxy) {
		this.rejectLimitedProxy = rejectLimitedProxy;
	}
}
