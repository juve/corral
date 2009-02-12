package edu.usc.glidein.util;

import java.net.InetAddress;

import org.apache.log4j.Logger;

public class ServiceUtil {
	private static Logger logger = Logger.getLogger(ServiceUtil.class);
	
	public static String getServiceHost() {
		// Set the default condor host
		try {
			InetAddress addr = InetAddress.getLocalHost();
			return addr.getHostName();
		}  catch (Exception e) {
			logger.warn("Unable to get service host",e);
			return null;
		}
	}
}
