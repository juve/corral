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

import static edu.usc.glidein.service.GlideinNames.*;

import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.axis.message.addressing.EndpointReferenceType;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.wsrf.impl.security.descriptor.ClientSecurityDescriptor;

import edu.usc.glidein.api.GlideinFactoryService;
import edu.usc.glidein.api.GlideinListener;
import edu.usc.glidein.api.GlideinService;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinState;
import edu.usc.glidein.stubs.types.GlideinStateChange;
import edu.usc.glidein.util.GlideinUtil;

public class CreateGlideinCommand extends Command implements GlideinListener
{
	private Glidein glidein = null;
	private GlobusCredential credential = null;
	private boolean verbose = false;
	private boolean wait = false;
	private CommandException exception = null;
	
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
				  .setDescription("Total number of processes across all hosts (default: = host-count)")
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
				  .setDescription("Glidein max idle time in minutes (default: wall-time)")
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
		String defaultCondorHost = getLocalHost();
		options.add(
			Option.create()
				  .setOption("ch")
				  .setLongOption("condor-host")
				  .setUsage("-ch [--condor-host] <name:port>")
				  .setDescription("Condor central manager to report to"+(defaultCondorHost==null?"":" (default: "+defaultCondorHost+")"))
				  .hasArgument()
							 
		);
		options.add(
			Option.create()
				  .setOption("cd")	
				  .setLongOption("condor-debug")
				  .setUsage("-cd [--condor-debug] <ops>")
				  .setDescription("Comma-separated list of Condor daemon debugging options (e.x. 'D_JOB,D_MACHINE'")
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
		options.add(
			Option.create()
				  .setOption("rsl")
				  .setLongOption("globus-rsl")
				  .setUsage("-rsl [--globus-rsl]")
				  .setDescription("The Globus RSL (GT2) or XML (GT4) to use for this job. This parameter will \n" +
				  		"override any values specified for count, host-count, wall-time and any project or queue \n" +
				  		"specified for the site's glidein execution service. You should include (jobType=multiple) \n" +
				  		"if you specify this parameter. Also be aware that the output of the list-glidein command \n" +
				  		"won't accurately reflect the parameters for the job as the output of that command is based \n" +
				  		"on the values of the regular parameters.")
				  .hasArgument()
		);
		options.add(
			Option.create()
				  .setOption("W")
				  .setLongOption("wait")
				  .setUsage("-W [--wait]")
				  .setDescription("Block waiting for the glidein to become RUNNING, FAILED, or DELETED.")
		);
	}
	
	private void setProperty(Properties p, String name, String value)
	{
		if (value == null) return;
		p.setProperty(name, value);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException
	{
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
		
		/* Create glidein */
		try {
			Properties p = new Properties();
			setProperty(p, SITE, cmdln.getOptionValue("s"));
			setProperty(p, CONDOR_HOST, cmdln.getOptionValue("ch", getLocalHost()));
			setProperty(p, HOST_COUNT, cmdln.getOptionValue("hc", "1"));
			setProperty(p, COUNT, cmdln.getOptionValue("c", cmdln.getOptionValue("hc", "1")));
			setProperty(p, NUM_CPUS, cmdln.getOptionValue("n", "1"));
			setProperty(p, WALL_TIME, cmdln.getOptionValue("w", "60"));
			setProperty(p, IDLE_TIME, cmdln.getOptionValue("i", cmdln.getOptionValue("w", "60")));
			setProperty(p, CONDOR_CONFIG, cmdln.getOptionValue("cc"));
			setProperty(p, CONDOR_DEBUG, cmdln.getOptionValue("cd"));
			setProperty(p, GCB_BROKER, cmdln.getOptionValue("b"));
			setProperty(p, RESUBMIT, cmdln.getOptionValue("r"));
			setProperty(p, RSL, cmdln.getOptionValue("rsl"));
			glidein = GlideinUtil.createGlidein(p);
		} catch (Exception e) {
			throw new CommandException(e);
		}
		
		/* Validate the credential given resubmits */
		if (glidein.isResubmit()) {
			long timeLeft = credential.getTimeLeft() * 1000;
			long timeRequired = 0;
			
			if (glidein.getResubmits() > 0) {
				int resubmits = glidein.getResubmits();
				timeRequired = resubmits * glidein.getWallTime() * 60 * 1000;
			} else {
				Calendar until = glidein.getUntil();
				Calendar now = Calendar.getInstance();
				timeRequired = until.getTimeInMillis() - now.getTimeInMillis();
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
	
	public void execute() throws CommandException
	{	
		if (isDebug()) System.out.println("Creating glidein...");
		
		// Delegate credential
		EndpointReferenceType credentialEPR = delegateCredential(credential);
		
		try {
			// Create glidein
			ClientSecurityDescriptor desc = getClientSecurityDescriptor();
			GlideinFactoryService factory = new GlideinFactoryService(
					getServiceURL(GLIDEIN_FACTORY_SERVICE));
			factory.setDescriptor(desc);
			EndpointReferenceType epr = factory.create(glidein);
			
			// Get instance
			GlideinService instance = new GlideinService(epr);
			instance.setDescriptor(desc);
			
			// If verbose, print details
			if (verbose) {
				glidein = instance.getGlidein();
				GlideinUtil.print(glidein);
				System.out.println();
			}
			
			if (isDebug()) System.out.println("Glidein created.");
			
			// Submit glidein
			instance.submit(credentialEPR);
			
			if (isDebug()) System.out.println("Glidein submitted.");
			
			// Wait for started event
			if (wait) {
				if (isDebug()) { 
					System.out.println("Waiting for glidein...");
				}
				
				// Subscribe
				instance.addListener(this);
				
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
				instance.removeListener(this);
				
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
		return "Usage: create-glidein --site <site>";
	}
	
	public void stateChanged(GlideinStateChange stateChange)
	{
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
