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
package edu.usc.corral.server;

public class GSIAttributes {
	public static final String GSI_ANONYMOUS = "org.globus.gsi.anonymous";
    public static final String GSI_AUTHORIZATION = "org.globus.gsi.authorization";
    public static final String GSI_AUTH_USERNAME = "org.globus.gsi.authorized.user.name";
    
    public static final String GSI_CONTEXT = "org.globus.gsi.context";
    public static final String GSI_USER_DN = "org.globus.gsi.authorized.user.dn";
    public static final String GSI_CREDENTIALS = "org.globus.gsi.credentials";
    public static final String GSI_DELEGATED_CREDENTIALS = "org.globus.gsi.credentials.delegated";
}
