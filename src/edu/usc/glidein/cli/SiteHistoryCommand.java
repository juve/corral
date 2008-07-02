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
import edu.usc.glidein.api.SiteFactoryService;
import edu.usc.glidein.service.SiteNames;
import edu.usc.glidein.stubs.types.SiteHistoryEntry;

public class SiteHistoryCommand extends Command
{
	private List<Integer> ids;
	
	public SiteHistoryCommand()
	{
		ids = new LinkedList<Integer>();
	}
	
	public void addOptions(List<Option> options)
	{
		/* This command has no options */
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException
	{
		/* Check for specific arguments */
		String[] args = cmdln.getArgs();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				int id = Integer.parseInt(arg);
				ids.add(id);
			} else {
				throw new CommandException("Invalid site id: "+arg);
			}
		}
	}
	
	public void execute() throws CommandException
	{
		
		try {
			int[] _ids = new int[ids.size()];
			int i = 0;
			for (int id : ids) _ids[i++] = id;
				
			SiteFactoryService factory = new SiteFactoryService(
					getServiceURL(SiteNames.SITE_FACTORY_SERVICE));
			factory.setDescriptor(getClientSecurityDescriptor());
			printHistory(factory.getHistory(_ids));
		} catch (GlideinException ge) {
			System.out.println(ge.getMessage());
			if (isDebug()) ge.printStackTrace();
		}
	}
	
	public void printHistory(SiteHistoryEntry[] history) throws CommandException
	{
		if (history == null) {
			if (isDebug()) System.out.println("No history");
			return;
		}
		
		System.out.printf("%-8s","ID");
		System.out.printf("%-10s","STATE");
		System.out.printf("%s","TIME");
		System.out.printf("\n");
		for (SiteHistoryEntry entry : history) {
			System.out.printf("%-8s", entry.getSiteId());
			System.out.printf("%-10s",entry.getState().toString());
			Calendar time = (Calendar)entry.getTime().clone();
			time.setTimeZone(TimeZone.getDefault());
			System.out.printf("%1$TF %1$TT",time);
			System.out.printf("\n");
		}
	}
	
	public String getName()
	{
		return "site-history";
	}
	
	public String[] getAliases()
	{
		return new String[]{"sh"};
	}
	
	public String getDescription()
	{
		return "site-history (sh): Display site history";
	}
	
	public String getUsage()
	{
		return "Usage: site-history SITE...";
	}
}
