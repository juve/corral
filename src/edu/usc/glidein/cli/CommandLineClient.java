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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class CommandLineClient {
	
	private static void fail() {
		System.out.println("Type '"+Command.COMMAND_NAME+" help' for usage");
		System.exit(1);
	}
	
	public static void main(String[] args) {
		String homeEnv = System.getenv("CORRAL_HOME");
		if (homeEnv == null) {
			System.out.println("Please set CORRAL_HOME");
			System.exit(1);
		}
		
		File homeDir = new File(homeEnv);
		if (!homeDir.isDirectory()) {
			System.out.println("CORRAL_HOME is not a directory: "+homeDir.getPath());
			System.exit(1);
		}
		
		if (System.getProperty("log4j.configuration") == null)
			System.setProperty("log4j.configuration","file://"+homeEnv+"/etc/client-log4j.properties");
		
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
					if (command.isDebug()) {
						e.printStackTrace();
					} else {
						System.out.println(e.getMessage());
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
