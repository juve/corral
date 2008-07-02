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
package edu.usc.glidein.service;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.naming.NamingException;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.delegation.DelegationException;
import org.globus.delegation.DelegationUtil;
import org.globus.delegation.service.DelegationResource;
import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.PersistenceCallback;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceIdentifier;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.impl.ReflectionResourceProperty;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.SimpleResourcePropertySet;
import org.globus.wsrf.impl.security.authorization.exceptions.InitializeException;

import edu.usc.glidein.condor.Condor;
import edu.usc.glidein.condor.CondorEventGenerator;
import edu.usc.glidein.condor.CondorException;
import edu.usc.glidein.condor.CondorGridType;
import edu.usc.glidein.condor.CondorJob;
import edu.usc.glidein.condor.CondorUniverse;
import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.SiteDAO;
import edu.usc.glidein.service.state.Event;
import edu.usc.glidein.service.state.EventQueue;
import edu.usc.glidein.service.state.GlideinEvent;
import edu.usc.glidein.service.state.GlideinEventCode;
import edu.usc.glidein.service.state.SiteEvent;
import edu.usc.glidein.service.state.SiteEventCode;
import edu.usc.glidein.service.state.InstallSiteListener;
import edu.usc.glidein.service.state.UninstallSiteListener;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;
import edu.usc.glidein.util.AddressingUtil;
import edu.usc.glidein.util.CredentialUtil;
import edu.usc.glidein.util.IOUtil;

public class SiteResource implements Resource, ResourceIdentifier, PersistenceCallback, ResourceProperties
{
	private final Logger logger = Logger.getLogger(SiteResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Site site;
	
	/**
	 * Default constructor required
	 */
	public SiteResource() { }
	
	public synchronized void setSite(Site site)
	{
		this.site = site;
		setResourceProperties();
	}
	
	public synchronized Site getSite()
	{
		return site;
	}
	
	public Object getID()
	{
		return getKey();
	}
	
	public ResourceKey getKey()
	{
		if (site==null) return null;
		return new SimpleResourceKey(
				SiteNames.RESOURCE_KEY,
				new Integer(site.getId()));
	}
	
	public ResourcePropertySet getResourcePropertySet()
	{
		return resourceProperties;
	}
	
	private void setResourceProperties()
	{
		try {
			resourceProperties = new SimpleResourcePropertySet(SiteNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_SITE_ID,"Id",site));
			// TODO: Set the rest of the resource properties, or don't
		} catch (Exception e) {
			throw new RuntimeException("Unable to set site resource properties",e);
		}
	}

	public void create(Site site) throws ResourceException
	{
		info("Creating site '"+site.getName()+"'");
		
		// Set state
		site.setState(SiteState.NEW);
		site.setShortMessage("Created");
		Calendar time = Calendar.getInstance();
		site.setLastUpdate(time);
		site.setCreated(time);
			
		// Check for Condor version and set reasonable default
		String ver = site.getCondorVersion();
		ver = "".equalsIgnoreCase(ver) ? null : ver;
		String pkg = site.getCondorPackage();
		pkg = "".equalsIgnoreCase(pkg) ? null : pkg;
		if (ver==null && pkg==null) {
			site.setCondorVersion("7.0.0");
		}
		
		// Eliminate empty strings
		ExecutionService glideinService = site.getGlideinService();
		if ("".equals(glideinService.getQueue()))
			glideinService.setQueue(null);
		if ("".equals(glideinService.getProject()))
			glideinService.setProject(null);
		
		// Eliminate empty strings
		ExecutionService stagingService = site.getStagingService();
		if ("".equals(stagingService.getQueue()))
			stagingService.setQueue(null);
		if ("".equals(stagingService.getProject()))
			stagingService.setProject(null);
		
		// Save site in database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.create(site);
			dao.insertHistory(site.getId(), site.getState(), site.getLastUpdate());
		} catch (DatabaseException de) {
			throw new ResourceException("Unable to create site", de);
		}
		
