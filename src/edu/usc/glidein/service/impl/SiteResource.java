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
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.service.exec.Condor;
import edu.usc.glidein.service.exec.CondorException;
import edu.usc.glidein.service.exec.CondorGridType;
import edu.usc.glidein.service.exec.CondorJob;
import edu.usc.glidein.service.exec.CondorUniverse;
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

//TODO: Improve dir hierarchy on remote

public class SiteResource implements Resource, ResourceIdentifier, PersistenceCallback, ResourceProperties
{
	private Logger logger = Logger.getLogger(SiteResource.class);
	private SimpleResourcePropertySet resourceProperties;
	private Site site;
	
	/**
	 * Default constructor required
	 */
	public SiteResource() { }
	
	public synchronized void setSite(Site site) throws ResourceException
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
		info("Creating site '"+site.getName()+"'");
		
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
		} catch (DatabaseException de) {
			throw new ResourceException(de);
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
			throw new ResourceException(de);
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
			Event event = new SiteEvent(code,getKey());
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
			Event event = new SiteEvent(SiteEventCode.SUBMIT,getKey());
			DelegationResource delegationResource = 
				DelegationUtil.getDelegationResource(credentialEPR);
			GlobusCredential credential = 
				delegationResource.getCredential();
			event.setProperty("credential", credential);
			EventQueue queue = EventQueue.getInstance();
			queue.add(event);
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get event queue",ne);
		} catch (DelegationException de) {
			throw new ResourceException("Unable to get delegated credential",de);
		}
	}
	
	private void updateState(SiteState state, String shortMessage, String longMessage)
	throws ResourceException
	{
		info("Changing state to "+state+": "+shortMessage);
		
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
			throw new ResourceException(
					"Unable to change state to "+state+": "+de.getMessage(),de);
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
		File dir = new File(getConfig().getTempDir(),"site-"+site.getId());
	
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
	
	private void submitInstallJob(GlobusCredential credential) throws ResourceException
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
		job.setCredential(credential);
		
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
			throw new ResourceException(de);
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
			throw new ResourceException(de);
		}
		
		// Create a remove event for each glidein
		try {
			EventQueue queue = EventQueue.getInstance();
			for (int id : ids) {
				ResourceKey key = AddressingUtil.getGlideinKey(id);
				Event event = new GlideinEvent(
						GlideinEventCode.REMOVE,key);
				queue.add(event);
			}
		} catch (NamingException ne) {
			throw new ResourceException("Unable to get queue: "+
					ne.getMessage(),ne);
		}
	}
	
	private void notifyGlideins() throws ResourceException
	{
		info("Notifying site glideins of "+SiteState.READY+" state");
		
		// Get glidein ids
		int[] ids;
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			ids = dao.getGlideinIds(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
		
		// Create a ready event for each glidein
		try {
			EventQueue queue = EventQueue.getInstance();
			for (int id : ids) {
				ResourceKey key = AddressingUtil.getGlideinKey(id);
				Event event = new GlideinEvent(
						GlideinEventCode.SITE_READY,key);
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
		
		// Find submit dir
		File dir = getInstallDirectory();
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
					"Unable to cancel install job: Unable to read job id",ioe);
		} catch (CondorException ce) {
			throw new ResourceException(
					"Unable to cancel install job: condor_rm failed",ce);
		}
	}
	
	private File getUninstallCredentialFile() throws ResourceException
	{
		File dir = getWorkingDirectory();
		File credFile = new File(dir,"credential");
		return credFile;
	}
	
	private void storeUninstallCredential(GlobusCredential credential)
	throws ResourceException
	{
		info("Storing uninstall credential");
		try {
			File credFile = getUninstallCredentialFile();
			CredentialUtil.store(credential,credFile);
		} catch (IOException e) {
			throw new ResourceException("Unable to save credential",e);
		}
	}
	
	private GlobusCredential loadUninstallCredential() 
	throws ResourceException
	{
		info("Loading uninstall credential");
		try {
			File credFile = getUninstallCredentialFile();
			return CredentialUtil.load(credFile);
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
		job.setCredential(loadUninstallCredential());
		
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
	
	private void delete() throws ResourceException
	{
		info("Deleting site from database");
		try {
			Database db = Database.getDatabase();
			SiteDAO dao = db.getSiteDAO();
			dao.delete(site.getId());
		} catch(DatabaseException de) {
			throw new ResourceException(de);
		}
	}
	
	private void fail(String message, Exception exception)
	{
		// Update status to FAILED
		error("Failure: "+message,exception);
		CharArrayWriter caw = new CharArrayWriter();
		exception.printStackTrace(new PrintWriter(caw));
		try {
			updateState(SiteState.FAILED, message, caw.toString());
		} catch (ResourceException re) {
			error("Unable to change state to "+SiteState.FAILED,re);
		}
	}
	
	public synchronized void handleEvent(SiteEvent event)
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
					try {
						// Change status to submitted
						updateState(SiteState.STAGING, "Staging executables", null);
						
						// Get delegated credential
						GlobusCredential credential = 
							(GlobusCredential)event.getProperty("credential");
						
						// Submit job
						submitInstallJob(credential);
					} catch (ResourceException re) {
						fail("Unable to submit install job",re);
					}
				} else {
					warn("State was not "+reqd+" when "+code+" was recieved");
				}
			} break;
				
			case INSTALL_SUCCESS: {
				SiteState reqd = SiteState.STAGING;
				if (reqd.equals(state)) {
					// Change status to READY
					try {
						updateState(SiteState.READY, "Installed", null);
					} catch (ResourceException re) {
						fail("Unable to update state",re);
					}
					
					// Notify any waiting glideins for this site
					try {
						notifyGlideins();
					} catch (ResourceException re) {
						warn("Unable to notify glideins",re);
					}
				} else {
					warn("State was not "+reqd+" when "+code+" was recieved");
				}
			} break;
			
			case INSTALL_FAILED: {
				SiteState reqd = SiteState.STAGING;
				if (reqd.equals(state)) {
					fail((String)event.getProperty("message"),
							(Exception)event.getProperty("exception"));
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
					try {
						cancelInstallJob();
					} catch (ResourceException re) {
						fail("Could not cancel install job",re);
					}
				}
				
				// Store the credential for later. We have to do this because
				// it is possible that there will be glideins that we need to
				// wait for. If that happens then the event with the credential
				// attached won't be available when we need to submit the
				// uninstall job.
				try {
					GlobusCredential credential = 
						(GlobusCredential)event.getProperty("credential");
					storeUninstallCredential(credential);
				} catch (ResourceException re) {
					fail("Could not store uninstall credential",re);
				}
				
				// Check to see if there are some glideins for this site
				boolean hasGlideins = true;
				try {
					hasGlideins = hasGlideins();
				} catch (ResourceException re) {
					fail("Unable to determine glidein state",re);
				}
				
				// If there are some glideins for this site
				if (hasGlideins) {
					// Cancel running glideins
					try {
						cancelGlideins();
					} catch (ResourceException re) {
						fail("Unable to cancel running glideins",re);
					}
					
					// Change state to EXITING
					try {
						updateState(SiteState.EXITING, "Waiting for glideins", null);
					} catch (ResourceException re) {
						error("Unable to change state to "+SiteState.EXITING,re);
					}
				} else {
					// Change state to REMOVING
					try {
						updateState(SiteState.REMOVING, "Removing site", null);
					} catch (ResourceException re) {
						error("Unable to change state to "+SiteState.REMOVING,re);
					}
					
					// Submit uninstall job
					try {
						submitUninstallJob();
					} catch (ResourceException re) {
						fail("Unable to submit uninstall job",re);
					}
				}
			} break;
				
			case GLIDEIN_DELETED: {
				// If exiting then check for last glidein
				SiteState reqd = SiteState.EXITING;
				if (reqd.equals(state)) {
					try {
						// If last glidein
						if (!hasGlideins()) {
							// Change state
							updateState(SiteState.REMOVING, "Removing site", null);
							
							// Submit uninstall job
							submitUninstallJob();
						}
					} catch (ResourceException re) {
						fail(re.getMessage(),re);
					}
				} else {
					/* This is expected to happen frequently */
				}
			} break;
			
			case UNINSTALL_SUCCESS:
			case DELETE: {
				// Set state to deleted in case any other event
				// handlers already have a reference to this resource
				site.setState(SiteState.DELETED);
				
				// Remove the Resource from the resource home
				try {
					SiteResourceHome resourceHome = SiteResourceHome.getInstance();
					resourceHome.remove(getKey());
				} catch (NamingException e) {
					fail("Unable to locate SiteResourceHome",e);
				} catch (ResourceException re) {
					fail("Unable to remove site from SiteResourceHome",re);
				}
				
				// Delete the site from the database
				try {
					delete();
				} catch (ResourceException re) {
					fail("Unable to delete site from database",re);
				}
				
				// Remove the working directory and all sub-directories
				try {
					IOUtil.rmdirs(getWorkingDirectory());
				} catch (ResourceException re) {
					fail("Unable to remove working directory",re);
				}
			} break;
			
			case UNINSTALL_FAILED: {
				SiteState reqd = SiteState.REMOVING;
				if (reqd.equals(state)) {
					fail((String)event.getProperty("message"),
							(Exception)event.getProperty("exception"));
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
