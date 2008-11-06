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
package edu.usc.glidein.cli;

import static edu.usc.glidein.service.SiteNames.*;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

import edu.usc.glidein.api.SiteFactoryService;
import edu.usc.glidein.api.SiteListener;
import edu.usc.glidein.api.SiteService;
import edu.usc.glidein.catalog.SiteCatalog;
import edu.usc.glidein.catalog.SiteCatalogException;
import edu.usc.glidein.catalog.SiteCatalogFactory;
import edu.usc.glidein.catalog.SiteCatalogFormat;
import edu.usc.glidein.service.SiteNames;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;
import edu.usc.glidein.stubs.types.SiteStateChange;
import edu.usc.glidein.util.SiteUtil;

public class CreateSiteCommand extends Command implements SiteListener
{
	private File catalogFile = null;
	private SiteCatalogFormat catalogFormat = null;
	private Site site = null;
	private GlobusCredential credential;
	private boolean verbose;
	private boolean wait;
	private CommandException exception;
	
	public void addOptions(List<Option> options)
	{
		options.add(
			Option.create()
				  .setOption("c")
				  .setLongOption("catalog-file")
				  .setUsage("-c [--catalog-file] <file>")
				  .setDescription("The catalog file containing sites")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("f")
				  .setLongOption("catalog-format")
				  .setUsage("-f [--catalog-format] <format>")
				  .setDescription("The format of the site catalog (one of: 'ini', or 'xml'; \n" +
				  				  "default: determined by extension)")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("n")
				  .setLongOption("site-name")
				  .setUsage("-n [--site-name] <name>")
				  .setDescription("The name of the site to create")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("C")
				  .setLongOption("credential")
				  .setUsage("-C [--credential] <file>")
				  .setDescription("The user's credential as a proxy file. If not specified the \n" +
				  				  "Globus default is used.")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("v")
				  .setLongOption("verbose")
				  .setUsage("-v [--verbose]")
				  .setDescription("Show details about the new site")
		);
		
		options.add(
			Option.create()
				  .setOption("ip")
				  .setLongOption("install-path")
				  .setUsage("-ip [--install-path] <path>")
				  .setDescription("This is the remote path where executables are installed. \n" +
				  				  "Default: $HOME/glidein")
				  .hasArgument()
		);
		
		
		options.add(
			Option.create()
				  .setOption("lp")
				  .setLongOption("local-path")
				  .setUsage("-lp [--local-path] <path>")
				  .setDescription("This is the remote path where log files, etc. are placed \n" +
				  				  "(i.e. local scratch). Default: /tmp/glidein")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("ss")
				  .setLongOption("staging-service")
				  .setUsage("-ss [--staging-service] <svc>")
				  .setDescription("This is the gatekeeper to use for setup (i.e. fork). The \n" +
						  		  "format follows the condor format for grid resource. Only \n" +
						  		  "the gt2 and gt4 grid types are supported right now. (e.x. \n" +
				  				  "'gt2 dynamic.usc.edu/jobmanager-fork')")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("ssp")
				  .setLongOption("staging-service-project")
				  .setUsage("-ssp [--staging-service-project] <proj>")
				  .setDescription("The project to use for the staging service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("ssq")
				  .setLongOption("staging-service-queue")
				  .setUsage("-ssq [--staging-service-queue] <queue>")
				  .setDescription("The queue to use for the staging service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("gs")
				  .setLongOption("glidein-service")
				  .setUsage("-gs [--glidein-service] <svc>")
				  .setDescription("This is the gatekeeper to use for glideins (i.e. pbs). \n" +
				  				  "The format is identical to the one for the staging service.")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("gsp")
				  .setLongOption("glidein-service-project")
				  .setUsage("-gsp [--glidein-service-project] <proj>")
				  .setDescription("The project to use for the glidein service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("gsq")
				  .setLongOption("glidein-service-queue")
				  .setUsage("-gsq [--glidein-service-queue] <queue>")
				  .setDescription("The queue to use for the glidein service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("cv")
				  .setLongOption("condor-version")
				  .setUsage("-cv [--condor-version] <ver>")
				  .setDescription("The version of Condor to setup on the remote site. \n" +
				  				  "(e.x '7.0.0')")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("cp")
				  .setLongOption("condor-package")
				  .setUsage("-cp [--condor-package] <pkg>")
				  .setDescription("The name of the package to download. This overrides condor-version. \n" +
				  				  "The typical format for the package name is: \n" +
				  				  "'<condorversion>-<arch>-<os>-<osversion>-glibc<glibcversion>.tar.gz' \n" +
				  				  "(e.x. '7.0.0-ia64-Linux-2.4-glibc2.2.tar.gz'), but you can \n" +
				  				  "specify any package name you like, as long as it is mapped in RLS. \n" +
				  				  "The value can be a simple file name like the example above, an \n" +
				  				  "absolute path, or a URL. The protocols supported for URLs include: \n" +
				  				  "file, http, https, ftp, and gsiftp. In most cases the value will end \n" +
				  				  "with .tar.gz. If you provide a package name that is not an \n" +
				  				  "absolute path or URL, then the service will try to look it up in RLS.")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("e")
				  .setLongOption("environment")
				  .setUsage("-e [--environment] <env>")
				  .setDescription("This is the environment for staging and glideins. Use ':' \n" +
				  				  "to separate entries. (e.x. 'FOO=f:BAR=b')")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("W")
				  .setLongOption("wait")
				  .setUsage("-W [--wait]")
				  .setDescription("Block waiting for the site to become READY, FAILED or DELETED.")
		);
	}
	
	private void setProperty(Properties p, String name, String value)
	{
		if (value == null) return;
		p.setProperty(name, value);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException
	{
		/* Check for extra arguments */
		String[] args = cmdln.getArgs();
		if (args.length > 0) {
			throw new CommandException("Unrecognized argument: "+args[0]);
		}
		
		/* The name of the new site */
		String siteName;
		if (cmdln.hasOption("n")) {
			siteName = cmdln.getOptionValue("site-name");
		} else {
			throw new CommandException("Missing site-name argument");
		}
		
		/* If we are loading the site from a catalog */
		if (cmdln.hasOption("c")) {
			
			/* Catalog file */
			String catalog = cmdln.getOptionValue("catalog-file");
			catalogFile = new File(catalog);
			if (!catalogFile.isFile()) {
				throw new CommandException("Invalid catalog file: "+catalog);
			}
			
			/* Catalog format */
			if (cmdln.hasOption("f")) {
				String format = cmdln.getOptionValue("catalog-format");
				try {
					catalogFormat = SiteCatalogFormat.valueOf(format.toUpperCase());
				} catch (Exception e) {
					throw new CommandException("Invalid catalog format: "+format);
				}
			}
			
			site = getSite(catalogFile,catalogFormat,siteName);
		} 
		
		/* If we are getting the properties from the command-line */
		else {
			try {
				Properties p = new Properties();
				setProperty(p, NAME, siteName);
				setProperty(p, INSTALL_PATH, cmdln.getOptionValue("ip","$HOME/glidein"));
				setProperty(p, LOCAL_PATH, cmdln.getOptionValue("lp","/tmp/glidein"));
				setProperty(p, STAGING_SERVICE, cmdln.getOptionValue("ss"));
				setProperty(p, STAGING_SERVICE_PROJECT, cmdln.getOptionValue("ssp"));
				setProperty(p, STAGING_SERVICE_QUEUE, cmdln.getOptionValue("ssq"));
				setProperty(p, GLIDEIN_SERVICE, cmdln.getOptionValue("gs"));
				setProperty(p, GLIDEIN_SERVICE_PROJECT, cmdln.getOptionValue("gsp"));
				setProperty(p, GLIDEIN_SERVICE_QUEUE, cmdln.getOptionValue("gsq"));
				setProperty(p, CONDOR_VERSION, cmdln.getOptionValue("cv"));
				setProperty(p, CONDOR_PACKAGE, cmdln.getOptionValue("cp"));
				setProperty(p, ENVIRONMENT, cmdln.getOptionValue("e"));
				site = SiteUtil.createSite(p);
			} catch (Exception e) {
				throw new CommandException(
						"Unable to create site: "+e.getMessage(),e);
			}
		}
		
		/* Get proxy credential */
		if (cmdln.hasOption("C")) {
			String proxy = cmdln.getOptionValue("C");
			try {
				credential = new GlobusCredential(proxy);
			} catch (GlobusCredentialException ce) {
				throw new CommandException("Unable to read proxy " +
						"credential: "+proxy+": "+ce.getMessage(),ce);
			}
		} else {
			try {
				credential = GlobusCredential.getDefaultCredential();
			} catch (GlobusCredentialException ce) {
				throw new CommandException("Unable to read default proxy " +
						"credential: "+ce.getMessage(),ce);
			}
		}
		
		/* Verbose */
		if (cmdln.hasOption("v")) {
			verbose = true;
		} else {
			verbose = false;
		}
		
		/* Wait */
		if (cmdln.hasOption("W")) {
			wait = true;
		} else {
			wait = false;
		}
	}
	
	public void execute() throws CommandException
	{
		createSite(site);
	}

	public Site getSite(File catalogFile, SiteCatalogFormat catalogFormat, String siteName)
	throws CommandException
	{
		if (isDebug()) System.out.printf("Reading site from '%s'...\n",catalogFile.getName());
		Site site = null;
		try {
			SiteCatalog catalog = null;
			if (catalogFormat == null) {
				catalog = SiteCatalogFactory.getSiteCatalog(catalogFile);
			} else {
				catalog = SiteCatalogFactory.getSiteCatalog(catalogFile,catalogFormat);
			}
			site = catalog.getSite(siteName);
		} catch (SiteCatalogException sce) {
			throw new CommandException("Unable to read site from catalog",sce);
		}
		if (isDebug()) System.out.println("Done reading site.");
		return site;
	}

	public void createSite(Site site) throws CommandException
	{
		if (isDebug()) System.out.printf("Creating site...\n");
		
		// Delegate credential
		EndpointReferenceType credentialEPR = delegateCredential(credential);
		
		try {
			// Create site
			SiteFactoryService factory = new SiteFactoryService(
					getServiceURL(SiteNames.SITE_FACTORY_SERVICE));
			factory.setDescriptor(getClientSecurityDescriptor());
			EndpointReferenceType epr = factory.create(site);
			
			// Get instance
			SiteService instance = new SiteService(epr);
			instance.setDescriptor(getClientSecurityDescriptor());
			
			// If verbose, print details
			if (verbose) {
				site = instance.getSite();
				SiteUtil.print(site);
				System.out.println();
			}
			
			if (isDebug()) System.out.printf("Site created.\n");

			// Submit the new site
			instance.submit(credentialEPR);
			
			if (isDebug()) System.out.printf("Site submitted.\n");
			
			// Wait for started event
			if (wait) {
				if (isDebug()) { 
					System.out.println("Waiting for site "+site.getId()+"...");
				}
				
				// Subscribe
				SiteService s = new SiteService(
						new URL("https://juve.usc.edu:8443/wsrf/services/glidein/SiteService"),
						site.getId());
				s.addListener(this);
				s.addListener(new SiteListener(){
					public void stateChanged(SiteStateChange stateChange)
					{
						System.out.println("State changed: "+stateChange.getState());
					}
				});
				//instance.addListener(this);
				
				// Wait for state change
				while (wait) {
					if (isDebug()) System.out.print(".");
					try {
						Thread.sleep(10000);
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}
				}
				
				// Unsubscribe
				s.removeListener(this);
				//instance.removeListener(this);
				
				if (isDebug()) {
					System.out.println("Finished waiting.");
				}
				
				// Throw the exception if it failed
				if (exception != null) {
					throw exception;
				}
			}
		} catch (Exception ge) {
			throw new CommandException("Unable to create site: "+
					site.getName()+": "+ge.getMessage(),ge);
		}
	}

	public String getName()
	{
		return "create-site";
	}
	
	public String[] getAliases()
	{
		return new String[]{"cs"};
	}
	
	public String getUsage()
	{
		return "Usage:\n" +
				"   1. create-site [options] [--catalog-format <format>] --catalog-file <file> --site-name <name>\n" +
				"\n" +
				"   2. create-site [options] --site-name <name> --install-path <path> --local-path <path>\n" +
				"                  --staging-service <svc> [--staging-service-project <proj>] [--staging-service-queue <queue>]\n" +
				"                  --glidein-service <svc> [--glidein-service-project <proj>] [--glidein-service-queue <queue>]\n" +
				"                  [--condor-version <ver> | --condor-package <pkg>] [--environment <env>]";
	}
	
	public String getDescription()
	{
		return "create-site (cs): Add a new site";
	}
	
	public void stateChanged(SiteStateChange stateChange)
	{
		SiteState state = stateChange.getState();
		if (isDebug()) {
			System.out.println("Site state changed to "+state);
			System.out.println("\tShort message: "+stateChange.getShortMessage());
			if (stateChange.getLongMessage() != null)
				System.out.println("\tLong message:\n"+stateChange.getLongMessage());
		}
		
		// If the new state is running, failed or deleted, then stop waiting
		if (state.equals(SiteState.READY)) {
			wait = false;
		} else if (state.equals(SiteState.FAILED) || 
				state.equals(SiteState.DELETED)) {
			exception = new CommandException("Site became "+state+": "+
					stateChange.getShortMessage()+"\n"+stateChange.getLongMessage());
			wait = false;
		}
	}
}
