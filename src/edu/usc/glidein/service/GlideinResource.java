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
import edu.usc.glidein.db.GlideinDAO;
import edu.usc.glidein.service.state.Event;
import edu.usc.glidein.service.state.EventQueue;
import edu.usc.glidein.service.state.GlideinEvent;
import edu.usc.glidein.service.state.GlideinEventCode;
import edu.usc.glidein.service.state.GlideinListener;
import edu.usc.glidein.service.state.SiteEvent;
import edu.usc.glidein.service.state.SiteEventCode;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinState;
import edu.usc.glidein.stubs.types.GlideinStateChange;
import edu.usc.glidein.stubs.types.GlideinStateChangeMessage;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;
import edu.usc.glidein.util.AddressingUtil;
import edu.usc.glidein.util.AuthenticationUtil;
import edu.usc.glidein.util.Base64;
import edu.usc.glidein.util.FilesystemUtil;
import edu.usc.glidein.util.IOUtil;

public class GlideinResource implements Resource, ResourceIdentifier, 
	PersistenceCallback, ResourceProperties, TopicListAccessor
{
	private Logger logger = Logger.getLogger(GlideinResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Topic stateChangeTopic;
	private TopicList topicList;
	private Glidein glidein = null;
	
	/**
	 * Default constructor required
	 */
	public GlideinResource()
	{
		resourceProperties = new SimpleResourcePropertySet(SiteNames.RESOURCE_PROPERTIES);
		stateChangeTopic = new SimpleTopic(GlideinNames.TOPIC_STATE_CHANGE);
		topicList = new SimpleTopicList(this);
		topicList.addTopic(stateChangeTopic);
	}

	public void setGlidein(Glidein glidein)
	{
		this.glidein = glidein;
		setResourceProperties();
	}
	
	public Glidein getGlidein()
	{
		return glidein;
	}
	
	public Object getID()
	{
		return getKey();
	}
	
	public ResourceKey getKey()
	{
		if (glidein == null) return null;
		return new SimpleResourceKey(
				GlideinNames.RESOURCE_KEY,
				new Integer(glidein.getId()));
	}
	
	public TopicList getTopicList()
	{
		return topicList;
	}
	
	public ResourcePropertySet getResourcePropertySet()
	{
		return resourceProperties;
	}
	
	private void setResourceProperties()
	{
		try {
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_ID,"id",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_SITE,"siteId",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_CONDOR_HOST,"condorHost",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_COUNT,"count",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_HOST_COUNT,"hostCount",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_WALL_TIME,"wallTime",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_NUM_CPUS,"numCpus",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_CONDOR_CONFIG,"condorConfig",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_GCB_BROKER,"gcbBroker",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_IDLE_TIME,"idleTime",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_CONDOR_DEBUG,"condorDebug",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_STATE,"state",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_SHORT_MESSAGE,"shortMessage",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_LONG_MESSAGE,"longMessage",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_CREATED,"created",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_LAST_UPDATE,"lastUpdate",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_RESUBMIT,"resubmit",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_RESUBMITS,"resubmits",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_UNTIL,"until",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_SUBMITS,"submits",glidein));
			resourceProperties.add(new ReflectionResourceProperty(
					GlideinNames.RP_RSL,"rsl",glidein));
		} catch(Exception e) {
			throw new RuntimeException("Unable to set glidein resource properties",e);
		}
	}
	
	public void create(Glidein glidein) throws ResourceException
	{
		info("Creating glidein for site "+glidein.getSiteId());
		
		// Initialize glidein
		glidein.setState(GlideinState.NEW);
		glidein.setShortMessage("Created");
		Calendar time = Calendar.getInstance();
		glidein.setLastUpdate(time);
		glidein.setCreated(time);
		glidein.setSubmits(0);
		
		// Validate glidein
		
		// Wall time must be >= 2 minutes because when we submit the glidein
		// we are going to subtract 1 minute to allow 1 minute for the glidein
		// to shut itself down before the local scheduler kills it
		if (glidein.getWallTime() < 2) {
			throw new ResourceException("Wall time must be >= 2 minutes");
		}
		
		try {
			// Authenticate the client
			AuthenticationUtil.authenticate();
			
			// Set subject and username
			glidein.setSubject(AuthenticationUtil.getSubject());
			glidein.setLocalUsername(AuthenticationUtil.getLocalUsername());
		} catch (SecurityException se) {
			throw new ResourceException("Unable to authenticate client",se);
		}
		
		// Get site or fail
		SiteResource siteResource = getSiteResource(glidein.getSiteId());
		Site site = siteResource.getSite();
		
		// Synchronize on the site resource to prevent state changes while
		// we are checking it and saving the glidein
		synchronized (siteResource) {
			
			// Check to make sure the site is in an 
			// appropriate state for creating a glidein
			SiteState siteState = site.getState();
			if (SiteState.FAILED.equals(siteState) || 
				SiteState.EXITING.equals(siteState) ||
				SiteState.REMOVING.equals(siteState)) {
				
				throw new ResourceException(
						"Site cannot be in "+siteState+
						" when creating a glidein");
				
			}
			
			// Set the name
			glidein.setSiteName(site.getName());
		
			// Save in the database
			try {
				Database db = Database.getDatabase();
				GlideinDAO dao = db.getGlideinDAO();
				dao.create(glidein);
				dao.insertHistory(glidein.getId(), glidein.getState(), glidein.getLastUpdate());
			} catch (DatabaseException dbe) {
				throw new ResourceException("Unable to create glidein",dbe);
			}
			
			// Set glidein
			setGlidein(glidein);
		}
	}

	public void load(ResourceKey key) throws ResourceException
	{
		info("Loading glidein resource "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		loadGlidein(id);
		loadListeners();
	}
	
	public void store() throws ResourceException 
	{
		info("Storing glidein resource "+glidein.getId());
		storeListeners();
		storeGlidein();
	}
	
	private void loadGlidein(int id) throws ResourceException
	{
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			setGlidein(dao.load(id));
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to load glidein",de);
		}
	}
	
	private void storeGlidein() throws ResourceException
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
			throw new ResourceException("Unable to store glidein listeners",e);
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
				throw new ResourceException("Unable to load glidein listeners",e);
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
	
	public void remove(boolean force) throws ResourceException 
	{
		info("Removing glidein");
		
		authorize();
		
		// If we are forcing the issue, then just delete it
		GlideinEventCode code = null;
		if (force) {
			code = GlideinEventCode.DELETE;
		} else {
			code = GlideinEventCode.REMOVE;
		}
		
		// Queue the event
		try {
			Event event = new GlideinEvent(code,Calendar.getInstance(),getKey());
			EventQueue queue = EventQueue.getInstance(); 
			queue.add(event);
		} catch(NamingException ne) {
			throw new ResourceException("Unable to remove glidein",ne);
		}
	}

	public void submit(EndpointReferenceType credentialEPR) throws ResourceException
	{
		info("Submitting glidein");
		
		authorize();
		
		// Create the working directory
		File work = getWorkingDirectory();
		if (!work.exists()) {
			work.mkdirs();
		}
		
		// Store the credential endpoint
		storeCredentialEPR(credentialEPR);
		
		try {
			// Create submit event
			Event event = new GlideinEvent(GlideinEventCode.SUBMIT,Calendar.getInstance(),getKey());
			EventQueue queue = EventQueue.getInstance(); 
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get event queue",ne);
		}
	}
	
	private void updateState(GlideinState state, String shortMessage, String longMessage, Calendar time) 
	throws ResourceException
	{
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
			dao.insertHistory(glidein.getId(), glidein.getState(), glidein.getLastUpdate());
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to update state to "+state,de);
		}
	
		// Notify topic listeners
		info("Notifying listeners of state change");
		try {
			GlideinStateChange stateChange = new GlideinStateChange();
			stateChange.setGlideinId(glidein.getId());
			stateChange.setState(state);
			stateChange.setShortMessage(shortMessage);
	        stateChange.setLongMessage(longMessage);
	        stateChange.setTime(time);
	        
	        stateChangeTopic.notify(new GlideinStateChangeMessage(stateChange));
		} catch (Exception e) {
			warn("Unable to notify topic listeners", e);
		}
	}
	
	private void submitGlideinJob() throws ResourceException
	{
		info("Submitting glidein job");
		
		SiteResource siteResource = getSiteResource(glidein.getSiteId());
		Site site = siteResource.getSite();
		
		// Get configuration
		ServiceConfiguration config;
		try {
			config = ServiceConfiguration.getInstance();
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get service configuration",ne);
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
		} else {
			job.setGridType(CondorGridType.GT4);
		}
		job.setGridContact(glideinService.getServiceContact());
		
		// Set rsl/xml
		if (glidein.getRsl() == null) {
			if (ServiceType.GT2.equals(glideinService.getServiceType())) {
				// GT2 uses globus_rsl
				StringBuilder rsl = new StringBuilder();
				if(glideinService.getProject() != null)
					rsl.append("(project="+glideinService.getProject()+")");
				if(glideinService.getQueue() != null)
					rsl.append("(queue="+glideinService.getQueue()+")");
				rsl.append("(hostCount="+glidein.getHostCount()+")");
				rsl.append("(count="+glidein.getCount()+")");
				rsl.append("(jobType=multiple)");
				rsl.append("(maxTime="+glidein.getWallTime()+")");
				job.setGlobusRSL(rsl.toString());
			} else {
				// GT4 uses globus_xml
				StringBuilder xml = new StringBuilder();
				xml.append("<count>"+glidein.getCount()+"</count>");
				xml.append("<hostCount>"+glidein.getHostCount()+"</hostCount>");
				if(glideinService.getProject() != null)
					xml.append("<project>"+glideinService.getProject()+"</project>");
				if(glideinService.getQueue() != null)
					xml.append("<queue>"+glideinService.getQueue()+"</queue>");
				xml.append("<maxTime>"+glidein.getWallTime()+"</maxTime>");
				xml.append("<jobType>multiple</jobType>");
				job.setGlobusXML(xml.toString());
			}
		} else {
			if (ServiceType.GT2.equals(glideinService.getServiceType())) {
				// GT2 uses globus_rsl
				job.setGlobusRSL(glidein.getRsl());
			} else {
				// GT4 uses globus_xml
				job.setGlobusXML(glidein.getRsl());
			}
		}
		
		// Set glidein executable
		String run = config.getRun();
		job.setExecutable(run);
		job.setLocalExecutable(true);
		
		// Add environment
		EnvironmentVariable env[] = site.getEnvironment();
		if (env!=null) {
			for (EnvironmentVariable var : env)
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add arguments
		job.addArgument("-site "+site.getName());
		job.addArgument("-installPath "+site.getInstallPath());
		job.addArgument("-localPath "+site.getLocalPath());
		job.addArgument("-condorHost "+glidein.getCondorHost());
		// Subtract 1 from wall time to give condor a chance to exit on its
		// own before it gets killed by the local scheduler
		job.addArgument("-wallTime "+(glidein.getWallTime()-1));
		if(glidein.getGcbBroker()!=null)
			job.addArgument("-gcbBroker "+glidein.getGcbBroker());
		if(glidein.getIdleTime()>0)
			job.addArgument("-idleTime "+glidein.getIdleTime());
		if(glidein.getCondorDebug()!=null)
		{
			String[] debug = glidein.getCondorDebug().split("[ ,;:]+");
			for(String level : debug)
				job.addArgument("-debug "+level);
		}
		if(glidein.getNumCpus()>0)
			job.addArgument("-numCpus "+glidein.getNumCpus());
		
		// If there is a special config file, use it
		String configFile = null;
		if (glidein.getCondorConfig()==null)
		{
			configFile = config.getGlideinCondorConfig();
		}
		else
		{
			try {
				// Save config to a file in the submit directory
				configFile = "glidein_condor_config";
				String cfg = Base64.fromBase64(glidein.getCondorConfig());
				IOUtil.write(cfg, new File(job.getJobDirectory(),configFile));
			} catch (IOException ioe) {
				throw new ResourceException("Error writing glidein_condor_config",ioe);
			}
		}
		job.addInputFile(configFile);
		
		// Set the credential
		GlobusCredential cred = getDelegatedCredential();
		if (validateCredentialLifetime(cred)) {
			job.setCredential(cred);
		} else {
			throw new ResourceException("Not enough time on credential");
		}
		
		// Add a listener
		job.addListener(new GlideinListener(getKey()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new ResourceException("Unable to submit glidein job",ce);
		}
		
		// Increment the number of submits
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.incrementSubmits(glidein.getId());
			glidein.setSubmits(glidein.getSubmits()+1);
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to load glidein",de);
		}
	}
	
	private String readJobId() throws ResourceException
	{
		File jobidFile = new File(getJobDirectory(),"jobid");
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
	
	private boolean siteIsReady() throws ResourceException
	{
		SiteResource resource = getSiteResource(glidein.getSiteId());
		SiteState state = resource.getSite().getState();
		return SiteState.READY.equals(state);
	}
	
	private SiteResource getSiteResource(int siteId) throws ResourceException
	{
		try {
			ResourceKey key = AddressingUtil.getSiteKey(siteId);
			SiteResourceHome home = SiteResourceHome.getInstance();
			SiteResource resource = (SiteResource)home.find(key);
			return resource;
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get SiteResourceHome",ne);
		}
	}
	
	private void cancelGlideinJob() throws ResourceException
	{
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
			throw new ResourceException(
					"Unable to read job id",ioe);
		} catch (CondorException ce) {
			throw new ResourceException(
					"Unable to cancel glidein job",ce);
		}
	}
	
	private void deleteFromDatabase() throws ResourceException
	{
		info("Deleting glidein from database");
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.delete(glidein.getId());
		} catch(DatabaseException de) {
			throw new ResourceException("Unable to delete glidein",de);
		}
	}
	
	private void delete() throws ResourceException
	{
		// Remove the glidein from the ResourceHome
		try {
			GlideinResourceHome home = GlideinResourceHome.getInstance();
			home.remove(getKey());
		}  catch (NamingException e) {
			throw new ResourceException("Unable to locate GlideinResourceHome",e);
		}
		
		// Delete the glidein from the database
		deleteFromDatabase();
		
		// Tell the Site About it
		try {
			ResourceKey siteKey = AddressingUtil.getSiteKey(glidein.getSiteId());
			Event siteEvent = new SiteEvent(SiteEventCode.GLIDEIN_DELETED, 
					glidein.getLastUpdate(), siteKey);
			EventQueue queue = EventQueue.getInstance();
			queue.add(siteEvent);
		} catch (NamingException ne) {
			warn("Unable to notify site of deleted glidein",ne);
		}
		
		// Remove the working directory
		FilesystemUtil.rm(getWorkingDirectory());
	}
	
	private ServiceConfiguration getConfig() throws ResourceException
	{
		try {
			return ServiceConfiguration.getInstance();
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get service configuration",ne);
		}
	}
	
	private File getJobDirectory() throws ResourceException
	{
		File jobDirectory = new File(getWorkingDirectory(),"job");
		if (!jobDirectory.exists()) jobDirectory.mkdir();
		return jobDirectory;
	}
	
	private File getWorkingDirectory() throws ResourceException
	{
		return new File(getConfig().getWorkingDirectory(),"glidein-"+glidein.getId());
	}
	
	private File getCredentialEPRFile() throws ResourceException
	{
		File work = getWorkingDirectory();
		File credFile = new File(work,"credential.epr");
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
			Principal principal = new GlobusPrincipal(glidein.getSubject());
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
	
	private boolean validateCredentialLifetime(GlobusCredential credential) 
	throws ResourceException
	{
		// Require enough time to cover at least the wall time of the job
		// More time may be required, but less time will not
		return (glidein.getWallTime()*60 < credential.getTimeLeft());
	}
	
	private boolean credentialIsValid() throws ResourceException
	{
		GlobusCredential cred = getDelegatedCredential();
		return validateCredentialLifetime(cred);
	}
	
	private void failQuietly(String message, Exception exception, Calendar time)
	{
		try {
			fail(message,exception,time);
		} catch (ResourceException re) {
			error("Unable to change state to "+GlideinState.FAILED,re);
		}
	}
	
	private void fail(String message, Exception exception, Calendar time) throws ResourceException
	{
		// Update status to FAILED
		error("Failure: "+message,exception);
		if (exception == null) {
			updateState(GlideinState.FAILED, message, null, time);
		} else {
			CharArrayWriter caw = new CharArrayWriter();
			exception.printStackTrace(new PrintWriter(caw));
			updateState(GlideinState.FAILED, message, caw.toString(), time);
		}
	}
	
	public synchronized void handleEvent(GlideinEvent event)
	{
		try {
			_handleEvent(event);
		} catch (ResourceException re) {
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
	
	private void _handleEvent(GlideinEvent event) throws ResourceException
	{
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
					} catch (ResourceException re) {
						// Just log it so that we can fail properly
						error("Unable to cancel glidein job",re);
					}
				}
				
				// If the site failed, then the glidein will fail
				failQuietly("Site failed",null,event.getTime());
				
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
					// the site should be in ready status, and the credential
					// should be valid
					if (shouldResubmit() && siteIsReady() && 
							credentialIsValid()) {
						
						info("Resubmitting glidein");
						updateState(GlideinState.SUBMITTED,
								"Local job submitted",null,event.getTime());
						submitGlideinJob();
						
					} else {
						
						// Otherwise, delete the glidein
						updateState(GlideinState.DELETED,
								"Glidein deleted",null,event.getTime());
						delete();
						
					}
					
				} else {
					
					warn("Glidein was not in "+reqd+" state when "+
							code+" was received");
					
				}
				
			} break;
			
			case JOB_ABORTED:
			case DELETE: {
				
				// If the job was aborted, or a delete was requested, then
				// just delete the glidein
				updateState(GlideinState.DELETED,
						"Glidein deleted",null,event.getTime());
				delete();
				
			} break;
			
			case JOB_FAILURE: {
				
				// If the job fails, then fail the glidein
				failQuietly((String)event.getProperty("message"), 
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
	
	private boolean shouldResubmit()
	{
		// If the glidein should be resubmitted
		if (glidein.isResubmit()) {
			
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
			Calendar until = glidein.getUntil();
			if (until != null) {
				// If until is in the future, we should resubmit
				Calendar now = Calendar.getInstance();
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
						"Unable to recover glidein "+glidein.getId(),re);
				
			}
		}
	}
	
	private void _recoverState() throws ResourceException
	{
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
					new GlideinListener(getKey());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialEPRFile().exists()) {
				
				// If the credential EPR file still exists, 
				// try to submit the install job
				submitGlideinJob();
				
			} else {
				
				// Otherwise set the state to failed
				fail("Unable to submit site",null,Calendar.getInstance());
				
			}
			
		} else if (GlideinState.QUEUED.equals(state) ||
				GlideinState.RUNNING.equals(state)) {
			
			// If glidein has been submitted to condor, then it can only 
			// be running or finished
			File jobDir = getJobDirectory();
			CondorJob job = new CondorJob(jobDir, glidein.getLocalUsername());
			job.setJobId(readJobId());
			GlideinListener listener = 
				new GlideinListener(getKey());
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
				} catch (ResourceException re) {
					warn("Unable to cancel job on recovery",re);
				}
			}
			
		} else if (GlideinState.FAILED.equals(state)) {
		
			// If the glidein failed, do nothing
			
		} else if (GlideinState.DELETED.equals(state)) {
			
			// Delete the glidein
			delete();
			
		} else {
			
			// This should not happen
			throw new IllegalStateException("Glidein state was: "+state);
			
		}
	}
	
	private void authorize() throws ResourceException
	{
		try {
			// Authenticate the client
			AuthenticationUtil.authenticate();
			
			// Set subject and username
			String subject = AuthenticationUtil.getSubject();
			if (subject == null || !subject.equals(glidein.getSubject())) {
				throw new ResourceException(
						"Subject "+subject+" is not authorized to modify glidein "+glidein.getId());
			}
		} catch (SecurityException se) {
			throw new ResourceException("Unable to authenticate client",se);
		}
	}

	private String logPrefix()
	{
		if (glidein == null) {
			return "";
		} else {
			return "Glidein "+glidein.getId()+": ";
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
