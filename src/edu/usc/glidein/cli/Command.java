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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.axis.util.Util;
import org.globus.delegation.DelegationUtil;
import org.globus.gsi.GlobusCredential;
import org.globus.wsrf.impl.security.authorization.Authorization;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;
import org.globus.wsrf.impl.security.util.AuthUtil;
import org.globus.wsrf.security.Constants;
import org.globus.wsrf.utils.AddressingUtils;

public abstract class Command
{
	static { Util.registerTransport(); }
	
	public static final String COMMAND_NAME = "corral";
	
	public static final Class[] SUBCOMMANDS = {
		HelpCommand.class,
		CreateSiteCommand.class,
		RemoveSiteCommand.class,
		ListSiteCommand.class,
		CreateGlideinCommand.class,
		RemoveGlideinCommand.class,
		ListGlideinCommand.class,
		SiteHistoryCommand.class,
		GlideinHistoryCommand.class
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
	private boolean anonymous;
	
	public Command() 
	{
		options = new LinkedList<Option>();
		
		// Add common options
		options.add(
			Option.create()
				  .setOption("h")
				  .setLongOption("host")
				  .setUsage("-h [--host] <host|ip>")
				  .setDescription("Service host (default: '"+getDefaultHost()+"'. The default can be set by\n" +
				  		          "specifying the GLIDEIN_HOST environment variable.)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("p")
				  .setLongOption("port")
				  .setUsage("-p [--port] <port>")
				  .setDescription("Service port (default: "+(getDefaultPort()==null
						  		 ?"'8443' for transport security, '8080' otherwise.\n"+
						  		  "The default can be set by specifying the GLIDEIN_PORT environment variable.)"
								 :"'"+getDefaultPort()+"'. The default can be set by\n" +
								  "specifying the GLIDEIN_PORT environment variable.)"
								  ))
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
				  .setDescription("Security mode. One of: 'msg', 'conv', 'trans', or 'none'. \n" +
				  				  "(default: trans)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("P")
				  .setLongOption("protection")
				  .setUsage("-P [--protection] <type>")
				  .setDescription("Protection type. Either 'sig', or 'enc'. (default: 'sig')")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("authz")
				  .setLongOption("authorization")
				  .setUsage("-authz [--authorization] <mode>")
				  .setDescription("Authorization mode. One of: 'host', 'self', 'none', or a DN. \n" +
				  				  "(default: 'host')")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("anon")
				  .setLongOption("anonymous")
				  .setUsage("-anon [--anonymous]")
				  .setDescription("Enable anonymous authentication")
		);
		options.add(
			Option.create()
				  .setOption("af")
				  .setLongOption("argument-file")
				  .setUsage("-af [--argument-file] <file>")
				  .setDescription("The name of a file containing command-line arguments. Format is the \n" +
				  				  "same as regular command-line arguments except carriage returns are \n" +
				  				  "allowed without being escaped and lines beginning with # are ignored.")
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
		// Parse command line args
		CommandLine cmdln = CommandLine.parse(options, args);
		
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
		
		// Authentication
		if (cmdln.hasOption("anon")) {
			anonymous = true;
		} else {
			anonymous = false;
		}
		
		// Host
		if (cmdln.hasOption("h")) {
			host = cmdln.getOptionValue("h");
		} else {
			host = getDefaultHost();
		}
		
		// Port
		if (cmdln.hasOption("p") || getDefaultPort()!=null) {
			String portString = cmdln.getOptionValue("p",getDefaultPort());
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
        
		// Set anonymous authentication
		if (anonymous) {
			desc.setAnonymous();
		}
		
		// Set authorization
        desc.setAuthz(authorization);
        
        return desc;
	}
	
	public EndpointReferenceType delegateCredential(GlobusCredential credential)
	throws CommandException
	{
		// (see org.globus.delegation.DelegationUtil, org.globus.delegation.client.Delegate)
		ClientSecurityDescriptor desc = getClientSecurityDescriptor();
		boolean fullDelegation = true;
		URL delegationServiceUrl = getServiceURL("DelegationFactoryService");
		
		try {
			EndpointReferenceType delegEpr =
	            AddressingUtils.createEndpointReference(delegationServiceUrl.toString(), null);
			
			X509Certificate[] certsToDelegateOn =
	            DelegationUtil.getCertificateChainRP(delegEpr,desc);
	        
			EndpointReferenceType credentialEPR = 
				DelegationUtil.delegate(delegationServiceUrl.toString(), 
	        		credential, certsToDelegateOn[0], fullDelegation, desc);
	        
			return credentialEPR;
		} catch (Exception e) {
			throw new CommandException("Unable to delegate credential: "+e.getMessage(), e);
		}
	}
	
	public String getDefaultHost()
	{
		// If $GLIDEIN_HOST is specified, use that. Otherwise
		// use the local host name.
		String host = System.getenv("GLIDEIN_HOST");
		if (host == null) return getLocalHost();
		else return host;
	}
	
	public String getLocalHost()
	{
		// Set the default condor host
		try {
			InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostName();
		}  catch (UnknownHostException uhe) {
			return null;
		}
	}
	
	public String getDefaultPort()
	{
		return System.getenv("GLIDEIN_PORT");
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
