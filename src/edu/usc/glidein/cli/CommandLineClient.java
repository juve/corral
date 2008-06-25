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

import java.util.LinkedList;
import java.util.List;

public class CommandLineClient 
{
	private static void fail()
	{
		System.out.println("Type '"+Command.COMMAND_NAME+" help' for usage");
		System.exit(1);
	}
	
	public static void main(String[] args)
	{
		if (args.length==0) {
			fail();
		} else {
			// Handle arguments
			List<String> arguments = new LinkedList<String>();
			for (String arg : args) arguments.add(arg);
			String name = arguments.remove(0);
			String[] ops = arguments.toArray(new String[0]); 
			
			// Invoke appropriate command
			Command command = Command.getCommand(name);
			if (command == null) {
				System.out.println("Unknown command: '"+name+"'");
				fail();
			} else {
				try {
					command.invoke(ops);
				} catch (CommandException e) {
					if (e.getMessage() != null) {
						System.out.println(e.getMessage());
					}
					if (command.isDebug() && e.getCause() != null) {
						e.getCause().printStackTrace();
					}
					System.exit(1);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}
}
