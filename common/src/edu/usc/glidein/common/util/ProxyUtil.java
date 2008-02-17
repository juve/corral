package edu.usc.glidein.common.util;

import java.io.File;
import java.io.IOException;

import org.globus.util.ConfigUtil;

import edu.usc.glidein.GlideinException;

/**
 * Manipulate proxies
 *
 * @author Gideon Juve <juve@usc.edu>
 */
public class ProxyUtil
{

	public static String readProxy(String proxyFile) throws IOException
	{
		File file = new File(proxyFile);
		String proxy = IOUtil.read(file);
		return proxy;
	}
	
	public static String readProxy() throws IOException
	{
		String location = System.getProperty("X509_USER_PROXY");
		if(location == null)
		{
			location = System.getenv("X509_USER_PROXY");
			if(location == null)
			{
				location = ConfigUtil.discoverProxyLocation();
			}
		}
		return readProxy(location);
	}
	
	public static void writeProxy(String proxy, File proxyFile) 
	throws IOException, GlideinException
	{
		// Write proxy file
		IOUtil.write(proxy, proxyFile);
		
		// Change permissions
		CommandLine chmod = new CommandLine();
		chmod.setExecutable(new File("/bin/chmod"));
		chmod.addArgument("600");
		chmod.addArgument(proxyFile.getAbsolutePath());
		chmod.execute();
		int exitCode = chmod.getExitCode();
		if(exitCode != 0){
			throw new GlideinException(
					"Unable to change proxy permissions\n\n"+
					"Stdout:\n"+chmod.getOutput()+
					"Stderr:\n"+chmod.getError());
		}
	}

	public static void main(String[] args)
	{
		try
		{
			String proxy = ProxyUtil.readProxy();
			System.out.println(proxy);
			ProxyUtil.writeProxy(proxy,new File("/tmp/proxy"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
