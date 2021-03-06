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

import java.util.LinkedList;
import java.util.List;

import org.globus.gsi.GlobusCredential;

import edu.usc.corral.api.GlideinService;
import edu.usc.corral.types.RemoveRequest;

public class RemoveGlideinCommand extends Command {
	private boolean force;
	private List<Integer> ids;
	
	public RemoveGlideinCommand() {
		ids = new LinkedList<Integer>();
	}
	
	public void addOptions(List<Option> options) {
		options.add(
			Option.create()
				  .setOption("f")
				  .setLongOption("force")
				  .setUsage("-f | --force")
				  .setDescription("Force the glidein to be deleted")
		);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException {
		// Force
		if (cmdln.hasOption("f")) {
			force = true;
		} else {
			force = false;
		}
		
		// Get IDs
		String[] args = cmdln.getArgs();
		if (args.length == 0) {
			throw new CommandException(getUsage());
		}
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
		/* Delete all the glideins */
		GlideinService svc = new GlideinService(getHost(), getPort());
		for (int id : ids) {
			try {
				if (isDebug()) System.out.print("Removing glidein "+id+"... ");
				svc.remove(new RemoveRequest(id, force, GlobusCredential.getDefaultCredential()));
				if (isDebug()) System.out.println("done.");
			} catch (Exception e) {
				System.out.println(e.getMessage());
				if (isDebug()) e.printStackTrace();
			}
		}
	}
	
	public String getName() {
		return "remove-glidein";
	}
	
	public String[] getAliases() {
		return new String[]{"rg"};
	}
	
	public String getDescription() {
		return "remove-glidein (rg): Remove an existing glidein";
	}
	
	public String getUsage() {
		return "Usage: remove-glidein ID...";
	}
}
