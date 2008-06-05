package edu.usc.glidein.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public abstract class Command
{
	public static final String COMMAND_NAME = "glidein";
	
	public static final Class[] SUBCOMMANDS = {
		HelpCommand.class,
		CreateSiteCommand.class,
		RemoveSiteCommand.class,
		ListSiteCommand.class,
		CreateGlideinCommand.class,
		RemoveGlideinCommand.class,
		ListGlideinCommand.class
	};
	
	public static HashMap<String,Class> LOOKUP;
	
	static {
		LOOKUP = new HashMap<String,Class>();
		for (Class clazz : SUBCOMMANDS) {
			Command command = null;
			try {
				command = (Command)clazz.newInstance();
			} catch(Exception e) {
				throw new IllegalStateException("Unable to instantiate command class");
			}
			LOOKUP.put(command.getName(), clazz);
			for (String alias : command.getAliases()) {
				LOOKUP.put(alias,clazz);
			}
		}
	}
	
	private boolean debug = false;
	
	public Command(){}
	
	public boolean isDebug()
	{
		return debug;
	}
	
	public void setDebug(boolean debug)
	{
		this.debug = debug;
	}
	
	public static Command getCommand(String name)
	{
		Class clazz = LOOKUP.get(name);
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
	
	public URL parseURL(String url) throws MalformedURLException
	{
		URL u =  new URL(url);
		
		String protocol = "https";
		if (u.getProtocol() != null)
			protocol = u.getProtocol();
		
		String host = "localhost";
		if (u.getHost() != null)
			host = u.getHost();
		
		int port = 0;
		if (u.getPort() == 0) {
			if ("https".equals(protocol)) {
				port = 8443;
			} else {
				port = 8080;
			}
		} else {
			port = u.getPort();
		}
		
		String path = "/wsrf/services/";
		if (u.getPath() != null) {
			path = u.getPath();
		}
		
		return new URL(protocol+"://"+host+":"+port+"/"+path);
	}
	
	abstract public void invoke(String[] args) throws CommandException;
	abstract public String getHelp();
	abstract public String getName();
	abstract public String[] getAliases();
}
