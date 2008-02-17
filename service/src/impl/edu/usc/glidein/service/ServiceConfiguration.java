package edu.usc.glidein.service;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.glidein.GlideinException;

/**
 * This class provides access to the glidein service's configuration. It 
 * checks the following variables in order to find the name of the 
 * configuration file to load:
 * <ol>
 *   <li>The system property -Dglidein.config</li>
 *   <li>The environment variable $GLIDEIN_CONFIG</li>
 *   <li>$HOME/.glidein/glidein.config</li>
 *   <li>$GLOBUS_LOCATION/etc/glidein_service/glidein.config</li>
 * </ol>
 * 
 * @author Gideon Juve <juve@usc.edu>
 */
public class ServiceConfiguration
{
	/**
	 * Singleton instance
	 */
	private static ServiceConfiguration instance;

	/**
	 * The path to the configuration file loaded
	 */
	private File configFile;
	
	/**
	 * The service properties loaded
	 */
	private Properties properties;
	
	/**
	 * Load the service configuration from a file
	 * @param configFile The configuration file
	 * @throws GlideinException If unable to read the configuration file
	 */
	private ServiceConfiguration(File configFile)
	throws GlideinException
	{
		this.configFile = configFile;
		this.properties = new Properties();
		
		// Load properties from file
		FileInputStream is = null;
		try
		{
			is = new FileInputStream(configFile);
			this.properties.load(is);
		}
		catch(IOException ioe)
		{
			throw new GlideinException(
					"Unable to read configuration from "+
					configFile.getAbsolutePath(), ioe);
		}
		finally 
		{
			try { if(is!=null) is.close(); }
			catch(Exception e){ /* Ignore */ }
		}
	}
	
	/**
	 * Get a property by name
	 * @param key The name of the property
	 * @return The value of the property
	 * @throws GlideinException If the property does not exist
	 */
	public String getProperty(String key)
	throws GlideinException
	{
		String prop = getTransliterated(key);
		if(prop==null) 
			throw new GlideinException(
				"Property "+key+" not found in configuration file "+
				configFile.getAbsolutePath());
		return prop;
	}
	
	/**
	 * Get a property by name with a default value
	 * @param key The name of the property
	 * @param def The default value if the property does not exist
	 * @return The value of the property or default
	 * @throws GlideinException If there's a problem getting the property
	 */
	public String getProperty(String key, String def)
	throws GlideinException
	{
		String prop = getTransliterated(key);
		if(prop==null) prop = def;
		return prop;
	}
	
	/**
	 * Get the value of a property by recursively transliterating all 
	 * expressions into their terminal values.
	 * @param key The name of the property
	 * @return The value or null if the property does not exist
	 * @throws GlideinException If there's a problem getting the property
	 */
	private String getTransliterated(String key)
	throws GlideinException
	{
		// Get the value
		String value = properties.getProperty(key);
		
		// If it doesn't exist, then don't continue
		if(value == null) return null;
		
		// Look for expressions like ${NAMESPACE:KEY} within the value
		Pattern p = Pattern.compile("\\$\\{(([a-z]+):)?([._a-zA-Z]+)\\}");
		StringBuffer buf = new StringBuffer(value);
		Matcher m = p.matcher(buf);
		while(m.find())
		{
			int start = m.start();
			int end = m.end();
			
			// The namespace is the first part before the colon
			String ns = m.group(2);
			
			// The next key is the part after the colon
			String nk = m.group(3);
			
			// If there was no namespace, then key is a regular property
			if(ns==null)
			{
				String next = getTransliterated(nk);
				if(next == null) next = "";
				buf = buf.replace(start, end, next);
			}
			
			// If the namespace is env, then key is an environment variable
			else if("env".equals(ns))
			{
				String env = System.getenv(nk);
				if(env==null) env = "";
				buf = buf.replace(start, end, env);
			}
			
			// If the namespace is sys, then key is a system property
			else if("sys".equals(ns))
			{
				String sys = System.getProperty(nk);
				if(sys==null) sys = "";
				buf = buf.replace(start, end, sys);
			}
			
			// If the namespace is something else, then that's an error
			else
			{
				throw new GlideinException(
						"Unrecognized namespace: "+ns);
			}
			
			// Look for more expressions
			m = p.matcher(buf);
		}
		
		return buf.toString();
	}
	
	/**
	 * Get the path to the configuration file that was actually loaded
	 * @return The configuration file
	 */
	public File getConfigFile()
	{
		return configFile.getAbsoluteFile();
	}

	/**
	 * Get an instance of the service configuration
	 * @return The instance
	 * @throws GlideinException if unable to load the configuration
	 */
	public static synchronized ServiceConfiguration getInstance()
	throws GlideinException
	{
		if(instance==null)
		{
			// 1. Try system property -Dglidein.config
			String sys = System.getProperty("glidein.config");
			if(sys!=null && sys.length()>0)
			{
				File sysConfig = new File(sys);
				if(sysConfig.exists())
				{
					instance = new ServiceConfiguration(sysConfig);
					return instance;
				}
			}
			
			// 2. Try environment variable $GLIDEIN_CONFIG
			String env = System.getenv("GLIDEIN_CONFIG");
			if(env!=null && env.length()>0)
			{
				File envConfig = new File(env);
				if(envConfig.exists())
				{
					instance = new ServiceConfiguration(envConfig);
					return instance;
				}
			}
			
			// 3. Try ${user.home}/.glidein/glidein.config
			String home = System.getProperty("user.home");
			if(home!=null && home.length()>0)
			{
				File homeConfig = new File(
						home + File.separator +
						".glidein" + File.separator +
						"glidein.config");
				if(homeConfig.exists())
				{
					instance = new ServiceConfiguration(homeConfig);
					return instance;
				}
			}
			
			// 4. Try $GLOBUS_LOCATION/etc/glidein_service/glidein.config
			String globus = System.getenv("GLOBUS_LOCATION");
			if(globus!=null && globus.length()>0){
				File globusFile = new File(
						globus + File.separator +
						"etc" + File.separator +
						"glidein_service" + File.separator +
						"glidein.conf");
				if(globusFile.exists())
				{
					instance = new ServiceConfiguration(globusFile);
					return instance;
				}
			}
			
			// 5. If none of the files above are found, then fail
			throw new GlideinException("Unable to load service configuration");
		}
		
		return instance;
	}
	
	public static void main(String[] args)
	{
		try 
		{
			ServiceConfiguration config = ServiceConfiguration.getInstance();
			System.out.println(config.getProperty("condor.home"));
			System.out.println(config.getProperty("condor.bin"));
			System.out.println(config.getProperty("condor.config"));
		}
		catch(GlideinException e)
		{
			e.printStackTrace();
		}
	}
}