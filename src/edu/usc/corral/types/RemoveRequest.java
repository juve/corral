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
package edu.usc.corral.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.globus.gsi.GlobusCredential;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class RemoveRequest implements Request {
	@Attribute
	private int id;
	@Element
	private boolean force = false;
	private GlobusCredential cred;
	
	public RemoveRequest() {
	}
	
	public RemoveRequest(int id, boolean force, GlobusCredential cred) {
		this.id = id;
		this.force = force;
		this.cred = cred;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public void setForce(boolean force) {
		this.force = force;
	}
	
	public boolean isForce() {
		return force;
	}
	
	@Element(data=true,required=false)
	public String getCredential() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		cred.save(baos);
		return new String(baos.toByteArray());
	}
	
	@Element(data=true,required=false)
	public void setCredential(String cred) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(cred.getBytes());
		this.cred = new GlobusCredential(bais);
	}
	
	public GlobusCredential getGlobusCredential() {
		return this.cred;
	}
	
	public void setGlobusCredential(GlobusCredential cred) {
		this.cred = cred;
	}
}
