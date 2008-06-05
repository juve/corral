package edu.usc.glidein.service.db.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.TimeZone;

import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.JDBCUtil;
import edu.usc.glidein.service.db.SiteDAO;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.ExecutionService;
import edu.usc.glidein.stubs.types.ServiceType;
import edu.usc.glidein.stubs.types.Site;
import edu.usc.glidein.stubs.types.SiteStatus;

public class MySQLSiteDAO implements SiteDAO
{
	private enum ServiceFunction {
		STAGING,
		GLIDEIN
	};
	
	private MySQLDatabase database = null;
	
	public MySQLSiteDAO(MySQLDatabase db)
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
			stmt = connection.prepareStatement("INSERT INTO site (name, installPath, localPath, condorPackage, condorVersion, status, statusMessage, submitted, lastUpdate) VALUES (?,?,?,?,?,?,?,NOW(),NOW())");
			int i = 1;
			stmt.setString(i++, site.getName());
			stmt.setString(i++, site.getInstallPath());
			stmt.setString(i++, site.getLocalPath());
			stmt.setString(i++, site.getCondorPackage());
			stmt.setString(i++, site.getCondorVersion());
			stmt.setString(i++, site.getStatus().toString());
			stmt.setString(i++, site.getStatusMessage());
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
		updateStatus(site.getId(),site.getStatus(),site.getStatusMessage());
	}
	
	public void delete(int siteId) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = database.getConnection();
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
	
	public void updateStatus(int siteId, SiteStatus status, String statusMessage)
	throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = database.getConnection();
			stmt = conn.prepareStatement("UPDATE site SET status=?, statusMessage=?, lastUpdate=NOW() WHERE id=?");
			stmt.setString(1, status.toString());
			stmt.setString(2, statusMessage);
			stmt.setInt(3, siteId);
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to update site status: wrong number of db updates");
			}
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to update site status: update failed",sqle);
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
			
			site.setStatus(SiteStatus.fromString(rs.getString("status")));
			site.setStatusMessage(rs.getString("statusMessage"));
			
			Calendar submitted = Calendar.getInstance(TimeZone.getDefault());
			Timestamp submit = rs.getTimestamp("submitted",submitted);
			submitted.setTime(submit);
			site.setSubmitted(submitted);
			
			Calendar lastUpdate = Calendar.getInstance(TimeZone.getDefault());
			Timestamp last = rs.getTimestamp("lastUpdate",lastUpdate);
			lastUpdate.setTime(last);
			site.setLastUpdate(lastUpdate);
			
			return site;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to create Site object",sqle);
		}
	}
}
