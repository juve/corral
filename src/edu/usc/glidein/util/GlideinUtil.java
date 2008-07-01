package edu.usc.glidein.util;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.TimeZone;

import edu.usc.glidein.stubs.types.Glidein;

public class GlideinUtil
{
	public static void print(Glidein glidein)
	{
		print(glidein,System.out);
	}
	
	public static void print(Glidein glidein, PrintStream out)
	{
		if (glidein == null) {
			out.println("null");
			return;
		}
		
		out.printf("id = %d\n",glidein.getId());
		out.printf("siteName = %s\n", glidein.getSiteName());
		out.printf("siteId = %d\n", glidein.getSiteId());
		out.printf("condorHost = %s\n",glidein.getCondorHost());
		out.printf("count = %d\n", glidein.getCount());
		out.printf("hostCount = %d\n", glidein.getHostCount());
		out.printf("numCpus = %d\n", glidein.getNumCpus());
		out.printf("wallTime = %d\n", glidein.getWallTime());
		out.printf("idleTime = %d\n", glidein.getIdleTime());
		
		Calendar created = glidein.getCreated();
		created.setTimeZone(TimeZone.getDefault());
		out.printf("created = %tc\n",created);
		
		Calendar lastUpdate = glidein.getLastUpdate();
		lastUpdate.setTimeZone(TimeZone.getDefault());
		out.printf("lastUpdate = %tc\n",lastUpdate);
		
		out.printf("state = %s\n",glidein.getState().toString());
		out.printf("shortMessage = %s\n",glidein.getShortMessage());
		out.printf("longMessage = %s\n",glidein.getLongMessage());
		
		out.printf("condorDebug = %s\n", glidein.getCondorDebug());
		out.printf("gcbBroker = %s\n", glidein.getGcbBroker());
		out.printf("resubmit = %s\n", glidein.isResubmit());
	}
}
