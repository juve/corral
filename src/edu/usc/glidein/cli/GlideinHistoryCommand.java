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

import org.apache.commons.cli.CommandLine;

import edu.usc.glidein.api.GlideinException;
import edu.usc.glidein.api.GlideinFactoryService;
import edu.usc.glidein.service.GlideinNames;
import edu.usc.glidein.stubs.types.GlideinHistory;
import edu.usc.glidein.stubs.types.GlideinHistoryEntry;

// TODO: Allow multiple history records to be returned at the same time

public class GlideinHistoryCommand extends Command
{
	private List<Integer> ids;
	
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
		if (args.length==0) {
			throw new CommandException(getUsage());
		} else {
			for (String arg : args) {
				if (arg.matches("[1-9][0-9]*")) {
					int id = Integer.parseInt(arg);
					ids.add(id);
				} else {
					System.out.println("Invalid glidein id: "+arg);
				}
			}
		}
	}

	public void execute() throws CommandException
	{
		for (int id : ids) {
			try {
				GlideinFactoryService factory = new GlideinFactoryService(
						getServiceURL(GlideinNames.GLIDEIN_FACTORY_SERVICE));
				factory.setDescriptor(getClientSecurityDescriptor());
				printHistory(factory.getHistory(id));
			} catch (GlideinException ge) {
				System.out.println(ge.getMessage());
				if (isDebug()) ge.printStackTrace();
			}
		}
	}
	
	public void printHistory(GlideinHistory history) throws CommandException
	{
		System.out.printf("Glidein %d\n\n",history.getGlideinId());
		System.out.printf("%-10s","STATE");
		System.out.printf("%s","TIME");
		System.out.printf("\n");
		for (GlideinHistoryEntry entry : history.getHistory()) {
			System.out.printf("%-10s",entry.getState().toString());
			Calendar time = (Calendar)entry.getTime().clone();
			time.setTimeZone(TimeZone.getDefault());
			System.out.printf("%1$TF %1$TT",time);
			System.out.printf("\n");
		}
		System.out.println();
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
