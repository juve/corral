package edu.usc.glidein.service.db.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import edu.usc.glidein.service.db.DatabaseException;
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.service.db.JDBCUtil;
import edu.usc.glidein.stubs.types.EnvironmentVariable;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinStatus;
import edu.usc.glidein.stubs.types.GlideinStatusCode;

public class MySQLGlideinDAO implements GlideinDAO
{
	private MySQLDatabase database = null;
	
	public MySQLGlideinDAO(MySQLDatabase db)
	{
		this.database = db;
	}

	public int create(Glidein glidein) throws DatabaseException
	{
		Connection conn = null;
		int id = 0;
		try {
			conn = database.getConnection();
			id = createGlidein(conn, glidein);
			createEnvironment(conn, id, glidein.getEnvironment());
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to create glidein: commit failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
		return id;
	}
	
	private int createGlidein(Connection connection, Glidein glidein) throws DatabaseException
	{
		int id = 0;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("INSERT INTO glidein (site, count, hostCount, wallTime, numCpus, configBase64, gcbBroker, idleTime, debug, status, statusMessage) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
			int i = 1;
			stmt.setInt(i++, glidein.getSiteId());
			stmt.setInt(i++, glidein.getCount());
			stmt.setInt(i++, glidein.getHostCount());
			stmt.setInt(i++, glidein.getWallTime());
			stmt.setInt(i++, glidein.getNumCpus());
			stmt.setString(i++, glidein.getConfigBase64());
			stmt.setString(i++, glidein.getGcbBroker());
			stmt.setInt(i++, glidein.getIdleTime());
			stmt.setString(i++, glidein.getDebug());
			stmt.setString(i++, GlideinStatusCode.NEW.toString());
			stmt.setString(i++, GlideinStatusCode.NEW.toString());
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to create glidein: wrong number of db updates");
			}
			
			// Get new ID
			rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
				glidein.setId(id);
			} else {
				throw new DatabaseException("Unable to get generated glidein id");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to create glidein: insert failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return id;
	}

	private void createEnvironment(Connection connection, int glideinId, EnvironmentVariable[] env)
	throws DatabaseException
	{
		if (env == null || env.length == 0) return;
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement("INSERT INTO glidein_environment (glidein, variable, value) VALUES (?,?,?)");
			for (EnvironmentVariable var : env) {
				stmt.setInt(1, glideinId);
				stmt.setString(2, var.getVariable());
				stmt.setString(3, var.getValue());
				stmt.addBatch();
			}
			int[] updates = stmt.executeBatch();
			for (int update : updates) {
				if (update!=1) {
					throw new DatabaseException("Unable to create glidein environment: wrong number of db updates");
				}
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to create glidein environment: insert failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}
		

	public Glidein load(int glideinId) throws DatabaseException
	{
		Connection conn = null;
		try {
			conn = database.getConnection();
			Glidein glidein = getGlidein(conn, glideinId);
			EnvironmentVariable[] env = getEnvironment(conn, glideinId);
			glidein.setEnvironment(env);
			return glidein;
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private Glidein getGlidein(Connection connection, int glideinId) throws DatabaseException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT * FROM glidein WHERE id=?");
			stmt.setInt(1, glideinId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				Glidein glidein = new Glidein();
				glidein.setId(glideinId);
				glidein.setSiteId(rs.getInt("site"));
				glidein.setCount(rs.getInt("count"));
				glidein.setHostCount(rs.getInt("hostCount"));
				glidein.setWallTime(rs.getInt("wallTime"));
				glidein.setNumCpus(rs.getInt("numCpus"));
				glidein.setConfigBase64(rs.getString("configBase64"));
				glidein.setGcbBroker(rs.getString("gcbBroker"));
				glidein.setIdleTime(rs.getInt("idleTime"));
				glidein.setDebug(rs.getString("debug"));
				return glidein;
			} else {
				throw new DatabaseException("Glidein "+glideinId+" not found");
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load glidein: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	private EnvironmentVariable[] getEnvironment(Connection connection, int glideinId) throws DatabaseException
	{
		LinkedList<EnvironmentVariable> env = new LinkedList<EnvironmentVariable>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT * FROM glidein_environment WHERE glidein=?");
			stmt.setInt(1, glideinId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				String variable = rs.getString("variable");
				String value = rs.getString("value");
				env.add(new EnvironmentVariable(value,variable));
			}
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load glidein environment: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
		return (EnvironmentVariable[])env.toArray(new EnvironmentVariable[0]);
	}
	
	public void store(Glidein glidein) throws DatabaseException
	{
		Connection conn = null;
		int glideinId = glidein.getId();
		try {
			conn = database.getConnection();
			updateGlidein(conn, glidein);
			updateEnvironment(conn, glideinId, glidein.getEnvironment());
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

	private void updateGlidein(Connection connection, Glidein glidein) throws DatabaseException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("UPDATE glidein SET site=?, count=?, hostCount=?, wallTime=?, numCpus=?, configBase64=?, gcbBroker=?, idleTime=?, debug=? WHERE id=?");
			int i = 1;
			stmt.setInt(i++, glidein.getSiteId());
			stmt.setInt(i++, glidein.getCount());
			stmt.setInt(i++, glidein.getHostCount());
			stmt.setInt(i++, glidein.getWallTime());
			stmt.setInt(i++, glidein.getNumCpus());
			stmt.setString(i++, glidein.getConfigBase64());
			stmt.setString(i++, glidein.getGcbBroker());
			stmt.setInt(i++, glidein.getIdleTime());
			stmt.setString(i++, glidein.getDebug());
			stmt.setInt(i++, glidein.getId());
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
	
	private void updateEnvironment(Connection connection, int glideinId, EnvironmentVariable[] env)
	throws DatabaseException
	{
		deleteEnvironment(connection, glideinId);
		createEnvironment(connection, glideinId, env);
	}
	
	public void delete(int glideinId) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = database.getConnection();
			/* Cascaded deletes should take care of the other tables */
			stmt = conn.prepareStatement("DELETE FROM glidein WHERE id=?");
			stmt.setInt(1, glideinId);
			if (stmt.executeUpdate() != 1) {
				throw new DatabaseException("Unable to delete glidein: wrong number of db updates");
			}
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to delete glidein: delete failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private void deleteEnvironment(Connection connection, int glideinId) throws DatabaseException
	{
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement("DELETE FROM glidein_environment WHERE glidein=?");
			stmt.setInt(1, glideinId);
			stmt.executeUpdate();
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to store glidein environment: delete failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	public GlideinStatus getStatus(int glideinId) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement("SELECT status, statusMessage FROM glidein WHERE id=?");
			stmt.setInt(1, glideinId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				GlideinStatus status = new GlideinStatus();
				status.setCode(GlideinStatusCode.fromString(rs.getString("status")));
				status.setMessage(rs.getString("statusMessage"));
				return status;
			} else {
				throw new DatabaseException("Unable to get glidein status: no record found");
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to get glidein status: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public void updateStatus(int glideinId, GlideinStatus status)
	throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE glidein SET status=?, statusMessage=? WHERE id=?");
			stmt.setString(1, status.getCode().toString());
			stmt.setString(2, status.getMessage());
			stmt.setInt(3, glideinId);
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to update glidein status: wrong number of db updates");
			}
			conn.commit();
		} catch (DatabaseException dbe) {
			JDBCUtil.rollbackQuietly(conn);
			throw dbe;
		} catch (SQLException sqle) {
			JDBCUtil.rollbackQuietly(conn);
			throw new DatabaseException("Unable to update glidein status: update failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
		
	}
}
