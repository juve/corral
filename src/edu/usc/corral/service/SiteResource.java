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

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredential;

import edu.usc.corral.api.GlideinException;
import edu.usc.corral.condor.Condor;
import edu.usc.corral.condor.CondorEventGenerator;
import edu.usc.corral.condor.CondorException;
import edu.usc.corral.condor.CondorGridType;
import edu.usc.corral.condor.CondorJob;
import edu.usc.corral.condor.CondorUniverse;
import edu.usc.corral.config.ConfigurationException;
import edu.usc.corral.db.Database;
import edu.usc.corral.db.DatabaseException;
import edu.usc.corral.db.SiteDAO;
import edu.usc.corral.nl.NetLogger;
import edu.usc.corral.nl.NetLoggerEvent;
import edu.usc.corral.nl.NetLoggerException;
import edu.usc.corral.service.state.Event;
import edu.usc.corral.service.state.EventQueue;
import edu.usc.corral.service.state.GlideinEvent;
import edu.usc.corral.service.state.GlideinEventCode;
import edu.usc.corral.service.state.InstallSiteListener;
import edu.usc.corral.service.state.SiteEvent;
import edu.usc.corral.service.state.SiteEventCode;
import edu.usc.corral.service.state.UninstallSiteListener;
import edu.usc.corral.types.EnvironmentVariable;
import edu.usc.corral.types.ExecutionService;
import edu.usc.corral.types.ServiceType;
import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteState;
import edu.usc.corral.util.CredentialUtil;
import edu.usc.corral.util.FilesystemUtil;
import edu.usc.corral.util.ServiceUtil;

public class SiteResource implements Resource {
	private final Logger logger = Logger.getLogger(SiteResource.class);
	private Site site;
	
	public SiteResource(Site site) {
		this.site = site;
	}
	
	public Site getSite() {
		return site;
	}
	
	public boolean authorized(String subject) {
		return site.getSubject().equals(subject);
	}
	
	public void remove(boolean force, GlobusCredential cred) throws GlideinException {
		info("Creating remove event");
		
		// Choose new state. If force, then just delete the record from
		// the database and remove the resource, otherwise go through the
		// removal process.
		SiteEventCode code;
		if (force) {
			code = SiteEventCode.DELETE;
		} else {
			code = SiteEventCode.REMOVE;
			// Save credential (its really only needed for REMOVE)
			storeCredential(cred);
		}
		
		// Schedule remove event
		try {
			Event event = new SiteEvent(code,new Date(),site.getId());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get event queue",ne);
		}
	}
	
	public void submit(GlobusCredential cred) throws GlideinException {
		info("Creating submit event");
		
		// Create working directory
		File work = getWorkingDirectory();
		if (!work.exists()) {
			work.mkdirs();
		}
		
		// Save credential
		storeCredential(cred);
		
		// Schedule submit event
		try {
			Event event = new SiteEvent(SiteEventCode.SUBMIT,new Date(),site.getId());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get event queue",ne);
		}
	}
	
	private void updateState(SiteState state, String shortMessage, String longMessage, Date time) throws GlideinException {
		info("Changing state to "+state+": "+shortMessage);
		
		// Update object
		site.setState(state);
		site.setShortMessage(shortMessage);
		site.setLongMessage(longMessage);
		site.setLastUpdate(time);
		
		// Update database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.updateState(site.getId(), state, shortMessage, longMessage, time);
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to change state to "+state,de);
		}
		
		// TODO Notify listeners
		
