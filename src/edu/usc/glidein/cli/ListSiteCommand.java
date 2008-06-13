package edu.usc.glidein.cli;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;

import edu.usc.glidein.stubs.SiteFactoryPortType;
import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.Site;

public class ListSiteCommand extends Command
{
	private boolean longFormat = false;
	private List<Integer> ids;
	
	public ListSiteCommand()
	{
		ids = new LinkedList<Integer>();
	}
	
	public void addOptions(List<Option> options)
	{
		options.add(
			Option.create()
				  .setOption("l")
				  .setLongOption("long")
				  .setUsage("-l [--long]")
				  .setDescription("Show detailed information")
		);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException
	{
		/* Long format/short format */
		if (cmdln.hasOption("l")) { 
			longFormat = true;
		}
		
		/* Check for specific arguments */
		String[] args = cmdln.getArgs();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				int id = Integer.parseInt(arg);
				ids.add(id);
			} else {
				System.out.println("Invalid site id: "+arg);
			}
		}
	}
	
	public void execute() throws CommandException
	{
		if (ids.size() > 0) {
			listIndividualSites();
		} else {
			listAllSites();
		}
	}
	
	public void listAllSites() throws CommandException
	{
		if (isDebug()) System.out.printf("Listing sites\n");
		try {
			// Get the sites
			SiteFactoryPortType factory = getSiteFactoryPortType();
			Site[] sites = factory.listSites(longFormat).getSites();
			
			// Print out the site list
			printSites(sites);
		} catch (RemoteException e) {
			throw new CommandException("Unable to list sites: "+
					"Error communicating with service: "+e.getMessage(), e);
		}
		if (isDebug()) System.out.printf("Done listing sites.\n");
	}
	
	public void listIndividualSites() throws CommandException
	{
		LinkedList<Site> sites = new LinkedList<Site>();
		for (int id : ids) {
			try {
				SitePortType instance = getSitePortType(id);
				Site site = instance.getSite(new EmptyObject());
				sites.add(site);
			} catch (RemoteException e) {
				System.out.println("Unable to get site '"+id+"': "+e.getMessage());
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
				
				System.out.printf("state = %s\n", site.getState().toString());
				System.out.printf("shortMessage = %s\n", site.getShortMessage());
				System.out.printf("longMessage = %s\n", site.getLongMessage());
				
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
			System.out.printf("%-10s","STATE");
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
				
				System.out.printf("%-10s",site.getState().toString());
				System.out.printf("%s",site.getShortMessage());
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
	
	public String getDescription()
	{
		return "list-site (ls): List available sites";
	}
	
	public String getUsage()
	{
		return "Usage: list-site [SITE...]";
	}
}
