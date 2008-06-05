package edu.usc.glidein.client.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.usc.glidein.GlideinException;
import edu.usc.glidein.stubs.SiteFactoryPortType;
import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.service.SiteFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.service.SiteServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.AddressingUtil;

public class ListSiteCommand extends Command
{
	private Options options;
	private boolean longFormat = false;
	private URL siteFactoryURL = null;
	private URL siteURL = null;
	
	@SuppressWarnings("static-access")
	public ListSiteCommand()
	{
		options = new Options();
		options.addOption(
				OptionBuilder.withLongOpt("long")
							 .withDescription("-l [--long]                 : " +
							 		"Show detailed information")
							 .create("l")
		);
		options.addOption(
				OptionBuilder.withLongOpt("factory")
							 .withDescription("-F [--factory] <contact>    : " +
							 		"The factory url (default: "+AddressingUtil.SITE_FACTORY_SERVICE_URL+")")
							 .hasArg()
							 .create("F")
		);
		options.addOption(
				OptionBuilder.withLongOpt("service")
							 .withDescription("-S [--service] <contact>    : " +
							 		"The service url (default: "+AddressingUtil.SITE_SERVICE_URL+")")
							 .hasArg()
							 .create("S")
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
			
		/* Long format/short format */
		if (cmdln.hasOption("l")) { 
			longFormat = true;
		}
		
		/* SiteFactoryService URL */
		try {
			if (cmdln.hasOption("F")) {
				siteFactoryURL = new URL(cmdln.getOptionValue("factory"));
			} else {
				siteFactoryURL = new URL(AddressingUtil.SITE_FACTORY_SERVICE_URL);
			}
			if (isDebug()) System.out.println("SiteFactoryService: "+siteFactoryURL);
		} catch(MalformedURLException e) {
			throw new CommandException("Invalid site factory service URL: "+e.getMessage(),e);
		}
		
		/* SiteService URL */
		try {
			if (cmdln.hasOption("S")) {
				siteURL = new URL(cmdln.getOptionValue("service"));
			} else {
				siteURL = new URL(AddressingUtil.SITE_SERVICE_URL);
			}
			if (isDebug()) System.out.println("SiteService: "+siteURL);
		} catch (MalformedURLException e) {
			throw new CommandException("Invalid site service URL: "+e.getMessage(),e);
		}
		
		/* Check for specific arguments */
		args = cmdln.getArgs();
		if (args.length > 0) {
			listIndividualSites(args);
		} else {
			listAllSites();
		}
	}
	
	public void listAllSites() throws CommandException
	{
		if (isDebug()) System.out.printf("Listing sites\n");
		Site[] sites;
		try {
			EndpointReferenceType siteFactoryEPR = 
				AddressingUtil.getSiteFactoryEPR(siteFactoryURL);
			SiteFactoryServiceAddressingLocator locator = 
				new SiteFactoryServiceAddressingLocator();
			SiteFactoryPortType factory = 
				locator.getSiteFactoryPortTypePort(siteFactoryEPR);
			
			// Use GSI Secure Conversation
			((Stub)factory)._setProperty(
					org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
					org.globus.wsrf.security.Constants.SIGNATURE);

			// Use self authorization
			((Stub)factory)._setProperty(
					org.globus.wsrf.security.Constants.AUTHORIZATION,
					org.globus.wsrf.impl.security.authorization.SelfAuthorization.getInstance());
			
			// Get the sites
			sites = factory.listSites(longFormat).getSites();
		} catch (Exception e) {
			throw new CommandException("Unable to list sites: "+
					"Error communicating with service: "+e.getMessage(), e);
		}
		
		// Print out the site list
		printSites(sites);
		
		if (isDebug()) System.out.printf("Done listing sites.\n");
	}
	
	public void listIndividualSites(String[] siteIds) throws CommandException
	{
		LinkedList<Site> sites = new LinkedList<Site>();
		SiteServiceAddressingLocator siteInstanceLocator = new SiteServiceAddressingLocator();
		for (String siteId : siteIds) {
			try {
				int id = Integer.parseInt(siteId);
				EndpointReferenceType siteEPR = 
					AddressingUtil.getSiteEPR(siteURL,id);
				SitePortType instance = 
					siteInstanceLocator.getSitePortTypePort(siteEPR);
					
				// Use GSI Secure Conversation
				((Stub)instance)._setProperty(
						org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
						org.globus.wsrf.security.Constants.SIGNATURE);
				
				// Use self authorization
				((Stub)instance)._setProperty(
						org.globus.wsrf.security.Constants.AUTHORIZATION,
						org.globus.wsrf.impl.security.authorization.SelfAuthorization.getInstance());
				
				Site site = instance.getSite(new EmptyObject());
				sites.add(site);
			} catch (NumberFormatException nfe) {
				System.out.println("Invalid site id: "+siteId);
			} catch (GlideinException ge) {
				throw new CommandException("Unable to get EPR: "+ge.getMessage(),ge);
			} catch (Exception e) {
				System.out.println("Unable to get site '"+siteId+"': "+e.getMessage());
				if (isDebug()) e.printStackTrace();
			}
		}
		
		printSites(sites.toArray(new Site[0]));
	}
	
	public void printSites(Site[] sites) throws CommandException
	{
		if (sites == null) {
			System.out.println("No sites");
			return;
		}
		
		if (longFormat) {
			for (Site site : sites) {
				System.out.printf("id = %s\n", site.getId());
				System.out.printf("name = %s\n",site.getName());
				
				Calendar submitted = (Calendar)site.getSubmitted().clone();
				submitted.setTimeZone(TimeZone.getDefault());
				System.out.printf("submitted = %tc\n", submitted);
				
				Calendar lastUpdate = (Calendar)site.getLastUpdate().clone();
				lastUpdate.setTimeZone(TimeZone.getDefault());
				System.out.printf("lastUpdate = %tc\n", lastUpdate);
				
				System.out.printf("status = %s\n", site.getStatus().toString());
				System.out.printf("statusMessage = %s\n", site.getStatusMessage());
				System.out.printf("installPath = %s\n", site.getInstallPath());
				System.out.printf("localPath = %s\n", site.getLocalPath());
				System.out.printf("condorVersion = %s\n", site.getCondorVersion());
				System.out.printf("condorPackage = %s\n", site.getCondorPackage());
				
				// Environment
				EnvironmentVariable[] env = site.getEnvironment();
				System.out.printf("environment =");
				if (env != null) {
					for (EnvironmentVariable var : env) {
						System.out.printf(" %s=%s", var.getVariable(), var.getValue());
					}
				}
				System.out.printf("\n");
				
				// Staging Service
				ExecutionService stagingService = site.getStagingService();
				if (stagingService == null) {
					System.out.printf("stagingService = \n");
				} else {
					System.out.printf("stagingService = %s %s\n",
							stagingService.getServiceType(),
							stagingService.getServiceContact());
					System.out.printf("  - project = %s\n", stagingService.getProject());
					System.out.printf("  - queue = %s\n", stagingService.getQueue());
				}
				
				// Glidein Service
				ExecutionService glideinService = site.getGlideinService();
				if (glideinService == null) {
					System.out.printf("glideinService = \n");
				} else {
					System.out.printf("glideinService = %s %s\n",
							glideinService.getServiceType(),
							glideinService.getServiceContact());
					System.out.printf("  - project = %s\n", glideinService.getProject());
					System.out.printf("  - queue = %s\n", glideinService.getQueue());
				}
				System.out.printf("\n");
			}
		} else {
			System.out.printf("%-8s","ID");
			System.out.printf("%-20s","NAME");
			System.out.printf("%-15s","SUBMITTED");
			System.out.printf("%-15s","LAST UPDATE");
			System.out.printf("%-10s","STATUS");
			System.out.printf("%s","MESSAGE");
			System.out.printf("\n");
			for (Site site : sites) {
				System.out.printf("%-8d",site.getId());
				System.out.printf("%-20s",site.getName());
				
				Calendar submitted = (Calendar)site.getSubmitted().clone();
				submitted.setTimeZone(TimeZone.getDefault());
				System.out.printf("%1$tm-%1$td %1$TR    ",submitted);
				
				Calendar lastUpdate = (Calendar)site.getLastUpdate().clone();
				lastUpdate.setTimeZone(TimeZone.getDefault());
				System.out.printf("%1$tm-%1$td %1$TR    ",lastUpdate);
				
				System.out.printf("%-10s",site.getStatus().toString());
				System.out.printf("%s",site.getStatusMessage());
				System.out.printf("\n");
			}
		}
	}
	
	public String getName()
	{
		return "list-site";
	}
	
	public String[] getAliases()
	{
		return new String[]{"ls"};
	}
	
	public String getHelp()
	{
		StringBuffer buff = new StringBuffer();
		buff.append("list-site (ls): List available sites\n");
		buff.append("usage: list-site [SITE...]\n");
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
