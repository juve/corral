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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.commons.cli.CommandLine;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;

import edu.usc.glidein.api.GlideinFactoryService;
import edu.usc.glidein.api.GlideinService;
import edu.usc.glidein.service.GlideinNames;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.util.Base64;
import edu.usc.glidein.util.GlideinUtil;
import edu.usc.glidein.util.IOUtil;

public class CreateGlideinCommand extends Command
{
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private Glidein glidein = null;
	private GlobusCredential credential = null;
	private boolean verbose;
	
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
				  .setDescription("Wall time for job in minutes (default: 60, min: 2, max: site-specific)")		 
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
		options.add(
			Option.create()
				  .setOption("r")
				  .setLongOption("resubmit")
				  .setUsage("-r [--resubmit] [number|date]")
				  .setDescription("Resubmit the glidein when it expires. The glidein will be resubmitted\n" +
				  		"indefinitely until the user removes it. The optional argument allows the user\n" +
				  		"to specify either the maximum number of times to resubmit the glidein, or a date,\n" +
				  		"in 'YYYY-MM-DD HH24:MM:SS' format, when to stop resubmitting the glidein. Dates must\n" +
				  		"be quoted on the command-line and are assumed to be relative to the client's time zone.\n" +
				  		"By default the service will keep resubmitting the glidein until the user's certificate\n" +
				  		"expires, or the glidein fails.")
				  .hasOptionalArgument()
		);
		options.add(
			Option.create()
				  .setOption("v")
				  .setLongOption("verbose")
				  .setUsage("-v [--verbose]")
				  .setDescription("Show details about the new glidein")
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
			if (wallTime<2) {
				throw new CommandException("Wall time must be >= 2 minutes");
			}
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
				byte[] condorConfigBytes = Base64.toBase64(condorConfig);
				glidein.setCondorConfig(condorConfigBytes);
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
		
		/* Resubmit the glidein when it expires */
		if (cmdln.hasOption("r")) {
			glidein.setResubmit(true);
			String value = cmdln.getOptionValue("r");
			long timeLeft = credential.getTimeLeft() * 1000;
			long timeRequired = glidein.getWallTime() * 60 * 1000;
			
			if (value != null) {
				if (value.matches("[0-9]+")) {
					int resubmits = Integer.parseInt(value);
					if (resubmits <= 0 || resubmits > 128) {
						throw new CommandException(
								"Resubmits must be between 0 and 128");
					}
					timeRequired = resubmits * glidein.getWallTime() * 60 * 1000;
					glidein.setResubmits(resubmits);
				} else {
					Calendar until = Calendar.getInstance();
					Calendar now = Calendar.getInstance();
					try {
						SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
						until.setTime(parser.parse(value));
					} catch (ParseException pe) {
						throw new CommandException(
								"Invalid resubmit option: "+value);
					}
					if (until.before(now)) {
						throw new CommandException(
								"Resubmit date should be in the future");
					}
					timeRequired = until.getTimeInMillis() - now.getTimeInMillis();
					glidein.setUntil(until);
				}
			}
			if (isDebug()) {
				System.out.println("Time Left: "+timeLeft+" ms");
				System.out.println("Time Required: "+timeRequired+" ms");
			}
			if (timeLeft < timeRequired) {
				throw new CommandException(
						"Not enough time left on credential for " +
						"specified run time (including resubmits)");
			}
		} else {
			glidein.setResubmit(false);
		}
		
		/* Verbose */
		if (cmdln.hasOption("v")) {
			verbose = true;
		} else {
			verbose = false;
		}
	}
	
	public void execute() throws CommandException
	{	
		if (isDebug()) System.out.println("Creating glidein...");
		
		// Delegate credential
		EndpointReferenceType credentialEPR = delegateCredential(credential);
		
		try {
			// Create glidein
			ClientSecurityDescriptor desc = getClientSecurityDescriptor();
			GlideinFactoryService factory = new GlideinFactoryService(
					getServiceURL(GlideinNames.GLIDEIN_FACTORY_SERVICE));
			factory.setDescriptor(desc);
			EndpointReferenceType epr = factory.createGlidein(glidein);
			
			// Get instance
			GlideinService instance = new GlideinService(epr);
			instance.setDescriptor(desc);
			
			// If verbose, print details
			if (verbose) {
				glidein = instance.getGlidein();
				GlideinUtil.print(glidein);
				System.out.println();
			}
			
			// Submit glidein
			instance.submit(credentialEPR);
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
