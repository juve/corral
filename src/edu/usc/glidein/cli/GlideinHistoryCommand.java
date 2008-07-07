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
import edu.usc.glidein.api.GlideinFactoryService;
import edu.usc.glidein.service.GlideinNames;
import edu.usc.glidein.stubs.types.GlideinHistoryEntry;

public class GlideinHistoryCommand extends Command
{
	private LinkedList<Integer> ids;
	
	public GlideinHistoryCommand()
	{
		ids = new LinkedList<Integer>();
	}
	
	public void addOptions(List<Option> options)
	{
		/* This command has no options */
	}

	public void setArguments(CommandLine cmdln) throws CommandException
	{		
		/* Remaining arguments */
		String[] args = cmdln.getArgs();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				int id = Integer.parseInt(arg);
				ids.add(id);
			} else {
				throw new CommandException("Invalid glidein id: "+arg);
			}
		}
	}

	public void execute() throws CommandException
	{
		try {
			int[] _ids = new int[ids.size()];
			int i = 0;
			for (int id : ids) _ids[i++] = id;
			
			GlideinFactoryService factory = new GlideinFactoryService(
					getServiceURL(GlideinNames.GLIDEIN_FACTORY_SERVICE));
			factory.setDescriptor(getClientSecurityDescriptor());
			printHistory(factory.getHistory(_ids));
		} catch (GlideinException ge) {
			System.out.println(ge.getMessage());
			if (isDebug()) ge.printStackTrace();
		}
	}
	
	public void printHistory(GlideinHistoryEntry[] history) throws CommandException
	{
		if (history == null) {
			if (isDebug()) System.out.println("No history");
			return;
		}
		
		System.out.printf("%-8s", "ID");
		System.out.printf("%-10s","STATE");
		System.out.printf("%s","TIME");
		System.out.printf("\n");
		for (GlideinHistoryEntry entry : history) {
			System.out.printf("%-8s", entry.getGlideinId());
			System.out.printf("%-10s",entry.getState().toString());
			Calendar time = (Calendar)entry.getTime().clone();
			time.setTimeZone(TimeZone.getDefault());
			System.out.printf("%1$TF %1$TT",time);
			System.out.printf("\n");
		}
	}
	
	public String getName()
	{
		return "glidein-history";
	}
	
	public String[] getAliases()
	{
		return new String[]{"gh"};
	}
	
	public String getDescription() 
	{
		return "glidein-history (gh): Display glidein history";
	}
	
	public String getUsage()
	{
		return "Usage: glidein-history GLIDEIN...";
	}
}
