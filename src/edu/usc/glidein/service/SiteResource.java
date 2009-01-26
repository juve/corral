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
package edu.usc.glidein.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Calendar;
import java.util.HashSet;

import javax.naming.NamingException;
import javax.security.auth.Subject;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.log4j.Logger;
import org.globus.delegation.DelegationException;
import org.globus.delegation.DelegationUtil;
import org.globus.delegation.service.DelegationResource;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.jaas.GlobusPrincipal;
import org.globus.util.Util;
import org.globus.wsrf.PersistenceCallback;
import org.globus.wsrf.Resource;
import org.globus.wsrf.ResourceException;
import org.globus.wsrf.ResourceIdentifier;
import org.globus.wsrf.ResourceKey;
import org.globus.wsrf.ResourceProperties;
import org.globus.wsrf.ResourcePropertySet;
import org.globus.wsrf.Topic;
import org.globus.wsrf.TopicList;
import org.globus.wsrf.TopicListAccessor;
import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.globus.wsrf.impl.ReflectionResourceProperty;
import org.globus.wsrf.impl.SimpleResourceKey;
import org.globus.wsrf.impl.SimpleResourcePropertySet;
import org.globus.wsrf.impl.SimpleTopic;
import org.globus.wsrf.impl.SimpleTopicList;
import org.globus.wsrf.impl.security.authorization.exceptions.InitializeException;
import org.globus.wsrf.security.SecurityException;
import org.globus.wsrf.utils.SubscriptionPersistenceUtils;
import org.xml.sax.InputSource;

import edu.usc.glidein.condor.Condor;
import edu.usc.glidein.condor.CondorEventGenerator;
import edu.usc.glidein.condor.CondorException;
import edu.usc.glidein.condor.CondorGridType;
import edu.usc.glidein.condor.CondorJob;
import edu.usc.glidein.condor.CondorUniverse;
import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.SiteDAO;
import edu.usc.glidein.nl.NetLogger;
import edu.usc.glidein.nl.NetLoggerEvent;
import edu.usc.glidein.nl.NetLoggerException;
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
import edu.usc.glidein.stubs.types.SiteStateChange;
import edu.usc.glidein.stubs.types.SiteStateChangeMessage;
import edu.usc.glidein.util.AddressingUtil;
import edu.usc.glidein.util.AuthenticationUtil;
import edu.usc.glidein.util.FilesystemUtil;

