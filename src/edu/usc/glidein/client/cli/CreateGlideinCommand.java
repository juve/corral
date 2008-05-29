package edu.usc.glidein.client.cli;

import java.io.File;
import java.io.IOException;
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
import edu.usc.glidein.stubs.GlideinFactoryPortType;
import edu.usc.glidein.stubs.service.GlideinFactoryServiceAddressingLocator;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.util.AddressingUtil;
import edu.usc.glidein.util.Base64;
import edu.usc.glidein.util.IOUtil;

public class CreateGlideinCommand extends Command
{
	private Options options = null;
	private EndpointReferenceType glideinFactoryEPR = null;
	
	@SuppressWarnings("static-access")
	public CreateGlideinCommand()
	{
		options = new Options();
		options.addOption(
				OptionBuilder.withLongOpt("site")
							 .hasArg()
							 .withDescription("-s [--site] <id>                : " +
							 		"Site to submit glidein to")
							 .create("s")
		);
		options.addOption(
				OptionBuilder.withLongOpt("count")
							 .hasArg()
							 .withDescription("-c [--count] <n>                : " +
							 		"Number of processors (default: 1)")
							 .create("c")
		);
		options.addOption(
				OptionBuilder.withLongOpt("host-count")
							 .hasArg()
							 .withDescription("-hc [--host-count] <n>           : " +
							 		"Number of hosts (default: 1)")
							 .create("hc")
		);
		options.addOption(
				OptionBuilder.withLongOpt("wall-time")
							 .hasArg()
							 .withDescription("-w [--wall-time] <t>            : " +
							 		"Wall time for job in minutes (default: 60)")
							 .create("w")
		);
		options.addOption(
				OptionBuilder.withLongOpt("idle-time")
							 .hasArg()
							 .withDescription("-i [--idle-time] <t>            : " +
							 		"Glidein max idle time in minutes (default: wallTime)")
							 .create("i")
		);
		options.addOption(
				OptionBuilder.withLongOpt("num-cpus")
							 .hasArg()
							 .withDescription("-n [--num-cpus] <n>             : " +
							 		"Number of cpus for condor to report")
							 .create("n")
		);
		options.addOption(
				OptionBuilder.withLongOpt("condor-host")
							 .hasArg()
							 .withDescription("-ch [--condor-host] <name:port> : " +
							 		"Condor central manager to report to")
							 .create("ch")
		);
		options.addOption(
				OptionBuilder.withLongOpt("condor-debug")
							 .hasArg()
							 .withDescription("-cd [--condor-debug] <ops>      : " +
							 		"Condor DaemonCore debugging options (csv)")
							 .create("cd")
		);
		options.addOption(
				OptionBuilder.withLongOpt("gcb-broker")
							 .hasArg()
							 .withDescription("-b [--gcb-broker] <ip>          : " +
							 		"GCB Broker IP address")
							 .create("b")
		);
		options.addOption(
				OptionBuilder.withLongOpt("condor-config")
							 .hasArg()
							 .withDescription("-cc [--condor-config] <file>    : " +
							 		"Condor config file for glidein")
							 .create("cc")
		);
		options.addOption(
				OptionBuilder.withLongOpt("host")
							 .hasArg()
							 .withDescription("-h [--host] <name:port>         : " +
							 		"The host:port where the service is running (default: localhost:8443)")
							 .create("h")
		);
	}
	
