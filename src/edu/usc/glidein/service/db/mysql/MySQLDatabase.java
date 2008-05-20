package edu.usc.glidein.service.db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import edu.usc.glidein.GlideinConfiguration;
import edu.usc.glidein.GlideinException;
import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.SiteDAO;

public class MySQLDatabase extends Database
{
	public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	
	private String url;
	private String user;
	private String password;
	
	public MySQLDatabase() { }
	
	public void initialize() throws DatabaseException
	{
		// Get configuration
		try {
			GlideinConfiguration config = GlideinConfiguration.getInstance();
			url = config.getProperty(URL);
			user = config.getProperty(USER);
			password = config.getProperty(PASSWORD);
		} catch(GlideinException ge) {
			throw new DatabaseException("Unable to load database configuration",ge);
		}
		
		// Load database driver
		try {
			Class.forName(DB_DRIVER).newInstance();
		} catch(Exception e) {
			throw new DatabaseException("Unable to load MySQL driver",e);
		}
	}
	
	public SiteDAO getSiteDAO()
	{
		return new MySQLSiteDAO(this);
	}
	
	public GlideinDAO getGlideinDAO()
	{
		return new MySQLGlideinDAO(this);
	}
	
	public Connection getConnection() throws DatabaseException
	{
		Connection connection;
		try {
			connection = DriverManager.getConnection(url,user,password);
			connection.setAutoCommit(false);
			return connection;
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to connect to database",sqle);
		}
	}
}