		// Log it in the netlogger log
		try {
			NetLoggerEvent event = new NetLoggerEvent("site."+state.toString().toLowerCase());
			event.setTimeStamp(time);
			event.put("site.id", site.getId());
			event.put("message", shortMessage);
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			warn("Unable to log site event to NetLogger log",nle);
		}
	}
	
	private ServiceConfiguration getConfig() throws GlideinException {
		try {
			return ServiceConfiguration.getInstance();
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get service configuration",ne);
		}
	}
	
	private File getWorkingDirectory() throws GlideinException {
		// Determine the directory path
		return new File(getConfig().getWorkingDirectory(),"site-"+site.getId());
	}
	
	private File getInstallDirectory() throws GlideinException {
		return new File(getWorkingDirectory(),"install");
	}
	
	private File getUninstallDirectory() throws GlideinException {
		return new File(getWorkingDirectory(),"uninstall");
	}
	
	private void submitInstallJob() throws GlideinException {
		info("Submitting install job");
		
		ServiceConfiguration config = getConfig();
		
		// Get job directory
		File jobDirectory = getInstallDirectory();
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory,site.getLocalUsername());
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT2);
		} else if(ServiceType.GT5.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT5);
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(stagingService.getServiceContact());
		
		// Set rsl/xml
		if (ServiceType.GT2.equals(stagingService.getServiceType()) ||
			ServiceType.GT5.equals(stagingService.getServiceType())) {
			// GT2 and GT5 use globus_rsl
			StringBuilder rsl = new StringBuilder();
			if(stagingService.getProject() != null)
				rsl.append("(project="+stagingService.getProject()+")");
			if(stagingService.getQueue() != null)
				rsl.append("(queue="+stagingService.getQueue()+")");
			rsl.append("(maxTime=5)"); // No more than 5 mins
			job.setGlobusRSL(rsl.toString());
		} else {
			// GT4 uses globus_xml
			StringBuilder xml = new StringBuilder();
			if(stagingService.getProject() != null)
				xml.append("<project>"+stagingService.getProject()+"</project>");
			if(stagingService.getQueue() != null)
				xml.append("<queue>"+stagingService.getQueue()+"</queue>");
			xml.append("<maxTime>5</maxTime>"); // No more than 5 mins
			job.setGlobusXML(xml.toString());
		}
		
		// Set glidein_install executable
		String install = config.getInstall();
		job.setExecutable(install);
		job.setLocalExecutable(true);
		
		// Set credential
		GlobusCredential cred = loadCredential();
		if (validateCredentialLifetime(cred)) {
			job.setCredential(cred);
		} else {
			throw new GlideinException("Not enough time left on credential");
		}
		
		// Add environment
		if (site.getEnvironment()!=null) {
			for (EnvironmentVariable var : site.getEnvironment())
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add some default environment entries
		job.addEnvironment("CORRAL_SERVER", ServiceUtil.getServiceHost());
		job.addEnvironment("CORRAL_SITE_ID", Integer.toString(site.getId()));
		job.addEnvironment("CORRAL_SITE_NAME", site.getName());
		job.addEnvironment("CORRAL_USERNAME", site.getLocalUsername());
		
		// Add extended attributes
		job.addXAttr("CorralServer", ServiceUtil.getServiceHost());
		job.addXAttr("CorralSiteId", site.getId());
		job.addXAttr("CorralSiteName", site.getName());
		job.addXAttr("CorralUsername", site.getLocalUsername());
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		if (site.getCondorPackage()==null) {
			job.addArgument("-condorVersion "+site.getCondorVersion());
		} else {
			job.addArgument("-condorPackage "+site.getCondorPackage());
		}
		job.addArgument("-rls "+config.getRls());
		job.addArgument("-mapper "+config.getMapper());
		
		// Add status output file
		job.addOutputFile("status");
		
		// Add a listener
		job.addListener(new InstallSiteListener(site.getId()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new GlideinException("Unable to submit install job",ce);
		}
		
		// Log the condor job id in netlogger
		try {
			NetLoggerEvent event = new NetLoggerEvent("site.submit.install");
			event.put("site.id", site.getId());
			event.put("condor.id", job.getJobId());
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			warn("Unable to log site event to NetLogger log",nle);
		}
	}
	
	private boolean hasGlideins() throws GlideinException {
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			return dao.hasGlideins(site.getId());
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to check for glideins",de);
		}
	}
	
	private void cancelGlideins() throws GlideinException {
		info("Canceling glideins");
		
		// Get glidein ids
		int[] ids;
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			ids = dao.getGlideinIds(site.getId());
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to get glidein ids",de);
		}
		
		// Create a remove event for each glidein
		try {
			EventQueue queue = EventQueue.getInstance();
			for (int id : ids) {
				Event event = new GlideinEvent(
						GlideinEventCode.REMOVE,site.getLastUpdate(),id);
				queue.add(event);
			}
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get event queue",ne);
		}
	}
	
	private void notifyGlideinsOfReady() throws GlideinException {
		notifyGlideins(SiteState.READY);
	}
	
	private void notifyGlideinsOfFailed() throws GlideinException {
		notifyGlideins(SiteState.FAILED);
	}
	
	private void notifyGlideins(SiteState state) throws GlideinException {
		info("Notifying site glideins of "+state+" state");
		
		// Get glidein ids
		int[] ids;
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			ids = dao.getGlideinIds(site.getId());
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to get glidein ids",de);
		}
		
		// Create a ready event for each glidein
		try {
			EventQueue queue = EventQueue.getInstance();
			for (int id : ids) {
				GlideinEventCode code;
				if (SiteState.READY.equals(state)) {
					code = GlideinEventCode.SITE_READY;
				} else if (SiteState.FAILED.equals(state)) {
					code = GlideinEventCode.SITE_FAILED;
				} else {
					throw new IllegalStateException("Unhandled state: "+state);
				}
				Event event = new GlideinEvent(code,site.getLastUpdate(),id);
				queue.add(event);
			}
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get queue: "+
					ne.getMessage(),ne);
		}
	}
	
	private void cancelInstallJob() throws GlideinException {
		info("Canceling install job");
		try {
			CondorJob job = new CondorJob(getInstallDirectory(),site.getLocalUsername());
			String jobid = readJobId(getInstallDirectory());
			job.setJobId(jobid);
			Condor.getInstance().cancelJob(job);
		} catch (CondorException ce) {
			throw new GlideinException("Unable to cancel install job",ce);
		}
	}
	
	private String readJobId(File jobDirectory) throws GlideinException {
		File jobidFile = new File(jobDirectory,"jobid");
		try {
			// Read job id from jobid file
			BufferedReader reader = new BufferedReader(
					new FileReader(jobidFile));
			String jobid = reader.readLine();
			reader.close();
			
			return jobid;
		} catch (IOException ioe) {
			throw new GlideinException("Unable to read job id",ioe);
		}
	}
	
	private void storeCredential(GlobusCredential cred) throws GlideinException {
		try {
			CredentialUtil.store(cred, getCredentialFile());
		} catch (IOException ioe) {
			throw new GlideinException("Unable to store credential", ioe);
		}
	}
	
	private GlobusCredential loadCredential() throws GlideinException {
		try {
			return CredentialUtil.load(getCredentialFile());
		} catch (IOException ioe) {
			throw new GlideinException("Unable to load credential", ioe);
		}
	}
	
	private File getCredentialFile() throws GlideinException {
		File dir = getWorkingDirectory();
		File credFile = new File(dir,"credential");
		return credFile;
	}
	
	private boolean validateCredentialLifetime(GlobusCredential credential) throws GlideinException {
		// Require at least 5 minutes
		long need = 300;
		return (need < credential.getTimeLeft());
	}
	
	private void submitUninstallJob() throws GlideinException {
		info("Submitting uninstall job");
		
		ServiceConfiguration config = getConfig();
		
		// Get job directory
		File jobDirectory = getUninstallDirectory();
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory,site.getLocalUsername());
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT2);
		} else if(ServiceType.GT5.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT5);
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(stagingService.getServiceContact());
		
		// Set rsl/xml
		if (ServiceType.GT2.equals(stagingService.getServiceType()) ||
			ServiceType.GT5.equals(stagingService.getServiceType())) {
			// GT2 and GT5 use globus_rsl
			StringBuilder rsl = new StringBuilder();
			if(stagingService.getProject() != null)
				rsl.append("(project="+stagingService.getProject()+")");
			if(stagingService.getQueue() != null)
				rsl.append("(queue="+stagingService.getQueue()+")");
			rsl.append("(maxTime=5)"); // No more than 5 mins
			job.setGlobusRSL(rsl.toString());
		} else {
			// GT4 uses globus_xml
			StringBuilder xml = new StringBuilder();
			if(stagingService.getProject() != null)
				xml.append("<project>"+stagingService.getProject()+"</project>");
			if(stagingService.getQueue() != null)
				xml.append("<queue>"+stagingService.getQueue()+"</queue>");
			xml.append("<maxTime>5</maxTime>"); // No more than 5 mins
			job.setGlobusXML(xml.toString());
		}
		
		// Set glidein_uninstall executable
		String uninstall = config.getUninstall();
		job.setExecutable(uninstall);
		job.setLocalExecutable(true);
		
		// Load credential
		GlobusCredential cred = loadCredential();
		if (validateCredentialLifetime(cred)) {
			job.setCredential(cred);
		} else {
			throw new GlideinException("Not enough time on credential");
		}
		
		// Add environment
		if (site.getEnvironment()!=null) {
			for (EnvironmentVariable var : site.getEnvironment())
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add some default environment entries
		job.addEnvironment("CORRAL_SERVER", ServiceUtil.getServiceHost());
		job.addEnvironment("CORRAL_SITE_ID", Integer.toString(site.getId()));
		job.addEnvironment("CORRAL_SITE_NAME", site.getName());
		job.addEnvironment("CORRAL_USERNAME", site.getLocalUsername());
		
		// Add extended attributes
		job.addXAttr("CorralServer", ServiceUtil.getServiceHost());
		job.addXAttr("CorralSiteId", site.getId());
		job.addXAttr("CorralSiteName", site.getName());
		job.addXAttr("CorralUsername", site.getLocalUsername());
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		
		// Add status output file
		job.addOutputFile("status");
		
		// Add a listener
		job.addListener(new UninstallSiteListener(site.getId()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new GlideinException("Unable to submit uninstall job",ce);
		}
		
		// Log the condor job id in netlogger
		try {
			NetLoggerEvent event = new NetLoggerEvent("site.submit.uninstall");
			event.put("site.id", site.getId());
			event.put("condor.id", job.getJobId());
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			warn("Unable to log site event to NetLogger log",nle);
		}
		
	}
	
	private void deleteFromDatabase() throws GlideinException {
		info("Deleting site from database");
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to delete site",de);
		}
	}
	
	private void delete() throws GlideinException {
		// Remove the Resource from the resource home
		try {
			SiteResourceHome resourceHome = SiteResourceHome.getInstance();
			resourceHome.remove(site.getId());
		} catch (ConfigurationException e) {
			throw new GlideinException("Unable to locate SiteResourceHome",e);
		}
		
		// Delete the site from the database
		deleteFromDatabase();
		
		// Remove the working directory and all sub-directories
		FilesystemUtil.rm(getWorkingDirectory());
	}
	
	private void fail(String message, Exception exception, Date time) throws GlideinException {
		// Update status to FAILED
		error("Failure: "+message,exception);
		if (exception == null) {
			updateState(SiteState.FAILED, message, null, time);
		} else {
			CharArrayWriter caw = new CharArrayWriter();
			exception.printStackTrace(new PrintWriter(caw));
			updateState(SiteState.FAILED, message, caw.toString(), time);
		}
		
		// Notify glideins that site failed
		notifyGlideinsOfFailed();
	}
	
	private void failQuietly(String message, Exception exception, Date time) {
		try {
			fail(message,exception,time);
		} catch (GlideinException re) {
			error("Unable to change state to "+SiteState.FAILED,re);
		}
	}
	
	public synchronized void handleEvent(SiteEvent event) {
		try {
			_handleEvent(event);
		} catch(GlideinException re) {
			// RemoteException tacks the cause on to the end of
			// the message. We don't want that in the database.
			// Instead, the database stores the entire stack trace
			// in the long message.
			String message = re.getMessage();
			if (message.length() > 64) {
				message = message.substring(0, 64);
			}
			int cause = message.indexOf("; nested exception is:");
			int lf = message.indexOf("\n");
			int br = Math.min(cause, lf);
			if (br > 0) {
				failQuietly(message.substring(0, br), re, event.getTime());
			} else {
				failQuietly(message, re, event.getTime());
			}
		}
	}
	
	private void _handleEvent(SiteEvent event) throws GlideinException {
		SiteState state = site.getState();
		SiteEventCode code = (SiteEventCode)event.getCode();
		
		// If the site was deleted, then we can just ignore the event
		// This is here just in case someone gets a reference to the resource
		// before we have a chance to remove it from the resource home
		if (SiteState.DELETED.equals(state)) {
			warn("Unable to process event "+code+": "+
					"Site has been deleted");
			return;
		}
		
		// Process event
		switch (code) {
			
			case SUBMIT: {
				
				SiteState reqd = SiteState.NEW;
				if (reqd.equals(state)) {
					
					// Change status to staging
					updateState(SiteState.STAGING, "Staging executables", null, event.getTime());
					
					// Submit job
					submitInstallJob();
					
				} else {
					warn("State was not "+reqd+" when "+code+" was recieved");
				}
				
			} break;
			
			case INSTALL_SUCCESS: {
				
				SiteState reqd = SiteState.STAGING;
				if (reqd.equals(state)) {
					
					// Change status to READY
					updateState(SiteState.READY, "Installed", null, event.getTime());
					
					// Notify any waiting glideins for this site
					try {
						notifyGlideinsOfReady();
					} catch (GlideinException re) {
						// No need to fail here, just print a warning
						warn("Unable to notify glideins",re);
					}
					
				} else {
					warn("State was not "+reqd+" when "+code+" was recieved");
				}
				
			} break;
			
			case INSTALL_FAILED: {
				
				SiteState reqd = SiteState.STAGING;
				if (reqd.equals(state)) {
					failQuietly((String)event.getProperty("message"),
							(Exception)event.getProperty("exception"),
							event.getTime());
				} else {
					warn("State was not "+reqd+" when "+code+" was recieved");
				}
				
			} break;
				
			case REMOVE: {
				
				// If we are already removing, then do nothing
				if (SiteState.REMOVING.equals(state)) {
					warn("Already removing site");
					return;
				}
				
				// If we are currently staging, then we need to cancel the install job
				if (SiteState.STAGING.equals(state)) {
					cancelInstallJob();
				}
				
				// If there are some glideins for this site
				if (hasGlideins()) {
					
					// Change state to EXITING
					updateState(SiteState.EXITING, "Waiting for glideins", null, event.getTime());
					
					// Cancel running glideins
					cancelGlideins();
					
				} else {
					
					// Change state to REMOVING
					updateState(SiteState.REMOVING, "Removing site", null, event.getTime());
					
					// Submit uninstall job
					submitUninstallJob();
					
				}
				
			} break;
				
			case GLIDEIN_DELETED: {
				
				// If exiting then check for last glidein
				if (SiteState.EXITING.equals(state)) {
					
					// If last glidein
					if (!hasGlideins()) {
						
						// Change state
						updateState(SiteState.REMOVING, 
								"Removing site", null, event.getTime());
						
						// Submit uninstall job
						submitUninstallJob();
						
					}
					
				}
				
			} break;
			
			case UNINSTALL_SUCCESS:
			case DELETE: {
				
				// Delete the site
				updateState(SiteState.DELETED,
						"Site deleted",null,event.getTime());
				delete();
				
			} break;
			
			case UNINSTALL_FAILED: {
				
				SiteState reqd = SiteState.REMOVING;
				if (reqd.equals(state)) {
					failQuietly((String)event.getProperty("message"),
							(Exception)event.getProperty("exception"),
							event.getTime());
				} else {
					warn("Site "+site.getId()+" was not "+reqd+
							" when "+code+" was recieved");
				}
				
			} break;
			
			default: {
				
				Exception e = new IllegalStateException();
				e.fillInStackTrace();
				error("Unhandled event: "+event,e);
				
			} break;
		}
	}
	
	public synchronized void recoverState() throws Exception {
		try {
			
			// Try to recover state
			_recoverState();
			
		} catch (GlideinException re) {
			try {
				
				// If recovery fails, try to update the state to failed
				fail("State recovery failed",re,new Date());
				
			} catch (GlideinException re2) {
				
				// If that fails, then fail the entire recovery process
				throw new Exception(
						"Unable to recover site "+site.getId(),re);
				
			}
		}
	}
	
	private void _recoverState() throws GlideinException {
		info("Recovering state");
		
		SiteState state = site.getState();
		if (SiteState.NEW.equals(state)) {
			
			/* Do nothing */
			
		} else if (SiteState.STAGING.equals(state)) {
			
			// If the current state is STAGING, then the job could be 
			// finished, running, ready to submit, or not ready
			
			File jobDir = getInstallDirectory();
			CondorJob job = new CondorJob(jobDir, site.getLocalUsername());
			
			if (job.getLog().exists()) {
				
				// If the install log exists, then recover the install job
				job.setJobId(readJobId(jobDir));
				InstallSiteListener listener = 
					new InstallSiteListener(site.getId());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialFile().exists()) {
				
				// If the credential file still exists, 
				// try to submit the install job
				submitInstallJob();
				
			} else {
				
				// Otherwise fail the site
				fail("Unable to recover site",null,new Date());
				
			}
			
		} else if (SiteState.READY.equals(state)) {
			
			// Let the glideins know we are ready just in case they have not
			// been informed yet
			notifyGlideinsOfReady();
			
		} else if (SiteState.EXITING.equals(state)) {
			
			// Notify the glideins to exit in case they have not been told to
			// do so yet.
			cancelGlideins();
			
		} else if (SiteState.REMOVING.equals(state)) {
			
			// If the current state is removing, then the uninstall job
			// could be finished, running, ready to submit or unready
			
			File jobDir = getUninstallDirectory();
			CondorJob job = new CondorJob(jobDir, site.getLocalUsername());
			
			if (job.getLog().exists()) {
				
				// If the log file exists, try to recover the job
				job.setJobId(readJobId(jobDir));
				UninstallSiteListener listener = 
					new UninstallSiteListener(site.getId());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialFile().exists()) {
				
				// Else, if the credential file exists, try to
				// submit the uninstall job
				submitUninstallJob();
				
			} else {
				
				// Otherwise, set the state to failed. We need to do this
				// because we can't continue with removing without a
				// credential, and we can't go back to ready because
				// we can't be sure that the site is OK.
				updateState(SiteState.FAILED,"State recovery failed", 
						null, new Date());
				
			}
			
		} else if (SiteState.FAILED.equals(state)) {
			
			/* Do nothing */
			
		} else if (SiteState.DELETED.equals(state)) {
			
			// Delete the site
			delete();
			
		} else {
			
			/* This should not happen */
			throw new IllegalStateException(
					"Unrecognized state: "+state);
			
		}
	}
	
	private String logPrefix() {
		if (site == null) {
			return "";
		} else {
			return "Site "+site.getId()+": ";
		}
	}
	protected void debug(String message) {
		debug(message,null);
	}
	protected void debug(String message, Throwable t) {
		logger.debug(logPrefix()+message, t);
	}
	protected void info(String message) {
		info(message,null);
	}
	protected void info(String message, Throwable t) {
		logger.info(logPrefix()+message,t);
	}
	protected void warn(String message) {
		warn(message,null);
	}
	protected void warn(String message, Throwable t) {
		logger.warn(logPrefix()+message,t);
	}
	protected void error(String message) {
		error(message,null);
	}
	protected void error(String message, Throwable t) {
		logger.error(logPrefix()+message,t);
	}
}
