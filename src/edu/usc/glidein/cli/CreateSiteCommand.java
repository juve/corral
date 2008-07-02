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

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
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

// TODO: Allow all site parameters to be specified on the command-line

public class CreateSiteCommand extends Command
{
	private File catalogFile = null;
	private SiteCatalogFormat catalogFormat = null;
	private String siteName = null;
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
				  .setDescription("The name of the site to create (default: all)")
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
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException
	{
		/* Check for extra arguments */
		String[] args = cmdln.getArgs();
		if (args.length > 0) {
			throw new CommandException("Unrecognized argument: "+args[0]);
		}
			
		/* Catalog */
		if (cmdln.hasOption("c")) {
			String catalog = cmdln.getOptionValue("catalog-file");
			catalogFile = new File(catalog);
			if (!catalogFile.isFile()) {
				throw new CommandException("Invalid catalog file: "+catalog);
			}
		} else {
			throw new CommandException("Missing catalog-file argument");
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
				
		/* The site to create */
		if (cmdln.hasOption("n")) {
			siteName = cmdln.getOptionValue("site-name");
		} else {
			throw new CommandException("Missing site-name argument");
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
		createSite(getSite(catalogFile,catalogFormat,siteName));
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
		return "Usage: create-site --catalog-file <file>";
	}
	
	public String getDescription()
	{
		return "create-site (cs): Add a new site";
	}
}
