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

import edu.usc.glidein.service.ServiceConfiguration;
import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.service.exec.Condor;
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

public class GlideinResource implements Resource, ResourceIdentifier, PersistenceCallback, ResourceProperties
{
	private Logger logger = Logger.getLogger(GlideinResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Glidein glidein = null;
	
	/**
	 * Default constructor required
	 */
	public GlideinResource() { }

	public void setGlidein(Glidein glidein) throws ResourceException
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
	
	private void setResourceProperties() throws ResourceException
	{
		try {
			resourceProperties = new SimpleResourcePropertySet(GlideinNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(GlideinNames.RP_GLIDEIN_ID,"Id",glidein));
			// TODO: Set the rest of the resource properties, or don't
		} catch(Exception e) {
			throw new ResourceException("Unable to set glidein resource properties",e);
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
			if (glidein.getWallTime()*60 > credential.getTimeLeft()) {
				throw new ResourceException("Credential does not have enough time left. " +
						"Need: "+(glidein.getWallTime()*60)+", have: "+credential.getTimeLeft());
			}
			
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
	
	private void submitGlideinJob(GlobusCredential credential) throws ResourceException
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
		
		job.setCredential(credential);
		
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
	
	private GlobusCredential loadCredential() throws ResourceException
	{
		try {
			File work = getWorkingDirectory();
			File credFile = new File(work,"credential");
			GlobusCredential credential = CredentialUtil.load(credFile);
			return credential;
		} catch (IOException ioe) {
			throw new ResourceException("Unable to load credential");
		}
	}
	
	private void fail(String message, Exception exception)
	{
		// Update status to FAILED
		error("Failure: "+message,exception);
		CharArrayWriter caw = new CharArrayWriter();
		exception.printStackTrace(new PrintWriter(caw));
		try {
			updateState(GlideinState.FAILED, message, caw.toString());
		} catch (ResourceException re) {
			error("Unable to change state to "+GlideinState.FAILED,re);
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
				if (GlideinState.NEW.equals(state)) {
					GlobusCredential credential = (GlobusCredential)event.getProperty("credential");
					
					// Check to see if site is ready
					boolean ready = false;
					try {
						ready = siteReady();
					} catch (ResourceException re) {
						fail("Unable to check for site ready state",re);
					}
					
					// If the site is ready, submit the glidein job, otherwise wait
					if (ready) {
						try {
							submitGlideinJob(credential);
							updateState(GlideinState.SUBMITTED,"Local job submitted",null);
						} catch (ResourceException re) {
							fail("Unable to submit job",re);
						}
					} else {
						try {
							storeCredential(credential);
							updateState(GlideinState.WAITING,"Waiting for site to be "+SiteState.READY,null);
						} catch (ResourceException re) {
							fail("Unable to wait for site ready state",re);
						}
					}
				}
			} break;
			
			case SITE_READY: {
				// If we are waiting, then queue a SUBMIT event
				if (GlideinState.WAITING.equals(state)) {
					try {
						GlobusCredential credential = loadCredential();
						submitGlideinJob(credential);
						updateState(GlideinState.SUBMITTED,"Local job submitted",null);
					} catch (ResourceException re) {
						fail("Unable to submit job",re);
					}
				}
			} break;
			
			case QUEUED: {
				if (GlideinState.SUBMITTED.equals(state)) {
					try {
						updateState(GlideinState.QUEUED,"Glidein job queued",null);
					} catch (ResourceException re) {
						fail("Unable to set state to "+GlideinState.QUEUED,re);
					}
				}
			} break;
			
			case RUNNING: {
				try {
					updateState(GlideinState.RUNNING,"Glidein job running",null);
				} catch (ResourceException re) {
					fail("Unable to set state to "+GlideinState.RUNNING,re);
				}
			} break;
			
			case REMOVE:
			case JOB_SUCCESS:
			case DELETE: {
				// If the user requested remove
				if (GlideinEventCode.REMOVE.equals(code)) {
					
					// If a glidein job has been submitted cancel the job
					if (GlideinState.SUBMITTED.equals(state) || 
							GlideinState.RUNNING.equals(state) || 
							GlideinState.QUEUED.equals(state)) {
						try {
							cancelGlideinJob();
						} catch (ResourceException re) {
							fail("Unable to cancel glidein job",re);
						}
					}
				}
				
				// Change the status to deleted
				glidein.setState(GlideinState.DELETED);
				
				// Remove the glidein from the ResourceHome
				try {
					GlideinResourceHome home = GlideinResourceHome.getInstance();
					home.remove(getKey());
				}  catch (NamingException e) {
					fail("Unable to locate GlideinResourceHome",e);
					return;
				} catch (ResourceException re) {
					fail("Unable to remove glidein from GlideinResourceHome",re);
					return;
				}
				
				// Delete the glidein from the database
				try {
					delete();
				} catch (ResourceException re) {
					fail("Unable to delete glidein from database",re);
					return;
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
				fail((String)event.getProperty("message"), 
						(Exception)event.getProperty("exception"));
			} break;
			
			default: {
				IllegalStateException ise = new IllegalStateException();
				ise.fillInStackTrace();
				error("Unhandled event: "+event,ise);
			} break;
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
