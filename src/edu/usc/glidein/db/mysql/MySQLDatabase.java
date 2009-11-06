/*
 *  Copyright 2007-2009 University Of Southern California
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
package edu.usc.glidein.db.mysql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import edu.usc.corral.config.Initializable;
import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.GlideinDAO;
import edu.usc.glidein.db.JDBCUtil;
import edu.usc.glidein.db.SiteDAO;
import edu.usc.glidein.db.sql.SQLDatabase;
import edu.usc.glidein.util.IOUtil;

public class MySQLDatabase extends SQLDatabase implements Initializable {
	public static final Logger logger = Logger.getLogger(MySQLDatabase.class);
	public static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	
	private String url;
	private String user;
	private String password;
	private File schemaFile;
	private boolean autoInstall;
	
	public MySQLDatabase() { }
	
	public void initialize() throws Exception {
		try {
			// Load database driver
			Class.forName(DB_DRIVER);
			
			// If we are allowing auto installation of the database tables
			if (isAutoInstall()) {

				// If tables aren't installed
				if (!tablesInstalled()) {
					logger.info("Installing database tables");

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
			logger.error("Unable to initialize MySQLDatabase",e);
			throw e;
		}
	}
	
	private void runDatabaseScript() throws DatabaseException {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			for (String s : getStatements(schemaFile)) {
				stmt.addBatch(s);
			}
			stmt.executeBatch();
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
	
	private boolean tablesInstalled() throws DatabaseException {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SHOW TABLES LIKE 'interface'");
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
	
	private String[] getStatements(File script) throws IOException {
		String stmts = IOUtil.read(script);
		return stmts.split("[;]");
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSchemaFile() {
		return schemaFile.getAbsolutePath();
	}

	public void setSchemaFile(String schemaFile) {
		this.schemaFile = new File(schemaFile);
		if (!this.schemaFile.isAbsolute()) {
			this.schemaFile = new File(System.getProperty("GLOBUS_LOCATION"),schemaFile);
		}
	}

	public boolean isAutoInstall() {
		return autoInstall;
	}

	public void setAutoInstall(boolean autoInstall) {
		this.autoInstall = autoInstall;
	}

	public SiteDAO getSiteDAO() {
		return new MySQLSiteDAO(this);
	}
	
	public GlideinDAO getGlideinDAO() {
		return new MySQLGlideinDAO(this);
	}
	
	public Connection getConnection() throws DatabaseException {
		try {
			Connection conn = DriverManager.getConnection(url,user,password);
			conn.setAutoCommit(false);
			return conn;
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to connect to database: "+
					sqle.getMessage(),sqle);
		}
	}
}
