/*
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.usc.glidein.db.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.globus.wsrf.jndi.Initializable;

import edu.usc.glidein.db.Database;
import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.GlideinDAO;
import edu.usc.glidein.db.JDBCUtil;
import edu.usc.glidein.db.SiteDAO;
import edu.usc.glidein.util.IOUtil;

public class SQLiteDatabase extends Database implements Initializable
{
	public static final Logger logger = Logger.getLogger(SQLiteDatabase.class);
	public static final String DB_DRIVER = "org.sqlite.JDBC";
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String TIME_ZONE = "UTC";
	
	private File databaseFile;
	private File schemaFile;
	private boolean autoInstall = false;
	private boolean initialized = false;
	
	
	public SQLiteDatabase() { }
	
	public void initialize() throws Exception
	{
		synchronized (this) {
			if (initialized)
				return;
			
			try {
				// Load database driver
				Class.forName(DB_DRIVER);
				
				// If we are allowing auto installation of the database tables
				if (isAutoInstall()) {
					
					// If tables aren't installed
					if (!tablesInstalled()) {
						logger.info("Installing database tables");
						
						// Create database dir
						createDatabaseDirectory();
						
						// Run script
						runDatabaseScript();
						
						// Check interface table again
						if (!tablesInstalled()) {
							throw new DatabaseException(
									"Unable to install tables");
						}
					}
				}
			} catch (Exception e) {
				logger.error("Unable to initialize SQLiteDatabase",e);
				throw e;
			}
			
			initialized = true;
		}
	}
	
	private void createDatabaseDirectory() throws DatabaseException
	{
		if (!databaseFile.exists()) {
			File dir = databaseFile.getParentFile();
			try {
				if (!dir.exists()) {
					if(!dir.mkdirs()) {
						throw new DatabaseException(
								"Unable to create database directory: "+
								dir.getAbsolutePath());
					}
				}
			} catch (Exception e) {
				throw new DatabaseException(
						"Unable to create database directory: "+
						dir.getAbsolutePath(),e);
			}
		}
	}
	
	private void runDatabaseScript() throws DatabaseException
	{
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			for (String s : getStatements(schemaFile)) {
				stmt.executeUpdate(s);
			}
			conn.commit();
		} catch (SQLException sqle) {
			throw new DatabaseException(
					"Unable to run database setup script",sqle);
		} catch (IOException ioe) {
			throw new DatabaseException(
					"Unable to read database setup script",ioe);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private boolean tablesInstalled() throws DatabaseException
	{
		// First, make sure that the database file exists
		if (!databaseFile.exists())
			return false;
		
		// Next, try to select some stuff from the database
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM sqlite_master WHERE name='interface' and type='table'");
			return rs.next();
		} catch (SQLException sqle) {
			throw new DatabaseException(
					"Unable to determine if tables are installed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private String[] getStatements(File script) throws IOException
	{
		String stmts = IOUtil.read(script);
		return stmts.split("[;]");
	}
	
	public String getDatabaseFile()
	{
		return databaseFile.getAbsolutePath();
	}
	
	public void setDatabaseFile(String databaseFile)
	{
		this.databaseFile = new File(databaseFile);
	}

	public String getSchemaFile()
	{
		return schemaFile.getAbsolutePath();
	}

	public void setSchemaFile(String schemaFile)
	{
		this.schemaFile = new File(schemaFile);
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
		return new SQLiteSiteDAO(this);
	}
	
	public GlideinDAO getGlideinDAO()
	{
		return new SQLiteGlideinDAO(this);
	}
	
	public String getURL()
	{
		return "jdbc:sqlite:"+getDatabaseFile();
	}
	
	public Connection getConnection() throws DatabaseException
	{
		try {
			Connection conn = DriverManager.getConnection(getURL());
			conn.setAutoCommit(false);
			return conn;
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to connect to database: "+
					sqle.getMessage(),sqle);
		}
	}
	
	public Calendar parseDate(String date) throws DatabaseException
	{
		if (date == null)
			return null;
		if ("".equals(date))
			return null;
		
		// SQLite stores dates as strings in this format in UTC
		final SimpleDateFormat dateParser = 
			new SimpleDateFormat(DATE_FORMAT);
		dateParser.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
		
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateParser.parse(date));
			return cal;
		} catch (ParseException pe) {
			throw new DatabaseException("Unable to parse date: "+date,pe);
		}
	}
}
