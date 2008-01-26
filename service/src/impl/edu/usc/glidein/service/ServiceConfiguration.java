package edu.usc.glidein.service;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;

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
	 * @throws ServiceException If unable to read the configuration file
	 */
	private ServiceConfiguration(File configFile)
	throws ServiceException
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
			throw new ServiceException(
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
	 * @throws ServiceException If the property does not exist
	 */
	public String getProperty(String key)
	throws ServiceException
	{
		String prop = properties.getProperty(key);
		if(prop==null) 
			throw new ServiceException(
				"Property "+key+" not found in configuration file "+
				configFile.getAbsolutePath());
		return prop;
	}
	
	/**
	 * Get a property by name with a default value
	 * @param key The name of the property
	 * @param def The default value if the property does not exist
	 * @return The value of the property or default
	 */
	public String getProperty(String key, String def)
	{
		String prop = properties.getProperty(key);
		if(prop==null) prop = def;
		return prop;
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
	 * @throws ServiceException if unable to load the configuration
	 */
	public static synchronized ServiceConfiguration getInstance()
	throws ServiceException
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
						home + File.pathSeparator +
						".glidein" + File.pathSeparator +
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
						globus + File.pathSeparator +
						"etc" + File.pathSeparator +
						"glidein_service" + File.pathSeparator +
						"glidein.conf");
				if(globusFile.exists())
				{
					instance = new ServiceConfiguration(globusFile);
					return instance;
				}
			}
			
			// 5. If none of the files above are found, then fail
			throw new ServiceException("Unable to load service configuration");
		}
		
		return instance;
	}
}