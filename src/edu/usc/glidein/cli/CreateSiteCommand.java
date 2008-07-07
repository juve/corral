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

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

import edu.usc.glidein.api.GlideinException;
import edu.usc.glidein.api.SiteFactoryService;
import edu.usc.glidein.api.SiteService;
import edu.usc.glidein.catalog.SiteCatalog;
import edu.usc.glidein.catalog.SiteCatalogException;
import edu.usc.glidein.catalog.SiteCatalogFactory;
import edu.usc.glidein.catalog.SiteCatalogFormat;
import edu.usc.glidein.service.SiteNames;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.SiteUtil;

// TODO : Allow the user to specify rsl

public class CreateSiteCommand extends Command
{
	private File catalogFile = null;
	private SiteCatalogFormat catalogFormat = null;
	private Site site = null;
	private GlobusCredential credential;
	private boolean verbose;
	
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
				  .setDescription("The format of the site catalog (one of: 'ini', or 'xml'; \ndefault: determined by extension)")
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
				  .setDescription("The user's credential as a proxy file. If not specified the Globus default is used.")
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
				  .setDescription("This is the remote path where executables are installed")
				  .hasArgument()
		);
		
		
		options.add(
			Option.create()
				  .setOption("lp")
				  .setLongOption("local-path")
				  .setUsage("-lp [--local-path] <path>")
				  .setDescription("This is the remote path where log files, etc. are placed (i.e. scratch)")
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
				  .setDescription("The version of Condor to setup on the remote site. (e.x '7.0.0')")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("cp")
				  .setLongOption("condor-package")
				  .setUsage("-cp [--condor-package] <pkg>")
				  .setDescription("The package name to download. This overrides condor-version. Don't use condor-package \n" +
				  				  "unless you know what you are doing. (e.x. '7.0.0-ia64-Linux-2.4-glibc2.2'). The \n" +
				  				  "general format for the package name itself is: \n" +
				  				  "'<version>-<arch>-<os>-<osversion>-glibc<glibcversion>')")
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
			
			Properties props = new Properties();
			
			props.setProperty("name", siteName);
			
			String installPath = cmdln.getOptionValue("ip");
			if (installPath != null) {
				props.setProperty("installPath", installPath);
			}
			
			String localPath = cmdln.getOptionValue("lp");
			if (localPath != null) {
				props.setProperty("localPath", localPath);
			}
			
			String stagingService = cmdln.getOptionValue("ss");
			if (stagingService != null) {
				props.setProperty("stagingService", stagingService);
			}
			
			String stagingServiceProject = cmdln.getOptionValue("ssp");
			if (stagingServiceProject != null) {
				props.setProperty("stagingService.project", stagingServiceProject);
			}
			
			String stagingServiceQueue = cmdln.getOptionValue("ssq");
			if (stagingServiceQueue != null) {
				props.setProperty("stagingService.queue", stagingServiceQueue);
			}
			
			String glideinService = cmdln.getOptionValue("gs");
			if (glideinService != null) {
				props.setProperty("glideinService", glideinService);
			}
			
			String glideinServiceProject = cmdln.getOptionValue("gsp");
			if (glideinServiceProject != null) {
				props.setProperty("glideinService.project", glideinServiceProject);
			}
			
			String glideinServiceQueue = cmdln.getOptionValue("gsq");
			if (glideinServiceQueue != null) {
				props.setProperty("glideinService.queue", glideinServiceQueue);
			}
			
			String condorVersion = cmdln.getOptionValue("cv");
			if (condorVersion != null) {
				props.setProperty("condorVersion", condorVersion);
			}
			
			String condorPackage = cmdln.getOptionValue("cp");
			if (condorPackage != null) {
				props.setProperty("condorPackage", condorPackage);
			}
			
			String environment = cmdln.getOptionValue("e");
			if (environment != null) {
				props.setProperty("environment", environment);
			}
			
			try {
				site = SiteUtil.createSite(props);
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
			EndpointReferenceType epr = factory.createSite(site);
			
			// Get instance
			SiteService instance = new SiteService(epr);
			instance.setDescriptor(getClientSecurityDescriptor());
			
			// If verbose, print details
			if (verbose) {
				site = instance.getSite();
				SiteUtil.print(site);
				System.out.println();
			}

			// Submit the new site
			instance.submit(credentialEPR);
			
		} catch (GlideinException ge) {
			throw new CommandException("Unable to create site: "+
					site.getName()+": "+ge.getMessage(),ge);
		}
		
		if (isDebug()) System.out.printf("Done creating site.\n");
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
}
