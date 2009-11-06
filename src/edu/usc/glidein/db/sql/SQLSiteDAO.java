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
package edu.usc.glidein.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.usc.corral.types.EnvironmentVariable;
import edu.usc.corral.types.ExecutionService;
import edu.usc.corral.types.Site;
import edu.usc.corral.types.SiteState;
import edu.usc.corral.types.ServiceType;
import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.JDBCUtil;
import edu.usc.glidein.db.SiteDAO;

public class SQLSiteDAO implements SiteDAO {
	private enum ServiceFunction {
		STAGING,
		GLIDEIN
	};
	
	private SQLDatabase database = null;
	
	public SQLSiteDAO(SQLDatabase db) {
		this.database = db;
	}
	
	public Connection getConnection() throws DatabaseException {
		return database.getConnection();
	}

	public int create(Site site) throws DatabaseException {
		Connection conn = null;
		int id = 0;
		try {
			conn = getConnection();
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
	
	private int createSite(Connection connection, Site site) throws DatabaseException {
		int id = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("INSERT INTO site (name, installPath, localPath, condorPackage, condorVersion, state, shortMessage, longMessage, created, lastUpdate, subject, localUsername) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
			int i = 1;
			stmt.setString(i++, site.getName());
			stmt.setString(i++, site.getInstallPath());
			stmt.setString(i++, site.getLocalPath());
			stmt.setString(i++, site.getCondorPackage());
			stmt.setString(i++, site.getCondorVersion());
			stmt.setString(i++, site.getState().toString());
			stmt.setString(i++, site.getShortMessage());
			stmt.setString(i++, site.getLongMessage());
			Date created = site.getCreated();
			if (created == null) {
				stmt.setTimestamp(i++, null);
			} else {
				stmt.setTimestamp(i++, new Timestamp(created.getTime()));
			}
			Date lastUpdate = site.getLastUpdate();
			if (lastUpdate == null) {
				stmt.setTimestamp(i++, null);
			} else {
				stmt.setTimestamp(i++, new Timestamp(lastUpdate.getTime()));
			}
			stmt.setString(i++, site.getSubject());
			stmt.setString(i++, site.getLocalUsername());
			
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
	
	private void createExecutionService(Connection connection, int siteId, ExecutionService service, ServiceFunction function) throws DatabaseException {
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

	private void createEnvironment(Connection connection, int siteId, List<EnvironmentVariable> env) throws DatabaseException {
		if (env == null || env.size() == 0) return;
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
	
	public Site load(int siteId) throws DatabaseException {
		Connection conn = null;
		try {
			conn = getConnection();
			Site site = getSite(conn, siteId);
			ExecutionService stagingService = getExecutionService(conn, siteId, ServiceFunction.STAGING);
			ExecutionService glideinService = getExecutionService(conn, siteId, ServiceFunction.GLIDEIN);
			List<EnvironmentVariable> env = getEnvironment(conn, siteId);
			site.setStagingService(stagingService);
			site.setGlideinService(glideinService);
			site.setEnvironment(env);
			return site;
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private Site getSite(Connection connection, int siteId) throws DatabaseException {
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
	
	private ExecutionService getExecutionService(Connection connection, int siteId, ServiceFunction function)  throws DatabaseException {
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
				service.setServiceType(ServiceType.valueOf(rs.getString("serviceType")));
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
	
	private List<EnvironmentVariable> getEnvironment(Connection connection, int siteId) throws DatabaseException {
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
				EnvironmentVariable ev = new EnvironmentVariable();
				ev.setValue(value);
				ev.setVariable(variable);
				env.add(ev);
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load site environment: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return env;
	}
	
	public void delete(int siteId) throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			/* Cascaded deletes should take care of the other tables */
			stmt = conn.prepareStatement("DELETE FROM site WHERE id=?");
			stmt.setInt(1, siteId);
			if (stmt.executeUpdate() != 1) {
				throw new DatabaseException("Unable to delete site: wrong number of db updates");
			}
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to delete site: delete failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public void updateState(int siteId, SiteState state, String shortMessage, String longMessage, Date time) throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("UPDATE site SET state=?, shortMessage=?, longMessage=?, lastUpdate=? WHERE id=?");
			int i = 1;
			stmt.setString(i++, state.toString());
			stmt.setString(i++, shortMessage);
			stmt.setString(i++, longMessage);
			stmt.setTimestamp(i++, new Timestamp(time.getTime()));
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
	
	public int[] listIds() throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		LinkedList<Integer> results = new LinkedList<Integer>();
		try {
			conn = getConnection();
			stmt = conn.prepareStatement("SELECT id FROM site");
			rs = stmt.executeQuery();
			while (rs.next()) {
				results.add(rs.getInt("id"));
			}
			
			int[] ids = new int[results.size()];
			int i = 0;
			for (Integer id : results) {
				ids[i++] = id;
			}
			return ids;
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to get site ids: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public List<Site> list(boolean longFormat, String user, boolean allUsers) throws DatabaseException {
		Connection conn = null;
		try {
			conn = getConnection();
			List<Site> sites = getSites(conn, user, allUsers);
			if (longFormat) {
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
	
	private List<Site> getSites(Connection connection, String user, boolean allUsers) throws DatabaseException {
		LinkedList<Site> sites = new LinkedList<Site>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT * FROM site"+(allUsers?"":" WHERE localUsername=?"));
			if (!allUsers) {
				stmt.setString(1, user);
			}
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
		return sites;
	}
	
	private Site newSite(ResultSet rs) throws DatabaseException {
		try {
			Site site = new Site();
			site.setId(rs.getInt("id"));
			site.setName(rs.getString("name"));
			site.setInstallPath(rs.getString("installPath"));
			site.setLocalPath(rs.getString("localPath"));
			site.setCondorPackage(rs.getString("condorPackage"));
			site.setCondorVersion(rs.getString("condorVersion"));
			site.setState(SiteState.valueOf(rs.getString("state")));
			site.setShortMessage(rs.getString("shortMessage"));
			site.setLongMessage(rs.getString("longMessage"));
			site.setCreated(rs.getTimestamp("created"));
			site.setLastUpdate(rs.getTimestamp("lastUpdate"));
			site.setSubject(rs.getString("subject"));
			site.setLocalUsername(rs.getString("localUsername"));
			
			return site;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to create Site object",sqle);
		}
	}
	
	public boolean hasGlideins(int siteId) throws DatabaseException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
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
	
	public int[] getGlideinIds(int siteId) throws DatabaseException {
		LinkedList<Integer> result = new LinkedList<Integer>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
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