	public void invoke(String[] args) throws CommandException
	{	
		Glidein g = new Glidein();
		
		if (args.length == 0) {
			throw new CommandException(getHelp());
		}
		
		/* Parse args */
		CommandLine cmdln = null;
		try {
			CommandLineParser parser = new PosixParser();
			cmdln = parser.parse(options, args);
		} catch (ParseException pe) {
			throw new CommandException("Invalid argument: "+pe.getMessage());
		}
		
		/* Check for extra arguments */
		args = cmdln.getArgs();
		if (args.length > 0) {
			throw new CommandException("Unrecognized argument: "+args[0]);
		}
		
		
		/* Required params ***************************************************/
		//site s
		if (!cmdln.hasOption("s")) {
			throw new CommandException("Missing required argument: site");
		}
		int siteId = Integer.parseInt(cmdln.getOptionValue("site"));
		g.setSiteId(siteId);
		
		//condor-host ch
		if (!cmdln.hasOption("ch")) {
			throw new CommandException("Missing required argument: condor-host");
		}
		String condorHost = cmdln.getOptionValue("condor-host");
		g.setCondorHost(condorHost);
		
		
		/* Options ***********************************************************/
		//count c
		if (cmdln.hasOption("c")) {
			int count = Integer.parseInt(cmdln.getOptionValue("count"));
			g.setCount(count);
		} else {
			g.setCount(1);
		}
		
		//host-count hc
		if (cmdln.hasOption("hc")) {
			int hostCount = Integer.parseInt(cmdln.getOptionValue("host-count"));
			g.setHostCount(hostCount);
		} else {
			g.setHostCount(1);
		}
		
		//num-cpus n
		if (cmdln.hasOption("n")) {
			int numCpus = Integer.parseInt(cmdln.getOptionValue("num-cpus"));
			g.setNumCpus(numCpus);
		} else {
			g.setNumCpus(1);
		}
		
		//wall-time w
		if (cmdln.hasOption("w")) {
			int wallTime = Integer.parseInt(cmdln.getOptionValue("wall-time"));
			g.setWallTime(wallTime);
		} else {
			g.setWallTime(60);
		}
		
		//idle-time i
		if (cmdln.hasOption("i")) {
			int idleTime = Integer.parseInt(cmdln.getOptionValue("idle-time"));
			g.setIdleTime(idleTime);
		} else {
			g.setIdleTime(g.getWallTime());
		}
		
		//condor-config cc
		if (cmdln.hasOption("cc")) {
			String fileName = cmdln.getOptionValue("condor-config");
			File file = new File(fileName);
			try {
				String condorConfig = IOUtil.read(file);
				String condorConfigBase64 = Base64.toBase64(condorConfig);
				g.setCondorConfigBase64(condorConfigBase64);
			} catch (IOException ioe) {
				throw new CommandException("Unable to read config file: "+fileName,ioe);
			}
		}
		
		//condor-debug cd
		String condorDebug = cmdln.getOptionValue("condor-debug",null);
		g.setCondorDebug(condorDebug);
		
		//gcb-broker b
		String gcbBroker = cmdln.getOptionValue("gcb-broker", null);
		g.setGcbBroker(gcbBroker);
		
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
			
		/* Create EPR */
		try {
			glideinFactoryEPR = AddressingUtil.getGlideinFactoryEPR(host);
		} catch(GlideinException ge) {
			throw new CommandException("Error creating factory EPR: "+ge.getMessage(),ge);
		}
		
		try {
			GlideinFactoryServiceAddressingLocator locator = 
				new GlideinFactoryServiceAddressingLocator();
			GlideinFactoryPortType factory = 
				locator.getGlideinFactoryPortTypePort(glideinFactoryEPR);
		
			((Stub)factory)._setProperty(
					org.globus.wsrf.security.Constants.GSI_SEC_CONV, 
					org.globus.wsrf.security.Constants.SIGNATURE);
			((Stub)factory)._setProperty(
					org.globus.axis.gsi.GSIConstants.GSI_MODE,
					org.globus.axis.gsi.GSIConstants.GSI_MODE_FULL_DELEG);
			
			factory.createGlidein(g);
		} catch (Exception e) {
			throw new CommandException("Unable to create glidein: "+e.getMessage(),e);
		}
	}

	public String getName()
	{
		return "create-glidein";
	}
	
	public String[] getAliases()
	{
		return new String[]{"cg"};
	}
	
	public String getHelp()
	{
		StringBuffer buff = new StringBuffer();
		buff.append("create-glidein (cg): Add a new glidein\n");
		buff.append("usage: create-glidein --site <site> --condor-host <host>\n");
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
