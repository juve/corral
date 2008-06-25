package edu.usc.glidein.service.db.mysql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.globus.wsrf.jndi.Initializable;

import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.service.db.Database;
import edu.usc.glidein.service.db.JDBCUtil;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.util.IOUtil;

public class MySQLDatabase extends Database implements Initializable
{
	public static final Logger logger = Logger.getLogger(MySQLDatabase.class);
	public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	
	private String url;
	private String user;
	private String password;
	private String schemaPath;
	private boolean autoInstall;
	
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
		
		// If we are allowing auto installation of the database tables
		if (isAutoInstall()) {
			
			// If tables aren't installed
			if (!tablesInstalled()) {
				logger.info("Installing database tables");
				
				// Run script
				runDatabaseScript();
				
				// Check interface table again
				if (!tablesInstalled()) {
					throw new DatabaseException("Unable to install tables");
				}
			}
		}
	}
	
	private void runDatabaseScript() throws DatabaseException
	{
		File script = new File(getSchemaPath());
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			for (String s : getStatements(script)) {
				stmt.addBatch(s);
			}
			stmt.executeBatch();
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to run database setup script");
		} catch (IOException ioe) {
			throw new DatabaseException("Unable to read database setup script");
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private boolean tablesInstalled() throws DatabaseException
	{
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = _getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SHOW TABLES LIKE 'interface'");
			return rs.next();
		} catch (SQLException sqle) {
			throw new DatabaseException(
					"Unable to determine if tables are installed");
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private String[] getStatements(File script) throws IOException
	{
		String stmts = IOUtil.read(script);
		return stmts.split("[;]");
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

	public String getSchemaPath()
	{
		return schemaPath;
	}

	public void setSchemaPath(String schemaPath)
	{
		this.schemaPath = schemaPath;
	}

	public boolean isAutoInstall()
	{
		return autoInstall;
	}

	public void setAutoInstall(boolean autoInstall)
	{
		this.autoInstall = autoInstall;
	}

	public SiteDAO getSiteDAO()
	{
		return new MySQLSiteDAO(this);
	}
	
	public GlideinDAO getGlideinDAO()
	{
		return new MySQLGlideinDAO(this);
	}
	
	private Connection _getConnection() throws SQLException 
	{
		Connection conn = DriverManager.getConnection(url,user,password);
		conn.setAutoCommit(false);
		return conn;
	}
	
	public Connection getConnection() throws DatabaseException
	{
		try {
			return _getConnection();
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to connect to database",sqle);
		}
	}
}
