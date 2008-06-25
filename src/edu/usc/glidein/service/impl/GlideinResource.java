package edu.usc.glidein.service.impl;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

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

import edu.usc.glidein.service.ServiceConfiguration;
import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorEventGenerator;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
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
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;
import edu.usc.glidein.util.AddressingUtil;
import edu.usc.glidein.util.Base64;
import edu.usc.glidein.util.CredentialUtil;
import edu.usc.glidein.util.IOUtil;

/* TODO: Fix error handling for failure cases
 * A serious failure should cause the handleEvent method to return.
 */

public class GlideinResource implements Resource, ResourceIdentifier, PersistenceCallback, ResourceProperties
{
	private Logger logger = Logger.getLogger(GlideinResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Glidein glidein = null;
	
	/**
	 * Default constructor required
	 */
	public GlideinResource() { }

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
	
	public ResourcePropertySet getResourcePropertySet()
	{
		return resourceProperties;
	}
	
	private void setResourceProperties()
	{
		try {
			resourceProperties = new SimpleResourcePropertySet(GlideinNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(GlideinNames.RP_GLIDEIN_ID,"Id",glidein));
			// TODO: Set the rest of the resource properties, or don't
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
		
		// Save in the database
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.create(glidein);
		} catch (DatabaseException dbe) {
			throw new ResourceException(dbe);
		}
		
		// Set glidein
		setGlidein(glidein);
	}

	public void load(ResourceKey key) throws ResourceException
	{
		info("Loading glidein "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			setGlidein(dao.load(id));
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public void store() throws ResourceException 
	{
		throw new UnsupportedOperationException();
	}
	
	public void remove(boolean force) throws ResourceException 
	{
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
			Event event = new GlideinEvent(code,getKey());
			EventQueue queue = EventQueue.getInstance(); 
			queue.add(event);
		} catch(NamingException ne) {
			throw new ResourceException("Unable to remove glidein",ne);
		}
	}

	public void submit(EndpointReferenceType credentialEPR) throws ResourceException
	{
		info("Submitting glidein");
		try {
			
			// Get delegated credential
			DelegationResource delegationResource = 
				DelegationUtil.getDelegationResource(credentialEPR);
			GlobusCredential credential = 
				delegationResource.getCredential();
			
			// Validate credential lifetime
			validateCredentialLifetime(credential);
			
			// Create submit event
			Event event = new GlideinEvent(GlideinEventCode.SUBMIT,getKey());
			event.setProperty("credential", credential);
			EventQueue queue = EventQueue.getInstance(); 
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to submit glidein: "+ne.getMessage(),ne);
		} catch (DelegationException de) {
			throw new ResourceException("Unable to submit glidein: "+de.getMessage(),de);
		}
	}
	
	private void updateState(GlideinState state, String shortMessage, String longMessage) 
	throws ResourceException
	{
		info("Changing state to "+state+": "+shortMessage);
		
		// Update object
		glidein.setState(state);
		glidein.setShortMessage(shortMessage);
		glidein.setLongMessage(longMessage);
		
		// Update database
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.updateState(glidein.getId(), state, shortMessage, longMessage);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	private void submitGlideinJob() throws ResourceException
	{
		info("Submitting glidein job");
		
		SiteResource siteResource = getSiteResource();
		Site site = siteResource.getSite();
		
		// Get configuration
		ServiceConfiguration config;
		try {
			config = ServiceConfiguration.getInstance();
		} catch (NamingException ne) {
			throw new ResourceException("Unable to submit glideing job: " +
					"Unable to get configuration",ne);
		}
		
		// Create working directory
		File jobDirectory = getWorkingDirectory();
		
		// Create a job description
		CondorJob job = new CondorJob(jobDirectory);
		job.setUniverse(CondorUniverse.GRID);
		
		// Set jobmanager info
		ExecutionService glideinService = site.getGlideinService();
		if(ServiceType.GT2.equals(glideinService.getServiceType()))
			job.setGridType(CondorGridType.GT2);
		else
			job.setGridType(CondorGridType.GT4);
		job.setGridContact(glideinService.getServiceContact());
		job.setProject(glideinService.getProject());
		job.setQueue(glideinService.getQueue());
		
		// Set glidein executable
		String run = config.getRun();
		job.setExecutable(run);
		job.setLocalExecutable(true);
		
		// Set number of processes
		job.setHostCount(glidein.getHostCount());
		job.setCount(glidein.getCount());
		job.setMaxTime(glidein.getWallTime());
		
		// Add environment
		EnvironmentVariable env[] = site.getEnvironment();
		if (env!=null) {
			for (EnvironmentVariable var : env)
				job.addEnvironment(var.getVariable(), var.getValue());
		}
		
		// Add arguments
		job.addArgument("-installPath "+site.getInstallPath());
		job.addArgument("-localPath "+site.getLocalPath());
		job.addArgument("-condorHost "+glidein.getCondorHost());
		job.addArgument("-wallTime "+glidein.getWallTime());
		if(glidein.getGcbBroker()!=null)
			job.addArgument("-gcbBroker "+glidein.getGcbBroker());
		if(glidein.getIdleTime()>0)
			job.addArgument("-idleTime "+glidein.getIdleTime());
		if(glidein.getCondorDebug()!=null)
		{
			String[] debug = glidein.getCondorDebug().split("[ ,]+");
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
				throw new ResourceException("Unable to submit glidein job: " +
						"Error writing glidein_condor_config",ioe);
			}
		}
		job.addInputFile(configFile);
		
		job.setCredential(loadCredential());
		
		// Add a listener
		job.addListener(new GlideinListener(getKey()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new ResourceException("Unable to submit glidein job: " +
					"Submit failed",ce);
		}
	}
	
	private String readJobId() throws ResourceException
	{
		File jobidFile = new File(getWorkingDirectory(),"jobid");
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
	
	private boolean siteReady() throws ResourceException
	{
		SiteResource resource = getSiteResource();
		SiteState state = resource.getSite().getState();
		return SiteState.READY.equals(state);
	}
	
	private SiteResource getSiteResource() throws ResourceException
	{
		try {
			ResourceKey key = AddressingUtil.getSiteKey(glidein.getSiteId());
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
		
		// Find submit dir
		File dir = getWorkingDirectory();
		File jobidFile = new File(dir,"jobid");
		
		try {
			// Read job id from jobid file
			BufferedReader reader = new BufferedReader(
					new FileReader(jobidFile));
			String jobid = reader.readLine();
			reader.close();
			
			// condor_rm job
			Condor.getInstance().cancelJob(jobid);
		} catch (IOException ioe) {
			throw new ResourceException(
					"Unable to cancel glidein job: Unable to read job id",ioe);
		} catch (CondorException ce) {
			throw new ResourceException(
					"Unable to cancel glidein job: condor_rm failed",ce);
		}
	}
	
	private void delete() throws ResourceException
	{
		info("Deleting glidein from database");
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.delete(glidein.getId());
		} catch(DatabaseException de) {
			throw new ResourceException(de);
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
		File dir = new File(getConfig().getTempDir(),"glidein-"+glidein.getId());
		
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
	
	private void storeCredential(GlobusCredential credential) throws ResourceException
	{
		try {
			File work = getWorkingDirectory();
			File credFile = new File(work,"credential");
			CredentialUtil.store(credential,credFile);
		} catch (IOException ioe) {
			throw new ResourceException("Unable to store credential");
		}
	}
	
	private File getCredentialFile() throws ResourceException
	{
		File work = getWorkingDirectory();
		File credFile = new File(work,"credential");
		return credFile;
	}
	
	private void validateCredentialLifetime(GlobusCredential credential) 
	throws ResourceException
	{
		// Require enough time to cover at least the wall time of the job
		// More time may be required, but less time will not
		if (glidein.getWallTime()*60 > credential.getTimeLeft()) {
			throw new ResourceException("Credential does not have enough time left. " +
					"Need: "+(glidein.getWallTime()*60)+", have: "+credential.getTimeLeft());
		}
	}
	
	private GlobusCredential loadCredential() throws ResourceException
	{
		try {
			GlobusCredential credential = CredentialUtil.load(getCredentialFile());
			validateCredentialLifetime(credential);
			return credential;
		} catch (IOException ioe) {
			throw new ResourceException("Unable to load credential");
		}
	}
	
	private void failQuietly(String message, Exception exception)
	{
		try {
			fail(message,exception);
		} catch (ResourceException re) {
			error("Unable to change state to "+GlideinState.FAILED,re);
		}
	}
	
	private void fail(String message, Exception exception) throws ResourceException
	{
		// Update status to FAILED
		error("Failure: "+message,exception);
		if (exception == null) {
			updateState(GlideinState.FAILED, message, null);
		} else {
			CharArrayWriter caw = new CharArrayWriter();
			exception.printStackTrace(new PrintWriter(caw));
			updateState(GlideinState.FAILED, message, caw.toString());
		}
	}
	
	public synchronized void handleEvent(GlideinEvent event)
	{
		GlideinState state = glidein.getState();
		GlideinEventCode code = (GlideinEventCode) event.getCode();
		
		// If the glidein was deleted, then don't process any more events
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
					
					// Try to save the credential
					try {
						GlobusCredential credential = 
							(GlobusCredential)event.getProperty("credential");
						storeCredential(credential);
					} catch (ResourceException re) {
						failQuietly("Unable to save credential",re);
					}
					
					// Check to see if site is ready
					boolean ready = false;
					try {
						ready = siteReady();
					} catch (ResourceException re) {
						failQuietly("Unable to check for site ready state",re);
					}
					
					if (ready) {
						
						// If the site is ready, submit the glidein job
						try {
							updateState(GlideinState.SUBMITTED,
									"Local job submitted",null);
							submitGlideinJob();
						} catch (ResourceException re) {
							failQuietly("Unable to submit job",re);
						}
						
					} else {
						
						// Otherwise, we need to wait for the site
						try {
							updateState(GlideinState.WAITING,
									"Waiting for site to be "+
									SiteState.READY, null);
						} catch (ResourceException re) {
							failQuietly("Unable to wait for site ready state",re);
						}
						
					}
				} else {
					warn("State was not "+reqd+" when event "+
							code+" was received");
				}
				
			} break;
			
			case SITE_READY: {
				
				// If we are waiting, then queue a SUBMIT event
				GlideinState reqd = GlideinState.WAITING;
				if (reqd.equals(state)) {
					try {
						updateState(GlideinState.SUBMITTED,"Local job submitted",null);
						submitGlideinJob();
					} catch (ResourceException re) {
						failQuietly("Unable to submit job",re);
					}
				} else {
					warn("State was not "+reqd+" when event "+
							code+" was received");
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
						// Just log it
						error("Unable to cancel glidein job",re);
					}
				}
				
				// If the site failed, then the glidein will fail
				failQuietly("Site failed",null);
				
			} break;
			
			case QUEUED: {
				
				GlideinState reqd = GlideinState.SUBMITTED;
				if (reqd.equals(state)) {
					try {
						updateState(GlideinState.QUEUED,"Glidein job queued",null);
					} catch (ResourceException re) {
						failQuietly("Unable to set state to "+GlideinState.QUEUED,re);
					}
				} else {
					warn("State was not "+reqd+" when event "+
							code+" was received");	
				}
				
			} break;
			
			case RUNNING: {
				
				// Update state to running regardless
				try {
					updateState(GlideinState.RUNNING,"Glidein job running",null);
				} catch (ResourceException re) {
					failQuietly("Unable to set state to "+GlideinState.RUNNING,re);
				}
				
			} break;
			
			case REMOVE: {
				
				// If a glidein job has been submitted cancel the job
				if (GlideinState.SUBMITTED.equals(state) || 
						GlideinState.RUNNING.equals(state) || 
						GlideinState.QUEUED.equals(state)) {
					try {
						cancelGlideinJob();
					} catch (ResourceException re) {
						failQuietly("Unable to cancel glidein job",re);
					}
					
					// Let abort event occur
					return;
				} else { 
					/* Fall through to delete */
				}
				
			} // Fall through!
			
			case JOB_ABORTED:
			case JOB_SUCCESS:
			case DELETE: {
				
				// Change the status to deleted
				glidein.setState(GlideinState.DELETED);
				
				// Remove the glidein from the ResourceHome
				try {
					GlideinResourceHome home = GlideinResourceHome.getInstance();
					home.remove(getKey());
				}  catch (NamingException e) {
					failQuietly("Unable to locate GlideinResourceHome",e);
				} catch (ResourceException re) {
					failQuietly("Unable to remove glidein from GlideinResourceHome",re);
				}
				
				// Delete the glidein from the database
				try {
					delete();
				} catch (ResourceException re) {
					failQuietly("Unable to delete glidein from database",re);
				}
				
				// Tell the Site About it
				try {
					ResourceKey siteKey = AddressingUtil.getSiteKey(glidein.getSiteId());
					Event siteEvent = new SiteEvent(SiteEventCode.GLIDEIN_DELETED, siteKey);
					EventQueue queue = EventQueue.getInstance();
					queue.add(siteEvent);
				} catch (NamingException ne) {
					warn("Unable to notify site of deleted glidein",ne);
				}
				
				// Remove the working directory
				try {
					IOUtil.rmdirs(getWorkingDirectory());
				} catch (ResourceException re) {
					warn("Unable to remove working dir",re);
				}
				
			} break;
			
			case JOB_FAILURE: {
				
				// If the job fails, then fail the glidein
				failQuietly((String)event.getProperty("message"), 
						(Exception)event.getProperty("exception"));
				
			} break;
			
			default: {
				
				IllegalStateException ise = new IllegalStateException();
				ise.fillInStackTrace();
				error("Unhandled event: "+event,ise);
				
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
				fail("State recovery failed",re);
				
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
			
			File jobDir = getWorkingDirectory();
			CondorJob job = new CondorJob(jobDir);
			
			if (job.getLog().exists()) {
				
				// If the install log exists, then recover the install job
				job.setJobId(readJobId());
				GlideinListener listener = 
					new GlideinListener(getKey());
				job.addListener(listener);
				CondorEventGenerator gen = new CondorEventGenerator(job);
				gen.start();
				
			} else if (getCredentialFile().exists()) {
				
				// If the credential file still exists, 
				// try to submit the install job
				submitGlideinJob();
				
			} else {
				
				// Otherwise set the state back to NEW
				updateState(GlideinState.NEW, "Glidein created", null);
				
			}
			
		} else if (GlideinState.QUEUED.equals(state) ||
				GlideinState.RUNNING.equals(state)) {
			
			// If glidein has been submitted to condor, then it can only 
			// be running or finished
			File jobDir = getWorkingDirectory();
			CondorJob job = new CondorJob(jobDir);
			job.setJobId(readJobId());
			GlideinListener listener = 
				new GlideinListener(getKey());
			job.addListener(listener);
			CondorEventGenerator gen = new CondorEventGenerator(job);
			gen.start();
			
		} else if (GlideinState.FAILED.equals(state)) {
		
			// If the glidein failed, do nothing
			
		} else if (GlideinState.DELETED.equals(state)) {
			
			// This should not happen
			throw new IllegalStateException("Glidein state was deleted");
			
		} else {
			
			// This should not happen
			throw new IllegalStateException("Glidein state was: "+state);
			
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
