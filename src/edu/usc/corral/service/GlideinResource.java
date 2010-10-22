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
import edu.usc.corral.db.GlideinDAO;
import edu.usc.corral.nl.NetLogger;
import edu.usc.corral.nl.NetLoggerEvent;
import edu.usc.corral.nl.NetLoggerException;
import edu.usc.corral.service.state.Event;
import edu.usc.corral.service.state.EventQueue;
import edu.usc.corral.service.state.GlideinEvent;
import edu.usc.corral.service.state.GlideinEventCode;
import edu.usc.corral.service.state.GlideinListener;
import edu.usc.corral.service.state.SiteEvent;
import edu.usc.corral.service.state.SiteEventCode;
import edu.usc.corral.types.EnvironmentVariable;
import edu.usc.corral.types.ExecutionService;
import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.GlideinState;
import edu.usc.corral.types.ServiceType;
import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteState;
import edu.usc.corral.util.CredentialUtil;
import edu.usc.corral.util.FilesystemUtil;
import edu.usc.corral.util.IOUtil;
import edu.usc.corral.util.ServiceUtil;

public class GlideinResource implements Resource {
	private Logger logger = Logger.getLogger(GlideinResource.class);
	private Glidein glidein = null;
	
	/**
	 * Default constructor required
	 */
	public GlideinResource(Glidein glidein) {
		this.glidein = glidein;
	}
	
	public Glidein getGlidein() {
		return glidein;
	}
	
	public boolean authorized(String subject) {
		return glidein.getSubject().equals(subject);
	}
	
	public void remove(boolean force) throws GlideinException  {
		info("Removing glidein");
		
		// If we are forcing the issue, then just delete it
		GlideinEventCode code = null;
		if (force) {
			code = GlideinEventCode.DELETE;
		} else {
			code = GlideinEventCode.REMOVE;
		}
		
		// Queue the event
		try {
			Event event = new GlideinEvent(code, new Date(), glidein.getId());
			EventQueue queue = EventQueue.getInstance(); 
			queue.add(event);
		} catch(ConfigurationException ne) {
			throw new GlideinException("Unable to remove glidein",ne);
		}
	}

	public void submit(GlobusCredential cred) throws GlideinException {
		info("Submitting glidein");
		
		// Create the working directory
		File work = getWorkingDirectory();
		if (!work.exists()) {
			work.mkdirs();
		}
		
		// Save the credential
		storeCredential(cred);
		
		try {
			// Create submit event
			Event event = new GlideinEvent(GlideinEventCode.SUBMIT,new Date(),glidein.getId());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get event queue",ne);
		}
	}
	
	private void updateState(GlideinState state, String shortMessage, String longMessage, Date time) throws GlideinException {
		info("Changing state to "+state+": "+shortMessage);
		
		// Update object
		glidein.setState(state);
		glidein.setShortMessage(shortMessage);
		glidein.setLongMessage(longMessage);
		glidein.setLastUpdate(time);
		
		// Update database
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.updateState(glidein.getId(), state, shortMessage, longMessage, time);
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to update state to "+state,de);
		}
		
		// TODO Notify listeners
		
