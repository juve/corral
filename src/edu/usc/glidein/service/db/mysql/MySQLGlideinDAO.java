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
import edu.usc.glidein.service.db.GlideinDAO;
import edu.usc.glidein.service.db.JDBCUtil;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinStatus;

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
			stmt = connection.prepareStatement("INSERT INTO glidein (site, count, hostCount, wallTime, numCpus, condorConfigBase64, gcbBroker, idleTime, condorDebug, status, statusMessage, submitted, lastUpdate, condorHost) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),?)");
			int i = 1;
			stmt.setInt(i++, glidein.getSiteId());
			stmt.setInt(i++, glidein.getCount());
			stmt.setInt(i++, glidein.getHostCount());
			stmt.setInt(i++, glidein.getWallTime());
			stmt.setInt(i++, glidein.getNumCpus());
			stmt.setString(i++, glidein.getCondorConfigBase64());
			stmt.setString(i++, glidein.getGcbBroker());
			stmt.setInt(i++, glidein.getIdleTime());
			stmt.setString(i++, glidein.getCondorDebug());
			stmt.setString(i++, glidein.getStatus().toString());
			stmt.setString(i++, glidein.getStatusMessage());
			stmt.setString(i++, glidein.getCondorHost());
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

	public Glidein load(int glideinId) throws DatabaseException
	{
		Connection conn = null;
		try {
			conn = database.getConnection();
			return getGlidein(conn, glideinId);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private Glidein getGlidein(Connection connection, int glideinId) throws DatabaseException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = connection.prepareStatement("SELECT g.*, s.name siteName FROM glidein g, site s WHERE g.id=? AND g.site=s.id");
			stmt.setInt(1, glideinId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				return newGlidein(rs);
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
	
	public void store(Glidein glidein) throws DatabaseException
	{
		updateStatus(glidein.getId(), glidein.getStatus(), glidein.getStatusMessage());
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
	
	public void updateStatus(int glideinId, GlideinStatus status, String statusMessage)
	throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE glidein SET status=?, statusMessage=? WHERE id=?");
			stmt.setString(1, status.toString());
			stmt.setString(2, statusMessage);
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
	
	public Glidein[] list(boolean longFormat) throws DatabaseException 
	{
		Connection conn = null;
		try {
			conn = database.getConnection();
			return getGlideins(conn, longFormat);
		} finally {
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	private Glidein[] getGlideins(Connection connection, boolean longFormat) throws DatabaseException
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		LinkedList<Glidein> results = new LinkedList<Glidein>();
		try {
			// Join to get the site name
			stmt = connection.prepareStatement("SELECT g.*, s.name siteName FROM glidein g, site s WHERE g.site=s.id");
			rs = stmt.executeQuery();
			while (rs.next()) {
				results.add(newGlidein(rs));
			}
			return results.toArray(new Glidein[0]);
		} catch(SQLException sqle) {
			throw new DatabaseException("Unable to load glidein: select failed",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
		}
	}
	
	private Glidein newGlidein(ResultSet rs) throws DatabaseException
	{
		try {
			Glidein glidein = new Glidein();
			glidein.setId(rs.getInt("id"));
			glidein.setSiteId(rs.getInt("site"));
			glidein.setSiteName(rs.getString("siteName"));
			glidein.setCount(rs.getInt("count"));
			glidein.setHostCount(rs.getInt("hostCount"));
			glidein.setWallTime(rs.getInt("wallTime"));
			glidein.setNumCpus(rs.getInt("numCpus"));
			glidein.setCondorConfigBase64(rs.getString("condorConfigBase64"));
			glidein.setGcbBroker(rs.getString("gcbBroker"));
			glidein.setIdleTime(rs.getInt("idleTime"));
			glidein.setCondorDebug(rs.getString("condorDebug"));
			glidein.setCondorHost(rs.getString("condorHost"));
			
			glidein.setStatus(GlideinStatus.fromString(rs.getString("status")));
			glidein.setStatusMessage(rs.getString("statusMessage"));
			
			Calendar submitted = Calendar.getInstance(TimeZone.getDefault());
			Timestamp submit = rs.getTimestamp("submitted",submitted);
			submitted.setTime(submit);
			glidein.setSubmitted(submitted);
			
			Calendar lastUpdate = Calendar.getInstance(TimeZone.getDefault());
			Timestamp last = rs.getTimestamp("lastUpdate",lastUpdate);
			lastUpdate.setTime(last);
			glidein.setLastUpdate(lastUpdate);
			return glidein;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to create Glidein object",sqle);
		}
	}
}
