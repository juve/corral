package edu.usc.glidein.service.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class CommandLine {
	public static void main(String[] args){
		try {
			Process p = Runtime.getRuntime().exec("/opt/condor/6.9.5/bin/condor_status -xml",
					new String[]{
						"GLOBUS_LOCATION=/opt/globus/4.0.5",
						"CONDOR_HOME=/opt/condor/6.9.5",
						"CONDOR_CONFIG=/opt/condor/6.9.5/etc/condor_config"}, 
					new File("/Users/juve"));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String err;
			String out;
			StringBuffer error = new StringBuffer();
			StringBuffer output = new StringBuffer();
			while(true){
				if((out = stdout.readLine()) != null)
					output.append(out);
				if((err = stderr.readLine()) != null)
					error.append(err);
				if(out == null && err == null)
					break;
			}
			
			if (p.waitFor() != 0) {
				throw new IllegalStateException("Command failed");
			}
			
			System.out.println("exit value = " + p.exitValue());
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