		// Set site
		setSite(site);
	}

	public void load(ResourceKey key) throws ResourceException
	{
		info("Loading site resource "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			setSite(dao.load(id));
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to load site",de);
		}
	}
	
	public void store() throws ResourceException
	{
		throw new UnsupportedOperationException();
	}

	public void remove(boolean force, EndpointReferenceType credentialEPR)
	throws ResourceException
	{
		info("Creating remove event");
		
		// Choose new state. If force, then just delete the record from
		// the database and remove the resource, otherwise go through the
		// removal process.
		SiteEventCode code;
		if (force) {
			code = SiteEventCode.DELETE;
		} else {
			code = SiteEventCode.REMOVE;
		}
		
		// Schedule remove event
		try {
			Event event = new SiteEvent(code,Calendar.getInstance(),getKey());
			if (!force) {
				DelegationResource delegationResource = 
					DelegationUtil.getDelegationResource(credentialEPR);
				GlobusCredential credential = 
					delegationResource.getCredential();
				event.setProperty("credential", credential);
			}
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get event queue",ne);
		} catch (DelegationException de) {
			throw new ResourceException("Unable to get delegated credential",de);
		}
	}
	
	public void submit(EndpointReferenceType credentialEPR) throws ResourceException 
	{
		info("Creating submit event");
		
		// Schedule submit event
		try {
			
			// Get delegated credential
			DelegationResource delegationResource = 
				DelegationUtil.getDelegationResource(credentialEPR);
			GlobusCredential credential = 
				delegationResource.getCredential();
			
			// Validate that there is enough time left on the credential
			validateCredentialLifetime(credential);
			
			Event event = new SiteEvent(SiteEventCode.SUBMIT,Calendar.getInstance(),getKey());
			event.setProperty("credential", credential);
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get event queue",ne);
		} catch (DelegationException de) {
			throw new ResourceException("Unable to get delegated credential",de);
		}
	}
	
	private void updateState(SiteState state, String shortMessage, String longMessage, Calendar time)
	throws ResourceException
	{
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
			// TODO: Prevent duplicate history entries during state recovery
			// This may be done by preventing handleEvent from processing the same
			// event twice.
			dao.insertHistory(site.getId(), state, time);
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to change state to "+state,de);
		}
	}
	
	private ServiceConfiguration getConfig() throws ResourceException
	{
		try {
			return ServiceConfiguration.getInstance();
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get service configuration",ne);
		}
	}
	
	private File getWorkingDirectory() throws ResourceException
	{
		// Determine the directory path
		File dir = new File(getConfig().getWorkingDirectory(),"site-"+site.getId());
	
		// Create the directory if it doesn't exist
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				throw new ResourceException(
						"Working directory is not a directory");
			}
		} else {
			try {
				if (!dir.mkdirs()) {
					throw new ResourceException(
							"Unable to create working directory");
				}
			} catch (SecurityException e) {
				throw new ResourceException(
						"Unable to create working directory");
			}
		}
		
		return dir;
	}
	
	private File getInstallDirectory() throws ResourceException
	{
		return new File(getWorkingDirectory(),"install");
	}
	
	private File getUninstallDirectory() throws ResourceException
	{
		return new File(getWorkingDirectory(),"uninstall");
	}
	
	private void submitInstallJob() throws ResourceException
	{
		info("Submitting install job");
		
		ServiceConfiguration config = getConfig();
		
		// Get job directory
		File jobDirectory = getInstallDirectory();
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory);
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT2);
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(stagingService.getServiceContact());
		job.setProject(stagingService.getProject());
		job.setQueue(stagingService.getQueue());
		
		// Set glidein_install executable
		String install = config.getInstall();
		job.setExecutable(install);
		job.setLocalExecutable(true);
		job.setMaxTime(300); // Not longer than 5 mins
		
		// Set credential
		GlobusCredential cred = loadCredential();
		if (validateCredentialLifetime(cred)) {
			job.setCredential(cred);
		} else {
			throw new ResourceException("Not enough time left on credential");
		}
		
		// Add environment
		EnvironmentVariable env[] = site.getEnvironment();
		if (env!=null) {
			for (EnvironmentVariable var : env)
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		if (site.getCondorPackage()==null) {
			job.addArgument("-condorVersion "+site.getCondorVersion());
		} else {
			job.addArgument("-condorPackage "+site.getCondorPackage());
		}
		String[] urls = config.getStagingURLs();
		for(String url : urls) job.addArgument("-url "+url);
		
		// Add a listener
		job.addListener(new InstallSiteListener(getKey()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new ResourceException("Unable to submit install job",ce);
		}
	}
	
	private boolean hasGlideins() throws ResourceException
	{
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			return dao.hasGlideins(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to check for glideins",de);
		}
	}
	
	private void cancelGlideins() throws ResourceException
	{
		info("Canceling glideins");
		
		// Get glidein ids
		int[] ids;
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			ids = dao.getGlideinIds(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to get glidein ids",de);
		}
		
		// Create a remove event for each glidein
		try {
			EventQueue queue = EventQueue.getInstance();
			for (int id : ids) {
				ResourceKey key = AddressingUtil.getGlideinKey(id);
				Event event = new GlideinEvent(
						GlideinEventCode.REMOVE,site.getLastUpdate(),key);
				queue.add(event);
			}
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get event queue",ne);
		}
	}
	
	private void notifyGlideinsOfReady() throws ResourceException
	{
		notifyGlideins(SiteState.READY);
	}
	
	private void notifyGlideinsOfFailed() throws ResourceException
	{
		notifyGlideins(SiteState.FAILED);
	}
	
	private void notifyGlideins(SiteState state) throws ResourceException
	{
		info("Notifying site glideins of "+state+" state");
		
		// Get glidein ids
		int[] ids;
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			ids = dao.getGlideinIds(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to get glidein ids",de);
		}
		
		// Create a ready event for each glidein
		try {
			EventQueue queue = EventQueue.getInstance();
			for (int id : ids) {
				ResourceKey key = AddressingUtil.getGlideinKey(id);
				GlideinEventCode code;
				if (SiteState.READY.equals(state)) {
					code = GlideinEventCode.SITE_READY;
				} else if (SiteState.FAILED.equals(state)) {
					code = GlideinEventCode.SITE_FAILED;
				} else {
					throw new IllegalStateException("Unhandled state: "+state);
				}
				Event event = new GlideinEvent(code,site.getLastUpdate(),key);
				queue.add(event);
			}
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get queue: "+
					ne.getMessage(),ne);
		}
	}
	
	private void cancelInstallJob() throws ResourceException
	{
		info("Canceling install job");
		try {
			String jobid = readJobId(getInstallDirectory());
			Condor.getInstance().cancelJob(jobid);
		} catch (CondorException ce) {
			throw new ResourceException("Unable to cancel install job",ce);
		}
	}
	
	private String readJobId(File jobDirectory) throws ResourceException
	{
		File jobidFile = new File(jobDirectory,"jobid");
		try {
			// Read job id from jobid file
			BufferedReader reader = new BufferedReader(
					new FileReader(jobidFile));
			String jobid = reader.readLine();
			reader.close();
			
			return jobid;
		} catch (IOException ioe) {
			throw new ResourceException("Unable to read job id",ioe);
		}
	}
	
	private File getCredentialFile() throws ResourceException
	{
		File dir = getWorkingDirectory();
		File credFile = new File(dir,"credential");
		return credFile;
	}
	
	private void storeCredential(GlobusCredential credential)
	throws ResourceException
	{
		info("Storing credential");
		try {
			CredentialUtil.store(credential,getCredentialFile());
		} catch (IOException e) {
			throw new ResourceException("Unable to store credential",e);
		}
	}
	
	private boolean validateCredentialLifetime(GlobusCredential credential)
	throws ResourceException
	{
		// Require at least 5 minutes
		long need = 300;
		return (need < credential.getTimeLeft());
	}
	
	private GlobusCredential loadCredential() 
	throws ResourceException
	{
		info("Loading credential");
		try {
			return CredentialUtil.load(getCredentialFile());
		} catch (IOException e) {
			throw new ResourceException("Unable to load credential",e);
		}
	}
	
	private void submitUninstallJob() throws ResourceException
	{
		info("Submitting uninstall job");
		
		ServiceConfiguration config = getConfig();
		
		// Get job directory
		File jobDirectory = getUninstallDirectory();
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory);
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT2);
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(stagingService.getServiceContact());
		job.setProject(stagingService.getProject());
		job.setQueue(stagingService.getQueue());
		
		// Set glidein_uninstall executable
		String uninstall = config.getUninstall();
		job.setExecutable(uninstall);
		job.setLocalExecutable(true);
		job.setMaxTime(300); // Not longer than 5 mins
		
		GlobusCredential cred = loadCredential();
		if (validateCredentialLifetime(cred)) {
			job.setCredential(cred);
		} else {
			throw new ResourceException("Not enough time on credential");
		}
		
		// Add environment
		EnvironmentVariable env[] = site.getEnvironment();
		if (env!=null) {
			for (EnvironmentVariable var : env)
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		
		// Add a listener
		job.addListener(new UninstallSiteListener(getKey()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new ResourceException("Unable to submit uninstall job",ce);
		}
	}
	
	private void deleteFromDatabase() throws ResourceException
	{
		info("Deleting site from database");
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to delete site",de);
		}
	}
	
	private void delete() throws ResourceException
	{
		// Remove the Resource from the resource home
		try {
			SiteResourceHome resourceHome = SiteResourceHome.getInstance();
			resourceHome.remove(getKey());
		} catch (NamingException e) {
			throw new ResourceException("Unable to locate SiteResourceHome",e);
		}
		
		// Delete the site from the database
		deleteFromDatabase();
		
		// Remove the working directory and all sub-directories
		try {
			IOUtil.rmdirs(getWorkingDirectory());
		} catch (ResourceException re) {
			throw new ResourceException("Unable to remove working directory",re);
		}
	}
	
	private void fail(String message, Exception exception, Calendar time) throws ResourceException
	{
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
	
	private void failQuietly(String message, Exception exception, Calendar time)
	{
		try {
			fail(message,exception,time);
		} catch (ResourceException re) {
			error("Unable to change state to "+SiteState.FAILED,re);
		}
	}
	
	public synchronized void handleEvent(SiteEvent event)
	{
		try {
			_handleEvent(event);
		} catch(ResourceException re) {
			// RemoteException tacks the cause on to the end of
			// the message. We don't want that in the database.
			// Instead, the database stores the entire stack trace
			// in the long message.
			String message = re.getMessage();
			int cause = message.indexOf("; nested exception is:");
			if (cause > 0) {
				failQuietly(message.substring(0, cause), re, event.getTime());
			} else {
				failQuietly(message, re, event.getTime());
			}
		}
	}
	
	private void _handleEvent(SiteEvent event) throws ResourceException
	{
		SiteState state = site.getState();
		SiteEventCode code = (SiteEventCode)event.getCode();
		
		// If the site was deleted, then we can just ignore the event
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
						
					// Store delegated credential
					GlobusCredential credential = 
						(GlobusCredential)event.getProperty("credential");
					storeCredential(credential);
						
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
					} catch (ResourceException re) {
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
				
				// Store the credential for later. We have to do this because
				// it is possible that there will be glideins that we need to
				// wait for. If that happens then the event with the credential
				// attached won't be available when we need to submit the
				// uninstall job.
				GlobusCredential credential = 
					(GlobusCredential)event.getProperty("credential");
				storeCredential(credential);
				
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
					
				} else {
					/* This is expected to happen frequently */
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
	
	public synchronized void recoverState() throws InitializeException
	{
		try {
			
			// Try to recover state
			_recoverState();
			
		} catch (ResourceException re) {
			try {
				
				// If recovery fails, try to update the state to failed
				fail("State recovery failed",re,Calendar.getInstance());
				
			} catch (ResourceException re2) {
				
				// If that fails, then fail the entire recovery process
				throw new InitializeException(
						"Unable to recover site "+site.getId(),re);
				
			}
		}
	}
	
	private void _recoverState() throws ResourceException
	{
		info("Recovering state");
		
		SiteState state = site.getState();
		if (SiteState.NEW.equals(state)) {
			
			/* Do nothing */
			
		} else if (SiteState.STAGING.equals(state)) {
			
			// If the current state is STAGING, then the job could be 
			// finished, running, ready to submit, or not ready
			
			File jobDir = getInstallDirectory();
			CondorJob job = new CondorJob(jobDir);
			
			if (job.getLog().exists()) {
				
				// If the install log exists, then recover the install job
				job.setJobId(readJobId(jobDir));
				InstallSiteListener listener = 
					new InstallSiteListener(getKey());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialFile().exists()) {
				
				// If the credential file still exists, 
				// try to submit the install job
				submitInstallJob();
				
			} else {
				
				// Otherwise fail the site
				fail("Unable to recover site",null,Calendar.getInstance());
				
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
			CondorJob job = new CondorJob(jobDir);
			
			if (job.getLog().exists()) {
				
				// If the log file exists, try to recover the job
				job.setJobId(readJobId(jobDir));
				UninstallSiteListener listener = 
					new UninstallSiteListener(getKey());
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
						null, Calendar.getInstance());
				
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
	
	private String logPrefix()
	{
		if (site == null) {
			return "";
		} else {
			return "Site "+site.getId()+": ";
		}
	}
	public void debug(String message)
	{
		debug(message,null);
	}
	public void debug(String message, Throwable t)
	{
		logger.debug(logPrefix()+message, t);
	}
	public void info(String message)
	{
		info(message,null);
	}
	public void info(String message, Throwable t)
	{
		logger.info(logPrefix()+message,t);
	}
	public void warn(String message)
	{
		warn(message,null);
	}
	public void warn(String message, Throwable t)
	{
		logger.warn(logPrefix()+message,t);
	}
	public void error(String message)
	{
		error(message,null);
	}
	public void error(String message, Throwable t)
	{
		logger.error(logPrefix()+message,t);
	}
}
