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
package edu.usc.glidein.db;

import java.util.Date;
import java.util.List;

import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.GlideinState;

public interface GlideinDAO {
	public int create(Glidein glidein) throws DatabaseException;
	public Glidein load(int glideinId) throws DatabaseException;
	public void delete(int glideinId) throws DatabaseException;
	public void updateState(int glideinId, GlideinState state, String shortMessage, String longMessage, Date time)  throws DatabaseException;
	public List<Glidein> list(boolean longFormat, String user, boolean allUsers) throws DatabaseException;
	public int[] listIds() throws DatabaseException;
	public int[] listTerminated() throws DatabaseException;
	public void incrementSubmits(int glideinId) throws DatabaseException;
}
