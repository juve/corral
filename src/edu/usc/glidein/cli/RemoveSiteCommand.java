package edu.usc.glidein.cli;

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
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;

import edu.usc.glidein.stubs.SitePortType;
import edu.usc.glidein.stubs.service.SiteServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.util.AddressingUtil;

public class RemoveSiteCommand extends Command
{	
	private Options options = null;
	private URL siteURL = null;
	
	@SuppressWarnings("static-access")
	public RemoveSiteCommand()
	{
		options = new Options();
		options.addOption(
			OptionBuilder.withLongOpt("service")
						 .withDescription("-S [--service] <url>          : " +
						 		"The service URL (default: "+AddressingUtil.SITE_SERVICE_URL+")")
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
				siteURL = new URL(cmdln.getOptionValue("service"));
			} else {
				siteURL = new URL(AddressingUtil.SITE_SERVICE_URL);
			}
			if (isDebug()) System.out.println("SiteService: "+siteURL);
		} catch (MalformedURLException e) {
			throw new CommandException("Invalid site service URL: "+e.getMessage(),e);
		}
		/* Delete all the sites */
		SiteServiceAddressingLocator siteInstanceLocator = new SiteServiceAddressingLocator();
		for (String arg : args) {
			if (arg.matches("[1-9][0-9]*")) {
				try {
					int id = Integer.parseInt(arg);
					EndpointReferenceType siteEPR = 
						AddressingUtil.getSiteEPR(siteURL,id);
					SitePortType site = 
						siteInstanceLocator.getSitePortTypePort(siteEPR);
						
					// Use GSI Secure Conversation so that the service can
					// retrieve the user's subject name
					((Stub)site)._setProperty(
							org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
							org.globus.wsrf.security.Constants.SIGNATURE);
					
					// Use full delegation so the service can submit the job
					((Stub)site)._setProperty(
							org.globus.axis.gsi.GSIConstants.GSI_MODE, 
							org.globus.axis.gsi.GSIConstants.GSI_MODE_FULL_DELEG);
					
					// Use self authorization
					((Stub)site)._setProperty(
							org.globus.wsrf.security.Constants.AUTHORIZATION,
							org.globus.wsrf.impl.security.authorization.SelfAuthorization.getInstance());
					
					if (isDebug()) System.out.print("Removing site "+id+"... ");
					site.remove(new EmptyObject());
					if (isDebug()) System.out.println("done.");
				} catch (Exception e) {
					System.out.println("Unable to remove site '"+arg+"': "+e.getMessage());
					if (isDebug()) e.printStackTrace();
				}
			} else {
				System.out.println("Invalid site id: "+arg);
			}
		}
	}
	
	public String getName()
	{
		return "remove-site";
	}
	
	public String[] getAliases()
	{
		return new String[]{"rs"};
	}
	
	public String getHelp()
	{
		StringBuffer buff = new StringBuffer();
		buff.append("remove-site (rs): Remove an existing site\n");
		buff.append("usage: remove-site ID...\n");
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
