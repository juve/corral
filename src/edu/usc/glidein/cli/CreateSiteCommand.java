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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globus.gsi.GlobusCredential;

import edu.usc.corral.types.CreateSiteRequest;
import edu.usc.corral.types.CreateSiteResponse;
import edu.usc.corral.types.EnvironmentVariable;
import edu.usc.corral.types.ExecutionService;
import edu.usc.corral.types.GetRequest;
import edu.usc.corral.types.ServiceType;
import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteState;
import edu.usc.corral.types.SiteStateChange;
import edu.usc.corral.types.SubmitRequest;
import edu.usc.glidein.api.SiteListener;
import edu.usc.glidein.api.SiteService;

public class CreateSiteCommand extends Command implements SiteListener {
	private CreateSiteRequest req = null;
	private boolean verbose;
	private boolean wait;
	private CommandException exception;
	
	public void addOptions(List<Option> options) {
		options.add(
			Option.create()
				  .setOption("c")
				  .setLongOption("catalog-file")
				  .setUsage("-c | --catalog-file <file>")
				  .setDescription("The catalog file containing sites")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("f")
				  .setLongOption("catalog-format")
				  .setUsage("-f | --catalog-format <format>")
				  .setDescription("The format of the site catalog (one of: 'ini', or 'xml'; \n" +
				  				  "default: determined by extension)")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("n")
				  .setLongOption("site-name")
				  .setUsage("-n | --site-name <name>")
				  .setDescription("The name of the site to create")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("C")
				  .setLongOption("credential")
				  .setUsage("-C | --credential <file>")
				  .setDescription("The user's credential as a proxy file. If not specified the \n" +
				  				  "Globus default is used.")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("v")
				  .setLongOption("verbose")
				  .setUsage("-v | --verbose")
				  .setDescription("Show details about the new site")
		);
		
		options.add(
			Option.create()
				  .setOption("ip")
				  .setLongOption("install-path")
				  .setUsage("-ip | --install-path <path>")
				  .setDescription("This is the remote path where executables are installed. \n" +
				  				  "Default: '$HOME/.corral/$CORRAL_SERVER/$CORRAL_SITE_ID'")
				  .hasArgument()
		);
		
		
		options.add(
			Option.create()
				  .setOption("lp")
				  .setLongOption("local-path")
				  .setUsage("-lp | --local-path <path>")
				  .setDescription("This is the remote path where log files, etc. are placed \n" +
				  				  "(i.e. local scratch). Default: '/tmp'")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("ss")
				  .setLongOption("staging-service")
				  .setUsage("-ss | --staging-service <svc>")
				  .setDescription("This is the gatekeeper to use for setup (i.e. fork). The \n" +
						  		  "format follows the condor format for grid resource. Only \n" +
						  		  "the gt2 and gt4 grid types are supported right now. (e.x. \n" +
				  				  "'gt2 dynamic.usc.edu/jobmanager-fork')")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("ssp")
				  .setLongOption("staging-service-project")
				  .setUsage("-ssp | --staging-service-project <proj>")
				  .setDescription("The project to use for the staging service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("ssq")
				  .setLongOption("staging-service-queue")
				  .setUsage("-ssq | --staging-service-queue <queue>")
				  .setDescription("The queue to use for the staging service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("gs")
				  .setLongOption("glidein-service")
				  .setUsage("-gs | --glidein-service <svc>")
				  .setDescription("This is the gatekeeper to use for glideins (i.e. pbs). \n" +
				  				  "The format is identical to the one for the staging service.")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("gsp")
				  .setLongOption("glidein-service-project")
				  .setUsage("-gsp | --glidein-service-project <proj>")
				  .setDescription("The project to use for the glidein service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("gsq")
				  .setLongOption("glidein-service-queue")
				  .setUsage("-gsq | --glidein-service-queue <queue>")
				  .setDescription("The queue to use for the glidein service")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("cv")
				  .setLongOption("condor-version")
				  .setUsage("-cv | --condor-version <ver>")
				  .setDescription("The version of Condor to setup on the remote site. \n" +
				  				  "(e.x '7.0.0')")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("cp")
				  .setLongOption("condor-package")
				  .setUsage("-cp | --condor-package <pkg>")
				  .setDescription("The name of the package to download. This overrides condor-version. \n" +
				  				  "The typical format for the package name is: \n" +
				  				  "'<condorversion>-<arch>-<os>-<osversion>-glibc<glibcversion>.tar.gz' \n" +
				  				  "(e.x. '7.0.0-ia64-Linux-2.4-glibc2.2.tar.gz'), but you can \n" +
				  				  "specify any package name you like, as long as it is mapped in RLS. \n" +
				  				  "The value can be a simple file name like the example above, an \n" +
				  				  "absolute path, or a URL. The protocols supported for URLs include: \n" +
				  				  "file, http, https, ftp, and gsiftp. In most cases the value will end \n" +
				  				  "with .tar.gz. If you provide a package name that is not an \n" +
				  				  "absolute path or URL, then the service will try to look it up in RLS.")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("e")
				  .setLongOption("environment")
				  .setUsage("-e | --environment <env>")
				  .setDescription("This is the environment for staging and glideins. Use ':' \n" +
				  				  "to separate entries. (e.x. 'FOO=f:BAR=b')")
				  .hasArgument()
		);
		
		options.add(
			Option.create()
				  .setOption("W")
				  .setLongOption("wait")
				  .setUsage("-W | --wait")
				  .setDescription("Block waiting for the site to become READY, FAILED or DELETED.")
		);
	}
	
	public void setArguments(CommandLine cmdln) throws CommandException {
		/* Check for extra arguments */
		String[] args = cmdln.getArgs();
		if (args.length > 0) {
			throw new CommandException("Unrecognized argument: "+args[0]);
		}
		
		req = new CreateSiteRequest();
		
		/* The name of the new site */
		String siteName;
		if (cmdln.hasOption("n")) {
			siteName = cmdln.getOptionValue("site-name");
		} else {
			throw new CommandException("Missing site-name argument");
		}
		req.setName(siteName);
		
		req.setInstallPath(cmdln.getOptionValue("ip","$HOME/.corral/$CORRAL_SERVER/$CORRAL_SITE_ID"));
		req.setLocalPath(cmdln.getOptionValue("lp","/tmp"));
		
		String condorPackage = cmdln.getOptionValue("cp");
		String condorVersion = cmdln.getOptionValue("cv");
		if (condorPackage == null && condorVersion == null) {
			throw new CommandException(
					"Must specify either condor-package or condor-version");
		} else {
			req.setCondorPackage(condorPackage);
			req.setCondorVersion(condorVersion);
		}
		
		/* Staging service */
		try {
			String staging = cmdln.getOptionValue("ss");
			String[] comp = staging.trim().split("[ ]", 2);
			ExecutionService stagingService = new ExecutionService();
			stagingService.setProject(cmdln.getOptionValue("ssp"));
			stagingService.setQueue(cmdln.getOptionValue("ssq"));
			stagingService.setServiceType(ServiceType.valueOf(comp[0].toUpperCase()));
			stagingService.setServiceContact(comp[1]);
			req.setStagingService(stagingService);
		} catch (Exception e) {
			throw new CommandException("Unable to create staging service " +
					"for site '"+siteName+"'. Are you sure you used the right " +
					"format for staging-service?");
		}
		
		/* Glidein service */
		try {
			String glidein = cmdln.getOptionValue("gs");
			String[] comp = glidein.trim().split("[ ]", 2);
			ExecutionService glideinService = new ExecutionService();
			glideinService.setProject(cmdln.getOptionValue("gsp"));
			glideinService.setQueue(cmdln.getOptionValue("gsq"));
			glideinService.setServiceType(ServiceType.valueOf(comp[0].toUpperCase()));
			glideinService.setServiceContact(comp[1]);
			req.setGlideinService(glideinService);
		} catch (Exception e) {
			throw new CommandException("Unable to create glidein service " +
					"for site '"+siteName+"'. Are you sure you used the right " +
					"format for glidein-service?");
		}
		
		/* Environment */
		String env = cmdln.getOptionValue("e");
		if (env!=null) {
			List<EnvironmentVariable> envs = new LinkedList<EnvironmentVariable>();
			Pattern pat = Pattern.compile("([^=]+)=([^:]+):?");
			Matcher mat = pat.matcher(env);
			while (mat.find()) {
				EnvironmentVariable e = new EnvironmentVariable();
				e.setVariable(mat.group(1));
				e.setValue(mat.group(2));
				envs.add(e);
			}
			req.setEnvironment(envs);
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
	}
	
	public void execute() throws CommandException {
		if (isDebug()) System.out.printf("Creating site...\n");
		
		try {
			// Create site
			SiteService svc = new SiteService(getHost(), getPort());
			CreateSiteResponse resp = svc.create(req);
			
			// If verbose, print details
			if (verbose) {
				GetRequest req = new GetRequest();
				req.setId(resp.getId());
				Site site = svc.getSite(req);
				site.print();
				System.out.println();
			}
			
			if (isDebug()) System.out.printf("Site created.\n");

			// Submit the new site
			SubmitRequest req = new SubmitRequest(resp.getId(), GlobusCredential.getDefaultCredential());
			svc.submit(req);
			
			if (isDebug()) System.out.printf("Site submitted.\n");
			
			// Wait for started event
			if (wait) {
				if (isDebug()) { 
					System.out.println("Waiting for site "+resp.getId()+"...");
				}
				
				// Subscribe
				svc.addListener(this);
				
				// Wait for state change
				while (wait) {
					if (isDebug()) System.out.print(".");
					try {
						Thread.sleep(10000);
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
		} catch (Exception ge) {
			throw new CommandException("Unable to create site: "+ge.getMessage(),ge);
		}
	}

	public String getName() {
		return "create-site";
	}
	
	public String[] getAliases() {
		return new String[]{"cs"};
	}
	
	public String getUsage() {
		return "Usage:\n" +
				"   create-site [options] --site-name <name> --install-path <path> --local-path <path>\n" +
				"                  --staging-service <svc> [--staging-service-project <proj>] [--staging-service-queue <queue>]\n" +
				"                  --glidein-service <svc> [--glidein-service-project <proj>] [--glidein-service-queue <queue>]\n" +
				"                  [--condor-version <ver> | --condor-package <pkg>] [--environment <env>]";
	}
	
	public String getDescription() {
		return "create-site (cs): Add a new site";
	}
	
	public void stateChanged(SiteStateChange stateChange) {
		SiteState state = stateChange.getState();
		if (isDebug()) {
			System.out.println("Site state changed to "+state);
			System.out.println("\tShort message: "+stateChange.getShortMessage());
			if (stateChange.getLongMessage() != null)
				System.out.println("\tLong message:\n"+stateChange.getLongMessage());
		}
		
		// If the new state is running, failed or deleted, then stop waiting
		if (state.equals(SiteState.READY)) {
			wait = false;
		} else if (state.equals(SiteState.FAILED) || 
				state.equals(SiteState.DELETED)) {
			exception = new CommandException("Site became "+state+": "+
					stateChange.getShortMessage()+"\n"+stateChange.getLongMessage());
			wait = false;
		}
	}
}
