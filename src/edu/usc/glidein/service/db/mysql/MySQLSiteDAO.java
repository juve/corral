package edu.usc.glidein.service.db.mysql;

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
import edu.usc.glidein.stubs.types.SiteStatus;
import edu.usc.glidein.stubs.types.SiteStatusCode;

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
			stmt = connection.prepareStatement("INSERT INTO site (name, installPath, localPath, submitPath, condorPackage, condorVersion, status, statusMessage) VALUES (?,?,?,?,?,?,?,?)");
			int i = 1;
			stmt.setString(i++, site.getName());
			stmt.setString(i++, site.getInstallPath());
			stmt.setString(i++, site.getLocalPath());
			stmt.setString(i++, site.getSubmitPath());
			stmt.setString(i++, site.getCondorPackage());
			stmt.setString(i++, site.getCondorVersion());
			stmt.setString(i++, SiteStatusCode.NEW.toString());
			stmt.setString(i++, SiteStatusCode.NEW.toString());
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
			stmt = connection.prepareStatement("INSERT INTO execution_service (site, function, serviceContact, serviceType, project, queue, proxy) VALUES (?,?,?,?,?,?,?)");
			int i = 1;
			stmt.setInt(i++, siteId);
			stmt.setString(i++, function.toString());
			stmt.setString(i++, service.getServiceContact());
			stmt.setString(i++, service.getServiceType().toString());
			stmt.setString(i++, service.getProject());
			stmt.setString(i++, service.getQueue());
			stmt.setString(i++, service.getProxy());
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
			stmt = connection.prepareStatement("INSERT INTO site_environment (site, variable, value) VALUES (?,?,?)");
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
				site = new Site();
				site.setId(siteId);
				site.setName(rs.getString("name"));
				site.setInstallPath(rs.getString("installPath"));
				site.setLocalPath(rs.getString("localPath"));
				site.setSubmitPath(rs.getString("submitPath"));
				site.setCondorPackage(rs.getString("condorPackage"));
				site.setCondorVersion(rs.getString("condorVersion"));
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
				service.setProxy(rs.getString("proxy"));
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
			stmt = connection.prepareStatement("SELECT * FROM site_environment WHERE site=?");
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
		Connection conn = null;
		int siteId = site.getId();
		try {
			conn = database.getConnection();
			updateSite(conn, site);
			updateExecutionService(conn, siteId, site.getStagingService(), ServiceFunction.STAGING);
			updateExecutionService(conn, siteId, site.getGlideinService(), ServiceFunction.GLIDEIN);
			updateEnvironment(conn, siteId, site.getEnvironment());
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to store site: commit failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private void updateSite(Connection connection, Site site) throws DatabaseException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("UPDATE site SET name=?, installPath=?, localPath=?, submitPath=?, condorPackage=?, condorVersion=? WHERE id=?");
			int i = 1;
			stmt.setString(i++, site.getName());
			stmt.setString(i++, site.getInstallPath());
			stmt.setString(i++, site.getLocalPath());
			stmt.setString(i++, site.getSubmitPath());
			stmt.setString(i++, site.getCondorPackage());
			stmt.setString(i++, site.getCondorVersion());
			stmt.setInt(i++, site.getId());
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to store site: wrong number of db updates");
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to store site: update failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);	
		}
	}

	private void updateExecutionService(Connection connection, int siteId, ExecutionService service, ServiceFunction function)
	throws DatabaseException
	{
		if (service == null) return;
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement("UPDATE execution_service SET serviceContact=?, serviceType=?, project=?, queue=?, proxy=? WHERE site=? AND function=?");
			int i = 1;
			stmt.setString(i++, service.getServiceContact());
			stmt.setString(i++, service.getServiceType().toString());
			stmt.setString(i++, service.getProject());
			stmt.setString(i++, service.getQueue());
			stmt.setString(i++, service.getProxy());
			stmt.setInt(i++, siteId);
			stmt.setString(i++, function.toString());
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to store execution service: wrong number of db updates");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to store execution service: update failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	private void updateEnvironment(Connection connection, int siteId, EnvironmentVariable[] env)
	throws DatabaseException
	{
		deleteEnvironment(connection,siteId);
		createEnvironment(connection,siteId,env);
	}
	
	private void deleteEnvironment(Connection connection, int siteId) throws DatabaseException
	{
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement("DELETE FROM site_environment WHERE site=?");
			stmt.setInt(1, siteId);
			stmt.executeUpdate();
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to store site environment: delete failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
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
	
	public SiteStatus getStatus(int siteId) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement("SELECT status, statusMessage FROM site WHERE id=?");
			stmt.setInt(1, siteId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				SiteStatus status = new SiteStatus();
				status.setCode(SiteStatusCode.fromString(rs.getString("status")));
				status.setMessage(rs.getString("statusMessage"));
				return status;
			} else {
				throw new DatabaseException("Unable to get site status: no record found");
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to get site status: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public void updateStatus(int siteId, SiteStatus status) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE site SET status=?, statusMessage=? WHERE id=?");
			stmt.setString(1, status.getCode().toString());
			stmt.setString(2, status.getMessage());
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
}
