package edu.usc.glidein.client.cli;

import java.net.MalformedURLException;
import java.net.URL;
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

import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.service.GlideinServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.util.AddressingUtil;

public class RemoveGlideinCommand extends Command
{
	private Options options = null;
	private URL glideinURL = null;
	
	@SuppressWarnings("static-access")
	public RemoveGlideinCommand()
	{
		options = new Options();
		options.addOption(
			OptionBuilder.withLongOpt("service")
						 .withDescription("-S [--service] <contact>          : " +
						 		"The service URL (default: "+AddressingUtil.GLIDEIN_SERVICE_URL+")")
						 .hasArg()
						 .create("S")
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
		
		/* Service URL */
		try {
			if (cmdln.hasOption("S")) {
				glideinURL = new URL(cmdln.getOptionValue("service"));
			} else {
				glideinURL = new URL(AddressingUtil.GLIDEIN_SERVICE_URL);
			}
			if (isDebug()) System.out.println("GlideinService: "+glideinURL);
		} catch (MalformedURLException e) {
			throw new CommandException("Invalid glidein service URL: "+e.getMessage(),e);
		}
		
		/* Delete all the sites */
		GlideinServiceAddressingLocator glideinInstanceLocator = 
			new GlideinServiceAddressingLocator();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				try {
					int id = Integer.parseInt(arg);
					EndpointReferenceType glideinEPR = 
						AddressingUtil.getGlideinEPR(glideinURL,id);
					GlideinPortType glidein = 
						glideinInstanceLocator.getGlideinPortTypePort(glideinEPR);
						
					// Use GSI Secure Conversation so that the service can
					// retrieve the user's subject name
					((Stub)glidein)._setProperty(
							org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
							org.globus.wsrf.security.Constants.SIGNATURE);
					
					// Use full delegation so the service can submit the job
					((Stub)glidein)._setProperty(
							org.globus.axis.gsi.GSIConstants.GSI_MODE, 
							org.globus.axis.gsi.GSIConstants.GSI_MODE_FULL_DELEG);
					
					// Use self authorization
					((Stub)glidein)._setProperty(
							org.globus.wsrf.security.Constants.AUTHORIZATION,
							org.globus.wsrf.impl.security.authorization.SelfAuthorization.getInstance());
					
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
