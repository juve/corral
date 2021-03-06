/*
 *  Copyright 2007-2009 University Of Southern California
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
package edu.usc.corral.cli;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.usc.corral.api.GlideinException;
import edu.usc.corral.api.SiteService;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.ListSitesResponse;
import edu.usc.corral.types.Site;

public class ListSiteCommand extends Command {
	private boolean longFormat = false;
	private String user = null;
	private boolean allUsers = false;
	private List<Integer> ids;
	
	public ListSiteCommand() {
		ids = new LinkedList<Integer>();
	}
	
	public void addOptions(List<Option> options) {
		options.add(
			Option.create()
				  .setOption("l")
				  .setLongOption("long")
				  .setUsage("-l | --long")
				  .setDescription("Show detailed site information")
		);
		options.add(
			Option.create()
				  .setOption("a")
				  .setLongOption("all")
				  .setUsage("-a | --all")
				  .setDescription("Show sites for all users")
		);
		options.add(
			Option.create()
				  .setOption("u")
				  .setLongOption("user")
				  .setUsage("-u | --user <user>")
				  .setDescription("Show sites for the specified user (default: current user)")
				  .hasArgument()
		);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException {
		/* Long format/short format */
		if (cmdln.hasOption("l")) { 
			longFormat = true;
		}
		
		/* All users */
		if (cmdln.hasOption("a")) {
			allUsers = true;
		}
		
		/* Specific user */
		if (cmdln.hasOption("u")) {
			user = cmdln.getOptionValue("user");
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
	
	public void execute() throws CommandException {
		if (ids.size() > 0) {
			listIndividualSites();
		} else {
			listAllSites();
		}
	}
	
	public void listAllSites() throws CommandException {
		if (isDebug()) System.out.printf("Listing sites\n");
		try {
			// Get the sites
			SiteService factory = new SiteService(getHost(), getPort());
			ListRequest req = new ListRequest();
			req.setLongFormat(longFormat);
			req.setAllUsers(allUsers);
			req.setUser(user);
			ListSitesResponse resp = factory.listSites(req);
			
			// Print out the site list
			printSites(resp.getSites());
		} catch (GlideinException ge) {
			throw new CommandException(ge.getMessage(), ge);
		}
		if (isDebug()) System.out.printf("Done listing sites.\n");
	}
	
	public void listIndividualSites() throws CommandException {
		LinkedList<Site> sites = new LinkedList<Site>();
		SiteService svc = new SiteService(getHost(), getPort());
		for (int id : ids) {
			try {
				GetRequest req = new GetRequest();
				req.setId(id);
				Site site = svc.getSite(req);
				sites.add(site);
			} catch (GlideinException ge) {
				System.out.println(ge.getMessage());
				if (isDebug()) ge.printStackTrace();
			}
		}
		printSites(sites);
	}
	
	public void printSites(List<Site> sites) throws CommandException {
		if (sites == null || sites.size() == 0) {
			if (isDebug()) System.out.println("No sites");
			return;
		}
		
		if (longFormat) {
			for (Site site : sites) {
				site.print();
				System.out.println();
			}
		} else {
			System.out.printf("%-8s","ID");
			System.out.printf("%-20s","NAME");
			System.out.printf("%-12s","OWNER");
			System.out.printf("%-15s","CREATED");
			System.out.printf("%-15s","LAST UPDATE");
			System.out.printf("%-10s","STATE");
			System.out.printf("%s","MESSAGE");
			System.out.printf("\n");
			for (Site site : sites) {
				System.out.printf("%-8d",site.getId());
				System.out.printf("%-20s",site.getName());
				System.out.printf("%-12s",site.getLocalUsername());
				Date created = site.getCreated();
				System.out.printf("%1$tm-%1$td %1$TR    ",created);
				
				Date lastUpdate = site.getLastUpdate();
				System.out.printf("%1$tm-%1$td %1$TR    ",lastUpdate);
				
				System.out.printf("%-10s",site.getState().toString());
				System.out.printf("%s",site.getShortMessage());
				System.out.printf("\n");
			}
		}
	}
	
	public String getName() {
		return "list-sites";
	}
	
	public String[] getAliases() {
		return new String[]{"ls"};
	}
	
	public String getDescription() {
		return "list-sites (ls): List available sites";
	}
	
	public String getUsage() {
		return "Usage: list-sites [SITE...]";
	}
}
