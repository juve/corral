package edu.usc.glidein.service.db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.globus.wsrf.jndi.Initializable;

import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.SiteDAO;

public class MySQLDatabase extends Database implements Initializable
{
	public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	
	private String url;
	private String user;
	private String password;
	
	public MySQLDatabase() { }
	
	public MySQLDatabase(String url, String user, String password)
	{
		this.url = url;
		this.user = user;
		this.password = password;
	}
	
	public void initialize() throws Exception
	{
		// Load database driver
		Class.forName(DB_DRIVER).newInstance();
	}
	
	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
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
