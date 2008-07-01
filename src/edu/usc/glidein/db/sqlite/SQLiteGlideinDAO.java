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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedList;

import edu.usc.glidein.db.DatabaseException;
import edu.usc.glidein.db.GlideinDAO;
import edu.usc.glidein.db.JDBCUtil;
import edu.usc.glidein.stubs.types.Glidein;
import edu.usc.glidein.stubs.types.GlideinHistory;
import edu.usc.glidein.stubs.types.GlideinHistoryEntry;
import edu.usc.glidein.stubs.types.GlideinState;

public class SQLiteGlideinDAO implements GlideinDAO
{
	private SQLiteDatabase database = null;
	
	public SQLiteGlideinDAO(SQLiteDatabase db)
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
			stmt = connection.prepareStatement("INSERT INTO glidein (site, count, hostCount, wallTime, numCpus, condorConfig, gcbBroker, idleTime, condorDebug, state, shortMessage, longMessage, submitted, lastUpdate, condorHost, resubmit) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			int i = 1;
			stmt.setInt(i++, glidein.getSiteId());
			stmt.setInt(i++, glidein.getCount());
			stmt.setInt(i++, glidein.getHostCount());
			stmt.setInt(i++, glidein.getWallTime());
			stmt.setInt(i++, glidein.getNumCpus());
			byte[] cfg = glidein.getCondorConfig();
			if (cfg == null) {
				stmt.setString(i++, null);
			} else {
				stmt.setString(i++, new String(cfg));
			}
			stmt.setString(i++, glidein.getGcbBroker());
			stmt.setInt(i++, glidein.getIdleTime());
			stmt.setString(i++, glidein.getCondorDebug());
			stmt.setString(i++, glidein.getState().toString());
			stmt.setString(i++, glidein.getShortMessage());
			stmt.setString(i++, glidein.getLongMessage());
			long time = System.currentTimeMillis();
			stmt.setLong(i++, time);
			stmt.setLong(i++, time);
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
	
	public void store(Glidein glidein) throws DatabaseException
	{
		updateState(glidein.getId(), glidein.getState(), glidein.getShortMessage(), glidein.getLongMessage());
	}
	
	public void delete(int glideinId) throws DatabaseException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = database.getConnection();
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
	
	public void updateState(int glideinId, GlideinState state, String shortMessage, String longMessage)
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
			stmt.setLong(i++, System.currentTimeMillis());
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
			String cfg = rs.getString("condorConfig");
			if (cfg == null) {
				glidein.setCondorConfig(null);
			} else {
				glidein.setCondorConfig(cfg.getBytes());
			}
			glidein.setGcbBroker(rs.getString("gcbBroker"));
			glidein.setIdleTime(rs.getInt("idleTime"));
			glidein.setCondorDebug(rs.getString("condorDebug"));
			glidein.setCondorHost(rs.getString("condorHost"));
			
			glidein.setState(GlideinState.fromString(rs.getString("state")));
			glidein.setShortMessage(rs.getString("shortMessage"));
			glidein.setLongMessage(rs.getString("longMessage"));
			
			Calendar submitted = Calendar.getInstance();
			submitted.setTimeInMillis(rs.getLong("submitted"));
			glidein.setSubmitted(submitted);
			
			Calendar lastUpdate = Calendar.getInstance();
			lastUpdate.setTimeInMillis(rs.getLong("lastUpdate"));
			glidein.setLastUpdate(lastUpdate);
			
			glidein.setResubmit(rs.getBoolean("resubmit"));
			
			return glidein;
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to create Glidein object",sqle);
		}
	}
	
	public GlideinHistory getHistory(int glideinId) throws DatabaseException
	{
		GlideinHistory hist = new GlideinHistory();
		hist.setGlideinId(glideinId);
		LinkedList<GlideinHistoryEntry> entries = new LinkedList<GlideinHistoryEntry>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = database.getConnection();
			stmt = conn.prepareStatement("SELECT * FROM glidein_history WHERE glidein=?");
			stmt.setInt(1,glideinId);
			rs = stmt.executeQuery();
			while (rs.next()) {
				GlideinState state = GlideinState.fromString(rs.getString("state"));
				Calendar time = Calendar.getInstance();
				time.setTimeInMillis(rs.getLong("time"));
				
				GlideinHistoryEntry entry = new GlideinHistoryEntry();
				entry.setState(state);
				entry.setTime(time);
				
				entries.add(entry);
			}
			hist.setHistory(entries.toArray(new GlideinHistoryEntry[0]));
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to get glidein history",sqle);
		} finally {
			JDBCUtil.closeQuietly(rs);
			JDBCUtil.closeQuietly(stmt);
			JDBCUtil.closeQuietly(conn);
		}
		
		return hist;
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
			stmt.setLong(3, time.getTimeInMillis());
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
}
