package edu.usc.glidein.util;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;

public class SiteUtil
{
	public static void print(Site site)
	{
		print(site,System.out);
	}
	
	public static void print(Site site, PrintStream out)
	{
		if (site==null) {
			out.println("null");
			return;
		}
		
		out.printf("id = %s\n", site.getId());
		out.printf("name = %s\n",site.getName());
		
		Calendar created = site.getCreated();
		if (created == null) {
			out.printf("created = null\n");
		} else {
			created.setTimeZone(TimeZone.getDefault());
			out.printf("created = %tc\n",created);
		}
		
		Calendar lastUpdate = site.getLastUpdate();
		if (lastUpdate == null) {
			out.printf("lastUpdate = null\n");
		} else {
			lastUpdate.setTimeZone(TimeZone.getDefault());
			out.printf("lastUpdate = %tc\n",lastUpdate);
		}
		
		out.printf("state = %s\n", site.getState().toString());
		out.printf("shortMessage = %s\n", site.getShortMessage());
		String longMessage = site.getLongMessage();
		if (longMessage == null) {
			out.printf("longMessage = null\n");
		} else {
			out.printf("longMessage = <<END\n");
			out.printf(longMessage);
			out.printf("\nEND\n");
		}
		out.printf("installPath = %s\n", site.getInstallPath());
		out.printf("localPath = %s\n", site.getLocalPath());
		out.printf("condorVersion = %s\n", site.getCondorVersion());
		out.printf("condorPackage = %s\n", site.getCondorPackage());
		
		// Environment
		EnvironmentVariable[] env = site.getEnvironment();
		out.printf("environment =");
		if (env != null) {
			for (EnvironmentVariable var : env) {
				out.printf(" %s=%s", var.getVariable(), var.getValue());
			}
		}
		out.printf("\n");
		
		// Staging Service
		ExecutionService stagingService = site.getStagingService();
		if (stagingService == null) {
			out.printf("stagingService = null\n");
		} else {
			out.printf("stagingService = %s %s\n",
					stagingService.getServiceType(),
					stagingService.getServiceContact());
			out.printf("stagingService.project = %s\n", stagingService.getProject());
			out.printf("stagingService.queue = %s\n", stagingService.getQueue());
		}
		
		// Glidein Service
		ExecutionService glideinService = site.getGlideinService();
		if (glideinService == null) {
			out.printf("glideinService = null\n");
		} else {
			out.printf("glideinService = %s %s\n",
					glideinService.getServiceType(),
					glideinService.getServiceContact());
			out.printf("glideinService.project = %s\n", glideinService.getProject());
			out.printf("glideinService.queue = %s\n", glideinService.getQueue());
		}
	}
	
	private static String getRequired(Properties p, String key, String name) throws Exception
	{
		String value = p.getProperty(key);
		if (value == null || "".equals(value)) {
			throw new Exception("Missing "+key);
		}
		return value.trim();
	}
	
	public static Site createSite(Properties p) throws Exception
	{
		Site s = new Site();
		
		String name = getRequired(p,"name","site name");
		s.setName(name);
		
		s.setInstallPath(getRequired(p,"installPath","install path"));
		s.setLocalPath(getRequired(p,"localPath","install path"));
		
		String condorPackage = p.getProperty("condorPackage");
		String condorVersion = p.getProperty("condorVersion");
		if (condorPackage == null && condorVersion == null) {
			throw new Exception(
					"Must specify either condor package or condor version");
		} else {
			s.setCondorPackage(condorPackage);
			s.setCondorVersion(condorVersion);
		}
		
		/* Staging service */
		try {
			String staging = getRequired(p,"stagingService","staging service");
			String[] comp = staging.trim().split("[ ]", 2);
			ExecutionService stagingService = new ExecutionService();
			stagingService.setProject(p.getProperty("stagingService.project"));
			stagingService.setQueue(p.getProperty("stagingService.queue"));
			stagingService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			stagingService.setServiceContact(comp[1]);
			s.setStagingService(stagingService);
		} catch (Exception e) {
			throw new Exception("Unable to create staging service " +
					"for site '"+name+"'. Are you sure you used the right " +
					"format for the staging service?");
		}
		
		/* Glidein service */
		try {
			String glidein = getRequired(p,"glideinService","glidein service");
			String[] comp = glidein.trim().split("[ ]", 2);
			ExecutionService glideinService = new ExecutionService();
			glideinService.setProject(p.getProperty("glideinService.project"));
			glideinService.setQueue(p.getProperty("glideinService.queue"));
			glideinService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			glideinService.setServiceContact(comp[1]);
			s.setGlideinService(glideinService);
		} catch (Exception e) {
			throw new Exception("Unable to create glidein service " +
					"for site '"+name+"'. Are you sure you used the right " +
					"format for the glidein service?");
		}
		
		/* Environment */
		String env = p.getProperty("environment");
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
			s.setEnvironment(envs.toArray(new EnvironmentVariable[0]));
		}
		
		return s;
	}
}
