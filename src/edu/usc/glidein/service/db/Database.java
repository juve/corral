package edu.usc.glidein.service.db;

import edu.usc.glidein.GlideinConfiguration;

public abstract class Database
{
	public static final String IMPL = "glidein.db.impl";
	public static final String URL = "glidein.db.url";
	public static final String USER = "glidein.db.user";
	public static final String PASSWORD = "glidein.db.password";
	
	public static Database getDatabase() throws DatabaseException
	{
		String impl = null;
		try {
			// Get configuration
			GlideinConfiguration config = GlideinConfiguration.getInstance();
			impl = config.getProperty(Database.IMPL);
			
			// Load database
			Database database = (Database)Class.forName(impl).newInstance();
			database.initialize();
			return database;
		} catch (Exception e) {
			throw new DatabaseException("Unable to load database: "+impl,e);
		}
	}
	
	public void initialize() throws DatabaseException
	{
		/* Do nothing by default */
	}
	
	public abstract SiteDAO getSiteDAO();
	public abstract GlideinDAO getGlideinDAO();
}
