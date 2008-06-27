/*
 *  Copyright 2007-2008 University Of Southern California
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
package edu.usc.glidein.db;

import javax.naming.Context;
import javax.naming.InitialContext;

// TODO: Add history tracking

public abstract class Database
{
	public static Database getDatabase() throws DatabaseException
	{
		String location = "java:comp/env/glidein/Database";
		try {
			Context initialContext = new InitialContext();
	    	return (Database)initialContext.lookup(location);
		} catch (Exception e) {
			throw new DatabaseException("Unable to load database: "+location,e);
		}
	}
	
	public abstract SiteDAO getSiteDAO();
	public abstract GlideinDAO getGlideinDAO();
}