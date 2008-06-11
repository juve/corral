package edu.usc.glidein.service.impl;

import java.io.File;

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
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.service.state.GlideinListener;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinState;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
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
	
	public GlideinResource(Glidein glidein) throws ResourceException
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
	
	public synchronized void create() throws ResourceException
	{
		logger.debug("Creating glidein");
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.create(glidein);
			setResourceProperties();
		} catch (DatabaseException dbe) {
			throw new ResourceException(dbe);
		}
	}

	public synchronized void load(ResourceKey key) throws ResourceException
	{
		logger.debug("Loading "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			glidein = dao.load(id);
			setResourceProperties();
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void store() throws ResourceException 
	{
		logger.debug("Storing "+getGlidein().getId());
		try {
			Database db = Database.getDatabase();
			GlideinDAO dao = db.getGlideinDAO();
			dao.store(glidein);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void remove() throws ResourceException
	{
		remove(false);
	}
	
	public synchronized void remove(boolean force) throws ResourceException 
	{
		logger.debug("Removing "+getGlidein().getId());
		
		// TODO: Remove glideins correctly
		delete();
	}

	public synchronized void submit() throws ResourceException
	{
		logger.debug("Submitting "+getGlidein().getId());
		
		// TODO: Get delegated credential
		// TODO: Check to make sure site is ready before submitting
		// TODO: Submit glidein
	}
	
	public synchronized void delete() throws ResourceException
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
	
	public synchronized void updateState(GlideinState state, String shortMessage, String longMessage) 
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
	
	public synchronized void processStateChange(GlideinState newState)
	{
		// TODO: Implement processStateChange
	}
	
	public void submitGlideinJob() throws Exception
	{
		// TODO: Get SiteResource, Site from SiteResourceHome
		Site site = null;
		ServiceConfiguration config = ServiceConfiguration.getInstance();

		logger.debug("Submitting glidein job for site '"+site.getName()+"'");
		
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
			// Save config to a file in the submit directory
			configFile = "glidein_condor_config";
			String cfg = Base64.fromBase64(glidein.getCondorConfigBase64());
			IOUtil.write(cfg, new File(job.getJobDirectory(),configFile));
		}
		job.addInputFile(configFile);
		
		// Add a listener
		job.addListener(new GlideinListener());
		
		// Submit job
		Condor condor = Condor.getInstance();
		condor.submitJob(job);
		
		logger.debug("Submitted glidein job for site '"+site.getName()+"'");
	}
}
