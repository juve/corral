package edu.usc.glidein.client.cli;

import java.util.Collection;

import javax.xml.rpc.Stub;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.usc.glidein.GlideinConfiguration;
import edu.usc.glidein.GlideinException;
import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.service.GlideinServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.util.AddressingUtil;

public class RemoveGlideinCommand extends Command
{
	private Options options = null;

	@SuppressWarnings("static-access")
	public RemoveGlideinCommand()
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
		GlideinServiceAddressingLocator glideinInstanceLocator = new GlideinServiceAddressingLocator();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				try {
					int id = Integer.parseInt(arg);
					EndpointReferenceType glideinEPR = AddressingUtil.getGlideinEPR(host,id);
					GlideinPortType glidein = glideinInstanceLocator.getGlideinPortTypePort(glideinEPR);
						
					// Use GSI Secure Conversation so that the service can
					// retrieve the user's subject name
					((Stub)glidein)._setProperty(
							org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
							org.globus.wsrf.security.Constants.SIGNATURE);
					
					if (isDebug()) System.out.print("Removing glidein "+id+"... ");
					glidein.remove(new EmptyObject());
					if (isDebug()) System.out.println("done.");
				} catch (Exception e) {
					System.out.println("Unable to remove glidein '"+arg+"': "+e.getMessage());
					if (isDebug()) e.printStackTrace();
				}
			} else {
				System.out.println("Invalid glidein id: "+arg);
			}
		}
	}
	
	public String getName()
	{
		return "remove-glidein";
	}
	
	public String[] getAliases()
	{
		return new String[]{"rg"};
	}
	
	public String getHelp()
	{
		StringBuffer buff = new StringBuffer();
		buff.append("remove-glidein (rg): Remove an existing glidein\n");
		buff.append("usage: remove-glidein ID...\n");
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
