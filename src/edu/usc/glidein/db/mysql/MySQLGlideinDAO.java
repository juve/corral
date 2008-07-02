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
package edu.usc.glidein.db.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedList;

import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.GlideinDAO;
import edu.usc.glidein.db.JDBCUtil;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinHistoryEntry;
import edu.usc.glidein.stubs.types.GlideinState;

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
			stmt = connection.prepareStatement("INSERT INTO glidein (site, count, hostCount, wallTime, numCpus, condorConfig, gcbBroker, idleTime, condorDebug, state, shortMessage, longMessage, created, lastUpdate, condorHost, resubmit) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			int i = 1;
			stmt.setInt(i++, glidein.getSiteId());
			stmt.setInt(i++, glidein.getCount());
			stmt.setInt(i++, glidein.getHostCount());
			stmt.setInt(i++, glidein.getWallTime());
			stmt.setInt(i++, glidein.getNumCpus());
			stmt.setBytes(i++, glidein.getCondorConfig());
			stmt.setString(i++, glidein.getGcbBroker());
			stmt.setInt(i++, glidein.getIdleTime());
			stmt.setString(i++, glidein.getCondorDebug());
			stmt.setString(i++, glidein.getState().toString());
			stmt.setString(i++, glidein.getShortMessage());
			stmt.setString(i++, glidein.getLongMessage());
			Calendar created = glidein.getCreated();
			if (created == null) {
				stmt.setTimestamp(i++, null);
			} else {
				stmt.setTimestamp(i++, new Timestamp(created.getTimeInMillis()));
			}
			Calendar lastUpdate = glidein.getLastUpdate();
			if (lastUpdate == null) {
				stmt.setTimestamp(i++, null);
			} else {
				stmt.setTimestamp(i++, new Timestamp(lastUpdate.getTimeInMillis()));
			}
			stmt.setString(i++, glidein.getCondorHost());
			stmt.setBoolean(i++, glidein.isResubmit());
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
	
	public void updateState(int glideinId, GlideinState state, String shortMessage, String longMessage, Calendar time)
	throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = database.getConnection();
			stmt = conn.prepareStatement("UPDATE glidein SET state=?, shortMessage=?, longMessage=?, lastUpdate=? WHERE id=?");
			int i = 1;
			stmt.setString(i++, state.toString());
			stmt.setString(i++, shortMessage);
			stmt.setString(i++, longMessage);
			stmt.setTimestamp(i++, new Timestamp(time.getTimeInMillis()));
			stmt.setInt(i++, glideinId);
			if (stmt.executeUpdate()!=1) {
				throw new DatabaseException("Unable to update glidein state: wrong number of db updates");
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
	
	public GlideinHistoryEntry[] getHistory(int[] glideinIds) throws DatabaseException
	{
		LinkedList<GlideinHistoryEntry> entries = new LinkedList<GlideinHistoryEntry>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = database.getConnection();
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM glidein_history");
			if(glideinIds != null && glideinIds.length > 0) {
				sql.append(" WHERE glidein in (");
				for (int i=1; i<=glideinIds.length; i++) {
					sql.append("?");
					if (i<glideinIds.length) sql.append(",");
				}
				sql.append(")");
			}
			sql.append(" ORDER BY time");
			
			stmt = conn.prepareStatement(sql.toString());
			if(glideinIds != null && glideinIds.length > 0) {
				for (int i=1; i<=glideinIds.length; i++)
					stmt.setInt(i,glideinIds[i-1]);
			}
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				int glideinId = rs.getInt("glidein");
				GlideinState state = GlideinState.fromString(rs.getString("state"));
				Calendar time = Calendar.getInstance();
				time.setTime(rs.getTimestamp("time",time));
				
				GlideinHistoryEntry entry = new GlideinHistoryEntry();
				entry.setGlideinId(glideinId);
				entry.setState(state);
				entry.setTime(time);
				
				entries.add(entry);
			}
			
			return entries.toArray(new GlideinHistoryEntry[0]);
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to get glidein history",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
	}
	
	public void insertHistory(int glideinId, GlideinState state, Calendar time)
			throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = database.getConnection();
			stmt = conn.prepareStatement("INSERT INTO glidein_history (glidein,state,time) VALUES (?,?,?)");
			stmt.setInt(1,glideinId);
			stmt.setString(2, state.toString());
			stmt.setTimestamp(3, new Timestamp(time.getTimeInMillis()), time);
			if (stmt.executeUpdate() != 1) {
				throw new DatabaseException(
						"Unable to insert glidein history: " +
						"wrong number of updates");
			}
			conn.commit();
		} catch (SQLException sqle) {
			throw new DatabaseException(
					"Unable to insert glidein history",sqle);
		} finally {
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
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
			glidein.setCondorConfig(rs.getBytes("condorConfig"));
			glidein.setGcbBroker(rs.getString("gcbBroker"));
			glidein.setIdleTime(rs.getInt("idleTime"));
			glidein.setCondorDebug(rs.getString("condorDebug"));
			glidein.setCondorHost(rs.getString("condorHost"));
			
			glidein.setState(GlideinState.fromString(rs.getString("state")));
			glidein.setShortMessage(rs.getString("shortMessage"));
			glidein.setLongMessage(rs.getString("longMessage"));
			
			Calendar created = Calendar.getInstance();
			created.setTime(rs.getTimestamp("created"));
			glidein.setCreated(created);
			
			Calendar lastUpdate = Calendar.getInstance();
			lastUpdate.setTime(rs.getTimestamp("lastUpdate"));
			glidein.setLastUpdate(lastUpdate);
			
			glidein.setResubmit(rs.getBoolean("resubmit"));
			return glidein;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to create Glidein object",sqle);
		}
	}
}
