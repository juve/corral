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

import java.util.Calendar;

import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteHistory;
import edu.usc.glidein.stubs.types.SiteState;

public interface SiteDAO
{
	public int create(Site site) throws DatabaseException;
	public void store(Site site) throws DatabaseException;
	public Site load(int siteId) throws DatabaseException;
	public void delete(int siteId) throws DatabaseException;
	public void updateState(int siteId, SiteState state, String shortMessage, String longMessage) throws DatabaseException;
	public Site[] list(boolean full) throws DatabaseException;
	public boolean hasGlideins(int siteId) throws DatabaseException;
	public int[] getGlideinIds(int siteId) throws DatabaseException;
	public void insertHistory(int siteId, SiteState state, Calendar time) throws DatabaseException;
	public SiteHistory getHistory(int siteId) throws DatabaseException;
}
