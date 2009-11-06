/*
 *  Copyright 2007-2009 University Of Southern California
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
import java.util.Date;
import java.util.List;

import org.globus.gsi.GlobusCredential;

import edu.usc.glidein.api.GlideinListener;
import edu.usc.glidein.api.GlideinService;
import edu.usc.glidein.util.IOUtil;
import edu.usc.corral.types.CreateGlideinRequest;
import edu.usc.corral.types.CreateGlideinResponse;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.Glidein;
import edu.usc.corral.types.GlideinState;
import edu.usc.corral.types.GlideinStateChange;
import edu.usc.corral.types.SubmitRequest;

public class CreateGlideinCommand extends Command implements GlideinListener {
	private CreateGlideinRequest request = null;
	private boolean verbose = false;
	private boolean wait = false;
	private CommandException exception = null;
	
	public void addOptions(List<Option> options) {
		options.add(
			Option.create()
				  .setOption("s")
				  .setLongOption("site")
				  .setUsage("-s | --site <id>")
				  .setDescription("Site to submit glidein to")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("c")
				  .setLongOption("count")
				  .setUsage("-c | --count <n>")
				  .setDescription("Total number of processes across all hosts (default: = host-count)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("hc")
				  .setLongOption("host-count")
				  .setUsage("-hc | --host-count <n>")
				  .setDescription("Number of hosts (default: 1)")
				  .hasArgument()
							 
		);
		options.add(
			Option.create()
				  .setOption("w")
				  .setLongOption("wall-time")
				  .setUsage("-w | --wall-time <t>")
				  .setDescription("Wall time for job in minutes (default: 60, min: 2, max: site-specific)")		 
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("i")
				  .setLongOption("idle-time")
				  .setUsage("-i | --idle-time <t>")
				  .setDescription("Glidein max idle time in minutes (default: wall-time)")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("n")
				  .setLongOption("num-cpus")
				  .setUsage("-n | --num-cpus <n>")
				  .setDescription("Number of cpus for condor to report")
				  .hasArgument()
		);
		String defaultCondorHost = getLocalHost();
		options.add(
			Option.create()
				  .setOption("ch")
				  .setLongOption("condor-host")
				  .setUsage("-ch | --condor-host <name:port>")
				  .setDescription("Condor central manager to report to"+(defaultCondorHost==null?"":" (default: "+defaultCondorHost+")"))
				  .hasArgument()
							 
		);
		options.add(
			Option.create()
				  .setOption("cd")	
				  .setLongOption("condor-debug")
				  .setUsage("-cd | --condor-debug <ops>")
				  .setDescription("Comma-separated list of Condor daemon debugging options (e.x. 'D_JOB,D_MACHINE'")
				  .hasArgument()
				
		);
		options.add(
			Option.create()
				  .setOption("b")
				  .setLongOption("gcb-broker")
				  .setUsage("-b | --gcb-broker <ip>")
				  .setDescription("GCB Broker IP address")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("cc")
				  .setLongOption("condor-config")
				  .setUsage("-cc | --condor-config <file>")
				  .setDescription("Condor config file for glidein")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("r")
				  .setLongOption("resubmit")
				  .setUsage("-r | --resubmit [<number>|<date>]")
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
				  .setUsage("-v | --verbose")
				  .setDescription("Show details about the new glidein")
		);
		options.add(
			Option.create()
				  .setOption("rsl")
				  .setLongOption("globus-rsl")
				  .setUsage("-rsl | --globus-rsl <rsl>")
				  .setDescription("The Globus RSL (GT2) or XML (GT4) to append to the glidein job.")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("W")
				  .setLongOption("wait")
				  .setUsage("-W | --wait")
				  .setDescription("Block waiting for the glidein to become RUNNING, FAILED, or DELETED.")
		);
		options.add(
			Option.create()
				  .setOption("hp")
				  .setLongOption("highport")
				  .setUsage("-hp | --highport <port>")
				  .setDescription("The high end of the port range to allow the glidein to use at the remote site.")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("lp")
				  .setLongOption("lowport")
				  .setUsage("-lp | --lowport <port>")
				  .setDescription("The low end of the port range to allow the glidein to use at the remote site.")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("ccb")
				  .setLongOption("ccb-address")
				  .setUsage("-ccb | --ccb-address [<host:port>]")
				  .setDescription("The address of the CCB broker to use. If no host is provided, then condor-host will be used.")
				  .hasOptionalArgument()
		);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException {
		final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
		
		/* Check for extra arguments */
		String[] args = cmdln.getArgs();
		if (args.length > 0) {
			throw new CommandException("Unrecognized argument: "+args[0]);
		}
		
		/* Verbose */
		if (cmdln.hasOption("v")) {
			verbose = true;
		} else {
			verbose = false;
		}
		
		/* Wait */
		if (cmdln.hasOption("W")) {
			wait = true;
		} else {
			wait = false;
		}
		
		request = new CreateGlideinRequest();
		
		/* Required parameters */
		request.setSiteId(Integer.parseInt(cmdln.getOptionValue("s")));
		request.setCondorHost(cmdln.getOptionValue("ch",getLocalHost()));
		request.setHostCount(Integer.parseInt(cmdln.getOptionValue("hc","1")));
		request.setCount(Integer.parseInt(cmdln.getOptionValue("c",cmdln.getOptionValue("hc","1"))));
		request.setNumCpus(Integer.parseInt(cmdln.getOptionValue("n","1")));
		request.setWallTime(Integer.parseInt(cmdln.getOptionValue("w","60")));
		request.setIdleTime(Integer.parseInt(cmdln.getOptionValue("i",cmdln.getOptionValue("w","60"))));
		
		/* Optional parameters */
		request.setCondorDebug(cmdln.getOptionValue("cd"));
		request.setGcbBroker(cmdln.getOptionValue("b"));
		request.setRsl(cmdln.getOptionValue("rsl"));
		
		if (cmdln.hasOption("ccb")) {
			request.setCcbAddress(cmdln.getOptionValue("ccb",cmdln.getOptionValue("ch",getLocalHost())));
		}
		
		/* Optional port range */
		String highport = cmdln.getOptionValue("hp");
		if (highport != null) request.setHighport(Integer.parseInt(highport));
		String lowport = cmdln.getOptionValue("lp");
		if (lowport != null) request.setLowport(Integer.parseInt(lowport));
		
		/* Custom glidein_condor_config file */
		String fileName = cmdln.getOptionValue("cc");
		if (fileName != null) {
			File file = new File(fileName);
			try {
				String condorConfig = IOUtil.read(file);
				request.setCondorConfig(condorConfig);
			} catch (IOException ioe) {
				throw new CommandException("Unable to read condor-config file: "+fileName,ioe);
			}
		}
		
		/* Resubmit the glidein when it expires */
		String value = cmdln.getOptionValue("r");
		if (value != null) {
			request.setResubmit(true);
			if (value.matches("[0-9]+")) {
				int resubmits = Integer.parseInt(value);
				if (resubmits <= 0 || resubmits > 128) {
					throw new CommandException("Resubmits must be between 0 and 128");
				}
				request.setResubmits(resubmits);
			} else {
				Date now = new Date();
				try {
					SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
					request.setUntil(parser.parse(value));
				} catch (ParseException pe) {
					throw new CommandException("Invalid resubmit option: "+value);
				}
				if (request.getUntil().before(now)) {
					throw new CommandException("Resubmit date should be in the future");
				}
			}
		} else {
			request.setResubmit(false);
		}
		
		/* Validate walltime */
		if (request.getWallTime()<2) {
			throw new CommandException("Wall time must be >= 2 minutes");
		}
		
		/* Validate the credential given resubmits */
		if (request.getResubmit()) {
			long timeLeft = getCredential().getTimeLeft() * 1000;
			long timeRequired = 0;
			
			if (request.getResubmits() > 0) {
				int resubmits = request.getResubmits();
				timeRequired = resubmits * request.getWallTime() * 60 * 1000;
			} else {
				Date until = request.getUntil();
				Date now = new Date();
				timeRequired = until.getTime() - now.getTime();
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
		}
	}
	
	public void execute() throws CommandException {	
		if (isDebug()) System.out.println("Creating glidein...");
		
		try {
			// Create glidein
			GlideinService svc = new GlideinService(
					getHost(), getPort());
			
			CreateGlideinResponse resp = svc.create(request);
			
			// If verbose, print details
			if (verbose) {
				GetRequest req = new GetRequest();
				req.setId(resp.getId());
				Glidein glidein = svc.getGlidein(req);
				glidein.print();
				System.out.println();
			}
			
			if (isDebug()) System.out.println("Glidein created.");
			
			// Submit glidein
			SubmitRequest req = new SubmitRequest(resp.getId(), GlobusCredential.getDefaultCredential());
			svc.submit(req);
			
			if (isDebug()) System.out.println("Glidein submitted.");
			
			// Wait for started event
			if (wait) {
				if (isDebug()) { 
					System.out.println("Waiting for glidein...");
				}
				
				// Subscribe
				svc.addListener(this);
				
				// Wait for state change
				while (wait) {
					if (isDebug()) System.out.print(".");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}
				}
				
				// Unsubscribe
				svc.removeListener(this);
				
				if (isDebug()) {
					System.out.println("Finished waiting.");
				}
				
				// Throw the exception if it failed
				if (exception != null) {
					throw exception;
				}
			}
		} catch (Exception e) {
			throw new CommandException("Unable to create glidein: "+e.getMessage(),e);
		}
	}

	public String getName() {
		return "create-glidein";
	}
	
	public String[] getAliases() {
		return new String[]{"cg"};
	}
	
	public String getDescription() {
		return "create-glidein (cg): Add a new glidein";
	}
	
	public String getUsage() {
		return "Usage: create-glidein --site <site>";
	}
	
	public void stateChanged(GlideinStateChange stateChange) {
		GlideinState state = stateChange.getState();
		if (isDebug()) {
			System.out.println("Glidein state changed to "+state);
			System.out.println("\tShort message: "+stateChange.getShortMessage());
			if (stateChange.getLongMessage() != null)
				System.out.println("\tLong message:\n"+stateChange.getLongMessage());
		}
		
		// If the new state is running, failed or deleted, then stop waiting
		if (state.equals(GlideinState.RUNNING)) {
			wait = false;
		} else if (state.equals(GlideinState.FAILED) || 
				state.equals(GlideinState.DELETED)) {
			exception = new CommandException("Glidein became "+state+": "+
					stateChange.getShortMessage()+"\n"+stateChange.getLongMessage());
			wait = false;
		}
	}
}