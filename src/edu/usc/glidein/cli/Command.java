package edu.usc.glidein.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.globus.axis.util.Util;
import org.globus.wsrf.impl.security.authorization.Authorization;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;
import org.globus.wsrf.impl.security.util.AuthUtil;
import org.globus.wsrf.security.Constants;

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
	private String security;
	private Integer protection;
	private Authorization authorization;
	
	public Command() 
	{
		options = new LinkedList<Option>();
		
		// Add common options
		options.add(
			Option.create()
				  .setOption("h")
				  .setLongOption("host")
				  .setUsage("-h [--host] <host|ip>")
				  .setDescription("Service host (default: localhost)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("p")
				  .setLongOption("port")
				  .setUsage("-p [--port] <port>")
				  .setDescription("Service port (default: 8443 for transport security, 8080 otherwise)")
				  .hasArgument()	 
		);
		options.add(
			Option.create()
				  .setOption("d")
				  .setLongOption("debug")
				  .setUsage("-d [--debug]")
				  .setDescription("Enable verbose debugging messages")
		);
		options.add(
			Option.create()
				  .setOption("sec")
				  .setLongOption("security")
				  .setUsage("-sec [--security] <mode>")
				  .setDescription("Security mode. One of: 'msg', 'conv', 'trans', or 'none'. (default: trans)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("P")
				  .setLongOption("protection")
				  .setUsage("-P [--protection] <type>")
				  .setDescription("Protection type. Either 'sig', or 'enc'. (default: sig)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("authz")
				  .setLongOption("authz")
				  .setUsage("-authz [--authorization] <mode>")
				  .setDescription("Authorization mode. One of: 'host', 'self', 'none', or a DN. (default: host)")
				  .hasArgument()
		);
		
		// Add command-specific options
		addOptions(options);
	}
	
	public boolean isDebug()
	{
		return debug;
	}
	
	public void setDebug(boolean debug)
	{
		this.debug = debug;
	}
	
	public String getHost()
	{
		return host;
	}
	
	public void setHost(String host)
	{
		this.host = host;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	public void setSecurity(String security)
	{
		this.security = security;
	}
	
	public String getSecurity()
	{
		return security;
	}
	
	public void setProtection(Integer protection)
	{
		this.protection = protection;
	}
	
	public Integer getProtection()
	{
		return protection;
	}
	
	public Authorization getAuthorization()
	{
		return authorization;
	}
	
	public void setAuthorization(Authorization authz)
	{
		this.authorization = authz;
	}

	public void invoke(String[] args) throws CommandException
	{
		// Construct options collection
		Options ops = new Options();
		for (Option option :  options) {
			ops.addOption(option.buildOption());
		}
		
		// Parse common arguments
		CommandLine cmdln = null;
		try {
			CommandLineParser parser = new PosixParser();
			cmdln = parser.parse(ops, args);
		} catch (ParseException pe) {
			throw new CommandException("Invalid argument: "+pe.getMessage());
		}
		
		// Set common arguments
		
		// Debug (NOTE: Debug should be first) 
		if (cmdln.hasOption("d")) {
			debug = true;
		} else {
			debug = false;
		}
		
		// Security
		String securityType = null;
		if (cmdln.hasOption("sec")) {
			securityType = cmdln.getOptionValue("sec");
		} else {
			securityType = "transport";
		}
		if (securityType.matches("^(msg)|(message)$")) {
			security = Constants.GSI_SEC_MSG;
		} else if (securityType.matches("^conv(ersation)?$")) {
			security = Constants.GSI_SEC_CONV;
		} else if (securityType.matches("^trans(port)?$")) {
			security = Constants.GSI_TRANSPORT;
			// I don't know if this is necessary. I got it from
			// org.globus.delegation.client.BaseClient
			Util.registerTransport();
		} else if (securityType.matches("^none$")) {
			security = null;
		} else {
			throw new CommandException("Invalid security type: "+securityType);
		}
		if (debug) {
			System.out.println("Using "+securityType+" security");
		}
		
		// Protection
		String protectionType = null;
		if (cmdln.hasOption("P")) {
			protectionType = cmdln.getOptionValue("P");
		} else {
			protectionType = "signature";
		}
		if (protectionType.matches("^sig(nature)?$")) {
			protection = Constants.SIGNATURE;
		} else if (protectionType.matches("^enc(ryption)?$")) {
			protection = Constants.ENCRYPTION;
		} else {
			throw new CommandException("Invalid protection type: "+protectionType);
		}
		if (debug) {
			System.out.println("Using "+protectionType+" protection");
		}
		
		// Authz
		String authz = null;
		if (cmdln.hasOption("authz")) {
			authz = cmdln.getOptionValue("authz");
		} else {
			authz = "host";
		}
		authorization = AuthUtil.getClientAuthorization(authz);
		if (debug) {
			System.out.println("Using "+authz+" authorization");
		}
		
		// Host
		if (cmdln.hasOption("h")) {
			host = cmdln.getOptionValue("h");
		} else {
			host = "localhost";
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
			// Default port depends on authentication mode
			if (Constants.GSI_TRANSPORT.equals(security)) {
				port = 8443;
			} else {
				port = 8080;
			}
		}
		
		// Set command-specific arguments
		setArguments(cmdln);
		
		// Execute command
		execute();
	}
	
	public URL getServiceURL(String service) throws CommandException
	{
		// Protocol depends on security setting
		String protocol = null;
		if (Constants.GSI_TRANSPORT.equals(security)) {
			protocol = "https";
		} else {
			protocol = "http";
		}
		
		// Construct url
		String url = protocol+"://"+host+":"+port+"/wsrf/services/"+service;
		try {
			return new URL(url);
		} catch(MalformedURLException e) {
			throw new CommandException("Invalid URL: "+url+": "+e.getMessage(),e);
		}
	}

	public String getOptionString()
	{
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
			buff.append(option.getDescription());
			buff.append("\n");
		}
		
		return buff.toString();
	}
	
	public ClientSecurityDescriptor getClientSecurityDescriptor()
	{
		// See org.globus.delegation.client.Delegate, 
		// and org.globus.delegation.client.BaseClient
		ClientSecurityDescriptor desc = new ClientSecurityDescriptor();
		
		// Set security
		if (security==null) {
			/* Do nothing for type 'none' */
		} else if (Constants.GSI_SEC_MSG.equals(security)) {
			desc.setGSISecureMsg(protection);
		} else if (Constants.GSI_SEC_CONV.equals(security)) {
			desc.setGSISecureConv(protection);
		} else if (Constants.GSI_TRANSPORT.equals(security)) {
			desc.setGSITransport(protection);
		} else {
			throw new IllegalStateException("Invalid security: "+security);
		}
        
		// Set authorization
        desc.setAuthz(authorization);
        
        return desc;
	}
	
	abstract public String getUsage();
	abstract public String getDescription();
	abstract public String getName();
	abstract public String[] getAliases();
	abstract protected void addOptions(List<Option> options);
	abstract public void setArguments(CommandLine cmdln) throws CommandException;
	abstract public void execute() throws CommandException;

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
}
