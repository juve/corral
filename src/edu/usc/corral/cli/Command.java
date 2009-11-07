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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

public abstract class Command {
	
	public static final String COMMAND_NAME = "corral";
	
	public static final Class<?>[] SUBCOMMANDS = {
		HelpCommand.class,
		CreateSiteCommand.class,
		RemoveSiteCommand.class,
		ListSiteCommand.class,
		CreateGlideinCommand.class,
		RemoveGlideinCommand.class,
		ListGlideinCommand.class
	};
	
	public static HashMap<String,Class<?>> LOOKUP;
	
	static {
		LOOKUP = new HashMap<String,Class<?>>();
		for (Class<?> clazz : SUBCOMMANDS) {
			Command command = null;
			try {
				command = (Command)clazz.newInstance();
			} catch(Exception e) {
				throw new IllegalStateException("Unable to instantiate command class",e);
			}
			LOOKUP.put(command.getName(), clazz);
			for (String alias : command.getAliases()) {
				LOOKUP.put(alias,clazz);
			}
		}
	}
	
	private List<Option> options;
	private String host;
	private int port;
	private boolean debug;
	private GlobusCredential credential;
	
	public Command() {
		options = new LinkedList<Option>();
		
		// Add common options
		options.add(
			Option.create()
				  .setOption("h")
				  .setLongOption("host")
				  .setUsage("-h | --host <host|ip>")
				  .setDescription("Service host (default: '"+getDefaultHost()+"'. The default can be set by\n" +
				  		          "specifying the CORRAL_HOST environment variable.)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("p")
				  .setLongOption("port")
				  .setUsage("-p | --port <port>")
				  .setDescription("Service port (default: '"+getDefaultPort()+"'. The default can be set by\n" +
								  "specifying the CORRAL_PORT environment variable.)")
				  .hasArgument()	 
		);
		options.add(
			Option.create()
				  .setOption("d")
				  .setLongOption("debug")
				  .setUsage("-d | --debug")
				  .setDescription("Enable verbose debugging messages")
		);
		options.add(
			Option.create()
				  .setOption("af")
				  .setLongOption("argument-file")
				  .setUsage("-af | --argument-file <file>")
				  .setDescription("The name of a file containing command-line arguments. Format is the \n" +
				  				  "same as regular command-line arguments except carriage returns are \n" +
				  				  "allowed without being escaped and lines beginning with # are ignored.")
				  .hasArgument()
		);
		
		
		// Add command-specific options
		addOptions(options);
	}
	
	public boolean isDebug() {
		return debug;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public GlobusCredential getCredential() {
		return credential;
	}
	
	public void setCredential(GlobusCredential credential) {
		this.credential = credential;
	}
	
	public void invoke(String[] args) throws CommandException {
		// Parse command line args
		CommandLine cmdln = CommandLine.parse(options, args);
		
		// Set common arguments
		
		// Debug (NOTE: Debug should be first) 
		if (cmdln.hasOption("d")) {
			debug = true;
		} else {
			debug = false;
		}
		
		// Host
		if (cmdln.hasOption("h")) {
			host = cmdln.getOptionValue("h");
		} else {
			host = getDefaultHost();
		}
		
		// Port
		if (cmdln.hasOption("p")) {
			String portString = cmdln.getOptionValue("p");
			if (portString.matches("[0-9]+")) {
				port = Integer.parseInt(portString);
			} else {
				throw new CommandException("Invalid port: "+portString);
			}
		} else {
			port = getDefaultPort();
		}
		
		// Get proxy credential
		try {
			if (HelpCommand.class != this.getClass())
				credential = GlobusCredential.getDefaultCredential();
		} catch (GlobusCredentialException ce) {
			throw new CommandException("Unable to get globus proxy: "+ce.getMessage(), ce);
		}
		
		// Set command-specific arguments
		setArguments(cmdln);
		
		// Execute command
		execute();
	}
	
	public String getOptionString() {
		// Determine the length of the longest usage
		int max = 0;
		for (Option option : options) {
			String usage = option.getUsage();
			int len = usage.length();
			max = max < len ? len : max;
		}
		
		// Put together a formatted string with 'usage : description'
		StringBuilder buff = new StringBuilder();
		for (Option option : options) {
			buff.append("   ");
			buff.append(option.getUsage());
			int len = option.getUsage().length();
			for (int i=len; i<=max; i++) buff.append(" ");
			buff.append(" : ");
			String desc = option.getDescription();
			String[] ds = desc.split("[\n]");
			for (int i = 0; i<ds.length; i++) {
				if (i>0) {
					buff.append("\n");
					for (int j=1; j<=max+7; j++) buff.append(" ");
				}
				buff.append(ds[i]);
			}
			buff.append("\n");
		}
		
		return buff.toString();
	}
	
	public String getDefaultHost() {
		String ehost = System.getenv("CORRAL_HOST");
		if (ehost == null) return getLocalHost();
		else return ehost;
	}
	
	public String getLocalHost() {
		// Set the default condor host
		try {
			InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostName();
		}  catch (UnknownHostException uhe) {
			throw new RuntimeException("localhost is invalid", uhe);
		}
	}
	
	public int getDefaultPort() {
		String eport = System.getenv("CORRAL_PORT");
		if (eport == null){
			return 8443; 
		} else {
			try {
				return Integer.parseInt(eport);
			} catch (Exception e) {
				throw new RuntimeException("CORRAL_PORT env var is invalid");
			}
		}
	}
	
	abstract public String getUsage();
	abstract public String getDescription();
	abstract public String getName();
	abstract public String[] getAliases();
	abstract protected void addOptions(List<Option> options);
	abstract public void setArguments(CommandLine cmdln) throws CommandException;
	abstract public void execute() throws CommandException;

	public static Command getCommand(String name) {
		Class<?> clazz = LOOKUP.get(name);
		if (clazz == null) {
			return null;
		}
		try {
			Command command = (Command)clazz.newInstance();
			return command;
		} catch(Exception e) {
			return null;
		}
	}
}
