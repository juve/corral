package edu.usc.glidein.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.globus.axis.util.Util;

import edu.usc.glidein.service.impl.GlideinNames;
import edu.usc.glidein.service.impl.SiteNames;
import edu.usc.glidein.stubs.GlideinFactoryPortType;
import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.SiteFactoryPortType;
import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.service.GlideinFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.service.GlideinServiceAddressingLocator;
import edu.usc.glidein.stubs.service.SiteFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.service.SiteServiceAddressingLocator;
import edu.usc.glidein.util.AddressingUtil;

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
	private SecurityMode security;
	private ProtectionType protection;
	private AuthorizationMode authorization;
	private String dn;
	
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
	
	public void setSecurityMode(SecurityMode security)
	{
		this.security = security;
	}
	
	public SecurityMode getSecurityMode()
	{
		return security;
	}
	
	public void setProtectionType(ProtectionType protection)
	{
		this.protection = protection;
	}
	
	public ProtectionType getProtectionType()
	{
		return protection;
	}
	
	public AuthorizationMode getAuthorizationMode()
	{
		return authorization;
	}
	
	public void setAuthorizationMode(AuthorizationMode authz)
	{
		this.authorization = authz;
	}
	
	public String getDN()
	{
		return dn;
	}

	public void setDN(String dn)
	{
		this.dn = dn;
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
		if (cmdln.hasOption("sec")) {
			String mode = cmdln.getOptionValue("sec");
			try {
				security = SecurityMode.fromString(mode);
				if (debug) {
					System.out.println("Using "+security+" security");
				}
			} catch (IllegalArgumentException ia) {
				throw new CommandException("Invalid security mode: "+mode);
			}
		} else {
			security = SecurityMode.TRANSPORT;
		}
		
		// Protection
		if (cmdln.hasOption("P")) {
			String type = cmdln.getOptionValue("P");
			try {
				protection = ProtectionType.fromString(type);
				if (debug) {
					System.out.println("Using "+protection+" protection");
				}
			} catch (IllegalArgumentException ia) {
				throw new CommandException("Invalid protection type: "+type);
			}
		} else {
			protection = ProtectionType.SIGNATURE;
		}
		
		// Authz
		if (cmdln.hasOption("authz")) {
			String mode = cmdln.getOptionValue("authz");
			try {
				authorization = AuthorizationMode.fromString(mode);
				if (debug) {
					System.out.println("Using "+authorization+" authorization");
				}
			} catch (IllegalArgumentException ia) {
				if (debug) {
					System.out.println("Using DN authorization: "+mode);
				}
				authorization = AuthorizationMode.DN;
				dn = mode;	
			}
		} else {
			authorization = AuthorizationMode.HOST;
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
			if (security == SecurityMode.TRANSPORT) {
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
	
	protected GlideinFactoryPortType getGlideinFactoryPortType()
	throws CommandException
	{
		try {
			EndpointReferenceType epr = 
				AddressingUtil.getGlideinFactoryEPR(
						getURL(GlideinNames.GLIDEIN_FACTORY_SERVICE));
			return getGlideinFactoryPortType(epr);
		} catch (Exception e) {
			throw new CommandException("Unable to get GlideinFactoryPortType: "+
					e.getMessage(),e);
		}
	}
	
	protected GlideinFactoryPortType getGlideinFactoryPortType(EndpointReferenceType epr)
	throws CommandException
	{
		try {
			GlideinFactoryServiceAddressingLocator locator = 
				new GlideinFactoryServiceAddressingLocator();
			GlideinFactoryPortType factory = 
				locator.getGlideinFactoryPortTypePort(epr);
			addSecurityProperties((Stub)factory);
			return factory;
		} catch (ServiceException se) {
			throw new CommandException("Unable to get GlideinFactoryPortType: "+
					se.getMessage(),se);
		}
	}
	
	protected GlideinPortType getGlideinPortType(int id)
	throws CommandException
	{
		try {
			EndpointReferenceType epr = 
				AddressingUtil.getGlideinEPR(
						getURL(GlideinNames.GLIDEIN_SERVICE), id);
			return getGlideinPortType(epr);
		} catch (Exception e) {
			throw new CommandException("Unable to get GlideinPortType: "+
					e.getMessage(),e);
		}
	}
	
	protected GlideinPortType getGlideinPortType(EndpointReferenceType epr)
	throws CommandException
	{
		try {
			GlideinServiceAddressingLocator locator = 
				new GlideinServiceAddressingLocator();
			GlideinPortType instance = 
				locator.getGlideinPortTypePort(epr);
			addSecurityProperties((Stub)instance);
			return instance;
		} catch (ServiceException se) {
			throw new CommandException("Unable to get GlideinPortType: "+
					se.getMessage(),se);
		}
	}
	
	protected SiteFactoryPortType getSiteFactoryPortType()
	throws CommandException
	{
		try {
			EndpointReferenceType epr = 
				AddressingUtil.getSiteFactoryEPR(
						getURL(SiteNames.SITE_FACTORY_SERVICE));
			return getSiteFactoryPortType(epr);
		} catch (Exception e) {
			throw new CommandException("Unable to get SiteFactoryPortType: "+
					e.getMessage(),e);
		}
	}
	
	protected SiteFactoryPortType getSiteFactoryPortType(EndpointReferenceType epr)
	throws CommandException
	{
		try {
			SiteFactoryServiceAddressingLocator locator = 
				new SiteFactoryServiceAddressingLocator();
			SiteFactoryPortType factory = 
				locator.getSiteFactoryPortTypePort(epr);
			
			addSecurityProperties((Stub)factory);
			
			return factory;
		} catch (ServiceException se) {
			throw new CommandException("Unable to get SiteFactoryPortType: "+
					se.getMessage(),se);
		}
	}
	
	protected SitePortType getSitePortType(int id) 
	throws CommandException
	{
		try {
			EndpointReferenceType epr = 
				AddressingUtil.getSiteEPR(
						getURL(SiteNames.SITE_SERVICE),id);
		return getSitePortType(epr);
		} catch (Exception e) {
			throw new CommandException("Unable to get SitePortType: "+
					e.getMessage(),e);
		}
	}
	
	protected SitePortType getSitePortType(EndpointReferenceType epr)
	throws CommandException
	{
		try {
			SiteServiceAddressingLocator locator = 
				new SiteServiceAddressingLocator();
			SitePortType instance = 
				locator.getSitePortTypePort(epr);
			addSecurityProperties((Stub)instance);
			return instance;
		} catch (ServiceException se) {
			throw new CommandException("Unable to get SitePortType: "+
					se.getMessage(),se);
		}
	}
	
	private void addSecurityProperties(Stub stub)
	{
		// TODO: Use org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor
		// See org.globus.delegation.client.Delegate
		//stub._setProperty("clientDescriptor", descriptor);
		
		// Determine protection value
		Integer protectionProp = null;
		switch (protection) {
			case ENCRYPTION:
				protectionProp = org.globus.wsrf.security.Constants.ENCRYPTION;
			break;
			case SIGNATURE:
				protectionProp = org.globus.wsrf.security.Constants.SIGNATURE;
			break;
			default:
				throw new IllegalStateException(
						"Invalid protection type: "+protection);
		}
		
		// Set security
		switch (security) {
			case CONVERSATION:
				stub._setProperty(
						org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
						protectionProp);
			break;
			case MESSAGE:
				stub._setProperty(
						org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
						protectionProp);
			break;
			case TRANSPORT:
				stub._setProperty(
						org.globus.wsrf.security.Constants.GSI_TRANSPORT, 
						protectionProp);
				// I don't know if this is necessary. I got it from
				// org.globus.delegation.client.BaseClient
				Util.registerTransport();
			break;
			case NONE:
				/* Do nothing */
			break;
			default:
				throw new IllegalStateException(
						"Invalid security mode: "+security);
		}
		
		// Set authorization
		if (security == SecurityMode.TRANSPORT || security == SecurityMode.CONVERSATION) {
			
			/* If GSI Secure Transport or GSI Secure Conversation is used, the 
			 * org.globus.axis.gsi.GSIConstants.GSI_AUTHORIZATION property must
			 * be set on the stub. The value of this property must be an 
			 * instance of an object that extends from 
			 * org.globus.gsi.gssapi.auth.GSSAuthorization. All distributed 
			 * authorization schemes have implementation in 
			 * org.globus.gsi.gssapi.auth package.
			 */
			switch (authorization) {
				case SELF:
					stub._setProperty(
						org.globus.axis.gsi.GSIConstants.GSI_AUTHORIZATION,
						org.globus.gsi.gssapi.auth.SelfAuthorization.getInstance()
					);
				break;
				case HOST:
					stub._setProperty(
						org.globus.axis.gsi.GSIConstants.GSI_AUTHORIZATION,
						org.globus.gsi.gssapi.auth.HostAuthorization.getInstance()
					);
				break;
				case NONE:
					stub._setProperty(
						org.globus.axis.gsi.GSIConstants.GSI_AUTHORIZATION,
						org.globus.gsi.gssapi.auth.NoAuthorization.getInstance()
					);
				break;
				case DN:
					stub._setProperty(
						org.globus.axis.gsi.GSIConstants.GSI_AUTHORIZATION,
						new org.globus.gsi.gssapi.auth.IdentityAuthorization(dn)
					);
				break;
				default:
					throw new IllegalStateException(
							"Unhandled authorization mode: "+authorization);
			}
		} else {
			
			/* For all other authentication schemes, the 
			 * org.globus.wsrf.impl.security.Constants.AUTHORIZATION property 
			 * must be set on the stub. The value of this property must be an 
			 * instance of an object that implements the 
			 * org.globus.wsrf.impl.security.authorization.Authorization 
			 * interface.
			 */
			switch (authorization) {
				case SELF:
					stub._setProperty(
						org.globus.wsrf.security.Constants.AUTHORIZATION,
						org.globus.wsrf.impl.security.authorization.SelfAuthorization.getInstance()
					);
				break;
				case HOST:
					stub._setProperty(
						org.globus.wsrf.security.Constants.AUTHORIZATION,
						org.globus.wsrf.impl.security.authorization.HostAuthorization.getInstance()
					);
				break;
				case NONE:
					stub._setProperty(
						org.globus.wsrf.security.Constants.AUTHORIZATION,
						org.globus.wsrf.impl.security.authorization.NoAuthorization.getInstance()
					);
				break;
				case DN:
					stub._setProperty(
						org.globus.wsrf.security.Constants.AUTHORIZATION,
						new org.globus.wsrf.impl.security.authorization.IdentityAuthorization(dn)
					);
				break;
				default:
					throw new IllegalStateException(
							"Unhandled authorization mode: "+authorization);
			}
		}
		
		// No longer using GSI delegation.
		// Use the delegation service instead.
		//stub._setProperty(
		//		org.globus.axis.gsi.GSIConstants.GSI_MODE, 
		//		org.globus.axis.gsi.GSIConstants.GSI_MODE_FULL_DELEG);
	}
	
	private URL getURL(String service) throws CommandException
	{
		// Protocol depends on security setting
		String protocol = null;
		if (security == SecurityMode.TRANSPORT) {
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
