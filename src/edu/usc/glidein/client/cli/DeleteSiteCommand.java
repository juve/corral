package edu.usc.glidein.client.cli;

import java.util.Collection;

import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;

import edu.usc.glidein.GlideinConfiguration;
import edu.usc.glidein.GlideinException;
import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.service.SiteServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.util.AddressingUtil;

public class DeleteSiteCommand extends Command
{	
	private Options options = null;
	
	@SuppressWarnings("static-access")
	public DeleteSiteCommand()
	{
		options = new Options();
		options.addOption(
			OptionBuilder.withLongOpt("host")
						 .withDescription("-h [--host] <name:port>          : " +
						 		"The host:port where the service is running (default: localhost:8443)")
						 .hasArg()
						 .create("h")
		);
	}
	
	public void invoke(String[] args) throws CommandException
	{
		/* Parse args */
		CommandLine cmdln = null;
		try {
			CommandLineParser parser = new PosixParser();
			cmdln = parser.parse(options, args);
		} catch (ParseException pe) {
			throw new CommandException("Invalid argument: "+pe.getMessage(),pe);
		}
		
		/* Check args */
		args = cmdln.getArgs();
		if (args.length == 0) {
			throw new CommandException(getHelp());
		}
		
		/* Host */
		String host = null;
		if (cmdln.hasOption("h")) {
			/* User-provided */
			host = cmdln.getOptionValue("host");
		} else {
			/* From config file */
			try {
				GlideinConfiguration config = GlideinConfiguration.getInstance();
				host = config.getProperty("glidein.host", "localhost:8443");
			} catch (GlideinException ge) {
				throw new CommandException("Error reading config file: "+ge.getMessage(), ge);
			}
		}
		if (isDebug()) System.out.println("Host: "+host);
			
		/* Delete all the sites */
		SiteServiceAddressingLocator siteInstanceLocator = new SiteServiceAddressingLocator();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				try {
					int id = Integer.parseInt(arg);
					EndpointReferenceType siteEPR = AddressingUtil.getSiteEPR(host,id);
					SitePortType site = siteInstanceLocator.getSitePortTypePort(siteEPR);
						
					// Use GSI Secure Conversation so that the service can
					// retrieve the user's subject name
					((Stub)site)._setProperty(
							org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
							org.globus.wsrf.security.Constants.SIGNATURE);
					
					if (isDebug()) System.out.print("Deleting site "+id+"... ");
					site.delete(new EmptyObject());
					if (isDebug()) System.out.println("done.");
				} catch (Exception e) {
					System.out.println("Unable to delete site '"+arg+"': "+e.getMessage());
					if (isDebug()) e.printStackTrace();
				}
			} else {
				System.out.println("Invalid site id: "+arg);
			}
		}
	}
	
	public String getName()
	{
		return "delete-site";
	}
	
	public String[] getAliases()
	{
		return new String[]{"ds"};
	}
	
	public String getHelp()
	{
		StringBuffer buff = new StringBuffer();
		buff.append("delete-site (ds): Delete an existing site\n");
		buff.append("usage: delete-site ID...\n");
		buff.append("\n");
		buff.append("Valid options:\n");
		@SuppressWarnings("unchecked")
		Collection<Option> collection = options.getOptions();
		Option[] ops = collection.toArray(new Option[0]);
		for (Option op : ops) {
			buff.append("   ");
			buff.append(op.getDescription());
			buff.append("\n");
		}
		return buff.toString();
	}
}
