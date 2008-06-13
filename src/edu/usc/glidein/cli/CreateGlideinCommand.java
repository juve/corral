package edu.usc.glidein.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

import edu.usc.glidein.stubs.GlideinFactoryPortType;
import edu.usc.glidein.stubs.GlideinPortType;
import edu.usc.glidein.stubs.types.EmptyObject;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.util.Base64;
import edu.usc.glidein.util.IOUtil;

public class CreateGlideinCommand extends Command
{
	private Glidein glidein = null;
	private GlobusCredential credential = null;
	
	public void addOptions(List<Option> options)
	{
		options.add(
			Option.create()
				  .setOption("s")
				  .setLongOption("site")
				  .setUsage("-s [--site] <id>")
				  .setDescription("Site to submit glidein to")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("c")
				  .setLongOption("count")
				  .setUsage("-c [--count] <n>")
				  .setDescription("Number of processors (default: 1)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("hc")
				  .setLongOption("host-count")
				  .setUsage("-hc [--host-count] <n>")
				  .setDescription("Number of hosts (default: 1)")
				  .hasArgument()
							 
		);
		options.add(
			Option.create()
				  .setOption("w")
				  .setLongOption("wall-time")
				  .setUsage("-w [--wall-time] <t>")
				  .setDescription("Wall time for job in minutes (default: 60)")		 
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("i")
				  .setLongOption("idle-time")
				  .setUsage("-i [--idle-time] <t>")
				  .setDescription("Glidein max idle time in minutes (default: wallTime)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("n")
				  .setLongOption("num-cpus")
				  .setUsage("-n [--num-cpus] <n>")
				  .setDescription("Number of cpus for condor to report")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("ch")
				  .setLongOption("condor-host")
				  .setUsage("-ch [--condor-host] <name:port>")
				  .setDescription("Condor central manager to report to")
				  .hasArgument()
							 
		);
		options.add(
			Option.create()
				  .setOption("cd")	
				  .setLongOption("condor-debug")
				  .setUsage("-cd [--condor-debug] <ops>")
				  .setDescription("Condor DaemonCore debugging options (csv)")
				  .hasArgument()
				
		);
		options.add(
			Option.create()
				  .setOption("b")
				  .setLongOption("gcb-broker")
				  .setUsage("-b [--gcb-broker] <ip>")
				  .setDescription("GCB Broker IP address")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("cc")
				  .setLongOption("condor-config")
				  .setUsage("-cc [--condor-config] <file>")
				  .setDescription("Condor config file for glidein")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("C")
				  .setLongOption("credential")
				  .setUsage("-C [--credential] <file>")
				  .setDescription("The user's credential as a proxy file. If not specified the Globus default is used.")
				  .hasArgument()
		);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException
	{
		/* Check for extra arguments */
		String[] args = cmdln.getArgs();
		if (args.length > 0) {
			throw new CommandException("Unrecognized argument: "+args[0]);
		}
		
		glidein = new Glidein();
		
		/* Required params ***************************************************/
		//site s
		if (!cmdln.hasOption("s")) {
			throw new CommandException("Missing required argument: site");
		}
		int siteId = Integer.parseInt(cmdln.getOptionValue("site"));
		glidein.setSiteId(siteId);
		
		//condor-host ch
		if (!cmdln.hasOption("ch")) {
			throw new CommandException("Missing required argument: condor-host");
		}
		String condorHost = cmdln.getOptionValue("condor-host");
		glidein.setCondorHost(condorHost);
		
		
		/* Options ***********************************************************/
		//count c
		if (cmdln.hasOption("c")) {
			int count = Integer.parseInt(cmdln.getOptionValue("count"));
			glidein.setCount(count);
		} else {
			glidein.setCount(1);
		}
		
		//host-count hc
		if (cmdln.hasOption("hc")) {
			int hostCount = Integer.parseInt(cmdln.getOptionValue("host-count"));
			glidein.setHostCount(hostCount);
		} else {
			glidein.setHostCount(1);
		}
		
		//num-cpus n
		if (cmdln.hasOption("n")) {
			int numCpus = Integer.parseInt(cmdln.getOptionValue("num-cpus"));
			glidein.setNumCpus(numCpus);
		} else {
			glidein.setNumCpus(1);
		}
		
		//wall-time w
		if (cmdln.hasOption("w")) {
			int wallTime = Integer.parseInt(cmdln.getOptionValue("wall-time"));
			glidein.setWallTime(wallTime);
		} else {
			glidein.setWallTime(60);
		}
		
		//idle-time i
		if (cmdln.hasOption("i")) {
			int idleTime = Integer.parseInt(cmdln.getOptionValue("idle-time"));
			glidein.setIdleTime(idleTime);
		} else {
			glidein.setIdleTime(glidein.getWallTime());
		}
		
		//condor-config cc
		if (cmdln.hasOption("cc")) {
			String fileName = cmdln.getOptionValue("condor-config");
			File file = new File(fileName);
			try {
				String condorConfig = IOUtil.read(file);
				String condorConfigBase64 = Base64.toBase64(condorConfig);
				glidein.setCondorConfigBase64(condorConfigBase64);
			} catch (IOException ioe) {
				throw new CommandException("Unable to read config file: "+fileName,ioe);
			}
		}
		
		//condor-debug cd
		String condorDebug = cmdln.getOptionValue("condor-debug",null);
		glidein.setCondorDebug(condorDebug);
		
		//gcb-broker b
		String gcbBroker = cmdln.getOptionValue("gcb-broker", null);
		glidein.setGcbBroker(gcbBroker);
		
		/* Get proxy credential */
		if (cmdln.hasOption("C")) {
			String proxy = cmdln.getOptionValue("C");
			try {
				credential = new GlobusCredential(proxy);
			} catch (GlobusCredentialException ce) {
				throw new CommandException("Unable to read proxy " +
						"credential: "+proxy+": "+ce.getMessage(),ce);
			}
		} else {
			try {
				credential = GlobusCredential.getDefaultCredential();
			} catch (GlobusCredentialException ce) {
				throw new CommandException("Unable to read default proxy " +
						"credential: "+ce.getMessage(),ce);
			}
		}
	}
	 
	public void execute() throws CommandException
	{	
		if (isDebug()) System.out.println("Creating glidein...");
		
		// TODO: Delegate credential 
		// (see org.globus.delegation.DelegationUtil, org.globus.delegation.client.Delegate)
		
		try {
			GlideinFactoryPortType factory = getGlideinFactoryPortType();	
			EndpointReferenceType epr = factory.createGlidein(glidein);
			GlideinPortType instance = getGlideinPortType(epr);
			instance.submit(new EmptyObject());
		} catch (Exception e) {
			throw new CommandException("Unable to create glidein: "+e.getMessage(),e);
		}
		
		if (isDebug()) System.out.println("Glidein created.");
	}

	public String getName()
	{
		return "create-glidein";
	}
	
	public String[] getAliases()
	{
		return new String[]{"cg"};
	}
	
	public String getDescription()
	{
		return "create-glidein (cg): Add a new glidein";
	}
	
	public String getUsage()
	{
		return "Usage: create-glidein --site <site> --condor-host <host>";
	}
}
