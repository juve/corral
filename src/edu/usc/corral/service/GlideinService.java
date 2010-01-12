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
package edu.usc.corral.service;

import java.util.Date;
import java.util.List;

import edu.usc.corral.api.GlideinException;
import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.db.Database;
import edu.usc.corral.db.DatabaseException;
import edu.usc.corral.db.GlideinDAO;
import edu.usc.corral.types.CreateGlideinRequest;
import edu.usc.corral.types.CreateGlideinResponse;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.GlideinState;
import edu.usc.corral.types.ListGlideinsResponse;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.RemoveRequest;
import edu.usc.corral.types.SubmitRequest;
import edu.usc.corral.types.VoidResponse;

public class GlideinService extends Service {
	
	public CreateGlideinResponse create(CreateGlideinRequest req) throws GlideinException {
		// Create new glidein
		Glidein glidein = new Glidein(req);
		
		// Set owner
		glidein.setSubject(getSubject());
		glidein.setLocalUsername(getUsername());
		
		// Initialize glidein
		glidein.setState(GlideinState.NEW);
		glidein.setShortMessage("Created");
		Date time = new Date();
		glidein.setLastUpdate(time);
		glidein.setCreated(time);
		glidein.setSubmits(0);
		
		// Validate glidein
		
		// Wall time must be >= 2 minutes because when we submit the glidein
		// we are going to subtract 1 minute to allow 1 minute for the glidein
		// to shut itself down before the local scheduler kills it
		if (glidein.getWallTime() < 2) {
			throw new GlideinException("Wall time must be >= 2 minutes");
		}
		
		// Create glidein
		try {
			GlideinResourceHome home = GlideinResourceHome.getInstance();
			return new CreateGlideinResponse(home.create(glidein));
		} catch (ConfigurationException e) {
			throw new GlideinException("Unable to get GlideinResourceHome", e);
		}
	}
	
	public ListGlideinsResponse list(ListRequest req) throws GlideinException {
		try {
			if(req.getUser()==null && !req.isAllUsers()) {
				req.setUser(getUsername());
			}
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			List<Glidein> glideins = dao.list(req.isLongFormat(), req.getUser(), req.isAllUsers());
			return new ListGlideinsResponse(glideins);
		} catch (DatabaseException de) {
			throw new GlideinException("Unable to list glideins",de);
		}
	}
	
	public Glidein get(GetRequest req) throws GlideinException {
		return getResource(req.getId()).getGlidein();
	}
	
	public VoidResponse submit(SubmitRequest req) throws GlideinException {
		GlideinResource r = getResource(req.getId());
		if (!r.authorized(getSubject())) {
			throw new GlideinException("Not authorized");
		}
		r.submit(req.getGlobusCredential());
		return new VoidResponse();
	}
	
	public VoidResponse remove(RemoveRequest req) throws GlideinException {
		GlideinResource r = getResource(req.getId());
		if (!r.authorized(getSubject())) {
			throw new GlideinException("Not authorized");
		}
		r.remove(req.isForce());
		return new VoidResponse();
	}
	
	private GlideinResource getResource(int id) throws GlideinException {
		try {
			GlideinResourceHome home = GlideinResourceHome.getInstance();
			return home.find(id);
		} catch (Exception e) {
			throw new GlideinException("Unable to find glidein resource", e);
		}
	}
}
