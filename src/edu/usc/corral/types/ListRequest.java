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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class ListRequest implements Request {
	private @Attribute(required=false) boolean longFormat = false;
	private @Attribute(required=false) String user;
	private @Attribute(required=false) boolean allUsers = false;
	
	public boolean isLongFormat() {
		return longFormat;
	}
	public void setLongFormat(boolean longFormat) {
		this.longFormat = longFormat;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public boolean isAllUsers() {
		return allUsers;
	}
	public void setAllUsers(boolean allUsers) {
		this.allUsers = allUsers;
	}
}
