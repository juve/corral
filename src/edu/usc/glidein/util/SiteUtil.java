package edu.usc.glidein.util;

import static edu.usc.glidein.service.SiteNames.*;

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
		
		out.printf("%s = %s\n",ID,site.getId());
		out.printf("%s = %s\n",NAME,site.getName());
		
		Calendar created = site.getCreated();
		if (created == null) {
			out.printf("%s = null\n",CREATED);
		} else {
			created.setTimeZone(TimeZone.getDefault());
			out.printf("%s = %tc\n",CREATED,created);
		}
		
		Calendar lastUpdate = site.getLastUpdate();
		if (lastUpdate == null) {
			out.printf("%s = null\n",LAST_UPDATE);
		} else {
			lastUpdate.setTimeZone(TimeZone.getDefault());
			out.printf("%s = %tc\n",LAST_UPDATE,lastUpdate);
		}
		
		out.printf("%s = %s\n", STATE, site.getState().toString());
		out.printf("%s = %s\n", SHORT_MESSAGE, site.getShortMessage());
		String longMessage = site.getLongMessage();
		if (longMessage == null) {
			out.printf("%s = null\n",LONG_MESSAGE);
		} else {
			out.printf("%s = <<END\n",LONG_MESSAGE);
			out.printf(longMessage);
			out.printf("\nEND\n");
		}
		out.printf("%s = %s\n", INSTALL_PATH, site.getInstallPath());
		out.printf("%s = %s\n", LOCAL_PATH, site.getLocalPath());
		out.printf("%s = %s\n", CONDOR_VERSION, site.getCondorVersion());
		out.printf("%s = %s\n", CONDOR_PACKAGE, site.getCondorPackage());
		
		// Environment
		EnvironmentVariable[] env = site.getEnvironment();
		out.printf("%s =", ENVIRONMENT);
		if (env != null) {
			for (EnvironmentVariable var : env) {
				out.printf(" %s=%s", var.getVariable(), var.getValue());
			}
		}
		out.printf("\n");
		
		// Staging Service
		ExecutionService stagingService = site.getStagingService();
		if (stagingService == null) {
			out.printf("%s = null\n", STAGING_SERVICE);
		} else {
			out.printf("%s = %s %s\n",
					STAGING_SERVICE,
					stagingService.getServiceType(),
					stagingService.getServiceContact());
			out.printf("%s = %s\n", STAGING_SERVICE_PROJECT, stagingService.getProject());
			out.printf("%s = %s\n", STAGING_SERVICE_QUEUE, stagingService.getQueue());
		}
		
		// Glidein Service
		ExecutionService glideinService = site.getGlideinService();
		if (glideinService == null) {
			out.printf("%s = null\n", GLIDEIN_SERVICE);
		} else {
			out.printf("%s = %s %s\n",
					GLIDEIN_SERVICE,
					glideinService.getServiceType(),
					glideinService.getServiceContact());
			out.printf("%s = %s\n", GLIDEIN_SERVICE_PROJECT, glideinService.getProject());
			out.printf("%s = %s\n", GLIDEIN_SERVICE_QUEUE, glideinService.getQueue());
		}
		
		out.printf("%s = %s\n", SUBJECT, site.getSubject());
		out.printf("%s = %s\n", LOCAL_USERNAME, site.getLocalUsername());
	}
	
	private static String getRequired(Properties p, String key) throws Exception
	{
		String value = p.getProperty(key);
		if (value == null || "".equals(value)) {
			throw new Exception("Missing required parameter "+key);
		}
		return value.trim();
	}
	
	public static Site createSite(Properties p) throws Exception
	{
		Site s = new Site();
		
		String name = getRequired(p,NAME);
		s.setName(name);
		
		s.setInstallPath(getRequired(p,INSTALL_PATH));
		s.setLocalPath(getRequired(p,LOCAL_PATH));
		
		String condorPackage = p.getProperty(CONDOR_PACKAGE);
		String condorVersion = p.getProperty(CONDOR_VERSION);
		if (condorPackage == null && condorVersion == null) {
			throw new Exception(
					"Must specify either "+CONDOR_PACKAGE+" or "+CONDOR_VERSION);
		} else {
			s.setCondorPackage(condorPackage);
			s.setCondorVersion(condorVersion);
		}
		
		/* Staging service */
		try {
			String staging = getRequired(p,STAGING_SERVICE);
			String[] comp = staging.trim().split("[ ]", 2);
			ExecutionService stagingService = new ExecutionService();
			stagingService.setProject(p.getProperty(STAGING_SERVICE_PROJECT));
			stagingService.setQueue(p.getProperty(STAGING_SERVICE_QUEUE));
			stagingService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			stagingService.setServiceContact(comp[1]);
			s.setStagingService(stagingService);
		} catch (Exception e) {
			throw new Exception("Unable to create staging service " +
					"for site '"+name+"'. Are you sure you used the right " +
					"format for "+STAGING_SERVICE+"?");
		}
		
		/* Glidein service */
		try {
			String glidein = getRequired(p,GLIDEIN_SERVICE);
			String[] comp = glidein.trim().split("[ ]", 2);
			ExecutionService glideinService = new ExecutionService();
			glideinService.setProject(p.getProperty(GLIDEIN_SERVICE_PROJECT));
			glideinService.setQueue(p.getProperty(GLIDEIN_SERVICE_QUEUE));
			glideinService.setServiceType(ServiceType.fromString(comp[0].toUpperCase()));
			glideinService.setServiceContact(comp[1]);
			s.setGlideinService(glideinService);
		} catch (Exception e) {
			throw new Exception("Unable to create glidein service " +
					"for site '"+name+"'. Are you sure you used the right " +
					"format for "+GLIDEIN_SERVICE+"?");
		}
		
		/* Environment */
		String env = p.getProperty(ENVIRONMENT);
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