public class SiteResource implements Resource, ResourceIdentifier, PersistenceCallback, 
	ResourceProperties, TopicListAccessor
{
	private final Logger logger = Logger.getLogger(SiteResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Topic stateChangeTopic;
	private TopicList topicList;
	private Site site;
	
	public SiteResource()
	{
		resourceProperties = new SimpleResourcePropertySet(SiteNames.RESOURCE_PROPERTIES);
		stateChangeTopic = new SimpleTopic(SiteNames.TOPIC_STATE_CHANGE);
		topicList = new SimpleTopicList(this);
		topicList.addTopic(stateChangeTopic);
	}
	
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
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_ID,"id",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_NAME,"name",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_INSTALL_PATH,"installPath",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_LOCAL_PATH,"localPath",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_CONDOR_PACKAGE,"condorPackage",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_CONDOR_VERSION,"condorVersion",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_STATE,"state",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_SHORT_MESSAGE,"shortMessage",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_LONG_MESSAGE,"longMessage",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_CREATED,"created",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_LAST_UPDATE,"lastUpdate",site));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_STAGING_SERVICE_CONTACT,
					"serviceContact",site.getStagingService()));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_STAGING_SERVICE_TYPE,
					"serviceType",site.getStagingService()));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_STAGING_PROJECT,
					"project",site.getStagingService()));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_STAGING_QUEUE,
					"queue",site.getStagingService()));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_GLIDEIN_SERVICE_CONTACT,
					"serviceContact",site.getGlideinService()));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_GLIDEIN_SERVICE_TYPE,
					"serviceType",site.getGlideinService()));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_GLIDEIN_PROJECT,
					"project",site.getGlideinService()));
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_GLIDEIN_QUEUE,
					"queue",site.getGlideinService()));
		} catch (Exception e) {
			throw new RuntimeException("Unable to set site resource properties",e);
		}
	}
	
	public TopicList getTopicList()
	{
		return topicList;
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
		
		// Must have staging service
		ExecutionService stagingService = site.getStagingService();
		if (stagingService == null)
			throw new ResourceException("Must provide staging service");
		
		// Must have glidein service
		ExecutionService glideinService = site.getGlideinService();
		if (glideinService == null)
			throw new ResourceException("Must provide glidein service");
		
		// Eliminate empty strings
		if ("".equals(site.getName()))
			site.setName(null);
		
		if ("".equals(site.getCondorVersion()))
			site.setCondorVersion(null);
		
		if ("".equals(site.getCondorPackage()))
			site.setCondorPackage(null);
		
		if ("".equals(glideinService.getServiceContact()))
			glideinService.setServiceContact(null);
		
		if ("".equals(glideinService.getQueue()))
			glideinService.setQueue(null);
		
		if ("".equals(glideinService.getProject()))
			glideinService.setProject(null);
		
		if ("".equals(stagingService.getServiceContact()))
			stagingService.setServiceContact(null);
		
		if ("".equals(stagingService.getQueue()))
			stagingService.setQueue(null);
		
		if ("".equals(stagingService.getProject()))
			stagingService.setProject(null);
		
		// Must have name
		if (site.getName() == null)
			throw new ResourceException("Site must have name");
		
		// Check glidein service
		if (glideinService.getServiceContact() == null || 
				glideinService.getServiceType() == null)
			throw new ResourceException("Invalid glidein service: " +
					"must specify service contact and service type");
		
		// Check staging service
		if (stagingService.getServiceContact() == null || 
				stagingService.getServiceType() == null)
			throw new ResourceException("Invalid staging service: " +
					"must specify service contact and service type");
		
		// Must specify condorPackage or condorVersion
		if (site.getCondorPackage() == null && site.getCondorVersion() == null)
			throw new ResourceException(
					"Must specify condor package or condor version");
		
		// Check install path
		if (site.getInstallPath() == null)
			throw new ResourceException("Must specify install path");
		
		// Check local path
		if (site.getLocalPath() == null) 
			throw new ResourceException("Must specify local path");
		
		try {
			// Authenticate the client
			AuthenticationUtil.authenticate();
			
			// Set subject and username
			site.setSubject(AuthenticationUtil.getSubject());
			site.setLocalUsername(AuthenticationUtil.getLocalUsername());
		} catch (SecurityException se) {
			throw new ResourceException("Unable to authenticate client",se);
		}
		
		// Save site in database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.create(site);
			dao.insertHistory(site.getId(), site.getState(), site.getLastUpdate());
		} catch (DatabaseException de) {
			throw new ResourceException("Unable to create site", de);
		}
		
		// Log it in the netlogger log
		try {
			NetLoggerEvent event = new NetLoggerEvent("site.new");
			event.setTimeStamp(site.getCreated().getTime());
			event.put("site.id", site.getId());
			event.put("name", site.getName());
			event.put("install_path", site.getInstallPath());
			event.put("local_path", site.getLocalPath());
			event.put("staging.type", stagingService.getServiceType());
			event.put("staging.contact", stagingService.getServiceContact());
			event.put("staging.queue", stagingService.getQueue());
			event.put("staging.project", stagingService.getProject());
			event.put("glidein.type", glideinService.getServiceType());
			event.put("glidein.contact", glideinService.getServiceContact());
			event.put("glidein.queue", glideinService.getQueue());
			event.put("glidein.project", glideinService.getProject());
			event.put("condor.version", site.getCondorVersion());
			event.put("condor.package", site.getCondorPackage());
			event.put("owner.subject",site.getSubject());
			event.put("owner.username", site.getLocalUsername());
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			warn("Unable to log site event to NetLogger log",nle);
		}
		
		// Set site
		setSite(site);
	}

	public void load(ResourceKey key) throws ResourceException
	{
		info("Loading site resource "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		loadSite(id);
		loadListeners();
	}
	
	public void store() throws ResourceException
	{
		info("Storing site resource "+site.getId());
		storeListeners();
		storeSite();
	}
	
	private void loadSite(int id) throws ResourceException
	{
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			setSite(dao.load(id));
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to load site",de);
		}
	}
	
	private void storeSite() throws ResourceException
	{
		/* Do nothing */
	}
	
	private void storeListeners() throws ResourceException
	{
		// TODO: Store listeners in database
		File listeners = getListenersFile();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(listeners));
			SubscriptionPersistenceUtils.storeSubscriptionListeners(topicList, oos);
			oos.close();
		} catch (Exception e) {
			throw new ResourceException("Unable to store site listeners",e);
		}
	}
	
	private void loadListeners() throws ResourceException
	{
		File listeners = getListenersFile();
		if (listeners.exists() && listeners.isFile()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(listeners));
				SubscriptionPersistenceUtils.loadSubscriptionListeners(topicList, ois);
				ois.close();
			} catch (Exception e) {
				throw new ResourceException("Unable to load site listeners",e);
			}
		}
	}

	private File getListenersFile() throws ResourceException
	{
		File work = getWorkingDirectory();
		if (!work.exists()) {
			work.mkdirs();
		}
		File listeners = new File(work,"listeners");
		return listeners;
	}
	
	public void remove(boolean force, EndpointReferenceType credentialEPR)
	throws ResourceException
	{
		info("Creating remove event");
		
		authorize();
		
		// Choose new state. If force, then just delete the record from
		// the database and remove the resource, otherwise go through the
		// removal process.
		SiteEventCode code;
		if (force) {
			code = SiteEventCode.DELETE;
		} else {
			code = SiteEventCode.REMOVE;
		}
		
		// Store the credential endpoint
		storeCredentialEPR(credentialEPR);
		
		// Schedule remove event
		try {
			Event event = new SiteEvent(code,Calendar.getInstance(),getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get event queue",ne);
		}
	}
	
	public void submit(EndpointReferenceType credentialEPR) throws ResourceException 
	{
		info("Creating submit event");
		
		authorize();
		
		// Create working directory
		File work = getWorkingDirectory();
		if (!work.exists()) {
			work.mkdirs();
		}
		
		// Store the credential endpoint
		storeCredentialEPR(credentialEPR);
		
		// Schedule submit event
		try {
			Event event = new SiteEvent(SiteEventCode.SUBMIT,Calendar.getInstance(),getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get event queue",ne);
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
			dao.insertHistory(site.getId(), state, time);
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to change state to "+state,de);
		}
		
		// Notify topic listeners
		info("Notifying listeners of state change");
		try {
			SiteStateChange stateChange = new SiteStateChange();
			stateChange.setSiteId(site.getId());
			stateChange.setState(state);
			stateChange.setShortMessage(shortMessage);
	        stateChange.setLongMessage(longMessage);
	        stateChange.setTime(time);
	        
	        stateChangeTopic.notify(new SiteStateChangeMessage(stateChange));
		} catch (Exception e) {
			warn("Unable to notify topic listeners", e);
		}
		
		// Log it in the netlogger log
		try {
			NetLoggerEvent event = new NetLoggerEvent("site."+state.toString().toLowerCase());
			event.setTimeStamp(time.getTime());
			event.put("site.id", site.getId());
			event.put("message", shortMessage);
			
			NetLogger netlogger = NetLogger.getLog();
			netlogger.log(event);
		} catch (NetLoggerException nle) {
			warn("Unable to log site event to NetLogger log",nle);
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
		return new File(getConfig().getWorkingDirectory(),"site-"+site.getId());
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
		CondorJob job = new CondorJob(jobDirectory,site.getLocalUsername());
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService stagingService = site.getStagingService();
		if(ServiceType.GT2.equals(stagingService.getServiceType())) {
			job.setGridType(CondorGridType.GT2);
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(stagingService.getServiceContact());
		
		// Set rsl/xml
		if (ServiceType.GT2.equals(stagingService.getServiceType())) {
			// GT2 uses globus_rsl
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
		GlobusCredential cred = getDelegatedCredential();
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
		job.addArgument("-rls "+config.getRls());
		
		// Add status output file
		job.addOutputFile("status");
		
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
			CondorJob job = new CondorJob(getInstallDirectory(),site.getLocalUsername());
			String jobid = readJobId(getInstallDirectory());
			job.setJobId(jobid);
			Condor.getInstance().cancelJob(job);
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
	
	private File getCredentialEPRFile() throws ResourceException
	{
		File dir = getWorkingDirectory();
		File credFile = new File(dir,"credential.epr");
		return credFile;
	}
	
	private void storeCredentialEPR(EndpointReferenceType epr)
	throws ResourceException
	{
		info("Storing credential EPR");
		// TODO: Store EPR in the database
		synchronized (this) {
			try {
				String endpointString = ObjectSerializer.toString(
						epr, EndpointReferenceType.getTypeDesc().getXmlType());
				File file = getCredentialEPRFile();
				BufferedWriter writer = new BufferedWriter(
						new FileWriter(file));
				writer.write(endpointString);
				writer.close();
				Util.setFilePermissions(file.getAbsolutePath(), 600);
			} catch (Exception e) {
				throw new ResourceException("Unable to store credential EPR",e);
			}
		}
	}
	
	private boolean validateCredentialLifetime(GlobusCredential credential)
	throws ResourceException
	{
		// Require at least 5 minutes
		long need = 300;
		return (need < credential.getTimeLeft());
	}
	
	private EndpointReferenceType loadCredentialEPR() 
	throws ResourceException
	{
		info("Loading credential EPR");
		synchronized (this) {
			try {
				FileInputStream fis = new FileInputStream(getCredentialEPRFile());
				EndpointReferenceType epr = (EndpointReferenceType)
					ObjectDeserializer.deserialize(
							new InputSource(fis), EndpointReferenceType.class);
				fis.close();
				return epr;
			} catch (Exception e) {
				throw new ResourceException("Unable to load credential EPR",e);
			}
		}
	}
	
	private GlobusCredential getDelegatedCredential() throws ResourceException
	{
		info("Retrieving delegated credential");
		try {
			// Load the endpoint reference
			EndpointReferenceType epr = loadCredentialEPR();
			
			// Create subject for authorization
			Principal principal = new GlobusPrincipal(site.getSubject());
			HashSet<Principal> principals = new HashSet<Principal>();
			principals.add(principal);
			Subject subject = new Subject(
					false,principals,new HashSet(),new HashSet());
			
			// Get delegated credential
			DelegationResource delegationResource = 
				DelegationUtil.getDelegationResource(epr);
			GlobusCredential credential = 
				delegationResource.getCredential(subject);
			org.globus.wsrf.security.SecurityManager.getManager().getCaller();
			return credential;
		} catch (DelegationException de) {
			throw new ResourceException("Unable to get delegated credential",de);
		}
	}
	
	private void submitUninstallJob() throws ResourceException
	{
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
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(stagingService.getServiceContact());
		
		// Set rsl/xml
		if (ServiceType.GT2.equals(stagingService.getServiceType())) {
			// GT2 uses globus_rsl
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
		
		GlobusCredential cred = getDelegatedCredential();
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
		job.addArgument("-localPath "+site.getLocalPath());
		
		// Add status output file
		job.addOutputFile("status");
		
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
		FilesystemUtil.rm(getWorkingDirectory());
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
	
	private void _handleEvent(SiteEvent event) throws ResourceException
	{
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
			CondorJob job = new CondorJob(jobDir, site.getLocalUsername());
			
			if (job.getLog().exists()) {
				
				// If the install log exists, then recover the install job
				job.setJobId(readJobId(jobDir));
				InstallSiteListener listener = 
					new InstallSiteListener(getKey());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialEPRFile().exists()) {
				
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
			CondorJob job = new CondorJob(jobDir, site.getLocalUsername());
			
			if (job.getLog().exists()) {
				
				// If the log file exists, try to recover the job
				job.setJobId(readJobId(jobDir));
				UninstallSiteListener listener = 
					new UninstallSiteListener(getKey());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialEPRFile().exists()) {
				
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
	
	private void authorize() throws ResourceException
	{
		try {
			// Authenticate the client
			AuthenticationUtil.authenticate();
			
			// Set subject and username
			String subject = AuthenticationUtil.getSubject();
			if (subject == null || !subject.equals(site.getSubject())) {
				throw new ResourceException(
						"Subject "+subject+" is not authorized to modify site "+site.getId());
			}
		} catch (SecurityException se) {
			throw new ResourceException("Unable to authenticate client",se);
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
