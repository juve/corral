package edu.usc.glidein.util;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.TimeZone;

import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
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
		
		Calendar created = (Calendar)site.getCreated().clone();
		created.setTimeZone(TimeZone.getDefault());
		out.printf("created = %tc\n", created);
		
		Calendar lastUpdate = (Calendar)site.getLastUpdate().clone();
		lastUpdate.setTimeZone(TimeZone.getDefault());
		out.printf("lastUpdate = %tc\n", lastUpdate);
		
		out.printf("state = %s\n", site.getState().toString());
		out.printf("shortMessage = %s\n", site.getShortMessage());
		out.printf("longMessage = %s\n", site.getLongMessage());
		
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
}
