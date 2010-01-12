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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import edu.usc.corral.service.*;
import edu.usc.corral.types.CreateSiteRequest;
import edu.usc.corral.types.CreateGlideinRequest;
import edu.usc.corral.types.ErrorResponse;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.RemoveRequest;
import edu.usc.corral.types.Request;
import edu.usc.corral.types.Response;
import edu.usc.corral.types.SubmitRequest;

public class CorralServlet extends HttpServlet {
	private static final long serialVersionUID = -5185068391223120393L;
	
	private Logger logger = Logger.getLogger(CorralServlet.class);
	
	private Map<String, Class<? extends Request>> requests;
	
	public void init(ServletConfig config) throws ServletException {
		requests = new HashMap<String, Class<? extends Request>>();
		requests.put("/site/list", ListRequest.class);
		requests.put("/site/create", CreateSiteRequest.class);
		requests.put("/site/submit", SubmitRequest.class);
		requests.put("/site/remove", RemoveRequest.class);
		requests.put("/site/get", GetRequest.class);
		requests.put("/glidein/list", ListRequest.class);
		requests.put("/glidein/create", CreateGlideinRequest.class);
		requests.put("/glidein/submit", SubmitRequest.class);
		requests.put("/glidein/remove", RemoveRequest.class);
		requests.put("/glidein/get", GetRequest.class);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		
		String path = req.getPathInfo();
		InputStream is = req.getInputStream();
		OutputStream os = resp.getOutputStream();
		
		String subject = (String)req.getAttribute(GSIAttributes.GSI_USER_DN);
		String username = (String)req.getAttribute(GSIAttributes.GSI_AUTH_USERNAME);
		
		// Authorize client
		if (username == null) {
			handleError("User "+subject+" not in grid-mapfile", null, resp);
			return;
		}
		
		// Get request class
		Class<? extends Request> reqclass = requests.get(path);
		if (reqclass == null) {
			handleError("Invalid path: "+path, null, resp);
		}
		
		// Get operation name
		String operation = path.substring(path.lastIndexOf('/')+1);
		
		Service svc = null;
		if (path.startsWith("/glidein")) {
			svc = new GlideinService();
		} else if (path.startsWith("/site")) {
			svc = new SiteService();
		} else {
			handleError("Invalid path: "+path, null, resp);
		}
		svc.setSubject(subject);
		svc.setUsername(username);
		
		// Handle request
		try {
			Serializer ser = new Persister();
			Request request = ser.read(reqclass, is);
			Response response = svc.invoke(operation, request);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("text/xml");
			ser.write(response, os);
		} catch (Exception e) {
			handleError(e.getMessage(), e, resp);
		}
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		doPost(req,resp);
	}
	
	private void handleError(String message, Exception ex, HttpServletResponse resp) {
		logger.error(message, ex);
		Serializer ser = new Persister();
		try {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("text/xml");
			ser.write(new ErrorResponse(message, ex), resp.getOutputStream());
		} catch (Exception f) {
			logger.error("Unable to notify user of error", f);
		}
	}
}
