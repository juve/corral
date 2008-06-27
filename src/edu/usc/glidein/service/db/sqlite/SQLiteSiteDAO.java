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
package edu.usc.glidein.service.db.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.JDBCUtil;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteState;

public class SQLiteSiteDAO implements SiteDAO
{
	private enum ServiceFunction {
		STAGING,
		GLIDEIN
	};
	
	private SQLiteDatabase database = null;
	
	public SQLiteSiteDAO(SQLiteDatabase db)
	{
		this.database = db;
	}

	public int create(Site site) throws DatabaseException 
	{
		Connection conn = null;
		int id = 0;
		try {
			conn = database.getConnection();
			id = createSite(conn, site);
			createExecutionService(conn, id, site.getStagingService(), ServiceFunction.STAGING);
			createExecutionService(conn, id, site.getGlideinService(), ServiceFunction.GLIDEIN);
			createEnvironment(conn, id, site.getEnvironment());
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to create site: commit failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
		return id;
	}
	
	private int createSite(Connection connection, Site site) throws DatabaseException
	{
		int id = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("INSERT INTO site (name, installPath, localPath, condorPackage, condorVersion, state, shortMessage, longMessage, submitted, lastUpdate) VALUES (?,?,?,?,?,?,?,?,datetime('now'),datetime('now'))");
			int i = 1;
			stmt.setString(i++, site.getName());
			stmt.setString(i++, site.getInstallPath());
			stmt.setString(i++, site.getLocalPath());
			stmt.setString(i++, site.getCondorPackage());
			stmt.setString(i++, site.getCondorVersion());
			stmt.setString(i++, site.getState().toString());
			stmt.setString(i++, site.getShortMessage());
			stmt.setString(i++, site.getLongMessage());
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to create site: wrong number of db updates");
			}
			
			// Get new ID
			rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
				site.setId(id);
			} else {
				throw new DatabaseException("Unable to get generated site id");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to create site: insert failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return id;
	}
	
