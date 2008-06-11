package edu.usc.glidein.service.impl;

import java.io.File;
import java.io.IOException;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.wsrf.PersistenceCallback;
import org.globus.wsrf.RemoveCallback;
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
import edu.usc.glidein.util.IOUtil;

public class GlideinResource implements Resource, ResourceIdentifier, PersistenceCallback, RemoveCallback, ResourceProperties
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
		logger.debug("Creating glidein");
		
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
		logger.debug("Loading "+key.getValue());
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
		logger.debug("Storing "+getGlidein().getId());
		
		throw new ResourceException("Tried to store GlideinResource");
		
		/*
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.store(glidein);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
		*/
	}
	
	public void remove() throws ResourceException
	{
		remove(false);
	}
	
	public void remove(boolean force) throws ResourceException 
	{
		logger.debug("Removing "+getGlidein().getId());
		// TODO: Do I need force?
		try {
			Event event = new GlideinEvent(GlideinEventCode.REMOVE,getKey());
			EventQueue queue = EventQueue.getInstance(); 
			queue.add(event);
		} catch(NamingException ne) {
			throw new ResourceException("Unable to remove glidein",ne);
		}
	}

	public void submit() throws ResourceException
	{
		logger.debug("Submitting "+glidein.getId());
		try {
			Event event = new GlideinEvent(GlideinEventCode.SUBMIT,getKey());
			EventQueue queue = EventQueue.getInstance(); 
			queue.add(event);
		} catch(NamingException ne) {
			throw new ResourceException("Unable to submit glidein",ne);
		}
	}
	
	private void updateState(GlideinState state, String shortMessage, String longMessage) 
	throws ResourceException
	{
		logger.debug("Changing state of glidein "+getGlidein().getId()+" to "+state+": "+shortMessage);
		
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
		logger.debug("Submitting glidein job "+glidein.getId());
		
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
		File jobDirectory = new File(config.getTempDir(),"glidein-"+glidein.getId());
		
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
		if (glidein.getCondorConfigBase64()==null)
		{
			configFile = config.getGlideinCondorConfig();
		}
		else
		{
			try {
				// Save config to a file in the submit directory
				configFile = "glidein_condor_config";
				String cfg = Base64.fromBase64(glidein.getCondorConfigBase64());
				IOUtil.write(cfg, new File(job.getJobDirectory(),configFile));
			} catch (IOException ioe) {
				throw new ResourceException("Unable to submit glidein job: " +
						"Error writing glidein_condor_config",ioe);
			}
		}
		job.addInputFile(configFile);
		
		// TODO: Get credential
		//job.setCredential(credential);
		
		// Add a listener
		job.addListener(new GlideinListener());
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new ResourceException("Unable to submit glidein job: " +
					"Submit failed",ce);
		}
		
		logger.debug("Submitted glidein job '"+glidein.getId()+"'");
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
		// TODO: Cancel glidein job
	}
	
	private void delete() throws ResourceException
	{
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.delete(glidein.getId());
			glidein = null;
			resourceProperties = null;
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void handleEvent(GlideinEventCode event)
	{
		/* TODO: Implement handleEvent
		GlideinState state = glidein.getState();
		
		switch (event) {
			
			case SUBMIT:
				// Only NEW glideins can be submitted
				if (GlideinState.NEW.equals(state)) { 
					if (siteReady()) {
						submitGlideinJob();
						updateState(GlideinState.SUBMITTED,"Local job submitted",null);
					} else {
						updateState(GlideinState.WAITING,"Waiting for Site to be READY",null);
					}
				}
			break;
			
			case SITE_READY:
				// If we are waiting, then queue a SUBMIT event
				if (GlideinState.WAITING.equals(state)) {
					submitGlideinJob();
					updateState(GlideinState.SUBMITTED,"Local job submitted",null);
				}
			break;
			
			case QUEUED:
				// A SUBMITTED job has been QUEUED remotely
				if (GlideinState.SUBMITTED.equals(state)) {
					updateState(GlideinState.QUEUED,"Glidein queued remotely",null);
				}
			break;
			
			case RUNNING:
				if (GlideinState.QUEUED.equals(state)) {
					updateState(GlideinState.RUNNING,"Glidein job running",null);
				}
			break;
				
			case REMOVE:
				cancelGlideinJob();
				updateState(GlideinState.CANCELLED,"Local job cancelled",null);
			break;
			
			case EXITED:
			case DELETE:
				ResourceKey siteKey = AddressingUtil.getSiteKey(glidein.getSiteId());
				
				// Delete the glidein after it has exited or if
				// a force delete was requested
				GlideinResourceHome home = GlideinResourceHome.getInstance();
				home.remove(getKey());
				// TODO: Figure out when Resource.remove gets called
				delete();
				
				// Tell the Site About it
				Event siteEvent = new SiteEvent(SiteEventCode.GLIDEIN_FINISHED, siteKey);
				EventQueue queue = EventQueue.getInstance();
				queue.add(siteEvent);
			break;
			
			case FAILED:
				// TODO: Fail job
			break;
			
			default:
				logger.warn("Unhandled event: "+event);
			break;
		}
		*/
	}
}
