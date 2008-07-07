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

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import edu.usc.glidein.api.GlideinException;
import edu.usc.glidein.api.SiteFactoryService;
import edu.usc.glidein.api.SiteService;
import edu.usc.glidein.service.SiteNames;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.util.SiteUtil;

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
			SiteFactoryService factory = new SiteFactoryService(
					getServiceURL(SiteNames.SITE_FACTORY_SERVICE));
			factory.setDescriptor(getClientSecurityDescriptor());
			Site[] sites = factory.listSites(longFormat);
			
			// Print out the site list
			printSites(sites);
		} catch (GlideinException ge) {
			throw new CommandException(ge.getMessage(), ge);
		}
		if (isDebug()) System.out.printf("Done listing sites.\n");
	}
	
	public void listIndividualSites() throws CommandException
	{
		LinkedList<Site> sites = new LinkedList<Site>();
		for (int id : ids) {
			try {
				SiteService instance = new SiteService(
						getServiceURL(SiteNames.SITE_SERVICE),id);
				instance.setDescriptor(getClientSecurityDescriptor());
				Site site = instance.getSite();
				sites.add(site);
			} catch (GlideinException ge) {
				System.out.println(ge.getMessage());
				if (isDebug()) ge.printStackTrace();
			}
		}
		printSites(sites.toArray(new Site[0]));
	}
	
	public void printSites(Site[] sites) throws CommandException
	{
		if (sites == null) {
			if (isDebug()) System.out.println("No sites");
			return;
		}
		
		if (longFormat) {
			for (Site site : sites) {
				SiteUtil.print(site);
				System.out.println();
			}
		} else {
			System.out.printf("%-8s","ID");
			System.out.printf("%-20s","NAME");
			System.out.printf("%-15s","CREATED");
			System.out.printf("%-15s","LAST UPDATE");
			System.out.printf("%-10s","STATE");
			System.out.printf("%s","MESSAGE");
			System.out.printf("\n");
			for (Site site : sites) {
				System.out.printf("%-8d",site.getId());
				System.out.printf("%-20s",site.getName());
				
				Calendar created = site.getCreated();
				created.setTimeZone(TimeZone.getDefault());
				System.out.printf("%1$tm-%1$td %1$TR    ",created);
				
				Calendar lastUpdate = site.getLastUpdate();
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
