package edu.usc.glidein.util;

import static edu.usc.glidein.service.GlideinNames.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
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
		
		out.printf("%s = %d\n", ID, glidein.getId());
		out.printf("%s = %d\n", SITE, glidein.getSiteId());
		out.printf("%s = %s\n", SITE_NAME, glidein.getSiteName());
		out.printf("%s = %s\n", CONDOR_HOST, glidein.getCondorHost());
		out.printf("%s = %d\n", COUNT, glidein.getCount());
		out.printf("%s = %d\n", HOST_COUNT, glidein.getHostCount());
		out.printf("%s = %d\n", NUM_CPUS, glidein.getNumCpus());
		out.printf("%s = %d\n", WALL_TIME, glidein.getWallTime());
		out.printf("%s = %d\n", IDLE_TIME, glidein.getIdleTime());
		
		Calendar created = glidein.getCreated();
		if (created == null) {
			out.printf("%s = null\n", CREATED);
		} else {
			created.setTimeZone(TimeZone.getDefault());
			out.printf("%s = %tc\n", CREATED, created);
		}
		
		Calendar lastUpdate = glidein.getLastUpdate();
		if (lastUpdate == null) {
			out.printf("%s = null\n", LAST_UPDATE);
		} else {
			lastUpdate.setTimeZone(TimeZone.getDefault());
			out.printf("%s = %tc\n", LAST_UPDATE, lastUpdate);
		}
		
		out.printf("%s = %s\n", STATE, glidein.getState().toString());
		out.printf("%s = %s\n", SHORT_MESSAGE, glidein.getShortMessage());
		String longMessage = glidein.getLongMessage();
		if (longMessage == null) {
			out.printf("%s = null\n", LONG_MESSAGE);
		} else {
			out.printf("%s = <<END\n", LONG_MESSAGE);
			out.printf(longMessage);
			out.printf("\nEND\n");
		}
		out.printf("%s = %s\n", CONDOR_DEBUG, glidein.getCondorDebug());
		out.printf("%s = %s\n", GCB_BROKER, glidein.getGcbBroker());
		out.printf("%s = %s\n", SUBMITS, glidein.getSubmits());
		out.printf("%s = %s\n", RESUBMIT, glidein.isResubmit());
		out.printf("%s = %s\n", RESUBMITS, glidein.getResubmits());
		
		Calendar until = glidein.getUntil();
		if (until == null) {
			out.printf("%s = null\n", UNTIL);
		} else {
			until.setTimeZone(TimeZone.getDefault());
			out.printf("%s = %tc\n", UNTIL, until);
		}
		out.printf("%s = %s\n", RSL, glidein.getRsl());
		out.printf("%s = %s\n", SUBJECT, glidein.getSubject());
		out.printf("%s = %s\n", LOCAL_USERNAME, glidein.getLocalUsername());
	}
	
	private static String getRequired(Properties p, String key) throws Exception
	{
		String value = p.getProperty(key);
		if (value == null || "".equals(value)) {
			throw new Exception("Missing required parameter "+key);
		}
		return value.trim();
	}
	
	public static Glidein createGlidein(Properties p) throws Exception
	{
		final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
		
		Glidein glidein = new Glidein();
		
		/* Required parameters */
		glidein.setSiteId(Integer.parseInt(getRequired(p,SITE)));
		glidein.setCondorHost(getRequired(p, CONDOR_HOST));
		glidein.setHostCount(Integer.parseInt(getRequired(p, HOST_COUNT)));
		glidein.setCount(Integer.parseInt(getRequired(p, COUNT)));
		glidein.setNumCpus(Integer.parseInt(getRequired(p, NUM_CPUS)));
		glidein.setWallTime(Integer.parseInt(getRequired(p, WALL_TIME)));
		glidein.setIdleTime(Integer.parseInt(getRequired(p, IDLE_TIME)));
		
		/* Optional parameters */
		glidein.setCondorDebug(p.getProperty(CONDOR_DEBUG));
		glidein.setGcbBroker(p.getProperty(GCB_BROKER));
		glidein.setRsl(p.getProperty(RSL));
		
		/* Custom glidein_condor_config file */
		String fileName = p.getProperty(CONDOR_CONFIG);
		if (fileName != null) {
			File file = new File(fileName);
			try {
				String condorConfig = IOUtil.read(file);
				byte[] condorConfigBytes = Base64.toBase64(condorConfig);
				glidein.setCondorConfig(condorConfigBytes);
			} catch (IOException ioe) {
				throw new Exception("Unable to read "+CONDOR_CONFIG+" file: "+fileName,ioe);
			}
		}
		
		/* Resubmit the glidein when it expires */
		String value = p.getProperty(RESUBMIT);
		if (value != null) {
			glidein.setResubmit(true);
			
			if (value.matches("[0-9]+")) {
				int resubmits = Integer.parseInt(value);
				if (resubmits <= 0 || resubmits > 128) {
					throw new Exception("Resubmits must be between 0 and 128");
				}
				glidein.setResubmits(resubmits);
			} else {
				Calendar until = Calendar.getInstance();
				Calendar now = Calendar.getInstance();
				try {
					SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
					until.setTime(parser.parse(value));
				} catch (ParseException pe) {
					throw new Exception("Invalid resubmit option: "+value);
				}
				if (until.before(now)) {
					throw new Exception("Resubmit date should be in the future");
				}
				glidein.setUntil(until);
			}
			
		} else {
			glidein.setResubmit(false);
		}
		
		/* Validate walltime */
		if (glidein.getWallTime()<2) {
			throw new Exception("Wall time must be >= 2 minutes");
		}
		
		return glidein;
	}
}
