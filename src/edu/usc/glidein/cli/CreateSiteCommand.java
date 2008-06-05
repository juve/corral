package edu.usc.glidein.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.usc.glidein.stubs.SiteFactoryPortType;
import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.service.SiteFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.service.SiteServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.AddressingUtil;
import edu.usc.glidein.util.INI;

public class CreateSiteCommand extends Command
{
	private Options options = null;
	private List<Site> sites = null;
	private URL siteFactoryURL = null;
	
	private static enum SiteCatalogFormat {
		ini,
		xml,
		classic
	};
	
	@SuppressWarnings("static-access")
	public CreateSiteCommand()
	{
		sites = new LinkedList<Site>();
		options = new Options();
		options.addOption(
			OptionBuilder.withLongOpt("catalog-file")
						 .isRequired()
						 .withDescription("-c [--catalogFile] <file>        : " +
						 		"The catalog file containing sites")
						 .hasArg()
						 .create("c")
		);
		options.addOption(
			OptionBuilder.withLongOpt("catalog-format")
						 .withDescription("-f [--catalog-format] <format>   : " +
						 		"The format of the site catalog (one of: ini, xml, classic; default: ini)")
						 .hasArg()
						 .create("f")
		);
		options.addOption(
			OptionBuilder.withLongOpt("site-name")
						 .withDescription("-n [--site-name] <name>          : " +
						 		"The name of the site to create (default: all)")
						 .hasArg()
						 .create("n")
		);
		options.addOption(
			OptionBuilder.withLongOpt("factory")
						 .withDescription("-F [--factory] <contact>         : " +
						 		"The factory URL (default: "+AddressingUtil.SITE_FACTORY_SERVICE_URL+")")
						 .hasArg()
						 .create("F")
		);
	}
	
	public void invoke(String[] args) throws CommandException
	{
		/* Parse args */
		CommandLine cmdln = null;
		try {
			CommandLineParser parser = new PosixParser();
			cmdln = parser.parse(options, args);
		} catch (ParseException pe) {
			throw new CommandException("Invalid argument: "+pe.getMessage());
		}
			
		/* Check for extra arguments */
		args = cmdln.getArgs();
		if (args.length > 0) {
			throw new CommandException("Unrecognized argument: "+args[0]);
		}
			
		/* Catalog */
		String catalog = cmdln.getOptionValue("catalog-file");
		File catalogFile = new File(catalog);
		if (!catalogFile.isFile()) {
			throw new CommandException("Invalid catalog file: "+catalog);
		}
				
		/* Catalog format */
		String format = cmdln.getOptionValue("catalog-format","ini");
		SiteCatalogFormat catalogFormat = null;
		try {
			catalogFormat = SiteCatalogFormat.valueOf(format);
		} catch (Exception e) {
			throw new CommandException("Invalid catalog format: "+format);
		}
				
		/* Factory URL */
		try {
			if (cmdln.hasOption("F")) {
				siteFactoryURL = new URL(cmdln.getOptionValue("factory"));
			} else {
				siteFactoryURL = new URL(AddressingUtil.SITE_FACTORY_SERVICE_URL);
			}
			if (isDebug()) System.out.println("SiteFactoryService: "+siteFactoryURL);
		} catch (MalformedURLException e) {
			throw new CommandException("Invalid site factory URL: "+e.getMessage(),e);
		}
				
		/* Create the site */
		String site = cmdln.getOptionValue("site-name");
		extractSites(catalogFile,catalogFormat,site);
		createSites();
	}
	
	public List<Site> getSites()
	{
		return sites;
	}
	
	public void setSites(List<Site> sites)
	{
		this.sites = sites;
	}

	public void extractSites(File catalogFile, SiteCatalogFormat catalogFormat, String site)
	throws CommandException
	{
		if (isDebug()) System.out.printf("Reading sites from '%s'...\n",catalogFile.getName());
		switch (catalogFormat) {
			case ini:
				extractINISites(catalogFile, site);
				break;
			case xml:
			case classic:
			default:
				throw new CommandException("Sorry, "+catalogFormat+
						" format catalogs are not yet supported");
		}
		if (isDebug()) System.out.println("Done reading sites.");
	}

