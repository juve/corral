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
package edu.usc.glidein.cli;

import java.util.List;

public class HelpCommand extends Command {
	private String[] commands;
	
	protected void addOptions(List<Option> options) {
		// Remove options not used by this command
		for (int i=0; i<options.size(); ) {
			Option option = options.get(i);
			if ("d".equals(option.getOption())) {
				// Keep debug option
				i++;
			} else {
				// Remove everything else
				options.remove(i);
			}
		}
	}
	
	public void setArguments(CommandLine cmdln) {
		commands = cmdln.getArgs();	
	}
	
	public void execute() throws CommandException {
		if (commands.length==0) {
			StringBuffer buff = new StringBuffer();
			buff.append("Usage: "+COMMAND_NAME+" <subcommand> [options] [args]\n");
			buff.append("Type '"+COMMAND_NAME+" help <subcommand>' for help on a specific subcommand.\n\n");
			buff.append("Available subcommands:\n");
			for (Class<?> clazz : SUBCOMMANDS) {
				Command subcommand = null;
				try {
					subcommand = (Command)clazz.newInstance();
				} catch(Exception e) {
					throw new CommandException("Unable to create command class");
				}
				buff.append("   ");
				buff.append(subcommand.getName());
				String[] aliases = subcommand.getAliases();
				for (int i=0; i<aliases.length; i++) {
					if (i==0) buff.append(" (");
					buff.append(aliases[i]);
					if (i<aliases.length-1) buff.append(", ");
					if (i==aliases.length-1) buff.append(")");
				}
				buff.append("\n");
			}
			System.out.println(buff.toString());
		} else {
			for (String command : commands) {
				Command cmd = getCommand(command);
				if (cmd == null) {
					System.out.println("Unknown command: '"+command+"'");
				} else {
					System.out.println(cmd.getDescription());
					System.out.println(cmd.getUsage());
					System.out.println();
					System.out.println("Valid options:");
					System.out.println(cmd.getOptionString());
				}
			}
		}
	}
	
	public String getName() {
		return "help";
	}
	
	public String[] getAliases() {
		return new String[]{"h"};
	}
	
	public String getDescription() {
		return "help (h): Describe the usage of this program or its subcommands.";
	}
	
	public String getUsage() {
		return "Usage: help [SUBCOMMAND...]";
	}
}
