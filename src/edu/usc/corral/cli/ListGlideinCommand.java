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
import edu.usc.corral.api.GlideinService;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.ListRequest;
import edu.usc.corral.types.ListGlideinsResponse;

public class ListGlideinCommand extends Command {
	private boolean longFormat = false;
	private boolean allUsers = false;
	private String user = null;
	private List<Integer> ids;
	
	public ListGlideinCommand() {
		ids = new LinkedList<Integer>();
	}
	
	public void addOptions(List<Option> options) {
		options.add(
			Option.create()
				  .setOption("l")
				  .setLongOption("long")
				  .setUsage("-l | --long")
				  .setDescription("Show detailed glidein information")
		);
		options.add(
			Option.create()
				  .setOption("a")
				  .setLongOption("all")
				  .setUsage("-a | --all")
				  .setDescription("Show glideins for all users")
		);
		options.add(
			Option.create()
				  .setOption("u")
				  .setLongOption("user")
				  .setUsage("-u | --user <user>")
				  .setDescription("Show glideins for the specified user (default: current user)")
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
		
		/* Remaining arguments */
		String[] args = cmdln.getArgs();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				int id = Integer.parseInt(arg);
				ids.add(id);
			} else {
				System.out.println("Invalid glidein id: "+arg);
			}
		}
	}

	public void execute() throws CommandException {
		/* Check for specific arguments */
		if (ids.size() > 0) {
			listIndividualGlideins();
		} else {
			listAllGlideins();
		}
	}
	
	public void listAllGlideins() throws CommandException {
		if (isDebug()) System.out.printf("Listing glideins\n");
		
		try {
			// Get the sites
			GlideinService svc = new GlideinService(getHost(), getPort());
			ListRequest req = new ListRequest();
			req.setLongFormat(longFormat);
			req.setUser(user);
			req.setAllUsers(allUsers);
			ListGlideinsResponse resp = svc.listGlideins(req);
			
			// Print out the site list
			printGlideins(resp.getGlideins());
		} catch (Exception e) {
			throw new CommandException("Unable to list glideins: "+
					"Error communicating with service: "+e.getMessage(), e);
		}
		
		if (isDebug()) System.out.printf("Done listing glideins.\n");
	}
	
	public void listIndividualGlideins() throws CommandException {
		if (isDebug()) System.out.println("Retrieving glideins");
		GlideinService svc = new GlideinService(getHost(), getPort());
		LinkedList<Glidein> glideins = new LinkedList<Glidein>();
		for (int id : ids) {
			try {
				GetRequest req = new GetRequest();
				req.setId(id);
				Glidein glidein = svc.getGlidein(req);
				glideins.add(glidein);
			} catch (GlideinException ge) {
				System.out.println(ge.getMessage());
				if (isDebug()) ge.printStackTrace();
			}
		}
		if(isDebug()) System.out.println("Done retrieving glideins");
		printGlideins(glideins);
	}
	
	public void printGlideins(List<Glidein> glideins) throws CommandException {
		if (isDebug()) System.out.println("Printing glideins");
		
		if (glideins == null || glideins.size() == 0) {
			if (isDebug()) System.out.println("No glideins");
			return;
		}
		
		if (longFormat) {
			if (isDebug()) System.out.println("Using long format");
			for (Glidein g : glideins) {
				g.print();
				System.out.println();
			}
		} else {
			System.out.printf("%-8s","ID");
			System.out.printf("%-25s","SITE");
			System.out.printf("%-12s","OWNER");
			System.out.printf("%-8s","SLOTS");
			System.out.printf("%-8s","WTIME");
			System.out.printf("%-15s","CREATED");
			System.out.printf("%-15s","LAST UPDATE");
			System.out.printf("%-10s","STATE");
			System.out.printf("%s","MESSAGE");
			System.out.printf("\n");
			for (Glidein g : glideins) {
				System.out.printf("%-8d",g.getId());
				System.out.printf("%-25s",""+g.getSiteName()+" ("+g.getSiteId()+")");
				System.out.printf("%-12s",g.getLocalUsername());
				System.out.printf("%-8d",(g.getCount()*g.getNumCpus()));
				System.out.printf("%-8d",g.getWallTime());
				
				Date created = g.getCreated();
				System.out.printf("%1$tm-%1$td %1$TR    ",created);
				
				Date lastUpdate = g.getLastUpdate();
				System.out.printf("%1$tm-%1$td %1$TR    ",lastUpdate);
				
				System.out.printf("%-10s",g.getState().toString());
				System.out.printf("%s",g.getShortMessage());
				System.out.printf("\n");
			}
			
		}
		if (isDebug()) System.out.println("Done printing glideins.");
	}
	
	public String getName() {
		return "list-glideins";
	}
	
	public String[] getAliases() {
		return new String[]{"lg"};
	}
	
	public String getDescription()  {
		return "list-glideins (lg): Display glideins";
	}
	
	public String getUsage() {
		return "Usage: list-glideins";
	}
}