	public void extractINISites(File iniFile, String site) throws CommandException
	{
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

	public void createSites() throws CommandException
	{
		if (isDebug()) System.out.printf("Creating sites...\n");
		for (Site s : sites) {
			EndpointReferenceType epr = createSite(s);
			submitSite(epr,s);
		}
		if (isDebug()) System.out.printf("Done creating sites.\n");
	}
	
	public EndpointReferenceType createSite(Site site) throws CommandException
	{
		EndpointReferenceType siteEPR;
		
		if (isDebug()) System.out.printf("Creating site '%s'... ",site.getName());
		try {
			// Look up the site factory
			EndpointReferenceType siteFactoryEPR = 
				AddressingUtil.getSiteFactoryEPR(siteFactoryURL);
			SiteFactoryServiceAddressingLocator locator = 
				new SiteFactoryServiceAddressingLocator();
			SiteFactoryPortType factory = 
				locator.getSiteFactoryPortTypePort(siteFactoryEPR);
			
			// Use GSI Secure Conversation so that the service can
			// retrieve the user's subject name
			((Stub)factory)._setProperty(
					org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
					org.globus.wsrf.security.Constants.SIGNATURE);
			
			// Use self authorization
			((Stub)factory)._setProperty(
					org.globus.wsrf.security.Constants.AUTHORIZATION,
					org.globus.wsrf.impl.security.authorization.SelfAuthorization.getInstance());
			
			// Create the site
			siteEPR = factory.createSite(site);
		} catch (Exception e) {
			throw new CommandException("Unable to create site '"+site.getName()+"': "+
					"Error communicating with service: "+e.getMessage(), e);
		}
		if (isDebug()) System.out.printf("done.\n");
		
		return siteEPR;
	}
	
	public void submitSite(EndpointReferenceType siteEPR, Site site) throws CommandException
	{
		if (isDebug()) System.out.printf("Submitting site '%s'... ",site.getName());
		try {
			// Look up the site instance
			SiteServiceAddressingLocator locator = 
				new SiteServiceAddressingLocator();
			SitePortType instance = 
				locator.getSitePortTypePort(siteEPR);
			
			// Use GSI Secure Conversation
			((Stub)instance)._setProperty(
					org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
					org.globus.wsrf.security.Constants.SIGNATURE);
			
			// Use full delegation so the service can submit the job
			((Stub)instance)._setProperty(
					org.globus.axis.gsi.GSIConstants.GSI_MODE, 
					org.globus.axis.gsi.GSIConstants.GSI_MODE_FULL_DELEG);
			
			// Use self authorization
			((Stub)instance)._setProperty(
					org.globus.wsrf.security.Constants.AUTHORIZATION,
					org.globus.wsrf.impl.security.authorization.SelfAuthorization.getInstance());
			
			// Create the site
			instance.submit(new EmptyObject());
		} catch (Exception e) {
			throw new CommandException("Unable to submit site '"+site.getName()+"': "+
					"Error communicating with service: "+e.getMessage(), e);
		}
		if (isDebug()) System.out.printf("done.\n");
	}

	public String getName()
	{
		return "create-site";
	}
	
	public String[] getAliases() {
		return new String[]{"cs"};
	}
	
	public String getHelp()
	{
		StringBuffer buff = new StringBuffer();
		buff.append("create-site (cs): Add a new site\n");
		buff.append("usage: create-site --catalog-file <file>\n");
		buff.append("\n");
		buff.append("Valid options:\n");
		@SuppressWarnings("unchecked")
		Collection<Option> collection = options.getOptions();
		Option[] ops = collection.toArray(new Option[0]);
		for (Option op : ops) {
			buff.append("   ");
			buff.append(op.getDescription());
			buff.append("\n");
		}
		return buff.toString();
	}
}
