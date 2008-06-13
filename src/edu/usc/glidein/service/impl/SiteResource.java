package edu.usc.glidein.service.impl;

import java.io.File;

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
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
import edu.usc.glidein.service.state.Event;
import edu.usc.glidein.service.state.EventQueue;
import edu.usc.glidein.service.state.SiteEvent;
import edu.usc.glidein.service.state.SiteEventCode;
import edu.usc.glidein.service.state.StageSiteListener;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;

public class SiteResource implements Resource, ResourceIdentifier, PersistenceCallback, RemoveCallback, ResourceProperties
{
	private Logger logger = Logger.getLogger(SiteResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Site site;
	
	/**
	 * Default constructor required
	 */
	public SiteResource() { }
	
	public void setSite(Site site) throws ResourceException
	{
		this.site = site;
		setResourceProperties();
	}
	
	public Site getSite()
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
	
	private void setResourceProperties() throws ResourceException
	{
		try {
			resourceProperties = new SimpleResourcePropertySet(SiteNames.RESOURCE_PROPERTIES);
			resourceProperties.add(new ReflectionResourceProperty(
					SiteNames.RP_SITE_ID,"Id",site));
			// TODO: Set the rest of the resource properties, or don't
		} catch(Exception e) {
			throw new ResourceException("Unable to set site resource properties",e);
		}
	}

	public void create(Site site) throws ResourceException
	{
		logger.debug("Creating site resource "+getSite().getId());
		
		// Set state
		site.setState(SiteState.NEW);
		site.setShortMessage("Created");
			
		// Check for Condor version and set reasonable default
		String ver = site.getCondorVersion();
		ver = "".equalsIgnoreCase(ver) ? null : ver;
		String pkg = site.getCondorPackage();
		pkg = "".equalsIgnoreCase(pkg) ? null : pkg;
		if (ver==null && pkg==null) {
			site.setCondorVersion("7.0.0");
		}
		
		// Save site in database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.create(site);
		} catch (DatabaseException de) {
			throw new ResourceException(de);
		}
		
		// Set site
		setSite(site);
	}

	public void load(ResourceKey key) throws ResourceException
	{
		logger.debug("Loading site resource "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			setSite(dao.load(id));
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public void store() throws ResourceException
	{
		logger.debug("Storing site resource "+getSite().getId());
		
		throw new ResourceException("Tried to store SiteResource");
		
		/*
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.store(site);
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
		logger.debug("Removing site resource "+getSite().getId());
		
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
			Event event = new SiteEvent(code,getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get queue: "+ne.getMessage(),ne);
		}
	}
	
	public void submit() throws ResourceException 
	{
		logger.debug("Submitting site "+getSite().getId());
		
		// Schedule submit event
		try {
			Event event = new SiteEvent(SiteEventCode.SUBMIT,getKey());
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException(ne.getMessage(),ne);
		}
	}
	
	private void updateState(SiteState state, String shortMessage, String longMessage)
	throws ResourceException
	{
		logger.debug("Changing state of site "+getSite().getId()+" to "+state+": "+shortMessage);
		
		// Update object
		site.setState(state);
		site.setShortMessage(shortMessage);
		site.setLongMessage(longMessage);
		
		// Update database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.updateState(site.getId(), state, shortMessage, longMessage);
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	private void submitInstallJob() throws ResourceException
	{
		logger.debug("Submitting install job for site '"+site.getName()+"'");
		
		ServiceConfiguration config = null;
		try {
			config = ServiceConfiguration.getInstance();
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get service configuration",ne);
		}
		
		// Create working directory
		File jobDirectory = new File(config.getTempDir(),"site-"+site.getId());
		
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
		// TODO: job.setCredential(cred);
		
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
		job.addListener(new StageSiteListener(getKey()));
		
		// Submit job
		try {
			Condor condor = Condor.getInstance();
			condor.submitJob(job);
		} catch (CondorException ce) {
			throw new ResourceException("Unable to submit staging job: "+ce.getMessage(),ce);
		}
			
		logger.debug("Submitted staging job for site '"+site.getName()+"'");
	}
	
	private boolean hasGlideins() throws ResourceException
	{
		// TODO: Implement hasGlideins
		return false;
	}
	
	private void cancelGlideins() throws ResourceException
	{
		// TODO: Cancel glideins
	}
	
	private void cancelInstallJob() throws ResourceException
	{
		// TODO: Find submit dir
		// TODO: Determine job ID
		// TODO: condor_rm job
	}
	
	private void submitUninstallJob() throws ResourceException
	{
		// TODO: Submit uninstall job
	}
	
	private void delete() throws ResourceException
	{
		// Remove the site from the database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
			site = null;
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void handleEvent(SiteEventCode event)
	{
		/* TODO: Implement handleEvent
		SiteState state = site.getState();
		
		switch (event) {
			
			case SUBMIT:
				if (SiteState.NEW.equals(state)) {
					try {
						submitInstallJob();
					} catch (ResourceException re) {
						//TODO: handleException(re);
					}
				} else {
					//TODO: Site must be in NEW state to be submitted
				}
			break;
				
			case INSTALLED:
				// TODO: Change status to READY
				// updateState(SiteState.READY, "Installed", null);
				// TODO: Trigger any ready glideins for this site
			break;
				
			case REMOVE:
			
				// If we are currently staging, then we need to cancel the install job
				if (SiteState.STAGING.equals(state)) {
					cancelInstallJob();
				}
				
				// If there are some glideins for this site
				if (hasGlideins()) { 
					// Cancel running glideins
					cancelGlideins();
					
					// Change state to EXITING
					updateState(SiteState.EXITING, "Waiting for glideins", null);
				} else {
					// Submit uninstall job
					submitUninstallJob();
				}
			break;
				
			case GLIDEIN_FINISHED:
				// If exiting then check for last glidein
				if (SiteState.EXITING.equals(state)) {
					// TODO: If last glidein, then submit REMOVE event
				}
			break;
			
			case UNINSTALLED:
			case DELETE:
				// TODO: Delete the Resource
				ResourceKey key = AddressingUtil.getSiteKey(site.getId());
				SiteResourceHome resourceHome = SiteResourceHome.getInstance();
				resourceHome.remove(key);
				// TODO: Find out when Resource.remove gets called
				delete();
			break;
			
			case FAILED:
				// TODO: Fail site
			break;
			
			default:
				try {
					throw new IllegalStateException();
				} catch (IllegalStateException e) {
					logger.error("Unhandled event: "+event,e);
				}
			break;
		}
		*/
	}
}