	private void createExecutionService(Connection connection, int siteId, ExecutionService service, ServiceFunction function)
	throws DatabaseException
	{
		if (service == null) return;
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement("INSERT INTO execution_service (site, function, serviceContact, serviceType, project, queue) VALUES (?,?,?,?,?,?)");
			int i = 1;
			stmt.setInt(i++, siteId);
			stmt.setString(i++, function.toString());
			stmt.setString(i++, service.getServiceContact());
			stmt.setString(i++, service.getServiceType().toString());
			stmt.setString(i++, service.getProject());
			stmt.setString(i++, service.getQueue());
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to create execution service: wrong number of db updates");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to create execution service: insert failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}

	private void createEnvironment(Connection connection, int siteId, EnvironmentVariable[] env)
	throws DatabaseException
	{
		if (env == null || env.length == 0) return;
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement("INSERT INTO environment (site, variable, value) VALUES (?,?,?)");
			for (EnvironmentVariable var : env) {
				stmt.setInt(1, siteId);
				stmt.setString(2, var.getVariable());
				stmt.setString(3, var.getValue());
				stmt.addBatch();
			}
			int[] updates = stmt.executeBatch();
			for (int update : updates) {
				if (update!=1) {
					throw new DatabaseException("Unable to create site environment: wrong number of db updates");
				}
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to create site environment: insert failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	public Site load(int siteId) throws DatabaseException
	{
		Connection conn = null;
		try {
			conn = database.getConnection();
			Site site = getSite(conn, siteId);
			ExecutionService stagingService = getExecutionService(conn, siteId, ServiceFunction.STAGING);
			ExecutionService glideinService = getExecutionService(conn, siteId, ServiceFunction.GLIDEIN);
			EnvironmentVariable[] env = getEnvironment(conn, siteId);
			site.setStagingService(stagingService);
			site.setGlideinService(glideinService);
			site.setEnvironment(env);
			return site;
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private Site getSite(Connection connection, int siteId) throws DatabaseException
	{
		Site site = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT * FROM site WHERE id=?");
			stmt.setInt(1, siteId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				site = newSite(rs);
			} else {
				throw new DatabaseException("Site "+siteId+" not found");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load site: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return site;
	}
	
	private ExecutionService getExecutionService(Connection connection, int siteId, ServiceFunction function) 
	throws DatabaseException
	{
		ExecutionService service = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT * FROM execution_service WHERE site=? AND function=?");
			stmt.setInt(1, siteId);
			stmt.setString(2, function.toString());
			rs = stmt.executeQuery();
			if (rs.next()) {
				service = new ExecutionService();
				service.setServiceContact(rs.getString("serviceContact"));
				service.setServiceType(ServiceType.fromString(rs.getString("serviceType")));
				service.setProject(rs.getString("project"));
				service.setQueue(rs.getString("queue"));
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load execution service: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return service;
	}
	
	private EnvironmentVariable[] getEnvironment(Connection connection, int siteId)
	throws DatabaseException
	{
		LinkedList<EnvironmentVariable> env = new LinkedList<EnvironmentVariable>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT * FROM environment WHERE site=?");
			stmt.setInt(1, siteId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				String variable = rs.getString("variable");
				String value = rs.getString("value");
				env.add(new EnvironmentVariable(value,variable));
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load site environment: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return (EnvironmentVariable[])env.toArray(new EnvironmentVariable[0]);
	}

	public void store(Site site) throws DatabaseException 
	{
		updateState(site.getId(),site.getState(),site.getShortMessage(),site.getLongMessage());
	}
	
	public void delete(int siteId) throws DatabaseException
	{
		Connection conn = null;
		try {
			conn = database.getConnection();
			deleteEnvironment(conn, siteId);
			deleteExecutionServices(conn, siteId);
			deleteSite(conn,siteId);
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to delete site: delete failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private void deleteSite(Connection conn, int siteId) throws DatabaseException
	{
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("DELETE FROM site WHERE id=?");
			stmt.setInt(1, siteId);
			if (stmt.executeUpdate() != 1) {
				throw new DatabaseException("Unable to delete site: wrong number of db updates");
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to delete site",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	private void deleteEnvironment(Connection conn, int siteId) throws DatabaseException
	{
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("DELETE FROM environment WHERE site=?");
			stmt.setInt(1, siteId);
			stmt.executeUpdate();
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to delete environment",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	private void deleteExecutionServices(Connection conn, int siteId) throws DatabaseException
	{
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("DELETE FROM execution_service WHERE site=?");
			stmt.setInt(1, siteId);
			stmt.executeUpdate();
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to delete execution services",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	public void updateState(int siteId, SiteState state, String shortMessage, String longMessage)
	throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = database.getConnection();
			stmt = conn.prepareStatement("UPDATE site SET state=?, shortMessage=?, longMessage=?, lastUpdate=datetime('now') WHERE id=?");
			int i = 1;
			stmt.setString(i++, state.toString());
			stmt.setString(i++, shortMessage);
			stmt.setString(i++, longMessage);
			stmt.setInt(i++, siteId);
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to update site status: wrong number of db updates");
			}
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to update site state: update failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public Site[] list(boolean full) throws DatabaseException
	{
		Connection conn = null;
		try {
			conn = database.getConnection();
			Site[] sites = getSites(conn);
			if (full) {
				for (Site site : sites) {
					int id = site.getId();
					site.setEnvironment(getEnvironment(conn, id));
					site.setStagingService(getExecutionService(
							conn,id,ServiceFunction.STAGING));
					site.setGlideinService(getExecutionService(
							conn,id,ServiceFunction.GLIDEIN));
				}
			}
			return sites;
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private Site[] getSites(Connection connection) throws DatabaseException
	{
		LinkedList<Site> sites = new LinkedList<Site>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT * FROM site");
			rs = stmt.executeQuery();
			while (rs.next()) {
				Site site = newSite(rs);
				sites.add(site);
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load site: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return sites.toArray(new Site[0]);
	}
	
	private Site newSite(ResultSet rs) throws DatabaseException
	{
		try {
			Site site = new Site();
			site.setId(rs.getInt("id"));
			site.setName(rs.getString("name"));
			site.setInstallPath(rs.getString("installPath"));
			site.setLocalPath(rs.getString("localPath"));
			site.setCondorPackage(rs.getString("condorPackage"));
			site.setCondorVersion(rs.getString("condorVersion"));
			
			site.setState(SiteState.fromString(rs.getString("state")));
			site.setShortMessage(rs.getString("shortMessage"));
			site.setLongMessage(rs.getString("longMessage"));
			
			String submit = rs.getString("submitted");
			site.setSubmitted(database.parseDate(submit));
			
			String last = rs.getString("lastUpdate");
			site.setLastUpdate(database.parseDate(last));
			
			return site;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to create Site object",sqle);
		}
	}
	
	public boolean hasGlideins(int siteId) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = database.getConnection();
			stmt = conn.prepareStatement("SELECT count(*) FROM glidein WHERE site=?");
			stmt.setInt(1, siteId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				return count>0;
			} else {
				throw new DatabaseException("Expected one result: got zero");
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to count glideins",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public int[] getGlideinIds(int siteId) throws DatabaseException
	{
		LinkedList<Integer> result = new LinkedList<Integer>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = database.getConnection();
			stmt = conn.prepareStatement("SELECT id FROM glidein WHERE site=?");
			stmt.setInt(1, siteId);
			rs = stmt.executeQuery();
			while (rs.next()) {
				result.add(rs.getInt(1));
			}
			int[] ids = new int[result.size()];
			int i = 0;
			for (int id : result) ids[i++] = id;
			return ids;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to get glidein ids",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
}
