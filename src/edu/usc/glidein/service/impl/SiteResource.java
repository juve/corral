package edu.usc.glidein.service.impl;

import java.io.File;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.globus.gsi.GlobusCredential;
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
import edu.usc.glidein.service.state.ReadyQueue;
import edu.usc.glidein.service.state.SiteStateChange;
import edu.usc.glidein.service.state.StageSiteListener;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;
import edu.usc.glidein.util.ProxyUtil;

public class SiteResource implements Resource, ResourceIdentifier, PersistenceCallback, RemoveCallback, ResourceProperties
{
	private Logger logger = Logger.getLogger(SiteResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Site site;
	
	/**
	 * Default constructor required
	 */
	public SiteResource() { }
	
	public SiteResource(Site site) throws ResourceException
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
			// TODO: Set the rest of the resource properties, or don't, they seem kinda useless to me
		} catch(Exception e) {
			throw new ResourceException("Unable to set site resource properties",e);
		}
	}

	public synchronized void create() throws ResourceException
	{
		logger.debug("Creating site resource "+getSite().getId());
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.create(site);
		} catch (DatabaseException de) {
			throw new ResourceException(de);
		}
	}

	public synchronized void load(ResourceKey key) throws ResourceException
	{
		logger.debug("Loading site resource "+key.getValue());
		int id = ((Integer)key.getValue()).intValue();
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			site = dao.load(id);
			setResourceProperties();
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void store() throws ResourceException
	{
		logger.debug("Storing site resource "+getSite().getId());
		
		// If we are removing, then don't store
		if (SiteState.REMOVING.equals(site.getState())) {
			throw new ResourceException("Site is being removed");
		}
		
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.store(site);
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
		logger.debug("Removing site resource "+getSite().getId());
		
		// TODO: Submit remove request
		try {
			GlobusCredential cred = ProxyUtil.getCallerCredential();
			ReadyQueue queue = ReadyQueue.getInstance();
			queue.add(new SiteStateChange(getKey(),SiteState.REMOVE));
		} catch (NamingException ne) {
			throw new ResourceException("Unable to remove site: "+ne.getMessage(),ne);
		}
		
		// TODO: Cancel staging operations
		
		// TODO: Cancel running glideins
		
		// TODO: Submit uninstall job
		
		// TODO: Delete the site
		//resource.delete();
		
		// TODO: Remove the site from the resource home
		/*
		ResourceKey key = AddressingUtil.getSiteKey(site.getId());
		SiteResourceHome resourceHome = SiteResourceHome.getInstance();
		resourceHome.remove(key);
		*/
	}
	
	public synchronized void submit() throws ResourceException 
	{
		logger.debug("Submitting site "+getSite().getId());
		
		// If we are not new, then don't submit
		if (!SiteState.NEW.equals(site.getState())) {
			throw new ResourceException("Can only submit sites in NEW state");
		}
		
		// TODO: Submit the staging job
		try {
			GlobusCredential cred = ProxyUtil.getCallerCredential();
			ReadyQueue queue = ReadyQueue.getInstance();
			queue.add(new SiteStateChange(getKey(),SiteState.SUBMIT));
		} catch (NamingException ne) {
			throw new ResourceException("Unable to schedule site: "+ne.getMessage(),ne);
		}
	}
	
	public synchronized void delete() throws ResourceException
	{
		// Remove the site from the database
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	public synchronized void updateState(SiteState state, String shortMessage, String longMessage)
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

	public synchronized void processStateChange(SiteState newState)
	{
		// TODO: Implement processStateChange
		
	}
	
	public void submitStagingJob() throws ResourceException
	{
		logger.debug("Submitting staging job for site '"+site.getName()+"'");
		
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
}
