package edu.usc.glidein.client.cli;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TimeZone;

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
import edu.usc.glidein.stubs.GlideinFactoryPortType;
import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.service.GlideinFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.service.GlideinServiceAddressingLocator;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.util.AddressingUtil;

public class ListGlideinCommand extends Command
{
	private Options options = null;
	private boolean longFormat = false;
	private String host = null;
	
	@SuppressWarnings("static-access")
	public ListGlideinCommand()
	{
		options = new Options();
		options.addOption(
				OptionBuilder.withLongOpt("long")
							 .withDescription("-l [--long]              : " +
							 		"Show detailed information")
							 .create("l")
		);
		options.addOption(
				OptionBuilder.withLongOpt("host")
							 .withDescription("-h [--host] <name:port>  : " +
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
			throw new CommandException("Invalid argument: "+pe.getMessage());
		}
			
		/* Long format/short format */
		if (cmdln.hasOption("l")) { 
			longFormat = true;
		}
		
		/* Host name / port */
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
		
		/* Check for specific arguments */
		args = cmdln.getArgs();
		if (args.length > 0) {
			listIndividualGlideins(args);
		} else {
			listAllGlideins();
		}
	}
	
	public void listAllGlideins() throws CommandException
	{
		if (isDebug()) System.out.printf("Listing glideins\n");
		Glidein[] glideins;
		try {
			EndpointReferenceType glideinFactoryEPR = 
				AddressingUtil.getGlideinFactoryEPR(host);
			GlideinFactoryServiceAddressingLocator locator = 
				new GlideinFactoryServiceAddressingLocator();
			GlideinFactoryPortType factory = 
				locator.getGlideinFactoryPortTypePort(glideinFactoryEPR);
			
			// Use GSI Secure Conversation so that the service can
			// retrieve the user's subject name
			((Stub)factory)._setProperty(
					org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
					org.globus.wsrf.security.Constants.SIGNATURE);
			
			// Get the sites
			glideins = factory.listGlideins(longFormat).getGlideins();
		} catch (Exception e) {
			throw new CommandException("Unable to list glideins: "+
					"Error communicating with service: "+e.getMessage(), e);
		}
		
		// Print out the site list
		printGlideins(glideins);
		
		if (isDebug()) System.out.printf("Done listing glideins.\n");
	}
	
	public void listIndividualGlideins(String[] glideinIds) throws CommandException
	{
		if (isDebug()) System.out.println("Retrieving glideins");
		LinkedList<Glidein> glideins = new LinkedList<Glidein>();
		GlideinServiceAddressingLocator glideinInstanceLocator = 
			new GlideinServiceAddressingLocator();
		for (String glideinId : glideinIds) {
			try {
				int id = Integer.parseInt(glideinId);
				EndpointReferenceType glideinEPR = 
					AddressingUtil.getGlideinEPR(host,id);
				GlideinPortType instance = 
					glideinInstanceLocator.getGlideinPortTypePort(glideinEPR);
					
				// Use GSI Secure Conversation so that the service can
				// retrieve the user's subject name
				((Stub)instance)._setProperty(
						org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
						org.globus.wsrf.security.Constants.SIGNATURE);
				
				Glidein glidein = instance.getGlidein(new EmptyObject());
				glideins.add(glidein);
			} catch (NumberFormatException nfe) {
				System.out.println("Invalid glidein id: "+glideinId);
			} catch (GlideinException ge) {
				throw new CommandException("Unable to get EPR: "+ge.getMessage(),ge);
			} catch (Exception e) {
				System.out.println("Unable to get glidein '"+glideinId+"': "+e.getMessage());
				if (isDebug()) e.printStackTrace();
			}
		}
		if(isDebug()) System.out.println("Done retrieving glideins");
		printGlideins(glideins.toArray(new Glidein[0]));
	}
	
	public void printGlideins(Glidein[] glideins) throws CommandException
	{
		if (isDebug()) System.out.println("Printing glideins");
		
		if (glideins == null || glideins.length == 0) {
			System.out.println("No glideins.");
			return;
		}
		
		if (longFormat) {
			if (isDebug()) System.out.println("Using long format");
			for (Glidein g : glideins) {
				System.out.printf("id = %d\n",g.getId());
				System.out.printf("siteName = %s\n", g.getSiteName());
				System.out.printf("siteId = %d\n", g.getSiteId());
				System.out.printf("condorHost = %s\n",g.getCondorHost());
				System.out.printf("count = %d\n", g.getCount());
				System.out.printf("hostCount = %d\n", g.getHostCount());
				System.out.printf("numCpus = %d\n", g.getNumCpus());
				System.out.printf("wallTime = %d\n", g.getWallTime());
				System.out.printf("idleTime = %d\n", g.getIdleTime());
				
				Calendar submitted = (Calendar)g.getSubmitted().clone();
				submitted.setTimeZone(TimeZone.getDefault());
				System.out.printf("submitted = %tc\n",submitted);
				
				Calendar lastUpdate = (Calendar)g.getLastUpdate().clone();
				lastUpdate.setTimeZone(TimeZone.getDefault());
				System.out.printf("lastUpdate = %tc\n",lastUpdate);
				
				System.out.printf("status = %s\n",g.getStatus().toString());
				System.out.printf("statusMessage = %s\n",g.getStatusMessage());
				
				System.out.printf("condorDebug = %s\n", g.getCondorDebug());
				System.out.printf("gcbBroker = %s\n", g.getGcbBroker());
				
				System.out.printf("\n");
			}
		} else {
			System.out.printf("%-8s", "ID");
			System.out.printf("%-20s", "SITE");
			System.out.printf("%-20s", "CONDOR HOST");
			System.out.printf("%-8s", "COUNT");
			System.out.printf("%-8s", "HOSTS");
			System.out.printf("%-8s", "TIME");
			System.out.printf("%-15s", "SUBMITTED");
			System.out.printf("%-15s", "LAST UPDATE");
			System.out.printf("%-10s", "STATUS");
			System.out.printf("%s", "MESSAGE");
			System.out.printf("\n");
			for (Glidein g : glideins) {
				System.out.printf("%-8d",g.getId());
				System.out.printf("%-20s", ""+g.getSiteName()+" ("+g.getSiteId()+")");
				System.out.printf("%-20s",g.getCondorHost());
				System.out.printf("%-8d", g.getCount());
				System.out.printf("%-8d", g.getHostCount());
				System.out.printf("%-8d", g.getWallTime());
				
				Calendar submitted = (Calendar)g.getSubmitted().clone();
				submitted.setTimeZone(TimeZone.getDefault());
				System.out.printf("%1$tm-%1$td %1$TR    ",submitted);
				
				Calendar lastUpdate = (Calendar)g.getLastUpdate().clone();
				lastUpdate.setTimeZone(TimeZone.getDefault());
				System.out.printf("%1$tm-%1$td %1$TR    ",lastUpdate);
				
				System.out.printf("%-10s",g.getStatus().toString());
				System.out.printf("%s",g.getStatusMessage());
				System.out.printf("\n");
			}
		}
		if (isDebug()) System.out.println("Done printing glideins.");
	}
	
	public String getName()
	{
		return "list-glidein";
	}
	
	public String[] getAliases()
	{
		return new String[]{"lg"};
	}
	
	public String getHelp() 
	{
		StringBuffer buff = new StringBuffer();
		buff.append("list-glidein (lg): Display glideins\n");
		buff.append("usage: list-glidein\n");
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
