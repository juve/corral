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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

import edu.usc.glidein.api.GlideinException;
import edu.usc.glidein.api.SiteFactoryService;
import edu.usc.glidein.api.SiteService;
import edu.usc.glidein.service.SiteNames;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.INI;

public class CreateSiteCommand extends Command
{
	private File catalogFile = null;
	private SiteCatalogFormat catalogFormat = null;
	private String siteName = null;
	private GlobusCredential credential;
	
	private static enum SiteCatalogFormat {
		ini,
		xml,
		classic
	};
	
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
				  .setDescription("The format of the site catalog (one of: ini, xml, classic; default: ini)")
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
		String format = cmdln.getOptionValue("catalog-format","ini");
		try {
			catalogFormat = SiteCatalogFormat.valueOf(format);
		} catch (Exception e) {
			throw new CommandException("Invalid catalog format: "+format);
		}
				
		/* The chosen site name (if any) */
		siteName = cmdln.getOptionValue("site-name");
		
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
	}
	
	public void execute() throws CommandException
	{
		createSites(extractSites(catalogFile,catalogFormat,siteName));
	}

	public List<Site> extractSites(File catalogFile, SiteCatalogFormat catalogFormat, String site)
	throws CommandException
	{
		if (isDebug()) System.out.printf("Reading sites from '%s'...\n",catalogFile.getName());
		List<Site> sites;
		switch (catalogFormat) {
			case ini:
				sites = extractINISites(catalogFile, site);
				break;
			case xml:
			case classic:
			default:
				throw new CommandException("Sorry, "+catalogFormat+
						" format catalogs are not yet supported");
		}
		if (isDebug()) System.out.println("Done reading sites.");
		return sites;
	}

	public List<Site> extractINISites(File iniFile, String site) throws CommandException
	{
		List<Site> sites = new LinkedList<Site>();
		INI ini = new INI();
		try {
			ini.read(iniFile);
		} catch (Exception e) {
			throw new CommandException("Error reading INI catalog file. " +
					"Are you sure it is the right format? " +
					"Try using --catalog-format.\n", e);
		}
		if (site == null) {
			for (String name : ini.getSections()) {
				sites.add(extractINISite(ini,name));
			}
		} else {
			if (ini.hasSection(site)) {
				sites.add(extractINISite(ini,site));
			} else {
				throw new CommandException("Site '"+site+"' not found in site catalog");
			}
		}
		return sites;
	}
	
	public Site extractINISite(INI ini, String name) throws CommandException
	{
		if (isDebug()) System.out.printf("Reading site '%s'... ",name);
		Site s = new Site();
		s.setName(name);
		s.setInstallPath(getINIValue(ini,name,"installPath"));
		s.setLocalPath(getINIValue(ini,name,"localPath"));
		s.setCondorPackage(getINIValue(ini,name,"condorPackage"));
		s.setCondorVersion(getINIValue(ini,name,"condorVersion"));
		
		/* Staging service */
		try {
			String staging = getINIValue(ini,name,"stagingService");
			if (staging == null) {
				throw new CommandException("Missing required parameter 'stagingService' " +
						"for site '"+name+"'");
			}
			String[] comp = staging.trim().split("[ ]", 2);
			ExecutionService stagingService = new ExecutionService();
			stagingService.setProject(getINIValue(ini,name,"project"));
			stagingService.setQueue(getINIValue(ini,name,"queue"));
			stagingService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			stagingService.setServiceContact(comp[1]);
			s.setStagingService(stagingService);
		} catch (Exception e) {
			throw new CommandException("Unable to create staging service for site '"+name+"'. " +
					"Are you sure you used the right format for stagingService?");
		}
		
		/* Glidein service */
		try {
			String glidein = getINIValue(ini,name,"glideinService");
			if (glidein == null) {
				throw new CommandException("Missing required parameter 'glideinService' " +
						"for site '"+name+"'");
			}
			String[] comp = glidein.trim().split("[ ]", 2);
			ExecutionService glideinService = new ExecutionService();
			glideinService.setProject(getINIValue(ini,name,"project"));
			glideinService.setQueue(getINIValue(ini,name,"queue"));
			glideinService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			glideinService.setServiceContact(comp[1]);
			s.setGlideinService(glideinService);
		} catch (Exception e) {
			throw new CommandException("Unable to create glidein service for site '"+name+"'. " +
					"Are you sure you used the right format for glideinService?");
		}
		
		/* Environment */
		String env = getINIValue(ini,name,"environment");
		if (env!=null) {
			List<EnvironmentVariable> envs = new LinkedList<EnvironmentVariable>();
			Pattern p = Pattern.compile("([^=]+)=([^:]+):?");
			Matcher m = p.matcher(env);
			while (m.find()) {
				EnvironmentVariable e = new EnvironmentVariable();
				e.setVariable(m.group(1));
				e.setValue(m.group(2));
				envs.add(e);
			}
			s.setEnvironment(envs.toArray(new EnvironmentVariable[0]));
		}
		
		if (isDebug()) System.out.println("done.");
		return s;
	}

	private String getINIValue(INI ini, String site, String key)
	{
		String value = ini.getString(site, key, null);
		if (value == null) {
			value = ini.getString(key, null);
		}
		if (value != null) {
			value = value.trim();
		}
		return value;
	}

	public void createSites(List<Site> sites) throws CommandException
	{
		if (isDebug()) System.out.printf("Creating sites...\n");
		
		// Delegate credential
		EndpointReferenceType credentialEPR = delegateCredential(credential);
		
		// Create sites
		for (Site site : sites) {
			try {
				SiteFactoryService factory = new SiteFactoryService(
						getServiceURL(SiteNames.SITE_FACTORY_SERVICE));
				factory.setDescriptor(getClientSecurityDescriptor());
				EndpointReferenceType epr = factory.createSite(site);
				SiteService instance = new SiteService(epr);
				instance.setDescriptor(getClientSecurityDescriptor());
				instance.submit(credentialEPR);
			} catch (GlideinException ge) {
				throw new CommandException("Unable to create site: "+
						site.getName()+": "+ge.getMessage(),ge);
			}
		}
		if (isDebug()) System.out.printf("Done creating sites.\n");
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
