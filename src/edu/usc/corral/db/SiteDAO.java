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
package edu.usc.corral.db;

import java.util.Date;
import java.util.List;

import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteState;

public interface SiteDAO {
	public int create(Site site) throws DatabaseException;
	public Site load(int siteId) throws DatabaseException;
	public void delete(int siteId) throws DatabaseException;
	public void updateState(int siteId, SiteState state, String shortMessage, String longMessage, Date time) throws DatabaseException;
	public List<Site> list(boolean longFormat, String user, boolean allUsers) throws DatabaseException;
	public int[] listIds() throws DatabaseException;
	public boolean hasGlideins(int siteId) throws DatabaseException;
	public int[] getGlideinIds(int siteId) throws DatabaseException;
}