		// Log it in the netlogger log
		try {
			NetLoggerEvent event = new NetLoggerEvent("glidein."+state.toString().toLowerCase());
			event.setTimeStamp(time);
			event.put("glidein.id", glidein.getId());
			event.put("message", shortMessage);
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			warn("Unable to log glidein event to NetLogger log",nle);
		}
	}
	
	private void submitGlideinJob() throws GlideinException {
		info("Submitting glidein job");
		
		SiteResource siteResource = getSiteResource(glidein.getSiteId());
		Site site = siteResource.getSite();
		
		// Get configuration
		ServiceConfiguration config;
		try {
			config = ServiceConfiguration.getInstance();
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get service configuration",ne);
		}
		
		// Create job directory
		File jobDirectory = getJobDirectory();
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory, glidein.getLocalUsername());
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService glideinService = site.getGlideinService();
		if(ServiceType.GT2.equals(glideinService.getServiceType())) {
			job.setGridType(CondorGridType.GT2);
		} else if(ServiceType.GT5.equals(glideinService.getServiceType())) {
			job.setGridType(CondorGridType.GT5);
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(glideinService.getServiceContact());
		
		// Set rsl/xml
		StringBuilder rsl = new StringBuilder();
		if (glidein.getRsl() != null) rsl.append(glidein.getRsl());
		if (ServiceType.GT2.equals(glideinService.getServiceType()) ||
			ServiceType.GT5.equals(glideinService.getServiceType())) {
			// GT2 and GT5 use globus_rsl
			if (rsl.indexOf("(project=")==-1 && glideinService.getProject() != null)
				rsl.append("(project="+glideinService.getProject()+")");
			if (rsl.indexOf("(queue=")==-1 && glideinService.getQueue() != null)
				rsl.append("(queue="+glideinService.getQueue()+")");
			if (rsl.indexOf("(hostCount=")==-1)
				rsl.append("(hostCount="+glidein.getHostCount()+")");
			if (rsl.indexOf("(count=")==-1)
				rsl.append("(count="+glidein.getCount()+")");
			if (rsl.indexOf("(jobType=")==-1)
				rsl.append("(jobType=multiple)");
			if (rsl.indexOf("(maxTime=")==-1)
				rsl.append("(maxTime="+glidein.getWallTime()+")");
			job.setGlobusRSL(rsl.toString());
		} else {
			// GT4 uses globus_xml
			if (rsl.indexOf("<count>")==-1)
				rsl.append("<count>"+glidein.getCount()+"</count>");
			if (rsl.indexOf("<hostCount>")==-1)
				rsl.append("<hostCount>"+glidein.getHostCount()+"</hostCount>");
			if (rsl.indexOf("<project>")==-1 && glideinService.getProject() != null)
				rsl.append("<project>"+glideinService.getProject()+"</project>");
			if (rsl.indexOf("<queue>")==-1 && glideinService.getQueue() != null)
				rsl.append("<queue>"+glideinService.getQueue()+"</queue>");
			if (rsl.indexOf("<maxTime>")==-1)
				rsl.append("<maxTime>"+glidein.getWallTime()+"</maxTime>");
			if (rsl.indexOf("<jobType>")==-1)
				rsl.append("<jobType>multiple</jobType>");
			job.setGlobusXML(rsl.toString());
		}
		
		// glidein_start is the executable to call
		String start = config.getStart();
		job.setExecutable(start);
		job.setLocalExecutable(true);
		
		// glidein_run is an input file
		String run = config.getRun();
		job.addInputFile(run);
		
		// Add environment
		if (site.getEnvironment()!=null) {
			for (EnvironmentVariable var : site.getEnvironment())
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add some default environment variables
		job.addEnvironment("CORRAL_SERVER", ServiceUtil.getServiceHost());
		job.addEnvironment("CORRAL_SITE_ID", Integer.toString(site.getId()));
		job.addEnvironment("CORRAL_GLIDEIN_ID", Integer.toString(glidein.getId()));
		job.addEnvironment("CORRAL_SITE_NAME", site.getName());
		job.addEnvironment("CORRAL_USERNAME", glidein.getLocalUsername());
		
		// Add extended attributes
		job.addXAttr("CorralServer", ServiceUtil.getServiceHost());
		job.addXAttr("CorralSiteId", site.getId());
		job.addXAttr("CorralGlideinId", glidein.getId());
		job.addXAttr("CorralSiteName", site.getName());
		job.addXAttr("CorralUsername", glidein.getLocalUsername());
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		job.addArgument("-localPath "+site.getLocalPath());
		job.addArgument("-condorHost "+glidein.getCondorHost());
		// Subtract 1 from wall time to give condor a chance to exit on its
		// own before it gets killed by the local scheduler
		job.addArgument("-wallTime "+(glidein.getWallTime()-1));
		if(glidein.getGcbBroker()!=null)
			job.addArgument("-gcbBroker "+glidein.getGcbBroker());
		if(glidein.getCcbAddress()!=null)
			job.addArgument("-ccbAddress "+glidein.getCcbAddress());
		if(glidein.getIdleTime()!=null && glidein.getIdleTime()>0)
			job.addArgument("-idleTime "+glidein.getIdleTime());
		if(glidein.getCondorDebug()!=null) {
			String[] debug = glidein.getCondorDebug().split("[ ,;:]+");
			for(String level : debug)
				job.addArgument("-debug "+level);
		}
		if(glidein.getNumCpus()!=null && glidein.getNumCpus()>0)
			job.addArgument("-numCpus "+glidein.getNumCpus());
		
		// Set port range if specified
		if (glidein.getHighport()!=null && glidein.getHighport()>0)
			job.addArgument("-highport "+glidein.getHighport());
		if (glidein.getLowport()!=null && glidein.getLowport()>0)
			job.addArgument("-lowport "+glidein.getLowport());
		
		// If there is a special config file, use it
		String configFile = null;
		if (glidein.getCondorConfig()==null) {
			configFile = config.getGlideinCondorConfig();
		} else {
			try {
				// Save config to a file in the submit directory
				configFile = "glidein_condor_config";
				IOUtil.write(glidein.getCondorConfig(),
						new File(job.getJobDirectory(),configFile));
			} catch (IOException ioe) {
				throw new GlideinException("Error writing glidein_condor_config",ioe);
			}
		}
		job.addInputFile(configFile);
		
		// Add status output file
		job.addOutputFile("status");
		
		// Set the credential
		GlobusCredential cred = loadCredential();
		if (validateCredentialLifetime(cred)) {
			job.setCredential(cred);
		} else {
			throw new GlideinException("Not enough time on credential");
		}
		
		// Add a listener
		job.addListener(new GlideinListener(glidein.getId()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new GlideinException("Unable to submit glidein job",ce);
		}
		
		// Log the condor job id in netlogger
		try {
			NetLoggerEvent event = new NetLoggerEvent("glidein.submit");
			event.put("glidein.id", glidein.getId());
			event.put("site.id", site.getId());
			event.put("condor.id", job.getJobId());
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			warn("Unable to log glidein event to NetLogger log",nle);
		}
		
		// Increment the number of submits
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.incrementSubmits(glidein.getId());
			glidein.setSubmits(glidein.getSubmits()+1);
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to load glidein",de);
		}
	}
	
	private String readJobId() throws GlideinException {
		File jobidFile = new File(getJobDirectory(),"jobid");
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
	
	private boolean siteIsReady() throws GlideinException {
		SiteResource resource = getSiteResource(glidein.getSiteId());
		SiteState state = resource.getSite().getState();
		return SiteState.READY.equals(state);
	}
	
	private SiteResource getSiteResource(int siteId) throws GlideinException {
		try {
			SiteResourceHome home = SiteResourceHome.getInstance();
			return (SiteResource)home.find(siteId);
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get SiteResourceHome",ne);
		}
	}
	
	private void cancelGlideinJob() throws GlideinException {
		info("Cancelling glidein job");
		
		try {
			// Find submit dir
			File dir = getJobDirectory();
			File jobidFile = new File(dir,"jobid");
			CondorJob job = new CondorJob(dir,glidein.getLocalUsername());
			
			// Read job id from jobid file
			BufferedReader reader = new BufferedReader(
					new FileReader(jobidFile));
			String jobid = reader.readLine();
			reader.close();
			job.setJobId(jobid);
			
			// condor_rm job
			Condor.getInstance().cancelJob(job);
		} catch (IOException ioe) {
			throw new GlideinException(
					"Unable to read job id",ioe);
		} catch (CondorException ce) {
			throw new GlideinException(
					"Unable to cancel glidein job",ce);
		}
	}
	
	private void deleteFromDatabase() throws GlideinException {
		info("Deleting glidein from database");
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.delete(glidein.getId());
		} catch(DatabaseException de) {
			throw new GlideinException("Unable to delete glidein",de);
		}
	}
	
	private void delete() throws GlideinException {
		// Remove the glidein from the ResourceHome
		try {
			GlideinResourceHome home = GlideinResourceHome.getInstance();
			home.remove(glidein.getId());
		}  catch (ConfigurationException e) {
			throw new GlideinException("Unable to locate GlideinResourceHome",e);
		}
		
		// Delete the glidein from the database
		deleteFromDatabase();
		
		// Tell the Site About it
		try {
			Event siteEvent = new SiteEvent(SiteEventCode.GLIDEIN_DELETED, 
					glidein.getLastUpdate(), glidein.getSiteId());
			EventQueue queue = EventQueue.getInstance();
			queue.add(siteEvent);
		} catch (ConfigurationException ne) {
			warn("Unable to notify site of deleted glidein",ne);
		}
		
		// Remove the working directory
		try {
			FilesystemUtil.rm(getWorkingDirectory());
		} catch (IOException ioe) {
			throw new GlideinException("Unable to remove working directory",ioe);
		}
	}
	
	private ServiceConfiguration getConfig() throws GlideinException {
		try {
			return ServiceConfiguration.getInstance();
		} catch (ConfigurationException ne) {
			throw new GlideinException("Unable to get service configuration",ne);
		}
	}
	
	private File getJobDirectory() throws GlideinException {
		File jobDirectory = new File(getWorkingDirectory(),"job");
		if (!jobDirectory.exists()) jobDirectory.mkdir();
		return jobDirectory;
	}
	
	private File getWorkingDirectory() throws GlideinException {
		return new File(getConfig().getWorkingDirectory(),"glidein-"+glidein.getId());
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
		File work = getWorkingDirectory();
		File credFile = new File(work,"credential");
		return credFile;
	}
	
	private boolean validateCredentialLifetime(GlobusCredential credential)  throws GlideinException {
		// Require enough time to cover at least the wall time of the job
		// More time may be required, but less time will not
		return (glidein.getWallTime()*60 < credential.getTimeLeft());
	}
	
	private void failQuietly(String message, String longMessage, Exception exception, Date time) {
		try {
			fail(message,longMessage,exception,time);
		} catch (GlideinException re) {
			error("Unable to change state to "+GlideinState.FAILED,re);
		}
	}
	
	private void fail(String message, String longMessage, Exception exception, Date time) throws GlideinException {
		// Update status to FAILED
		error("Failure: "+message,exception);
		if (exception == null) {
			updateState(GlideinState.FAILED, message, longMessage, time);
		} else {
			CharArrayWriter caw = new CharArrayWriter();
			exception.printStackTrace(new PrintWriter(caw));
			updateState(GlideinState.FAILED, message, caw.toString()+"\n"+longMessage, time);
		}
	}
	
	public synchronized void handleEvent(GlideinEvent event) {
		try {
			_handleEvent(event);
		} catch (GlideinException re) {
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
				failQuietly(message.substring(0, br), null, re, event.getTime());
			} else {
				failQuietly(message, null, re, event.getTime());
			}
		}
	}
	
	private void _handleEvent(GlideinEvent event) throws GlideinException {
		GlideinState state = glidein.getState();
		GlideinEventCode code = (GlideinEventCode) event.getCode();
		
		// If the glidein was deleted, then don't process any more events
		// This is here just in case someone gets a reference to the resource
		// before we have a chance to remove it from the resource home
		if (GlideinState.DELETED.equals(state)) {
			warn("Unable to process event "+code+": "+
					"Glidein has been deleted");
			return;
		}
		
		switch (code) {
			
			case SUBMIT: {
				
				// Only NEW glideins can be submitted
				GlideinState reqd = GlideinState.NEW;
				if (reqd.equals(state)) {
					
					if (siteIsReady()) {
						
						// If the site is ready, submit the glidein job
						updateState(GlideinState.SUBMITTED,
								"Local job submitted",null,event.getTime());
						submitGlideinJob();
						
					} else {
						
						// Otherwise, we need to wait for the site
						updateState(GlideinState.WAITING, 
								"Waiting for site to be "+ SiteState.READY, 
								null,event.getTime());
						
					}
				} else {
					warn("State was not "+reqd+" when event "+
							code+" was received");
				}
				
			} break;
			
			case SITE_READY: {
				
				// If we were waiting, then submit the job
				if (GlideinState.WAITING.equals(state)) {
					updateState(GlideinState.SUBMITTED,
							"Local job submitted",null,event.getTime());
					submitGlideinJob();
				}
				
			} break;
			
			case SITE_FAILED: {
				
				// If a glidein job has been submitted cancel the job
				if (GlideinState.SUBMITTED.equals(state) || 
						GlideinState.RUNNING.equals(state) || 
						GlideinState.QUEUED.equals(state)) {
					try {
						cancelGlideinJob();
					} catch (GlideinException re) {
						// Just log it so that we can fail properly
						error("Unable to cancel glidein job",re);
					}
				}
				
				// If the site failed, then the glidein will fail
				failQuietly("Site failed",null,null,event.getTime());
				
			} break;
			
			case QUEUED: {
				
				GlideinState reqd = GlideinState.SUBMITTED;
				if (reqd.equals(state)) {
					updateState(GlideinState.QUEUED,
							"Glidein job queued",null,event.getTime());
				} else {
					warn("State was not "+reqd+" when event "+
							code+" was received");	
				}
				
			} break;
			
			case RUNNING: {
				
				if (GlideinState.SUBMITTED.equals(state) || 
						GlideinState.QUEUED.equals(state)) {
					
					// Update state to running
					updateState(GlideinState.RUNNING,
							"Glidein job running",null,event.getTime());
					
				}
				
			} break;
			
			case REMOVE: {
				
				if (GlideinState.SUBMITTED.equals(state) || 
						GlideinState.RUNNING.equals(state) || 
						GlideinState.QUEUED.equals(state)) {
					
					updateState(GlideinState.REMOVING, 
							"Cancelling job",null,event.getTime());
					
					// If a glidein job has been submitted cancel the job
					cancelGlideinJob();
					
					// This will cause an abort event, which will cause
					// the glidein to be deleted.
					
				} else {
					
					// Otherwise, just delete it
					updateState(GlideinState.DELETED,
							"Glidein deleted",null,event.getTime());
					delete();
					
				}
				
			} break;
			
			case JOB_SUCCESS: {
				
				GlideinState reqd = GlideinState.RUNNING;
				if (reqd.equals(state)) {
					
					// In order to resubmit, the glidein should be resubmit,
					// and the site should be in ready status
					if (shouldResubmit() && siteIsReady()) {
						
						info("Resubmitting glidein");
						updateState(GlideinState.SUBMITTED,
								"Local job submitted",null,event.getTime());
						submitGlideinJob();
						
					} else {
						
						// Otherwise, mark as finished
						updateState(GlideinState.FINISHED,
								"Glidein finished",null,event.getTime());
						
					}
					
				} else {
					
					warn("Glidein was not in "+reqd+" state when "+
							code+" was received");
					
				}
				
			} break;
			
			case DELETE: {
				
				// If the job was aborted, or a delete was requested, then
				// just delete the glidein
				updateState(GlideinState.DELETED,
						"Glidein deleted",null,event.getTime());
				delete();
				
			} break;
			
			case JOB_ABORTED: {
				
				// If job is aborted, then glidein should be failed, but
				// don't change the long message in case it already failed.
				// Otherwise the original error message will be lost!
				updateState(GlideinState.FAILED, "Glidein aborted",
						glidein.getLongMessage(),event.getTime());
				
			} break;
			
			case JOB_FAILURE: {
				
				// If the job fails, then fail the glidein
				failQuietly((String)event.getProperty("message"),
						(String)event.getProperty("longMessage"),
						(Exception)event.getProperty("exception"),
						event.getTime());
				
			} break;
			
			default: {
				
				IllegalStateException ise = new IllegalStateException();
				ise.fillInStackTrace();
				error("Unhandled event: "+event,ise);
				
			} break;
		}
	}
	
	private boolean shouldResubmit() {
		// If the glidein should be resubmitted
		if (glidein.getResubmit()) {
			
			// If resubmits is set
			int resubmits = glidein.getResubmits();
			if (resubmits>0) {
				// If the total number of submits is less 
				// than or equal to  the number of resubmits
				// then we should resubmit
				int submits = glidein.getSubmits();
				if (submits<=resubmits) {
					return true;
				} else {
					return false;
				}
			}
			
			// If until is set
			Date until = glidein.getUntil();
			if (until != null) {
				// If until is in the future, we should resubmit
				Date now = new Date();
				if (until.after(now)) {
					return true;
				} else {
					return false;
				}
			}
			
			// Otherwise, we should resubmit
			return true;
		} else {
			
			// Otherwise we don't resubmit
			return false;
		}
	}
	
	public synchronized void recoverState() throws Exception {
		try {
			// Try to recover state
			_recoverState();
		} catch (GlideinException re) {
			try {
				
				// If recovery fails, try to update the state to failed
				fail("State recovery failed",null,re,new Date());
				
			} catch (GlideinException re2) {
				
				// If that fails, then fail the entire recovery process
				throw new Exception(
						"Unable to recover glidein "+glidein.getId(),re);
				
			}
		}
	}
	
	private void _recoverState() throws GlideinException {
		info("Recovering state");
		
		GlideinState state = glidein.getState();
		
		if (GlideinState.NEW.equals(state)) {
			
			// If the glidein is new, do nothing
			
		} else if (GlideinState.WAITING.equals(state)) {
			
			// If the glidein is waiting for its site, do nothing
			
		} else if (GlideinState.SUBMITTED.equals(state)) {
		
			// If the state is submitted, then the job could be finished,
			// running, ready, or unready
			
			File jobDir = getJobDirectory();
			CondorJob job = new CondorJob(jobDir, glidein.getLocalUsername());
			
			if (job.getLog().exists()) {
				
				// If the install log exists, then recover the install job
				job.setJobId(readJobId());
				GlideinListener listener = 
					new GlideinListener(glidein.getId());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialFile().exists()) {
				
				// If the credential file still exists, 
				// try to submit the install job
				submitGlideinJob();
				
			} else {
				
				// Otherwise set the state to failed
				fail("Unable to submit glidein",null,null,new Date());
				
			}
			
		} else if (GlideinState.QUEUED.equals(state) ||
				GlideinState.RUNNING.equals(state)) {
			
			// If glidein has been submitted to condor, then it can only 
			// be running or finished
			File jobDir = getJobDirectory();
			CondorJob job = new CondorJob(jobDir, glidein.getLocalUsername());
			job.setJobId(readJobId());
			GlideinListener listener = 
				new GlideinListener(glidein.getId());
			job.addListener(listener);
			CondorEventGenerator gen = new CondorEventGenerator(job);
			gen.start();
			
		} else if (GlideinState.REMOVING.equals(state)) {
			
			File jobDir = getJobDirectory();
			CondorJob job = new CondorJob(jobDir, glidein.getLocalUsername());
			
			// If the log is still there, then try to cancel the job
			if (job.getLog().exists()) {
				try {
					cancelGlideinJob();
				} catch (GlideinException re) {
					warn("Unable to cancel job on recovery",re);
				}
			}
			
		} else if (GlideinState.FAILED.equals(state) ||
				   GlideinState.FINISHED.equals(state) ||
				   GlideinState.DELETED.equals(state)) {
			
			// On restarts automatically delete the junk
			
			// Queue up a delete event
			try {
				EventQueue queue = EventQueue.getInstance();
				GlideinEvent delete = new GlideinEvent(
						GlideinEventCode.DELETE, 
						new Date(), glidein.getId());
				queue.add(delete);
			} catch (ConfigurationException e) {
				warn("Unable to queue a delete event");
			}
			
		} else {
			
			// This should not happen
			throw new IllegalStateException("Glidein state was: "+state);
			
		}
	}

	private String logPrefix() {
		if (glidein == null) {
			return "";
		} else {
			return "Glidein "+glidein.getId()+": ";
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